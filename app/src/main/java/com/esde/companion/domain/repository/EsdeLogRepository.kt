package com.esde.companion.domain.repository

import com.esde.companion.domain.model.EsdeEvent
import kotlinx.coroutines.flow.Flow

/**
 * Source of truth for ES-DE events, as a stream of already-parsed [EsdeEvent]s.
 *
 * Domain only depends on this interface. How the underlying log is actually watched
 * and read (polling, FileObserver, file path, encoding handling, etc.) is entirely a
 * data-layer implementation detail and must stay out of anything above this interface.
 */
interface EsdeLogRepository {
    fun observeEvents(): Flow<EsdeEvent>

    /**
     * Whether es_log.txt currently exists at the configured path. Emits an initial value
     * immediately, then again only when existence actually changes (e.g. ES-DE is
     * launched for the first time after the app started, or the configured folder turns
     * out to be wrong). Used by [ObserveConnectionStateUseCase] to distinguish "genuinely
     * idle" from "nothing to read yet."
     */
    fun observeLogFileExists(): Flow<Boolean>
}
