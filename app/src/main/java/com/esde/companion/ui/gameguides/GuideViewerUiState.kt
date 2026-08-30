package com.esde.companion.ui.gameguides

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Renders one downloaded guide: [com.esde.companion.domain.model.GameGuideFormat.PlainText]
 * guides in a scrollable, font-scaled text list (see [PlainTextGuideContent]);
 * [com.esde.companion.domain.model.GameGuideFormat.Html] guides in a sandboxed offline
 * WebView (see [HtmlGuideContent]). Both share a table of contents (tap to jump - exact for
 * HTML via its tagged heading ids, approximate for plain text via
 * [com.esde.companion.domain.gameguides.PlainTextGuideTocParser]'s character-offset-based
 * entries) and find-in-guide search from the toolbar. Scroll position is reported back
 * debounced via `onScrollFractionChanged` so it isn't written on every frame of a fling.
 * Page-turn controls and search live in the header at the top; a tap anywhere on the guide
 * content toggles both the header and (for a multi-page guide) the page-nav footer out of
 * the way - see [GuideContentArea]/[GuideFooter].
 */
internal class GuideViewerUiState(initialPageIndex: Int) {
    var showToc: Boolean by mutableStateOf(false)
    var showSearch: Boolean by mutableStateOf(false)
    var searchQuery: String by mutableStateOf("")
    var currentMatchIndex: Int by mutableIntStateOf(0)
    var htmlMatchTotal: Int by mutableIntStateOf(0)
    var htmlFindRequestId: Int by mutableIntStateOf(0)
    var scrollToFractionRequest: Float? by mutableStateOf(null)
    var scrollToAnchorId: String? by mutableStateOf(null)
    var currentPageIndex: Int by mutableIntStateOf(initialPageIndex)
    var chromeVisible: Boolean by mutableStateOf(true)
}
