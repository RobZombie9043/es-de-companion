package com.esde.companion.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.data.storage.SafPathResolver
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onboardingComplete.collect { onOnboardingComplete() }
    }

    // Re-check permission whenever this screen resumes - covers returning from the
    // system Settings screen after granting (or not granting) All files access.
    val currentOnPermissionResult = rememberUpdatedState(viewModel::onPermissionResult)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnPermissionResult.value(AllFilesAccessPermission.isGranted())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState.step) {
            OnboardingStep.Permission -> PermissionStep(granted = uiState.permissionGranted)

            OnboardingStep.LogFolder -> LogFolderStep(
                path = uiState.logFolderPath,
                validation = uiState.logFolderValidation,
                isValidating = uiState.isValidatingLogFolder,
                onPickFolder = { uri ->
                    SafPathResolver.resolvePath(uri)?.let(viewModel::onLogFolderPicked)
                },
                onConfirm = viewModel::onLogFolderConfirmed,
            )

            OnboardingStep.MediaFolder -> MediaFolderStep(
                path = uiState.mediaFolderPath,
                validation = uiState.mediaFolderValidation,
                isValidating = uiState.isValidatingMediaFolder,
                isCompleting = uiState.isCompleting,
                onPickFolder = { uri ->
                    SafPathResolver.resolvePath(uri)?.let(viewModel::onMediaFolderPicked)
                },
                onConfirm = viewModel::onMediaFolderConfirmed,
            )
        }
    }
}

@Composable
private fun PermissionStep(granted: Boolean) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Result code is ignored - the DisposableEffect's ON_RESUME check in
        // OnboardingScreen is what actually re-evaluates isExternalStorageManager().
    }

    StepScaffold(
        title = "Storage access",
        description = "ES-DE Companion reads ES-DE's log file and media folder directly " +
                "from storage. Grant \"All files access\" to continue.",
    ) {
        if (granted) {
            Text("Access granted.")
        } else {
            Button(onClick = { launcher.launch(AllFilesAccessPermission.requestIntent(context)) }) {
                Text("Grant access")
            }
        }
    }
}

@Composable
private fun LogFolderStep(
    path: String,
    validation: LogFolderValidation?,
    isValidating: Boolean,
    onPickFolder: (Uri) -> Unit,
    onConfirm: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(onPickFolder)
    }

    val logFileFound = (validation as? LogFolderValidation.FolderFound)?.logFileFound == true
    val folderExistsButNoLogFile = validation is LogFolderValidation.FolderFound && !logFileFound

    StepScaffold(
        title = "ES-DE folder",
        description = "Select the ES-DE folder ES-DE itself is configured to use. Currently: $path",
    ) {
        when {
            isValidating -> CircularProgressIndicator()
            logFileFound -> Text("Found logs/es_log.txt - this looks right.")
            folderExistsButNoLogFile -> Text(
                "This folder exists, but logs/es_log.txt wasn't found inside it. You can " +
                        "continue anyway if ES-DE hasn't been launched yet, or choose a different folder.",
            )
            validation is LogFolderValidation.FolderNotFound -> Text("This folder doesn't exist. Choose the correct folder.")
        }

        OutlinedButton(onClick = { launcher.launch(null) }) {
            Text("Choose different folder")
        }

        Button(onClick = onConfirm, enabled = !isValidating) {
            Text("Continue")
        }
    }
}

@Composable
private fun MediaFolderStep(
    path: String,
    validation: MediaFolderValidation?,
    isValidating: Boolean,
    isCompleting: Boolean,
    onPickFolder: (Uri) -> Unit,
    onConfirm: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(onPickFolder)
    }

    StepScaffold(
        title = "Media folder",
        description = "Select ES-DE's downloaded_media folder, used to show game artwork. Currently: $path",
    ) {
        when {
            isValidating -> CircularProgressIndicator()
            validation is MediaFolderValidation.FolderFound -> Text("Folder found.")
            validation is MediaFolderValidation.FolderNotFound -> Text("This folder doesn't exist. Choose the correct folder.")
        }

        OutlinedButton(onClick = { launcher.launch(null) }) {
            Text("Choose different folder")
        }

        Button(onClick = onConfirm, enabled = !isValidating && !isCompleting) {
            Text(if (isCompleting) "Finishing..." else "Finish setup")
        }
    }
}

@Composable
private fun StepScaffold(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
        content()
    }
}