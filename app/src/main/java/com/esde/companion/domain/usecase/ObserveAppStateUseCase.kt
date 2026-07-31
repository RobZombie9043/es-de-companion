package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.state.AppStateReducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

/**
 * Exposes the app's current [AppState] as a stream, by folding [AppStateReducer] over
 * the event stream from [EsdeLogRepository]. This is the single entry point the UI
 * layer should depend on for "what is ES-DE doing right now" - it should never talk
 * to the repository or the reducer directly.
 *
 * The scan/fold is computed exactly once, at construction, and shared via [stateIn] -
 * NOT rebuilt fresh on every [invoke] call. This matters: [EsdeLogRepository]'s event
 * stream is itself shared (see SharedEsdeLogRepository) but has no replay, so a caller
 * whose own independent scan gets torn down and restarted (e.g. MainViewModel's
 * connectionState after WhileSubscribed(5_000) expires while Settings or Edit Widgets is
 * showing) would rejoin the already-warm shared event stream with nothing to fold from,
 * and sit at Idle until the next live event happened to arrive - regardless of what the
 * real current state actually was. Sharing the derived AppState itself here, with a
 * StateFlow (which always replays its latest value to new subscribers), means every
 * caller sees the same correctly-reconstructed state immediately, not just whichever
 * caller happened to be first.
 */
class ObserveAppStateUseCase(
    logRepository: EsdeLogRepository,
    scope: CoroutineScope,
) {
    private val sharedAppState: StateFlow<AppState> =
        logRepository.observeEvents()
            .scan(AppState.Idle as AppState) { state, event -> AppStateReducer.reduce(state, event) }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = AppState.Idle,
            )

    operator fun invoke(): Flow<AppState> = sharedAppState
}