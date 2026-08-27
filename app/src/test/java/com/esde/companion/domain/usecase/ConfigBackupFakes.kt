package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AppFolder
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.GameLaunchDisplayTarget
import com.esde.companion.domain.model.GameLaunchOverride
import com.esde.companion.domain.model.GameMatchOverride
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.HallSensorCalibration
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.SavedWidgetCanvas
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.model.VolumeSyncMode
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import com.esde.companion.domain.repository.AppFolderRepository
import com.esde.companion.domain.repository.DockSettingsRepository
import com.esde.companion.domain.repository.GameLaunchAppRepository
import com.esde.companion.domain.repository.GameMatchOverrideRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.repository.ThorSettingsRepository
import com.esde.companion.domain.repository.WidgetLayoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory fakes for the settings repositories [ExportConfigBackupUseCase]/
 * [RestoreConfigBackupUseCase] read and write, shared by both use cases' tests plus the
 * export-then-restore round trip. Every field is independently settable/observable via a
 * backing [MutableStateFlow], same shape as [SetFabAssignmentUseCaseTest]'s
 * `FakeOnboardingRepository` generalized to every field this backup feature touches.
 */
@Suppress("TooManyFunctions", "LongParameterList", "EmptyFunctionBlock")
internal class FakeOnboardingRepository(
    logFolderPath: String? = "/storage/emulated/0/ES-DE",
    mediaFolderPath: String? = "/storage/emulated/0/ES-DE/downloaded_media",
    customSystemImagesFolderPath: String? = null,
    customLogosFolderPath: String? = null,
    customMusicFolderPath: String? = null,
    themePreference: ThemePreference = ThemePreference.Auto,
    gamePlayingBehavior: ScreenBehavior = ScreenBehavior.Nothing,
    gamePlayingDimPercent: Int = 50,
    screensaverBehavior: ScreenBehavior = ScreenBehavior.Nothing,
    screensaverDimPercent: Int = 50,
    overlayOpacityPercent: Int = 80,
    fabAssignments: FabAssignments = FabAssignments.Default,
    musicEnabled: Boolean = true,
    musicPlayWhileBrowsingSystems: Boolean = true,
    musicPlayWhileBrowsingGames: Boolean = true,
    musicPlayDuringScreensaver: Boolean = false,
    musicDuckingMode: MusicDuckingMode = MusicDuckingMode.LowerVolume,
    closeCompanionOnQuitEnabled: Boolean = false,
    launchEsdeOnStartEnabled: Boolean = false,
    debugLoggingEnabled: Boolean = false,
    updateAchievementsOnScreensaverEnabled: Boolean = true,
) : OnboardingRepository {
    private val logFolderPathFlow = MutableStateFlow(logFolderPath)
    private val mediaFolderPathFlow = MutableStateFlow(mediaFolderPath)
    private val customSystemImagesFolderPathFlow = MutableStateFlow(customSystemImagesFolderPath)
    private val customLogosFolderPathFlow = MutableStateFlow(customLogosFolderPath)
    private val customMusicFolderPathFlow = MutableStateFlow(customMusicFolderPath)
    private val themePreferenceFlow = MutableStateFlow(themePreference)
    private val gamePlayingBehaviorFlow = MutableStateFlow(gamePlayingBehavior)
    private val gamePlayingDimPercentFlow = MutableStateFlow(gamePlayingDimPercent)
    private val screensaverBehaviorFlow = MutableStateFlow(screensaverBehavior)
    private val screensaverDimPercentFlow = MutableStateFlow(screensaverDimPercent)
    private val overlayOpacityPercentFlow = MutableStateFlow(overlayOpacityPercent)
    private val fabAssignmentsFlow = MutableStateFlow(fabAssignments)
    private val musicEnabledFlow = MutableStateFlow(musicEnabled)
    private val musicPlayWhileBrowsingSystemsFlow = MutableStateFlow(musicPlayWhileBrowsingSystems)
    private val musicPlayWhileBrowsingGamesFlow = MutableStateFlow(musicPlayWhileBrowsingGames)
    private val musicPlayDuringScreensaverFlow = MutableStateFlow(musicPlayDuringScreensaver)
    private val musicDuckingModeFlow = MutableStateFlow(musicDuckingMode)
    private val closeCompanionOnQuitEnabledFlow = MutableStateFlow(closeCompanionOnQuitEnabled)
    private val launchEsdeOnStartEnabledFlow = MutableStateFlow(launchEsdeOnStartEnabled)
    private val debugLoggingEnabledFlow = MutableStateFlow(debugLoggingEnabled)
    private val updateAchievementsOnScreensaverEnabledFlow = MutableStateFlow(updateAchievementsOnScreensaverEnabled)

    override fun defaultLogFolderPath() = "/storage/emulated/0/ES-DE"

    override fun defaultMediaFolderPath() = "/storage/emulated/0/ES-DE/downloaded_media"

    override suspend fun validateLogFolder(path: String) = LogFolderValidation.FolderFound(true)

    override suspend fun validateMediaFolder(path: String) = MediaFolderValidation.FolderFound

    override suspend fun saveLogFolderPath(path: String) {
        logFolderPathFlow.value = path
    }

    override suspend fun saveMediaFolderPath(path: String) {
        mediaFolderPathFlow.value = path
    }

    override fun observeLogFolderPath(): Flow<String?> = logFolderPathFlow

    override fun observeMediaFolderPath(): Flow<String?> = mediaFolderPathFlow

    override suspend fun saveCustomSystemImagesFolderPath(path: String) {
        customSystemImagesFolderPathFlow.value = path
    }

    override fun observeCustomSystemImagesFolderPath(): Flow<String?> = customSystemImagesFolderPathFlow

    override suspend fun clearCustomSystemImagesFolderPath() {
        customSystemImagesFolderPathFlow.value = null
    }

    override suspend fun saveCustomLogosFolderPath(path: String) {
        customLogosFolderPathFlow.value = path
    }

    override fun observeCustomLogosFolderPath(): Flow<String?> = customLogosFolderPathFlow

    override suspend fun clearCustomLogosFolderPath() {
        customLogosFolderPathFlow.value = null
    }

    override suspend fun markOnboardingComplete() {}

    override fun observeOnboardingComplete(): Flow<Boolean> = MutableStateFlow(true)

    override suspend fun setThemePreference(preference: ThemePreference) {
        themePreferenceFlow.value = preference
    }

    override fun observeThemePreference(): Flow<ThemePreference> = themePreferenceFlow

    override suspend fun setGamePlayingBehavior(behavior: ScreenBehavior) {
        gamePlayingBehaviorFlow.value = behavior
    }

    override fun observeGamePlayingBehavior(): Flow<ScreenBehavior> = gamePlayingBehaviorFlow

    override suspend fun setGamePlayingDimPercent(percent: Int) {
        gamePlayingDimPercentFlow.value = percent
    }

    override fun observeGamePlayingDimPercent(): Flow<Int> = gamePlayingDimPercentFlow

    override suspend fun setScreensaverBehavior(behavior: ScreenBehavior) {
        screensaverBehaviorFlow.value = behavior
    }

    override fun observeScreensaverBehavior(): Flow<ScreenBehavior> = screensaverBehaviorFlow

    override suspend fun setScreensaverDimPercent(percent: Int) {
        screensaverDimPercentFlow.value = percent
    }

    override fun observeScreensaverDimPercent(): Flow<Int> = screensaverDimPercentFlow

    override suspend fun setMusicEnabled(enabled: Boolean) {
        musicEnabledFlow.value = enabled
    }

    override fun observeMusicEnabled(): Flow<Boolean> = musicEnabledFlow

    override suspend fun setMusicPlayWhileBrowsingSystems(enabled: Boolean) {
        musicPlayWhileBrowsingSystemsFlow.value = enabled
    }

    override fun observeMusicPlayWhileBrowsingSystems(): Flow<Boolean> = musicPlayWhileBrowsingSystemsFlow

    override suspend fun setMusicPlayWhileBrowsingGames(enabled: Boolean) {
        musicPlayWhileBrowsingGamesFlow.value = enabled
    }

    override fun observeMusicPlayWhileBrowsingGames(): Flow<Boolean> = musicPlayWhileBrowsingGamesFlow

    override suspend fun setMusicPlayDuringScreensaver(enabled: Boolean) {
        musicPlayDuringScreensaverFlow.value = enabled
    }

    override fun observeMusicPlayDuringScreensaver(): Flow<Boolean> = musicPlayDuringScreensaverFlow

    override suspend fun setMusicDuckingMode(mode: MusicDuckingMode) {
        musicDuckingModeFlow.value = mode
    }

    override fun observeMusicDuckingMode(): Flow<MusicDuckingMode> = musicDuckingModeFlow

    override suspend fun setOverlayOpacityPercent(percent: Int) {
        overlayOpacityPercentFlow.value = percent
    }

    override fun observeOverlayOpacityPercent(): Flow<Int> = overlayOpacityPercentFlow

    override suspend fun saveCustomMusicFolderPath(path: String) {
        customMusicFolderPathFlow.value = path
    }

    override fun observeCustomMusicFolderPath(): Flow<String?> = customMusicFolderPathFlow

    override suspend fun clearCustomMusicFolderPath() {
        customMusicFolderPathFlow.value = null
    }

    override suspend fun setCloseCompanionOnQuitEnabled(enabled: Boolean) {
        closeCompanionOnQuitEnabledFlow.value = enabled
    }

    override fun observeCloseCompanionOnQuitEnabled(): Flow<Boolean> = closeCompanionOnQuitEnabledFlow

    override suspend fun setFabAssignments(assignments: FabAssignments) {
        fabAssignmentsFlow.value = assignments
    }

    override fun observeFabAssignments(): Flow<FabAssignments> = fabAssignmentsFlow

    override suspend fun setLaunchEsdeOnStartEnabled(enabled: Boolean) {
        launchEsdeOnStartEnabledFlow.value = enabled
    }

    override fun observeLaunchEsdeOnStartEnabled(): Flow<Boolean> = launchEsdeOnStartEnabledFlow

    override suspend fun setDebugLoggingEnabled(enabled: Boolean) {
        debugLoggingEnabledFlow.value = enabled
    }

    override fun observeDebugLoggingEnabled(): Flow<Boolean> = debugLoggingEnabledFlow

    override suspend fun setUpdateAchievementsOnScreensaverEnabled(enabled: Boolean) {
        updateAchievementsOnScreensaverEnabledFlow.value = enabled
    }

    override fun observeUpdateAchievementsOnScreensaverEnabled(): Flow<Boolean> {
        return updateAchievementsOnScreensaverEnabledFlow
    }

    // Deliberately not part of this fake's constructor params, same as the RetroAchievements
    // credentials repository - see AppContainer's Backup & Restore wiring comment for why this
    // preference is excluded from Export/RestoreConfigBackupUseCase.
    override suspend fun setPlaytimeStatsHardcoreModeEnabled(enabled: Boolean) {}

    override fun observePlaytimeStatsHardcoreModeEnabled(): Flow<Boolean> = MutableStateFlow(false)

    private val bluetoothPermissionRequestedFlow = MutableStateFlow(false)

    override suspend fun setBluetoothPermissionRequested(requested: Boolean) {
        bluetoothPermissionRequestedFlow.value = requested
    }

    override fun observeBluetoothPermissionRequested(): Flow<Boolean> = bluetoothPermissionRequestedFlow
}

internal class FakeAppDrawerSettingsRepository(
    hiddenApps: Set<String> = emptySet(),
    gridColumns: Int = 5,
    otherScreenLaunchApps: Set<String> = emptySet(),
    sortFoldersOnTop: Boolean = true,
    showSearchBar: Boolean = true,
) : AppDrawerSettingsRepository {
    private val hiddenAppsFlow = MutableStateFlow(hiddenApps)
    private val gridColumnsFlow = MutableStateFlow(gridColumns)
    private val otherScreenLaunchAppsFlow = MutableStateFlow(otherScreenLaunchApps)
    private val sortFoldersOnTopFlow = MutableStateFlow(sortFoldersOnTop)
    private val showSearchBarFlow = MutableStateFlow(showSearchBar)

    override suspend fun setHiddenApps(packageNames: Set<String>) {
        hiddenAppsFlow.value = packageNames
    }

    override fun observeHiddenApps(): Flow<Set<String>> = hiddenAppsFlow

    override suspend fun setGridColumns(columns: Int) {
        gridColumnsFlow.value = columns
    }

    override fun observeGridColumns(): Flow<Int> = gridColumnsFlow

    override suspend fun setOtherScreenLaunchApps(packageNames: Set<String>) {
        otherScreenLaunchAppsFlow.value = packageNames
    }

    override fun observeOtherScreenLaunchApps(): Flow<Set<String>> = otherScreenLaunchAppsFlow

    override suspend fun setSortFoldersOnTop(sortOnTop: Boolean) {
        sortFoldersOnTopFlow.value = sortOnTop
    }

    override fun observeSortFoldersOnTop(): Flow<Boolean> = sortFoldersOnTopFlow

    override suspend fun setShowSearchBar(show: Boolean) {
        showSearchBarFlow.value = show
    }

    override fun observeShowSearchBar(): Flow<Boolean> = showSearchBarFlow
}

internal class FakeAppFolderRepository(
    folders: List<AppFolder> = emptyList(),
) : AppFolderRepository {
    private val foldersFlow = MutableStateFlow(folders)

    override suspend fun setFolders(folders: List<AppFolder>) {
        foldersFlow.value = folders
    }

    override fun observeFolders(): Flow<List<AppFolder>> = foldersFlow
}

internal class FakeDockSettingsRepository(
    dockEnabled: Boolean = false,
    dockMaxApps: Int = 5,
    dockSize: DockSize = DockSize.Medium,
    dockApps: List<String> = emptyList(),
) : DockSettingsRepository {
    private val dockEnabledFlow = MutableStateFlow(dockEnabled)
    private val dockMaxAppsFlow = MutableStateFlow(dockMaxApps)
    private val dockSizeFlow = MutableStateFlow(dockSize)
    private val dockAppsFlow = MutableStateFlow(dockApps)

    override suspend fun setDockEnabled(enabled: Boolean) {
        dockEnabledFlow.value = enabled
    }

    override fun observeDockEnabled(): Flow<Boolean> = dockEnabledFlow

    override suspend fun setDockMaxApps(maxApps: Int) {
        dockMaxAppsFlow.value = maxApps
    }

    override fun observeDockMaxApps(): Flow<Int> = dockMaxAppsFlow

    override suspend fun setDockSize(size: DockSize) {
        dockSizeFlow.value = size
    }

    override fun observeDockSize(): Flow<DockSize> = dockSizeFlow

    override suspend fun setDockApps(packageNames: List<String>) {
        dockAppsFlow.value = packageNames
    }

    override fun observeDockApps(): Flow<List<String>> = dockAppsFlow
}

@Suppress("LongParameterList")
internal class FakeThorSettingsRepository(
    lidWakeGuardEnabled: Boolean = false,
    hallSensorCalibration: HallSensorCalibration = HallSensorCalibration.Uncalibrated,
    autoFpsEnabled: Boolean = false,
    autoFpsTriggerPackages: Set<String> = emptySet(),
    taskKillerEnabled: Boolean = false,
    taskKillerExcludedPackages: Set<String> = emptySet(),
    volumeSyncEnabled: Boolean = false,
    volumeSyncMode: VolumeSyncMode = VolumeSyncMode.Linked,
) : ThorSettingsRepository {
    private val lidWakeGuardEnabledFlow = MutableStateFlow(lidWakeGuardEnabled)
    private val hallSensorCalibrationFlow = MutableStateFlow(hallSensorCalibration)
    private val autoFpsEnabledFlow = MutableStateFlow(autoFpsEnabled)
    private val autoFpsTriggerPackagesFlow = MutableStateFlow(autoFpsTriggerPackages)
    private val taskKillerEnabledFlow = MutableStateFlow(taskKillerEnabled)
    private val taskKillerExcludedPackagesFlow = MutableStateFlow(taskKillerExcludedPackages)
    private val volumeSyncEnabledFlow = MutableStateFlow(volumeSyncEnabled)
    private val volumeSyncModeFlow = MutableStateFlow(volumeSyncMode)

    override suspend fun setLidWakeGuardEnabled(enabled: Boolean) {
        lidWakeGuardEnabledFlow.value = enabled
    }

    override fun observeLidWakeGuardEnabled(): Flow<Boolean> = lidWakeGuardEnabledFlow

    override suspend fun setHallSensorCalibration(calibration: HallSensorCalibration) {
        hallSensorCalibrationFlow.value = calibration
    }

    override fun observeHallSensorCalibration(): Flow<HallSensorCalibration> = hallSensorCalibrationFlow

    override suspend fun setAutoFpsEnabled(enabled: Boolean) {
        autoFpsEnabledFlow.value = enabled
    }

    override fun observeAutoFpsEnabled(): Flow<Boolean> = autoFpsEnabledFlow

    override suspend fun setAutoFpsTriggerPackages(packages: Set<String>) {
        autoFpsTriggerPackagesFlow.value = packages
    }

    override fun observeAutoFpsTriggerPackages(): Flow<Set<String>> = autoFpsTriggerPackagesFlow

    override suspend fun setTaskKillerEnabled(enabled: Boolean) {
        taskKillerEnabledFlow.value = enabled
    }

    override fun observeTaskKillerEnabled(): Flow<Boolean> = taskKillerEnabledFlow

    override suspend fun setTaskKillerExcludedPackages(packages: Set<String>) {
        taskKillerExcludedPackagesFlow.value = packages
    }

    override fun observeTaskKillerExcludedPackages(): Flow<Set<String>> = taskKillerExcludedPackagesFlow

    override suspend fun setVolumeSyncEnabled(enabled: Boolean) {
        volumeSyncEnabledFlow.value = enabled
    }

    override fun observeVolumeSyncEnabled(): Flow<Boolean> = volumeSyncEnabledFlow

    override suspend fun setVolumeSyncMode(mode: VolumeSyncMode) {
        volumeSyncModeFlow.value = mode
    }

    override fun observeVolumeSyncMode(): Flow<VolumeSyncMode> = volumeSyncModeFlow
}

internal class FakeWidgetLayoutRepository(
    initial: Map<StateGroup, SavedWidgetCanvas> = emptyMap(),
) : WidgetLayoutRepository {
    private val canvases =
        StateGroup.entries.associateWith { stateGroup ->
            MutableStateFlow(initial[stateGroup] ?: SavedWidgetCanvas(grid = null, widgets = emptyList()))
        }

    override fun observeCanvas(stateGroup: StateGroup): Flow<SavedWidgetCanvas> = canvases.getValue(stateGroup)

    override suspend fun saveCanvas(
        stateGroup: StateGroup,
        widgets: List<PlacedWidget>,
        grid: GridDimensions,
    ) {
        canvases.getValue(stateGroup).value = SavedWidgetCanvas(grid = grid, widgets = widgets)
    }
}

internal class FakeGameMatchOverrideRepository(
    initial: List<GameMatchOverride> = emptyList(),
) : GameMatchOverrideRepository {
    private val overridesFlow = MutableStateFlow(initial)

    override suspend fun setOverride(override: GameMatchOverride) {
        overridesFlow.value = overridesFlow.value.filterNot { it.matchesReference(override) } + override
    }

    override suspend fun clearOverride(gameReference: GameReference) {
        overridesFlow.value = overridesFlow.value.filterNot { it.matchesReference(gameReference) }
    }

    override suspend fun getOverride(gameReference: GameReference): GameMatchOverride? =
        overridesFlow.value.firstOrNull { it.matchesReference(gameReference) }

    override fun observeAllOverrides(): Flow<List<GameMatchOverride>> = overridesFlow

    private fun GameMatchOverride.matchesReference(other: GameMatchOverride) =
        systemShortName == other.systemShortName && romPath == other.romPath

    private fun GameMatchOverride.matchesReference(reference: GameReference) =
        systemShortName == reference.systemShortName && romPath == reference.romPath
}

internal class FakeGameLaunchAppRepository(
    initialSystemDefaults: Map<String, String> = emptyMap(),
    initialGameOverrides: List<GameLaunchOverride> = emptyList(),
    initialLaunchDisplayTarget: GameLaunchDisplayTarget = GameLaunchDisplayTarget.ThisScreen,
) : GameLaunchAppRepository {
    private val systemDefaultsFlow = MutableStateFlow(initialSystemDefaults)
    private val gameOverridesFlow = MutableStateFlow(initialGameOverrides)
    private val launchDisplayTargetFlow = MutableStateFlow(initialLaunchDisplayTarget)

    override fun observeSystemDefaults(): Flow<Map<String, String>> = systemDefaultsFlow

    override suspend fun setSystemDefault(
        systemShortName: String,
        packageName: String?,
    ) {
        systemDefaultsFlow.value =
            if (packageName == null) {
                systemDefaultsFlow.value - systemShortName
            } else {
                systemDefaultsFlow.value + (systemShortName to packageName)
            }
    }

    override fun observeGameOverrides(): Flow<List<GameLaunchOverride>> = gameOverridesFlow

    override suspend fun setGameOverride(
        systemShortName: String,
        relativeRomPath: String,
        packageName: String?,
    ) {
        val newOverride = GameLaunchOverride(systemShortName, relativeRomPath, packageName)
        gameOverridesFlow.value =
            gameOverridesFlow.value.filterNot { it.matches(systemShortName, relativeRomPath) } + newOverride
    }

    override suspend fun clearGameOverride(
        systemShortName: String,
        relativeRomPath: String,
    ) {
        gameOverridesFlow.value = gameOverridesFlow.value.filterNot { it.matches(systemShortName, relativeRomPath) }
    }

    override fun observeLaunchDisplayTarget(): Flow<GameLaunchDisplayTarget> = launchDisplayTargetFlow

    override suspend fun setLaunchDisplayTarget(target: GameLaunchDisplayTarget) {
        launchDisplayTargetFlow.value = target
    }

    private fun GameLaunchOverride.matches(
        systemShortName: String,
        relativeRomPath: String,
    ) = this.systemShortName == systemShortName && this.relativeRomPath == relativeRomPath
}
