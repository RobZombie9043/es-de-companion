package com.esde.companion.data.gameguides

import android.webkit.WebView
import com.esde.companion.domain.model.GuideTocEntry
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

/**
 * Reads a GameFAQs in-line HTML guide's real `.ftoc` chapter/subsection hierarchy - split out
 * of [GameFaqsBrowserBridge] purely to keep that class under detekt's function-count threshold,
 * not for reuse elsewhere; [GameFaqsBrowserBridge.walkHtmlChapters] calls [extract] exactly
 * once, on the true first page.
 *
 * Reads `.ftoc` directly rather than tagging whatever headings happen to appear in each
 * chapter's own content ([GuidePageContentProcessor]'s fallback for guides with no `.ftoc` at
 * all - see [extract]'s kdoc for why this replaced that as the primary source). Confirmed on a
 * real guide (Persona 4 Golden, GameFAQs FAQ id 76145) that a chapter `<li>` and its nested
 * `<ol>`/`<ul>` are DOM siblings within the same outer `<ol>` - not the nested list inside the
 * chapter's own `<li>`, despite how the rendered indentation looks - so [FTOC_EXTRACT_SCRIPT]
 * walks the outer `<ol>`'s direct children in order and attaches each nested list to whichever
 * `<li>` immediately preceded it, rather than querying into each `<li>` itself.
 *
 * A nested list's own links are **not** always in-page anchors on the preceding entry's page,
 * despite Persona 4 Golden making it look that way (there, every one of them is - a `#fragment`
 * href into that same chapter). Confirmed on a second real guide (Metroid Prime,
 * GameFAQs FAQ id 81836) that a nested list can instead hold real *sub-chapters* - each its own
 * separate page (a plain, fragment-free href, exactly like a top-level chapter link), just
 * grouped for readability under a parent heading like "Walkthrough" rather than listed flat.
 * [extract] tells the two apart by each link's own *pre-fragment* href (its [TocOutlineEntryDto.pageKey],
 * everything before a literal `#` if present, the whole href otherwise) changing from the
 * previous entry's - not, as an earlier version of this assumed, by whether the link carries a
 * `#fragment` at all. That assumption broke on a fourth real guide (NieR: Automata, GameFAQs FAQ
 * id 75141) whose real top-level chapter links, not just their subsections, each carry their own
 * page's first-heading fragment too (`?page=4#*Chapter 01`, not a bare `?page=4`) - every single
 * `.ftoc` entry in that guide has a fragment, so the old "fragment present -> same page" rule
 * left `currentPageIndex` at 0 for the entire guide, and every table-of-contents tap silently
 * failed to navigate (tried to scroll to an id that only existed on some other, never-loaded
 * page). Comparing the fragment-stripped href instead still groups Persona 4 Golden's own
 * subsections correctly (each shares its parent chapter's exact fragment-free href) and still
 * treats each of Metroid Prime's fragment-free sub-chapter hrefs as its own new page (each
 * genuinely differs), while also handling NieR's shape, where every entry - chapter or
 * subsection - reuses the SAME pre-fragment href within one real page and changes it for the
 * next. [GuideTocEntry.anchorId] is still the literal fragment as-is (nullable - see below for
 * why no id needs inventing), regardless of which rule decided the page transition. Depth is
 * still recorded separately, straight from DOM nesting, purely for the table of contents' own
 * indentation - it's cosmetic once the paging story above is right. A first version of this only
 * recognized subsection-style nested lists, silently dropping every real sub-chapter in a guide
 * shaped like Metroid Prime's - most of that guide's pages ended up with zero real entries and
 * fell back to the old per-page heading detection, reproducing the exact over-generated table of
 * contents this feature exists to fix.
 *
 * An in-page anchor's fragment (e.g. `guide-checklist` from `100-guide#guide-checklist`) is
 * used as-is for [GuideTocEntry.anchorId], not a synthesized id - confirmed on a real chapter
 * page that GameFAQs already writes a real `<a id="guide-checklist">` immediately before the
 * heading it names, so the saved page's own scraped HTML already has exactly the element this
 * needs to scroll to, with nothing extra to tag.
 *
 * Not every `<a>` inside `.ftoc` is a navigation link at all, either - confirmed on a third
 * real guide (Tomb Raider, GameFAQs FAQ id 66644) that a "show more..." link with no `href`
 * attribute whatsoever sat in the middle of an otherwise ordinary subsection list, a plain
 * jQuery show/hide toggle for a long list GameFAQs truncates by default. Left in, its missing
 * href made it fragment-null the same way a real separately-paged sub-chapter is, so it was
 * wrongly counted as a new page - both showing a bogus "show more..." entry and shifting every
 * later chapter's pageIndex off by one. [FTOC_EXTRACT_SCRIPT]'s `hasRealHref` check is a
 * completely separate filter from the fragment check above: it excludes a link outright,
 * before fragment presence is ever considered for it.
 */
class GuideTocOutlineExtractor {
    /**
     * Assigns each raw outline entry its real page index positionally - see this class's own
     * kdoc for why that's driven by each entry's pre-fragment href changing, not DOM nesting
     * depth or fragment presence. This relies on the walk visiting pages in the exact order
     * `.ftoc` itself lists them, which holds since both are derived from the same site-defined
     * sequence (the walk just follows each page's own "Next: <title>" link, one hop at a time,
     * rather than reading `.ftoc`'s order directly) - confirmed on three real guides (Persona 4
     * Golden, GameFAQs FAQ id 76145: 13 chapters, 147 subsections; Metroid Prime, GameFAQs FAQ
     * id 81836: 3 top-level chapters plus 18 real sub-chapter pages nested under one of them;
     * NieR: Automata, GameFAQs FAQ id 75141: ~20 chapters, every entry fragment-bearing), all
     * matching gamefaqs.gamespot.com's own listing exactly.
     */
    suspend fun extract(webView: WebView): List<GuideTocEntry> {
        val outline = evaluateOutline(webView)
        var currentPageIndex = -1
        var previousPageKey: String? = null
        return outline.map { entry ->
            if (entry.pageKey != previousPageKey) currentPageIndex++
            previousPageKey = entry.pageKey
            GuideTocEntry(
                title = entry.title,
                anchorId = entry.fragment,
                pageIndex = currentPageIndex.coerceAtLeast(0),
                depth = entry.depth,
            )
        }
    }

    private suspend fun evaluateOutline(webView: WebView): List<TocOutlineEntryDto> {
        val raw =
            suspendCancellableCoroutine { continuation ->
                webView.evaluateJavascript(FTOC_EXTRACT_SCRIPT) { result -> continuation.resume(result ?: "null") }
            }
        return try {
            Json.decodeFromString<TocOutlineDto>(raw).entries
        } catch (
            @Suppress("SwallowedException") e: SerializationException,
        ) {
            emptyList()
        }
    }

    private companion object {
        val FTOC_EXTRACT_SCRIPT =
            """
            (function() {
                function fragmentOf(href) {
                    var i = href.indexOf('#');
                    return i === -1 ? null : href.slice(i + 1);
                }
                function pageKeyOf(href) {
                    var i = href.indexOf('#');
                    return i === -1 ? href : href.slice(0, i);
                }
                // A real chapter/subsection link always has SOME href, even a bare "#" one -
                // an <a> with none at all is a decorative JS control, not a navigation target.
                // Confirmed on a real guide (Tomb Raider, GameFAQs FAQ id 66644): a "show
                // more..." link with no href attribute at all (a jQuery show/hide toggle for a
                // long subsection list GameFAQs truncates by default) sitting right in the
                // middle of an otherwise normal subsection list. Left unfiltered, its href-less
                // (therefore fragment-null) href was indistinguishable from a real
                // separately-paged sub-chapter (see this file's own kdoc on Metroid Prime),
                // so it was wrongly counted as a new page - shifting every following chapter's
                // pageIndex off by one, on top of showing a bogus "show more..." TOC entry.
                function hasRealHref(a) { return a.getAttribute('href') !== null; }
                var ftoc = document.querySelector('.ftoc');
                var ol = ftoc ? ftoc.querySelector('ol, ul') : null;
                if (!ol) return { entries: [] };
                var entries = [];
                Array.prototype.slice.call(ol.children).forEach(function(child) {
                    if (child.tagName === 'LI') {
                        var a = child.querySelector('a');
                        if (!a || !hasRealHref(a)) return;
                        entries.push({
                            title: (a.textContent || '').trim(),
                            fragment: fragmentOf(a.getAttribute('href') || ''),
                            pageKey: pageKeyOf(a.getAttribute('href') || ''),
                            depth: 0
                        });
                    } else if (child.tagName === 'OL' || child.tagName === 'UL') {
                        // Every REAL link here, not just the ones with a #fragment - a nested
                        // list can hold real, separately-paged sub-chapters just as often as
                        // in-page anchors on the preceding entry's page (see this file's class
                        // kdoc); fragmentOf(...) being null vs non-null is exactly what tells
                        // them apart, so nothing here should be filtered on that basis - only
                        // hasRealHref(...) (a completely separate, orthogonal check) excludes
                        // anything at all.
                        Array.prototype.slice.call(child.querySelectorAll('a')).forEach(function(subA) {
                            if (!hasRealHref(subA)) return;
                            entries.push({
                                title: (subA.textContent || '').trim(),
                                fragment: fragmentOf(subA.getAttribute('href') || ''),
                                pageKey: pageKeyOf(subA.getAttribute('href') || ''),
                                depth: 1
                            });
                        });
                    }
                });
                return { entries: entries };
            })();
            """.trimIndent()
    }
}

@Serializable
private data class TocOutlineDto(
    val entries: List<TocOutlineEntryDto> = emptyList(),
)

@Serializable
private data class TocOutlineEntryDto(
    val title: String,
    val fragment: String? = null,
    val pageKey: String = "",
    val depth: Int = 0,
)
