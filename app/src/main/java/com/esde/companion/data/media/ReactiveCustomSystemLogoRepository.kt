package com.esde.companion.data.media

import com.esde.companion.domain.repository.CustomSystemLogoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ReactiveCustomSystemLogoRepository(
    private val folderPath: Flow<String?>,
    private val repositoryFactory: (String) -> CustomSystemLogoRepository = { folder ->
        FileCustomSystemLogoRepository(folderPath = folder)
    },
) : CustomSystemLogoRepository {

    override suspend fun findLogo(systemShortName: String): String? {
        val folder = folderPath.first() ?: return null
        return repositoryFactory(folder).findLogo(systemShortName)
    }
}