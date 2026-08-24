package com.esde.companion.data.systemstatus

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Hot signal that BLUETOOTH_CONNECT may have just been granted or revoked - there's no OS
 * broadcast for "a runtime permission changed while the app was backgrounded", so
 * SystemStatusFabContent's ON_RESUME check calls [notifyChanged] to make
 * AndroidSystemStatusRepository re-evaluate. Same shape as StorageMountEvents, for the same
 * "any number of coalesced triggers should still only cause one fresh re-check" reason.
 */
class BluetoothPermissionRecheckSignal {
    private val _events =
        MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun notifyChanged() {
        _events.tryEmit(Unit)
    }
}
