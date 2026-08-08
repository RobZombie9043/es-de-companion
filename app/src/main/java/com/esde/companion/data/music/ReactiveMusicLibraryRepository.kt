package com.esde.companion.data.music

import com.esde.companion.domain.model.MusicPool
import com.esde.companion.domain.repository.MusicLibraryRepository
import com.esde.companion.domain.repository.MusicPoolContents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Rebuilds a fresh [FileMusicLibraryRepository] per call so a Settings change to the
 * custom music folder takes effect without restart - same shape as
 * [com.esde.companion.data.media.ReactiveSystemMediaRepository].
 */
class ReactiveMusicLibraryRepository(
    private val musicFolderPath: Flow<String?>,
    private val repositoryFactory: (String) -> MusicLibraryRepository = { folder ->
        FileMusicLibraryRepository(musicFolderPath = folder)
    },
) : MusicLibraryRepository {
    override suspend fun loadPool(desired: MusicPool): MusicPoolContents {
        val folder = musicFolderPath.first() ?: return MusicPoolContents(desired, emptyList())
        return repositoryFactory(folder).loadPool(desired)
    }
}
