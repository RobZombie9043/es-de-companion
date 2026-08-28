package com.esde.companion.data.gamelist

import android.content.Context
import com.esde.companion.data.apps.AppLauncher
import com.esde.companion.data.apps.CompanionDisplayHolder
import com.esde.companion.data.apps.SecondaryDisplayResolver
import com.esde.companion.data.debug.DebugFileLogger
import com.esde.companion.data.thor.TaskKillerShell
import com.esde.companion.data.thor.awaitDisplayFocus
import com.esde.companion.data.thor.focusDisplay
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.GameLaunchDisplayTarget
import com.esde.companion.domain.model.GameLaunchOverride
import com.esde.companion.domain.model.resolveGameLaunchPackage
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
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
 * keep the latest system-defaults/game-overrides/display-target/close-on-end snapshots in
 * `@Volatile` fields, and one more reacts to the actual [AppState] stream. No
 * `distinctUntilChanged` is needed on the `AppState` collection - it's a `StateFlow`, which
 * already conflates truly-consecutive-identical emissions, and any real replay of the same game
 * has a non-[AppState.PlayingGame] state in between (quitting to a menu, browsing away), so it
 * naturally re-fires on a genuine restart.
 *
 * When Settings > UI Settings > Game Launch Override's "Close App on Game End" toggle is on, the
 * app most recently launched by [onGameStarted] is force-stopped the moment [AppState] moves away
 * from [AppState.PlayingGame] ("ends" here means that state transition, not a literal ES-DE
 * `game-end` scripting event - the reducer already folds that into whatever [AppState] comes
 * next). Closing reuses [TaskKillerShell.forceStop] - the same root-shell mechanism Thor
 * Settings' Task Killer uses - rather than a new mechanism: there's no public, unprivileged
 * Android API to close another app's foreground activity from outside it, so like every other
 * [com.esde.companion.data.thor.PrivilegedShell] consumer this is a Thor-only, best-effort
 * capability that silently no-ops (logged, never crashes) on firmware where the privileged
 * service isn't available - the toggle simply does nothing there, which is safe since it
 * defaults off.
 *
 * [onGameStarted] also hands hardware-key focus back once the launched app appears, via
 * [focusDisplay] - starting an activity on a display necessarily steals focus there, and
 * nothing else does this today. The rule is deterministic, not "restore whatever was focused
 * before": focus always goes to whichever of the two known displays *wasn't* the launch target
 * (`ThisScreen` launches -> focus the other display; `OtherScreen` launches -> focus Companion's
 * own display), so the game gets focus back either way - including the case where an ES-DE
 * setting has the game itself landing on Companion's own screen instead of its usual one, which
 * is exactly why "the other screen" can't be assumed to always mean "the game's screen." Same
 * Thor-only/best-effort/no-toggle reasoning as above; no public API exists for this either.
 * [onGameStarted] first waits ([awaitDisplayFocus]) for the launched app to actually take focus
 * before reclaiming it - see that function's kdoc for the race this avoids.
 */
class GameLaunchOverrideCoordinator(
    private val observeAppState: ObserveAppStateUseCase,
    private val settings: GameLaunchOverrideSettings,
    private val companionDisplayHolder: CompanionDisplayHolder,
    private val debugFileLogger: DebugFileLogger,
) {
    @Volatile
    private var systemDefaults: Map<String, String> = emptyMap()

    @Volatile
    private var gameOverrides: List<GameLaunchOverride> = emptyList()

    @Volatile
    private var displayTarget: GameLaunchDisplayTarget = GameLaunchDisplayTarget.ThisScreen

    @Volatile
    private var closeAppOnGameEnd: Boolean = false

    // Only ever read/written from the single observeAppState collector below, so no
    // synchronization beyond @Volatile's visibility guarantee is needed.
    @Volatile
    private var lastLaunchedPackage: String? = null

    fun start(
        context: Context,
        applicationScope: CoroutineScope,
    ) {
        val appContext = context.applicationContext

        applicationScope.launch {
            settings.observeSystemDefaults().collect { systemDefaults = it }
        }
        applicationScope.launch {
            settings.observeOverrides().collect { gameOverrides = it }
        }
        applicationScope.launch {
            settings.observeDisplayTarget().collect { displayTarget = it }
        }
        applicationScope.launch {
            settings.observeCloseAppOnGameEnd().collect { closeAppOnGameEnd = it }
        }
        applicationScope.launch {
            observeAppState().collect { state ->
                if (state is AppState.PlayingGame) onGameStarted(appContext, state) else onGameEnded()
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

        // context is an application Context (this coordinator runs in application scope), so
        // it isn't tied to any display - omitting a display option entirely does NOT mean
        // "wherever Companion is running," it means Android picks one by its own heuristics
        // (observed on-device: intermittently landing on the other screen).
        // companionDisplayHolder.displayId is the only reliable source for "Companion's own
        // screen" from here.
        val companionId = companionDisplayHolder.displayId
        val secondaryId = SecondaryDisplayResolver.secondaryDisplayId(context, companionId)
        val (launchDisplayId, focusDisplayId) =
            when (displayTarget) {
                GameLaunchDisplayTarget.ThisScreen -> companionId to secondaryId
                GameLaunchDisplayTarget.OtherScreen -> secondaryId to companionId
            }
        AppLauncher.launch(context, packageName, launchDisplayId)
        lastLaunchedPackage = packageName
        debugFileLogger.logInfo(LOG_TAG, "Launched $packageName for ${state.gameName} (${state.systemShortName})")

        if (focusDisplayId != null) {
            // Wait for the launched app to actually grab focus before trying to reclaim it -
            // firing immediately races the app's own (slower) focus-steal as it finishes
            // loading, and loses: the later steal simply overwrites an early restore attempt
            // with nothing left to correct it. See awaitDisplayFocus's kdoc.
            if (launchDisplayId != null) awaitDisplayFocus(launchDisplayId)
            val focused = focusDisplay(context, focusDisplayId)
            debugFileLogger.logInfo(
                LOG_TAG,
                "${if (focused) "Restored" else "FAILED to restore"} focus to display $focusDisplayId",
            )
        }
    }

    private fun onGameEnded() {
        val packageName = lastLaunchedPackage ?: return
        lastLaunchedPackage = null
        if (!closeAppOnGameEnd) return
        val stopped = TaskKillerShell.forceStop(packageName)
        debugFileLogger.logInfo(LOG_TAG, "${if (stopped) "Closed" else "FAILED to close"} $packageName on game end")
    }

    private companion object {
        const val LOG_TAG = "GameLaunchOverride"
    }
}
