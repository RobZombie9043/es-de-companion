package com.esde.companion.data.gamelist

import android.content.Context
import com.esde.companion.data.apps.AppLauncher
import com.esde.companion.data.apps.CompanionDisplayHolder
import com.esde.companion.data.apps.SecondaryDisplayResolver
import com.esde.companion.data.debug.DebugFileLogger
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.GameLaunchDisplayTarget
import com.esde.companion.domain.model.GameLaunchOverride
import com.esde.companion.domain.model.resolveGameLaunchPackage
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
import com.esde.companion.domain.usecase.ObserveGameLaunchDisplayTargetUseCase
import com.esde.companion.domain.usecase.ObserveGameLaunchOverridesUseCase
import com.esde.companion.domain.usecase.ObserveGameLaunchSystemDefaultsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * App-scoped reactive piece for Game Launch Override: whenever the shared [ObserveAppStateUseCase]
 * stream emits [AppState.PlayingGame], resolves the configured launch app (see
 * [resolveGameLaunchPackage]) and, if one is configured, launches it on the display selected by
 * the global [GameLaunchDisplayTarget] setting (Settings > UI Settings > Game Launch Override) -
 * Companion's own screen by default, or the other screen (ES-DE/the game's own display) via
 * [SecondaryDisplayResolver]. No enable/disable gate is needed - an unconfigured system/game
 * already resolves to null (nothing launches), the same implicit-off shape `FabType.None` uses,
 * so this is unconditionally safe to keep running.
 *
 * Modeled on [com.esde.companion.data.thor.AutoFpsCoordinator]'s shape: independent collectors
 * keep the latest system-defaults/game-overrides/display-target snapshots in `@Volatile` fields,
 * and one more reacts to the actual [AppState] stream. No `distinctUntilChanged` is needed on the
 * `AppState` collection - it's a `StateFlow`, which already conflates truly-consecutive-identical
 * emissions, and any real replay of the same game has a non-[AppState.PlayingGame] state in
 * between (quitting to a menu, browsing away), so it naturally re-fires on a genuine restart.
 */
class GameLaunchOverrideCoordinator(
    private val observeAppState: ObserveAppStateUseCase,
    private val observeGameLaunchSystemDefaults: ObserveGameLaunchSystemDefaultsUseCase,
    private val observeGameLaunchOverrides: ObserveGameLaunchOverridesUseCase,
    private val observeGameLaunchDisplayTarget: ObserveGameLaunchDisplayTargetUseCase,
    private val companionDisplayHolder: CompanionDisplayHolder,
    private val debugFileLogger: DebugFileLogger,
) {
    @Volatile
    private var systemDefaults: Map<String, String> = emptyMap()

    @Volatile
    private var gameOverrides: List<GameLaunchOverride> = emptyList()

    @Volatile
    private var displayTarget: GameLaunchDisplayTarget = GameLaunchDisplayTarget.ThisScreen

    fun start(
        context: Context,
        applicationScope: CoroutineScope,
    ) {
        val appContext = context.applicationContext

        applicationScope.launch {
            observeGameLaunchSystemDefaults().collect { systemDefaults = it }
        }
        applicationScope.launch {
            observeGameLaunchOverrides().collect { gameOverrides = it }
        }
        applicationScope.launch {
            observeGameLaunchDisplayTarget().collect { displayTarget = it }
        }
        applicationScope.launch {
            observeAppState().collect { state ->
                if (state is AppState.PlayingGame) onGameStarted(appContext, state)
            }
        }
    }

    private fun onGameStarted(
        context: Context,
        state: AppState.PlayingGame,
    ) {
        val packageName =
            resolveGameLaunchPackage(
                systemShortName = state.systemShortName,
                romPath = state.romPath,
                systemDefaults = systemDefaults,
                gameOverrides = gameOverrides,
            ) ?: return

        val displayId =
            when (displayTarget) {
                // context is an application Context (this coordinator runs in application
                // scope), so it isn't tied to any display - omitting a display option
                // entirely does NOT mean "wherever Companion is running," it means Android
                // picks one by its own heuristics (observed on-device: intermittently
                // landing on the other screen). companionDisplayHolder.displayId is the
                // only reliable source for "Companion's own screen" from here.
                GameLaunchDisplayTarget.ThisScreen -> companionDisplayHolder.displayId
                GameLaunchDisplayTarget.OtherScreen ->
                    SecondaryDisplayResolver.secondaryDisplayId(context, companionDisplayHolder.displayId)
            }
        AppLauncher.launch(context, packageName, displayId)
        debugFileLogger.logInfo(LOG_TAG, "Launched $packageName for ${state.gameName} (${state.systemShortName})")
    }

    private companion object {
        const val LOG_TAG = "GameLaunchOverride"
    }
}
