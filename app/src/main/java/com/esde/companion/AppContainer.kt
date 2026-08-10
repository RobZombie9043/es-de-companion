package com.esde.companion

import android.content.Context
import android.os.Environment
import com.esde.companion.data.activity.ProcessActivityVisibilityRepository
import com.esde.companion.data.apps.PackageManagerAppsRepository
import com.esde.companion.data.context.FileLastKnownContextRepository
import com.esde.companion.data.debug.DebugFileLogger
import com.esde.companion.data.gamelist.ReactiveGameDescriptionRepository
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
import com.esde.companion.data.settings.FileAppDrawerSettingsRepository
import com.esde.companion.data.settings.FileAppFolderRepository
import com.esde.companion.data.settings.FileDockSettingsRepository
import com.esde.companion.data.settings.FileEsdeInstallationRepository
import com.esde.companion.data.settings.FileOnboardingRepository
import com.esde.companion.data.settings.FileWidgetLayoutRepository
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
import com.esde.companion.domain.repository.BundledSystemLogoRepository
import com.esde.companion.domain.repository.CustomSystemImageRepository
import com.esde.companion.domain.repository.CustomSystemLogoRepository
import com.esde.companion.domain.repository.DockSettingsRepository
import com.esde.companion.domain.repository.EsdeInstallationRepository
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.repository.GameDescriptionRepository
import com.esde.companion.domain.repository.GameMediaRepository
import com.esde.companion.domain.repository.InstalledAppsRepository
import com.esde.companion.domain.repository.LastKnownContextRepository
import com.esde.companion.domain.repository.MusicLibraryRepository
import com.esde.companion.domain.repository.MusicPlayerController
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.repository.SystemMediaRepository
import com.esde.companion.domain.repository.UpdateRepository
import com.esde.companion.domain.repository.UpdateStateRepository
import com.esde.companion.domain.repository.VideoPlaybackStateRepository
import com.esde.companion.domain.repository.WidgetLayoutRepository
import com.esde.companion.domain.state.AppStateReducer
import com.esde.companion.domain.usecase.CheckForUpdateUseCase
import com.esde.companion.domain.usecase.CompleteOnboardingUseCase
import com.esde.companion.domain.usecase.DeleteLegacyScriptFilesUseCase
import com.esde.companion.domain.usecase.DownloadApkUseCase
import com.esde.companion.domain.usecase.FetchReleaseNotesForVersionUseCase
import com.esde.companion.domain.usecase.FindLegacyScriptFilesUseCase
import com.esde.companion.domain.usecase.ObserveAppFoldersUseCase
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
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
import com.esde.companion.domain.usecase.ObserveGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveHiddenAppsUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveLastGameReferenceUseCase
import com.esde.companion.domain.usecase.ObserveLastSeenChangelogVersionCodeUseCase
import com.esde.companion.domain.usecase.ObserveLastSystemShortNameUseCase
import com.esde.companion.domain.usecase.ObserveLaunchEsdeOnStartEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.ObserveMusicEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.ObserveOnboardingCompleteUseCase
import com.esde.companion.domain.usecase.ObserveOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayOpacityUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveShowSearchBarUseCase
import com.esde.companion.domain.usecase.ObserveSortFoldersOnTopUseCase
import com.esde.companion.domain.usecase.ObserveThemePreferenceUseCase
import com.esde.companion.domain.usecase.ObserveVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.ObserveVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.ObserveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.ReadEsdeEventScriptSettingsUseCase
import com.esde.companion.domain.usecase.ReadEsdeMediaDirectoryUseCase
import com.esde.companion.domain.usecase.ResolveBundledSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemImageUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveGameDescriptionUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import com.esde.companion.domain.usecase.ResolveRandomSystemMediaUseCase
import com.esde.companion.domain.usecase.SaveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.SetAppFoldersUseCase
import com.esde.companion.domain.usecase.SetCloseCompanionOnQuitEnabledUseCase
import com.esde.companion.domain.usecase.SetDebugLoggingEnabledUseCase
import com.esde.companion.domain.usecase.SetDockAppsUseCase
import com.esde.companion.domain.usecase.SetDockEnabledUseCase
import com.esde.companion.domain.usecase.SetDockMaxAppsUseCase
import com.esde.companion.domain.usecase.SetDockSizeUseCase
import com.esde.companion.domain.usecase.SetFabAssignmentUseCase
import com.esde.companion.domain.usecase.SetGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.SetGridColumnsUseCase
import com.esde.companion.domain.usecase.SetHiddenAppsUseCase
import com.esde.companion.domain.usecase.SetLastGameReferenceUseCase
import com.esde.companion.domain.usecase.SetLastSeenChangelogVersionCodeUseCase
import com.esde.companion.domain.usecase.SetLastSystemShortNameUseCase
import com.esde.companion.domain.usecase.SetLaunchEsdeOnStartEnabledUseCase
import com.esde.companion.domain.usecase.SetMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.SetMusicEnabledUseCase
import com.esde.companion.domain.usecase.SetMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.SetOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.SetOverlayOpacityUseCase
import com.esde.companion.domain.usecase.SetScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.SetShowSearchBarUseCase
import com.esde.companion.domain.usecase.SetSortFoldersOnTopUseCase
import com.esde.companion.domain.usecase.SetThemePreferenceUseCase
import com.esde.companion.domain.usecase.SetVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.SetVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.SetVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.ValidateEsdeLogFolderUseCase
import com.esde.companion.domain.usecase.ValidateEsdeMediaFolderUseCase
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

    val esdeInstallationRepository: EsdeInstallationRepository = FileEsdeInstallationRepository()

    val activityVisibilityRepository: ActivityVisibilityRepository = ProcessActivityVisibilityRepository()

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

    // Video-overlay playback events (VideoOverlayScreen) are reported from a Compose/
    // ExoPlayer callback, not through a use case - exposed as plain function references
    // (rather than the DebugFileLogger type itself) so MainActivity, which already wires
    // other domain-typed callbacks like videoPlaybackStateRepository::setIsPlaying down
    // into VideoOverlayScreen, can do the same here without reaching into the data layer.
    val logVideoPlaybackStarted: (String) -> Unit = { path -> debugFileLogger.logPlaybackStarted("Video", path) }
    val logVideoPlaybackError: (String, String) -> Unit =
        { path, message -> debugFileLogger.logPlaybackError("Video", path, message) }

    private val logRepository: EsdeLogRepository =
        SharedEsdeLogRepository(
            inner = ReactiveEsdeLogRepository(logFolderPath = onboardingRepository.observeLogFolderPath()),
            scope = applicationScope,
        )

    // Same reactive-to-Settings pattern as logRepository, for the media folder.
    private val gameMediaRepository: GameMediaRepository =
        LoggingGameMediaRepository(
            inner = ReactiveGameMediaRepository(mediaFolderPath = onboardingRepository.observeMediaFolderPath()),
            debugFileLogger = debugFileLogger,
        )

    // gamelists/ lives alongside logs/ under the ES-DE root, so this reacts to the log
    // folder path, not the media folder path - see ReactiveGameDescriptionRepository.
    private val gameDescriptionRepository: GameDescriptionRepository =
        ReactiveGameDescriptionRepository(esdeRootPath = onboardingRepository.observeLogFolderPath())

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
    val observeScreensaverBehaviorUseCase = ObserveScreensaverBehaviorUseCase(onboardingRepository)
    val setScreensaverBehaviorUseCase = SetScreensaverBehaviorUseCase(onboardingRepository)
    val observeThemePreferenceUseCase = ObserveThemePreferenceUseCase(onboardingRepository)
    val setThemePreferenceUseCase = SetThemePreferenceUseCase(onboardingRepository)
    val observeVideoPlaybackEnabledUseCase = ObserveVideoPlaybackEnabledUseCase(onboardingRepository)
    val setVideoPlaybackEnabledUseCase = SetVideoPlaybackEnabledUseCase(onboardingRepository)
    val observeVideoDelaySecondsUseCase = ObserveVideoDelaySecondsUseCase(onboardingRepository)
    val setVideoDelaySecondsUseCase = SetVideoDelaySecondsUseCase(onboardingRepository)
    val observeVideoAudioEnabledUseCase = ObserveVideoAudioEnabledUseCase(onboardingRepository)
    val setVideoAudioEnabledUseCase = SetVideoAudioEnabledUseCase(onboardingRepository)

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
    val observeLaunchEsdeOnStartEnabledUseCase = ObserveLaunchEsdeOnStartEnabledUseCase(onboardingRepository)
    val setLaunchEsdeOnStartEnabledUseCase = SetLaunchEsdeOnStartEnabledUseCase(onboardingRepository)
    val observeDebugLoggingEnabledUseCase = ObserveDebugLoggingEnabledUseCase(onboardingRepository)
    val setDebugLoggingEnabledUseCase = SetDebugLoggingEnabledUseCase(onboardingRepository)

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
