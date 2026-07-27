package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.repository.EsdeLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The single entry point the UI layer should depend on for "what should the main screen
 * show right now" - supersedes observing [ObserveAppStateUseCase] directly, since the UI
 * also needs to distinguish "log file missing" from any particular AppState.
 *
 * Combines the file-existence signal with the reduced AppState stream. When the file is
 * absent, AppState's own value is irrelevant to the UI (there's nothing to display), so
 * this deliberately discards it rather than trying to merge "not found" into AppState.
 */
class ObserveConnectionStateUseCase(
    private val logRepository: EsdeLogRepository,
    private val observeAppState: ObserveAppStateUseCase,
) {
    operator fun invoke(): Flow<EsdeConnectionState> =
        combine(
            logRepository.observeLogFileExists(),
            observeAppState(),
        ) { fileExists, appState ->
            if (fileExists) {
                EsdeConnectionState.Connected(appState)
            } else {
                EsdeConnectionState.LogFileNotFound
            }
        }
}