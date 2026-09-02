package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AppConfigBackup
import com.esde.companion.domain.repository.BackupRepositories
import com.esde.companion.domain.repository.ConfigBackupRepository

/**
 * Applies a previously-exported [AppConfigBackup] snapshot (see [ExportConfigBackupUseCase])
 * back onto every repository it covers - a full overwrite of everything the snapshot type
 * knows about, not a partial merge (see [AppConfigBackup]'s kdoc). [invoke] fails as a
 * [Result] rather than throwing if [contents] isn't a valid/supported backup, without
 * touching any repository - callers can treat a failed restore as a no-op on current state.
 */
@Suppress("LongParameterList")
class RestoreConfigBackupUseCase(
    private val repositories: BackupRepositories,
    private val configBackupRepository: ConfigBackupRepository,
) {
    suspend operator fun invoke(contents: String): Result<Unit> =
        configBackupRepository.deserialize(contents).map { snapshot -> apply(snapshot) }

    private suspend fun apply(snapshot: AppConfigBackup) {
        applyOnboardingSettings(snapshot)
        with(repositories.appDrawerSettingsRepository) {
            setHiddenApps(snapshot.hiddenApps)
            setGridColumns(snapshot.gridColumns)
            setOtherScreenLaunchApps(snapshot.otherScreenLaunchApps)
            setSortFoldersOnTop(snapshot.sortFoldersOnTop)
            setShowSearchBar(snapshot.showSearchBar)
        }
        repositories.appFolderRepository.setFolders(snapshot.folders)
        with(repositories.dockSettingsRepository) {
            setDockEnabled(snapshot.dockEnabled)
            setDockMaxApps(snapshot.dockMaxApps)
            setDockSize(snapshot.dockSize)
            setDockApps(snapshot.dockApps)
        }
        // A null grid means "nothing meaningfully saved for this canvas yet" (see
        // SavedWidgetCanvas's kdoc) - nothing to restore, so it's skipped rather than
        // guessed at.
        snapshot.widgetCanvases.forEach { (stateGroup, canvas) ->
            val grid = canvas.grid ?: return@forEach
            repositories.widgetLayoutRepository.saveCanvas(stateGroup, canvas.widgets, grid)
        }
        with(repositories.thorSettingsRepository) {
            setLidWakeGuardEnabled(snapshot.lidWakeGuardEnabled)
            setHallSensorCalibration(snapshot.hallSensorCalibration)
            setAutoFpsEnabled(snapshot.autoFpsEnabled)
            setAutoFpsTriggerPackages(snapshot.autoFpsTriggerPackages)
            setTaskKillerEnabled(snapshot.taskKillerEnabled)
            setTaskKillerExcludedPackages(snapshot.taskKillerExcludedPackages)
            setTaskKillerTarget(snapshot.taskKillerTarget)
            setVolumeSyncEnabled(snapshot.volumeSyncEnabled)
            setVolumeSyncMode(snapshot.volumeSyncMode)
        }
        // Re-applies each saved override rather than clearing every existing one first -
        // GameMatchOverrideRepository has no bulk-clear operation, and a stray override
        // this snapshot doesn't mention is harmless leftover corrective data, unlike a
        // stale toggle/path the settings above would silently keep if not overwritten.
        snapshot.gameMatchOverrides.forEach { repositories.gameMatchOverrideRepository.setOverride(it) }
        // Same re-apply-without-clearing reasoning as gameMatchOverrides above.
        with(repositories.gameLaunchAppRepository) {
            snapshot.gameLaunchSystemDefaults.forEach { (systemShortName, packageName) ->
                setSystemDefault(systemShortName, packageName)
            }
            snapshot.gameLaunchOverrides.forEach {
                setGameOverride(it.systemShortName, it.relativeRomPath, it.packageName)
            }
            setLaunchDisplayTarget(snapshot.gameLaunchDisplayTarget)
            setCloseAppOnGameEnd(snapshot.closeAppOnGameEndEnabled)
            setEnabled(snapshot.gameLaunchEnabled)
        }
        with(repositories.gameGuideSettingsRepository) {
            setManualFallbackOnNoGuideEnabled(snapshot.manualFallbackOnNoGuideEnabled)
            setDisplayPreferences(snapshot.guideDisplayPreferences)
        }
    }

    private suspend fun applyOnboardingSettings(snapshot: AppConfigBackup) {
        with(repositories.onboardingRepository) {
            snapshot.logFolderPath?.let { saveLogFolderPath(it) }
            snapshot.mediaFolderPath?.let { saveMediaFolderPath(it) }
            if (snapshot.customSystemImagesFolderPath != null) {
                saveCustomSystemImagesFolderPath(snapshot.customSystemImagesFolderPath)
            } else {
                clearCustomSystemImagesFolderPath()
            }
            if (snapshot.customLogosFolderPath != null) {
                saveCustomLogosFolderPath(snapshot.customLogosFolderPath)
            } else {
                clearCustomLogosFolderPath()
            }
            if (snapshot.customMusicFolderPath != null) {
                saveCustomMusicFolderPath(snapshot.customMusicFolderPath)
            } else {
                clearCustomMusicFolderPath()
            }
            setThemePreference(snapshot.themePreference)
            setGamePlayingBehavior(snapshot.gamePlayingBehavior)
            setGamePlayingDimPercent(snapshot.gamePlayingDimPercent)
            setScreensaverBehavior(snapshot.screensaverBehavior)
            setScreensaverDimPercent(snapshot.screensaverDimPercent)
            setOverlayOpacityPercent(snapshot.overlayOpacityPercent)
            setFabAssignments(snapshot.fabAssignments)
            setMusicEnabled(snapshot.musicEnabled)
            setMusicPlayWhileBrowsingSystems(snapshot.musicPlayWhileBrowsingSystems)
            setMusicPlayWhileBrowsingGames(snapshot.musicPlayWhileBrowsingGames)
            setMusicPlayDuringScreensaver(snapshot.musicPlayDuringScreensaver)
            setMusicDuckingMode(snapshot.musicDuckingMode)
            setCloseCompanionOnQuitEnabled(snapshot.closeCompanionOnQuitEnabled)
            setLaunchEsdeOnStartEnabled(snapshot.launchEsdeOnStartEnabled)
            setDebugLoggingEnabled(snapshot.debugLoggingEnabled)
            setUpdateAchievementsOnScreensaverEnabled(snapshot.updateAchievementsOnScreensaverEnabled)
            setUpdateGameGuidesOnScreensaverEnabled(snapshot.updateGameGuidesOnScreensaverEnabled)
        }
    }
}
