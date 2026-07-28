package com.esde.companion.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.CompleteOnboardingUseCase
import com.esde.companion.domain.usecase.ValidateEsdeLogFolderUseCase
import com.esde.companion.domain.usecase.ValidateEsdeMediaFolderUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Drives the first-run onboarding flow: grant "All files access", then pick and confirm
 * the ES-DE root folder and the downloaded_media folder.
 *
 * Actual permission-grant state (`Environment.isExternalStorageManager()`) and folder
 * browsing (the SAF picker) are Android-framework concerns the composable/Activity layer
 * owns - this ViewModel only receives their *results* via [onPermissionResult] and
 * [onLogFolderPicked]/[onMediaFolderPicked], keeping it plain and easy to reason about.
 *
 * The initial [step]/[OnboardingUiState.permissionGranted] are seeded from
 * [AllFilesAccessPermission.isGranted] directly, rather than defaulting to "not granted"
 * and waiting for OnboardingScreen's ON_RESUME DisposableEffect to correct it. The
 * permission is a special app-op the OS tracks against the package, not app-private data,
 * so it can already be granted the first time this ViewModel is constructed - e.g. after
 * "Clear storage" resets onboarding_complete without revoking the OS-level grant. Without
 * this seeding, PermissionStep would flash "Grant access" for a frame before auto-advancing.
 */
class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val validateLogFolderUseCase: ValidateEsdeLogFolderUseCase,
    private val validateMediaFolderUseCase: ValidateEsdeMediaFolderUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        run {
            val permissionGranted = AllFilesAccessPermission.isGranted()
            OnboardingUiState(
                step = if (permissionGranted) OnboardingStep.LogFolder else OnboardingStep.Permission,
                permissionGranted = permissionGranted,
                logFolderPath = onboardingRepository.defaultLogFolderPath(),
                mediaFolderPath = onboardingRepository.defaultMediaFolderPath(),
            )
        },
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _onboardingComplete = Channel<Unit>(capacity = Channel.CONFLATED)
    val onboardingComplete = _onboardingComplete.receiveAsFlow()

    init {
        // If we started straight on LogFolder because permission was already granted,
        // kick off its validation immediately - the old code path only did this inside
        // onPermissionResult() when the transition happened live.
        if (_uiState.value.step == OnboardingStep.LogFolder) {
            validateLogFolder(_uiState.value.logFolderPath)
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(permissionGranted = granted)
        if (granted && _uiState.value.step == OnboardingStep.Permission) {
            _uiState.value = _uiState.value.copy(step = OnboardingStep.LogFolder)
            validateLogFolder(_uiState.value.logFolderPath)
        }
    }

    fun onLogFolderPicked(path: String) {
        _uiState.value = _uiState.value.copy(logFolderPath = path)
        validateLogFolder(path)
    }

    fun onLogFolderConfirmed() {
        _uiState.value = _uiState.value.copy(step = OnboardingStep.MediaFolder)
        validateMediaFolder(_uiState.value.mediaFolderPath)
    }

    fun onMediaFolderPicked(path: String) {
        _uiState.value = _uiState.value.copy(mediaFolderPath = path)
        validateMediaFolder(path)
    }

    fun onMediaFolderConfirmed() {
        val state = _uiState.value
        _uiState.value = state.copy(isCompleting = true)
        viewModelScope.launch {
            completeOnboardingUseCase(state.logFolderPath, state.mediaFolderPath)
            _onboardingComplete.trySend(Unit)
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