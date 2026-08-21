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
 * [com.esde.companion.domain.model.resolveAchievementsSystem]) over ES-DE's live [AppState]
 * and the "Update on Screensaver" setting via [Flow.scan] - shared by
 * [com.esde.companion.ui.retroachievements.RetroAchievementsViewModel] and
 * [com.esde.companion.ui.retroachievements.RetroAchievementsSystemGamesViewModel], which
 * previously duplicated this combine/scan construction with only the resolver function
 * differing. Construct one instance per resolved shape [T], passing the matching resolver.
 */
class ObserveScreensaverAwareContextUseCase<T>(
    private val observeConnectionState: ObserveConnectionStateUseCase,
    private val observeUpdateAchievementsOnScreensaverEnabled: ObserveUpdateAchievementsOnScreensaverEnabledUseCase,
    private val resolve: (AppState?, T?, Boolean) -> T?,
) {
    operator fun invoke(): Flow<T?> =
        combine(
            observeConnectionState().map { connection -> (connection as? EsdeConnectionState.Connected)?.appState },
            observeUpdateAchievementsOnScreensaverEnabled(),
        ) { appState, updateOnScreensaver -> appState to updateOnScreensaver }
            .scan(null as T?) { previous, (appState, updateOnScreensaver) ->
                resolve(appState, previous, updateOnScreensaver)
            }
}
