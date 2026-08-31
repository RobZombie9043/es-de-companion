package com.esde.companion.ui.settings

import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.GameLaunchDisplayTarget
import com.esde.companion.domain.model.HallSensorCalibration
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.TaskKillerTarget
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.model.VolumeSyncMode

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
    val sortFoldersOnTop: Boolean = true,
    val showSearchBar: Boolean = true,
    val dockEnabled: Boolean = false,
    val dockMaxApps: Int = 5,
    val dockSize: DockSize = DockSize.Medium,
    val gamePlayingBehavior: ScreenBehavior = ScreenBehavior.Nothing,
    val gamePlayingDimPercent: Int = 50,
    val screensaverBehavior: ScreenBehavior = ScreenBehavior.Nothing,
    val screensaverDimPercent: Int = 50,
    val musicEnabled: Boolean = true,
    val musicPlayWhileBrowsingSystems: Boolean = true,
    val musicPlayWhileBrowsingGames: Boolean = true,
    val musicPlayDuringScreensaver: Boolean = true,
    val musicDuckingMode: MusicDuckingMode = MusicDuckingMode.LowerVolume,
    val customMusicFolderPath: String? = null,
    val customMusicFolderValidation: MediaFolderValidation? = null,
    val isValidatingCustomMusicFolder: Boolean = false,
    val closeCompanionOnQuitEnabled: Boolean = false,
    val manualFallbackOnNoGuideEnabled: Boolean = false,
    val updateGameGuidesOnScreensaverEnabled: Boolean = false,
    val fabAssignments: FabAssignments = FabAssignments.Default,
    val installedApps: List<InstalledApp> = emptyList(),
    val launchEsdeOnStartEnabled: Boolean = false,
    val debugLoggingEnabled: Boolean = false,
    val retroAchievementsCredentials: RetroAchievementsCredentials? = null,
    val retroAchievementsUsernameInput: String = "",
    val retroAchievementsWebApiKeyInput: String = "",
    val isConnectingToRetroAchievements: Boolean = false,
    val retroAchievementsConnectError: String? = null,
    val updateAchievementsOnScreensaverEnabled: Boolean = true,
    val lidWakeGuardEnabled: Boolean = false,
    val hallSensorCalibration: HallSensorCalibration = HallSensorCalibration.Uncalibrated,
    val autoFpsEnabled: Boolean = false,
    val thorAccessibilityGranted: Boolean = false,
    val thorPrivilegedServiceAvailable: Boolean = false,
    val taskKillerEnabled: Boolean = false,
    val taskKillerTarget: TaskKillerTarget = TaskKillerTarget.FocusApp,
    val volumeSyncEnabled: Boolean = false,
    val volumeSyncMode: VolumeSyncMode = VolumeSyncMode.Linked,
    val volumeSyncSecondarySettingPresent: Boolean = false,
    val gameLaunchDisplayTarget: GameLaunchDisplayTarget = GameLaunchDisplayTarget.ThisScreen,
    val closeAppOnGameEndEnabled: Boolean = false,
    val gameLaunchEnabled: Boolean = true,
)
