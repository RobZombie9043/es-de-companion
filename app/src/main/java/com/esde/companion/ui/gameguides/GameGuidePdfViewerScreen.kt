package com.esde.companion.ui.gameguides

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.esde.companion.data.pdf.ManualRenderer
import com.esde.companion.data.pdf.PdfManualRenderer
import com.esde.companion.ui.pdf.PdfPageViewer
import com.esde.companion.ui.pdf.PdfPageViewerActions
import com.esde.companion.ui.pdf.PdfPageViewerState

/**
 * Renders a [com.esde.companion.domain.model.GameGuideFormat.Pdf] guide via the same
 * [PdfPageViewer] chrome GameManualScreen uses - opens its own [PdfManualRenderer] directly
 * (unlike GameManualScreen, there's no GameManualViewModel here to own it, since this guide
 * isn't necessarily the current game's manual) against [GameGuidesUiState.Viewing.contentFilePath],
 * seeded at [GameGuidesUiState.Viewing.initialPageIndex]. [onPageChanged] is called (skipping
 * the very first, resumed page) so the caller can persist reading progress the same way the
 * text viewer does.
 */
@Composable
fun GameGuidePdfViewerScreen(
    state: GameGuidesUiState.Viewing,
    onPageChanged: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var renderer by remember { mutableStateOf<ManualRenderer?>(null) }
    var currentPage by remember { mutableIntStateOf(state.initialPageIndex) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var targetWidthPx by remember { mutableIntStateOf(0) }

    val path = state.contentFilePath
    LaunchedEffect(path) {
        renderer?.close()
        currentBitmap = null
        pageCount = 0
        val opened = path?.let { PdfManualRenderer.open(it) }
        renderer = opened
        if (opened != null) {
            pageCount = opened.pageCount
            currentPage = state.initialPageIndex.coerceIn(0, (opened.pageCount - 1).coerceAtLeast(0))
        }
    }

    DisposableEffect(Unit) {
        onDispose { renderer?.close() }
    }

    LaunchedEffect(renderer, currentPage, targetWidthPx) {
        val activeRenderer = renderer ?: return@LaunchedEffect
        if (targetWidthPx <= 0) return@LaunchedEffect
        currentBitmap = activeRenderer.renderPage(currentPage, targetWidthPx)
    }

    // Skips the first composition's "change" (the resumed page), same reasoning as
    // GameGuideViewerContent's PersistPageIndexOnChange for the text viewer.
    var isFirstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(currentPage) {
        if (isFirstComposition) {
            isFirstComposition = false
        } else {
            onPageChanged(currentPage)
        }
    }

    val viewerState =
        PdfPageViewerState(
            currentBitmap = currentBitmap,
            currentPage = currentPage,
            pageCount = pageCount,
            contentDescriptionLabel = state.guide.title,
        )
    val viewerActions =
        PdfPageViewerActions(
            onWidthMeasured = { widthPx -> if (widthPx > 0) targetWidthPx = widthPx },
            onNextPage = { if (currentPage + 1 < pageCount) currentPage++ },
            onPreviousPage = { if (currentPage - 1 >= 0) currentPage-- },
            onExit = onClose,
        )
    PdfPageViewer(state = viewerState, actions = viewerActions, modifier = modifier)
}
