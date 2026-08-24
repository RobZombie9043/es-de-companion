package com.esde.companion.data.gamelist

import com.esde.companion.domain.model.GameRating
import com.esde.companion.domain.repository.GameRatingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Wraps an underlying [GameRatingRepository] built against whichever ES-DE root folder is
 * currently configured - same reactive-to-Settings, keep-one-instance-alive shape as
 * ReactiveGameDescriptionRepository (see its kdoc), just for ratings.
 */
class ReactiveGameRatingRepository(
    private val esdeRootPath: Flow<String?>,
    private val repositoryFactory: (String) -> GameRatingRepository = { root ->
        FileGameRatingRepository(esdeRootPath = root)
    },
) : GameRatingRepository {
    @Volatile
    private var cached: Pair<String, GameRatingRepository>? = null

    override suspend fun resolveRating(
        systemShortName: String,
        romPath: String,
    ): GameRating {
        val root = esdeRootPath.first() ?: return GameRating(value = null)
        val repository =
            cached?.takeIf { it.first == root }?.second
                ?: repositoryFactory(root).also { cached = root to it }
        return repository.resolveRating(systemShortName, romPath)
    }
}
