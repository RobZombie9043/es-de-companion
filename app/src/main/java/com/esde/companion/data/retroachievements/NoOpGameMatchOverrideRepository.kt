package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.GameMatchOverride
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.repository.GameMatchOverrideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Placeholder [GameMatchOverrideRepository] with no real persistence - lets
 * [com.esde.companion.domain.usecase.ResolveRetroAchievementsGameUseCase] be wired into
 * [com.esde.companion.AppContainer] ahead of the manual correction UI/persistence work
 * (Phase 1 PR 6), the same stub-then-real sequencing already used for
 * [RetroAchievementsRepositoryImpl] between PR 1 and PR 3. Overrides set via
 * [setOverride] only last for the current process - nothing reachable in the UI yet calls
 * it, since the search/picker screen that would is PR 6's job.
 */
class NoOpGameMatchOverrideRepository : GameMatchOverrideRepository {
    private val overrides = MutableStateFlow<List<GameMatchOverride>>(emptyList())

    override suspend fun setOverride(override: GameMatchOverride) {
        overrides.value = overrides.value.filterNot { it.matches(override) } + override
    }

    override suspend fun clearOverride(gameReference: GameReference) {
        overrides.value = overrides.value.filterNot { it.matchesReference(gameReference) }
    }

    override suspend fun getOverride(gameReference: GameReference): GameMatchOverride? {
        return overrides.value.firstOrNull { it.matchesReference(gameReference) }
    }

    override fun observeAllOverrides(): StateFlow<List<GameMatchOverride>> = overrides

    private fun GameMatchOverride.matches(other: GameMatchOverride): Boolean {
        return systemShortName == other.systemShortName && romPath == other.romPath
    }

    private fun GameMatchOverride.matchesReference(reference: GameReference) =
        systemShortName == reference.systemShortName && romPath == reference.romPath
}
