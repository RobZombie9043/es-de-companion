package com.esde.companion.ui.settings

import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference

data class SettingsUiState(
    val permissionGranted: Boolean = false,
    val logFolderPath: String = "",
    val logFolderValidation: LogFolderValidation? = null,
    val isValidatingLogFolder: Boolean = false,
    val mediaFolderPath: String = "",
    val mediaFolderValidation: MediaFolderValidation? = null,
    val isValidatingMediaFolder: Boolean = false,
    val customSystemImagesFolderPath: String? = null,
    val customSystemImagesFolderValidation: MediaFolderValidation? = null,
    val isValidatingCustomSystemImagesFolder: Boolean = false,
    val customLogosFolderPath: String? = null,
    val customLogosFolderValidation: MediaFolderValidation? = null,
    val isValidatingCustomLogosFolder: Boolean = false,
    val themePreference: ThemePreference = ThemePreference.Auto,
    val overlayOpacityPercent: Int = 80,
    val gridColumns: Int = 4,
    val dockEnabled: Boolean = false,
    val dockMaxApps: Int = 5,
    val dockSize: DockSize = DockSize.Medium,
    val widgetsLocked: Boolean = false,
    val gamePlayingBehavior: ScreenBehavior = ScreenBehavior.Nothing,
    val screensaverBehavior: ScreenBehavior = ScreenBehavior.Nothing,
    val videoPlaybackEnabled: Boolean = false,
    val videoDelaySeconds: Int = 0,
    val videoAudioEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val musicPlayWhileBrowsingSystems: Boolean = true,
    val musicPlayWhileBrowsingGames: Boolean = true,
    val musicPlayDuringScreensaver: Boolean = true,
    val musicDuckingMode: MusicDuckingMode = MusicDuckingMode.LowerVolume,
    val customMusicFolderPath: String? = null,
    val customMusicFolderValidation: MediaFolderValidation? = null,
    val isValidatingCustomMusicFolder: Boolean = false,
)