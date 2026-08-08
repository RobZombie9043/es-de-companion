package com.esde.companion.ui.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.esde.companion.data.storage.ApkInstaller
import com.esde.companion.data.storage.InstallUnknownAppsPermission
import com.esde.companion.domain.model.DownloadState
import com.esde.companion.domain.model.ReleaseInfo
import java.io.File

/**
 * Shown both when the silent startup check finds an update and when the manual "Check
 * for Updates" row does - the caller decides when to show it, this dialog only knows how
 * to walk the permission -> download -> install sequence once it's visible.
 */
@Composable
fun UpdateAvailableDialog(
    release: ReleaseInfo,
    downloadState: DownloadState?,
    onDownloadAndInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    var permissionGranted by remember { mutableStateOf(InstallUnknownAppsPermission.isGranted(context)) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Result code is ignored - the DisposableEffect's ON_RESUME check below is
            // what actually re-evaluates canRequestPackageInstalls(), same pattern as
            // OnboardingScreen's PermissionStep for AllFilesAccessPermission.
        }

    val currentOnResume = rememberUpdatedState({ permissionGranted = InstallUnknownAppsPermission.isGranted(context) })
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) currentOnResume.value()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Success) {
            context.startActivity(ApkInstaller.installIntent(context, File(downloadState.filePath)))
        }
    }

    val releaseNotesModifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update available - v${release.versionName}") },
        text = {
            Column(modifier = releaseNotesModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(release.releaseNotes.ifBlank { "No release notes provided." })
            }
        },
        confirmButton = {
            DownloadAndInstallButton(
                downloadState = downloadState,
                permissionGranted = permissionGranted,
                onClick = {
                    if (!permissionGranted) {
                        launcher.launch(InstallUnknownAppsPermission.requestIntent(context))
                    } else {
                        onDownloadAndInstall()
                    }
                },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        },
    )
}

@Composable
private fun DownloadAndInstallButton(
    downloadState: DownloadState?,
    permissionGranted: Boolean,
    onClick: () -> Unit,
) {
    val isDownloading = downloadState is DownloadState.Progress
    Button(onClick = onClick, enabled = !isDownloading) {
        if (isDownloading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Downloading... ${(downloadState as DownloadState.Progress).percent}%")
        } else {
            Text(
                when {
                    !permissionGranted -> "Grant permission"
                    downloadState is DownloadState.Failed -> "Retry"
                    else -> "Download & Install"
                },
            )
        }
    }
}
