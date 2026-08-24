package com.esde.companion.ui.onboarding

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.data.storage.SafPathResolver
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.EsdeEventScriptSettings
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.ui.theme.LocalIsDarkTheme
import com.esde.companion.ui.widgets.fallbackBackgroundAssetPath

/** Forward order (see OnboardingStep's kdoc) - a plain list rather than a `when`-mapped
 * ordinal since [OnboardingStep] is a sealed class with no built-in ordinal. */
private val ORDERED_ONBOARDING_STEPS =
    listOf(
        OnboardingStep.Permission,
        OnboardingStep.EsdeFolder,
        OnboardingStep.MediaFolder,
        OnboardingStep.LegacyScripts,
        OnboardingStep.EventScriptSettings,
        OnboardingStep.LiveLogCheck,
    )

/** Fixed position in the wizard's forward order - used purely to pick the step-transition
 * slide direction below, since some steps are conditionally skipped, so adjacent steps in
 * a given run aren't always adjacent here. */
private val OnboardingStep.order: Int
    get() = ORDERED_ONBOARDING_STEPS.indexOf(this)

/** Slide/fade durations for the step-to-step wizard transition. */
private const val STEP_SLIDE_DURATION_MS = 220
private const val STEP_FADE_OUT_DURATION_MS = 150

/** Onboarding text renders directly over the themed fallback background image rather than
 * an opaque Material surface, so it can't rely on colorScheme.onBackground/onSurface for
 * contrast the way a flat-colored Surface could - white in dark mode, black in light mode,
 * same explicit-contrast approach AppDrawer/AppDock use for text over background art. */
@Composable
private fun onboardingContentColor(): Color = if (LocalIsDarkTheme.current) Color.White else Color.Black

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onBack: (() -> Unit)? = if (uiState.stepBackStack.isNotEmpty()) viewModel::onBack else null

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
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    currentOnPermissionResult.value(AllFilesAccessPermission.isGranted())
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = fallbackBackgroundAssetPath(LocalIsDarkTheme.current),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            contentColor = onboardingContentColor(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "ES-DE Companion Setup",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    // Slide direction mirrors the step's position in the linear wizard
                    // sequence (see OnboardingStep.order above) - moving forward slides
                    // in from the right, going back (via onBack) slides in from the
                    // left, same drill-down idiom LongPressSettingsMenu's AnimatedContent
                    // uses for its category subpages.
                    AnimatedContent(
                        targetState = uiState.step,
                        transitionSpec = {
                            val movingForward = targetState.order > initialState.order
                            val slideDistance = { width: Int -> width / 3 }
                            if (movingForward) {
                                (
                                    slideInHorizontally(tween(STEP_SLIDE_DURATION_MS)) { slideDistance(it) } +
                                        fadeIn(tween(STEP_SLIDE_DURATION_MS))
                                ).togetherWith(
                                    slideOutHorizontally(tween(STEP_SLIDE_DURATION_MS)) { -slideDistance(it) } +
                                        fadeOut(tween(STEP_FADE_OUT_DURATION_MS)),
                                )
                            } else {
                                (
                                    slideInHorizontally(tween(STEP_SLIDE_DURATION_MS)) { -slideDistance(it) } +
                                        fadeIn(tween(STEP_SLIDE_DURATION_MS))
                                ).togetherWith(
                                    slideOutHorizontally(tween(STEP_SLIDE_DURATION_MS)) { slideDistance(it) } +
                                        fadeOut(tween(STEP_FADE_OUT_DURATION_MS)),
                                )
                            }
                        },
                        label = "onboardingStepContent",
                    ) { step ->
                        when (step) {
                            OnboardingStep.Permission ->
                                PermissionStep(
                                    granted = uiState.permissionGranted,
                                    onBack = onBack,
                                    onNext = { viewModel.onPermissionResult(uiState.permissionGranted) },
                                )

                            OnboardingStep.EsdeFolder ->
                                EsdeFolderStep(
                                    path = uiState.logFolderPath,
                                    validation = uiState.logFolderValidation,
                                    isValidating = uiState.isValidatingLogFolder,
                                    isCheckingInstallation = uiState.isCheckingInstallation,
                                    onPickFolder = { uri ->
                                        SafPathResolver.resolvePath(uri)?.let(viewModel::onEsdeFolderPicked)
                                    },
                                    onBack = onBack,
                                    onNext = viewModel::onEsdeFolderConfirmed,
                                )

                            OnboardingStep.MediaFolder ->
                                MediaFolderStep(
                                    path = uiState.mediaFolderPath,
                                    validation = uiState.mediaFolderValidation,
                                    isValidating = uiState.isValidatingMediaFolder,
                                    autoDetected = uiState.mediaFolderAutoDetected,
                                    onPickFolder = { uri ->
                                        SafPathResolver.resolvePath(uri)?.let(viewModel::onMediaFolderPicked)
                                    },
                                    onBack = onBack,
                                    onNext = viewModel::onMediaFolderConfirmed,
                                )

                            OnboardingStep.LegacyScripts ->
                                LegacyScriptsStep(
                                    scriptFiles = uiState.legacyScriptFiles,
                                    isDeleting = uiState.isDeletingLegacyScripts,
                                    onDelete = viewModel::onDeleteLegacyScriptFiles,
                                    onBack = onBack,
                                    onNext = viewModel::onLegacyScriptsConfirmed,
                                )

                            OnboardingStep.EventScriptSettings ->
                                EventScriptSettingsStep(
                                    settings = uiState.eventScriptSettings,
                                    onBack = onBack,
                                    onNext = viewModel::onEventScriptSettingsConfirmed,
                                )

                            OnboardingStep.LiveLogCheck ->
                                LiveLogCheckStep(
                                    connectionState = uiState.connectionState,
                                    passed = uiState.liveCheckPassed,
                                    isCompleting = uiState.isCompleting,
                                    onBack = onBack,
                                    onNext = viewModel::onFinishSetup,
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStep(
    granted: Boolean,
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Result code is ignored - the DisposableEffect's ON_RESUME check in
            // OnboardingScreen is what actually re-evaluates isExternalStorageManager().
        }

    StepScaffold(
        title = "Storage access",
        description =
            "ES-DE Companion reads ES-DE's log file and media folder directly " +
                "from storage. Grant \"All files access\" to continue.",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = granted,
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
    isCheckingInstallation: Boolean,
    onPickFolder: (Uri) -> Unit,
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let(onPickFolder)
        }

    val settingsFileFound = (validation as? LogFolderValidation.FolderFound)?.settingsFileFound == true
    val folderExistsButLooksWrong = validation is LogFolderValidation.FolderFound && !settingsFileFound

    StepScaffold(
        title = "ES-DE folder",
        description = "Select the ES-DE folder ES-DE itself is configured to use.\nCurrently: $path",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = settingsFileFound && !isValidating && !isCheckingInstallation,
    ) {
        when {
            isValidating -> CircularProgressIndicator()
            settingsFileFound -> Text("Found settings/es_settings.xml - this looks right.")
            folderExistsButLooksWrong ->
                Text(
                    "This folder exists, but settings/es_settings.xml wasn't found inside it - " +
                        "it appears to be the incorrect folder. Choose the correct one to continue.",
                )
            validation is LogFolderValidation.FolderNotFound ->
                Text(
                    "This folder doesn't exist. Choose the correct folder.",
                )
        }

        if (isCheckingInstallation) {
            CircularProgressIndicator()
            Text("Checking ES-DE's installation...")
        }

        OutlinedButton(onClick = { launcher.launch(null) }) {
            Text("Choose different folder")
        }
    }
}

@Composable
private fun MediaFolderStep(
    path: String,
    validation: MediaFolderValidation?,
    isValidating: Boolean,
    autoDetected: Boolean,
    onPickFolder: (Uri) -> Unit,
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let(onPickFolder)
        }

    StepScaffold(
        title = "Media folder",
        description = "ES-DE's downloaded_media folder, used to show game artwork.\nCurrently: $path",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = !isValidating,
    ) {
        when {
            isValidating -> CircularProgressIndicator()
            validation is MediaFolderValidation.FolderFound -> Text("Folder found.")
            validation is MediaFolderValidation.FolderNotFound && autoDetected ->
                Text(
                    "ES-DE's settings point at this path, but it doesn't exist - choose the correct folder.",
                )
            validation is MediaFolderValidation.FolderNotFound ->
                Text(
                    "This folder doesn't exist. Choose the correct folder.",
                )
        }

        OutlinedButton(onClick = { launcher.launch(null) }) {
            Text("Choose different folder")
        }
    }
}

@Composable
private fun LegacyScriptsStep(
    scriptFiles: List<String>,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
) {
    StepScaffold(
        title = "Leftover script files",
        description =
            "Found ${scriptFiles.size} leftover script file(s) from an older " +
                "version of ES-DE Companion. These are no longer required - removing them " +
                "will lead to improved performance.",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = !isDeleting,
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
    }
}

@Composable
private fun EventScriptSettingsStep(
    settings: EsdeEventScriptSettings?,
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
) {
    val allEnabled = settings?.allEnabled == true

    StepScaffold(
        title = "ES-DE settings",
        description =
            "ES-DE Companion needs a few settings enabled in ES-DE itself: " +
                "Main Menu > Other Settings >",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = allEnabled,
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

        if (allEnabled) {
            Text("Confirmed - these settings are enabled correctly.")
        } else {
            Text(
                "Make these changes in ES-DE, then navigate back out of the settings menu - " +
                    "this will be detected automatically.",
            )
        }

        if (settings?.debugSkipInputLogging == true) {
            Text(
                "Warning: \"DebugSkipInputLogging\" is enabled in es_settings.xml. ES-DE " +
                    "Companion won't be able to detect which direction you're navigating, so " +
                    "slide animations won't work correctly. This isn't required to fix - if " +
                    "you'd like to, this option isn't in ES-DE's settings menu, so edit " +
                    "es_settings.xml directly and set it back to false.",
            )
        }
    }
}

@Composable
private fun LiveLogCheckStep(
    connectionState: EsdeConnectionState?,
    passed: Boolean,
    isCompleting: Boolean,
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
) {
    StepScaffold(
        title = "Confirm it's working",
        description =
            "Browse to a system or game in ES-DE now, to confirm the log is " +
                "found and generating activity.",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = passed && !isCompleting,
        nextLabel = if (isCompleting) "Finishing..." else "Finish setup",
    ) {
        when {
            connectionState == null -> CircularProgressIndicator()
            connectionState is EsdeConnectionState.LogFileNotFound ->
                Text("es_log.txt not found - check the folder you selected, then go back and fix it.")
            passed -> Text("Working! ES-DE activity detected.")
            else -> Text("Waiting for activity - browse a system or game in ES-DE now.")
        }
    }
}

@Composable
private fun StepScaffold(
    title: String,
    description: String,
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
    nextEnabled: Boolean,
    nextLabel: String = "Next",
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(onClick = { onBack?.invoke() }, enabled = onBack != null) {
                Text("Back")
            }
            Button(onClick = onNext, enabled = nextEnabled) {
                Text(nextLabel)
            }
        }
    }
}
