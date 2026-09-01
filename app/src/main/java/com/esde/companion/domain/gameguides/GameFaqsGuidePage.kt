package com.esde.companion.domain.gameguides

import com.esde.companion.domain.model.GameGuideFormat
import com.esde.companion.domain.model.GuideTocEntry

/**
 * What [GameFaqsBrowserBridge]'s page-detection JS extracted from a WebView's currently loaded
 * page. [isGuidePage] false means "this doesn't look like a guide page" - the browser screen's
 * Save action stays hidden rather than saving garbage. [format] reflects which page template
 * matched, and therefore whether [pages] holds extracted plain text or raw HTML markup.
 * [pages] holds one entry per page already combined - always at least one entry when
 * [isGuidePage] is true. [tocEntries] is only ever populated for [GameGuideFormat.Html], and
 * only when the guide actually has a GameFAQs `.ftoc` block to read (see
 * `GameFaqsBrowserBridge.walkHtmlChapters`'s `buildTocEntries` - empty otherwise, in which case
 * `GuidePageContentProcessor`'s own per-page heading-tagging fallback fills the guide's table of
 * contents in instead). A plain Kotlin type (no WebView dependency) so any future cleanup of
 * extracted content stays pure and unit-testable against fixture strings.
 */
data class GameFaqsGuidePage(
    val isGuidePage: Boolean,
    val title: String,
    val format: GameGuideFormat,
    val pages: List<String>,
    val tocEntries: List<GuideTocEntry> = emptyList(),
)
