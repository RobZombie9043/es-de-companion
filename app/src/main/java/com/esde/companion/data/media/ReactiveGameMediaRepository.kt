package com.esde.companion.data.media

import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.repository.GameMediaRepository
import com.esde.companion.domain.repository.SystemPathRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Wraps an underlying [GameMediaRepository] (by default [FileGameMediaRepository]) built
 * against whichever media folder is currently configured, mirroring how
 * ReactiveEsdeLogRepository handles the log folder changing via Settings without a
 * restart. Reads [mediaFolderPath] fresh on every call rather than caching a single
 * instance, since media resolution is a one-shot suspend call rather than a Flow.
 *
 * [systemPathRepository] is already itself reactive-to-Settings (see
 * ReactiveSystemPathRepository) and is passed straight through to every constructed
 * [FileGameMediaRepository] rather than rebuilt per call, so its own file-content cache
 * survives across calls despite [FileGameMediaRepository] itself being reconstructed
 * fresh each time.
 *
 * A null path (onboarding not completed, nothing configured) resolves to an empty
 * [GameMedia] rather than pointing at a bogus path.
 *
 * [repositoryFactory] is injected purely so this class's behavior is unit-testable
 * with a fake, with no Android dependency required.
 */
class ReactiveGameMediaRepository(
    private val mediaFolderPath: Flow<String?>,
    private val systemPathRepository: SystemPathRepository,
    private val repositoryFactory: (String, SystemPathRepository) -> GameMediaRepository = { folder, systemPaths ->
        FileGameMediaRepository(mediaFolderPath = folder, systemPathRepository = systemPaths)
    },
) : GameMediaRepository {
    override suspend fun resolveMedia(
        systemShortName: String,
        systemPath: String?,
        romPath: String,
        mediaTypes: Set<MediaType>,
    ): GameMedia {
        val folder = mediaFolderPath.first() ?: return GameMedia(baseRelativePath = null, filesByType = emptyMap())
        val repository = repositoryFactory(folder, systemPathRepository)
        return repository.resolveMedia(systemShortName, systemPath, romPath, mediaTypes)
    }
}
