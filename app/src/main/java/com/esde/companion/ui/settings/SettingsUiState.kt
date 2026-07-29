package com.esde.companion.ui.settings

import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.ThemePreference

data class SettingsUiState(
    val permissionGranted: Boolean = false,
    val logFolderPath: String = "",
    val logFolderValidation: LogFolderValidation? = null,
    val isValidatingLogFolder: Boolean = false,
    val mediaFolderPath: String = "",
    val mediaFolderValidation: MediaFolderValidation? = null,
    val isValidatingMediaFolder: Boolean = false,
    val overlayEnabled: Boolean = true,
    val themePreference: ThemePreference = ThemePreference.Auto,
    val drawerOpacityPercent: Int = 80,
    val gridColumns: Int = 4,
)