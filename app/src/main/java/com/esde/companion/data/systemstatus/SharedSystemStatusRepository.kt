package com.esde.companion.data.systemstatus

import com.esde.companion.domain.model.SystemStatus
import com.esde.companion.domain.repository.SystemStatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

/**
 * Wraps an [AndroidSystemStatusRepository] so its battery/Wifi/Bluetooth reads are collected
 * exactly once regardless of subscriber count - both top corners could reference SystemStatus
 * at once (e.g. ClockAndSystemStatus in one, SystemStatus in the other). Same shareIn()
 * pattern as SharedEsdeLogRepository, for the same reason.
 */
class SharedSystemStatusRepository(
    private val inner: SystemStatusRepository,
    scope: CoroutineScope,
    replayExpiryMillis: Long = 5_000,
) : SystemStatusRepository {
    private val sharedStatus: Flow<SystemStatus> =
        inner.observeSystemStatus().shareIn(scope, SharingStarted.WhileSubscribed(replayExpiryMillis), replay = 1)

    override fun observeSystemStatus(): Flow<SystemStatus> = sharedStatus
}
