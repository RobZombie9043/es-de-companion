package com.esde.companion.data.log

import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.repository.EsdeLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

/**
 * Wraps an [EsdeLogRepository] so observeEvents()/observeLogFileExists() are collected
 * exactly once regardless of how many downstream ViewModels subscribe. Both are cold on
 * the wrapped repository - confirmed via logcat that MainViewModel's connectionState and
 * WidgetsViewModel's contentIdentity, both tracing back to the same repository instance,
 * were each independently triggering a full separate run of the tailing logic (its own
 * FileObserver, its own fallback poll, its own file reads/parsing) rather than sharing
 * one. shareIn() makes this one upstream collection shared by all subscribers.
 */
class SharedEsdeLogRepository(
    private val inner: EsdeLogRepository,
    scope: CoroutineScope,
    replayExpiryMillis: Long = 5_000,
) : EsdeLogRepository {

    private val sharedEvents: Flow<EsdeEvent> =
        inner.observeEvents().shareIn(scope, SharingStarted.WhileSubscribed(replayExpiryMillis))

    private val sharedExists: Flow<Boolean> =
        inner.observeLogFileExists().shareIn(scope, SharingStarted.WhileSubscribed(replayExpiryMillis), replay = 1)

    override fun observeEvents(): Flow<EsdeEvent> = sharedEvents
    override fun observeLogFileExists(): Flow<Boolean> = sharedExists
}