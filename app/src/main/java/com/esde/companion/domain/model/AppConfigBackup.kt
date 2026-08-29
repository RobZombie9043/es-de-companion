package com.esde.companion.domain.model

/**
 * A point-in-time snapshot of every user-configurable setting this app persists, for the
 * Settings > Setup > Backup & Restore feature. Deliberately excludes state that isn't real
 * user configuration: [OnboardingRepository.observeOnboardingComplete] (this app's
 * first-run flag), anything from `LastKnownContextRepository` (transient widget-edit-mode
 * tracking data), and `UpdateStateRepository`'s last-seen-changelog-version bookkeeping.
 * [gameLaunchSystemDefaults]/[gameLaunchOverrides] are likewise user-curated library data,
 * included for the same reason as [gameMatchOverrides] below.
 *
 * [gameMatchOverrides] is user-curated library data rather than sensitive, so - unlike
 * RetroAchievements' username/Web API Key, which are deliberately never wired into
 * [com.esde.companion.domain.usecase.ExportConfigBackupUseCase]/
 * [com.esde.companion.domain.usecase.RestoreConfigBackupUseCase] - it's included here.
 *
 * [version] lets a future schema change be detected on restore - see
 * [com.esde.companion.domain.repository.ConfigBackupRepository]'s kdoc for the exact rules.
 * A field added after v1 must get a default so an older backup (missing that field) still
 * decodes - restoring such a backup then explicitly sets that field to its default, since
 * restore is a full overwrite of everything this snapshot type covers, not a partial merge
 * of only the fields present in a given file.
 */
data class AppConfigBackup(
    val version: Int = CURRENT_VERSION,
    val logFolderPath: String?,
    val mediaFolderPath: String?,
    val customSystemImagesFolderPath: String?,
    val customLogosFolderPath: String?,
    val customMusicFolderPath: String?,
    val themePreference: ThemePreference,
    val gamePlayingBehavior: ScreenBehavior,
    val gamePlayingDimPercent: Int,
    val screensaverBehavior: ScreenBehavior,
    val screensaverDimPercent: Int,
    val overlayOpacityPercent: Int,
    val fabAssignments: FabAssignments,
    val musicEnabled: Boolean,
    val musicPlayWhileBrowsingSystems: Boolean,
    val musicPlayWhileBrowsingGames: Boolean,
    val musicPlayDuringScreensaver: Boolean,
    val musicDuckingMode: MusicDuckingMode,
    val closeCompanionOnQuitEnabled: Boolean,
    val launchEsdeOnStartEnabled: Boolean,
    val debugLoggingEnabled: Boolean,
    val hiddenApps: Set<String>,
    val gridColumns: Int,
    val otherScreenLaunchApps: Set<String>,
    val sortFoldersOnTop: Boolean,
    val showSearchBar: Boolean,
    val folders: List<AppFolder>,
    val dockEnabled: Boolean,
    val dockMaxApps: Int,
    val dockSize: DockSize,
    val dockApps: List<String>,
    val widgetCanvases: Map<StateGroup, SavedWidgetCanvas>,
    val gameMatchOverrides: List<GameMatchOverride> = emptyList(),
    val updateAchievementsOnScreensaverEnabled: Boolean = true,
    val lidWakeGuardEnabled: Boolean,
    val hallSensorCalibration: HallSensorCalibration,
    val autoFpsEnabled: Boolean,
    val autoFpsTriggerPackages: Set<String>,
    val taskKillerEnabled: Boolean,
    val taskKillerExcludedPackages: Set<String>,
    val taskKillerTarget: TaskKillerTarget = TaskKillerTarget.FocusApp,
    val volumeSyncEnabled: Boolean,
    val volumeSyncMode: VolumeSyncMode,
    val gameLaunchSystemDefaults: Map<String, String> = emptyMap(),
    val gameLaunchOverrides: List<GameLaunchOverride> = emptyList(),
    val gameLaunchDisplayTarget: GameLaunchDisplayTarget = GameLaunchDisplayTarget.ThisScreen,
    val closeAppOnGameEndEnabled: Boolean = false,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
