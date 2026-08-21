package com.esde.companion.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.esde.companion.BuildConfig
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.data.thor.ThorAccessibilityPermission
import com.esde.companion.data.thor.isAynThorDevice
import com.esde.companion.ui.settings.AppDrawerSettingsContent
import com.esde.companion.ui.settings.AutoFpsSettingsState
import com.esde.companion.ui.settings.LidWakeGuardSettingsState
import com.esde.companion.ui.settings.ManageAppsScreen
import com.esde.companion.ui.settings.ManageAppsViewModel
import com.esde.companion.ui.settings.OtherSettingsContent
import com.esde.companion.ui.settings.SettingsCategory
import com.esde.companion.ui.settings.SettingsCategoryRow
import com.esde.companion.ui.settings.SettingsQuitRow
import com.esde.companion.ui.settings.SettingsViewModel
import com.esde.companion.ui.settings.SetupSettingsContent
import com.esde.companion.ui.settings.SoundSettingsContent
import com.esde.companion.ui.settings.TaskKillerSettingsState
import com.esde.companion.ui.settings.ThorSettingsContent
import com.esde.companion.ui.settings.UISettingsContent
import com.esde.companion.ui.settings.VideoPlaybackSettingsContent
import com.esde.companion.ui.settings.VolumeSyncSettingsState
import com.esde.companion.ui.settings.WidgetsSettingsContent
import com.esde.companion.ui.theme.LocalIsDarkTheme
import com.esde.companion.ui.thor.AutoFpsTriggerAppsScreen
import com.esde.companion.ui.thor.AutoFpsTriggerAppsViewModel
import com.esde.companion.ui.thor.TaskKillerExcludedAppsScreen
import com.esde.companion.ui.thor.TaskKillerExcludedAppsViewModel
import com.esde.companion.ui.update.UpdateViewModel
import com.esde.companion.ui.widgets.fallbackBackgroundAssetPath
import kotlinx.coroutines.launch

/** Number of consecutive taps on the version label (see SettingsMenuHome) required to
 * trigger the Easter egg below - not exposed anywhere, deliberately undiscoverable short
 * of trying it. */
private const val EASTER_EGG_TAP_THRESHOLD = 7

private val EasterEggMessages =
    listOf(
        "It's dangerous to go alone! Take this.",
        "Hey! Listen!",
        "Do a barrel roll!",
        "Snake? Snake?! SNAAAAAKE!",
        "The cake is a lie.",
        "Stay a while and listen.",
        "Praise the Sun!",
        "War. War never changes.",
        "Would you kindly...",
        "Finish the fight.",
        "Rip and tear!",
        "You must construct additional pylons.",
        "Gotta go fast!",
        "A winner is you!",
        "Wake up, we've got a city to burn.",
        "The princess is in another castle.",
        "Get over here!",
        "There is no cow level.",
        "Wake me. When you need me.",
    )

/**
 * The menu's single navigation state - Home (the [SettingsCategory] list), a drilled-into
 * [Category] subpage, or [ManageApps] (reachable only from the AppDrawer category, hence
 * it carries [ManageApps.fromCategory] so back-navigation returns to that same subpage
 * rather than jumping all the way to Home). Replaces the old separate
 * `selectedCategory`/`showManageApps` booleans with one state so all three pages can share
 * a single [AnimatedContent] transition below instead of ManageApps being a plain
 * un-animated `if` sitting beside it.
 */
private sealed interface MenuPage {
    data object Home : MenuPage

    data class Category(val category: SettingsCategory) : MenuPage

    data class ManageApps(val fromCategory: SettingsCategory) : MenuPage

    /** Reachable only from [SettingsCategory.Thor]'s Auto FPS Mode panel - same
     * fromCategory-carrying shape as [ManageApps], for the same back-navigation reason. */
    data class AutoFpsTriggerApps(val fromCategory: SettingsCategory) : MenuPage

    /** Reachable only from [SettingsCategory.Thor]'s Task Killer panel - same shape as
     * [AutoFpsTriggerApps]. */
    data class TaskKillerExcludedApps(val fromCategory: SettingsCategory) : MenuPage
}

/** Home = 0, a Category subpage = 1, ManageApps/AutoFpsTriggerApps/TaskKillerExcludedApps = 2 -
 * used purely to pick the [AnimatedContent] slide direction below (drilling deeper slides in
 * from the right, stepping back slides in from the left), the same "how many levels deep"
 * comparison the old two-state version made with a plain boolean. */
private val MenuPage.depth: Int
    get() =
        when (this) {
            MenuPage.Home -> 0
            is MenuPage.Category -> 1
            is MenuPage.ManageApps -> 2
            is MenuPage.AutoFpsTriggerApps -> 2
            is MenuPage.TaskKillerExcludedApps -> 2
        }

/**
 * Full body of the long-press popup - everything that used to be the standalone
 * `SettingsScreen` destination, now hosted inside `MainScreen`'s `Popup` instead. Shows a
 * home page (the [SettingsCategory] list) and drills into a subpage for whichever category
 * is selected, exactly the same navigation `SettingsScreen` used - just with [onDismiss] as
 * the terminal "back" step instead of leaving a full-screen destination. [MenuPage] is
 * plain `remember`, not `rememberSaveable`: this composable is only ever part of
 * composition while the popup is open, so it's naturally torn down (and its state reset to
 * [MenuPage.Home]) every time the popup closes and reopens.
 */
@Composable
fun LongPressSettingsMenu(
    settingsViewModel: SettingsViewModel,
    manageAppsViewModel: ManageAppsViewModel,
    autoFpsTriggerAppsViewModel: AutoFpsTriggerAppsViewModel,
    taskKillerExcludedAppsViewModel: TaskKillerExcludedAppsViewModel,
    updateViewModel: UpdateViewModel,
    onEditWidgetsClick: () -> Unit,
    onQuitClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()

    // Named locally so passing it into OtherSettingsContent/ThorSettingsContent below doesn't
    // push those calls' lines over the max line length.
    val onLaunchEsdeOnStartEnabledChanged = settingsViewModel::onLaunchEsdeOnStartEnabledChanged
    val onLidWakeGuardEnabledChanged = settingsViewModel::onLidWakeGuardEnabledChanged
    val onHallSensorCalibrationChanged = settingsViewModel::onHallSensorCalibrationChanged
    val onTaskKillerEnabledChanged = settingsViewModel::onTaskKillerEnabledChanged
    val onVolumeSyncEnabledChanged = settingsViewModel::onVolumeSyncEnabledChanged
    val onVolumeSyncModeChanged = settingsViewModel::onVolumeSyncModeChanged

    val currentOnRefresh = rememberUpdatedState(settingsViewModel::refreshPermissionState)
    val currentOnRefreshThorAccessibility = rememberUpdatedState(settingsViewModel::refreshThorAccessibilityGranted)
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    currentOnRefresh.value(AllFilesAccessPermission.isGranted())
                    currentOnRefreshThorAccessibility.value(ThorAccessibilityPermission.isEnabledInSettings(context))
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var page by remember { mutableStateOf<MenuPage>(MenuPage.Home) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val onEasterEggUnlocked: () -> Unit = {
        coroutineScope.launch { snackbarHostState.showSnackbar(EasterEggMessages.random()) }
    }
    // Result feedback for SetupSettingsContent's Backup & Restore action - same snackbar
    // the Easter egg above uses, just a different trigger.
    val onBackupMessage: (String) -> Unit = { message ->
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    // Steps back one level at a time, same chain SettingsScreen used - just closing the
    // whole popup (onDismiss) instead of leaving a full-screen destination as the last
    // step. Shared by the hardware BackHandler below and the header's back arrow, same as
    // SettingsScreen's single onBack was reused by both its BackHandler and its TopAppBar.
    val onBack: () -> Unit = {
        when (val current = page) {
            is MenuPage.ManageApps -> page = MenuPage.Category(current.fromCategory)
            is MenuPage.AutoFpsTriggerApps -> page = MenuPage.Category(current.fromCategory)
            is MenuPage.TaskKillerExcludedApps -> page = MenuPage.Category(current.fromCategory)
            is MenuPage.Category -> page = MenuPage.Home
            MenuPage.Home -> onDismiss()
        }
    }

    // Composed only while the popup is open, so this naturally takes priority over
    // MainScreenContent's own BackHandler for as long as it's mounted (Compose's back
    // dispatch is LIFO by composition order) - see MainScreenContent's own BackHandler,
    // which no longer needs a case for the long-press menu at all because of this.
    BackHandler(onBack = onBack)

    // Always shown, unlike a plain drill-down title - the top level reads "Main Menu" (the
    // popup's own identity) rather than having no header at all, matching how
    // SettingsScreen's TopAppBar looked before it was retired.
    val title =
        when (val current = page) {
            MenuPage.Home -> "Main Menu"
            is MenuPage.Category -> current.category.title
            is MenuPage.ManageApps -> "Manage Apps"
            is MenuPage.AutoFpsTriggerApps -> "Trigger Apps"
            is MenuPage.TaskKillerExcludedApps -> "Excluded Apps"
        }

    Box(modifier = modifier) {
        // Same fallback background image SettingsScreen used to render behind its
        // Scaffold - the popup's own Surface color is a solid theme color, not this
        // artwork, so it's redrawn here to keep that backdrop across every settings page.
        AsyncImage(
            model = fallbackBackgroundAssetPath(LocalIsDarkTheme.current),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            SettingsMenuHeader(title = title, onBack = onBack)

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        // Drilling deeper (Home -> Category -> ManageApps) slides in from
                        // the right; stepping back slides in from the left - mirrors
                        // typical drill-down navigation even though this isn't
                        // nav-compose under the hood. ManageApps now shares this same
                        // transition rather than swapping in via a plain unanimated `if`.
                        val enteringDeeper = targetState.depth > initialState.depth
                        val slideDistance = { width: Int -> width / 3 }
                        if (enteringDeeper) {
                            (slideInHorizontally(tween(220), slideDistance) + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally(tween(220)) { -slideDistance(it) } + fadeOut(tween(150)))
                        } else {
                            (slideInHorizontally(tween(220)) { -slideDistance(it) } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally(tween(220), slideDistance) + fadeOut(tween(150)))
                        }
                    },
                    label = "longPressSettingsContent",
                ) { targetPage ->
                    when (targetPage) {
                        MenuPage.Home ->
                            SettingsMenuHome(
                                onCategorySelected = { page = MenuPage.Category(it) },
                                onEditWidgetsClick = onEditWidgetsClick,
                                onQuitClick = onQuitClick,
                                onEasterEggUnlocked = onEasterEggUnlocked,
                            )

                        is MenuPage.ManageApps -> ManageAppsScreen(viewModel = manageAppsViewModel)

                        is MenuPage.AutoFpsTriggerApps ->
                            AutoFpsTriggerAppsScreen(viewModel = autoFpsTriggerAppsViewModel)

                        is MenuPage.TaskKillerExcludedApps ->
                            TaskKillerExcludedAppsScreen(viewModel = taskKillerExcludedAppsViewModel)

                        is MenuPage.Category ->
                            when (targetPage.category) {
                                SettingsCategory.Setup ->
                                    SetupSettingsContent(
                                        uiState = uiState,
                                        onLogFolderPicked = settingsViewModel::onLogFolderPicked,
                                        onMediaFolderPicked = settingsViewModel::onMediaFolderPicked,
                                        onCustomSystemImagesFolderPicked = settingsViewModel::onCustomSystemImagesFolderPicked,
                                        onCustomSystemImagesFolderCleared = settingsViewModel::onCustomSystemImagesFolderCleared,
                                        onCustomLogosFolderPicked = settingsViewModel::onCustomLogosFolderPicked,
                                        onCustomLogosFolderCleared = settingsViewModel::onCustomLogosFolderCleared,
                                        onCustomMusicFolderPicked = settingsViewModel::onCustomMusicFolderPicked,
                                        onCustomMusicFolderCleared = settingsViewModel::onCustomMusicFolderCleared,
                                        onExportBackup = settingsViewModel::exportBackupJson,
                                        onRestoreBackup = settingsViewModel::restoreBackup,
                                        onBackupMessage = onBackupMessage,
                                    )
                                SettingsCategory.UI ->
                                    UISettingsContent(
                                        themePreference = uiState.themePreference,
                                        onThemePreferenceChanged = settingsViewModel::onThemePreferenceChanged,
                                        overlayOpacityPercent = uiState.overlayOpacityPercent,
                                        onOverlayOpacityChanged = settingsViewModel::onOverlayOpacityChanged,
                                        gamePlayingBehavior = uiState.gamePlayingBehavior,
                                        onGamePlayingBehaviorChanged = settingsViewModel::onGamePlayingBehaviorChanged,
                                        gamePlayingDimPercent = uiState.gamePlayingDimPercent,
                                        onGamePlayingDimPercentChanged = settingsViewModel::onGamePlayingDimPercentChanged,
                                        screensaverBehavior = uiState.screensaverBehavior,
                                        onScreensaverBehaviorChanged = settingsViewModel::onScreensaverBehaviorChanged,
                                        screensaverDimPercent = uiState.screensaverDimPercent,
                                        onScreensaverDimPercentChanged = settingsViewModel::onScreensaverDimPercentChanged,
                                        fabAssignments = uiState.fabAssignments,
                                        installedApps = uiState.installedApps,
                                        onFabTypeChanged = settingsViewModel::onFabTypeChanged,
                                        onFabCustomAppChanged = settingsViewModel::onFabCustomAppChanged,
                                    )
                                SettingsCategory.VideoPlayback ->
                                    VideoPlaybackSettingsContent(
                                        videoPlaybackEnabled = uiState.videoPlaybackEnabled,
                                        onVideoPlaybackEnabledChanged = settingsViewModel::onVideoPlaybackEnabledChanged,
                                        videoDelaySeconds = uiState.videoDelaySeconds,
                                        onVideoDelaySecondsChanged = settingsViewModel::onVideoDelaySecondsChanged,
                                        videoAudioEnabled = uiState.videoAudioEnabled,
                                        onVideoAudioEnabledChanged = settingsViewModel::onVideoAudioEnabledChanged,
                                    )
                                SettingsCategory.AppDrawer ->
                                    AppDrawerSettingsContent(
                                        gridColumns = uiState.gridColumns,
                                        onGridColumnsChanged = settingsViewModel::onGridColumnsChanged,
                                        sortFoldersOnTop = uiState.sortFoldersOnTop,
                                        onSortFoldersOnTopChanged = settingsViewModel::onSortFoldersOnTopChanged,
                                        showSearchBar = uiState.showSearchBar,
                                        onShowSearchBarChanged = settingsViewModel::onShowSearchBarChanged,
                                        onManageAppsClick = { page = MenuPage.ManageApps(fromCategory = targetPage.category) },
                                        dockEnabled = uiState.dockEnabled,
                                        onDockEnabledChanged = settingsViewModel::onDockEnabledChanged,
                                        dockMaxApps = uiState.dockMaxApps,
                                        onDockMaxAppsChanged = settingsViewModel::onDockMaxAppsChanged,
                                        dockSize = uiState.dockSize,
                                        onDockSizeChanged = settingsViewModel::onDockSizeChanged,
                                    )
                                SettingsCategory.Sound ->
                                    SoundSettingsContent(
                                        musicEnabled = uiState.musicEnabled,
                                        onMusicEnabledChanged = settingsViewModel::onMusicEnabledChanged,
                                        musicPlayWhileBrowsingSystems = uiState.musicPlayWhileBrowsingSystems,
                                        onMusicPlayWhileBrowsingSystemsChanged = settingsViewModel::onMusicPlayWhileBrowsingSystemsChanged,
                                        musicPlayWhileBrowsingGames = uiState.musicPlayWhileBrowsingGames,
                                        onMusicPlayWhileBrowsingGamesChanged = settingsViewModel::onMusicPlayWhileBrowsingGamesChanged,
                                        musicPlayDuringScreensaver = uiState.musicPlayDuringScreensaver,
                                        onMusicPlayDuringScreensaverChanged = settingsViewModel::onMusicPlayDuringScreensaverChanged,
                                        musicDuckingMode = uiState.musicDuckingMode,
                                        onMusicDuckingModeChanged = settingsViewModel::onMusicDuckingModeChanged,
                                    )
                                SettingsCategory.Other ->
                                    OtherSettingsContent(
                                        updateCheckResult = updateUiState.lastManualCheckResult,
                                        isCheckingForUpdate = updateUiState.isCheckingForUpdate,
                                        onCheckForUpdatesClicked = updateViewModel::checkForUpdatesManually,
                                        closeCompanionOnQuitEnabled = uiState.closeCompanionOnQuitEnabled,
                                        onCloseCompanionOnQuitEnabledChanged = settingsViewModel::onCloseCompanionOnQuitEnabledChanged,
                                        launchEsdeOnStartEnabled = uiState.launchEsdeOnStartEnabled,
                                        onLaunchEsdeOnStartEnabledChanged = onLaunchEsdeOnStartEnabledChanged,
                                        debugLoggingEnabled = uiState.debugLoggingEnabled,
                                        onDebugLoggingEnabledChanged = settingsViewModel::onDebugLoggingEnabledChanged,
                                    )
                                SettingsCategory.Widgets -> WidgetsSettingsContent(onEditWidgetsClick = onEditWidgetsClick)
                                SettingsCategory.Thor ->
                                    ThorSettingsContent(
                                        lidWakeGuard =
                                            LidWakeGuardSettingsState(
                                                enabled = uiState.lidWakeGuardEnabled,
                                                accessibilityGranted = uiState.thorAccessibilityGranted,
                                                calibration = uiState.hallSensorCalibration,
                                                onEnabledChanged = onLidWakeGuardEnabledChanged,
                                                onCalibrationChanged = onHallSensorCalibrationChanged,
                                            ),
                                        autoFps =
                                            AutoFpsSettingsState(
                                                enabled = uiState.autoFpsEnabled,
                                                accessibilityGranted = uiState.thorAccessibilityGranted,
                                                privilegedServiceAvailable = uiState.thorPrivilegedServiceAvailable,
                                                onEnabledChanged = settingsViewModel::onAutoFpsEnabledChanged,
                                                onManageTriggerAppsClick = {
                                                    val category = targetPage.category
                                                    page = MenuPage.AutoFpsTriggerApps(fromCategory = category)
                                                },
                                            ),
                                        taskKiller =
                                            TaskKillerSettingsState(
                                                enabled = uiState.taskKillerEnabled,
                                                accessibilityGranted = uiState.thorAccessibilityGranted,
                                                privilegedServiceAvailable = uiState.thorPrivilegedServiceAvailable,
                                                onEnabledChanged = onTaskKillerEnabledChanged,
                                                onManageExcludedAppsClick = {
                                                    val category = targetPage.category
                                                    page = MenuPage.TaskKillerExcludedApps(fromCategory = category)
                                                },
                                            ),
                                        volumeSync =
                                            VolumeSyncSettingsState(
                                                enabled = uiState.volumeSyncEnabled,
                                                accessibilityGranted = uiState.thorAccessibilityGranted,
                                                privilegedServiceAvailable = uiState.thorPrivilegedServiceAvailable,
                                                secondarySettingPresent = uiState.volumeSyncSecondarySettingPresent,
                                                mode = uiState.volumeSyncMode,
                                                onEnabledChanged = onVolumeSyncEnabledChanged,
                                                onModeChanged = onVolumeSyncModeChanged,
                                            ),
                                    )
                            }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
        ) { data ->
            // Same reasoning as SettingsScreen's snackbar used to have: the stock
            // Snackbar always fills the offered max width regardless of how short its
            // text is, which reads as oversized for a one-off quip like the Easter egg
            // message - this plain styled Surface+Text wraps to its content instead.
            Surface(
                shape = SnackbarDefaults.shape,
                color = SnackbarDefaults.color,
                contentColor = SnackbarDefaults.contentColor,
                shadowElevation = 6.dp,
            ) {
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SettingsMenuHome(
    onCategorySelected: (SettingsCategory) -> Unit,
    onEditWidgetsClick: () -> Unit,
    onQuitClick: () -> Unit,
    onEasterEggUnlocked: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Thor Settings is only meaningful on an actual Ayn Thor device - the `when` branch
        // that routes to it above still exists unconditionally regardless (see
        // ThorSettingsContent's kdoc), this is purely a list-visibility filter.
        val visibleCategories = SettingsCategory.entries.filter { it != SettingsCategory.Thor || isAynThorDevice() }
        visibleCategories.forEach { category ->
            SettingsCategoryRow(
                category = category,
                // Widgets skips the subpage entirely - there's nothing else to show
                // there besides a single "Edit Widgets" entry, so tapping the row goes
                // straight to the editor instead of drilling in first.
                onClick = {
                    if (category == SettingsCategory.Widgets) onEditWidgetsClick() else onCategorySelected(category)
                },
            )
        }

        // Quitting is destructive/terminal (see SettingsQuitRow's kdoc) and easy to hit
        // by accident so close to the category list above it - a confirmation dialog
        // gates the actual onQuitClick call rather than firing it directly from the row.
        var showQuitConfirmation by remember { mutableStateOf(false) }
        SettingsQuitRow(onClick = { showQuitConfirmation = true })
        if (showQuitConfirmation) {
            QuitConfirmationDialog(
                onConfirm = {
                    showQuitConfirmation = false
                    onQuitClick()
                },
                onDismiss = { showQuitConfirmation = false },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // No visual tap affordance (indication = null) - same no-ripple pattern as
        // AppDock's no-op clickable - so this reads as plain text, not an obvious
        // button, keeping the Easter egg's presence undiscoverable short of trying it.
        var versionTapCount by remember { mutableIntStateOf(0) }
        val debugSuffix = if (BuildConfig.DEBUG) " (Debug)" else ""
        Text(
            text = "ES-DE Companion v${BuildConfig.VERSION_NAME}$debugSuffix",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            versionTapCount++
                            if (versionTapCount >= EASTER_EGG_TAP_THRESHOLD) {
                                versionTapCount = 0
                                onEasterEggUnlocked()
                            }
                        },
                    ),
        )
    }
}

@Composable
private fun QuitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Really Quit?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Yes") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("No") } },
    )
}
