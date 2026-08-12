package com.esde.companion.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BrandingWatermark
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.esde.companion.data.apps.AppIconLoader
import com.esde.companion.data.storage.ConfigBackupFileIo
import com.esde.companion.data.storage.SafPathResolver
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.FabPosition
import com.esde.companion.domain.model.FabSlot
import com.esde.companion.domain.model.FabType
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.model.UpdateCheckResult
import com.esde.companion.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Shared corner radius for the background panel behind every settings item (category
 * rows, folder settings, toggles, the theme picker) so they read as a consistent set of
 * cards rather than each item picking its own rounding.
 */
internal val SettingsItemShape = RoundedCornerShape(16.dp)

/** Shared opacity for every settings item's background - fully opaque, not translucent. */
private const val SETTINGS_PANEL_ALPHA = 1f

/** Corner radius of the accent-colored square drawn behind each [SettingsLabel] icon. */
private val SettingsLabelIconShape = RoundedCornerShape(8.dp)

/** Thickness of the accent-colored border surrounding each [SettingsLabel] icon. */
private val SettingsLabelIconBorderWidth = 8.dp

/**
 * The two accent-square colors [SettingsLabel] swaps between light/dark mode - deliberately
 * the *other* theme's default primary tone (dark theme's in light mode and vice versa)
 * rather than [MaterialTheme.colorScheme.primary] directly, per design request.
 */
private val SettingsLabelBorderColorInLightMode = darkColorScheme().primary
private val SettingsLabelBorderColorInDarkMode = lightColorScheme().primary

/**
 * Name row shared by every settings category and every individual setting: an icon in an
 * accent-colored (purple), rounded-corner square, followed by the name itself. The icon and
 * text share one color that resolves to black in light mode / white in dark mode (see
 * [LocalIsDarkTheme]).
 */
@Composable
internal fun SettingsLabel(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    val isDark = LocalIsDarkTheme.current
    val labelColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) SettingsLabelBorderColorInDarkMode else SettingsLabelBorderColorInLightMode
    val iconSize = style.fontSize.value.dp
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = SettingsLabelIconShape, color = borderColor) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = labelColor,
                modifier =
                    Modifier
                        .padding(SettingsLabelIconBorderWidth)
                        .size(iconSize),
            )
        }
        Text(text = text, style = style, color = labelColor)
    }
}

/**
 * Per-category settings content, one composable per [SettingsCategory] plus the shared
 * category-list row and leaf setting composables (toggle/slider/picker/folder-picker
 * rows). These used to be hosted by a standalone full-screen `SettingsScreen`; that
 * screen is gone - this content is now hosted inside the long-press popup instead, see
 * `LongPressSettingsMenu` in the `ui.main` package (hence `internal`, not `private`,
 * visibility throughout this file).
 */
@Composable
internal fun SettingsCategoryRow(
    category: SettingsCategory,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsLabel(icon = category.icon, text = category.title)
                Text(text = category.description, style = MaterialTheme.typography.bodySmall)
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

/**
 * Bottom-of-menu row that closes the app outright (see [MainActivity][com.esde.companion.ui.MainActivity]'s
 * `finishAndRemoveTask` call, the same one "Close Companion App on ES-DE Quit" uses).
 * Styled in the error color, distinct from [SettingsCategoryRow], so it doesn't read as
 * just another drill-down category - tapping it is terminal, not navigation.
 */
@Composable
internal fun SettingsQuitRow(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = "Quit Companion App",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/**
 * Top item of Other Settings - manual "Check for Updates" entry. [checkResult] is null
 * until the first manual check completes (the silent startup check never sets it - see
 * UpdateUiState's kdoc), in which case no status text is shown yet.
 */
@Composable
internal fun CheckForUpdatesRow(
    checkResult: UpdateCheckResult?,
    isChecking: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLabel(icon = Icons.Filled.SystemUpdate, text = "Check for Updates")
            when {
                isChecking -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                checkResult != null -> Text(text = checkResult.statusText(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun UpdateCheckResult.statusText(): String =
    when (this) {
        is UpdateCheckResult.UpdateAvailable -> "Update available"
        UpdateCheckResult.UpToDate -> "Up to date"
        is UpdateCheckResult.Failed -> "Check failed"
    }

@Composable
internal fun SetupSettingsContent(
    uiState: SettingsUiState,
    onLogFolderPicked: (String) -> Unit,
    onMediaFolderPicked: (String) -> Unit,
    onCustomSystemImagesFolderPicked: (String) -> Unit,
    onCustomSystemImagesFolderCleared: () -> Unit,
    onCustomLogosFolderPicked: (String) -> Unit,
    onCustomLogosFolderCleared: () -> Unit,
    onCustomMusicFolderPicked: (String) -> Unit,
    onCustomMusicFolderCleared: () -> Unit,
    onExportBackup: suspend () -> String,
    onRestoreBackup: suspend (String) -> Result<Unit>,
    onBackupMessage: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
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
            label = { SettingsLabel(icon = Icons.Filled.Folder, text = "ES-DE folder") },
            path = uiState.logFolderPath,
            statusText = uiState.logFolderValidation.toStatusText().unlessValidating(uiState.isValidatingLogFolder),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onLogFolderPicked) },
        )

        FolderSetting(
            label = { SettingsLabel(icon = Icons.Filled.PermMedia, text = "Media folder") },
            path = uiState.mediaFolderPath,
            statusText = uiState.mediaFolderValidation.toStatusText().unlessValidating(uiState.isValidatingMediaFolder),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onMediaFolderPicked) },
        )

        CustomFolderSettings(
            uiState = uiState,
            onCustomSystemImagesFolderPicked = onCustomSystemImagesFolderPicked,
            onCustomSystemImagesFolderCleared = onCustomSystemImagesFolderCleared,
            onCustomLogosFolderPicked = onCustomLogosFolderPicked,
            onCustomLogosFolderCleared = onCustomLogosFolderCleared,
            onCustomMusicFolderPicked = onCustomMusicFolderPicked,
            onCustomMusicFolderCleared = onCustomMusicFolderCleared,
        )

        BackupRestoreSetting(
            onExportBackup = onExportBackup,
            onRestoreBackup = onRestoreBackup,
            onMessage = onBackupMessage,
        )
    }
}

/** The three optional custom-media folder pickers on the Setup page, split out of
 * [SetupSettingsContent] purely to stay under detekt's LongMethod threshold. */
@Composable
private fun CustomFolderSettings(
    uiState: SettingsUiState,
    onCustomSystemImagesFolderPicked: (String) -> Unit,
    onCustomSystemImagesFolderCleared: () -> Unit,
    onCustomLogosFolderPicked: (String) -> Unit,
    onCustomLogosFolderCleared: () -> Unit,
    onCustomMusicFolderPicked: (String) -> Unit,
    onCustomMusicFolderCleared: () -> Unit,
) {
    OptionalFolderSetting(
        label = { SettingsLabel(icon = Icons.Filled.Image, text = "Custom System Images Folder") },
        path = uiState.customSystemImagesFolderPath,
        statusText =
            uiState.customSystemImagesFolderValidation.toStatusText()
                .unlessValidating(uiState.isValidatingCustomSystemImagesFolder),
        onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onCustomSystemImagesFolderPicked) },
        onClear = onCustomSystemImagesFolderCleared,
    )

    OptionalFolderSetting(
        label = { SettingsLabel(icon = Icons.AutoMirrored.Filled.BrandingWatermark, text = "Custom Logos Folder") },
        path = uiState.customLogosFolderPath,
        statusText =
            uiState.customLogosFolderValidation.toStatusText()
                .unlessValidating(uiState.isValidatingCustomLogosFolder),
        onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onCustomLogosFolderPicked) },
        onClear = onCustomLogosFolderCleared,
    )

    OptionalFolderSetting(
        label = { SettingsLabel(icon = Icons.Filled.MusicNote, text = "Custom Music Folder") },
        path = uiState.customMusicFolderPath,
        statusText =
            uiState.customMusicFolderValidation.toStatusText()
                .unlessValidating(uiState.isValidatingCustomMusicFolder),
        onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onCustomMusicFolderPicked) },
        onClear = onCustomMusicFolderCleared,
    )
}

/**
 * Export writes every setting [ExportConfigBackupUseCase][com.esde.companion.domain.usecase.ExportConfigBackupUseCase]
 * captures to a user-chosen file (ACTION_CREATE_DOCUMENT - a genuine "Save As", not the
 * folder-picker pattern [FolderSetting] uses, since the destination may be a real SAF/cloud
 * document rather than local storage). Restore reads a user-picked file back
 * (ACTION_OPEN_DOCUMENT, same contract EditWidgetsOverlay's image picker uses) and, after
 * an explicit confirmation since it overwrites every current setting, applies it. Both
 * picker results are handled with [ConfigBackupFileIo] directly in this Composable, same as
 * [FolderSetting] handles its own Uri via [SafPathResolver] - the ViewModel callbacks never
 * see a [Uri].
 */
@Composable
private fun BackupRestoreSetting(
    onExportBackup: suspend () -> String,
    onRestoreBackup: suspend (String) -> Result<Unit>,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val json = onExportBackup()
                val result = ConfigBackupFileIo.writeText(context, uri, json)
                onMessage(if (result.isSuccess) "Backup exported" else "Failed to write backup file")
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) pendingRestoreUri = uri
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.SettingsBackupRestore, text = "Backup & Restore")
            Text(
                text = "Export every setting on this device to a file, or restore from a previously exported file.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { exportLauncher.launch("esde-companion-backup-${LocalDate.now()}.json") }) {
                    Text("Export Backup")
                }
                TextButton(onClick = { restoreLauncher.launch(arrayOf("application/json")) }) {
                    Text("Restore Backup")
                }
            }
        }
    }

    val restoreUri = pendingRestoreUri
    if (restoreUri != null) {
        RestoreBackupConfirmationDialog(
            onConfirm = {
                pendingRestoreUri = null
                // Launched on this composable's own scope, not the (about to be dismissed
                // and torn down) dialog's - see the kdoc above for why that distinction is
                // load-bearing: a scope tied to the dialog gets cancelled the instant the
                // dialog leaves composition, racing against (and beating) this coroutine
                // ever actually doing the restore.
                coroutineScope.launch {
                    onMessage(restoreBackupAndDescribeResult(context, restoreUri, onRestoreBackup))
                }
            },
            onDismiss = { pendingRestoreUri = null },
        )
    }
}

private suspend fun restoreBackupAndDescribeResult(
    context: Context,
    uri: Uri,
    onRestoreBackup: suspend (String) -> Result<Unit>,
): String =
    ConfigBackupFileIo.readText(context, uri).fold(
        onSuccess = { text ->
            onRestoreBackup(text).fold(
                onSuccess = { "Settings restored" },
                onFailure = { error -> "Failed to restore backup: ${error.message}" },
            )
        },
        onFailure = { "Failed to read backup file" },
    )

/**
 * A dumb confirmation dialog only - [onConfirm] must do its actual work on a
 * [androidx.compose.runtime.rememberCoroutineScope] that outlives this dialog, not one
 * scoped to the dialog itself. See [BackupRestoreSetting]'s call site.
 */
@Composable
private fun RestoreBackupConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Backup?") },
        text = { Text("This overwrites every current setting with the contents of the selected file.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Restore") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
internal fun UISettingsContent(
    themePreference: ThemePreference,
    onThemePreferenceChanged: (ThemePreference) -> Unit,
    overlayOpacityPercent: Int,
    onOverlayOpacityChanged: (Int) -> Unit,
    gamePlayingBehavior: ScreenBehavior,
    onGamePlayingBehaviorChanged: (ScreenBehavior) -> Unit,
    gamePlayingDimPercent: Int,
    onGamePlayingDimPercentChanged: (Int) -> Unit,
    screensaverBehavior: ScreenBehavior,
    onScreensaverBehaviorChanged: (ScreenBehavior) -> Unit,
    screensaverDimPercent: Int,
    onScreensaverDimPercentChanged: (Int) -> Unit,
    fabAssignments: FabAssignments,
    installedApps: List<InstalledApp>,
    onFabTypeChanged: (FabPosition, FabType) -> Unit,
    onFabCustomAppChanged: (FabPosition, String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ThemePicker(selected = themePreference, onSelected = onThemePreferenceChanged)
        OverlayOpacitySetting(percent = overlayOpacityPercent, onPercentChanged = onOverlayOpacityChanged)
        ScreenBehaviorPicker(
            title = "Game Playing Screen Behavior",
            icon = Icons.Filled.SportsEsports,
            options = listOf(ScreenBehavior.Nothing, ScreenBehavior.Dim, ScreenBehavior.Black, ScreenBehavior.GameManual),
            selected = gamePlayingBehavior,
            onSelected = onGamePlayingBehaviorChanged,
            dimAmount = DimAmountControl(gamePlayingDimPercent, onGamePlayingDimPercentChanged),
        )
        ScreenBehaviorPicker(
            title = "Screensaver Screen Behavior",
            icon = Icons.Filled.Nightlight,
            options = listOf(ScreenBehavior.Nothing, ScreenBehavior.Dim, ScreenBehavior.Black),
            selected = screensaverBehavior,
            onSelected = onScreensaverBehaviorChanged,
            dimAmount = DimAmountControl(screensaverDimPercent, onScreensaverDimPercentChanged),
        )
        FabControlSetting(
            fabAssignments = fabAssignments,
            installedApps = installedApps,
            onFabTypeChanged = onFabTypeChanged,
            onFabCustomAppChanged = onFabCustomAppChanged,
        )
    }
}

// Bottom corners never offer Music - it can only occupy one of the two top corners (see
// FabAssignments.with) - so the picker itself simply never presents the option there
// rather than needing runtime validation to reject an invalid selection.
private val TOP_FAB_OPTIONS =
    listOf(FabType.Music, FabType.Settings, FabType.GameManual, FabType.AppDrawer, FabType.CustomApp, FabType.None)
private val BOTTOM_FAB_OPTIONS =
    listOf(FabType.Settings, FabType.GameManual, FabType.AppDrawer, FabType.CustomApp, FabType.None)

/**
 * Widgets category content - just hands off to the full-screen widget editor, same
 * "navigate out" idiom Edit Widgets used under UI Settings before it got its own
 * top-level category.
 */
@Composable
internal fun WidgetsSettingsContent(onEditWidgetsClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        EditWidgetsEntry(onClick = onEditWidgetsClick)
    }
}

/**
 * Master background opacity for every translucent overlay surface - the App Drawer, the
 * App Dock, the music controls panel, and the Settings/music-FAB/Edit-Widgets corner
 * buttons - see OnboardingRepository.observeOverlayOpacityPercent's kdoc for why this
 * replaced a separate slider per surface.
 */
@Composable
private fun OverlayOpacitySetting(
    percent: Int,
    onPercentChanged: (Int) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.Opacity, text = "Overlay Opacity")
            Text(text = "$percent%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = percent.toFloat(),
                onValueChange = { onPercentChanged(it.roundToInt()) },
                onValueChangeFinished = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                valueRange = 0f..100f,
            )
        }
    }
}

@Composable
internal fun VideoPlaybackSettingsContent(
    videoPlaybackEnabled: Boolean,
    onVideoPlaybackEnabledChanged: (Boolean) -> Unit,
    videoDelaySeconds: Int,
    onVideoDelaySecondsChanged: (Int) -> Unit,
    videoAudioEnabled: Boolean,
    onVideoAudioEnabledChanged: (Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
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
private fun VideoPlaybackEnabledSetting(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.Videocam, text = "Background Video")
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
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnabledChanged(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun VideoDelaySetting(
    delaySeconds: Int,
    onDelaySecondsChanged: (Int) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.Timer, text = "Video Start Delay")
            Text(
                text = if (delaySeconds == 0) "Off" else "${delaySeconds}s",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = delaySeconds.toFloat(),
                onValueChange = { onDelaySecondsChanged(it.roundToInt()) },
                onValueChangeFinished = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                valueRange = 0f..10f,
                steps = 9,
            )
        }
    }
}

@Composable
private fun VideoAudioSetting(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.VolumeUp, text = "Video Audio")
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
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnabledChanged(it)
                    },
                )
            }
        }
    }
}

@Composable
internal fun OtherSettingsContent(
    updateCheckResult: UpdateCheckResult?,
    isCheckingForUpdate: Boolean,
    onCheckForUpdatesClicked: () -> Unit,
    closeCompanionOnQuitEnabled: Boolean,
    onCloseCompanionOnQuitEnabledChanged: (Boolean) -> Unit,
    launchEsdeOnStartEnabled: Boolean,
    onLaunchEsdeOnStartEnabledChanged: (Boolean) -> Unit,
    debugLoggingEnabled: Boolean,
    onDebugLoggingEnabledChanged: (Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        CheckForUpdatesRow(updateCheckResult, isCheckingForUpdate, onCheckForUpdatesClicked)
        CloseCompanionOnQuitSetting(
            enabled = closeCompanionOnQuitEnabled,
            onEnabledChanged = onCloseCompanionOnQuitEnabledChanged,
        )
        LaunchEsdeOnStartSetting(
            enabled = launchEsdeOnStartEnabled,
            onEnabledChanged = onLaunchEsdeOnStartEnabledChanged,
        )
        DebugLoggingSetting(
            enabled = debugLoggingEnabled,
            onEnabledChanged = onDebugLoggingEnabledChanged,
        )
    }
}

@Composable
private fun CloseCompanionOnQuitSetting(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.AutoMirrored.Filled.ExitToApp, text = "Close Companion App on ES-DE Quit")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Close ES-DE Companion when ES-DE quits",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnabledChanged(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun LaunchEsdeOnStartSetting(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.RocketLaunch, text = "Launch ES-DE on Companion App Start")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Launch ES-DE on the other display when Companion App starts",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnabledChanged(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun DebugLoggingSetting(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.BugReport, text = "Debug Logging")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Write a debug log to help diagnose reported issues",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnabledChanged(it)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenBehaviorPicker(
    title: String,
    icon: ImageVector,
    options: List<ScreenBehavior>,
    selected: ScreenBehavior,
    onSelected: (ScreenBehavior) -> Unit,
    dimAmount: DimAmountControl,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsLabel(icon = icon, text = title)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, behavior ->
                    SegmentedButton(
                        selected = behavior == selected,
                        onClick = { onSelected(behavior) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
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
            if (selected == ScreenBehavior.Dim) {
                DimAmountSlider(percent = dimAmount.percent, onPercentChanged = dimAmount.onPercentChanged)
            }
        }
    }
}

/** Bundles a Dim amount with its change handler into one [ScreenBehaviorPicker] parameter,
 * keeping that function's parameter count under detekt's LongParameterList threshold. */
private data class DimAmountControl(
    val percent: Int,
    val onPercentChanged: (Int) -> Unit,
)

/**
 * Strength of the translucent black scrim MainActivity draws when a Screen Behavior
 * picker above is set to Dim - only shown while that's the current selection, same
 * on-demand-reveal idiom as VideoDelaySetting appearing only once video playback is
 * enabled.
 */
@Composable
private fun DimAmountSlider(
    percent: Int,
    onPercentChanged: (Int) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Dimming Amount: $percent%", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = percent.toFloat(),
            onValueChange = { onPercentChanged(it.roundToInt()) },
            onValueChangeFinished = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
            valueRange = 0f..100f,
        )
    }
}

// Presentation-only icon/label, same reasoning as ThemePreference.icon/label above.
private val ScreenBehavior.icon: ImageVector
    get() =
        when (this) {
            ScreenBehavior.Nothing -> Icons.Filled.Brightness7
            ScreenBehavior.Dim -> Icons.Filled.Brightness4
            ScreenBehavior.Black -> Icons.Filled.Brightness1
            ScreenBehavior.GameManual -> Icons.Filled.MenuBook
        }

private val ScreenBehavior.label: String
    get() =
        when (this) {
            ScreenBehavior.Nothing -> "On"
            ScreenBehavior.Dim -> "Dim"
            ScreenBehavior.Black -> "Off"
            ScreenBehavior.GameManual -> "Manual"
        }

/**
 * Settings > UI Settings > Floating Action Buttons - one panel covering all four corners
 * (see [UISettingsContent]), rather than four separate cards. Each corner is a dropdown
 * (see [FabTypeDropdown]) rather than a [SingleChoiceSegmentedButtonRow] - once
 * [FabType.AppDrawer]/[FabType.CustomApp] joined the original four options, a segmented
 * row of 5-6 icon+label buttons per corner no longer fit comfortably; a dropdown scales to
 * any option count without the row wrapping or shrinking illegibly.
 */
@Composable
private fun FabControlSetting(
    fabAssignments: FabAssignments,
    installedApps: List<InstalledApp>,
    onFabTypeChanged: (FabPosition, FabType) -> Unit,
    onFabCustomAppChanged: (FabPosition, String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.Apps, text = "Floating Action Buttons")
            FabPositionRow(
                title = "Top Left",
                options = TOP_FAB_OPTIONS,
                slot = fabAssignments[FabPosition.TopStart],
                installedApps = installedApps,
                onTypeSelected = { onFabTypeChanged(FabPosition.TopStart, it) },
                onAppSelected = { onFabCustomAppChanged(FabPosition.TopStart, it) },
            )
            FabPositionRow(
                title = "Top Right",
                options = TOP_FAB_OPTIONS,
                slot = fabAssignments[FabPosition.TopEnd],
                installedApps = installedApps,
                onTypeSelected = { onFabTypeChanged(FabPosition.TopEnd, it) },
                onAppSelected = { onFabCustomAppChanged(FabPosition.TopEnd, it) },
            )
            FabPositionRow(
                title = "Bottom Left",
                options = BOTTOM_FAB_OPTIONS,
                slot = fabAssignments[FabPosition.BottomStart],
                installedApps = installedApps,
                onTypeSelected = { onFabTypeChanged(FabPosition.BottomStart, it) },
                onAppSelected = { onFabCustomAppChanged(FabPosition.BottomStart, it) },
            )
            FabPositionRow(
                title = "Bottom Right",
                options = BOTTOM_FAB_OPTIONS,
                slot = fabAssignments[FabPosition.BottomEnd],
                installedApps = installedApps,
                onTypeSelected = { onFabTypeChanged(FabPosition.BottomEnd, it) },
                onAppSelected = { onFabCustomAppChanged(FabPosition.BottomEnd, it) },
            )
        }
    }
}

/** One corner's worth of controls: its type dropdown, plus an app-picker row that only
 * appears once [FabType.CustomApp] is selected. */
@Composable
private fun FabPositionRow(
    title: String,
    options: List<FabType>,
    slot: FabSlot,
    installedApps: List<InstalledApp>,
    onTypeSelected: (FabType) -> Unit,
    onAppSelected: (String) -> Unit,
) {
    var showAppPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge)
        FabTypeDropdown(options = options, selected = slot.type, onSelected = onTypeSelected)
        if (slot.type == FabType.CustomApp) {
            val selectedApp = installedApps.firstOrNull { it.packageName == slot.customAppPackageName }
            FabRowSurface(onClick = { showAppPicker = true }) {
                Text(text = selectedApp?.label ?: "Select App", style = MaterialTheme.typography.bodyMedium)
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
    }

    if (showAppPicker) {
        SelectAppDialog(
            installedApps = installedApps,
            onAppPicked = {
                onAppSelected(it)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false },
        )
    }
}

@Composable
private fun FabTypeDropdown(
    options: List<FabType>,
    selected: FabType,
    onSelected: (FabType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FabRowSurface(onClick = { expanded = true }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = selected.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(text = selected.label, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = { Icon(imageVector = option.icon, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

/** Shared row chrome for [FabTypeDropdown]'s trigger and the Custom App selector row - a
 * tappable surface a shade lighter than the panel behind it, with its content spaced to
 * the edges. */
@Composable
private fun FabRowSurface(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun SelectAppDialog(
    installedApps: List<InstalledApp>,
    onAppPicked: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select app") },
        text = {
            // Same fillMaxWidth-on-every-row reasoning as AppDock's AddAppDialog - without
            // it, varying label widths make the dialog visibly wobble side to side while
            // scrolling.
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(installedApps, key = { it.packageName }) { app ->
                    SelectAppRow(app = app, onClick = { onAppPicked(app.packageName) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SelectAppRow(
    app: InstalledApp,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val icon by produceState<Any?>(initialValue = null, key1 = app.packageName) {
        value = AppIconLoader.loadIcon(context, app.packageName)
    }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(model = icon, contentDescription = null, modifier = Modifier.size(40.dp))
        Text(text = app.label, style = MaterialTheme.typography.bodyLarge)
    }
}

// Presentation-only icon/label, same reasoning as ScreenBehavior.icon/label above.
private val FabType.icon: ImageVector
    get() =
        when (this) {
            FabType.Music -> Icons.Filled.MusicNote
            FabType.Settings -> Icons.Filled.Menu
            FabType.GameManual -> Icons.Filled.MenuBook
            FabType.AppDrawer -> Icons.Filled.Apps
            FabType.CustomApp -> Icons.Filled.Launch
            FabType.None -> Icons.Filled.Clear
        }

private val FabType.label: String
    get() =
        when (this) {
            FabType.Music -> "Music"
            FabType.Settings -> "Main Menu"
            FabType.GameManual -> "Manual"
            FabType.AppDrawer -> "App Drawer"
            FabType.CustomApp -> "App"
            FabType.None -> "None"
        }

@Composable
internal fun AppDrawerSettingsContent(
    gridColumns: Int,
    onGridColumnsChanged: (Int) -> Unit,
    sortFoldersOnTop: Boolean,
    onSortFoldersOnTopChanged: (Boolean) -> Unit,
    showSearchBar: Boolean,
    onShowSearchBarChanged: (Boolean) -> Unit,
    onManageAppsClick: () -> Unit,
    dockEnabled: Boolean,
    onDockEnabledChanged: (Boolean) -> Unit,
    dockMaxApps: Int,
    onDockMaxAppsChanged: (Int) -> Unit,
    dockSize: DockSize,
    onDockSizeChanged: (DockSize) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ManageAppsEntry(onClick = onManageAppsClick)
        ToggleSettingRow(
            icon = Icons.Filled.Search,
            title = "Show Search Bar",
            description = "Search bar and settings shortcuts at the top of the app drawer",
            enabled = showSearchBar,
            onEnabledChanged = onShowSearchBarChanged,
        )
        GridColumnsSetting(columns = gridColumns, onColumnsChanged = onGridColumnsChanged)
        ToggleSettingRow(
            icon = Icons.AutoMirrored.Filled.Sort,
            title = "Sort folders on top of apps",
            description = "Group folders ahead of ungrouped apps instead of sorting them in alphabetically",
            enabled = sortFoldersOnTop,
            onEnabledChanged = onSortFoldersOnTopChanged,
        )
        ToggleSettingRow(
            icon = Icons.Filled.Dock,
            title = "Enable Dock",
            description = "A row of pinned apps at the bottom of the main screen",
            enabled = dockEnabled,
            onEnabledChanged = onDockEnabledChanged,
        )
        if (dockEnabled) {
            DockMaxAppsSetting(maxApps = dockMaxApps, onMaxAppsChanged = onDockMaxAppsChanged)
            DockSizeSetting(size = dockSize, onSizeChanged = onDockSizeChanged)
        }
    }
}

@Composable
private fun DockMaxAppsSetting(
    maxApps: Int,
    onMaxAppsChanged: (Int) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.FormatListNumbered, text = "Maximum dock apps")
            Text(text = "$maxApps apps", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = maxApps.toFloat(),
                onValueChange = { onMaxAppsChanged(it.roundToInt()) },
                onValueChangeFinished = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                valueRange = 2f..5f,
                steps = 2,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DockSizeSetting(
    size: DockSize,
    onSizeChanged: (DockSize) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.AspectRatio, text = "Dock size")
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
    get() =
        when (this) {
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsLabel(icon = Icons.Filled.Apps, text = "Manage Apps")
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
private fun GridColumnsSetting(
    columns: Int,
    onColumnsChanged: (Int) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.GridView, text = "Grid columns")
            Text(text = "$columns columns", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = columns.toFloat(),
                onValueChange = { onColumnsChanged(it.roundToInt()) },
                onValueChangeFinished = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                valueRange = 3f..6f,
                steps = 2,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SoundSettingsContent(
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
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        MusicEnabledSetting(enabled = musicEnabled, onEnabledChanged = onMusicEnabledChanged)
        if (musicEnabled) {
            ToggleSettingRow(
                icon = Icons.Filled.Devices,
                title = "Play while browsing systems",
                description = null,
                enabled = musicPlayWhileBrowsingSystems,
                onEnabledChanged = onMusicPlayWhileBrowsingSystemsChanged,
            )
            ToggleSettingRow(
                icon = Icons.Filled.SportsEsports,
                title = "Play while browsing games",
                description = null,
                enabled = musicPlayWhileBrowsingGames,
                onEnabledChanged = onMusicPlayWhileBrowsingGamesChanged,
            )
            ToggleSettingRow(
                icon = Icons.Filled.Nightlight,
                title = "Play during screensaver",
                description = null,
                enabled = musicPlayDuringScreensaver,
                onEnabledChanged = onMusicPlayDuringScreensaverChanged,
            )
            MusicDuckingModeSetting(selected = musicDuckingMode, onSelected = onMusicDuckingModeChanged)
        }
    }
}

@Composable
private fun MusicEnabledSetting(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.MusicNote, text = "Background Music")
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
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnabledChanged(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun ToggleSettingRow(
    icon: ImageVector,
    title: String,
    description: String?,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (description != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    SettingsLabel(icon = icon, text = title)
                    Text(text = description, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                SettingsLabel(icon = icon, text = title, modifier = Modifier.weight(1f))
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEnabledChanged(it)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicDuckingModeSetting(
    selected: MusicDuckingMode,
    onSelected: (MusicDuckingMode) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.Movie, text = "Volume During Video Playback")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                MusicDuckingMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == selected,
                        onClick = { onSelected(mode) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
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
    get() =
        when (this) {
            MusicDuckingMode.Unchanged -> Icons.Filled.VolumeUp
            MusicDuckingMode.LowerVolume -> Icons.Filled.VolumeDown
            MusicDuckingMode.Pause -> Icons.Filled.Pause
        }

private val MusicDuckingMode.label: String
    get() =
        when (this) {
            MusicDuckingMode.Unchanged -> "Unchanged"
            MusicDuckingMode.LowerVolume -> "Lower"
            MusicDuckingMode.Pause -> "Pause"
        }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePicker(
    selected: ThemePreference,
    onSelected: (ThemePreference) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.Palette, text = "Theme")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemePreference.entries.forEachIndexed { index, theme ->
                    SegmentedButton(
                        selected = theme == selected,
                        onClick = { onSelected(theme) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
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
    get() =
        when (this) {
            ThemePreference.Auto -> Icons.Filled.BrightnessAuto
            ThemePreference.Light -> Icons.Filled.LightMode
            ThemePreference.Dark -> Icons.Filled.DarkMode
        }

private val ThemePreference.label: String
    get() =
        when (this) {
            ThemePreference.Auto -> "Auto"
            ThemePreference.Light -> "Light"
            ThemePreference.Dark -> "Dark"
        }

@Composable
private fun EditWidgetsEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsLabel(icon = Icons.Filled.Widgets, text = "Edit Widgets")
                Text(
                    text = "Add, move, and resize widgets on the main screen",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

/**
 * [FolderSetting] and [OptionalFolderSetting] take their name as a `label` slot (rather
 * than a plain `String` + `ImageVector` pair) and fold `isValidating`/`statusText` into one
 * nullable `statusText` (null means "still validating") purely to stay under detekt's
 * `LongParameterList` threshold once an icon joined the parameter list - not a design this
 * pair otherwise needed.
 */
@Composable
private fun FolderSetting(
    label: @Composable () -> Unit,
    path: String,
    statusText: String?,
    onPick: (Uri) -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let(onPick)
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            label()
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
            if (statusText == null) {
                CircularProgressIndicator()
            } else {
                Text(text = statusText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun OptionalFolderSetting(
    label: @Composable () -> Unit,
    path: String?,
    statusText: String?,
    onPick: (Uri) -> Unit,
    onClear: () -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let(onPick)
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            label()
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
                if (statusText == null) {
                    CircularProgressIndicator()
                } else {
                    Text(text = statusText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * Null while still validating (see [FolderSetting]/[OptionalFolderSetting]'s `statusText`),
 * the computed text otherwise.
 */
private fun String.unlessValidating(isValidating: Boolean): String? = if (isValidating) null else this

private fun LogFolderValidation?.toStatusText(): String =
    when (this) {
        null -> ""
        is LogFolderValidation.FolderNotFound -> "Folder not found"
        is LogFolderValidation.FolderFound ->
            if (settingsFileFound) "settings/es_settings.xml found" else "Folder found, but appears to be the incorrect folder"
    }

private fun MediaFolderValidation?.toStatusText(): String =
    when (this) {
        null -> ""
        is MediaFolderValidation.FolderNotFound -> "Folder not found"
        is MediaFolderValidation.FolderFound -> "Folder found"
    }

/**
 * Bundles the two credential text fields and their change handlers into one parameter,
 * keeping [RetroAchievementsSettingsContent]/[RetroAchievementsCredentialsForm] under
 * detekt's LongParameterList threshold - same reasoning as [DimAmountControl].
 */
internal data class RetroAchievementsCredentialsInput(
    val username: String,
    val onUsernameChanged: (String) -> Unit,
    val webApiKey: String,
    val onWebApiKeyChanged: (String) -> Unit,
)

/** Bundles the Connect action's transient state - same reasoning as [RetroAchievementsCredentialsInput]. */
internal data class RetroAchievementsConnectStatus(
    val isConnecting: Boolean,
    val connectError: String?,
)

@Composable
internal fun RetroAchievementsSettingsContent(
    credentials: RetroAchievementsCredentials?,
    input: RetroAchievementsCredentialsInput,
    connectStatus: RetroAchievementsConnectStatus,
    onConnectClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        RetroAchievementsConnectionStatus(credentials, onSignOutClicked)
        RetroAchievementsCredentialsForm(
            input = input,
            connectStatus = connectStatus,
            onConnectClicked = onConnectClicked,
        )
    }
}

@Composable
private fun RetroAchievementsConnectionStatus(
    credentials: RetroAchievementsCredentials?,
    onSignOutClicked: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLabel(
                icon = Icons.Filled.EmojiEvents,
                text = if (credentials != null) "Signed in as ${credentials.username}" else "Not signed in",
            )
            if (credentials != null) {
                TextButton(onClick = onSignOutClicked) { Text("Sign Out") }
            }
        }
    }
}

@Composable
private fun RetroAchievementsCredentialsForm(
    input: RetroAchievementsCredentialsInput,
    connectStatus: RetroAchievementsConnectStatus,
    onConnectClicked: () -> Unit,
) {
    var webApiKeyVisible by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.EmojiEvents, text = "RetroAchievements Account")
            OutlinedTextField(
                value = input.username,
                onValueChange = input.onUsernameChanged,
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            val webApiKeyTransformation =
                if (webApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation()
            OutlinedTextField(
                value = input.webApiKey,
                onValueChange = input.onWebApiKeyChanged,
                label = { Text("Web API Key") },
                singleLine = true,
                visualTransformation = webApiKeyTransformation,
                trailingIcon = {
                    WebApiKeyVisibilityToggle(
                        visible = webApiKeyVisible,
                        onToggle = { webApiKeyVisible = !webApiKeyVisible },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (connectStatus.connectError != null) {
                Text(
                    text = connectStatus.connectError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = onConnectClicked,
                enabled = !connectStatus.isConnecting && input.username.isNotBlank() && input.webApiKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (connectStatus.isConnecting) {
                    val spinnerColor = MaterialTheme.colorScheme.onPrimary
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = spinnerColor)
                } else {
                    Text("Connect")
                }
            }
        }
    }
}

@Composable
private fun WebApiKeyVisibilityToggle(
    visible: Boolean,
    onToggle: () -> Unit,
) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (visible) "Hide Web API Key" else "Show Web API Key",
        )
    }
}
