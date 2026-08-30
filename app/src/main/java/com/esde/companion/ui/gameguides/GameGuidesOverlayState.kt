package com.esde.companion.ui.gameguides

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.esde.companion.AppContainer
import com.esde.companion.domain.model.ScreenBehavior

/**
 * Bundles the Game Guides overlay's ViewModel plus every piece of state MainActivity needs
 * to decide whether it's showing and what to do when it closes - pulled out of MainActivity
 * itself (which was tipping detekt's LargeClass threshold) rather than left as loose
 * `remember`ed state scattered through its composable body. [GameGuidesOverlayActions.onOpen]/
 * [GameGuidesOverlayActions.onClose] cover the Browsing/Library screens, which always fully
 * close the overlay; the Viewing screen needs [GameGuidesOverlayActions.onCloseViewer]
 * instead, since a guide opened by the Game Playing Screen Behavior auto-trigger should exit
 * the overlay entirely on close (same as dismissing the GameManual cover), while one opened
 * via the Game Guides FAB should drop back to the Library ([GameGuidesViewModel.open])
 * instead.
 *
 * [GameGuidesOverlayActions.onOpenDirectly] is the third way in, alongside the FAB
 * ([onOpen]) and the Game Playing Screen Behavior auto-trigger: Settings > Game Guides
 * (Browse Downloaded Guides' "open this guide", Add Guide's "browse for this game") calls
 * [GameGuidesOverlayState.viewModel] directly ([GameGuidesViewModel.openGuide]/
 * [GameGuidesViewModel.openBrowserFor]) to set up the exact state it wants, then
 * [onOpenDirectly] to reveal *this* full-screen overlay showing it - the same one the FAB
 * opens - rather than rendering a second, Settings-popup-sized copy of the browser/viewer.
 * Closing a directly-opened guide/browser exits the whole overlay, same as the auto-trigger,
 * since there's no "current game" Library it would make sense to fall back into.
 */
data class GameGuidesOverlayActions(
    val onOpen: () -> Unit,
    val onClose: () -> Unit,
    val onCloseViewer: () -> Unit,
    val onOpenDirectly: () -> Unit,
)

class GameGuidesOverlayState(
    val viewModel: GameGuidesViewModel,
    val hasCurrentGame: Boolean,
    val uiState: GameGuidesUiState,
    val isShowing: Boolean,
    val actions: GameGuidesOverlayActions,
)

@Composable
fun rememberGameGuidesOverlayState(
    appContainer: AppContainer,
    activeScreenBehavior: ScreenBehavior,
    isPlayingGame: Boolean,
): GameGuidesOverlayState {
    val viewModel: GameGuidesViewModel = viewModel(factory = GameGuidesViewModelFactory(appContainer))
    val hasCurrentGame by viewModel.hasCurrentGame.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // "Exit" dismissal for the auto-trigger below - separate from the FAB-driven
    // showOverlay flag itself, same shape as MainActivity's manualDismissed. Reset
    // on every PlayingGame transition (see the isPlayingGame LaunchedEffect below)
    // rather than on a current-game-reference change: ES-DE keeps reporting the
    // same GameReference while browsing the just-exited game, so a reference-keyed
    // reset never fires on a plain relaunch of the same game and the dismissal
    // would otherwise wrongly suppress every future play of it too.
    val guideDismissed = rememberSaveable { mutableStateOf(false) }

    // True only while the overlay was opened by the auto-trigger below, not by
    // tapping the Game Guides FAB - distinguishes the two so the FAB-open effect
    // (which always re-resolves Library-vs-Browser) doesn't stomp the specific
    // guide the auto-trigger already jumped straight into, so onCloseViewer exits
    // the whole overlay for an auto-opened guide instead of dropping into the
    // Library, and so the isPlayingGame effect below only force-closes a guide it
    // opened itself (a FAB-opened guide stays open across playing ending, same as
    // GameManual's manualViewerOpenedViaFab independence).
    val guideAutoOpened = rememberSaveable { mutableStateOf(false) }

    // True only while the overlay was opened directly (Settings > Game Guides, see
    // [GameGuidesOverlayActions.onOpenDirectly]'s kdoc) - same "don't let the FAB-open
    // effect below stomp it" reasoning as [guideAutoOpened], and the same "close exits the
    // whole overlay" reasoning on [GameGuidesOverlayActions.onCloseViewer], just for a
    // different caller.
    val guideOpenedDirectly = rememberSaveable { mutableStateOf(false) }

    val showOverlay = rememberSaveable { mutableStateOf(false) }

    // Reopening (tapping the FAB) always re-resolves Library-vs-Browser for
    // whichever game is current, rather than resuming whatever sub-state was
    // showing last time it was open - skipped when guideAutoOpened/guideOpenedDirectly,
    // since whoever set one of those flags already put uiState into the exact state it
    // wants shown.
    LaunchedEffect(showOverlay.value) {
        if (showOverlay.value && !guideAutoOpened.value && !guideOpenedDirectly.value) viewModel.open()
    }

    // Mirrors GameManual's auto-display, which is naturally non-sticky (it's a
    // plain derived boolean re-read every frame) - this needs an explicit reset
    // here instead because opening/closing this overlay is asynchronous (it reads
    // guide/progress records off disk) rather than a synchronously-available path
    // string. Playing ending both closes an auto-opened guide (never a FAB-opened
    // one) and clears the dismissal, so the very next PlayingGame transition -
    // even a relaunch of the exact same game - auto-opens fresh again.
    LaunchedEffect(isPlayingGame) {
        if (!isPlayingGame) {
            guideDismissed.value = false
            if (guideAutoOpened.value) {
                guideAutoOpened.value = false
                showOverlay.value = false
            }
        }
    }

    // Settings > UI Settings > Game Playing Screen Behavior > Guide: jumps
    // straight into the current game's most-recently-viewed downloaded guide,
    // resumed at its last position. The resolve is async (it reads guide/
    // progress records) so this is a LaunchedEffect rather than a derived
    // boolean feeding a simpler `visible =` condition.
    val autoShowGuide = isPlayingGame && activeScreenBehavior == ScreenBehavior.GameGuide && !guideDismissed.value
    LaunchedEffect(autoShowGuide) {
        if (autoShowGuide && !showOverlay.value && viewModel.autoOpenLastViewedGuideForCurrentGame()) {
            guideAutoOpened.value = true
            showOverlay.value = true
        }
    }

    return GameGuidesOverlayState(
        viewModel = viewModel,
        hasCurrentGame = hasCurrentGame,
        uiState = uiState,
        isShowing = showOverlay.value,
        actions = buildOverlayActions(viewModel, showOverlay, guideAutoOpened, guideOpenedDirectly, guideDismissed),
    )
}

private fun buildOverlayActions(
    viewModel: GameGuidesViewModel,
    showOverlay: MutableState<Boolean>,
    guideAutoOpened: MutableState<Boolean>,
    guideOpenedDirectly: MutableState<Boolean>,
    guideDismissed: MutableState<Boolean>,
): GameGuidesOverlayActions =
    GameGuidesOverlayActions(
        onOpen = { showOverlay.value = true },
        onClose = {
            guideAutoOpened.value = false
            guideOpenedDirectly.value = false
            showOverlay.value = false
        },
        onCloseViewer = {
            when {
                guideAutoOpened.value -> {
                    guideDismissed.value = true
                    guideAutoOpened.value = false
                    showOverlay.value = false
                }
                guideOpenedDirectly.value -> {
                    guideOpenedDirectly.value = false
                    showOverlay.value = false
                }
                else -> viewModel.open()
            }
        },
        onOpenDirectly = {
            guideOpenedDirectly.value = true
            showOverlay.value = true
        },
    )
