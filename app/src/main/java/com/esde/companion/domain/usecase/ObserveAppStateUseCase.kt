package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.state.AppStateReducer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.scan

/**
 * Exposes the app's current [AppState] as a stream, by folding [AppStateReducer] over
 * the event stream from [EsdeLogRepository]. This is the single entry point the UI
 * layer should depend on for "what is ES-DE doing right now" - it should never talk
 * to the repository or the reducer directly.
 */
class ObserveAppStateUseCase(
    private val logRepository: EsdeLogRepository,
) {
    operator fun invoke(): Flow<AppState> =
        logRepository.observeEvents()
            .scan(AppState.Idle as AppState) { state, event -> AppStateReducer.reduce(state, event) }
}
