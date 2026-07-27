package com.esde.companion.data.log

import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.repository.EsdeLogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Wraps an underlying [EsdeLogRepository] (by default [EsdeLogFileRepository]) and swaps
 * to a fresh instance whenever the configured ES-DE root folder changes - e.g. via a
 * Settings screen - without requiring the app to restart.
 *
 * [logFolderPath] emits the ES-DE *root* folder the user picked (e.g.
 * "/storage/emulated/0/ES-DE"), not the log file path itself. This class is what derives
 * "<root>/logs/es_log.txt" for the underlying repository, so callers above this (domain,
 * onboarding) only ever deal with the folder the user actually picked, not the specific
 * log file layout inside it.
 *
 * A null path (onboarding not completed yet, nothing configured) produces an empty events
 * stream and a constant `false` existence stream rather than pointing at a bogus path.
 *
 * [repositoryFactory] is injected (rather than hardcoding `EsdeLogFileRepository(...)`
 * inline) purely so this class's switching behavior can be unit tested with a fake, with
 * no Android dependency required.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReactiveEsdeLogRepository(
    private val logFolderPath: Flow<String?>,
    private val repositoryFactory: (String) -> EsdeLogRepository = { folderPath ->
        EsdeLogFileRepository(logFilePath = "$folderPath/logs/es_log.txt")
    },
) : EsdeLogRepository {

    override fun observeEvents(): Flow<EsdeEvent> =
        logFolderPath.flatMapLatest { folder ->
            folder?.let { repositoryFactory(it).observeEvents() } ?: emptyFlow()
        }

    override fun observeLogFileExists(): Flow<Boolean> =
        logFolderPath.flatMapLatest { folder ->
            folder?.let { repositoryFactory(it).observeLogFileExists() } ?: flowOf(false)
        }
}