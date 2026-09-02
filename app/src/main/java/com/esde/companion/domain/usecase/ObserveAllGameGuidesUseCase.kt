package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.repository.GameGuideLibraryRepository
import kotlinx.coroutines.flow.Flow

/** Every downloaded guide across every game/system - see
 * [GameGuideLibraryRepository.observeAllGuides]. */
class ObserveAllGameGuidesUseCase(
    private val gameGuideLibraryRepository: GameGuideLibraryRepository,
) {
    operator fun invoke(): Flow<List<DownloadedGameGuide>> = gameGuideLibraryRepository.observeAllGuides()
}
