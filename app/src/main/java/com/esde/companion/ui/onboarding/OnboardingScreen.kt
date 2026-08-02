package com.esde.companion.ui.onboarding

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.EsdeEventScriptSettings
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

    // Onboarding never exits the app on back - either pop to the previous step, or (on
    // the first step) simply consume the event, same "never exit" principle CLAUDE.md
    // documents for MainScreen/SettingsScreen/EditWidgetsOverlay.
    BackHandler(enabled = true) { viewModel.onBack() }

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

            OnboardingStep.EsdeFolder -> EsdeFolderStep(
                path = uiState.logFolderPath,
                validation = uiState.logFolderValidation,
                isValidating = uiState.isValidatingLogFolder,
                onPickFolder = { uri ->
                    SafPathResolver.resolvePath(uri)?.let(viewModel::onEsdeFolderPicked)
                },
                onConfirm = viewModel::onEsdeFolderConfirmed,
            )

            OnboardingStep.MediaFolder -> MediaFolderStep(
                path = uiState.mediaFolderPath,
                validation = uiState.mediaFolderValidation,
                isValidating = uiState.isValidatingMediaFolder,
                autoDetected = uiState.mediaFolderAutoDetected,
                isCheckingInstallation = uiState.isCheckingInstallation,
                onPickFolder = { uri ->
                    SafPathResolver.resolvePath(uri)?.let(viewModel::onMediaFolderPicked)
                },
                onConfirm = viewModel::onMediaFolderConfirmed,
            )

            OnboardingStep.LegacyScripts -> LegacyScriptsStep(
                scriptFiles = uiState.legacyScriptFiles,
                isDeleting = uiState.isDeletingLegacyScripts,
                onDelete = viewModel::onDeleteLegacyScriptFiles,
                onConfirm = viewModel::onLegacyScriptsConfirmed,
            )

            OnboardingStep.EventScriptSettings -> EventScriptSettingsStep(
                settings = uiState.eventScriptSettings,
                onConfirm = viewModel::onEventScriptSettingsConfirmed,
            )

            OnboardingStep.LiveLogCheck -> LiveLogCheckStep(
                connectionState = uiState.connectionState,
                passed = uiState.liveCheckPassed,
                isCompleting = uiState.isCompleting,
                onFinish = viewModel::onFinishSetup,
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
private fun EsdeFolderStep(
    path: String,
    validation: LogFolderValidation?,
    isValidating: Boolean,
    onPickFolder: (Uri) -> Unit,
    onConfirm: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(onPickFolder)
    }

    val settingsFileFound = (validation as? LogFolderValidation.FolderFound)?.settingsFileFound == true
    val folderExistsButLooksWrong = validation is LogFolderValidation.FolderFound && !settingsFileFound

    StepScaffold(
        title = "ES-DE folder",
        description = "Select the ES-DE folder ES-DE itself is configured to use. Currently: $path",
    ) {
        when {
            isValidating -> CircularProgressIndicator()
            settingsFileFound -> Text("Found settings/es_settings.xml - this looks right.")
            folderExistsButLooksWrong -> Text(
                "This folder exists, but settings/es_settings.xml wasn't found inside it - " +
                        "it appears to be the incorrect folder. You can continue anyway, or " +
                        "choose a different folder.",
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
    autoDetected: Boolean,
    isCheckingInstallation: Boolean,
    onPickFolder: (Uri) -> Unit,
    onConfirm: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(onPickFolder)
    }

    StepScaffold(
        title = "Media folder",
        description = "ES-DE's downloaded_media folder, used to show game artwork. Currently: $path",
    ) {
        if (autoDetected) {
            Text("Detected from ES-DE's own settings - confirm this looks right.")
        }
        when {
            isValidating -> CircularProgressIndicator()
            validation is MediaFolderValidation.FolderFound -> Text("Folder found.")
            validation is MediaFolderValidation.FolderNotFound -> Text("This folder doesn't exist. Choose the correct folder.")
        }

        OutlinedButton(onClick = { launcher.launch(null) }) {
            Text("Choose different folder")
        }

        Button(onClick = onConfirm, enabled = !isValidating && !isCheckingInstallation) {
            Text(if (isCheckingInstallation) "Checking..." else "Continue")
        }
    }
}

@Composable
private fun LegacyScriptsStep(
    scriptFiles: List<String>,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
) {
    StepScaffold(
        title = "Leftover script files",
        description = "Found ${scriptFiles.size} leftover script file(s) from an older " +
                "version of ES-DE Companion. These are no longer required - removing them " +
                "will lead to improved performance.",
    ) {
        if (isDeleting) {
            CircularProgressIndicator()
        } else if (scriptFiles.isNotEmpty()) {
            Button(onClick = onDelete) {
                Text("Delete these files")
            }
        } else {
            Text("Deleted.")
        }

        Button(onClick = onConfirm, enabled = !isDeleting) {
            Text("Continue")
        }
    }
}

@Composable
private fun EventScriptSettingsStep(
    settings: EsdeEventScriptSettings?,
    onConfirm: () -> Unit,
) {
    StepScaffold(
        title = "ES-DE settings",
        description = "ES-DE Companion needs a few settings enabled in ES-DE itself: " +
                "Main Menu > Other Settings >",
    ) {
        if (settings?.customEventScripts != true) {
            Text("- \"Enable Custom Event Scripts\"")
        }
        if (settings?.customEventScriptsBrowsing != true) {
            Text("- \"Browsing Custom Event Scripts\"")
        }
        if (settings?.debugMode != true) {
            Text("- \"Debug Mode\"")
        }
        Text(
            "ES-DE doesn't update its settings file live, so this can't be re-checked " +
                    "automatically - once you've made these changes in ES-DE, continue below.",
        )

        Button(onClick = onConfirm) {
            Text("I've made these changes")
        }
    }
}

@Composable
private fun LiveLogCheckStep(
    connectionState: EsdeConnectionState?,
    passed: Boolean,
    isCompleting: Boolean,
    onFinish: () -> Unit,
) {
    StepScaffold(
        title = "Confirm it's working",
        description = "Browse to a system or game in ES-DE now, to confirm the log is " +
                "found and generating activity.",
    ) {
        when {
            connectionState == null -> CircularProgressIndicator()
            connectionState is EsdeConnectionState.LogFileNotFound ->
                Text("es_log.txt not found - check the folder you selected, then go back and fix it.")
            passed -> Text("Working! ES-DE activity detected.")
            else -> Text("Waiting for activity - browse a system or game in ES-DE now.")
        }

        Button(onClick = onFinish, enabled = passed && !isCompleting) {
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
