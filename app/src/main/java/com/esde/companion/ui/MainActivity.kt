package com.esde.companion.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.esde.companion.CompanionApplication
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.MusicPlaybackState
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.model.stateGroup
import com.esde.companion.ui.dock.AppDockViewModel
import com.esde.companion.ui.dock.AppDockViewModelFactory
import com.esde.companion.ui.drawer.AppDrawerViewModel
import com.esde.companion.ui.drawer.AppDrawerViewModelFactory
import com.esde.companion.ui.main.MainScreen
import com.esde.companion.ui.main.MainViewModel
import com.esde.companion.ui.main.MainViewModelFactory
import com.esde.companion.ui.manual.GameManualScreen
import com.esde.companion.ui.manual.GameManualViewModel
import com.esde.companion.ui.manual.GameManualViewModelFactory
import com.esde.companion.ui.music.MusicControlsOverlay
import com.esde.companion.ui.music.MusicControlsViewModel
import com.esde.companion.ui.music.MusicControlsViewModelFactory
import com.esde.companion.ui.onboarding.OnboardingScreen
import com.esde.companion.ui.onboarding.OnboardingViewModel
import com.esde.companion.ui.onboarding.OnboardingViewModelFactory
import com.esde.companion.ui.settings.ManageAppsViewModel
import com.esde.companion.ui.settings.ManageAppsViewModelFactory
import com.esde.companion.ui.settings.SettingsScreen
import com.esde.companion.ui.settings.SettingsViewModel
import com.esde.companion.ui.settings.SettingsViewModelFactory
import com.esde.companion.ui.theme.EsdeCompanionTheme
import com.esde.companion.ui.video.VideoOverlayScreen
import com.esde.companion.ui.video.VideoOverlayViewModel
import com.esde.companion.ui.video.VideoOverlayViewModelFactory
import com.esde.companion.ui.widgets.WidgetOverlay
import com.esde.companion.ui.widgets.WidgetsViewModel
import com.esde.companion.ui.widgets.WidgetsViewModelFactory
import com.esde.companion.ui.widgets.edit.EditWidgetsOverlay
import com.esde.companion.ui.widgets.edit.EditWidgetsViewModel
import com.esde.companion.ui.widgets.edit.EditWidgetsViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private object Destinations {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

/**
 * Resolved once at startup (see the produceState block below) before NavHost's start
 * destination is decided. [savedLogFolderPath]/[savedMediaFolderPath] carry whatever was
 * already confirmed and persisted from a prior onboarding run, if any - passed into
 * OnboardingViewModelFactory so re-entering onboarding solely because the permission was
 * revoked (see [showOnboarding]) seeds EsdeFolder/MediaFolder with the real prior
 * location instead of the hardcoded default guess.
 */
private data class OnboardingStartupInfo(
    val showOnboarding: Boolean,
    val savedLogFolderPath: String?,
    val savedMediaFolderPath: String?,
)

// Layout constants for the music FAB + MusicControlsOverlay pairing - see their usage
// site for why these are needed rather than a plain Row (the FAB and overlay are
// independently aligned children of a BoxWithConstraints, not siblings in a Row, since
// the overlay's visibility is conditional and shouldn't reflow the FAB's own position).
// Size/edge padding come from CornerButtonMetrics, shared with MainScreen's Settings
// button so all three corner controls stay aligned by construction.
private val MUSIC_OVERLAY_GAP = 12.dp

// MainScreen's Settings button is sized/positioned from the same CornerButtonMetrics
// constants as the music FAB, so its footprint in the opposite corner is exactly
// CORNER_BUTTON_EDGE_PADDING + CORNER_BUTTON_SIZE - no guessing needed.
private val SETTINGS_BUTTON_RESERVED_WIDTH = CORNER_BUTTON_EDGE_PADDING + CORNER_BUTTON_SIZE

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
                // underneath the NavHost. Also re-checked here (not just at first-ever
                // install) because "All files access" can be revoked from Android
                // Settings independently of the onboarding-complete flag - the log/media
                // read pipeline this permission gates would otherwise silently stop
                // working with no path back to the grant screen. OnboardingViewModel
                // already knows how to start at OnboardingStep.Permission and walk
                // forward from there, same as a fresh install.
                val startupInfo by produceState<OnboardingStartupInfo?>(initialValue = null) {
                    val onboardingComplete = appContainer.observeOnboardingCompleteUseCase().first()
                    value = OnboardingStartupInfo(
                        showOnboarding = !onboardingComplete || !AllFilesAccessPermission.isGranted(),
                        savedLogFolderPath = appContainer.onboardingRepository.observeLogFolderPath().first(),
                        savedMediaFolderPath = appContainer.onboardingRepository.observeMediaFolderPath().first(),
                    )
                }

                startupInfo?.let { info ->
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = if (info.showOnboarding) Destinations.ONBOARDING else Destinations.MAIN,
                    ) {
                        composable(Destinations.ONBOARDING) {
                            val viewModel: OnboardingViewModel = viewModel(
                                factory = OnboardingViewModelFactory(
                                    appContainer = appContainer,
                                    initialLogFolderPath = info.savedLogFolderPath,
                                    initialMediaFolderPath = info.savedMediaFolderPath,
                                ),
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
                            val dockViewModel: AppDockViewModel = viewModel(factory = AppDockViewModelFactory(appContainer))
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

                            val mainScreenActive = !showSettings && !showEditWidgets && !drawerOpen

                            // Settings > UI Settings: how the main screen should react
                            // while a game is playing / the screensaver is active.
                            val gamePlayingBehavior by viewModel.gamePlayingBehavior.collectAsStateWithLifecycle()
                            val screensaverBehavior by viewModel.screensaverBehavior.collectAsStateWithLifecycle()

                            val widgetsViewModel: WidgetsViewModel = viewModel(factory = WidgetsViewModelFactory(appContainer))

                            val gameManualViewModel: GameManualViewModel = viewModel(factory = GameManualViewModelFactory(appContainer))
                            val manualPdfPath by gameManualViewModel.pdfPath.collectAsStateWithLifecycle()

                            // Manual "exit" dismissal for the GameManual cover - separate
                            // from isBlanked since it's specific to this one behavior.
                            // Reset below whenever PlayingGame ends, so dismissing it once
                            // doesn't suppress the manual for every future game too.
                            var manualDismissed by rememberSaveable { mutableStateOf(false) }

                            val videoPlaybackEnabled by viewModel.videoPlaybackEnabled.collectAsStateWithLifecycle()

                            val isActivityVisible by produceState(initialValue = true) {
                                appContainer.activityVisibilityRepository.observeIsVisible().collect { value = it }
                            }

                            val videoOverlayViewModel: VideoOverlayViewModel =
                                viewModel(factory = VideoOverlayViewModelFactory(appContainer))
                            val videoPath by videoOverlayViewModel.videoPath.collectAsStateWithLifecycle()
                            val videoDelaySeconds by videoOverlayViewModel.delaySeconds.collectAsStateWithLifecycle()
                            val videoAudioEnabled by videoOverlayViewModel.audioEnabled.collectAsStateWithLifecycle()

                            val musicControlsViewModel: MusicControlsViewModel =
                                viewModel(factory = MusicControlsViewModelFactory(appContainer))
                            val musicPlaybackState by musicControlsViewModel.playbackState.collectAsStateWithLifecycle()
                            val musicEnabled by produceState(initialValue = true) {
                                appContainer.observeMusicEnabledUseCase().collect { value = it }
                            }
                            // Master opacity for every translucent overlay surface (App
                            // Drawer, App Dock, this music panel, and the Settings/
                            // music-FAB/Edit-Widgets corner buttons) - one shared Settings
                            // > UI Settings slider rather than a separate one per surface.
                            val overlayOpacityPercent by produceState(initialValue = 80) {
                                appContainer.observeOverlayOpacityUseCase().collect { value = it }
                            }

                            // Tapping the FAB toggles this; the timer alone controls
                            // dismissal - it must not be re-derived from musicPlaybackState,
                            // since tapping the panel's own Pause button flips
                            // Playing -> Paused but shouldn't close the panel.
                            var musicControlsRevealed by remember { mutableStateOf(false) }

                            val musicTrack = when (val state = musicPlaybackState) {
                                is MusicPlaybackState.Playing -> state.track
                                is MusicPlaybackState.Paused -> state.track
                                MusicPlaybackState.Stopped -> null
                            }

                            // Keyed on the track too, not just musicControlsRevealed, so a
                            // new song starting while the panel is already showing restarts
                            // the 4s countdown instead of letting it expire on the previous
                            // track's schedule - setting an already-true mutableState to
                            // true again doesn't change the key and wouldn't otherwise
                            // restart this effect. Play/pause toggling the same track
                            // doesn't change filePath, so it still doesn't reset the timer.
                            LaunchedEffect(musicControlsRevealed, musicTrack?.filePath) {
                                if (musicControlsRevealed) {
                                    delay(4_000)
                                    musicControlsRevealed = false
                                }
                            }

                            LaunchedEffect(musicTrack?.filePath) {
                                if (musicTrack != null) musicControlsRevealed = true
                            }

                            val activeScreenBehavior = when ((connectionState as? EsdeConnectionState.Connected)?.appState) {
                                is AppState.PlayingGame -> gamePlayingBehavior
                                is AppState.Screensaver -> screensaverBehavior
                                else -> ScreenBehavior.Nothing
                            }

                            val isPlayingGame = (connectionState as? EsdeConnectionState.Connected)?.appState is AppState.PlayingGame
                            LaunchedEffect(isPlayingGame) {
                                if (!isPlayingGame) manualDismissed = false
                            }

                            val isBrowsingGame = (connectionState as? EsdeConnectionState.Connected)?.appState is AppState.BrowsingGame
                            val showVideoOverlay = videoPlaybackEnabled &&
                                    videoPath != null &&
                                    isBrowsingGame &&
                                    mainScreenActive &&
                                    isActivityVisible

                            LaunchedEffect(videoPlaybackEnabled, videoPath, isBrowsingGame, mainScreenActive, isActivityVisible) {
                                android.util.Log.d(
                                    "VideoDebug",
                                    "enabled=$videoPlaybackEnabled path=$videoPath browsing=$isBrowsingGame " +
                                            "mainScreenActive=$mainScreenActive visible=$isActivityVisible -> show=$showVideoOverlay",
                                )
                            }

                            // GameManual selected but no manual resolved for this game, or
                            // the user tapped exit on it -> falls through to the plain
                            // main screen, same as ScreenBehavior.Nothing.
                            val showGameManual = activeScreenBehavior == ScreenBehavior.GameManual &&
                                    manualPdfPath != null &&
                                    !manualDismissed

                            val autoBlackTrigger = activeScreenBehavior == ScreenBehavior.Black && !showGameManual
                            val isDimmed = activeScreenBehavior == ScreenBehavior.Dim && !showGameManual

                            LaunchedEffect(autoBlackTrigger) {
                                isBlanked = autoBlackTrigger
                            }

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
                                            dockViewModel = dockViewModel,
                                            widgetsLocked = widgetsLocked,
                                            overlayOpacityPercent = overlayOpacityPercent,
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

                                // GameManual cover (Settings > UI Settings: Game Playing
                                // Behavior) - same placement/guard as Dim/Black above, but
                                // its own branch rather than reusing isBlanked, since it
                                // renders interactive content (paged/zoomable PDF) rather
                                // than a plain cover.
                                if (showGameManual && mainScreenActive) {
                                    GameManualScreen(
                                        viewModel = gameManualViewModel,
                                        onExit = { manualDismissed = true },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }

                                if (showVideoOverlay) {
                                    VideoOverlayScreen(
                                        videoPath = videoPath!!,
                                        delaySeconds = videoDelaySeconds,
                                        audioEnabled = videoAudioEnabled,
                                        modifier = Modifier.fillMaxSize(),
                                        onIsPlayingChanged = appContainer.videoPlaybackStateRepository::setIsPlaying,
                                    )
                                }

                                // Corner FAB (not a full-screen tap gesture) so it never
                                // clashes with MainScreen's existing long-press/double-tap
                                // handling - same "small corner button" architecture as
                                // EditWidgetsOverlay's options button, opposite corner.
                                if (mainScreenActive && !isBlanked && isActivityVisible && musicEnabled && musicPlaybackState != MusicPlaybackState.Stopped) {
                                    // BoxWithConstraints (not the outer fillMaxSize Box) so
                                    // the overlay's max width below can be computed from the
                                    // actual available screen width, capped short of
                                    // MainScreen's Settings gear in the opposite corner
                                    // (that button lives in a different composable/file, so
                                    // there's no shared layout to measure it against - a
                                    // fixed reserved width is the simplest way to guarantee
                                    // no overlap regardless of screen size).
                                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                        CornerFab(
                                            onClick = { musicControlsRevealed = !musicControlsRevealed },
                                            opacityPercent = overlayOpacityPercent,
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(CORNER_BUTTON_EDGE_PADDING),
                                        ) {
                                            Icon(imageVector = Icons.Filled.MusicNote, contentDescription = "Music controls")
                                        }

                                        if (musicControlsRevealed) {
                                            val overlayStart = CORNER_BUTTON_EDGE_PADDING + CORNER_BUTTON_SIZE + MUSIC_OVERLAY_GAP
                                            val overlayMaxWidth = maxWidth - overlayStart - SETTINGS_BUTTON_RESERVED_WIDTH
                                            MusicControlsOverlay(
                                                viewModel = musicControlsViewModel,
                                                opacityPercent = overlayOpacityPercent,
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(start = overlayStart, top = CORNER_BUTTON_EDGE_PADDING)
                                                    .widthIn(max = overlayMaxWidth.coerceAtLeast(0.dp))
                                                    // min, not exact - a wrapped two-line
                                                    // title (see MusicControlsOverlay) needs
                                                    // to grow taller than the FAB, not be
                                                    // clipped to match it.
                                                    .heightIn(min = CORNER_BUTTON_SIZE),
                                            )
                                        }
                                    }
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

    override fun onStart() {
        super.onStart()
        (application as CompanionApplication).appContainer.activityVisibilityRepository.setVisible(true)
    }

    override fun onStop() {
        super.onStop()
        (application as CompanionApplication).appContainer.activityVisibilityRepository.setVisible(false)
    }

    private fun hideStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}