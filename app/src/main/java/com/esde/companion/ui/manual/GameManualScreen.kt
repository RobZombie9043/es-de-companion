package com.esde.companion.ui.manual

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.ui.pdf.PdfPageViewer
import com.esde.companion.ui.pdf.PdfPageViewerActions
import com.esde.companion.ui.pdf.PdfPageViewerState

private const val MANUAL_CONTENT_DESCRIPTION_LABEL = "Game manual"

/**
 * Opaque full-screen manual PDF viewer, drawn as a sibling of WidgetOverlay in
 * MainActivity whenever Settings > UI Settings > Game Playing Behavior is GameManual and
 * a manual was resolved for the current game - same "cover the plain main screen only"
 * placement as the existing Dim/Black behaviors. Reverts automatically when AppState
 * leaves PlayingGame; [onExit] additionally lets the user dismiss it early without
 * waiting for the game to end (see MainActivity's manualDismissed flag).
 *
 * A thin wrapper over [PdfPageViewer] (the pinch-zoom/paging/tap-to-reveal-controls chrome,
 * extracted here so GameGuidePdfViewerScreen can reuse it for an imported PDF guide) that
 * just adapts [viewModel]'s StateFlows/methods to that composable's plain state/actions
 * shape.
 */
@Composable
fun GameManualScreen(
    viewModel: GameManualViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentBitmap by viewModel.currentBitmap.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val pageCount by viewModel.pageCount.collectAsStateWithLifecycle()

    PdfPageViewer(
        state =
            PdfPageViewerState(
                currentBitmap = currentBitmap,
                currentPage = currentPage,
                pageCount = pageCount,
                contentDescriptionLabel = MANUAL_CONTENT_DESCRIPTION_LABEL,
            ),
        actions =
            PdfPageViewerActions(
                onWidthMeasured = viewModel::setTargetWidth,
                onNextPage = viewModel::nextPage,
                onPreviousPage = viewModel::previousPage,
                onExit = onExit,
            ),
        modifier = modifier,
    )
}
