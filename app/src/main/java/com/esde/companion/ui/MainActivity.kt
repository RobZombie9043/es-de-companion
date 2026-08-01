package com.esde.companion.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.esde.companion.CompanionApplication
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.model.stateGroup
import com.esde.companion.ui.drawer.AppDrawerViewModel
import com.esde.companion.ui.drawer.AppDrawerViewModelFactory
import com.esde.companion.ui.main.MainScreen
import com.esde.companion.ui.main.MainViewModel
import com.esde.companion.ui.main.MainViewModelFactory
import com.esde.companion.ui.onboarding.OnboardingScreen
import com.esde.companion.ui.onboarding.OnboardingViewModel
import com.esde.companion.ui.onboarding.OnboardingViewModelFactory
import com.esde.companion.ui.settings.ManageAppsViewModel
import com.esde.companion.ui.settings.ManageAppsViewModelFactory
import com.esde.companion.ui.settings.SettingsScreen
import com.esde.companion.ui.settings.SettingsViewModel
import com.esde.companion.ui.settings.SettingsViewModelFactory
import com.esde.companion.ui.theme.EsdeCompanionTheme
import com.esde.companion.ui.widgets.WidgetOverlay
import com.esde.companion.ui.widgets.WidgetsViewModel
import com.esde.companion.ui.widgets.WidgetsViewModelFactory
import com.esde.companion.ui.widgets.edit.EditWidgetsOverlay
import com.esde.companion.ui.widgets.edit.EditWidgetsViewModel
import com.esde.companion.ui.widgets.edit.EditWidgetsViewModelFactory
import kotlinx.coroutines.flow.first

private object Destinations {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideStatusBar()
        val appContainer = (application as CompanionApplication).appContainer

        setContent {
            val themePreference by produceState(initialValue = ThemePreference.Auto) {
                appContainer.observeThemePreferenceUseCase().collect { value = it }
            }
            EsdeCompanionTheme(themePreference = themePreference) {
                // Read once, not continuously observed - NavHost's start destination is
                // fixed at first composition. Onboarding finishing later is handled by
                // an explicit navController.navigate call, not by this value changing
                // underneath the NavHost.
                val onboardingComplete by produceState<Boolean?>(initialValue = null) {
                    value = appContainer.observeOnboardingCompleteUseCase().first()
                }

                onboardingComplete?.let { complete ->
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = if (complete) Destinations.MAIN else Destinations.ONBOARDING,
                    ) {
                        composable(Destinations.ONBOARDING) {
                            val viewModel: OnboardingViewModel = viewModel(
                                factory = OnboardingViewModelFactory(appContainer),
                            )
                            OnboardingScreen(
                                viewModel = viewModel,
                                onOnboardingComplete = {
                                    navController.navigate(Destinations.MAIN) {
                                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(Destinations.MAIN) {
                            val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(appContainer))
                            val appDrawerViewModel: AppDrawerViewModel = viewModel(factory = AppDrawerViewModelFactory(appContainer))
                            var showSettings by rememberSaveable { mutableStateOf(false) }
                            var showEditWidgets by rememberSaveable { mutableStateOf(false) }

                            // Whichever StateGroup is live right now - read fresh on every
                            // recomposition, so by the time edit mode actually opens (long
                            // press on MainScreen, or the Settings entry point) it reflects
                            // wherever ES-DE currently is, not whatever canvas was last left
                            // open in the editor. Idle/no connection falls back to System.
                            val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
                            val editWidgetsInitialCanvas = (connectionState as? EsdeConnectionState.Connected)
                                ?.appState?.stateGroup() ?: StateGroup.System

                            val widgetsLocked by produceState(initialValue = false) {
                                appContainer.observeWidgetsLockedUseCase().collect { value = it }
                            }

                            // Momentary "is the screen currently blanked" state - local,
                            // not persisted. Double-tap-to-blank is always available (no
                            // longer gated by a Settings toggle), and this same flag also
                            // drives the automatic Black behavior below - see
                            // LaunchedEffect(autoBlackTrigger). Owned here (rather than
                            // inside MainScreen) so the black cover it drives can be drawn
                            // as a sibling of WidgetOverlay below, and therefore actually
                            // sit on top of the widgets - see the Box below for why.
                            var isBlanked by rememberSaveable { mutableStateOf(false) }

                            // Whether the App Drawer is currently open - reported by
                            // MainScreen via onDrawerOpenChanged. Combined with
                            // showSettings/showEditWidgets below so the automatic
                            // Dim/Black cover never shows over anything but the plain
                            // main screen.
                            var drawerOpen by remember { mutableStateOf(false) }

                            // Settings > UI Settings: how the main screen should react
                            // while a game is playing / the screensaver is active.
                            val gamePlayingBehavior by viewModel.gamePlayingBehavior.collectAsStateWithLifecycle()
                            val screensaverBehavior by viewModel.screensaverBehavior.collectAsStateWithLifecycle()

                            // Whichever behavior applies to the live AppState right now,
                            // or Nothing otherwise - AppState is a single sealed value,
                            // so PlayingGame and Screensaver can never both apply at once.
                            val activeScreenBehavior = when ((connectionState as? EsdeConnectionState.Connected)?.appState) {
                                is AppState.PlayingGame -> gamePlayingBehavior
                                is AppState.Screensaver -> screensaverBehavior
                                else -> ScreenBehavior.Nothing
                            }
                            val autoBlackTrigger = activeScreenBehavior == ScreenBehavior.Black
                            val isDimmed = activeScreenBehavior == ScreenBehavior.Dim

                            // Reuses the exact same isBlanked flag/gesture as the manual
                            // double-tap: entering a Black-triggering state forces it on,
                            // leaving one forces it off. While the trigger stays constant
                            // this effect doesn't rerun, so a double-tap dismissal (below)
                            // sticks until the state actually changes again.
                            LaunchedEffect(autoBlackTrigger) {
                                isBlanked = autoBlackTrigger
                            }

                            // Collected here, above the settings/edit-widgets toggles, so
                            // this call site - and the WidgetCanvas/CrossfadeAsyncImage
                            // state inside WidgetOverlay - never leaves composition just
                            // because Settings or edit mode is showing. Otherwise
                            // WhileSubscribed(5_000) on the ViewModel's flow would stop and
                            // restart on a long-enough visit, causing a visible
                            // reload/flash on return.
                            val widgetsViewModel: WidgetsViewModel = viewModel(factory = WidgetsViewModelFactory(appContainer))

                            // Dim/Black should only ever affect the plain main screen -
                            // not Settings, not Edit Widgets, and not the App Drawer.
                            val mainScreenActive = !showSettings && !showEditWidgets && !drawerOpen

                            Box(modifier = Modifier.fillMaxSize()) {
                                WidgetOverlay(viewModel = widgetsViewModel, modifier = Modifier.fillMaxSize())

                                when {
                                    showEditWidgets -> {
                                        val editWidgetsViewModel: EditWidgetsViewModel =
                                            viewModel(factory = EditWidgetsViewModelFactory(appContainer))
                                        EditWidgetsOverlay(
                                            viewModel = editWidgetsViewModel,
                                            initialCanvas = editWidgetsInitialCanvas,
                                            onDone = { showEditWidgets = false },
                                        )
                                    }

                                    showSettings -> {
                                        val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(appContainer))
                                        val manageAppsViewModel: ManageAppsViewModel = viewModel(factory = ManageAppsViewModelFactory(appContainer))
                                        SettingsScreen(
                                            viewModel = settingsViewModel,
                                            manageAppsViewModel = manageAppsViewModel,
                                            onDone = { showSettings = false },
                                            onEditWidgetsClick = {
                                                showSettings = false
                                                showEditWidgets = true
                                            },
                                        )
                                    }

                                    else -> {
                                        MainScreen(
                                            viewModel = viewModel,
                                            appDrawerViewModel = appDrawerViewModel,
                                            widgetsLocked = widgetsLocked,
                                            onOpenSettings = { showSettings = true },
                                            onOpenEditWidgets = { showEditWidgets = true },
                                            onToggleBlankScreen = { isBlanked = !isBlanked },
                                            onDrawerOpenChanged = { drawerOpen = it },
                                        )
                                    }
                                }

                                // Automatic Dim (Settings > UI Settings: Game Playing /
                                // Screensaver Behavior). Purely visual - no clickable or
                                // pointerInput, so touches pass straight through to
                                // whatever's underneath, unlike the Black cover below.
                                if (isDimmed && mainScreenActive) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                    )
                                }

                                // Full-black cover for the always-available double-tap
                                // blank-screen gesture, and for automatic Black behavior
                                // (Settings > UI Settings). Deliberately the last child of
                                // THIS Box - the same parent WidgetOverlay is a child of -
                                // so it draws over the widget layer too, not just whatever
                                // screen (MainScreen/EditWidgets/Settings) happens to be
                                // showing underneath at the moment. The gesture that flips
                                // isBlanked lives in MainScreen; this is purely the cover
                                // plus the matching double-tap-to-dismiss.
                                //
                                // The no-op clickable (indication = null) claims every
                                // tap/press so nothing underneath - the drawer's drag zone,
                                // MainScreen's long-press-to-edit, widget taps - receives
                                // input while blanked. "Blanked" should mean genuinely
                                // unreachable, not just visually covered.
                                if (isBlanked && mainScreenActive) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {},
                                            )
                                            .pointerInput(Unit) {
                                                detectTapGestures(onDoubleTap = {
                                                    isBlanked = false
                                                })
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // System UI can reappear on its own - a swipe-down gesture, or a transient system
    // dialog taking focus - and Android doesn't re-hide it automatically once dismissed.
    // Re-applying on every focus regain is the standard way to keep it suppressed for a
    // kiosk-style screen that's expected to run continuously.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideStatusBar()
        }
    }

    private fun hideStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}