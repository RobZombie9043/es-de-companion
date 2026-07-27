package com.esde.companion

import com.esde.companion.data.log.EsdeLogFileRepository
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.usecase.ObserveAppStateUseCase

/**
 * Minimal hand-rolled composition root. At this project's current scale (single module,
 * one developer) this is a deliberate, simpler alternative to a DI framework - see
 * CLAUDE.md. If/when the dependency graph grows enough to justify it, this can be
 * replaced with Hilt without changing anything below the ViewModel layer, since
 * everything here is already expressed as interfaces / constructor dependencies.
 */
class AppContainer {

    // TODO: hardcoded for this initial slice. A Settings screen will make this
    // user-configurable later - see CLAUDE.md, this is explicitly one of the
    // iterative pieces, not part of the base app.
    private val logFilePath = "/storage/emulated/0/ES-DE/logs/es_log.txt"

    private val logRepository: EsdeLogRepository = EsdeLogFileRepository(logFilePath)

    val observeAppStateUseCase = ObserveAppStateUseCase(logRepository)
}
