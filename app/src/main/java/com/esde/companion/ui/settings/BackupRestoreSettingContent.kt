package com.esde.companion.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Export writes every setting [ExportConfigBackupUseCase][com.esde.companion.domain.usecase.ExportConfigBackupUseCase]
 * captures to a user-chosen file (ACTION_CREATE_DOCUMENT - a genuine "Save As", not the
 * folder-picker pattern [FolderSetting] uses, since the destination may be a real SAF/cloud
 * document rather than local storage). Restore reads a user-picked file back
 * (ACTION_OPEN_DOCUMENT, same contract EditWidgetsOverlay's image picker uses) and, after
 * an explicit confirmation since it overwrites every current setting, applies it. Both
 * picker results are handled with [ConfigBackupFileIo] directly in this Composable, same as
 * [FolderSetting] handles its own Uri via [SafPathResolver][com.esde.companion.data.storage.SafPathResolver] -
 * the ViewModel callbacks never see a [Uri].
 *
 * Split out of `SetupSettingsContent.kt` into its own file once adding the in-progress dialog
 * below pushed that file over detekt's per-file function-count threshold.
 */
@Composable
internal fun BackupRestoreSetting(
    onExportBackup: suspend () -> String,
    onRestoreBackup: suspend (String) -> Result<Unit>,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    // Surfaced as a non-dismissable progress dialog below - previously this operation had zero
    // in-progress feedback at all (a coroutine just fired, with only a one-shot Snackbar message
    // after completion). Indeterminate only, not a determinate progress bar - export/import is a
    // single flat sequence of repository reads/writes with no meaningful sub-steps to report a
    // fraction for.
    var activeOperation by remember { mutableStateOf<BackupOperation?>(null) }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                activeOperation = BackupOperation.Exporting
                try {
                    val json = onExportBackup()
                    val result = ConfigBackupFileIo.writeText(context, uri, json)
                    onMessage(if (result.isSuccess) "Backup exported" else "Failed to write backup file")
                } finally {
                    activeOperation = null
                }
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) pendingRestoreUri = uri
        }

    BackupRestoreCard(
        onExportClick = { exportLauncher.launch("esde-companion-backup-${LocalDate.now()}.json") },
        onRestoreClick = { restoreLauncher.launch(arrayOf("application/json")) },
    )

    activeOperation?.let { BackupOperationProgressDialog(it) }

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
                    activeOperation = BackupOperation.Restoring
                    try {
                        onMessage(restoreBackupAndDescribeResult(context, restoreUri, onRestoreBackup))
                    } finally {
                        activeOperation = null
                    }
                }
            },
            onDismiss = { pendingRestoreUri = null },
        )
    }
}

/** The card's own visual content - pulled out of [BackupRestoreSetting] purely to keep that
 * composable under detekt's LongMethod threshold. */
@Composable
private fun BackupRestoreCard(
    onExportClick: () -> Unit,
    onRestoreClick: () -> Unit,
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
            SettingsLabel(icon = Icons.Filled.SettingsBackupRestore, text = "Backup & Restore")
            Text(
                text = "Export every setting on this device to a file, or restore from a previously exported file.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onExportClick) { Text("Export Backup") }
                TextButton(onClick = onRestoreClick) { Text("Restore Backup") }
            }
        }
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

private sealed interface BackupOperation {
    data object Exporting : BackupOperation

    data object Restoring : BackupOperation
}

/** Not dismissable - only [BackupRestoreSetting]'s own coroutine clearing `activeOperation`
 * (in a `finally`) closes this, once the export/import actually finishes. */
@Composable
private fun BackupOperationProgressDialog(operation: BackupOperation) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = {
            Text(
                when (operation) {
                    BackupOperation.Exporting -> "Exporting backup"
                    BackupOperation.Restoring -> "Restoring backup"
                },
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
            }
        },
    )
}

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
