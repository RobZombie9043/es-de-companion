package com.esde.companion.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.CompleteOnboardingUseCase
import com.esde.companion.domain.usecase.DeleteLegacyScriptFilesUseCase
import com.esde.companion.domain.usecase.FindLegacyScriptFilesUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ReadEsdeEventScriptSettingsUseCase
import com.esde.companion.domain.usecase.ReadEsdeMediaDirectoryUseCase
import com.esde.companion.domain.usecase.ValidateEsdeLogFolderUseCase
import com.esde.companion.domain.usecase.ValidateEsdeMediaFolderUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Drives the first-run onboarding flow. Forward sequence (see [OnboardingStep]):
 * Permission -> EsdeFolder -> MediaFolder -> [LegacyScripts] -> [EventScriptSettings] ->
 * LiveLogCheck. The two bracketed steps are skipped entirely when there's nothing to fix,
 * decided from installation info fetched once, in the background, right after EsdeFolder
 * is confirmed (see [fetchInstallationInfo]).
 *
 * Unlike the old linear-forward-only flow, this supports going back ([onBack]) via a plain
 * back-stack of visited steps - LiveLogCheck's troubleshooting needs a way to send the user
 * back to fix the folder or ES-DE's settings rather than being a dead end. Same manual
 * back-stack idiom CLAUDE.md already established for SettingsScreen/EditWidgetsOverlay
 * instead of NavHost.
 *
 * Folder paths are persisted as soon as each is confirmed (not deferred to the very end)
 * so the app-wide shared log repository is already tailing the right file by the time
 * LiveLogCheck needs it - see saveLogFolderPath's "re-enterable by design" kdoc.
 * [completeOnboardingUseCase] at the very end re-saves the same paths (idempotent) and
 * marks onboarding complete.
 *
 * Actual permission-grant state and folder browsing are Android-framework concerns the
 * composable/Activity layer owns - this ViewModel only receives their *results* via
 * [onPermissionResult] and [onEsdeFolderPicked]/[onMediaFolderPicked].
 */
class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val validateLogFolderUseCase: ValidateEsdeLogFolderUseCase,
    private val validateMediaFolderUseCase: ValidateEsdeMediaFolderUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val readEsdeMediaDirectoryUseCase: ReadEsdeMediaDirectoryUseCase,
    private val readEsdeEventScriptSettingsUseCase: ReadEsdeEventScriptSettingsUseCase,
    private val findLegacyScriptFilesUseCase: FindLegacyScriptFilesUseCase,
    private val deleteLegacyScriptFilesUseCase: DeleteLegacyScriptFilesUseCase,
    private val observeConnectionStateUseCase: ObserveConnectionStateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        run {
            val permissionGranted = AllFilesAccessPermission.isGranted()
            OnboardingUiState(
                step = if (permissionGranted) OnboardingStep.EsdeFolder else OnboardingStep.Permission,
                permissionGranted = permissionGranted,
                logFolderPath = onboardingRepository.defaultLogFolderPath(),
                mediaFolderPath = onboardingRepository.defaultMediaFolderPath(),
            )
        },
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _onboardingComplete = Channel<Unit>(capacity = Channel.CONFLATED)
    val onboardingComplete = _onboardingComplete.receiveAsFlow()

    private var liveCheckJob: Job? = null
    private var installationInfoJob: Job? = null

    init {
        // See the class kdoc's seeding rationale - only kick off validation immediately
        // when we started straight on EsdeFolder because permission was already granted.
        if (_uiState.value.step == OnboardingStep.EsdeFolder) {
            validateLogFolder(_uiState.value.logFolderPath)
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(permissionGranted = granted)
        if (granted && _uiState.value.step == OnboardingStep.Permission) {
            advanceTo(OnboardingStep.EsdeFolder)
            validateLogFolder(_uiState.value.logFolderPath)
        }
    }

    fun onEsdeFolderPicked(path: String) {
        _uiState.value = _uiState.value.copy(logFolderPath = path)
        validateLogFolder(path)
    }

    fun onEsdeFolderConfirmed() {
        val path = _uiState.value.logFolderPath
        viewModelScope.launch { onboardingRepository.saveLogFolderPath(path) }
        advanceTo(OnboardingStep.MediaFolder)
        validateMediaFolder(_uiState.value.mediaFolderPath)
        fetchInstallationInfo(path)
    }

    fun onMediaFolderPicked(path: String) {
        _uiState.value = _uiState.value.copy(mediaFolderPath = path, mediaFolderAutoDetected = false)
        validateMediaFolder(path)
    }

    fun onMediaFolderConfirmed() {
        val state = _uiState.value
        viewModelScope.launch {
            onboardingRepository.saveMediaFolderPath(state.mediaFolderPath)
            // Wait for fetchInstallationInfo (kicked off on EsdeFolder confirm) to finish
            // rather than deciding from whatever's in state right now - otherwise tapping
            // through quickly can race ahead of it (confirmed bug: on slower storage, e.g.
            // an SD card, the legacy-script check can lose the race and look like nothing
            // was found even when files are present).
            installationInfoJob?.join()
            val current = _uiState.value
            when {
                current.legacyScriptFiles.isNotEmpty() -> advanceTo(OnboardingStep.LegacyScripts)
                current.eventScriptSettings?.allEnabled != true -> advanceTo(OnboardingStep.EventScriptSettings)
                else -> enterLiveLogCheck()
            }
        }
    }

    fun onDeleteLegacyScriptFiles() {
        val esdeRootPath = _uiState.value.logFolderPath
        _uiState.value = _uiState.value.copy(isDeletingLegacyScripts = true)
        viewModelScope.launch {
            deleteLegacyScriptFilesUseCase(esdeRootPath)
            _uiState.value = _uiState.value.copy(isDeletingLegacyScripts = false, legacyScriptFiles = emptyList())
        }
    }

    fun onLegacyScriptsConfirmed() {
        if (_uiState.value.eventScriptSettings?.allEnabled != true) {
            advanceTo(OnboardingStep.EventScriptSettings)
        } else {
            enterLiveLogCheck()
        }
    }

    fun onEventScriptSettingsConfirmed() {
        enterLiveLogCheck()
    }

    fun onFinishSetup() {
        val state = _uiState.value
        _uiState.value = state.copy(isCompleting = true)
        viewModelScope.launch {
            completeOnboardingUseCase(state.logFolderPath, state.mediaFolderPath)
            _onboardingComplete.trySend(Unit)
        }
    }

    fun onBack() {
        val state = _uiState.value
        val previous = state.stepBackStack.lastOrNull() ?: return
        if (state.step == OnboardingStep.LiveLogCheck) {
            liveCheckJob?.cancel()
            liveCheckJob = null
        }
        _uiState.value = state.copy(step = previous, stepBackStack = state.stepBackStack.dropLast(1))
    }

    private fun advanceTo(next: OnboardingStep) {
        val state = _uiState.value
        _uiState.value = state.copy(step = next, stepBackStack = state.stepBackStack + state.step)
    }

    /** Reads settings/es_settings.xml (media directory, the 3 event-script flags) and
     * checks for legacy script files, all from [esdeRootPath] - kicked off once, right
     * after EsdeFolder is confirmed, so MediaFolder/LegacyScripts/EventScriptSettings can
     * decide what to show without their own loading step. The three reads run
     * concurrently, but [installationInfoJob] as a whole is joined by
     * [onMediaFolderConfirmed] before it decides what to show next - see that kdoc for why
     * that join is required for correctness, not just a nicety. */
    private fun fetchInstallationInfo(esdeRootPath: String) {
        installationInfoJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingInstallation = true)

            val mediaDirectoryDeferred = async { readEsdeMediaDirectoryUseCase(esdeRootPath) }
            val eventScriptSettingsDeferred = async { readEsdeEventScriptSettingsUseCase(esdeRootPath) }
            val legacyScriptFilesDeferred = async { findLegacyScriptFilesUseCase(esdeRootPath) }

            val mediaDirectory = mediaDirectoryDeferred.await()
            if (mediaDirectory != null) {
                _uiState.value = _uiState.value.copy(mediaFolderPath = mediaDirectory, mediaFolderAutoDetected = true)
                validateMediaFolder(mediaDirectory)
            }
            _uiState.value = _uiState.value.copy(
                eventScriptSettings = eventScriptSettingsDeferred.await(),
                legacyScriptFiles = legacyScriptFilesDeferred.await(),
                isCheckingInstallation = false,
            )
        }
    }

    /** Subscribes to the app-wide shared connection-state stream (not a fresh instance -
     * see CLAUDE.md's gotcha on cold Flows duplicated per collector) and requires an
     * emission distinct from whatever was seen *first* upon entering this step before
     * [OnboardingUiState.liveCheckPassed] flips true - proving a fresh event was parsed
     * while the user was watching, not just a replayed/stale value. Cancelled and
     * restarted on re-entry (including via onBack + forward again) so a stale baseline
     * from a previous visit can't linger. */
    private fun enterLiveLogCheck() {
        advanceTo(OnboardingStep.LiveLogCheck)
        liveCheckJob?.cancel()
        liveCheckJob = viewModelScope.launch {
            var baseline: EsdeConnectionState? = null
            var baselineCaptured = false
            observeConnectionStateUseCase().collect { connectionState ->
                if (!baselineCaptured) {
                    baseline = connectionState
                    baselineCaptured = true
                    _uiState.value = _uiState.value.copy(connectionState = connectionState, liveCheckPassed = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        connectionState = connectionState,
                        liveCheckPassed = _uiState.value.liveCheckPassed || connectionState != baseline,
                    )
                }
            }
        }
    }

    private fun validateLogFolder(path: String) {
        _uiState.value = _uiState.value.copy(isValidatingLogFolder = true)
        viewModelScope.launch {
            val result = validateLogFolderUseCase(path)
            _uiState.value = _uiState.value.copy(isValidatingLogFolder = false, logFolderValidation = result)
        }
    }

    private fun validateMediaFolder(path: String) {
        _uiState.value = _uiState.value.copy(isValidatingMediaFolder = true)
        viewModelScope.launch {
            val result = validateMediaFolderUseCase(path)
            _uiState.value = _uiState.value.copy(isValidatingMediaFolder = false, mediaFolderValidation = result)
        }
    }
}
