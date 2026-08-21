package com.esde.companion.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.esde.companion.data.storage.ConfigBackupFileIo
import com.esde.companion.data.storage.SafPathResolver
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import kotlinx.coroutines.launch
import java.time.LocalDate

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
