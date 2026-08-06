package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.repository.EsdeLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Emits once for every `Scripting::fireEvent(): quit` ES-DE logs - used by Settings >
 * Other Settings' "Close Companion App on ES-DE Quit" to close this app in reaction to
 * ES-DE shutting down. Filters the raw event stream directly rather than going through
 * [ObserveAppStateUseCase], since [com.esde.companion.domain.state.AppStateReducer] folds
 * Quit into AppState.Idle indistinguishably from Startup/Reload - AppState alone can't
 * tell "ES-DE just quit" apart from those other cases.
 */
class ObserveEsdeQuitEventUseCase(
    private val logRepository: EsdeLogRepository,
) {
    operator fun invoke(): Flow<Unit> =
        logRepository.observeEvents()
            .filter { it is EsdeEvent.Quit }
            .map { }
}
