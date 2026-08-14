package com.esde.companion.data.storage

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Hot signal that removable storage may have changed mount state - fed by
 * [StorageMountReceiver], consumed by `EsdeLogFileRepository`/`FileEsdeInstallationRepository`
 * to trigger an immediate re-check and rearm their directory watchers, rather than waiting
 * out the several-second fallback poll interval.
 *
 * Carries no payload - every emission means "re-evaluate, don't trust the specific action."
 * Mount and unmount get an identical response (try to rearm the watcher; that correctly
 * no-ops if the directory turns out not to exist), so there's nothing a payload would add.
 *
 * `replay = 1` so a repository instance created or switched (e.g. via
 * `ReactiveEsdeLogRepository` reacting to a folder-path change) shortly after a mount event
 * still gets one recheck even if its collector subscribes just after the live emission -
 * harmless if redundant with the watcher's own on-start check.
 */
class StorageMountEvents {
    private val _events =
        MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun notifyChanged() {
        _events.tryEmit(Unit)
    }
}
