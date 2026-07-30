package com.esde.companion.data.media

import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.repository.SystemMediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ReactiveSystemMediaRepository(
    private val mediaFolderPath: Flow<String?>,
    private val repositoryFactory: (String) -> SystemMediaRepository = { folder ->
        FileSystemMediaRepository(mediaFolderPath = folder)
    },
) : SystemMediaRepository {

    override suspend fun randomMedia(systemShortName: String, mediaType: MediaType): String? {
        val folder = mediaFolderPath.first() ?: return null
        return repositoryFactory(folder).randomMedia(systemShortName, mediaType)
    }
}