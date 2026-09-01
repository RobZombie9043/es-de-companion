package com.esde.companion.domain.model

/**
 * One table-of-contents entry for a downloaded guide. [anchorId] means different things per
 * [GameGuideFormat]: for [GameGuideFormat.Html] it's a real element id already present in
 * GameFAQs' own scraped markup - a chapter-level entry (see [depth]) has none at all (`null`),
 * since it addresses a whole saved page rather than a position within one, while a subsection
 * entry's id is the literal anchor GameFAQs itself wrote into the page for its own
 * `.ftoc` to link to (see `GameFaqsBrowserBridge`'s `FTOC_EXTRACT_SCRIPT` - confirmed on-device
 * that these real anchors already exist in the scraped HTML, e.g. `<a id="guide-checklist">`
 * immediately before the heading it names, so no synthetic id needs inventing); for
 * [GameGuideFormat.PlainText] it's a stringified character offset into the guide's single page
 * string (see `domain/gameguides/PlainTextGuideTocParser`) - the viewer resolves this against
 * the rendered text's own line layout (`ui/gameguides/GameGuidePlainTextViewer.kt`'s
 * `scrollToCharOffset`) for an exact jump to that line, since the plain-text page is one
 * unchunked block of text with no other addressable sub-position to jump to directly.
 *
 * [pageIndex] is which of a multi-page [GameGuideFormat.Html] guide's saved pages this entry
 * lives on (a real in-line HTML guide is saved and shown page-by-page, one per chapter -
 * see `GameFaqsBrowserBridge.walkHtmlChapters` - not concatenated into one document), so
 * jumping to this entry means switching to that page first, then scrolling to [anchorId]
 * within it (skipped entirely when [anchorId] is null - landing on the page is the whole
 * jump). Always 0 for [GameGuideFormat.PlainText], which only ever has one page.
 *
 * [depth] is this entry's nesting level (0 = top-level) within the table of contents. For a
 * guide whose `.ftoc` GameFAQs itself organizes into chapters and indented subsections (a real
 * hierarchy - see `FTOC_EXTRACT_SCRIPT`'s kdoc for why this replaced flat per-page heading
 * detection, which both invented ids GameFAQs' own TOC never used and, worse, surfaced every
 * in-content heading rather than GameFAQs' own curated subsection list, confirmed on a real
 * guide as showing far more entries than gamefaqs.gamespot.com's own page), depth 0 is a
 * chapter and depth 1 is one of its subsections; a guide with no `.ftoc` at all (GameFAQs'
 * other, plain-paginated template) falls back to a flat depth-0 list of in-content headings,
 * same as before. For plain text, 0 or 1 or 2 for a guide whose own printed table of contents
 * follows the common Roman-numeral/letter/number FAQ convention (see
 * `domain/gameguides/PlainTextGuideTocParser`'s hierarchical parsing path), 0 otherwise.
 */
data class GuideTocEntry(
    val title: String,
    val anchorId: String?,
    val pageIndex: Int = 0,
    val depth: Int = 0,
)
