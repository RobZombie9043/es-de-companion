package com.esde.companion.data.gamelist

import com.esde.companion.domain.model.GameDescription
import com.esde.companion.domain.repository.GameDescriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Wraps an underlying [GameDescriptionRepository] built against whichever ES-DE root
 * folder is currently configured - reactive-to-Settings, same idea as
 * ReactiveGameMediaRepository, but keyed off the log folder path (the ES-DE root) rather
 * than the media folder, since gamelists/ lives alongside logs/ under that same root.
 *
 * Unlike ReactiveGameMediaRepository, this deliberately keeps one [GameDescriptionRepository]
 * instance alive across calls (rebuilt only when the root path actually changes) rather
 * than constructing a fresh one per call - FileGameDescriptionRepository's per-system
 * content cache only has value if it survives between game switches.
 */
class ReactiveGameDescriptionRepository(
    private val esdeRootPath: Flow<String?>,
    private val repositoryFactory: (String) -> GameDescriptionRepository = { root ->
        FileGameDescriptionRepository(esdeRootPath = root)
    },
) : GameDescriptionRepository {
    @Volatile
    private var cached: Pair<String, GameDescriptionRepository>? = null

    override suspend fun resolveDescription(
        systemShortName: String,
        romPath: String,
    ): GameDescription {
        val root = esdeRootPath.first() ?: return GameDescription(text = null)
        val repository =
            cached?.takeIf { it.first == root }?.second
                ?: repositoryFactory(root).also { cached = root to it }
        return repository.resolveDescription(systemShortName, romPath)
    }
}
