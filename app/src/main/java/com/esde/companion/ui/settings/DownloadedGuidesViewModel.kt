package com.esde.companion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.usecase.DeleteGameGuideUseCase
import com.esde.companion.domain.usecase.ObserveAllGameGuidesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STATE_STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Backs Settings > Game Guides > "Browse Downloaded Guides" - every downloaded guide across
 * every game/system, grouped for display (system -> game -> guide) by the screens themselves,
 * since [allGuides] is the only state that side needs. Opening a guide to read, or opening the
 * browser for Add Guide, goes straight through the FAB's own
 * [com.esde.companion.ui.gameguides.GameGuidesViewModel]/
 * [com.esde.companion.ui.gameguides.GameGuidesOverlayState] instead of being duplicated here -
 * see `LongPressSettingsMenu`'s wiring - so Settings shows the exact same full-screen
 * viewer/browser the FAB does, not a second, popup-sized copy of it.
 */
class DownloadedGuidesViewModel(
    observeAllGameGuides: ObserveAllGameGuidesUseCase,
    private val deleteGameGuide: DeleteGameGuideUseCase,
) : ViewModel() {
    val allGuides: StateFlow<List<DownloadedGameGuide>> =
        observeAllGameGuides()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STATE_STOP_TIMEOUT_MILLIS), emptyList())

    fun onDeleteGuide(guideId: String) {
        viewModelScope.launch { deleteGameGuide(guideId) }
    }
}
