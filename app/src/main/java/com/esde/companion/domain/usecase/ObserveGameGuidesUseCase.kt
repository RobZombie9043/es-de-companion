package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.repository.GameGuideLibraryRepository
import kotlinx.coroutines.flow.Flow

/** Guides already downloaded for one game. Game-context resolution (which game is
 * "current") is the caller's job, same as `ResolveGameMediaUseCase` - see
 * `GameGuidesViewModel`. */
class ObserveGameGuidesUseCase(
    private val gameGuideLibraryRepository: GameGuideLibraryRepository,
) {
    operator fun invoke(gameReference: GameReference): Flow<List<DownloadedGameGuide>> =
        gameGuideLibraryRepository.observeGuidesFor(gameReference)
}
