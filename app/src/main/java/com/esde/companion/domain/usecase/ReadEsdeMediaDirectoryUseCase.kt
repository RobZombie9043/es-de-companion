package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.EsdeInstallationRepository

class ReadEsdeMediaDirectoryUseCase(
    private val esdeInstallationRepository: EsdeInstallationRepository,
) {
    suspend operator fun invoke(esdeRootPath: String): String? =
        esdeInstallationRepository.readMediaDirectory(esdeRootPath)
}
