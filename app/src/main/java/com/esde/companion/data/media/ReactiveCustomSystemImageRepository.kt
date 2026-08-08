package com.esde.companion.data.media

import com.esde.companion.domain.repository.CustomSystemImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ReactiveCustomSystemImageRepository(
    private val folderPath: Flow<String?>,
    private val repositoryFactory: (String) -> CustomSystemImageRepository = { folder ->
        FileCustomSystemImageRepository(folderPath = folder)
    },
) : CustomSystemImageRepository {
    override suspend fun findImage(systemShortName: String): String? {
        val folder = folderPath.first() ?: return null
        return repositoryFactory(folder).findImage(systemShortName)
    }
}
