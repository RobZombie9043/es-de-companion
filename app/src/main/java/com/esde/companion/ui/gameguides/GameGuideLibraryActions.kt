package com.esde.companion.ui.gameguides

import com.esde.companion.domain.model.DownloadedGameGuide

/** Bundles [GameGuideLibraryScreen]'s callbacks into one parameter, keeping the composable
 * under detekt's LongParameterList threshold - same reasoning as UISettingsContent's
 * DimAmountControl. */
data class GameGuideLibraryActions(
    val onOpenGuide: (DownloadedGameGuide) -> Unit,
    val onDeleteGuide: (DownloadedGameGuide) -> Unit,
    val onFindAnotherGuide: () -> Unit,
    val onClose: () -> Unit,
)
