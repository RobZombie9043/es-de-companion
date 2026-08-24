package com.esde.companion.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.esde.companion.CompanionApplication
import com.esde.companion.data.apps.AppIconLoader
import com.esde.companion.data.apps.AppLauncher
import com.esde.companion.data.apps.SecondaryDisplayResolver
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.BatteryStatus
import com.esde.companion.domain.model.BatteryTier
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.FabPosition
import com.esde.companion.domain.model.FabType
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LaunchLocation
import com.esde.companion.domain.model.MusicPlaybackState
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.SystemStatus
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
import com.esde.companion.ui.retroachievements.RetroAchievementsResolutionState
import com.esde.companion.ui.retroachievements.RetroAchievementsScreen
import com.esde.companion.ui.retroachievements.RetroAchievementsSystemGamesScreen
import com.esde.companion.ui.retroachievements.RetroAchievementsSystemGamesViewModel
import com.esde.companion.ui.retroachievements.RetroAchievementsSystemGamesViewModelFactory
import com.esde.companion.ui.retroachievements.RetroAchievementsViewModel
import com.esde.companion.ui.retroachievements.RetroAchievementsViewModelFactory
import com.esde.companion.ui.retroachievements.SystemGamesUiState
import com.esde.companion.ui.settings.ManageAppsViewModel
import com.esde.companion.ui.settings.ManageAppsViewModelFactory
import com.esde.companion.ui.settings.SettingsViewModel
import com.esde.companion.ui.settings.SettingsViewModelFactory
import com.esde.companion.ui.theme.EsdeCompanionTheme
import com.esde.companion.ui.theme.LocalIsDarkTheme
import com.esde.companion.ui.thor.AutoFpsTriggerAppsViewModel
import com.esde.companion.ui.thor.AutoFpsTriggerAppsViewModelFactory
import com.esde.companion.ui.thor.TaskKillerExcludedAppsViewModel
import com.esde.companion.ui.thor.TaskKillerExcludedAppsViewModelFactory
import com.esde.companion.ui.update.UpdateAvailableDialog
import com.esde.companion.ui.update.UpdateViewModel
import com.esde.companion.ui.update.UpdateViewModelFactory
import com.esde.companion.ui.update.WhatsNewDialog
import com.esde.companion.ui.video.VideoOverlayScreen
import com.esde.companion.ui.video.VideoOverlayViewModel
import com.esde.companion.ui.video.VideoOverlayViewModelFactory
import com.esde.companion.ui.video.VideoPlaybackEvent
import com.esde.companion.ui.widgets.WidgetOverlay
import com.esde.companion.ui.widgets.WidgetsViewModel
import com.esde.companion.ui.widgets.WidgetsViewModelFactory
import com.esde.companion.ui.widgets.edit.EditWidgetsOverlay
import com.esde.companion.ui.widgets.edit.EditWidgetsViewModel
import com.esde.companion.ui.widgets.edit.EditWidgetsViewModelFactory
import com.esde.companion.ui.widgets.fallbackBackgroundAssetPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

// Layout constant for the music FAB + MusicControlsOverlay pairing - see their usage
// site for why this is needed rather than a plain Row (the FAB and overlay are
// independently aligned children of a BoxWithConstraints, not siblings in a Row, since
// the overlay's visibility is conditional and shouldn't reflow the FAB's own position).
// Size/edge padding come from CornerButtonMetrics, shared with every other corner FAB so
// they all stay aligned by construction.
private val MUSIC_OVERLAY_GAP = 12.dp

// Slightly larger than a system Icon's default 24dp - a real app icon reads better with a
// bit more room inside the 56dp CornerFab, same reasoning as AppDock's larger dock icons.
private val CUSTOM_APP_ICON_SIZE = 32.dp

// How strongly the widget backdrop/MainScreen chrome blurs behind the long-press menu -
// see longPressMenuOpen below. Modifier.blur() only actually renders a blur on API 31+
// (backed by RenderEffect); on this app's minSdk 29/30 it's a harmless no-op, so those
// devices simply don't get the blur rather than crashing or looking broken.
private val LONG_PRESS_MENU_BLUR_RADIUS = 4.dp

// Settings > Other Settings: "Launch ES-DE on Companion App Start". ES-DE's package name
// isn't referenced anywhere else in this codebase (apps are identified via the installed
// apps list, not a hardcoded constant) - this is the one call site that needs it.
private const val ESDE_PACKAGE_NAME = "org.es_de.frontend"

/** How long the music controls panel stays revealed before auto-hiding. */
private const val MUSIC_CONTROLS_AUTO_HIDE_DELAY_MS = 4_000L

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
                    value =
                        OnboardingStartupInfo(
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
                            val viewModel: OnboardingViewModel =
                                viewModel(
                                    factory =
                                        OnboardingViewModelFactory(
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
                            val appDrawerViewModel: AppDrawerViewModel =
                                viewModel(factory = AppDrawerViewModelFactory(appContainer))
                            val dockViewModel: AppDockViewModel =
                                viewModel(factory = AppDockViewModelFactory(appContainer))
                            // Constructed unconditionally now, rather than only while
                            // Settings was showing - the long-press popup that hosts this
                            // content is a child of MainScreen, which is always in
                            // composition (unlike the old standalone SettingsScreen
                            // destination it replaced).
                            val settingsViewModel: SettingsViewModel =
                                viewModel(factory = SettingsViewModelFactory(appContainer))
                            val manageAppsViewModel: ManageAppsViewModel =
                                viewModel(factory = ManageAppsViewModelFactory(appContainer))
                            val autoFpsTriggerAppsViewModel: AutoFpsTriggerAppsViewModel =
                                viewModel(factory = AutoFpsTriggerAppsViewModelFactory(appContainer))
                            val taskKillerExcludedAppsViewModel: TaskKillerExcludedAppsViewModel =
                                viewModel(factory = TaskKillerExcludedAppsViewModelFactory(appContainer))
                            val updateViewModel: UpdateViewModel =
                                viewModel(factory = UpdateViewModelFactory(appContainer))
                            val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) { updateViewModel.runStartupChecks() }
                            var showEditWidgets by rememberSaveable { mutableStateOf(false) }

                            // Whichever StateGroup is live right now - read fresh on every
                            // recomposition, so by the time edit mode actually opens (long
                            // press on MainScreen, or the Settings entry point) it reflects
                            // wherever ES-DE currently is, not whatever canvas was last left
                            // open in the editor. Idle/no connection falls back to System.
                            val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
                            val editWidgetsInitialCanvas =
                                (connectionState as? EsdeConnectionState.Connected)
                                    ?.appState?.stateGroup() ?: StateGroup.System

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
                            // longPressMenuOpen/showEditWidgets below so the automatic
                            // Dim/Black cover never shows over anything but the plain
                            // main screen.
                            var drawerOpen by remember { mutableStateOf(false) }

                            // Reported by MainScreen via onLongPressMenuOpenChanged - used
                            // below to blur WidgetOverlay while the long-press menu is open,
                            // since that content is a sibling of MainScreen here, not a
                            // descendant it could blur itself. Also folded into
                            // mainScreenActive below: the popup now hosts the entire
                            // settings hierarchy (not just a couple of menu items), so it
                            // represents "the user is in Settings" the same way showSettings
                            // used to, before that was retired in favor of this popup.
                            var longPressMenuOpen by remember { mutableStateOf(false) }

                            // Incremented by a tap on the Clock/SystemStatus/
                            // ClockAndSystemStatus FAB (see wideFabContext below) - those
                            // FABs are rendered here, outside MainScreen, so they can't reach
                            // MainScreen's local setLongPressMenuOpen closure the way its own
                            // Settings FAB does. Passed down as openSettingsMenuRequest;
                            // MainScreen opens its menu on every new (unconsumed) increment.
                            var openSettingsMenuRequest by remember { mutableIntStateOf(0) }

                            // Reported by MainScreen (via AppDrawer) through
                            // onFolderOpenChanged - folded into mainScreenActive below so
                            // the automatic Dim/Black cover never shows over an open App
                            // Drawer folder panel. Deliberately does NOT drive the backdrop
                            // blur below (unlike longPressMenuOpen) - the folder panel no
                            // longer needs it now that it's an in-tree overlay rather than a
                            // separate Popup window, and it wasn't wanted visually either.
                            var folderOpen by remember { mutableStateOf(false) }

                            val mainScreenActive = !longPressMenuOpen && !showEditWidgets && !drawerOpen && !folderOpen

                            // Settings > UI Settings: how the main screen should react
                            // while a game is playing / the screensaver is active.
                            val gamePlayingBehavior by viewModel.gamePlayingBehavior.collectAsStateWithLifecycle()
                            val screensaverBehavior by viewModel.screensaverBehavior.collectAsStateWithLifecycle()
                            val gamePlayingDimPercent by viewModel.gamePlayingDimPercent.collectAsStateWithLifecycle()
                            val screensaverDimPercent by viewModel.screensaverDimPercent.collectAsStateWithLifecycle()

                            val widgetsViewModel: WidgetsViewModel =
                                viewModel(factory = WidgetsViewModelFactory(appContainer))

                            val gameManualViewModel: GameManualViewModel =
                                viewModel(factory = GameManualViewModelFactory(appContainer))
                            val manualPdfPath by gameManualViewModel.pdfPath.collectAsStateWithLifecycle()

                            // Manual "exit" dismissal for the GameManual cover - separate
                            // from isBlanked since it's specific to this one behavior. Reset
                            // below whenever the resolved manual itself changes, so
                            // dismissing it once doesn't suppress the manual for every
                            // future game too.
                            var manualDismissed by rememberSaveable { mutableStateOf(false) }

                            // Set by tapping the Game Manual FAB (FAB Control) - opens the
                            // same GameManualScreen the automatic Game Playing Behavior >
                            // Manual setting does, independent of that setting. Reset
                            // together with manualDismissed below.
                            var manualViewerOpenedViaFab by rememberSaveable { mutableStateOf(false) }

                            val retroAchievementsViewModel: RetroAchievementsViewModel =
                                viewModel(factory = RetroAchievementsViewModelFactory(appContainer))
                            val retroAchievementsResolution by
                                retroAchievementsViewModel.resolution.collectAsStateWithLifecycle()
                            val retroAchievementsHasContent =
                                retroAchievementsResolution != RetroAchievementsResolutionState.NotSignedIn &&
                                    retroAchievementsResolution != RetroAchievementsResolutionState.NoGame

                            val systemGamesViewModel: RetroAchievementsSystemGamesViewModel =
                                viewModel(factory = RetroAchievementsSystemGamesViewModelFactory(appContainer))
                            val systemGamesState by systemGamesViewModel.state.collectAsStateWithLifecycle()
                            // True for every state that means "signed in and browsing an
                            // actual system" - not just Loaded. Loading is included since
                            // RetroAchievementsSystemGamesViewModel only ever enters it once
                            // the browsed system is confirmed RA-supported (see its kdoc), so
                            // the FAB shows as soon as that's known rather than waiting on the
                            // games-list fetch to finish - a system with a huge catalog (gba,
                            // psx) can take a while, or transiently fail, to fetch/cache, and
                            // gating the FAB on Loaded alone made it disappear for that entire
                            // window even though the system definitely has achievements.
                            // UnsupportedSystem is included too - the FAB should still open to
                            // an explicit "doesn't support this system" message rather than
                            // vanish, and staying System-mode here (instead of falling through
                            // to None) also means navigating from a supported to an unsupported
                            // system while the viewer is already open switches its content
                            // instead of auto-dismissing it (see the LaunchedEffect below).
                            val systemGamesHasContent =
                                systemGamesState is SystemGamesUiState.Loading ||
                                    systemGamesState is SystemGamesUiState.Loaded ||
                                    systemGamesState is SystemGamesUiState.UnsupportedSystem

                            // One FAB, contextually aware of what ES-DE is currently
                            // showing rather than a separate FAB type per screen: whichever
                            // of the game-achievements/system-games ViewModels currently has
                            // content wins (the two are naturally mutually exclusive - RA-DE
                            // is either browsing a system or a game/screensaver at once, so
                            // at most one of the two "has content" flags above is ever true).
                            // Read live (not just at FAB-tap time) below so the open overlay
                            // itself flips between the two screens if the browsed system/game
                            // changes while it's still showing, rather than staying stuck on
                            // whichever one was open when the FAB was tapped.
                            val retroAchievementsFabMode =
                                when {
                                    retroAchievementsHasContent -> RetroAchievementsFabMode.Game
                                    systemGamesHasContent -> RetroAchievementsFabMode.System
                                    else -> RetroAchievementsFabMode.None
                                }
                            // See rememberHeldRetroAchievementsFabMode's kdoc: retroAchievementsFabMode
                            // genuinely is None for one or more frames during a live system<->game
                            // navigation, since the two source ViewModels resolve independently and
                            // asynchronously - this holds the previous mode through that gap rather
                            // than reacting to a momentary None.
                            val heldRetroAchievementsFabMode =
                                rememberHeldRetroAchievementsFabMode(retroAchievementsFabMode)

                            // Set by tapping the RetroAchievements FAB (FAB Control); closed
                            // via either screen's own close button. No automatic open-on-launch
                            // in this phase (see CLAUDE.md's RetroAchievements section). Which
                            // of the two screens actually renders while this is true is
                            // decided live from heldRetroAchievementsFabMode below, not frozen at
                            // the moment this was set - see the two AnimatedVisibility blocks.
                            var showRetroAchievementsOverlay by rememberSaveable { mutableStateOf(false) }

                            // Auto-dismiss if navigating away leaves neither screen with
                            // anything to show (e.g. back to Idle, or off to an unsupported
                            // system) - keeping a stale screen open would show content for
                            // a game/system that's no longer the one ES-DE is on. Keyed on the
                            // held mode, not the raw one, so a momentary gap mid-navigation
                            // never triggers this.
                            LaunchedEffect(heldRetroAchievementsFabMode) {
                                if (heldRetroAchievementsFabMode == RetroAchievementsFabMode.None) {
                                    showRetroAchievementsOverlay = false
                                }
                            }

                            // Fed into each ViewModel's onOverlayVisibilityChanged below so its
                            // own screensaver-hold logic (resolveAchievementsGame/System) only
                            // freezes the displayed game/system while that exact screen is
                            // genuinely on screen, matching these AnimatedVisibility conditions.
                            val retroAchievementsGameVisible =
                                showRetroAchievementsOverlay && mainScreenActive &&
                                    heldRetroAchievementsFabMode == RetroAchievementsFabMode.Game
                            val retroAchievementsSystemVisible =
                                showRetroAchievementsOverlay && mainScreenActive &&
                                    heldRetroAchievementsFabMode == RetroAchievementsFabMode.System
                            LaunchedEffect(retroAchievementsGameVisible) {
                                retroAchievementsViewModel.onOverlayVisibilityChanged(retroAchievementsGameVisible)
                            }
                            LaunchedEffect(retroAchievementsSystemVisible) {
                                systemGamesViewModel.onOverlayVisibilityChanged(retroAchievementsSystemVisible)
                            }

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

                            // Settings > Other Settings: close this app when ES-DE fires
                            // a quit event. Keyed on the toggle itself so flipping it off
                            // cancels the collection rather than leaving it running.
                            val closeCompanionOnQuitEnabled by produceState(initialValue = false) {
                                appContainer.observeCloseCompanionOnQuitEnabledUseCase().collect { value = it }
                            }
                            LaunchedEffect(closeCompanionOnQuitEnabled) {
                                if (closeCompanionOnQuitEnabled) {
                                    appContainer.observeEsdeQuitEventUseCase().collect {
                                        finishAndRemoveTask()
                                    }
                                }
                            }

                            // Settings > Other Settings: launch ES-DE on the other display
                            // as soon as Companion starts up. LaunchedEffect(Unit) - a
                            // one-shot check, not a continuous collection - since this
                            // should only fire once when this MAIN destination first
                            // composes (cold start, or right after onboarding finishes),
                            // not on every settings change.
                            val esdeLaunchContext = LocalContext.current
                            LaunchedEffect(Unit) {
                                if (appContainer.observeLaunchEsdeOnStartEnabledUseCase().first()) {
                                    AppLauncher.launch(
                                        context = esdeLaunchContext,
                                        packageName = ESDE_PACKAGE_NAME,
                                        displayId = SecondaryDisplayResolver.secondaryDisplayId(esdeLaunchContext),
                                    )
                                }
                            }

                            // Settings > UI Settings > FAB Control: which FabType occupies
                            // each screen corner. Settings stays reachable regardless via
                            // the long-press menu - see MainScreen.
                            val fabAssignments by produceState(initialValue = FabAssignments.Default) {
                                appContainer.observeFabAssignmentsUseCase().collect { value = it }
                            }

                            // Backs the Clock/SystemStatus/ClockAndSystemStatus FABs. Initial
                            // value shows nothing (every flag false/lowest tier) until the
                            // first real read arrives.
                            val initialSystemStatus =
                                SystemStatus(
                                    battery = BatteryStatus(BatteryTier.Low, isCharging = false),
                                    wifiConnected = false,
                                    bluetoothConnected = false,
                                )
                            val systemStatus by produceState(initialValue = initialSystemStatus) {
                                appContainer.observeSystemStatusUseCase().collect { value = it }
                            }
                            val bluetoothPermissionRequested by produceState(initialValue = true) {
                                appContainer.observeBluetoothPermissionRequestedUseCase().collect { value = it }
                            }
                            val bluetoothPermissionScope = rememberCoroutineScope()

                            // Reported live by whichever wide FAB (Clock/SystemStatus/
                            // ClockAndSystemStatus) occupies a top corner - see
                            // MusicFabContent's reservedWidth below, which reads this instead
                            // of assuming every FAB is CORNER_BUTTON_SIZE wide.
                            val measuredFabWidthsPx = remember { mutableStateMapOf<FabPosition, Int>() }

                            // Backs the Custom App FAB (FAB Control) - resolving its icon/
                            // label needs the full installed-apps list, same source the App
                            // Drawer/App Dock already use.
                            val installedApps by produceState(initialValue = emptyList<InstalledApp>()) {
                                appContainer.observeInstalledAppsUseCase().collect { value = it }
                            }

                            // Same shared this-screen/other-screen preference set the App
                            // Dock and App Drawer read/write - an app's remembered launch
                            // location is one global preference, not something the Custom
                            // App FAB tracks separately. A single tap always launches on
                            // whichever screen is currently preferred; a double tap flips
                            // the preference to the other screen and launches there (see
                            // CustomAppFabContent), mirroring AppDock's FilledDockSlot.
                            val otherScreenLaunchApps by produceState(initialValue = emptySet<String>()) {
                                appContainer.observeOtherScreenLaunchAppsUseCase().collect { value = it }
                            }
                            val fabLaunchLocationScope = rememberCoroutineScope()

                            fun recordFabLaunchLocation(
                                packageName: String,
                                location: LaunchLocation,
                            ) {
                                fabLaunchLocationScope.launch {
                                    val current = appContainer.observeOtherScreenLaunchAppsUseCase().first()
                                    val updated =
                                        if (location == LaunchLocation.OtherScreen) {
                                            current + packageName
                                        } else {
                                            current - packageName
                                        }
                                    appContainer.setOtherScreenLaunchAppsUseCase(updated)
                                }
                            }

                            // Tapping the FAB toggles this; the timer alone controls
                            // dismissal - it must not be re-derived from musicPlaybackState,
                            // since tapping the panel's own Pause button flips
                            // Playing -> Paused but shouldn't close the panel.
                            var musicControlsRevealed by remember { mutableStateOf(false) }

                            val musicTrack =
                                when (val state = musicPlaybackState) {
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
                                    delay(MUSIC_CONTROLS_AUTO_HIDE_DELAY_MS)
                                    musicControlsRevealed = false
                                }
                            }

                            LaunchedEffect(musicTrack?.filePath) {
                                if (musicTrack != null) musicControlsRevealed = true
                            }

                            val activeScreenBehavior =
                                when ((connectionState as? EsdeConnectionState.Connected)?.appState) {
                                    is AppState.PlayingGame -> gamePlayingBehavior
                                    is AppState.Screensaver -> screensaverBehavior
                                    else -> ScreenBehavior.Nothing
                                }
                            val activeDimPercent =
                                when ((connectionState as? EsdeConnectionState.Connected)?.appState) {
                                    is AppState.PlayingGame -> gamePlayingDimPercent
                                    is AppState.Screensaver -> screensaverDimPercent
                                    else -> 0
                                }

                            val isBrowsingGame =
                                (connectionState as? EsdeConnectionState.Connected)?.appState is AppState.BrowsingGame
                            val showVideoOverlay =
                                videoPlaybackEnabled &&
                                    isBrowsingGame &&
                                    mainScreenActive &&
                                    isActivityVisible

                            // Shows either because Game Playing Behavior is set to Manual
                            // (and the user hasn't dismissed it for this game), or because
                            // the Game Manual FAB was tapped - either way, only if a manual
                            // actually resolved for the current game. Resets below
                            // (LaunchedEffect(manualPdfPath)) whenever the resolved manual
                            // itself changes.
                            val autoShowGameManual =
                                activeScreenBehavior == ScreenBehavior.GameManual && !manualDismissed
                            val showGameManual =
                                (autoShowGameManual || manualViewerOpenedViaFab) && manualPdfPath != null

                            LaunchedEffect(manualPdfPath) {
                                manualDismissed = false
                                manualViewerOpenedViaFab = false
                            }

                            val autoBlackTrigger = activeScreenBehavior == ScreenBehavior.Black && !showGameManual
                            val isDimmed = activeScreenBehavior == ScreenBehavior.Dim && !showGameManual

                            LaunchedEffect(autoBlackTrigger) {
                                isBlanked = autoBlackTrigger
                            }

                            // Extracted so each corner's content can be handed to
                            // MainScreen as a slot rendered INSIDE its own
                            // gesture-handling Box (see topStartOverlay's kdoc in
                            // MainScreen.kt for why a sibling composed elsewhere, even one
                            // ordered to draw underneath the App Drawer, silently loses
                            // every touch to MainScreen's full-screen drag/tap detectors -
                            // Compose only gives a descendant the touch-priority edge, not
                            // a same-level sibling). Still gating on showEditWidgets isn't
                            // needed here since this is only ever passed to the `else`
                            // branch below, where it's already false. Dispatches on
                            // whichever FabType (Settings > UI Settings > FAB Control)
                            // occupies `position` - Settings itself isn't handled here
                            // since its click handler is a local closure inside
                            // MainScreen, not something reachable from here (see
                            // MainScreen's own FabPosition.entries loop). The actual
                            // content per type is a top-level composable (see
                            // MusicFabContent/GameManualFabContent below this class) so
                            // this dispatcher itself stays a short, single-purpose `when`.
                            val bluetoothPermissionState =
                                BluetoothPermissionState(
                                    requested = bluetoothPermissionRequested,
                                    onRequested = {
                                        bluetoothPermissionScope.launch {
                                            appContainer.setBluetoothPermissionRequestedUseCase(true)
                                        }
                                    },
                                    onRecheck = { appContainer.bluetoothPermissionRecheckSignal.notifyChanged() },
                                )

                            // Shared by Clock/SystemStatus/ClockAndSystemStatus below - all
                            // three use the same visibility condition and width-reporting
                            // callback, unlike Music/GameManual/CustomApp which each differ.
                            fun wideFabContext(position: FabPosition) =
                                WideFabContext(
                                    position = position,
                                    visible = !isBlanked && isActivityVisible,
                                    overlayOpacityPercent = overlayOpacityPercent,
                                    onWidthMeasured = { px -> measuredFabWidthsPx[position] = px },
                                    onClick = { openSettingsMenuRequest++ },
                                )

                            fun fabSlotContent(position: FabPosition): @Composable BoxScope.() -> Unit =
                                {
                                    val slot = fabAssignments[position]
                                    when (slot.type) {
                                        FabType.Music ->
                                            MusicFabContent(
                                                position = position,
                                                fabAssignments = fabAssignments,
                                                measuredFabWidthsPx = measuredFabWidthsPx,
                                                visible =
                                                    !isBlanked && isActivityVisible && musicEnabled &&
                                                        musicPlaybackState != MusicPlaybackState.Stopped,
                                                controlsRevealed = musicControlsRevealed,
                                                onControlsRevealedChanged = { musicControlsRevealed = it },
                                                overlayOpacityPercent = overlayOpacityPercent,
                                                musicControlsViewModel = musicControlsViewModel,
                                            )
                                        FabType.GameManual ->
                                            GameManualFabContent(
                                                position = position,
                                                visible = !isBlanked && isActivityVisible && manualPdfPath != null,
                                                overlayOpacityPercent = overlayOpacityPercent,
                                                onClick = { manualViewerOpenedViaFab = true },
                                            )
                                        FabType.CustomApp ->
                                            CustomAppFabContent(
                                                position = position,
                                                packageName = slot.customAppPackageName,
                                                installedApps = installedApps,
                                                visible = !isBlanked && isActivityVisible,
                                                overlayOpacityPercent = overlayOpacityPercent,
                                                otherScreenLaunchApps = otherScreenLaunchApps,
                                                onRecordLaunchLocation = ::recordFabLaunchLocation,
                                            )
                                        FabType.RetroAchievements ->
                                            RetroAchievementsFabContent(
                                                position = position,
                                                visible =
                                                    !isBlanked && isActivityVisible &&
                                                        heldRetroAchievementsFabMode != RetroAchievementsFabMode.None,
                                                overlayOpacityPercent = overlayOpacityPercent,
                                                onClick = { showRetroAchievementsOverlay = true },
                                            )
                                        FabType.Clock -> ClockFabContent(wideFabContext(position))
                                        FabType.SystemStatus ->
                                            SystemStatusFabContent(
                                                context = wideFabContext(position),
                                                systemStatus = systemStatus,
                                                bluetoothPermission = bluetoothPermissionState,
                                            )
                                        FabType.ClockAndSystemStatus ->
                                            ClockAndSystemStatusFabContent(
                                                context = wideFabContext(position),
                                                systemStatus = systemStatus,
                                                bluetoothPermission = bluetoothPermissionState,
                                            )
                                        FabType.Settings, FabType.AppDrawer, FabType.None -> {}
                                    }
                                }

                            // Blurs everything behind the long-press menu (the widget
                            // backdrop, and MainScreen's own dock/corner buttons) while it's
                            // open - animated rather than snapping, so it settles in step
                            // with the menu's own entrance animation. The menu itself is
                            // unaffected: Popup renders in its own platform window, entirely
                            // outside this Box's render tree, so blurring this Box can never
                            // blur it too. Deliberately not applied while a folder is open -
                            // see folderOpen's kdoc above.
                            val longPressMenuBlurRadius by animateDpAsState(
                                targetValue = if (longPressMenuOpen) LONG_PRESS_MENU_BLUR_RADIUS else 0.dp,
                                label = "longPressMenuBlur",
                            )

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .blur(longPressMenuBlurRadius),
                            ) {
                                WidgetOverlay(viewModel = widgetsViewModel, modifier = Modifier.fillMaxSize())

                                // Full-content swap (not an overlay appearing over
                                // existing content), so Crossfade rather than
                                // AnimatedVisibility - a hard cut here read jarringly
                                // different from every other menu transition in the app.
                                Crossfade(targetState = showEditWidgets, label = "editWidgetsSwap") { isEditing ->
                                    if (isEditing) {
                                        val editWidgetsViewModel: EditWidgetsViewModel =
                                            viewModel(factory = EditWidgetsViewModelFactory(appContainer))
                                        EditWidgetsOverlay(
                                            viewModel = editWidgetsViewModel,
                                            initialCanvas = editWidgetsInitialCanvas,
                                            onDone = { showEditWidgets = false },
                                        )
                                    } else {
                                        MainScreen(
                                            appDrawerViewModel = appDrawerViewModel,
                                            dockViewModel = dockViewModel,
                                            settingsViewModel = settingsViewModel,
                                            manageAppsViewModel = manageAppsViewModel,
                                            autoFpsTriggerAppsViewModel = autoFpsTriggerAppsViewModel,
                                            taskKillerExcludedAppsViewModel = taskKillerExcludedAppsViewModel,
                                            updateViewModel = updateViewModel,
                                            fabAssignments = fabAssignments,
                                            overlayOpacityPercent = overlayOpacityPercent,
                                            onOpenEditWidgets = { showEditWidgets = true },
                                            onToggleBlankScreen = { isBlanked = !isBlanked },
                                            onQuitClick = { finishAndRemoveTask() },
                                            onDrawerOpenChanged = { drawerOpen = it },
                                            onLongPressMenuOpenChanged = { longPressMenuOpen = it },
                                            onFolderOpenChanged = { folderOpen = it },
                                            openSettingsMenuRequest = openSettingsMenuRequest,
                                            topStartOverlay = fabSlotContent(FabPosition.TopStart),
                                            topEndOverlay = fabSlotContent(FabPosition.TopEnd),
                                            bottomStartOverlay = fabSlotContent(FabPosition.BottomStart),
                                            bottomEndOverlay = fabSlotContent(FabPosition.BottomEnd),
                                        )
                                    }
                                }

                                // Automatic Dim (Settings > UI Settings: Game Playing /
                                // Screensaver Behavior). Purely visual - no clickable or
                                // pointerInput, so touches pass straight through to
                                // whatever's underneath, unlike the Black cover below.
                                AnimatedVisibility(
                                    visible = isDimmed && mainScreenActive,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = activeDimPercent / 100f)),
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
                                AnimatedVisibility(
                                    visible = isBlanked && mainScreenActive,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
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
                                AnimatedVisibility(
                                    visible = showGameManual && mainScreenActive,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    GameManualScreen(
                                        viewModel = gameManualViewModel,
                                        onExit = {
                                            manualDismissed = true
                                            manualViewerOpenedViaFab = false
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }

                                // RetroAchievements (RetroAchievements FAB) - same
                                // placement/guard as GameManual above, opened/closed purely
                                // via showRetroAchievementsOverlay (no automatic Game
                                // Playing Behavior-style trigger in this phase). Which of
                                // these two screens is actually visible is decided live by
                                // heldRetroAchievementsFabMode, not frozen at tap time, so the
                                // overlay flips from one to the other if the browsed
                                // system/game changes while it's still open.
                                //
                                // The two screens below are each their own AnimatedVisibility,
                                // fading independently - during a Game<->System handoff both
                                // are simultaneously mid-fade (one fading out, one fading in),
                                // so their combined opacity dips below 100% for the whole
                                // crossfade, letting WidgetOverlay's live canvas bleed through
                                // underneath at partial opacity. This backdrop sits beneath
                                // both, sharing their "overlay is up" visibility condition
                                // (which does NOT change on a Game<->System handoff, only on
                                // open/close), so it stays fully opaque for the entire handoff
                                // and only fades with the screens on genuine open/close. Uses
                                // the same fallback asset both screens already paint as their
                                // own base layer, so any residual blend during open/close reads
                                // as "the same background", not a mismatched flash.
                                AnimatedVisibility(
                                    visible =
                                        showRetroAchievementsOverlay && mainScreenActive &&
                                            heldRetroAchievementsFabMode != RetroAchievementsFabMode.None,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    AsyncImage(
                                        model = fallbackBackgroundAssetPath(LocalIsDarkTheme.current),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }

                                AnimatedVisibility(
                                    visible = retroAchievementsGameVisible,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    RetroAchievementsScreen(
                                        viewModel = retroAchievementsViewModel,
                                        overlayOpacityPercent = overlayOpacityPercent,
                                        onExit = { showRetroAchievementsOverlay = false },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }

                                AnimatedVisibility(
                                    visible = retroAchievementsSystemVisible,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    RetroAchievementsSystemGamesScreen(
                                        viewModel = systemGamesViewModel,
                                        overlayOpacityPercent = overlayOpacityPercent,
                                        onExit = { showRetroAchievementsOverlay = false },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }

                                if (showVideoOverlay) {
                                    videoPath?.let { resolvedVideoPath ->
                                        val videoPlaybackStateRepository = appContainer.videoPlaybackStateRepository
                                        VideoOverlayScreen(
                                            videoPath = resolvedVideoPath,
                                            delaySeconds = videoDelaySeconds,
                                            audioEnabled = videoAudioEnabled,
                                            modifier = Modifier.fillMaxSize(),
                                            onPlaybackEvent = { event ->
                                                when (event) {
                                                    is VideoPlaybackEvent.PlayingChanged ->
                                                        videoPlaybackStateRepository.setIsPlaying(event.isPlaying)
                                                    VideoPlaybackEvent.Started ->
                                                        appContainer.logVideoPlaybackStarted(resolvedVideoPath)
                                                    is VideoPlaybackEvent.Error ->
                                                        appContainer.logVideoPlaybackError(
                                                            resolvedVideoPath,
                                                            event.message,
                                                        )
                                                }
                                            },
                                        )
                                    }
                                }

                                // Update checker dialogs (silent startup check + manual
                                // "Check for Updates" in Settings > Setup both funnel into
                                // this same showUpdateDialog flag). Placement here isn't
                                // functionally load-bearing - AlertDialog renders into its
                                // own window, same reason LongPressSettingsMenu's Popup
                                // doesn't need special z-ordering either.
                                if (updateUiState.showUpdateDialog) {
                                    updateUiState.availableRelease?.let { release ->
                                        UpdateAvailableDialog(
                                            release = release,
                                            downloadState = updateUiState.downloadState,
                                            onDownloadAndInstall = updateViewModel::onDownloadAndInstallClicked,
                                            onDismiss = updateViewModel::onUpdateDialogDismissed,
                                        )
                                    }
                                }
                                if (updateUiState.showWhatsNewDialog) {
                                    updateUiState.whatsNewRelease?.let { release ->
                                        WhatsNewDialog(
                                            release = release,
                                            onDismiss = updateViewModel::onWhatsNewDialogDismissed,
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

/**
 * Content for a corner assigned [FabType.Music] (see MainActivity's fabSlotContent) -
 * a FAB that toggles [MusicControlsOverlay]'s expanded track/controls panel. Position-aware:
 * aligns to whichever corner it's in, and expands toward the screen's inner edge (padding
 * from `start` at [FabPosition.TopStart], from `end` at [FabPosition.TopEnd]) so the panel
 * never runs off the opposite side of the screen, reserving width for whatever FAB (if any)
 * occupies the opposite top corner. At [FabPosition.TopEnd] this means the panel itself is
 * right-aligned (`Alignment.TopEnd` + `padding(end = expansionGap)` pins its trailing edge
 * a fixed gap from the FAB's own leading edge, so both sit flush against the screen's right
 * side as one visual unit) and grows leftward, the mirror image of the TopStart case rather
 * than always expanding rightward regardless of which corner the FAB is actually in.
 *
 * [measuredFabWidthsPx] holds the live-measured width (see Clock/SystemStatus/
 * ClockAndSystemStatusFabContent's onWidthMeasured) of whatever wide FAB currently occupies
 * a top corner - falls back to the fixed CORNER_BUTTON_SIZE assumption below for every other
 * FAB type, which is correct since they're all fixed-size squares. Without this, a wide FAB
 * in the opposite corner would be under-reserved-for and the panel would overlap it.
 */
@Composable
private fun BoxScope.MusicFabContent(
    position: FabPosition,
    fabAssignments: FabAssignments,
    measuredFabWidthsPx: Map<FabPosition, Int>,
    visible: Boolean,
    controlsRevealed: Boolean,
    onControlsRevealedChanged: (Boolean) -> Unit,
    overlayOpacityPercent: Int,
    musicControlsViewModel: MusicControlsViewModel,
) {
    if (!visible) return

    // BoxWithConstraints (not a plain fillMaxSize Box) so the overlay's max width below can
    // be computed from the actual available screen width.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val alignment = position.toAlignment()
        val opposite = position.topOpposite
        val density = LocalDensity.current
        val reservedWidth =
            if (opposite == null || fabAssignments[opposite].type == FabType.None) {
                0.dp
            } else {
                val measuredPx = measuredFabWidthsPx[opposite]
                val oppositeWidth = if (measuredPx != null) with(density) { measuredPx.toDp() } else CORNER_BUTTON_SIZE
                CORNER_BUTTON_EDGE_PADDING + oppositeWidth
            }
        val expansionGap = CORNER_BUTTON_EDGE_PADDING + CORNER_BUTTON_SIZE + MUSIC_OVERLAY_GAP

        CornerFab(
            onClick = { onControlsRevealedChanged(!controlsRevealed) },
            opacityPercent = overlayOpacityPercent,
            modifier = Modifier.align(alignment).padding(CORNER_BUTTON_EDGE_PADDING),
        ) {
            Icon(imageVector = Icons.Filled.MusicNote, contentDescription = "Music controls")
        }

        val overlayMaxWidth = maxWidth - expansionGap - reservedWidth
        val overlayPadding =
            if (position == FabPosition.TopStart) {
                Modifier.padding(start = expansionGap, top = CORNER_BUTTON_EDGE_PADDING)
            } else {
                Modifier.padding(end = expansionGap, top = CORNER_BUTTON_EDGE_PADDING)
            }
        // align/padding/widthIn must live on AnimatedVisibility's own modifier, not on a
        // composable nested inside its content lambda - Box only reads alignment
        // parent-data from its direct children, and AnimatedVisibility (not its content)
        // is that direct child here. Putting align() further down silently falls back to
        // Box's default TopStart regardless of `position` - confirmed on-device (the panel
        // stuck to the screen's left edge and covered the Top Left FAB even with this FAB
        // set to Top Right). Hoisted to a local val, same ktlintFormat/detekt Indentation
        // mismatch workaround as elsewhere in this file.
        val animatedVisibilityModifier =
            Modifier
                .align(alignment)
                .then(overlayPadding)
                .widthIn(max = overlayMaxWidth.coerceAtLeast(0.dp))
        AnimatedVisibility(
            visible = controlsRevealed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = animatedVisibilityModifier,
        ) {
            MusicControlsOverlay(
                viewModel = musicControlsViewModel,
                opacityPercent = overlayOpacityPercent,
                // min, not exact - a wrapped two-line title (see MusicControlsOverlay)
                // needs to grow taller than the FAB, not be clipped to match it.
                modifier = Modifier.heightIn(min = CORNER_BUTTON_SIZE),
            )
        }
    }
}

/**
 * Content for a corner assigned [FabType.GameManual] (see MainActivity's fabSlotContent) -
 * a FAB shown whenever the current game has a resolved manual, opening the Game Manual
 * viewer on tap (see MainActivity's manualViewerOpenedViaFab).
 */
@Composable
private fun BoxScope.GameManualFabContent(
    position: FabPosition,
    visible: Boolean,
    overlayOpacityPercent: Int,
    onClick: () -> Unit,
) {
    if (!visible) return
    CornerFab(
        onClick = onClick,
        opacityPercent = overlayOpacityPercent,
        modifier = Modifier.align(position.toAlignment()).padding(CORNER_BUTTON_EDGE_PADDING),
    ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Game Manual")
    }
}

/**
 * Which of the two RetroAchievements screens the shared [FabType.RetroAchievements] FAB
 * currently points at - [Game] while browsing a game/screensaver/playing (see
 * RetroAchievementsViewModel.resolution), [System] while browsing a system's game list (see
 * RetroAchievementsSystemGamesViewModel.state), [None] when neither has anything to show
 * (e.g. signed out, or Idle). The two source states are naturally mutually exclusive - ES-DE
 * is never simultaneously "browsing a system" and "browsing/playing a game" - so this is a
 * plain priority pick, not a merge of overlapping signals. Note that mutually exclusive
 * *states* doesn't mean atomic *transitions* between them - see
 * [rememberHeldRetroAchievementsFabMode].
 */
private enum class RetroAchievementsFabMode { Game, System, None }

/**
 * Holds the previous non-[RetroAchievementsFabMode.None] mode for
 * [RETRO_ACHIEVEMENTS_FAB_MODE_NONE_GRACE_PERIOD_MILLIS] before actually reporting
 * [RetroAchievementsFabMode.None], rather than propagating a momentary None straight
 * through to the UI.
 *
 * `RetroAchievementsViewModel` and `RetroAchievementsSystemGamesViewModel` are independent,
 * each resolving asynchronously (a candidate-list/ROM-hash lookup, or a system's cached game
 * list) - on a live system<->game navigation, the side losing context drops to "no content"
 * synchronously (its ViewModel sees the game/system disappear from AppState) while the side
 * gaining context is still mid-resolve, so [target] genuinely is
 * [RetroAchievementsFabMode.None] for one or more frames even though the transition is
 * expected to land on a real mode shortly after. Without this hold, that frame both hides
 * both RetroAchievements screens (MainActivity's two `AnimatedVisibility` blocks require a
 * mode match) and permanently auto-dismisses the overlay (see MainActivity's
 * `LaunchedEffect` keyed on this value) - exposing the widget canvas underneath and
 * requiring the FAB to be tapped again to reopen. Confirmed on-device: the backdrop-image
 * flash this was mistaken for during a WidgetOverlay fix turned out to be this instead, once
 * that fix didn't resolve it.
 *
 * A genuine "nothing to show" (signed out, Idle, both screens truly inapplicable) still
 * resolves to [RetroAchievementsFabMode.None] once the grace period elapses with no
 * non-None [target] having arrived.
 */
@Composable
private fun rememberHeldRetroAchievementsFabMode(target: RetroAchievementsFabMode): RetroAchievementsFabMode {
    var held by remember { mutableStateOf(target) }
    LaunchedEffect(target) {
        if (target != RetroAchievementsFabMode.None) {
            held = target
            return@LaunchedEffect
        }
        delay(RETRO_ACHIEVEMENTS_FAB_MODE_NONE_GRACE_PERIOD_MILLIS)
        held = RetroAchievementsFabMode.None
    }
    return held
}

private const val RETRO_ACHIEVEMENTS_FAB_MODE_NONE_GRACE_PERIOD_MILLIS = 500L

/**
 * Content for a corner assigned [FabType.RetroAchievements] (see MainActivity's
 * fabSlotContent) - one FAB, contextually aware of `RetroAchievementsFabMode`: opens
 * [RetroAchievementsScreen] for the current game or [RetroAchievementsSystemGamesScreen]
 * for the current system, whichever currently applies. Same trophy icon in both contexts -
 * the FAB doesn't hint at which screen a tap opens, since that already flips live once the
 * screen itself is open (see MainActivity's `retroAchievementsFabMode` usage below).
 */
@Composable
private fun BoxScope.RetroAchievementsFabContent(
    position: FabPosition,
    visible: Boolean,
    overlayOpacityPercent: Int,
    onClick: () -> Unit,
) {
    if (!visible) return
    CornerFab(
        onClick = onClick,
        opacityPercent = overlayOpacityPercent,
        modifier = Modifier.align(position.toAlignment()).padding(CORNER_BUTTON_EDGE_PADDING),
    ) {
        Icon(imageVector = Icons.Filled.EmojiEvents, contentDescription = "RetroAchievements")
    }
}

/**
 * Content for a corner assigned [FabType.CustomApp] (see MainActivity's fabSlotContent) -
 * a FAB showing the selected app's own icon. Renders nothing if [packageName] is null (
 * Custom App selected but no app chosen yet in Settings > UI Settings > FAB Control) or the
 * app's since been uninstalled out from under a stale selection.
 *
 * Same this-screen/other-screen launch convention (and remembered preference) as the App
 * Dock and App Drawer: a single tap always launches on whichever screen [packageName] is
 * currently preferred on (per [otherScreenLaunchApps], the same shared preference set both
 * of those surfaces read/write), and a double tap flips that preference to the other
 * screen - via [onRecordLaunchLocation] - and launches there.
 */
@Composable
private fun BoxScope.CustomAppFabContent(
    position: FabPosition,
    packageName: String?,
    installedApps: List<InstalledApp>,
    visible: Boolean,
    overlayOpacityPercent: Int,
    otherScreenLaunchApps: Set<String>,
    onRecordLaunchLocation: (packageName: String, location: LaunchLocation) -> Unit,
) {
    if (!visible || packageName == null) return
    val app = installedApps.firstOrNull { it.packageName == packageName } ?: return
    val context = LocalContext.current
    val icon by produceState<Any?>(initialValue = null, key1 = packageName) {
        value = AppIconLoader.loadIcon(context, packageName)
    }
    val isOtherScreenPreferred = packageName in otherScreenLaunchApps
    CornerFab(
        onClick = {
            val displayId = if (isOtherScreenPreferred) SecondaryDisplayResolver.secondaryDisplayId(context) else null
            AppLauncher.launch(context, packageName, displayId = displayId)
        },
        onDoubleClick = {
            if (isOtherScreenPreferred) {
                onRecordLaunchLocation(packageName, LaunchLocation.ThisScreen)
                AppLauncher.launch(context, packageName)
            } else {
                val secondaryDisplayId = SecondaryDisplayResolver.secondaryDisplayId(context)
                if (secondaryDisplayId != null) {
                    onRecordLaunchLocation(packageName, LaunchLocation.OtherScreen)
                }
                AppLauncher.launch(context, packageName, displayId = secondaryDisplayId)
            }
        },
        opacityPercent = overlayOpacityPercent,
        modifier = Modifier.align(position.toAlignment()).padding(CORNER_BUTTON_EDGE_PADDING),
    ) {
        // Same indicator dot (size/color/placement) as AppDock's FilledDockSlot, so an
        // app's other-screen preference reads identically wherever it's shown.
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(model = icon, contentDescription = app.label, modifier = Modifier.size(CUSTOM_APP_ICON_SIZE))
            if (isOtherScreenPreferred) {
                val dotModifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(10.dp)
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                Box(modifier = dotModifier)
            }
        }
    }
}
