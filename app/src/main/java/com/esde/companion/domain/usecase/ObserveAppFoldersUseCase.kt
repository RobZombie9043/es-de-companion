package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AppFolder
import com.esde.companion.domain.repository.AppFolderRepository
import kotlinx.coroutines.flow.Flow

class ObserveAppFoldersUseCase(
    private val appFolderRepository: AppFolderRepository,
) {
    operator fun invoke(): Flow<List<AppFolder>> = appFolderRepository.observeFolders()
}
