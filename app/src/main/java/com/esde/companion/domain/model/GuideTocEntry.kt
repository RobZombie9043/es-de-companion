package com.esde.companion.domain.model

/**
 * One table-of-contents entry for a downloaded guide. [anchorId] means different things per
 * [GameGuideFormat]: for [GameGuideFormat.Html] it's a real element id injected into the
 * saved markup at download time (see `GameFaqsBrowserBridge`'s image-embedding pass, which
 * tags headings in the same script pass), so jumping to it is an exact
 * `scrollIntoView()`-style WebView jump; for [GameGuideFormat.PlainText] it's a stringified
 * character offset into the guide's single page string (see
 * `domain/gameguides/PlainTextGuideTocParser`), used to compute an approximate scroll
 * fraction rather than an exact position, since the viewer renders a plain-text page as one
 * unchunked block of text with no addressable sub-position.
 *
 * [pageIndex] is which of a multi-page [GameGuideFormat.Html] guide's saved pages [anchorId]
 * lives on (a real in-line HTML guide is saved and shown page-by-page, one per chapter -
 * see `GameFaqsBrowserBridge.walkHtmlChapters` - not concatenated into one document), so
 * jumping to this entry means switching to that page first, then scrolling to [anchorId]
 * within it. Always 0 for [GameGuideFormat.PlainText], which only ever has one page.
 *
 * [depth] is this entry's nesting level (0 = top-level) within the table of contents -
 * always 0 for HTML (each heading tag is one flat entry) and for a plain-text guide with no
 * recognizable numbered structure, but can be 1 or 2 for a plain-text guide whose own printed
 * table of contents follows the common Roman-numeral/letter/number FAQ convention (see
 * `domain/gameguides/PlainTextGuideTocParser`'s hierarchical parsing path).
 */
data class GuideTocEntry(
    val title: String,
    val anchorId: String,
    val pageIndex: Int = 0,
    val depth: Int = 0,
)
