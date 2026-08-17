package com.esde.companion.data.systems

import com.esde.companion.domain.repository.SystemPathRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Wraps an underlying [SystemPathRepository] built against whichever ES-DE root folder
 * is currently configured - same reactive-to-Settings pattern as
 * [com.esde.companion.data.gamelist.ReactiveGameDescriptionRepository], keyed off the
 * log folder path (the ES-DE root) since `custom_systems/` lives alongside `logs/`
 * under that same root. Keeps one [SystemPathRepository] instance alive across calls
 * (rebuilt only when the root path actually changes) rather than constructing a fresh
 * one per call, so [FileSystemPathRepository]'s file-content cache survives between
 * lookups.
 */
class ReactiveSystemPathRepository(
    private val esdeRootPath: Flow<String?>,
    private val repositoryFactory: (String) -> SystemPathRepository = { root ->
        FileSystemPathRepository(esdeRootPath = root)
    },
) : SystemPathRepository {
    @Volatile
    private var cached: Pair<String, SystemPathRepository>? = null

    override suspend fun resolveSystemPath(systemShortName: String): String? {
        val root = esdeRootPath.first() ?: return null
        val repository =
            cached?.takeIf { it.first == root }?.second
                ?: repositoryFactory(root).also { cached = root to it }
        return repository.resolveSystemPath(systemShortName)
    }
}
