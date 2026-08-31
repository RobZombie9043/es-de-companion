package com.esde.companion

import android.content.Context
import android.os.Environment
import com.esde.companion.data.activity.ProcessActivityVisibilityRepository
import com.esde.companion.data.apps.CompanionDisplayHolder
import com.esde.companion.data.apps.PackageManagerAppsRepository
import com.esde.companion.data.backup.JsonConfigBackupRepository
import com.esde.companion.data.context.FileLastKnownContextRepository
import com.esde.companion.data.debug.DebugFileLogger
import com.esde.companion.data.gameguides.FileGameGuideLibraryRepository
import com.esde.companion.data.gameguides.FileGameGuideSettingsRepository
import com.esde.companion.data.gamelist.DataStoreGameLaunchAppRepository
import com.esde.companion.data.gamelist.FileGameDescriptionRepository
import com.esde.companion.data.gamelist.FileGameRatingRepository
import com.esde.companion.data.gamelist.FileGameRomHashRepository
import com.esde.companion.data.gamelist.FileGamelistLibraryRepository
import com.esde.companion.data.gamelist.GameLaunchOverrideCoordinator
import com.esde.companion.data.gamelist.GameLaunchOverrideSettings
import com.esde.companion.data.gamelist.GamelistFileReader
import com.esde.companion.data.gamelist.LoggingGameDescriptionRepository
import com.esde.companion.data.gamelist.LoggingGameRatingRepository
import com.esde.companion.data.gamelist.LoggingGameRomHashRepository
import com.esde.companion.data.log.EsdeLogFileRepository
import com.esde.companion.data.log.ReactiveEsdeLogRepository
import com.esde.companion.data.log.SharedEsdeLogRepository
import com.esde.companion.data.media.AssetBundledSystemLogoRepository
import com.esde.companion.data.media.LoggingGameMediaRepository
import com.esde.companion.data.media.ReactiveCustomSystemImageRepository
import com.esde.companion.data.media.ReactiveCustomSystemLogoRepository
import com.esde.companion.data.media.ReactiveGameMediaRepository
import com.esde.companion.data.media.ReactiveSystemMediaRepository
import com.esde.companion.data.music.ExoMusicPlayerController
import com.esde.companion.data.music.ReactiveMusicLibraryRepository
import com.esde.companion.data.retroachievements.AchievementCommentsCache
import com.esde.companion.data.retroachievements.AchievementSummaryCache
import com.esde.companion.data.retroachievements.DataStoreGameListCacheStore
import com.esde.companion.data.retroachievements.DataStoreGameMatchOverrideRepository
import com.esde.companion.data.retroachievements.DataStoreUserProgressCacheStore
import com.esde.companion.data.retroachievements.EncryptedRetroAchievementsCredentialsRepository
import com.esde.companion.data.retroachievements.GameLeaderboardsCache
import com.esde.companion.data.retroachievements.GameListCache
import com.esde.companion.data.retroachievements.LeaderboardEntriesCache
import com.esde.companion.data.retroachievements.RetroAchievementsCaches
import com.esde.companion.data.retroachievements.RetroAchievementsRepositoryImpl
import com.esde.companion.data.retroachievements.RetroClientRetroAchievementsApi
import com.esde.companion.data.retroachievements.UserProgressCache
import com.esde.companion.data.settings.FileAppDrawerSettingsRepository
import com.esde.companion.data.settings.FileAppFolderRepository
import com.esde.companion.data.settings.FileDockSettingsRepository
import com.esde.companion.data.settings.FileEsdeInstallationRepository
import com.esde.companion.data.settings.FileOnboardingRepository
import com.esde.companion.data.settings.FileWidgetLayoutRepository
import com.esde.companion.data.storage.SelfHealConfig
import com.esde.companion.data.storage.StorageMountEvents
import com.esde.companion.data.systems.LoggingSystemPathRepository
import com.esde.companion.data.systems.ReactiveSystemPathRepository
import com.esde.companion.data.systemstatus.AndroidSystemStatusRepository
import com.esde.companion.data.systemstatus.BluetoothPermissionRecheckSignal
import com.esde.companion.data.systemstatus.SharedSystemStatusRepository
import com.esde.companion.data.thor.AutoFpsCoordinator
import com.esde.companion.data.thor.FileThorSettingsRepository
import com.esde.companion.data.thor.LidWakeGuardCoordinator
import com.esde.companion.data.thor.TaskKillerCoordinator
import com.esde.companion.data.thor.VolumeSyncCoordinator
import com.esde.companion.data.thor.VolumeSyncShell
import com.esde.companion.data.update.FileUpdateStateRepository
import com.esde.companion.data.update.GitHubUpdateRepository
import com.esde.companion.data.video.ProcessVideoPlaybackStateRepository
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.currentGameReference
import com.esde.companion.domain.music.MusicPlaybackCoordinator
import com.esde.companion.domain.repository.ActivityVisibilityRepository
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import com.esde.companion.domain.repository.AppFolderRepository
import com.esde.companion.domain.repository.BackupRepositories
import com.esde.companion.domain.repository.BundledSystemLogoRepository
import com.esde.companion.domain.repository.ConfigBackupRepository
import com.esde.companion.domain.repository.CustomSystemImageRepository
import com.esde.companion.domain.repository.CustomSystemLogoRepository
import com.esde.companion.domain.repository.DockSettingsRepository
import com.esde.companion.domain.repository.EsdeInstallationRepository
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.repository.GameDescriptionRepository
import com.esde.companion.domain.repository.GameGuideLibraryRepository
import com.esde.companion.domain.repository.GameGuideSettingsRepository
import com.esde.companion.domain.repository.GameLaunchAppRepository
import com.esde.companion.domain.repository.GameMatchOverrideRepository
import com.esde.companion.domain.repository.GameMediaRepository
import com.esde.companion.domain.repository.GameRatingRepository
import com.esde.companion.domain.repository.GameRomHashRepository
import com.esde.companion.domain.repository.GamelistLibraryRepository
import com.esde.companion.domain.repository.InstalledAppsRepository
import com.esde.companion.domain.repository.LastKnownContextRepository
import com.esde.companion.domain.repository.MusicLibraryRepository
import com.esde.companion.domain.repository.MusicPlayerController
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.repository.RetroAchievementsCredentialsRepository
import com.esde.companion.domain.repository.RetroAchievementsRepository
import com.esde.companion.domain.repository.SystemMediaRepository
import com.esde.companion.domain.repository.SystemPathRepository
import com.esde.companion.domain.repository.SystemStatusRepository
import com.esde.companion.domain.repository.ThorSettingsRepository
import com.esde.companion.domain.repository.UpdateRepository
import com.esde.companion.domain.repository.UpdateStateRepository
import com.esde.companion.domain.repository.VideoPlaybackStateRepository
import com.esde.companion.domain.repository.WidgetLayoutRepository
import com.esde.companion.domain.state.AppStateReducer
import com.esde.companion.domain.usecase.CheckForUpdateUseCase
import com.esde.companion.domain.usecase.ClearGameLaunchOverrideUseCase
import com.esde.companion.domain.usecase.ClearRetroAchievementsCredentialsUseCase
import com.esde.companion.domain.usecase.CompleteOnboardingUseCase
import com.esde.companion.domain.usecase.DeleteAllGameGuidesUseCase
import com.esde.companion.domain.usecase.DeleteGameGuideUseCase
import com.esde.companion.domain.usecase.DeleteLegacyScriptFilesUseCase
import com.esde.companion.domain.usecase.DownloadApkUseCase
import com.esde.companion.domain.usecase.ExportConfigBackupUseCase
import com.esde.companion.domain.usecase.FetchReleaseNotesForVersionUseCase
import com.esde.companion.domain.usecase.FindLegacyScriptFilesUseCase
import com.esde.companion.domain.usecase.GetAchievementCommentsUseCase
import com.esde.companion.domain.usecase.GetGameAchievementSummaryUseCase
import com.esde.companion.domain.usecase.GetGameHashSupportUseCase
import com.esde.companion.domain.usecase.GetGameLeaderboardsUseCase
import com.esde.companion.domain.usecase.GetLeaderboardEntriesUseCase
import com.esde.companion.domain.usecase.GetRetroAchievementsSystemGamesUseCase
import com.esde.companion.domain.usecase.GetUserGameProgressUseCase
import com.esde.companion.domain.usecase.ImportGameGuideUseCase
import com.esde.companion.domain.usecase.ListGamelistGamesUseCase
import com.esde.companion.domain.usecase.ListGamelistSystemsUseCase
import com.esde.companion.domain.usecase.LoadGameGuideBinaryPathUseCase
import com.esde.companion.domain.usecase.LoadGameGuideContentUseCase
import com.esde.companion.domain.usecase.ObserveAllGameGuidesUseCase
import com.esde.companion.domain.usecase.ObserveAppFoldersUseCase
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
import com.esde.companion.domain.usecase.ObserveAutoFpsEnabledUseCase
import com.esde.companion.domain.usecase.ObserveAutoFpsTriggerPackagesUseCase
import com.esde.companion.domain.usecase.ObserveBluetoothPermissionRequestedUseCase
import com.esde.companion.domain.usecase.ObserveCloseAppOnGameEndUseCase
import com.esde.companion.domain.usecase.ObserveCloseCompanionOnQuitEnabledUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveDebugLoggingEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockMaxAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockSizeUseCase
import com.esde.companion.domain.usecase.ObserveEsdeEventScriptSettingsUseCase
import com.esde.companion.domain.usecase.ObserveEsdeLogActivityUseCase
import com.esde.companion.domain.usecase.ObserveEsdeQuitEventUseCase
import com.esde.companion.domain.usecase.ObserveFabAssignmentsUseCase
import com.esde.companion.domain.usecase.ObserveGameGuideDisplayPreferencesUseCase
import com.esde.companion.domain.usecase.ObserveGameGuideReadingProgressUseCase
import com.esde.companion.domain.usecase.ObserveGameGuidesUseCase
import com.esde.companion.domain.usecase.ObserveGameLaunchDisplayTargetUseCase
import com.esde.companion.domain.usecase.ObserveGameLaunchEnabledUseCase
import com.esde.companion.domain.usecase.ObserveGameLaunchOverridesUseCase
import com.esde.companion.domain.usecase.ObserveGameLaunchSystemDefaultsUseCase
import com.esde.companion.domain.usecase.ObserveGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveGamePlayingDimPercentUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveHallSensorCalibrationUseCase
import com.esde.companion.domain.usecase.ObserveHiddenAppsUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveLastGameReferenceUseCase
import com.esde.companion.domain.usecase.ObserveLastSeenChangelogVersionCodeUseCase
import com.esde.companion.domain.usecase.ObserveLastSystemShortNameUseCase
import com.esde.companion.domain.usecase.ObserveLaunchEsdeOnStartEnabledUseCase
import com.esde.companion.domain.usecase.ObserveLidWakeGuardEnabledUseCase
import com.esde.companion.domain.usecase.ObserveManualFallbackOnNoGuideEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.ObserveMusicEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.ObserveOnboardingCompleteUseCase
import com.esde.companion.domain.usecase.ObserveOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayOpacityUseCase
import com.esde.companion.domain.usecase.ObservePlaytimeStatsHardcoreModeEnabledUseCase
import com.esde.companion.domain.usecase.ObserveRetroAchievementsCredentialsUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverDimPercentUseCase
import com.esde.companion.domain.usecase.ObserveShowSearchBarUseCase
import com.esde.companion.domain.usecase.ObserveSortFoldersOnTopUseCase
import com.esde.companion.domain.usecase.ObserveSystemStatusUseCase
import com.esde.companion.domain.usecase.ObserveTaskKillerEnabledUseCase
import com.esde.companion.domain.usecase.ObserveTaskKillerExcludedPackagesUseCase
import com.esde.companion.domain.usecase.ObserveTaskKillerTargetUseCase
import com.esde.companion.domain.usecase.ObserveThemePreferenceUseCase
import com.esde.companion.domain.usecase.ObserveUpdateAchievementsOnScreensaverEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVolumeSyncEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVolumeSyncModeUseCase
import com.esde.companion.domain.usecase.ObserveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.ReadEsdeEventScriptSettingsUseCase
import com.esde.companion.domain.usecase.ReadEsdeMediaDirectoryUseCase
import com.esde.companion.domain.usecase.ResolveBundledSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemImageUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveGameDescriptionUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import com.esde.companion.domain.usecase.ResolveGameRatingUseCase
import com.esde.companion.domain.usecase.ResolveRandomSystemMediaUseCase
import com.esde.companion.domain.usecase.ResolveRetroAchievementsGameUseCase
import com.esde.companion.domain.usecase.RestoreConfigBackupUseCase
import com.esde.companion.domain.usecase.SaveGameGuideUseCase
import com.esde.companion.domain.usecase.SaveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.SearchRetroAchievementsGamesUseCase
import com.esde.companion.domain.usecase.SetAppFoldersUseCase
import com.esde.companion.domain.usecase.SetAutoFpsEnabledUseCase
import com.esde.companion.domain.usecase.SetAutoFpsTriggerPackagesUseCase
import com.esde.companion.domain.usecase.SetBluetoothPermissionRequestedUseCase
import com.esde.companion.domain.usecase.SetCloseAppOnGameEndUseCase
import com.esde.companion.domain.usecase.SetCloseCompanionOnQuitEnabledUseCase
import com.esde.companion.domain.usecase.SetDebugLoggingEnabledUseCase
import com.esde.companion.domain.usecase.SetDockAppsUseCase
import com.esde.companion.domain.usecase.SetDockEnabledUseCase
import com.esde.companion.domain.usecase.SetDockMaxAppsUseCase
import com.esde.companion.domain.usecase.SetDockSizeUseCase
import com.esde.companion.domain.usecase.SetFabAssignmentUseCase
import com.esde.companion.domain.usecase.SetGameGuideDisplayPreferencesUseCase
import com.esde.companion.domain.usecase.SetGameGuideReadingProgressUseCase
import com.esde.companion.domain.usecase.SetGameLaunchDisplayTargetUseCase
import com.esde.companion.domain.usecase.SetGameLaunchEnabledUseCase
import com.esde.companion.domain.usecase.SetGameLaunchOverrideUseCase
import com.esde.companion.domain.usecase.SetGameLaunchSystemDefaultUseCase
import com.esde.companion.domain.usecase.SetGameMatchOverrideUseCase
import com.esde.companion.domain.usecase.SetGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.SetGamePlayingDimPercentUseCase
import com.esde.companion.domain.usecase.SetGridColumnsUseCase
import com.esde.companion.domain.usecase.SetHallSensorCalibrationUseCase
import com.esde.companion.domain.usecase.SetHiddenAppsUseCase
import com.esde.companion.domain.usecase.SetLastGameReferenceUseCase
import com.esde.companion.domain.usecase.SetLastSeenChangelogVersionCodeUseCase
import com.esde.companion.domain.usecase.SetLastSystemShortNameUseCase
import com.esde.companion.domain.usecase.SetLaunchEsdeOnStartEnabledUseCase
import com.esde.companion.domain.usecase.SetLidWakeGuardEnabledUseCase
import com.esde.companion.domain.usecase.SetManualFallbackOnNoGuideEnabledUseCase
import com.esde.companion.domain.usecase.SetMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.SetMusicEnabledUseCase
import com.esde.companion.domain.usecase.SetMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.SetOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.SetOverlayOpacityUseCase
import com.esde.companion.domain.usecase.SetPlaytimeStatsHardcoreModeEnabledUseCase
import com.esde.companion.domain.usecase.SetScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.SetScreensaverDimPercentUseCase
import com.esde.companion.domain.usecase.SetShowSearchBarUseCase
import com.esde.companion.domain.usecase.SetSortFoldersOnTopUseCase
import com.esde.companion.domain.usecase.SetTaskKillerEnabledUseCase
import com.esde.companion.domain.usecase.SetTaskKillerExcludedPackagesUseCase
import com.esde.companion.domain.usecase.SetTaskKillerTargetUseCase
import com.esde.companion.domain.usecase.SetThemePreferenceUseCase
import com.esde.companion.domain.usecase.SetUpdateAchievementsOnScreensaverEnabledUseCase
import com.esde.companion.domain.usecase.SetVolumeSyncEnabledUseCase
import com.esde.companion.domain.usecase.SetVolumeSyncModeUseCase
import com.esde.companion.domain.usecase.ValidateEsdeLogFolderUseCase
import com.esde.companion.domain.usecase.ValidateEsdeMediaFolderUseCase
import com.esde.companion.domain.usecase.ValidateRetroAchievementsCredentialsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock

/**
 * Minimal hand-rolled composition root. At this project's current scale (single module,
 * one developer) this is a deliberate, simpler alternative to a DI framework - see
 * CLAUDE.md. If/when the dependency graph grows enough to justify it, this can be
 * replaced with Hilt without changing anything below the ViewModel layer, since
 * everything here is already expressed as interfaces / constructor dependencies.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val onboardingRepository: OnboardingRepository = FileOnboardingRepository(appContext)

    // Fed by StorageMountReceiver (manifest-registered - see AndroidManifest.xml) so the
    // log/settings directory watchers below can rearm promptly once removable storage (e.g.
    // an SD card holding the ES-DE folder) mounts after being unavailable at app startup, or
    // tear down cleanly on removal. Public: the receiver reaches this via
    // (application as CompanionApplication).appContainer.storageMountEvents.
    val storageMountEvents = StorageMountEvents()

    // Fed by SystemStatusFabContent's ON_RESUME check - there's no OS broadcast for "a
    // runtime permission changed while the app was backgrounded", so this lets
    // systemStatusRepository below re-evaluate its Bluetooth half after the user
    // grants/revokes BLUETOOTH_CONNECT via system Settings. Same shape as storageMountEvents.
    val bluetoothPermissionRecheckSignal = BluetoothPermissionRecheckSignal()

    private val androidSystemStatusRepository =
        AndroidSystemStatusRepository(
            context = appContext,
            bluetoothPermissionRecheck = bluetoothPermissionRecheckSignal.events,
        )

    // Shared (not rebuilt per subscriber) since both top FAB corners could reference this at
    // once (e.g. ClockAndSystemStatus in one, SystemStatus in the other) - same reasoning as
    // logRepository below.
    private val systemStatusRepository: SystemStatusRepository =
        SharedSystemStatusRepository(inner = androidSystemStatusRepository, scope = applicationScope)

    val esdeInstallationRepository: EsdeInstallationRepository =
        FileEsdeInstallationRepository(storageEvents = storageMountEvents.events)

    val activityVisibilityRepository: ActivityVisibilityRepository = ProcessActivityVisibilityRepository()

    // Populated by MainActivity's own (visual) Context - see CompanionDisplayHolder's kdoc for
    // why GameLaunchOverrideCoordinator can't safely derive this itself.
    val companionDisplayHolder = CompanionDisplayHolder()

    val videoPlaybackStateRepository: VideoPlaybackStateRepository = ProcessVideoPlaybackStateRepository()

    // Opt-in debug log (state transitions, media resolution outcomes) to help diagnose
    // reported issues - see Settings > Other Settings. Lives in a top-level
    // "ES-DE Companion" folder parallel to the user's "ES-DE" folder, not nested inside
    // it and not tied to wherever the user configured the ES-DE root to be.
    private val debugFileLogger =
        DebugFileLogger(
            logFile = File(Environment.getExternalStorageDirectory(), "ES-DE Companion/logs/esde_companion_log.txt"),
            onboardingRepository = onboardingRepository,
            applicationScope = applicationScope,
            clock = Clock.systemDefaultZone(),
        )

    // Video widget playback events (WidgetVideoContent) are reported from a Compose/
    // ExoPlayer callback, not through a use case - exposed as plain function references
    // (rather than the DebugFileLogger type itself) so MainActivity, which already wires
    // other domain-typed callbacks like videoPlaybackStateRepository::setIsPlaying down
    // into the widget canvas, can do the same here without reaching into the data layer.
    val logVideoPlaybackStarted: (String) -> Unit = { path -> debugFileLogger.logPlaybackStarted("Video", path) }
    val logVideoPlaybackError: (String, String) -> Unit =
        { path, message -> debugFileLogger.logPlaybackError("Video", path, message) }

    // Named separately (rather than inlined into logRepository below) purely to keep that
    // property's own nesting shallow - see CLAUDE.md's ktlint/detekt indentation gotcha.
    private val logRepositorySelfHeal =
        SelfHealConfig(
            storageEvents = storageMountEvents.events,
            onFallbackPollCaughtUpdate = {
                debugFileLogger.logInfo("Poll", "fallback poll (not FileObserver) picked up an es_log.txt update")
            },
            onStorageMountEvent = {
                debugFileLogger.logInfo("Storage", "mount/unmount broadcast received, rearming directory watcher")
            },
        )

    private val esdeLogFileRepositoryFactory: (String) -> EsdeLogRepository = { folderPath ->
        EsdeLogFileRepository(
            logFilePath = "$folderPath/logs/es_log.txt",
            selfHeal = logRepositorySelfHeal,
        )
    }

    private val reactiveEsdeLogRepository: EsdeLogRepository =
        ReactiveEsdeLogRepository(
            logFolderPath = onboardingRepository.observeLogFolderPath(),
            repositoryFactory = esdeLogFileRepositoryFactory,
        )

    private val logRepository: EsdeLogRepository =
        SharedEsdeLogRepository(
            inner = reactiveEsdeLogRepository,
            scope = applicationScope,
        )

    // custom_systems/ lives alongside logs/ under the ES-DE root, so this reacts to the
    // log folder path, not the media folder path - see ReactiveSystemPathRepository.
    // Last-resort tier of media resolution (see GameMediaPathResolver's kdoc), shared
    // by gameMediaRepository below.
    private val systemPathRepository: SystemPathRepository =
        LoggingSystemPathRepository(
            inner = ReactiveSystemPathRepository(esdeRootPath = onboardingRepository.observeLogFolderPath()),
            debugFileLogger = debugFileLogger,
        )

    // Same reactive-to-Settings pattern as logRepository, for the media folder.
    private val reactiveGameMediaRepository =
        ReactiveGameMediaRepository(
            mediaFolderPath = onboardingRepository.observeMediaFolderPath(),
            systemPathRepository = systemPathRepository,
        )

    private val gameMediaRepository: GameMediaRepository =
        LoggingGameMediaRepository(
            inner = reactiveGameMediaRepository,
            debugFileLogger = debugFileLogger,
        )

    // gamelists/ lives alongside logs/ under the ES-DE root, so this reacts to the log
    // folder path, not the media folder path - see GamelistFileReader. Shared by the
    // description, ROM-hash, and rating repositories below so there's one cached copy of
    // each gamelist.xml's text, not one per consumer.
    private val gamelistFileReader = GamelistFileReader(esdeRootPath = onboardingRepository.observeLogFolderPath())

    private val gameDescriptionRepository: GameDescriptionRepository =
        LoggingGameDescriptionRepository(
            inner = FileGameDescriptionRepository(gamelistFileReader),
            debugFileLogger = debugFileLogger,
        )

    // Ready ahead of ES-DE's own upcoming ROM-hash support - see CLAUDE.md's
    // RetroAchievements section and GameListParser's ROM_HASH_TAG TODO.
    private val gameRomHashRepository: GameRomHashRepository =
        LoggingGameRomHashRepository(
            inner = FileGameRomHashRepository(gamelistFileReader),
            debugFileLogger = debugFileLogger,
        )

    private val gameRatingRepository: GameRatingRepository =
        LoggingGameRatingRepository(
            inner = FileGameRatingRepository(gamelistFileReader),
            debugFileLogger = debugFileLogger,
        )

    // Whole-library enumeration for Game Launch Override's browse UI - see
    // GamelistLibraryRepository's kdoc for why this only supports the standard gamelist
    // location, unlike the single-game lookups above.
    private val gamelistLibraryRepository: GamelistLibraryRepository = FileGamelistLibraryRepository(gamelistFileReader)
    val listGamelistSystemsUseCase = ListGamelistSystemsUseCase(gamelistLibraryRepository)
    val listGamelistGamesUseCase = ListGamelistGamesUseCase(gamelistLibraryRepository)

    private val gameLaunchAppRepository: GameLaunchAppRepository = DataStoreGameLaunchAppRepository(appContext)
    val observeGameLaunchSystemDefaultsUseCase = ObserveGameLaunchSystemDefaultsUseCase(gameLaunchAppRepository)
    val setGameLaunchSystemDefaultUseCase = SetGameLaunchSystemDefaultUseCase(gameLaunchAppRepository)
    val observeGameLaunchOverridesUseCase = ObserveGameLaunchOverridesUseCase(gameLaunchAppRepository)
    val setGameLaunchOverrideUseCase = SetGameLaunchOverrideUseCase(gameLaunchAppRepository)
    val clearGameLaunchOverrideUseCase = ClearGameLaunchOverrideUseCase(gameLaunchAppRepository)
    val observeGameLaunchDisplayTargetUseCase = ObserveGameLaunchDisplayTargetUseCase(gameLaunchAppRepository)
    val setGameLaunchDisplayTargetUseCase = SetGameLaunchDisplayTargetUseCase(gameLaunchAppRepository)
    val observeCloseAppOnGameEndUseCase = ObserveCloseAppOnGameEndUseCase(gameLaunchAppRepository)
    val setCloseAppOnGameEndUseCase = SetCloseAppOnGameEndUseCase(gameLaunchAppRepository)
    val observeGameLaunchEnabledUseCase = ObserveGameLaunchEnabledUseCase(gameLaunchAppRepository)
    val setGameLaunchEnabledUseCase = SetGameLaunchEnabledUseCase(gameLaunchAppRepository)

    private val systemMediaRepository: SystemMediaRepository =
        ReactiveSystemMediaRepository(mediaFolderPath = onboardingRepository.observeMediaFolderPath())

    private val customSystemImageRepository: CustomSystemImageRepository =
        ReactiveCustomSystemImageRepository(folderPath = onboardingRepository.observeCustomSystemImagesFolderPath())

    private val customSystemLogoRepository: CustomSystemLogoRepository =
        ReactiveCustomSystemLogoRepository(folderPath = onboardingRepository.observeCustomLogosFolderPath())

    private val bundledSystemLogoRepository: BundledSystemLogoRepository = AssetBundledSystemLogoRepository(appContext)

    private val musicLibraryRepository: MusicLibraryRepository =
        ReactiveMusicLibraryRepository(musicFolderPath = onboardingRepository.observeCustomMusicFolderPath())

    private val musicPlayerController: MusicPlayerController = ExoMusicPlayerController(appContext)

    val resolveCustomSystemImageUseCase = ResolveCustomSystemImageUseCase(customSystemImageRepository)
    val resolveCustomSystemLogoUseCase = ResolveCustomSystemLogoUseCase(customSystemLogoRepository)
    val resolveBundledSystemLogoUseCase = ResolveBundledSystemLogoUseCase(bundledSystemLogoRepository)

    private val installedAppsRepository: InstalledAppsRepository = PackageManagerAppsRepository(appContext)
    val observeInstalledAppsUseCase = ObserveInstalledAppsUseCase(installedAppsRepository)

    // App Drawer display/visibility settings (hidden apps, opacity, grid columns) - a
    // distinct concern from onboarding, kept in its own repository/DataStore. See
    // AppDrawerSettingsRepository.
    private val appDrawerSettingsRepository: AppDrawerSettingsRepository = FileAppDrawerSettingsRepository(appContext)

    private val widgetLayoutRepository: WidgetLayoutRepository = FileWidgetLayoutRepository(appContext)

    val observeWidgetCanvasUseCase = ObserveWidgetCanvasUseCase(widgetLayoutRepository)
    val saveWidgetCanvasUseCase = SaveWidgetCanvasUseCase(widgetLayoutRepository)

    // Tracks the most recently browsed system/game, independent of live AppState - used
    // by the widget edit-mode preview so it has something real to show even when Idle or
    // editing a canvas that isn't the one currently live. Persisted (own DataStore file),
    // not session-only, so it's already populated on the very next app launch too. See
    // LastKnownContextRepository's kdoc.
    private val lastKnownContextRepository: LastKnownContextRepository =
        FileLastKnownContextRepository(appContext)

    val observeLastSystemShortNameUseCase = ObserveLastSystemShortNameUseCase(lastKnownContextRepository)
    val setLastSystemShortNameUseCase = SetLastSystemShortNameUseCase(lastKnownContextRepository)
    val observeLastGameReferenceUseCase = ObserveLastGameReferenceUseCase(lastKnownContextRepository)
    val setLastGameReferenceUseCase = SetLastGameReferenceUseCase(lastKnownContextRepository)

    val observeHiddenAppsUseCase = ObserveHiddenAppsUseCase(appDrawerSettingsRepository)
    val setHiddenAppsUseCase = SetHiddenAppsUseCase(appDrawerSettingsRepository)

    val observeOtherScreenLaunchAppsUseCase = ObserveOtherScreenLaunchAppsUseCase(appDrawerSettingsRepository)
    val setOtherScreenLaunchAppsUseCase = SetOtherScreenLaunchAppsUseCase(appDrawerSettingsRepository)

    val observeGridColumnsUseCase = ObserveGridColumnsUseCase(appDrawerSettingsRepository)
    val setGridColumnsUseCase = SetGridColumnsUseCase(appDrawerSettingsRepository)

    val observeSortFoldersOnTopUseCase = ObserveSortFoldersOnTopUseCase(appDrawerSettingsRepository)
    val setSortFoldersOnTopUseCase = SetSortFoldersOnTopUseCase(appDrawerSettingsRepository)

    val observeShowSearchBarUseCase = ObserveShowSearchBarUseCase(appDrawerSettingsRepository)
    val setShowSearchBarUseCase = SetShowSearchBarUseCase(appDrawerSettingsRepository)

    // App Drawer folders - structured data, so its own repository/DataStore rather than
    // folded into appDrawerSettingsRepository above. See AppFolderRepository.
    private val appFolderRepository: AppFolderRepository = FileAppFolderRepository(appContext)
    val observeAppFoldersUseCase = ObserveAppFoldersUseCase(appFolderRepository)
    val setAppFoldersUseCase = SetAppFoldersUseCase(appFolderRepository)

    // App Dock settings (enabled, max apps, size, opacity, pinned apps) - its own
    // repository/DataStore, though it reuses appDrawerSettingsRepository's
    // other-screen-launch preference above so launch location is shared with the
    // Drawer rather than tracked twice. See DockSettingsRepository.
    private val dockSettingsRepository: DockSettingsRepository = FileDockSettingsRepository(appContext)

    val observeDockEnabledUseCase = ObserveDockEnabledUseCase(dockSettingsRepository)
    val setDockEnabledUseCase = SetDockEnabledUseCase(dockSettingsRepository)
    val observeDockMaxAppsUseCase = ObserveDockMaxAppsUseCase(dockSettingsRepository)
    val setDockMaxAppsUseCase = SetDockMaxAppsUseCase(dockSettingsRepository)
    val observeDockSizeUseCase = ObserveDockSizeUseCase(dockSettingsRepository)
    val setDockSizeUseCase = SetDockSizeUseCase(dockSettingsRepository)
    val observeDockAppsUseCase = ObserveDockAppsUseCase(dockSettingsRepository)
    val setDockAppsUseCase = SetDockAppsUseCase(dockSettingsRepository)

    // Not a secret - plain DataStore, included in Backup & Restore below (unlike
    // RetroAchievements' credentials repository, further down this file).
    private val gameMatchOverrideRepository: GameMatchOverrideRepository =
        DataStoreGameMatchOverrideRepository(appContext)

    // Thor Settings (Ayn Thor-only: Lid Wake Guard + Auto FPS Mode) - its own repository/
    // DataStore, not onboardingRepository's, since these settings are meaningless on any
    // non-Thor device. Declared ahead of Backup & Restore below, which needs it at
    // construction time. See CLAUDE.md.
    private val thorSettingsRepository: ThorSettingsRepository = FileThorSettingsRepository(appContext)

    // Whether this firmware exposes the vendor Settings.System key Volume Sync writes to -
    // effectively fixed for the process lifetime, same reasoning as RefreshRateController's own
    // canWrite() check, so this is a plain one-time snapshot rather than a Flow. Needs
    // appContext (ContentResolver access), unlike the other Thor capability checks, so it's
    // computed here rather than directly in SettingsViewModel's init.
    val volumeSyncSecondarySettingPresent = VolumeSyncShell.hasSecondarySetting(appContext)

    val observeLidWakeGuardEnabledUseCase = ObserveLidWakeGuardEnabledUseCase(thorSettingsRepository)
    val setLidWakeGuardEnabledUseCase = SetLidWakeGuardEnabledUseCase(thorSettingsRepository)
    val observeHallSensorCalibrationUseCase = ObserveHallSensorCalibrationUseCase(thorSettingsRepository)
    val setHallSensorCalibrationUseCase = SetHallSensorCalibrationUseCase(thorSettingsRepository)
    val observeAutoFpsEnabledUseCase = ObserveAutoFpsEnabledUseCase(thorSettingsRepository)
    val setAutoFpsEnabledUseCase = SetAutoFpsEnabledUseCase(thorSettingsRepository)
    val observeAutoFpsTriggerPackagesUseCase = ObserveAutoFpsTriggerPackagesUseCase(thorSettingsRepository)
    val setAutoFpsTriggerPackagesUseCase = SetAutoFpsTriggerPackagesUseCase(thorSettingsRepository)
    val observeTaskKillerEnabledUseCase = ObserveTaskKillerEnabledUseCase(thorSettingsRepository)
    val setTaskKillerEnabledUseCase = SetTaskKillerEnabledUseCase(thorSettingsRepository)
    val observeTaskKillerExcludedPackagesUseCase = ObserveTaskKillerExcludedPackagesUseCase(thorSettingsRepository)
    val setTaskKillerExcludedPackagesUseCase = SetTaskKillerExcludedPackagesUseCase(thorSettingsRepository)
    val observeTaskKillerTargetUseCase = ObserveTaskKillerTargetUseCase(thorSettingsRepository)
    val setTaskKillerTargetUseCase = SetTaskKillerTargetUseCase(thorSettingsRepository)
    val observeVolumeSyncEnabledUseCase = ObserveVolumeSyncEnabledUseCase(thorSettingsRepository)
    val setVolumeSyncEnabledUseCase = SetVolumeSyncEnabledUseCase(thorSettingsRepository)
    val observeVolumeSyncModeUseCase = ObserveVolumeSyncModeUseCase(thorSettingsRepository)
    val setVolumeSyncModeUseCase = SetVolumeSyncModeUseCase(thorSettingsRepository)

    // Settings > Setup > Backup & Restore. Reaches directly into the settings repositories
    // above (rather than through their narrower per-field use cases) since export/restore
    // needs every field at once - see ExportConfigBackupUseCase/RestoreConfigBackupUseCase.
    // Bundled into BackupRepositories (see its own kdoc) to keep both use cases' constructors
    // within this project's LongParameterList limit.
    private val configBackupRepository: ConfigBackupRepository = JsonConfigBackupRepository()
    private val backupRepositories =
        BackupRepositories(
            onboardingRepository = onboardingRepository,
            appDrawerSettingsRepository = appDrawerSettingsRepository,
            appFolderRepository = appFolderRepository,
            dockSettingsRepository = dockSettingsRepository,
            widgetLayoutRepository = widgetLayoutRepository,
            thorSettingsRepository = thorSettingsRepository,
            gameMatchOverrideRepository = gameMatchOverrideRepository,
            gameLaunchAppRepository = gameLaunchAppRepository,
        )
    val exportConfigBackupUseCase = ExportConfigBackupUseCase(backupRepositories, configBackupRepository)
    val restoreConfigBackupUseCase = RestoreConfigBackupUseCase(backupRepositories, configBackupRepository)

    val observeAppStateUseCase =
        ObserveAppStateUseCase(
            logRepository = logRepository,
            scope = applicationScope,
            reducer = { state, event ->
                val newState = AppStateReducer.reduce(state, event)
                debugFileLogger.logStateTransition(event, newState)
                newState
            },
        )
    val observeConnectionStateUseCase = ObserveConnectionStateUseCase(logRepository, observeAppStateUseCase)
    val observeEsdeLogActivityUseCase = ObserveEsdeLogActivityUseCase(logRepository)
    val observeEsdeQuitEventUseCase = ObserveEsdeQuitEventUseCase(logRepository)
    val resolveGameMediaUseCase = ResolveGameMediaUseCase(gameMediaRepository)
    val resolveGameDescriptionUseCase = ResolveGameDescriptionUseCase(gameDescriptionRepository)
    val resolveGameRatingUseCase = ResolveGameRatingUseCase(gameRatingRepository)
    val resolveRandomSystemMediaUseCase = ResolveRandomSystemMediaUseCase(systemMediaRepository)

    val validateEsdeLogFolderUseCase = ValidateEsdeLogFolderUseCase(onboardingRepository)
    val validateEsdeMediaFolderUseCase = ValidateEsdeMediaFolderUseCase(onboardingRepository)
    val completeOnboardingUseCase = CompleteOnboardingUseCase(onboardingRepository)
    val observeOnboardingCompleteUseCase = ObserveOnboardingCompleteUseCase(onboardingRepository)
    val readEsdeMediaDirectoryUseCase = ReadEsdeMediaDirectoryUseCase(esdeInstallationRepository)
    val readEsdeEventScriptSettingsUseCase = ReadEsdeEventScriptSettingsUseCase(esdeInstallationRepository)
    val observeEsdeEventScriptSettingsUseCase = ObserveEsdeEventScriptSettingsUseCase(esdeInstallationRepository)
    val findLegacyScriptFilesUseCase = FindLegacyScriptFilesUseCase(esdeInstallationRepository)
    val deleteLegacyScriptFilesUseCase = DeleteLegacyScriptFilesUseCase(esdeInstallationRepository)

    val observeGamePlayingBehaviorUseCase = ObserveGamePlayingBehaviorUseCase(onboardingRepository)
    val setGamePlayingBehaviorUseCase = SetGamePlayingBehaviorUseCase(onboardingRepository)
    val observeGamePlayingDimPercentUseCase = ObserveGamePlayingDimPercentUseCase(onboardingRepository)
    val setGamePlayingDimPercentUseCase = SetGamePlayingDimPercentUseCase(onboardingRepository)
    val observeScreensaverBehaviorUseCase = ObserveScreensaverBehaviorUseCase(onboardingRepository)
    val setScreensaverBehaviorUseCase = SetScreensaverBehaviorUseCase(onboardingRepository)
    val observeScreensaverDimPercentUseCase = ObserveScreensaverDimPercentUseCase(onboardingRepository)
    val setScreensaverDimPercentUseCase = SetScreensaverDimPercentUseCase(onboardingRepository)
    val observeThemePreferenceUseCase = ObserveThemePreferenceUseCase(onboardingRepository)
    val setThemePreferenceUseCase = SetThemePreferenceUseCase(onboardingRepository)

    val observeMusicEnabledUseCase = ObserveMusicEnabledUseCase(onboardingRepository)
    val setMusicEnabledUseCase = SetMusicEnabledUseCase(onboardingRepository)
    val observeMusicPlayWhileBrowsingSystemsUseCase = ObserveMusicPlayWhileBrowsingSystemsUseCase(onboardingRepository)
    val setMusicPlayWhileBrowsingSystemsUseCase = SetMusicPlayWhileBrowsingSystemsUseCase(onboardingRepository)
    val observeMusicPlayWhileBrowsingGamesUseCase = ObserveMusicPlayWhileBrowsingGamesUseCase(onboardingRepository)
    val setMusicPlayWhileBrowsingGamesUseCase = SetMusicPlayWhileBrowsingGamesUseCase(onboardingRepository)
    val observeMusicPlayDuringScreensaverUseCase = ObserveMusicPlayDuringScreensaverUseCase(onboardingRepository)
    val setMusicPlayDuringScreensaverUseCase = SetMusicPlayDuringScreensaverUseCase(onboardingRepository)
    val observeMusicDuckingModeUseCase = ObserveMusicDuckingModeUseCase(onboardingRepository)
    val setMusicDuckingModeUseCase = SetMusicDuckingModeUseCase(onboardingRepository)

    // Master opacity for every translucent overlay surface (App Drawer, App Dock, music
    // controls panel, and the Settings/music-FAB/Edit-Widgets corner buttons) - one shared
    // setting rather than a separate one per surface. See OnboardingRepository's kdoc.
    val observeOverlayOpacityUseCase = ObserveOverlayOpacityUseCase(onboardingRepository)
    val setOverlayOpacityUseCase = SetOverlayOpacityUseCase(onboardingRepository)

    val observeCloseCompanionOnQuitEnabledUseCase = ObserveCloseCompanionOnQuitEnabledUseCase(onboardingRepository)
    val setCloseCompanionOnQuitEnabledUseCase = SetCloseCompanionOnQuitEnabledUseCase(onboardingRepository)
    val observeFabAssignmentsUseCase = ObserveFabAssignmentsUseCase(onboardingRepository)
    val setFabAssignmentUseCase = SetFabAssignmentUseCase(onboardingRepository)
    val observeSystemStatusUseCase = ObserveSystemStatusUseCase(systemStatusRepository)
    val observeBluetoothPermissionRequestedUseCase = ObserveBluetoothPermissionRequestedUseCase(onboardingRepository)
    val setBluetoothPermissionRequestedUseCase = SetBluetoothPermissionRequestedUseCase(onboardingRepository)
    val observeLaunchEsdeOnStartEnabledUseCase = ObserveLaunchEsdeOnStartEnabledUseCase(onboardingRepository)
    val setLaunchEsdeOnStartEnabledUseCase = SetLaunchEsdeOnStartEnabledUseCase(onboardingRepository)
    val observeDebugLoggingEnabledUseCase = ObserveDebugLoggingEnabledUseCase(onboardingRepository)
    val setDebugLoggingEnabledUseCase = SetDebugLoggingEnabledUseCase(onboardingRepository)

    // Settings > RetroAchievements: whether the achievement pages follow ES-DE's screensaver
    // to whatever game it's showing, or freeze on the pre-screensaver game/system instead.
    val observeUpdateAchievementsOnScreensaverEnabledUseCase =
        ObserveUpdateAchievementsOnScreensaverEnabledUseCase(onboardingRepository)
    val setUpdateAchievementsOnScreensaverEnabledUseCase =
        SetUpdateAchievementsOnScreensaverEnabledUseCase(onboardingRepository)

    // The achievement screens' Playtime Stats line's Casual/Hardcore toggle - global, not
    // per-game, see OnboardingRepository.observePlaytimeStatsHardcoreModeEnabled's kdoc.
    val observePlaytimeStatsHardcoreModeEnabledUseCase =
        ObservePlaytimeStatsHardcoreModeEnabledUseCase(onboardingRepository)
    val setPlaytimeStatsHardcoreModeEnabledUseCase =
        SetPlaytimeStatsHardcoreModeEnabledUseCase(onboardingRepository)

    // RetroAchievements sign-in (Settings > RetroAchievements). Credentials get their own
    // EncryptedSharedPreferences-backed repository rather than DataStore - see CLAUDE.md and
    // EncryptedRetroAchievementsCredentialsRepository's kdoc - and are deliberately never
    // passed into exportConfigBackupUseCase/restoreConfigBackupUseCase above, a structural
    // guarantee against a plaintext credential leak (the same mechanism lastKnownContextRepository
    // relies on). The game list is cached (disk + memory, 24h TTL) since api-kotlin flags
    // GetGameList as rate-limited/response-heavy - see GameListCache's kdoc. The signed-in
    // user's cross-console completion progress (system browser's Progress sort/filter) is a
    // separate cache with its own 1h TTL and its own DataStore file - see UserProgressCache's
    // and DataStoreUserProgressCacheStore's kdocs for why it can't share GameListCache's TTL
    // or file. A third cache, AchievementSummaryCache, covers the per-game achievement summary
    // (the achievement screen's data) - in-memory only, keyed by (username, gameId), 15-minute
    // TTL, with a manual forceRefresh bypass wired to that screen's kebab "Refresh" entry - see
    // its kdoc for why it isn't disk-backed like the other two. A fourth, AchievementCommentsCache,
    // covers per-achievement wall comments (fetched on-demand when a row is expanded) - in-memory
    // only, keyed by achievementId alone (public data, not per-user), 30-minute TTL. A fifth,
    // GameLeaderboardsCache, covers the per-game leaderboard list (the same screen's Leaderboards
    // tab) - in-memory only, keyed by (username, gameId) same as AchievementSummaryCache since
    // each row's myEntry is per-user, 15-minute TTL, refreshed alongside the achievement summary
    // by the same kebab "Refresh" entry. A sixth, LeaderboardEntriesCache, covers one leaderboard's
    // entries (fetched on-demand when a row is expanded) - in-memory only, keyed by leaderboardId
    // alone (public data), 15-minute TTL. None of the six caches are covered by Backup & Restore,
    // same as the credentials.
    private val retroAchievementsCredentialsRepository: RetroAchievementsCredentialsRepository =
        EncryptedRetroAchievementsCredentialsRepository(appContext)
    private val gameListCache = GameListCache(store = DataStoreGameListCacheStore(appContext))
    private val userProgressCache = UserProgressCache(store = DataStoreUserProgressCacheStore(appContext))
    private val achievementSummaryCache = AchievementSummaryCache()
    private val achievementCommentsCache = AchievementCommentsCache()
    private val gameLeaderboardsCache = GameLeaderboardsCache()
    private val leaderboardEntriesCache = LeaderboardEntriesCache()
    private val retroAchievementsCaches =
        RetroAchievementsCaches(
            gameList = gameListCache,
            userProgress = userProgressCache,
            achievementSummary = achievementSummaryCache,
            achievementComments = achievementCommentsCache,
            gameLeaderboards = gameLeaderboardsCache,
            leaderboardEntries = leaderboardEntriesCache,
        )
    private val retroAchievementsRepository: RetroAchievementsRepository =
        RetroAchievementsRepositoryImpl(
            credentialsRepository = retroAchievementsCredentialsRepository,
            caches = retroAchievementsCaches,
            apiFactory = { credentials -> RetroClientRetroAchievementsApi(credentials) },
        )

    val observeRetroAchievementsCredentialsUseCase =
        ObserveRetroAchievementsCredentialsUseCase(retroAchievementsCredentialsRepository)
    val clearRetroAchievementsCredentialsUseCase =
        ClearRetroAchievementsCredentialsUseCase(retroAchievementsCredentialsRepository)
    val validateRetroAchievementsCredentialsUseCase =
        ValidateRetroAchievementsCredentialsUseCase(retroAchievementsRepository, retroAchievementsCredentialsRepository)
    val resolveRetroAchievementsGameUseCase =
        ResolveRetroAchievementsGameUseCase(
            gameMatchOverrideRepository = gameMatchOverrideRepository,
            retroAchievementsRepository = retroAchievementsRepository,
            gameRomHashRepository = gameRomHashRepository,
            onResolved = { message -> debugFileLogger.logInfo("Cheevo", message) },
        )
    val getGameAchievementSummaryUseCase = GetGameAchievementSummaryUseCase(retroAchievementsRepository)
    val getGameHashSupportUseCase = GetGameHashSupportUseCase(gameRomHashRepository, retroAchievementsRepository)
    val setGameMatchOverrideUseCase = SetGameMatchOverrideUseCase(gameMatchOverrideRepository)
    val searchRetroAchievementsGamesUseCase = SearchRetroAchievementsGamesUseCase(retroAchievementsRepository)
    val getRetroAchievementsSystemGamesUseCase = GetRetroAchievementsSystemGamesUseCase(retroAchievementsRepository)
    val getUserGameProgressUseCase = GetUserGameProgressUseCase(retroAchievementsRepository)
    val getAchievementCommentsUseCase = GetAchievementCommentsUseCase(retroAchievementsRepository)
    val getGameLeaderboardsUseCase = GetGameLeaderboardsUseCase(retroAchievementsRepository)
    val getLeaderboardEntriesUseCase = GetLeaderboardEntriesUseCase(retroAchievementsRepository)

    // Game Guides FAB (debug-only for now - see GameGuidesFeatureFlag's kdoc). Downloaded
    // guide content/metadata is app-private storage (filesDir + its own DataStore file),
    // deliberately excluded from Backup & Restore - same reasoning as the RetroAchievements
    // caches above: it's fetched/regenerable content, not user configuration, and could be
    // large - so these two repositories are never added to BackupRepositories.
    private val gameGuideLibraryRepository: GameGuideLibraryRepository = FileGameGuideLibraryRepository(appContext)
    private val gameGuideSettingsRepository: GameGuideSettingsRepository = FileGameGuideSettingsRepository(appContext)
    val observeGameGuidesUseCase = ObserveGameGuidesUseCase(gameGuideLibraryRepository)
    val observeAllGameGuidesUseCase = ObserveAllGameGuidesUseCase(gameGuideLibraryRepository)
    val saveGameGuideUseCase = SaveGameGuideUseCase(gameGuideLibraryRepository)
    val importGameGuideUseCase = ImportGameGuideUseCase(gameGuideLibraryRepository)
    val loadGameGuideContentUseCase = LoadGameGuideContentUseCase(gameGuideLibraryRepository)
    val loadGameGuideBinaryPathUseCase = LoadGameGuideBinaryPathUseCase(gameGuideLibraryRepository)
    val deleteGameGuideUseCase = DeleteGameGuideUseCase(gameGuideLibraryRepository)
    val deleteAllGameGuidesUseCase = DeleteAllGameGuidesUseCase(gameGuideLibraryRepository)
    val observeGameGuideDisplayPreferencesUseCase =
        ObserveGameGuideDisplayPreferencesUseCase(gameGuideSettingsRepository)
    val setGameGuideDisplayPreferencesUseCase = SetGameGuideDisplayPreferencesUseCase(gameGuideSettingsRepository)
    val observeGameGuideReadingProgressUseCase = ObserveGameGuideReadingProgressUseCase(gameGuideSettingsRepository)
    val setGameGuideReadingProgressUseCase = SetGameGuideReadingProgressUseCase(gameGuideSettingsRepository)
    val observeManualFallbackOnNoGuideEnabledUseCase =
        ObserveManualFallbackOnNoGuideEnabledUseCase(gameGuideSettingsRepository)
    val setManualFallbackOnNoGuideEnabledUseCase =
        SetManualFallbackOnNoGuideEnabledUseCase(gameGuideSettingsRepository)

    // Constructed unconditionally (cheap - same as the self-healing directory watchers above),
    // but each coordinator's *internal* listeners/receivers stay gated behind its own
    // DataStore-persisted enabled flag - see LidWakeGuardCoordinator/AutoFpsCoordinator's kdoc
    // for why that's the deliberate divergence from Asgard's always-on install.
    private val lidWakeGuardCoordinator =
        LidWakeGuardCoordinator(
            context = appContext,
            observeLidWakeGuardEnabled = observeLidWakeGuardEnabledUseCase,
            setLidWakeGuardEnabled = setLidWakeGuardEnabledUseCase,
            observeHallSensorCalibration = observeHallSensorCalibrationUseCase,
            debugFileLogger = debugFileLogger,
        )

    private val autoFpsCoordinator =
        AutoFpsCoordinator(
            observeAutoFpsEnabled = observeAutoFpsEnabledUseCase,
            setAutoFpsEnabled = setAutoFpsEnabledUseCase,
            observeAutoFpsTriggerPackages = observeAutoFpsTriggerPackagesUseCase,
            debugFileLogger = debugFileLogger,
        )

    private val taskKillerCoordinator =
        TaskKillerCoordinator(
            observeTaskKillerEnabled = observeTaskKillerEnabledUseCase,
            setTaskKillerEnabled = setTaskKillerEnabledUseCase,
            observeTaskKillerExcludedPackages = observeTaskKillerExcludedPackagesUseCase,
            observeTaskKillerTarget = observeTaskKillerTargetUseCase,
            companionDisplayHolder = companionDisplayHolder,
            debugFileLogger = debugFileLogger,
        )

    private val volumeSyncCoordinator =
        VolumeSyncCoordinator(
            observeVolumeSyncEnabled = observeVolumeSyncEnabledUseCase,
            setVolumeSyncEnabled = setVolumeSyncEnabledUseCase,
            observeVolumeSyncMode = observeVolumeSyncModeUseCase,
        )

    private val gameLaunchOverrideSettings =
        GameLaunchOverrideSettings(
            observeSystemDefaults = observeGameLaunchSystemDefaultsUseCase,
            observeOverrides = observeGameLaunchOverridesUseCase,
            observeDisplayTarget = observeGameLaunchDisplayTargetUseCase,
            observeCloseAppOnGameEnd = observeCloseAppOnGameEndUseCase,
            observeEnabled = observeGameLaunchEnabledUseCase,
        )

    private val gameLaunchOverrideCoordinator =
        GameLaunchOverrideCoordinator(
            observeAppState = observeAppStateUseCase,
            settings = gameLaunchOverrideSettings,
            companionDisplayHolder = companionDisplayHolder,
            debugFileLogger = debugFileLogger,
        )

    // Update checker (GitHub Releases). The one sanctioned network use case in this app -
    // see CLAUDE.md's "What NOT to Do" entry on network layers.
    private val updateRepository: UpdateRepository = GitHubUpdateRepository(appContext)
    private val updateStateRepository: UpdateStateRepository = FileUpdateStateRepository(appContext)

    val checkForUpdateUseCase = CheckForUpdateUseCase(updateRepository)
    val fetchReleaseNotesForVersionUseCase = FetchReleaseNotesForVersionUseCase(updateRepository)
    val downloadApkUseCase = DownloadApkUseCase(updateRepository)
    val observeLastSeenChangelogVersionCodeUseCase = ObserveLastSeenChangelogVersionCodeUseCase(updateStateRepository)
    val setLastSeenChangelogVersionCodeUseCase = SetLastSeenChangelogVersionCodeUseCase(updateStateRepository)

    val musicPlaybackCoordinator =
        MusicPlaybackCoordinator(
            observeConnectionState = observeConnectionStateUseCase,
            observeMusicEnabled = observeMusicEnabledUseCase,
            observeMusicPlayWhileBrowsingSystems = observeMusicPlayWhileBrowsingSystemsUseCase,
            observeMusicPlayWhileBrowsingGames = observeMusicPlayWhileBrowsingGamesUseCase,
            observeMusicPlayDuringScreensaver = observeMusicPlayDuringScreensaverUseCase,
            observeMusicDuckingMode = observeMusicDuckingModeUseCase,
            activityVisibilityRepository = activityVisibilityRepository,
            videoPlaybackStateRepository = videoPlaybackStateRepository,
            musicLibraryRepository = musicLibraryRepository,
            musicPlayerController = musicPlayerController,
            applicationScope = applicationScope,
            onTrackStarted = { track -> debugFileLogger.logPlaybackStarted("Music", track.filePath) },
            onPlaybackError = { track, message -> debugFileLogger.logPlaybackError("Music", track?.filePath, message) },
        )

    init {
        lidWakeGuardCoordinator.start(applicationScope)
        autoFpsCoordinator.start(appContext, applicationScope)
        taskKillerCoordinator.start(appContext, applicationScope)
        volumeSyncCoordinator.start(appContext, applicationScope)
        gameLaunchOverrideCoordinator.start(appContext, applicationScope)

        // Always-running, independent of whether edit mode is even open - records
        // whatever's actually been browsed so edit-mode preview has something real to
        // show later, and so a game/system browsed just before app restart is still
        // available as "last known" on the very next launch.
        applicationScope.launch {
            observeConnectionStateUseCase().collect { connection ->
                val appState = (connection as? EsdeConnectionState.Connected)?.appState ?: return@collect

                (appState as? AppState.BrowsingSystem)?.let { browsing ->
                    setLastSystemShortNameUseCase(browsing.systemShortName)
                }

                appState.currentGameReference()?.let { gameRef ->
                    setLastGameReferenceUseCase(gameRef)
                    setLastSystemShortNameUseCase(gameRef.systemShortName)
                }
            }
        }
    }
}
