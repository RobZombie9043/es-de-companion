package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

/**
 * Folds a screensaver-freeze resolver (e.g.
 * [com.esde.companion.domain.model.resolveAchievementsGame]/
 * [com.esde.companion.domain.model.resolveAchievementsSystem]) over ES-DE's live [AppState],
 * the "Update on Screensaver" setting, and whether the caller's own screen is actually visible
 * right now, via [Flow.scan] - shared by
 * [com.esde.companion.ui.retroachievements.RetroAchievementsViewModel] and
 * [com.esde.companion.ui.retroachievements.RetroAchievementsSystemGamesViewModel], which
 * previously duplicated this combine/scan construction with only the resolver function
 * differing. Construct one instance per resolved shape [T], passing the matching resolver.
 *
 * [observeVisible] exists because the freeze-during-screensaver behavior only makes sense while
 * a person is actually looking at the page - see the resolver kdocs for why holding [T] with
 * nobody watching just leaves a stale value to surface, unfrozen, the next time the page opens.
 */
class ObserveScreensaverAwareContextUseCase<T>(
    private val observeConnectionState: ObserveConnectionStateUseCase,
    private val observeUpdateAchievementsOnScreensaverEnabled: ObserveUpdateAchievementsOnScreensaverEnabledUseCase,
    private val observeVisible: () -> Flow<Boolean>,
    private val resolve: (AppState?, T?, Boolean, Boolean) -> T?,
) {
    operator fun invoke(): Flow<T?> =
        combine(
            observeConnectionState().map { connection -> (connection as? EsdeConnectionState.Connected)?.appState },
            observeUpdateAchievementsOnScreensaverEnabled(),
            observeVisible(),
        ) { appState, updateOnScreensaver, visible -> Triple(appState, updateOnScreensaver, visible) }
            .scan(null as T?) { previous, (appState, updateOnScreensaver, visible) ->
                resolve(appState, previous, updateOnScreensaver, visible)
            }
}
