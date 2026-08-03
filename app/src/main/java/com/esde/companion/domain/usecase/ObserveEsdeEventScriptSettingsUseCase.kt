package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.EsdeEventScriptSettings
import com.esde.companion.domain.repository.EsdeInstallationRepository
import kotlinx.coroutines.flow.Flow

class ObserveEsdeEventScriptSettingsUseCase(
    private val esdeInstallationRepository: EsdeInstallationRepository,
) {
    operator fun invoke(esdeRootPath: String): Flow<EsdeEventScriptSettings?> =
        esdeInstallationRepository.observeEventScriptSettings(esdeRootPath)
}
