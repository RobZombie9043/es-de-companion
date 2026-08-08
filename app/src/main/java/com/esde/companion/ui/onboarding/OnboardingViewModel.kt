package com.esde.companion.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.CompleteOnboardingUseCase
import com.esde.companion.domain.usecase.DeleteLegacyScriptFilesUseCase
import com.esde.companion.domain.usecase.FindLegacyScriptFilesUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveEsdeEventScriptSettingsUseCase
import com.esde.companion.domain.usecase.ObserveEsdeLogActivityUseCase
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
 * Permission -> EsdeFolder -> [MediaFolder] -> [LegacyScripts] -> [EventScriptSettings] ->
 * LiveLogCheck. The three bracketed steps are skipped entirely when there's nothing to fix
 * (MediaFolder when ES-DE's own settings file already names a media folder that exists on
 * disk), decided from installation info fetched once, in the background, right after
 * EsdeFolder is confirmed (see [fetchInstallationInfo]).
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
    private val observeEsdeEventScriptSettingsUseCase: ObserveEsdeEventScriptSettingsUseCase,
    private val findLegacyScriptFilesUseCase: FindLegacyScriptFilesUseCase,
    private val deleteLegacyScriptFilesUseCase: DeleteLegacyScriptFilesUseCase,
    private val observeConnectionStateUseCase: ObserveConnectionStateUseCase,
    private val observeEsdeLogActivityUseCase: ObserveEsdeLogActivityUseCase,
    /** Whatever was already confirmed and persisted from a prior onboarding run, if any -
     * null on a genuinely fresh install. Lets re-entering onboarding solely because the
     * permission was revoked post-completion (see MainActivity) seed EsdeFolder/MediaFolder
     * with the real prior location instead of the hardcoded default guess. */
    private val savedLogFolderPath: String? = null,
    private val savedMediaFolderPath: String? = null,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            run {
                val permissionGranted = AllFilesAccessPermission.isGranted()
                OnboardingUiState(
                    step = if (permissionGranted) OnboardingStep.EsdeFolder else OnboardingStep.Permission,
                    permissionGranted = permissionGranted,
                    logFolderPath = savedLogFolderPath ?: onboardingRepository.defaultLogFolderPath(),
                    mediaFolderPath = savedMediaFolderPath ?: onboardingRepository.defaultMediaFolderPath(),
                )
            },
        )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _onboardingComplete = Channel<Unit>(capacity = Channel.CONFLATED)
    val onboardingComplete = _onboardingComplete.receiveAsFlow()

    private var liveCheckJob: Job? = null
    private var installationInfoJob: Job? = null
    private var eventScriptSettingsJob: Job? = null

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
        validateLogFolder(path, autoConfirmIfValid = true)
    }

    fun onEsdeFolderConfirmed() {
        val path = _uiState.value.logFolderPath
        viewModelScope.launch { onboardingRepository.saveLogFolderPath(path) }
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
            proceedFromMediaFolder()
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
            enterEventScriptSettingsCheck()
        } else {
            enterLiveLogCheck()
        }
    }

    fun onEventScriptSettingsConfirmed() {
        eventScriptSettingsJob?.cancel()
        eventScriptSettingsJob = null
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
        if (state.step == OnboardingStep.EventScriptSettings) {
            eventScriptSettingsJob?.cancel()
            eventScriptSettingsJob = null
        }
        _uiState.value = state.copy(step = previous, stepBackStack = state.stepBackStack.dropLast(1))
    }

    private fun advanceTo(next: OnboardingStep) {
        val state = _uiState.value
        _uiState.value = state.copy(step = next, stepBackStack = state.stepBackStack + state.step)
    }

    /** Reads settings/es_settings.xml (media directory, the 3 event-script flags) and
     * checks for legacy script files, all from [esdeRootPath] - kicked off once, right
     * after EsdeFolder is confirmed, while remaining on the EsdeFolder step (gated by
     * [OnboardingUiState.isCheckingInstallation]).
     *
     * If the media directory is both found in ES-DE's own settings file and validated as
     * an existing folder, that's a confirmed-correct location straight from ES-DE - the
     * MediaFolder step is skipped entirely rather than making the user re-confirm
     * something ES-DE already told us. It's only shown if auto-detection comes back empty
     * or points at a folder that doesn't actually exist, so the user can pick one by hand.
     */
    private fun fetchInstallationInfo(esdeRootPath: String) {
        installationInfoJob =
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isCheckingInstallation = true)

                val mediaDirectoryDeferred = async { readEsdeMediaDirectoryUseCase(esdeRootPath) }
                val eventScriptSettingsDeferred = async { readEsdeEventScriptSettingsUseCase(esdeRootPath) }
                val legacyScriptFilesDeferred = async { findLegacyScriptFilesUseCase(esdeRootPath) }

                val mediaDirectory = mediaDirectoryDeferred.await()
                val mediaFolderValidation = mediaDirectory?.let { validateMediaFolderUseCase(it) }

                _uiState.value =
                    _uiState.value.copy(
                        mediaFolderPath = mediaDirectory ?: _uiState.value.mediaFolderPath,
                        mediaFolderAutoDetected = mediaDirectory != null,
                        mediaFolderValidation = mediaFolderValidation,
                        eventScriptSettings = eventScriptSettingsDeferred.await(),
                        legacyScriptFiles = legacyScriptFilesDeferred.await(),
                        isCheckingInstallation = false,
                    )

                if (mediaDirectory != null && mediaFolderValidation is MediaFolderValidation.FolderFound) {
                    onboardingRepository.saveMediaFolderPath(mediaDirectory)
                    proceedFromMediaFolder()
                } else {
                    advanceTo(OnboardingStep.MediaFolder)
                    // Auto-detection found nothing - validate the fallback default guess so
                    // the step doesn't land with a blank validation state.
                    if (mediaDirectory == null) validateMediaFolder(_uiState.value.mediaFolderPath)
                }
            }
    }

    /** Shared by both the auto-skip path in [fetchInstallationInfo] and the manual
     * [onMediaFolderConfirmed] path - decides which of LegacyScripts/EventScriptSettings/
     * LiveLogCheck to show next, from whatever [fetchInstallationInfo] already found. Safe
     * to call from either without an extra join: the manual path only reaches MediaFolder
     * once [installationInfoJob] has already completed (see [fetchInstallationInfo]), so
     * its results are always ready here. */
    private fun proceedFromMediaFolder() {
        val current = _uiState.value
        when {
            current.legacyScriptFiles.isNotEmpty() -> advanceTo(OnboardingStep.LegacyScripts)
            current.eventScriptSettings?.allEnabled != true -> enterEventScriptSettingsCheck()
            else -> enterLiveLogCheck()
        }
    }

    /** Advances to EventScriptSettings and starts monitoring settings/es_settings.xml for
     * changes - ES-DE rewrites this file once the user navigates back out of its settings
     * menu (see [ObserveEsdeEventScriptSettingsUseCase]), so this lets the step confirm
     * itself automatically rather than requiring a manual re-check. Cancelled on
     * [onEventScriptSettingsConfirmed] and on stepping back out of this step via [onBack]. */
    private fun enterEventScriptSettingsCheck() {
        val esdeRootPath = _uiState.value.logFolderPath
        advanceTo(OnboardingStep.EventScriptSettings)
        eventScriptSettingsJob?.cancel()
        eventScriptSettingsJob =
            viewModelScope.launch {
                observeEsdeEventScriptSettingsUseCase(esdeRootPath).collect { settings ->
                    _uiState.value = _uiState.value.copy(eventScriptSettings = settings)
                }
            }
    }

    /** Subscribes to two independent streams while this step is showing, both cancelled
     * and restarted together on re-entry (including via onBack + forward again) so
     * neither can carry stale state from a previous visit:
     *
     * - The app-wide shared connection-state stream (not a fresh instance - see CLAUDE.md's
     *   gotcha on cold Flows duplicated per collector), purely to drive the UI's
     *   found/not-found/waiting message.
     * - The raw event stream ([ObserveEsdeLogActivityUseCase]), which is what actually
     *   flips [OnboardingUiState.liveCheckPassed] true - proving a fresh
     *   Scripting::fireEvent() line was parsed while the user was watching. This
     *   deliberately does NOT diff against a captured "baseline" AppState/connection
     *   value: on a second visit to this step (go back, then forward again), the baseline
     *   would already be whatever AppState the *first* visit ended on, so repeating the
     *   exact same ES-DE navigation - the natural thing for a user re-attempting this step
     *   to do - would reduce to that identical value and never register as "different."
     *   Worse, the derived AppState/connection stream is backed by a StateFlow, which
     *   doesn't even emit downstream when a new value structurally equals the one it
     *   already holds - so a baseline-diff here could see *zero* emissions at all for a
     *   repeated action, not just a false "unchanged" one. Any event on the raw stream,
     *   regardless of content, is unambiguous proof of life. */
    private fun enterLiveLogCheck() {
        advanceTo(OnboardingStep.LiveLogCheck)
        _uiState.value = _uiState.value.copy(liveCheckPassed = false)
        liveCheckJob?.cancel()
        liveCheckJob =
            viewModelScope.launch {
                launch {
                    observeConnectionStateUseCase().collect { connectionState ->
                        _uiState.value = _uiState.value.copy(connectionState = connectionState)
                    }
                }
                launch {
                    observeEsdeLogActivityUseCase().collect {
                        _uiState.value = _uiState.value.copy(liveCheckPassed = true)
                    }
                }
            }
    }

    /** Validates [path] as ES-DE's root folder. [autoConfirmIfValid] is true only when the
     * user just actively picked this folder via the file browser ([onEsdeFolderPicked]) -
     * a folder they deliberately chose is auto-confirmed the instant it validates
     * (settings/es_settings.xml actually found inside it), same "don't ask again for
     * something already confirmed" principle applied to auto-skipping MediaFolder (see
     * [fetchInstallationInfo]).
     *
     * It's false for the very first validation of the default guessed path (from [init]/
     * [onPermissionResult]) even when that guess turns out correct - a location the user
     * never actually chose still gets shown to them once, with Next already enabled, so
     * they can see and confirm what was found (or pick something else) rather than being
     * skipped straight past a step they haven't seen yet.
     *
     * Guarded by re-checking step/path once validation resolves, since this runs async and
     * the user may have since picked a different folder or moved on entirely. */
    private fun validateLogFolder(
        path: String,
        autoConfirmIfValid: Boolean = false,
    ) {
        _uiState.value = _uiState.value.copy(isValidatingLogFolder = true)
        viewModelScope.launch {
            val result = validateLogFolderUseCase(path)
            _uiState.value = _uiState.value.copy(isValidatingLogFolder = false, logFolderValidation = result)

            val stillOnThisStepWithThisPath =
                _uiState.value.step == OnboardingStep.EsdeFolder && _uiState.value.logFolderPath == path
            val settingsFileFound = (result as? LogFolderValidation.FolderFound)?.settingsFileFound == true
            if (autoConfirmIfValid && stillOnThisStepWithThisPath && settingsFileFound) {
                onEsdeFolderConfirmed()
            }
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
