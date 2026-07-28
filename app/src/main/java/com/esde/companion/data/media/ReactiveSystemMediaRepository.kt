package com.esde.companion.data.media

import com.esde.companion.domain.repository.SystemMediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Wraps an underlying [SystemMediaRepository] (by default [FileSystemMediaRepository])
 * built against whichever media folder is currently configured. Same pattern as
 * ReactiveGameMediaRepository - see its kdoc for the reasoning.
 */
class ReactiveSystemMediaRepository(
    private val mediaFolderPath: Flow<String?>,
    private val repositoryFactory: (String) -> SystemMediaRepository = { folder ->
        FileSystemMediaRepository(mediaFolderPath = folder)
    },
) : SystemMediaRepository {

    override suspend fun randomFanart(systemShortName: String): String? {
        val folder = mediaFolderPath.first() ?: return null
        return repositoryFactory(folder).randomFanart(systemShortName)
    }
}