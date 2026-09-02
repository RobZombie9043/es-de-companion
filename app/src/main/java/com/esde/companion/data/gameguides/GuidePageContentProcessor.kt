package com.esde.companion.data.gameguides

import android.webkit.WebView
import com.esde.companion.domain.model.GuideTocEntry
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

private const val MAX_EMBEDDED_IMAGES = 40

/**
 * Post-processes one already-fetched chapter page's HTML for offline saving: tags every
 * heading with a stable id (for the viewer's table of contents), then makes a best-effort
 * attempt to save its images as real files under [mediaDirectoryPath] and rewrite their `src`
 * to point at them, via [imageDownloader] - see its kdoc for why this saves real files rather
 * than inlining base64 `data:` URIs. Split out of [GameFaqsBrowserBridge] to keep that class
 * focused on browsing/detection/pagination.
 *
 * Heading-tagging and image-fetching are deliberately independent steps, not one combined
 * pass: tagging headings is synchronous DOM work with no way to fail slowly, while image
 * fetching depends on network calls that can legitimately run long - an earlier version did
 * both together, so a slow/timed-out image pass silently discarded the table of contents
 * too, even though it had nothing to do with why images were slow. Keeping them separate
 * means a bad connection can only ever cost embedded images, never the TOC.
 */
class GuidePageContentProcessor(
    private val imageDownloader: NativeImageDownloader = NativeImageDownloader(),
) {
    suspend fun process(
        webView: WebView,
        html: String,
        pageIndex: Int,
        mediaDirectoryPath: String,
    ): EmbeddedGuideContent {
        val tagged = tagHeadings(webView, html, pageIndex)
        val imageUrls = extractImageUrls(webView, tagged.html)
        val replacements =
            if (imageUrls.isEmpty()) {
                emptyMap()
            } else {
                imageDownloader.downloadImages(imageUrls, mediaDirectoryPath, pageIndex)
            }
        if (replacements.isEmpty()) return tagged
        val embeddedHtml = substituteImageSrcs(webView, tagged.html, replacements)
        return tagged.copy(html = embeddedHtml)
    }

    private suspend fun tagHeadings(
        webView: WebView,
        html: String,
        pageIndex: Int,
    ): EmbeddedGuideContent {
        val raw = evaluateRaw(webView, tagHeadingsScript(html, pageIndex))
        return try {
            Json.decodeFromString<EmbeddedGuideContentDto>(raw).toDomain(pageIndex)
        } catch (
            @Suppress("SwallowedException") e: SerializationException,
        ) {
            EmbeddedGuideContent(html = html, tocEntries = emptyList())
        }
    }

    /** Reads back the resolved absolute URL of every `<img>` in [html] (bounded by
     * [MAX_EMBEDDED_IMAGES]) - a plain synchronous DOM query, no network involved. Resolving
     * through the DOM (rather than parsing `src` attributes as text) correctly turns a
     * page-relative `src` into the real absolute URL [imageDownloader] needs. */
    private suspend fun extractImageUrls(
        webView: WebView,
        html: String,
    ): List<String> {
        val raw = evaluateRaw(webView, extractImageUrlsScript(html))
        return try {
            Json.decodeFromString<List<String>>(raw)
        } catch (
            @Suppress("SwallowedException") e: SerializationException,
        ) {
            emptyList()
        }
    }

    /** Swaps each `<img>`'s `src` for its downloaded data URI where [replacements] has one
     * (any URL that failed to download is simply absent, left pointing at its original
     * network URL) - another plain synchronous DOM pass, not a network operation. */
    private suspend fun substituteImageSrcs(
        webView: WebView,
        html: String,
        replacements: Map<String, String>,
    ): String {
        val raw = evaluateRaw(webView, substituteImageSrcsScript(html, replacements))
        return try {
            Json.decodeFromString<EmbedImagesResultDto>(raw).html
        } catch (
            @Suppress("SwallowedException") e: SerializationException,
        ) {
            html
        }
    }

    private suspend fun evaluateRaw(
        webView: WebView,
        script: String,
    ): String =
        suspendCancellableCoroutine { continuation ->
            webView.evaluateJavascript(script) { result -> continuation.resume(result ?: "null") }
        }
}

// __HTML__/__PAGE_INDEX__ substituted textually by tagHeadingsScript() - a plain synchronous
// DOM pass, returned via the normal evaluateJavascript callback (same plain-object-not-
// JSON.stringify convention GameFaqsBrowserBridge's own scripts use, for the same double-
// encoding reason documented there) - nothing here can run long.
private val TAG_HEADINGS_SCRIPT_TEMPLATE =
    """
    (function() {
        var container = document.createElement('div');
        container.innerHTML = __HTML__;
        var headings = Array.prototype.slice.call(container.querySelectorAll('h1, h2, h3, h4'))
            .filter(function(h) { return (h.innerText || h.textContent || '').trim().length > 0; });
        var toc = headings.map(function(h, i) {
            var id = 'toc-__PAGE_INDEX__-' + i;
            h.id = id;
            return { title: (h.innerText || h.textContent || '').trim(), anchorId: id };
        });
        return { html: container.innerHTML, toc: toc };
    })();
    """.trimIndent()

private fun tagHeadingsScript(
    html: String,
    pageIndex: Int,
): String =
    TAG_HEADINGS_SCRIPT_TEMPLATE
        .replace("__HTML__", Json.encodeToString(html))
        .replace("__PAGE_INDEX__", pageIndex.toString())

// __HTML__/__MAX_IMAGES__ substituted textually by extractImageUrlsScript() - a plain
// synchronous DOM query, no network involved (see extractImageUrls()'s kdoc for why reading
// through the DOM's resolved img.src, not the raw src attribute text, matters here).
private val EXTRACT_IMAGE_URLS_SCRIPT_TEMPLATE =
    """
    (function() {
        var container = document.createElement('div');
        container.innerHTML = __HTML__;
        var imgs = Array.prototype.slice.call(container.querySelectorAll('img')).slice(0, __MAX_IMAGES__);
        return imgs.map(function(img) { return img.src; });
    })();
    """.trimIndent()

private fun extractImageUrlsScript(html: String): String =
    EXTRACT_IMAGE_URLS_SCRIPT_TEMPLATE
        .replace("__HTML__", Json.encodeToString(html))
        .replace("__MAX_IMAGES__", MAX_EMBEDDED_IMAGES.toString())

// __HTML__/__REPLACEMENTS__ substituted textually by substituteImageSrcsScript() - both
// spliced in already JSON-encoded. Another plain synchronous DOM pass (build the same
// container, swap srcs, read innerHTML back out) - the actual downloading already happened
// natively in NativeImageDownloader by the time this runs.
private val SUBSTITUTE_IMAGE_SRCS_SCRIPT_TEMPLATE =
    """
    (function() {
        var container = document.createElement('div');
        container.innerHTML = __HTML__;
        var replacements = __REPLACEMENTS__;
        var imgs = Array.prototype.slice.call(container.querySelectorAll('img'));
        imgs.forEach(function(img) {
            if (Object.prototype.hasOwnProperty.call(replacements, img.src)) {
                img.src = replacements[img.src];
            }
        });
        return { html: container.innerHTML };
    })();
    """.trimIndent()

private fun substituteImageSrcsScript(
    html: String,
    replacements: Map<String, String>,
): String =
    SUBSTITUTE_IMAGE_SRCS_SCRIPT_TEMPLATE
        .replace("__HTML__", Json.encodeToString(html))
        .replace("__REPLACEMENTS__", Json.encodeToString(replacements))

data class EmbeddedGuideContent(
    val html: String,
    val tocEntries: List<GuideTocEntry>,
)

@Serializable
private data class EmbedImagesResultDto(
    val html: String,
)

@Serializable
private data class EmbeddedGuideContentDto(
    val html: String,
    val toc: List<EmbeddedTocEntryDto>,
)

@Serializable
private data class EmbeddedTocEntryDto(
    val title: String,
    val anchorId: String,
)

private fun EmbeddedGuideContentDto.toDomain(pageIndex: Int) =
    EmbeddedGuideContent(html = html, tocEntries = toc.map { GuideTocEntry(it.title, it.anchorId, pageIndex) })
