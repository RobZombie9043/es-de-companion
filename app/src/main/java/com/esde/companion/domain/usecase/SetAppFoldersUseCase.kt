package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AppFolder
import com.esde.companion.domain.repository.AppFolderRepository

class SetAppFoldersUseCase(
    private val appFolderRepository: AppFolderRepository,
) {
    suspend operator fun invoke(folders: List<AppFolder>) = appFolderRepository.setFolders(folders)
}
