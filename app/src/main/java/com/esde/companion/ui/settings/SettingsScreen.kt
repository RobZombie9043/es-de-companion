package com.esde.companion.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.BuildConfig
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.data.storage.SafPathResolver
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import kotlin.math.roundToInt

/**
 * Shared corner radius for the background panel behind every settings item (category
 * rows, folder settings, toggles, the theme picker) so they read as a consistent set of
 * cards rather than each item picking its own rounding.
 */
private val SettingsItemShape = RoundedCornerShape(16.dp)

/**
 * Settings entry point: shows a top-level list of [SettingsCategory] items, and drills
 * into a subpage for whichever one is selected. `selectedCategory == null` means the
 * category list is showing; back from a subpage returns to the list, back from the list
 * calls [onDone]. See [SettingsCategory] for why this is plain state rather than a nested
 * NavHost.
 *
 * The App Drawer category has a second level - Manage Apps - handled the same way via
 * `showManageApps`, rather than extending [SettingsCategory] itself, since Manage Apps
 * isn't a top-level category the user picks from the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    manageAppsViewModel: ManageAppsViewModel,
    onDone: () -> Unit,
    onEditWidgetsClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnRefresh = rememberUpdatedState(viewModel::refreshPermissionState)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnRefresh.value(AllFilesAccessPermission.isGranted())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }
    var showManageApps by rememberSaveable { mutableStateOf(false) }

    val onBack: () -> Unit = {
        when {
            showManageApps -> showManageApps = false
            selectedCategory != null -> selectedCategory = null
            else -> onDone()
        }
    }

    BackHandler(onBack = onBack)

    val title = when {
        showManageApps -> "Manage Apps"
        else -> selectedCategory?.title ?: "Settings"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (showManageApps) {
                ManageAppsScreen(viewModel = manageAppsViewModel)
            } else {
                AnimatedContent(
                    targetState = selectedCategory,
                    transitionSpec = {
                        // Drilling into a subpage slides in from the right; returning to the
                        // list slides in from the left - mirrors typical drill-down navigation
                        // even though this isn't nav-compose under the hood.
                        val enteringSubpage = targetState != null
                        val slideDistance = { width: Int -> width / 3 }
                        if (enteringSubpage) {
                            (slideInHorizontally(tween(220), slideDistance) + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally(tween(220)) { -slideDistance(it) } + fadeOut(tween(150)))
                        } else {
                            (slideInHorizontally(tween(220)) { -slideDistance(it) } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally(tween(220), slideDistance) + fadeOut(tween(150)))
                        }
                    },
                    label = "settingsContent",
                ) { category ->
                    when (category) {
                        null -> SettingsCategoryList(onCategorySelected = { selectedCategory = it })
                        SettingsCategory.Setup -> SetupSettingsContent(
                            uiState = uiState,
                            onLogFolderPicked = viewModel::onLogFolderPicked,
                            onMediaFolderPicked = viewModel::onMediaFolderPicked,
                            onCustomSystemImagesFolderPicked = viewModel::onCustomSystemImagesFolderPicked,
                            onCustomSystemImagesFolderCleared = viewModel::onCustomSystemImagesFolderCleared,
                            onCustomLogosFolderPicked = viewModel::onCustomLogosFolderPicked,
                            onCustomLogosFolderCleared = viewModel::onCustomLogosFolderCleared,
                            onCustomMusicFolderPicked = viewModel::onCustomMusicFolderPicked,
                            onCustomMusicFolderCleared = viewModel::onCustomMusicFolderCleared,
                        )
                        SettingsCategory.UI -> UISettingsContent(
                            themePreference = uiState.themePreference,
                            onThemePreferenceChanged = viewModel::onThemePreferenceChanged,
                            imageTransitionMode = uiState.imageTransitionMode,
                            onImageTransitionModeChanged = viewModel::onImageTransitionModeChanged,
                            logoTransitionMode = uiState.logoTransitionMode,
                            onLogoTransitionModeChanged = viewModel::onLogoTransitionModeChanged,
                            overlayOpacityPercent = uiState.overlayOpacityPercent,
                            onOverlayOpacityChanged = viewModel::onOverlayOpacityChanged,
                            gamePlayingBehavior = uiState.gamePlayingBehavior,
                            onGamePlayingBehaviorChanged = viewModel::onGamePlayingBehaviorChanged,
                            screensaverBehavior = uiState.screensaverBehavior,
                            onScreensaverBehaviorChanged = viewModel::onScreensaverBehaviorChanged,
                        )
                        SettingsCategory.VideoPlayback -> VideoPlaybackSettingsContent(
                            videoPlaybackEnabled = uiState.videoPlaybackEnabled,
                            onVideoPlaybackEnabledChanged = viewModel::onVideoPlaybackEnabledChanged,
                            videoDelaySeconds = uiState.videoDelaySeconds,
                            onVideoDelaySecondsChanged = viewModel::onVideoDelaySecondsChanged,
                            videoAudioEnabled = uiState.videoAudioEnabled,
                            onVideoAudioEnabledChanged = viewModel::onVideoAudioEnabledChanged,
                        )
                        SettingsCategory.Widgets -> WidgetsSettingsContent(
                            widgetsLocked = uiState.widgetsLocked,
                            onWidgetsLockedChanged = viewModel::onWidgetsLockedChanged,
                            onEditWidgetsClick = onEditWidgetsClick,
                        )
                        SettingsCategory.AppDrawer -> AppDrawerSettingsContent(
                            gridColumns = uiState.gridColumns,
                            onGridColumnsChanged = viewModel::onGridColumnsChanged,
                            onManageAppsClick = { showManageApps = true },
                            dockEnabled = uiState.dockEnabled,
                            onDockEnabledChanged = viewModel::onDockEnabledChanged,
                            dockMaxApps = uiState.dockMaxApps,
                            onDockMaxAppsChanged = viewModel::onDockMaxAppsChanged,
                            dockSize = uiState.dockSize,
                            onDockSizeChanged = viewModel::onDockSizeChanged,
                        )
                        SettingsCategory.Sound -> SoundSettingsContent(
                            musicEnabled = uiState.musicEnabled,
                            onMusicEnabledChanged = viewModel::onMusicEnabledChanged,
                            musicPlayWhileBrowsingSystems = uiState.musicPlayWhileBrowsingSystems,
                            onMusicPlayWhileBrowsingSystemsChanged = viewModel::onMusicPlayWhileBrowsingSystemsChanged,
                            musicPlayWhileBrowsingGames = uiState.musicPlayWhileBrowsingGames,
                            onMusicPlayWhileBrowsingGamesChanged = viewModel::onMusicPlayWhileBrowsingGamesChanged,
                            musicPlayDuringScreensaver = uiState.musicPlayDuringScreensaver,
                            onMusicPlayDuringScreensaverChanged = viewModel::onMusicPlayDuringScreensaverChanged,
                            musicDuckingMode = uiState.musicDuckingMode,
                            onMusicDuckingModeChanged = viewModel::onMusicDuckingModeChanged,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryList(onCategorySelected: (SettingsCategory) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsCategory.entries.forEach { category ->
            SettingsCategoryRow(category = category, onClick = { onCategorySelected(category) })
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "ES-DE Companion v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SettingsCategoryRow(category: SettingsCategory, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = category.description, style = MaterialTheme.typography.bodySmall)
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun SetupSettingsContent(
    uiState: SettingsUiState,
    onLogFolderPicked: (String) -> Unit,
    onMediaFolderPicked: (String) -> Unit,
    onCustomSystemImagesFolderPicked: (String) -> Unit,
    onCustomSystemImagesFolderCleared: () -> Unit,
    onCustomLogosFolderPicked: (String) -> Unit,
    onCustomLogosFolderCleared: () -> Unit,
    onCustomMusicFolderPicked: (String) -> Unit,
    onCustomMusicFolderCleared: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (!uiState.permissionGranted) {
            Text(
                "All files access isn't currently granted - folder changes below " +
                        "may not take effect until it's re-enabled in system Settings.",
            )
        }

        FolderSetting(
            label = "ES-DE folder",
            path = uiState.logFolderPath,
            isValidating = uiState.isValidatingLogFolder,
            statusText = uiState.logFolderValidation.toStatusText(),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onLogFolderPicked) },
        )

        FolderSetting(
            label = "Media folder",
            path = uiState.mediaFolderPath,
            isValidating = uiState.isValidatingMediaFolder,
            statusText = uiState.mediaFolderValidation.toStatusText(),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onMediaFolderPicked) },
        )

        OptionalFolderSetting(
            label = "Custom System Images Folder",
            path = uiState.customSystemImagesFolderPath,
            isValidating = uiState.isValidatingCustomSystemImagesFolder,
            statusText = uiState.customSystemImagesFolderValidation.toStatusText(),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onCustomSystemImagesFolderPicked) },
            onClear = onCustomSystemImagesFolderCleared,
        )

        OptionalFolderSetting(
            label = "Custom Logos Folder",
            path = uiState.customLogosFolderPath,
            isValidating = uiState.isValidatingCustomLogosFolder,
            statusText = uiState.customLogosFolderValidation.toStatusText(),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onCustomLogosFolderPicked) },
            onClear = onCustomLogosFolderCleared,
        )

        OptionalFolderSetting(
            label = "Custom Music Folder",
            path = uiState.customMusicFolderPath,
            isValidating = uiState.isValidatingCustomMusicFolder,
            statusText = uiState.customMusicFolderValidation.toStatusText(),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onCustomMusicFolderPicked) },
            onClear = onCustomMusicFolderCleared,
        )
    }
}

@Composable
private fun UISettingsContent(
    themePreference: ThemePreference,
    onThemePreferenceChanged: (ThemePreference) -> Unit,
    imageTransitionMode: ImageTransitionMode,
    onImageTransitionModeChanged: (ImageTransitionMode) -> Unit,
    logoTransitionMode: LogoTransitionMode,
    onLogoTransitionModeChanged: (LogoTransitionMode) -> Unit,
    overlayOpacityPercent: Int,
    onOverlayOpacityChanged: (Int) -> Unit,
    gamePlayingBehavior: ScreenBehavior,
    onGamePlayingBehaviorChanged: (ScreenBehavior) -> Unit,
    screensaverBehavior: ScreenBehavior,
    onScreensaverBehaviorChanged: (ScreenBehavior) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ThemePicker(selected = themePreference, onSelected = onThemePreferenceChanged)
        OverlayOpacitySetting(percent = overlayOpacityPercent, onPercentChanged = onOverlayOpacityChanged)
        ImageTransitionPicker(selected = imageTransitionMode, onSelected = onImageTransitionModeChanged)
        LogoTransitionPicker(selected = logoTransitionMode, onSelected = onLogoTransitionModeChanged)
        ScreenBehaviorPicker(
            title = "Game Playing Screen Behavior",
            options = listOf(ScreenBehavior.Nothing, ScreenBehavior.Dim, ScreenBehavior.Black, ScreenBehavior.GameManual),
            selected = gamePlayingBehavior,
            onSelected = onGamePlayingBehaviorChanged,
        )
        ScreenBehaviorPicker(
            title = "Screensaver Screen Behavior",
            options = listOf(ScreenBehavior.Nothing, ScreenBehavior.Dim, ScreenBehavior.Black),
            selected = screensaverBehavior,
            onSelected = onScreensaverBehaviorChanged,
        )
    }
}

/**
 * Master background opacity for every translucent overlay surface - the App Drawer, the
 * App Dock, the music controls panel, and the Settings/music-FAB/Edit-Widgets corner
 * buttons - see OnboardingRepository.observeOverlayOpacityPercent's kdoc for why this
 * replaced a separate slider per surface.
 */
@Composable
private fun OverlayOpacitySetting(percent: Int, onPercentChanged: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Overlay Opacity",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = "$percent%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = percent.toFloat(),
                onValueChange = { onPercentChanged(it.roundToInt()) },
                valueRange = 0f..100f,
            )
        }
    }
}

@Composable
private fun VideoPlaybackSettingsContent(
    videoPlaybackEnabled: Boolean,
    onVideoPlaybackEnabledChanged: (Boolean) -> Unit,
    videoDelaySeconds: Int,
    onVideoDelaySecondsChanged: (Int) -> Unit,
    videoAudioEnabled: Boolean,
    onVideoAudioEnabledChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        VideoPlaybackEnabledSetting(enabled = videoPlaybackEnabled, onEnabledChanged = onVideoPlaybackEnabledChanged)
        if (videoPlaybackEnabled) {
            VideoDelaySetting(delaySeconds = videoDelaySeconds, onDelaySecondsChanged = onVideoDelaySecondsChanged)
            VideoAudioSetting(enabled = videoAudioEnabled, onEnabledChanged = onVideoAudioEnabledChanged)
        }
    }
}

@Composable
private fun VideoPlaybackEnabledSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Background Video",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Play game videos while browsing",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = enabled, onCheckedChange = onEnabledChanged)
            }
        }
    }
}

@Composable
private fun VideoDelaySetting(delaySeconds: Int, onDelaySecondsChanged: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Video Start Delay",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (delaySeconds == 0) "Off" else "${delaySeconds}s",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = delaySeconds.toFloat(),
                onValueChange = { onDelaySecondsChanged(it.roundToInt()) },
                valueRange = 0f..10f,
                steps = 9,
            )
        }
    }
}

@Composable
private fun VideoAudioSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Video Audio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Enable video audio",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = enabled, onCheckedChange = onEnabledChanged)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenBehaviorPicker(
    title: String,
    options: List<ScreenBehavior>,
    selected: ScreenBehavior,
    onSelected: (ScreenBehavior) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, behavior ->
                    SegmentedButton(
                        selected = behavior == selected,
                        onClick = { onSelected(behavior) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size,
                        ),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = behavior == selected) {
                                Icon(
                                    imageVector = behavior.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        },
                        label = { Text(behavior.label) },
                    )
                }
            }
        }
    }
}

// Presentation-only icon/label, same reasoning as ThemePreference.icon/label above.
private val ScreenBehavior.icon: ImageVector
    get() = when (this) {
        ScreenBehavior.Nothing -> Icons.Filled.Brightness7
        ScreenBehavior.Dim -> Icons.Filled.Brightness4
        ScreenBehavior.Black -> Icons.Filled.Brightness1
        ScreenBehavior.GameManual -> Icons.Filled.MenuBook
    }

private val ScreenBehavior.label: String
    get() = when (this) {
        ScreenBehavior.Nothing -> "On"
        ScreenBehavior.Dim -> "Dimmed"
        ScreenBehavior.Black -> "Off"
        ScreenBehavior.GameManual -> "Manual"
    }

@Composable
private fun AppDrawerSettingsContent(
    gridColumns: Int,
    onGridColumnsChanged: (Int) -> Unit,
    onManageAppsClick: () -> Unit,
    dockEnabled: Boolean,
    onDockEnabledChanged: (Boolean) -> Unit,
    dockMaxApps: Int,
    onDockMaxAppsChanged: (Int) -> Unit,
    dockSize: DockSize,
    onDockSizeChanged: (DockSize) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ManageAppsEntry(onClick = onManageAppsClick)
        GridColumnsSetting(columns = gridColumns, onColumnsChanged = onGridColumnsChanged)
        DockEnabledSetting(enabled = dockEnabled, onEnabledChanged = onDockEnabledChanged)
        if (dockEnabled) {
            DockMaxAppsSetting(maxApps = dockMaxApps, onMaxAppsChanged = onDockMaxAppsChanged)
            DockSizeSetting(size = dockSize, onSizeChanged = onDockSizeChanged)
        }
    }
}

@Composable
private fun DockEnabledSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable Dock",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "A row of pinned apps at the bottom of the main screen",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChanged)
        }
    }
}

@Composable
private fun DockMaxAppsSetting(maxApps: Int, onMaxAppsChanged: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Maximum dock apps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = "$maxApps apps", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = maxApps.toFloat(),
                onValueChange = { onMaxAppsChanged(it.roundToInt()) },
                valueRange = 2f..5f,
                steps = 2,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DockSizeSetting(size: DockSize, onSizeChanged: (DockSize) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Dock size",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DockSize.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = entry == size,
                        onClick = { onSizeChanged(entry) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = DockSize.entries.size),
                        label = { Text(entry.label) },
                    )
                }
            }
        }
    }
}

// Presentation-only label, same reasoning as ThemePreference.label above.
private val DockSize.label: String
    get() = when (this) {
        DockSize.Small -> "Small"
        DockSize.Medium -> "Medium"
        DockSize.Large -> "Large"
    }

@Composable
private fun ManageAppsEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Manage Apps",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Choose which apps appear in the App Drawer",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun GridColumnsSetting(columns: Int, onColumnsChanged: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Grid columns",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = "$columns columns", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = columns.toFloat(),
                onValueChange = { onColumnsChanged(it.roundToInt()) },
                valueRange = 3f..6f,
                steps = 2,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundSettingsContent(
    musicEnabled: Boolean,
    onMusicEnabledChanged: (Boolean) -> Unit,
    musicPlayWhileBrowsingSystems: Boolean,
    onMusicPlayWhileBrowsingSystemsChanged: (Boolean) -> Unit,
    musicPlayWhileBrowsingGames: Boolean,
    onMusicPlayWhileBrowsingGamesChanged: (Boolean) -> Unit,
    musicPlayDuringScreensaver: Boolean,
    onMusicPlayDuringScreensaverChanged: (Boolean) -> Unit,
    musicDuckingMode: MusicDuckingMode,
    onMusicDuckingModeChanged: (MusicDuckingMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        MusicEnabledSetting(enabled = musicEnabled, onEnabledChanged = onMusicEnabledChanged)
        if (musicEnabled) {
            MusicPlayWhileBrowsingSystemsSetting(
                enabled = musicPlayWhileBrowsingSystems,
                onEnabledChanged = onMusicPlayWhileBrowsingSystemsChanged,
            )
            MusicPlayWhileBrowsingGamesSetting(
                enabled = musicPlayWhileBrowsingGames,
                onEnabledChanged = onMusicPlayWhileBrowsingGamesChanged,
            )
            MusicPlayDuringScreensaverSetting(
                enabled = musicPlayDuringScreensaver,
                onEnabledChanged = onMusicPlayDuringScreensaverChanged,
            )
            MusicDuckingModeSetting(selected = musicDuckingMode, onSelected = onMusicDuckingModeChanged)
        }
    }
}

@Composable
private fun MusicEnabledSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Background Music",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Enable background music",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = enabled, onCheckedChange = onEnabledChanged)
            }
        }
    }
}

@Composable
private fun MusicPlayWhileBrowsingSystemsSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Play while browsing systems",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = enabled, onCheckedChange = onEnabledChanged)
        }
    }
}

@Composable
private fun MusicPlayWhileBrowsingGamesSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Play while browsing games",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = enabled, onCheckedChange = onEnabledChanged)
        }
    }
}

@Composable
private fun MusicPlayDuringScreensaverSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Play during screensaver",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = enabled, onCheckedChange = onEnabledChanged)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicDuckingModeSetting(selected: MusicDuckingMode, onSelected: (MusicDuckingMode) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "During Video Playback",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                MusicDuckingMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == selected,
                        onClick = { onSelected(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = MusicDuckingMode.entries.size,
                        ),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = mode == selected) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        },
                        label = { Text(mode.label) },
                    )
                }
            }
        }
    }
}

// Presentation-only icon/label, same reasoning as ThemePreference.icon/label above.
private val MusicDuckingMode.icon: ImageVector
    get() = when (this) {
        MusicDuckingMode.Unchanged -> Icons.Filled.VolumeUp
        MusicDuckingMode.LowerVolume -> Icons.Filled.VolumeDown
        MusicDuckingMode.Pause -> Icons.Filled.Pause
    }

private val MusicDuckingMode.label: String
    get() = when (this) {
        MusicDuckingMode.Unchanged -> "Unchanged"
        MusicDuckingMode.LowerVolume -> "Lower volume"
        MusicDuckingMode.Pause -> "Pause"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePicker(selected: ThemePreference, onSelected: (ThemePreference) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemePreference.entries.forEachIndexed { index, theme ->
                    SegmentedButton(
                        selected = theme == selected,
                        onClick = { onSelected(theme) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemePreference.entries.size,
                        ),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = theme == selected) {
                                Icon(
                                    imageVector = theme.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        },
                        label = { Text(theme.label) },
                    )
                }
            }
        }
    }
}

// Presentation-only icon/label, kept in the UI layer rather than on the enum itself so
// ThemePreference stays a plain domain identifier with no display concerns.
private val ThemePreference.icon: ImageVector
    get() = when (this) {
        ThemePreference.Auto -> Icons.Filled.BrightnessAuto
        ThemePreference.Light -> Icons.Filled.LightMode
        ThemePreference.Dark -> Icons.Filled.DarkMode
    }

private val ThemePreference.label: String
    get() = when (this) {
        ThemePreference.Auto -> "Auto"
        ThemePreference.Light -> "Light"
        ThemePreference.Dark -> "Dark"
    }

/**
 * Applies to general/opaque image widgets (fanart, screenshots, custom system images) -
 * see ImageTransitionMode's kdoc for why logo-style content (LogoTransitionPicker) is a
 * separate setting with different options.
 */
@Composable
private fun ImageTransitionPicker(selected: ImageTransitionMode, onSelected: (ImageTransitionMode) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Image Transitions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ImageTransitionMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == selected,
                        onClick = { onSelected(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ImageTransitionMode.entries.size,
                        ),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = mode == selected) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        },
                        label = { Text(mode.label) },
                    )
                }
            }
        }
    }
}

private val ImageTransitionMode.icon: ImageVector
    get() = when (this) {
        ImageTransitionMode.None -> Icons.Filled.Block
        ImageTransitionMode.Fade -> Icons.Filled.BlurOn
        ImageTransitionMode.FadeScale -> Icons.Filled.ZoomOutMap
    }

private val ImageTransitionMode.label: String
    get() = when (this) {
        ImageTransitionMode.None -> "None"
        ImageTransitionMode.Fade -> "Fade"
        ImageTransitionMode.FadeScale -> "Fade + Scale"
    }

/**
 * Applies to system logos and game marquees - deliberately no Fade option, see
 * AnimatedLogoImage's kdoc for why (double-exposure on overlapping transparent images).
 */
@Composable
private fun LogoTransitionPicker(selected: LogoTransitionMode, onSelected: (LogoTransitionMode) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Logo Transitions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                LogoTransitionMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == selected,
                        onClick = { onSelected(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = LogoTransitionMode.entries.size,
                        ),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = mode == selected) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        },
                        label = { Text(mode.label) },
                    )
                }
            }
        }
    }
}

private val LogoTransitionMode.icon: ImageVector
    get() = when (this) {
        LogoTransitionMode.None -> Icons.Filled.Block
        LogoTransitionMode.Slide -> Icons.Filled.ArrowForward
        LogoTransitionMode.Scale -> Icons.Filled.ZoomIn
    }

private val LogoTransitionMode.label: String
    get() = when (this) {
        LogoTransitionMode.None -> "None"
        LogoTransitionMode.Slide -> "Slide"
        LogoTransitionMode.Scale -> "Scale"
    }

@Composable
private fun WidgetsSettingsContent(
    widgetsLocked: Boolean,
    onWidgetsLockedChanged: (Boolean) -> Unit,
    onEditWidgetsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        EditWidgetsEntry(onClick = onEditWidgetsClick)
        WidgetsLockToggle(locked = widgetsLocked, onLockedChange = onWidgetsLockedChanged)
    }
}

@Composable
private fun EditWidgetsEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Edit Widgets",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Add, move, and resize widgets on the main screen",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun WidgetsLockToggle(locked: Boolean, onLockedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Lock widget editing",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Prevent long-press from opening the widget editor",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = locked, onCheckedChange = onLockedChange)
            }
        }
    }
}
@Composable
private fun FolderSetting(
    label: String,
    path: String,
    isValidating: Boolean,
    statusText: String,
    onPick: (Uri) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(onPick)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { launcher.launch(null) }) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = "Change folder",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (isValidating) {
                CircularProgressIndicator()
            } else {
                Text(text = statusText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun OptionalFolderSetting(
    label: String,
    path: String?,
    isValidating: Boolean,
    statusText: String,
    onPick: (Uri) -> Unit,
    onClear: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(onPick)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = path ?: "Not set",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                if (path != null) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear folder",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                IconButton(onClick = { launcher.launch(null) }) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = "Choose folder",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (path != null) {
                if (isValidating) {
                    CircularProgressIndicator()
                } else {
                    Text(text = statusText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun LogFolderValidation?.toStatusText(): String = when (this) {
    null -> ""
    is LogFolderValidation.FolderNotFound -> "Folder not found"
    is LogFolderValidation.FolderFound ->
        if (settingsFileFound) "settings/es_settings.xml found" else "Folder found, but appears to be the incorrect folder"
}

private fun MediaFolderValidation?.toStatusText(): String = when (this) {
    null -> ""
    is MediaFolderValidation.FolderNotFound -> "Folder not found"
    is MediaFolderValidation.FolderFound -> "Folder found"
}