package com.esde.companion.data.gameguides

import android.webkit.WebView
import android.webkit.WebViewClient
import com.esde.companion.domain.gameguides.GameFaqsGuidePage
import com.esde.companion.domain.gameguides.GuideDownloadProgress
import com.esde.companion.domain.model.GameGuideFormat
import com.esde.companion.domain.model.GuideTocEntry
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

// A per-hop dead-man's switch, not a budget for the whole multi-chapter walk - a large guide
// legitimately takes many navigations, but any ONE of them either finishes well under this or
// is considered stuck and the walk stops with whatever chapters it already has (see
// walkHtmlChapters()), rather than hanging the Save flow forever.
private const val NAVIGATE_TIMEOUT_MILLIS = 20_000L

// Bounds how many chapter pages downloadFullGuide will walk for one guide, in case a
// malformed/looping "Next:" chain never terminates.
private const val MAX_CHAPTERS = 200

/**
 * Reads back whatever HTML a [WebView] has already rendered, via JS evaluation - the WebView
 * owns its own networking for browsing/detection/pagination, this just inspects/drives the
 * result. Guide *images* are fetched separately - see [NativeImageDownloader]'s kdoc for why
 * those go through real native `HttpURLConnection` calls instead of the WebView's own
 * `fetch()` (see CLAUDE.md's "What NOT to Do" for this project's networking conventions).
 *
 * [DETECT_SCRIPT]'s selectors were confirmed against live GameFAQs pages for both guide
 * formats the site actually serves: a plain-text FAQ (`<div class="faqtext" id="faqtext">
 * <pre id="faqspan-1">...`, [GameGuideFormat.PlainText]) and an "in-line HTML" guide (`<div
 * id="faqwrap" class="ffaq ffaqbody">`, containing a `.ftoc` table-of-contents block plus
 * real heading/paragraph/image markup - no `<pre>` at all, [GameGuideFormat.Html]). The
 * text-only fallback candidates are defensive guesses for page templates that weren't
 * sampled, since GameFAQs has redesigned its template more than once and this app has no
 * way to pin to a known-good version of it going forward. It's written to degrade to
 * isGuidePage=false rather than throw when nothing matches, so a wrong guess just hides the
 * Save action instead of crashing or saving garbage - same resilience principle as
 * `com.esde.companion.data.thor.PrivilegedShell`'s "degrade, never crash" contract.
 */
class GameFaqsBrowserBridge(
    private val tocOutlineExtractor: GuideTocOutlineExtractor = GuideTocOutlineExtractor(),
) {
    suspend fun detectGuidePage(webView: WebView): GameFaqsGuidePage = evaluate(webView, DETECT_SCRIPT)

    /**
     * For [GameGuideFormat.Html], walks every chapter page (see [walkHtmlChapters]) and
     * returns each chapter's raw (un-embedded - no images inlined yet) HTML as its own entry
     * in the returned page's `pages` list, rather than concatenated into one document - a real
     * in-line HTML guide keeps GameFAQs' own per-chapter page structure, the same way the site
     * itself presents it, rather than becoming one arbitrarily long scroll. Image embedding/
     * heading-tagging happens later, per page, in `GuideDownloadAndSave` as each page is about
     * to be saved - not here - so a many-chapter guide's fully-embedded (base64-images-inlined)
     * pages are never all resident in memory at once (confirmed crashing an image-heavy 20+
     * chapter guide with an OutOfMemoryError when this method used to embed everything before
     * returning it).
     */
    suspend fun downloadFullGuide(
        webView: WebView,
        onProgress: (GuideDownloadProgress) -> Unit = {},
    ): GameFaqsGuidePage {
        val first = evaluate(webView, DETECT_SCRIPT)
        if (!first.isGuidePage || first.format != GameGuideFormat.Html) return first
        rewindToFirstPageIfNeeded(webView)
        return walkHtmlChapters(webView, first, onProgress)
    }

    /**
     * Some in-line HTML guides are split across chapter pages (see [walkHtmlChapters]'s kdoc),
     * others across a plain "Page 1 of N" / "Next Page" scheme for one long document (confirmed
     * on a real guide, Ori and the Blind Forest FAQ id 71410 - no `.ftoc` at all, just a
     * `ul.paginate` with First/Previous/Next/Last Page links). Either way, the Download action
     * is triggered from whatever page the user happened to be browsing when they tapped it -
     * confirmed on that same guide, whose own page 1 never shows Download at all (see
     * [DETECT_SCRIPT]'s kdoc), so a real download always starts mid-guide. Without rewinding
     * first, [walkHtmlChapters] would only walk *forward* from wherever it started, silently
     * saving a guide missing every page before that one. [FIND_FIRST_PAGE_SCRIPT] is a no-op
     * (returns null) for guides that don't expose a "First Page" link at all - the chapter-link
     * style guides, and anything already on page 1 - so this only ever affects the case it's
     * meant to fix.
     */
    private suspend fun rewindToFirstPageIfNeeded(webView: WebView) {
        val firstPageUrl = findFirstPageUrl(webView) ?: return
        withTimeoutOrNull(NAVIGATE_TIMEOUT_MILLIS) { navigateAndAwaitLoad(webView, firstPageUrl) }
    }

    private suspend fun findFirstPageUrl(webView: WebView): String? {
        val raw = evaluateRaw(webView, FIND_FIRST_PAGE_SCRIPT)
        if (raw == "null") return null
        return try {
            Json.decodeFromString<String>(raw)
        } catch (
            @Suppress("SwallowedException") e: SerializationException,
        ) {
            null
        }
    }

    /**
     * In-line HTML guides are split across separate page loads by GameFAQs itself, each
     * carrying either a "Next: <title>" chapter link (with its own copy of the guide's full
     * `.ftoc` chapter listing, used to report [GuideDownloadProgress.LoadingPage]'s
     * `totalPages`) or, for guides that aren't chaptered at all, a plain "Next Page" link in a
     * `ul.paginate` block alongside a "Page X of Y" label (used for `totalPages` instead, when
     * there's no `.ftoc` to count) - see [CHAPTER_EXTRACT_SCRIPT]. Either way the walk itself
     * stops on a missing next-hop link, not a count. GameFAQs' own `?single=1` query param was
     * tried first as a simpler alternative to reconstructing this chapter-link structure, but
     * doesn't work for every guide template - confirmed on a real guide (Metroid Prime, GameFAQs
     * FAQ id 81836) where it returned an unrelated challenge page instead of the combined guide,
     * silently truncating every download of that guide to its first chapter. Walking each real
     * next-hop link is slower but doesn't depend on an unconfirmed per-guide site feature.
     *
     * Each hop navigates via [navigateAndAwaitLoad] under its own [NAVIGATE_TIMEOUT_MILLIS] -
     * [WebView.loadUrl] returns before the new page has actually loaded, so evaluating a
     * script right after it (as an earlier version of this method did) read back the
     * *previous* page's still-current DOM, silently "succeeding" with only the first chapter
     * every time. A hop that times out stops the walk with whatever chapters were already
     * collected, rather than failing (or hanging) the whole download.
     */
    private suspend fun walkHtmlChapters(
        webView: WebView,
        first: GameFaqsGuidePage,
        onProgress: (GuideDownloadProgress) -> Unit,
    ): GameFaqsGuidePage {
        val pages = mutableListOf<String>()
        var totalPages = 1
        var tocEntries = emptyList<GuideTocEntry>()
        var page = 0
        var nextUrl: String?
        do {
            page++
            val chapter = evaluateChapter(webView)
            if (page == 1) {
                totalPages = chapter.totalChapters.coerceAtLeast(1)
                // Read once, from the true first page (walkHtmlChapters always starts there -
                // see downloadFullGuide's rewind step) - every chapter page repeats the same
                // full .ftoc listing (same assumption CHAPTER_EXTRACT_SCRIPT's own totalChapters
                // count already relies on), so there's nothing more to gain reading it again on
                // later pages, only more work.
                tocEntries = tocOutlineExtractor.extract(webView)
            }
            pages += chapter.html
            onProgress(GuideDownloadProgress.LoadingPage(page, totalPages))
            val urlToLoad = chapter.nextUrl
            nextUrl =
                if (urlToLoad != null && page < MAX_CHAPTERS) {
                    val loaded = withTimeoutOrNull(NAVIGATE_TIMEOUT_MILLIS) { navigateAndAwaitLoad(webView, urlToLoad) }
                    if (loaded == null) null else urlToLoad
                } else {
                    null
                }
        } while (nextUrl != null && page < MAX_CHAPTERS)

        return GameFaqsGuidePage(
            isGuidePage = true,
            title = first.title,
            format = GameGuideFormat.Html,
            pages = pages,
            tocEntries = tocEntries,
        )
    }

    /** Temporarily swaps in a [WebViewClient] that resumes once the new page finishes
     * loading, restoring the original client immediately after (including on cancellation,
     * e.g. the caller's timeout firing before `onPageFinished` ever does) - self-contained
     * so callers don't need to plumb a "wait for load" signal through the UI layer. */
    private suspend fun navigateAndAwaitLoad(
        webView: WebView,
        url: String,
    ) {
        val originalClient = webView.webViewClient
        try {
            suspendCancellableCoroutine { continuation ->
                webView.webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView,
                            finishedUrl: String?,
                        ) {
                            webView.webViewClient = originalClient
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }
                webView.loadUrl(url)
            }
        } finally {
            webView.webViewClient = originalClient
        }
    }

    private suspend fun evaluate(
        webView: WebView,
        script: String,
    ): GameFaqsGuidePage {
        val raw = evaluateRaw(webView, script)
        return try {
            Json.decodeFromString<GameFaqsGuidePageDto>(raw).toDomain()
        } catch (
            @Suppress("SwallowedException") e: SerializationException,
        ) {
            GameFaqsGuidePage(isGuidePage = false, title = "", format = GameGuideFormat.PlainText, pages = emptyList())
        }
    }

    private suspend fun evaluateRaw(
        webView: WebView,
        script: String,
    ): String =
        suspendCancellableCoroutine { continuation ->
            webView.evaluateJavascript(script) { result -> continuation.resume(result ?: "null") }
        }

    private suspend fun evaluateChapter(webView: WebView): Chapter {
        val raw = evaluateRaw(webView, CHAPTER_EXTRACT_SCRIPT)
        return try {
            Json.decodeFromString<ChapterDto>(raw).toDomain()
        } catch (
            @Suppress("SwallowedException") e: SerializationException,
        ) {
            Chapter(html = "", nextUrl = null, totalChapters = 1)
        }
    }

    private companion object {
        // Checks #faqwrap (in-line HTML guide) first, since it's a structurally different
        // template from the plain-text one - falls through to the #faqtext/<pre> family
        // only when it's absent. Both branches strip .ftoc (the in-line HTML guide's own
        // table-of-contents block) out before reading content, so a saved guide doesn't
        // start with a dump of every chapter link. Returns a plain object, NOT
        // JSON.stringify(...) - WebView.evaluateJavascript's callback already JSON-encodes
        // whatever the script returns, so a script that itself returns a string gets that
        // string encoded AGAIN (wrapped in an extra layer of quotes/escaping) by the time it
        // reaches evaluateRaw's callback. Returning the raw object here lets WebView do that
        // encoding exactly once, so the callback receives plain, directly-decodable JSON -
        // confirmed via a real double-encoded string silently failing Json.decodeFromString
        // in evaluate() (caught, but always looked like "not a guide page" rather than a
        // real page-detection issue).
        val DETECT_SCRIPT =
            """
            (function() {
                function text(el) { return el ? (el.innerText || el.textContent || '') : ''; }
                // The site's own per-guide title (platform + author, never the game name -
                // confirmed present, and game-name-free, on every chapter page sampled for
                // both guide formats: "<h2 class="title">Guide and Walkthrough (VITA) by
                // <a>Zoel</a></h2>"). Falls back to document.title (which DOES include the
                // game name - see GuideTitleCleaner) only if this template ever changes.
                function guideTitle() {
                    var titleText = text(document.querySelector('h2.title')).trim();
                    return titleText.length > 0 ? titleText : (document.title || '');
                }
                function withoutToc(el) {
                    var clone = el.cloneNode(true);
                    var toc = clone.querySelector('.ftoc');
                    if (toc) toc.parentNode.removeChild(toc);
                    return clone;
                }
                var faqWrapEl = document.querySelector('#faqwrap');
                // #faqwrap alone isn't a reliable signal - some plain-text guides are ALSO
                // wrapped in a #faqwrap div (a generic page-layout wrapper there, not a
                // marker of real HTML content), with their actual #faqtext/<pre> plain-text
                // block nested directly inside it. Confirmed on a real guide (Persona 4
                // Golden, GameFAQs FAQ id 64387) that was misdetected as HTML purely because
                // of this - a real in-line HTML guide's #faqwrap has no such nested
                // plain-text container carrying real text.
                // Checking for the nested element's mere presence isn't enough, though - a
                // genuine in-line HTML guide can still contain an empty, purely decorative
                // <pre></pre> (confirmed on a real guide, Ori and the Blind Forest FAQ id
                // 71410's own first page) that matched this same selector and wrongly
                // disqualified it, hiding Download on an otherwise perfectly normal guide
                // page. Requiring actual text in the nested match keeps the Persona 4 Golden
                // fix (its nested block is real, substantial plain text) while no longer
                // being fooled by an empty one.
                var nestedTextEls = faqWrapEl
                    ? Array.prototype.slice.call(faqWrapEl.querySelectorAll('#faqtext, .faqtext, pre'))
                    : [];
                var hasRealNestedText = nestedTextEls.some(function(el) { return text(el).trim().length > 0; });
                var htmlEl = (faqWrapEl && !hasRealNestedText) ? faqWrapEl : null;
                if (htmlEl) {
                    var cleanedHtml = withoutToc(htmlEl);
                    var htmlContent = cleanedHtml.innerHTML || '';
                    var isHtmlGuide = text(cleanedHtml).trim().length > 500;
                    return {
                        isGuidePage: isHtmlGuide,
                        title: guideTitle(),
                        format: 'html',
                        pages: isHtmlGuide ? [htmlContent] : []
                    };
                }
                var textCandidates = [
                    document.querySelector('#faqtext'),
                    document.querySelector('.faqtext'),
                    document.querySelector('article pre'),
                    document.querySelector('.floatholder pre')
                ].filter(Boolean);
                if (textCandidates.length === 0) {
                    var pres = Array.prototype.slice.call(document.querySelectorAll('pre'));
                    pres.sort(function(a, b) { return text(b).length - text(a).length; });
                    if (pres.length > 0) textCandidates.push(pres[0]);
                }
                var textContent = textCandidates.length > 0 ? text(withoutToc(textCandidates[0])) : '';
                var isTextGuide = textContent.trim().length > 500;
                return {
                    isGuidePage: isTextGuide,
                    title: guideTitle(),
                    format: 'text',
                    pages: isTextGuide ? [textContent] : []
                };
            })();
            """.trimIndent()

        // Run on every chapter page of an in-line HTML guide (including the first, before any
        // navigation) - extracts that page's own cleaned #faqwrap content, the absolute URL of
        // its next-hop link (null on the last page), and the total page count. Two distinct
        // GameFAQs pagination styles are handled, since a guide only ever uses one of them:
        // a "Next: <title>" chapter link plus a `.ftoc` chapter-listing block (every chapter
        // page repeats the same full listing, so counting it is stable regardless of which
        // chapter it's read from), or a plain "Next Page" link plus a `ul.paginate` "Page X
        // of Y" label for a guide that's really one long document GameFAQs split up by length
        // rather than by chapter - confirmed on a real guide (Ori and the Blind Forest,
        // GameFAQs FAQ id 71410) with no `.ftoc` at all.
        //
        // .ftoc lists every real chapter link (one per separate page GameFAQs will actually
        // serve) AND, nested underneath each, in-page sub-section anchors that jump to a
        // heading *within* that same chapter rather than a new page - counting all of them
        // together massively overcounts the walk's true length. Confirmed on a real guide
        // (Persona 4 Golden, GameFAQs FAQ id 76145): 13 real chapters, but 160 total <a>
        // elements once every sub-section link is included, reported to the user as
        // "downloading page x of 160" for a guide that's actually 13 pages. A real chapter
        // link's href is just a page slug ("dungeons"); a sub-section link's href is that
        // same slug plus a "#fragment" pointing at a heading id on that page - filtering to
        // only fragment-free hrefs recovers the real per-page chapter count.
        val CHAPTER_EXTRACT_SCRIPT =
            """
            (function() {
                var wrap = document.querySelector('#faqwrap');
                var html = '';
                if (wrap) {
                    var clone = wrap.cloneNode(true);
                    var toc = clone.querySelector('.ftoc');
                    if (toc) toc.parentNode.removeChild(toc);
                    html = clone.innerHTML || '';
                }
                var paginateLinks = Array.prototype.slice.call(document.querySelectorAll('ul.paginate a'));
                var nextLink = paginateLinks.filter(function(a) {
                    var t = (a.innerText || a.textContent || '').trim();
                    return t.indexOf('Next:') === 0 || t === 'Next Page';
                })[0];
                var chapterLinks = Array.prototype.slice.call(document.querySelectorAll('.ftoc a'))
                    .filter(function(a) {
                        var href = a.getAttribute('href');
                        return href && href.indexOf('#') === -1;
                    });
                var totalChapters = chapterLinks.length;
                if (totalChapters === 0) {
                    var paginate = document.querySelector('ul.paginate');
                    var match = paginate ? (paginate.innerText || '').match(/Page\s+\d+\s+of\s+(\d+)/) : null;
                    if (match) totalChapters = parseInt(match[1], 10);
                }
                return {
                    html: html,
                    nextUrl: nextLink ? nextLink.href : null,
                    totalChapters: totalChapters
                };
            })();
            """.trimIndent()

        // Finds the "First Page" link in a plain "Page X of Y" pagination block (see
        // CHAPTER_EXTRACT_SCRIPT's kdoc) - absent both for chapter-link-style guides (which
        // have no such link at all) and for a plain-paginated guide's own actual first page,
        // so this only ever fires when it needs to. See downloadFullGuide's kdoc for why the
        // walk must rewind to it before starting.
        val FIND_FIRST_PAGE_SCRIPT =
            """
            (function() {
                var firstLink = Array.prototype.slice.call(document.querySelectorAll('ul.paginate a'))
                    .filter(function(a) { return (a.innerText || a.textContent || '').trim() === 'First Page'; })[0];
                return firstLink ? firstLink.href : null;
            })();
            """.trimIndent()
    }
}

@Serializable
private data class GameFaqsGuidePageDto(
    val isGuidePage: Boolean,
    val title: String,
    val format: String,
    val pages: List<String>,
)

private fun GameFaqsGuidePageDto.toDomain() =
    GameFaqsGuidePage(
        isGuidePage = isGuidePage,
        title = title,
        format = if (format == "html") GameGuideFormat.Html else GameGuideFormat.PlainText,
        pages = pages,
    )

private data class Chapter(
    val html: String,
    val nextUrl: String?,
    val totalChapters: Int,
)

@Serializable
private data class ChapterDto(
    val html: String,
    val nextUrl: String? = null,
    val totalChapters: Int = 1,
)

private fun ChapterDto.toDomain() = Chapter(html = html, nextUrl = nextUrl, totalChapters = totalChapters)
