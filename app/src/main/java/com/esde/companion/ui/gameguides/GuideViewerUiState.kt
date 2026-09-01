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
 * HTML via its tagged heading ids; for plain text,
 * [com.esde.companion.domain.gameguides.PlainTextGuideTocParser]'s entries are exact too,
 * once its own best-effort heading detection has actually located the right character
 * offset - [PlainTextGuideContent] resolves that offset against the text's real line layout,
 * not an approximated scroll fraction) and find-in-guide search from the toolbar. Scroll
 * position is reported back
 * debounced via `onScrollFractionChanged` so it isn't written on every frame of a fling.
 * Page-turn controls and search live in the header at the top; a tap anywhere on the guide
 * content toggles both the header and (for a multi-page guide) the page-nav footer out of
 * the way - see [GuideContentArea]/[GuideFooter].
 *
 * [scrollToCharOffsetRequest] carries a raw character offset (a table-of-contents jump, or a
 * "next match" jump), not a pre-computed scroll fraction - [PlainTextGuideContent] resolves it
 * against the text's own actual line layout, so it lands on the exact line rather than an
 * approximate one.
 */
internal class GuideViewerUiState(initialPageIndex: Int) {
    var showToc: Boolean by mutableStateOf(false)
    var showSearch: Boolean by mutableStateOf(false)
    var searchQuery: String by mutableStateOf("")
    var currentMatchIndex: Int by mutableIntStateOf(0)
    var htmlMatchTotal: Int by mutableIntStateOf(0)
    var htmlFindRequestId: Int by mutableIntStateOf(0)
    var scrollToCharOffsetRequest: Int? by mutableStateOf(null)
    var scrollToAnchorId: String? by mutableStateOf(null)

    // A TOC entry on a different page can't set scrollToAnchorId directly the way a same-page
    // entry does - see GameGuideViewerScreen.onEntrySelected's kdoc for why setting it in the
    // same synchronous step as currentPageIndex raced the page navigation it triggers. Held
    // here until GameGuideViewerScreen's own effect (which - unlike HtmlGuideContent - survives
    // the isLoadingContent-driven unmount/remount the navigation causes) confirms the new page's
    // content has actually arrived, then promotes it to scrollToAnchorId.
    var pendingAnchorId: String? by mutableStateOf(null)
    var currentPageIndex: Int by mutableIntStateOf(initialPageIndex)
    var chromeVisible: Boolean by mutableStateOf(true)
}
