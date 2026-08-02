package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.EsdeInstallationRepository

class DeleteLegacyScriptFilesUseCase(
    private val esdeInstallationRepository: EsdeInstallationRepository,
) {
    suspend operator fun invoke(esdeRootPath: String) =
        esdeInstallationRepository.deleteLegacyScriptFiles(esdeRootPath)
}
