package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.EsdeInstallationRepository

class FindLegacyScriptFilesUseCase(
    private val esdeInstallationRepository: EsdeInstallationRepository,
) {
    suspend operator fun invoke(esdeRootPath: String): List<String> =
        esdeInstallationRepository.findLegacyScriptFiles(esdeRootPath)
}
