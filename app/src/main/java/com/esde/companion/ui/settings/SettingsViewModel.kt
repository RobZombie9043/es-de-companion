package com.esde.companion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.ObserveDrawerOpacityUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayEnabledUseCase
import com.esde.companion.domain.usecase.ObserveThemePreferenceUseCase
import com.esde.companion.domain.usecase.ObserveWidgetsLockedUseCase
import com.esde.companion.domain.usecase.SetDrawerOpacityUseCase
import com.esde.companion.domain.usecase.SetGridColumnsUseCase
import com.esde.companion.domain.usecase.SetOverlayEnabledUseCase
import com.esde.companion.domain.usecase.SetThemePreferenceUseCase
import com.esde.companion.domain.usecase.SetWidgetsLockedUseCase
import com.esde.companion.domain.usecase.ValidateEsdeLogFolderUseCase
import com.esde.companion.domain.usecase.ValidateEsdeMediaFolderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val validateLogFolderUseCase: ValidateEsdeLogFolderUseCase,
    private val validateMediaFolderUseCase: ValidateEsdeMediaFolderUseCase,
    private val observeOverlayEnabledUseCase: ObserveOverlayEnabledUseCase,
    private val setOverlayEnabledUseCase: SetOverlayEnabledUseCase,
    private val observeThemePreferenceUseCase: ObserveThemePreferenceUseCase,
    private val setThemePreferenceUseCase: SetThemePreferenceUseCase,
    private val observeDrawerOpacityUseCase: ObserveDrawerOpacityUseCase,
    private val setDrawerOpacityUseCase: SetDrawerOpacityUseCase,
    private val observeGridColumnsUseCase: ObserveGridColumnsUseCase,
    private val setGridColumnsUseCase: SetGridColumnsUseCase,
    private val observeWidgetsLockedUseCase: ObserveWidgetsLockedUseCase,
    private val setWidgetsLockedUseCase: SetWidgetsLockedUseCase,
) : ViewModel() {

    // Seeded with the real value up front - see OnboardingViewModel's kdoc for why
    // relying solely on the screen's ON_RESUME DisposableEffect isn't sufficient.
    private val _uiState = MutableStateFlow(
        SettingsUiState(permissionGranted = AllFilesAccessPermission.isGranted()),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val logPath = onboardingRepository.observeLogFolderPath().first()
                ?: onboardingRepository.defaultLogFolderPath()
            val mediaPath = onboardingRepository.observeMediaFolderPath().first()
                ?: onboardingRepository.defaultMediaFolderPath()
            val overlayEnabled = observeOverlayEnabledUseCase().first()
            val themePreference = observeThemePreferenceUseCase().first()
            val drawerOpacityPercent = observeDrawerOpacityUseCase().first()
            val gridColumns = observeGridColumnsUseCase().first()
            val widgetsLocked = observeWidgetsLockedUseCase().first()
            _uiState.value = _uiState.value.copy(
                logFolderPath = logPath,
                mediaFolderPath = mediaPath,
                overlayEnabled = overlayEnabled,
                themePreference = themePreference,
                drawerOpacityPercent = drawerOpacityPercent,
                gridColumns = gridColumns,
                widgetsLocked = widgetsLocked,
            )
            validateLogFolder(logPath)
            validateMediaFolder(mediaPath)
        }
    }

    fun refreshPermissionState(granted: Boolean) {
        _uiState.value = _uiState.value.copy(permissionGranted = granted)
    }

    fun onLogFolderPicked(path: String) {
        _uiState.value = _uiState.value.copy(logFolderPath = path)
        viewModelScope.launch {
            validateLogFolder(path)
            onboardingRepository.saveLogFolderPath(path)
        }
    }

    fun onMediaFolderPicked(path: String) {
        _uiState.value = _uiState.value.copy(mediaFolderPath = path)
        viewModelScope.launch {
            validateMediaFolder(path)
            onboardingRepository.saveMediaFolderPath(path)
        }
    }

    fun onOverlayEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(overlayEnabled = enabled)
        viewModelScope.launch { setOverlayEnabledUseCase(enabled) }
    }

    fun onWidgetsLockedChanged(locked: Boolean) {
        _uiState.value = _uiState.value.copy(widgetsLocked = locked)
        viewModelScope.launch { setWidgetsLockedUseCase(locked) }
    }

    fun onThemePreferenceChanged(preference: ThemePreference) {
        _uiState.value = _uiState.value.copy(themePreference = preference)
        viewModelScope.launch { setThemePreferenceUseCase(preference) }
    }

    fun onDrawerOpacityChanged(percent: Int) {
        _uiState.value = _uiState.value.copy(drawerOpacityPercent = percent)
        viewModelScope.launch { setDrawerOpacityUseCase(percent) }
    }

    fun onGridColumnsChanged(columns: Int) {
        _uiState.value = _uiState.value.copy(gridColumns = columns)
        viewModelScope.launch { setGridColumnsUseCase(columns) }
    }

    private suspend fun validateLogFolder(path: String) {
        _uiState.value = _uiState.value.copy(isValidatingLogFolder = true)
        val result = validateLogFolderUseCase(path)
        _uiState.value = _uiState.value.copy(isValidatingLogFolder = false, logFolderValidation = result)
    }

    private suspend fun validateMediaFolder(path: String) {
        _uiState.value = _uiState.value.copy(isValidatingMediaFolder = true)
        val result = validateMediaFolderUseCase(path)
        _uiState.value = _uiState.value.copy(isValidatingMediaFolder = false, mediaFolderValidation = result)
    }
}