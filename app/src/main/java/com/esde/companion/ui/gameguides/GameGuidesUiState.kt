package com.esde.companion.ui.gameguides

import com.esde.companion.domain.gameguides.GuideDownloadProgress
import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.GameGuideDisplayPreferences
import com.esde.companion.domain.model.GameReference

/**
 * What the Game Guides overlay currently shows - [Library] always, whether or not any guides
 * are downloaded yet for the current game (its own empty-state note covers zero guides -
 * reaching [Browsing] is only ever a deliberate choice, via the "+" dropdown's GameFAQs item,
 * see [GameGuidesViewModel.openBrowser]). [Viewing] a saved guide is reached from [Library],
 * not nested inside it, so the viewer can be full-screen on its own.
 */
sealed interface GameGuidesUiState {
    data object NoGame : GameGuidesUiState

    data class Library(
        val gameReference: GameReference,
        val gameName: String,
        val guides: List<DownloadedGameGuide>,
        val readingProgressByGuideId: Map<String, Float> = emptyMap(),
        // Resolved via ResolveGameMediaUseCase (MediaType.Manuals), independent of the live
        // ES-DE current game - see GameGuidesViewModel's kdoc for why this can't just reuse
        // GameManualViewModel.pdfPath. Null means no manual exists for this game, driving the
        // Game Manual section's own empty-state note.
        val manualPdfPath: String? = null,
    ) : GameGuidesUiState

    data class Browsing(
        val gameReference: GameReference,
        val gameName: String,
        val searchUrl: String,
        val currentPageIsGuide: Boolean,
        // null when not saving - non-null while GameGuidesViewModel.saveCurrentGuide is
        // running, driving GameGuidesBrowserScreen's download progress dialog.
        val downloadProgress: GuideDownloadProgress? = null,
    ) : GameGuidesUiState {
        val isSaving: Boolean get() = downloadProgress != null
    }

    data class Viewing(
        val guide: DownloadedGameGuide,
        // Populated only for GameGuideFormat.PlainText/Html - a text guide's saved pages.
        // Empty (and meaningless) for Pdf/Image, which populate contentFilePath instead - see
        // GameGuidesViewModel.loadedViewingStateFor's format branch.
        val pages: List<String> = emptyList(),
        val displayPreferences: GameGuideDisplayPreferences,
        val initialScrollFraction: Float,
        val initialPageIndex: Int = 0,
        // Populated only for GameGuideFormat.Pdf/Image - the on-disk path to the guide's own
        // imported binary file (see GameGuideLibraryRepository.binaryContentPath). Null for
        // PlainText/Html, which use pages instead.
        val contentFilePath: String? = null,
        // True for the brief window between the viewer appearing (header/chrome, matching the
        // HTML viewer's own "show immediately, render when ready" feel) and its actual page
        // content finishing its (potentially slow, disk-bound) load - see
        // GameGuidesViewModel.openGuide's kdoc.
        val isLoadingContent: Boolean = false,
    ) : GameGuidesUiState
}
