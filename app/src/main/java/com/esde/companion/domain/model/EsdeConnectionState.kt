package com.esde.companion.domain.model

/**
 * Wraps [AppState] with whether es_log.txt is currently readable at all.
 *
 * Kept separate from AppState deliberately - AppState's only job is "event stream in,
 * correct state out" (see CLAUDE.md), and it must not know anything about file paths
 * or file existence. This type sits one level above it: [ObserveConnectionStateUseCase]
 * combines AppState with a file-existence signal from the repository, and the UI
 * observes this type rather than AppState directly.
 */
sealed class EsdeConnectionState {

    /** es_log.txt does not currently exist at the configured path. */
    data object LogFileNotFound : EsdeConnectionState()

    data class Connected(val appState: AppState) : EsdeConnectionState()
}