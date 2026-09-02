package com.esde.companion.ui.gameguides

import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.GameGuideFormat

/** Bundles [GameGuideLibraryScreen]'s callbacks into one parameter, keeping the composable
 * under detekt's LongParameterList threshold - same reasoning as UISettingsContent's
 * DimAmountControl. [onOpenManual] is null when opening the current game's manual from here
 * isn't safe (a Library opened directly for an explicitly-picked game rather than ES-DE's
 * live current one - see GameGuidesOverlayState.openedDirectly) - the Game Manual row
 * disables itself in that case rather than risk opening the wrong game's manual. */
data class GameGuideLibraryActions(
    val onOpenGuide: (DownloadedGameGuide) -> Unit,
    val onDeleteGuide: (DownloadedGameGuide) -> Unit,
    val onBrowseGameFaqs: () -> Unit,
    val onImportGuide: (bytes: ByteArray, fileName: String, format: GameGuideFormat) -> Unit,
    val onOpenManual: (() -> Unit)?,
    val onClose: () -> Unit,
)
