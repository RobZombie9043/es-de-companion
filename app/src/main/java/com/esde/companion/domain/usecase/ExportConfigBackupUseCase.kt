package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AppConfigBackup
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.repository.BackupRepositories
import com.esde.companion.domain.repository.ConfigBackupRepository
import kotlinx.coroutines.flow.first

/**
 * Captures every setting [AppConfigBackup] covers into one serialized snapshot, for
 * Settings > Setup > Backup & Restore's Export Backup action. Each value is read via a
 * single `.first()` off its repository's observe `Flow` - a one-shot snapshot, not a live
 * subscription, matching what "export the current state" means.
 */
@Suppress("LongParameterList")
class ExportConfigBackupUseCase(
    private val repositories: BackupRepositories,
    private val configBackupRepository: ConfigBackupRepository,
) {
    suspend operator fun invoke(): String {
        val widgetCanvases =
            StateGroup.entries.associateWith { stateGroup ->
                repositories.widgetLayoutRepository.observeCanvas(stateGroup).first()
            }
        val updateAchievementsOnScreensaverEnabled =
            repositories.onboardingRepository.observeUpdateAchievementsOnScreensaverEnabled().first()
        val snapshot =
            with(repositories) {
                AppConfigBackup(
                    logFolderPath = onboardingRepository.observeLogFolderPath().first(),
                    mediaFolderPath = onboardingRepository.observeMediaFolderPath().first(),
                    customSystemImagesFolderPath = onboardingRepository.observeCustomSystemImagesFolderPath().first(),
                    customLogosFolderPath = onboardingRepository.observeCustomLogosFolderPath().first(),
                    customMusicFolderPath = onboardingRepository.observeCustomMusicFolderPath().first(),
                    themePreference = onboardingRepository.observeThemePreference().first(),
                    gamePlayingBehavior = onboardingRepository.observeGamePlayingBehavior().first(),
                    gamePlayingDimPercent = onboardingRepository.observeGamePlayingDimPercent().first(),
                    screensaverBehavior = onboardingRepository.observeScreensaverBehavior().first(),
                    screensaverDimPercent = onboardingRepository.observeScreensaverDimPercent().first(),
                    overlayOpacityPercent = onboardingRepository.observeOverlayOpacityPercent().first(),
                    fabAssignments = onboardingRepository.observeFabAssignments().first(),
                    musicEnabled = onboardingRepository.observeMusicEnabled().first(),
                    musicPlayWhileBrowsingSystems = onboardingRepository.observeMusicPlayWhileBrowsingSystems().first(),
                    musicPlayWhileBrowsingGames = onboardingRepository.observeMusicPlayWhileBrowsingGames().first(),
                    musicPlayDuringScreensaver = onboardingRepository.observeMusicPlayDuringScreensaver().first(),
                    musicDuckingMode = onboardingRepository.observeMusicDuckingMode().first(),
                    closeCompanionOnQuitEnabled = onboardingRepository.observeCloseCompanionOnQuitEnabled().first(),
                    launchEsdeOnStartEnabled = onboardingRepository.observeLaunchEsdeOnStartEnabled().first(),
                    debugLoggingEnabled = onboardingRepository.observeDebugLoggingEnabled().first(),
                    hiddenApps = appDrawerSettingsRepository.observeHiddenApps().first(),
                    gridColumns = appDrawerSettingsRepository.observeGridColumns().first(),
                    otherScreenLaunchApps = appDrawerSettingsRepository.observeOtherScreenLaunchApps().first(),
                    sortFoldersOnTop = appDrawerSettingsRepository.observeSortFoldersOnTop().first(),
                    showSearchBar = appDrawerSettingsRepository.observeShowSearchBar().first(),
                    folders = appFolderRepository.observeFolders().first(),
                    dockEnabled = dockSettingsRepository.observeDockEnabled().first(),
                    dockMaxApps = dockSettingsRepository.observeDockMaxApps().first(),
                    dockSize = dockSettingsRepository.observeDockSize().first(),
                    dockApps = dockSettingsRepository.observeDockApps().first(),
                    widgetCanvases = widgetCanvases,
                    gameMatchOverrides = gameMatchOverrideRepository.observeAllOverrides().first(),
                    updateAchievementsOnScreensaverEnabled = updateAchievementsOnScreensaverEnabled,
                    lidWakeGuardEnabled = thorSettingsRepository.observeLidWakeGuardEnabled().first(),
                    hallSensorCalibration = thorSettingsRepository.observeHallSensorCalibration().first(),
                    autoFpsEnabled = thorSettingsRepository.observeAutoFpsEnabled().first(),
                    autoFpsTriggerPackages = thorSettingsRepository.observeAutoFpsTriggerPackages().first(),
                    taskKillerEnabled = thorSettingsRepository.observeTaskKillerEnabled().first(),
                    taskKillerExcludedPackages = thorSettingsRepository.observeTaskKillerExcludedPackages().first(),
                    volumeSyncEnabled = thorSettingsRepository.observeVolumeSyncEnabled().first(),
                    volumeSyncMode = thorSettingsRepository.observeVolumeSyncMode().first(),
                    gameLaunchSystemDefaults = gameLaunchAppRepository.observeSystemDefaults().first(),
                    gameLaunchOverrides = gameLaunchAppRepository.observeGameOverrides().first(),
                    gameLaunchDisplayTarget = gameLaunchAppRepository.observeLaunchDisplayTarget().first(),
                    closeAppOnGameEndEnabled = gameLaunchAppRepository.observeCloseAppOnGameEnd().first(),
                )
            }
        return configBackupRepository.serialize(snapshot)
    }
}
