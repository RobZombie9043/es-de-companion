package com.esde.companion.data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Groups everything related to a directory watcher's self-healing behavior into one
 * parameter, rather than three separate ones, to keep the constructors of repositories that
 * watch removable-storage-backed directories within this project's `LongParameterList`
 * limit (see CLAUDE.md):
 *
 * - [storageEvents]: the storage mount/unmount signal (see [StorageMountEvents]) that can
 *   trigger an out-of-band recheck of a directory watcher that failed to arm against a
 *   not-yet-mounted parent directory.
 * - [onFallbackPollCaughtUpdate]: fired when the fallback poll catches a real change the
 *   directory watcher should have caught but didn't - direct proof the watch has gone stale.
 * - [onStorageMountEvent]: fired when a [storageEvents] emission is what triggered a rearm
 *   check, independent of whether anything was actually stale.
 */
class SelfHealConfig(
    val storageEvents: Flow<Unit> = emptyFlow(),
    val onFallbackPollCaughtUpdate: () -> Unit = {},
    val onStorageMountEvent: () -> Unit = {},
)
