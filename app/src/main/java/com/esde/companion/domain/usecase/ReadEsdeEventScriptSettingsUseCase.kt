package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.EsdeEventScriptSettings
import com.esde.companion.domain.repository.EsdeInstallationRepository

class ReadEsdeEventScriptSettingsUseCase(
    private val esdeInstallationRepository: EsdeInstallationRepository,
) {
    suspend operator fun invoke(esdeRootPath: String): EsdeEventScriptSettings? =
        esdeInstallationRepository.readEventScriptSettings(esdeRootPath)
}
