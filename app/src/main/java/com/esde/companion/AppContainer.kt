package com.esde.companion

import android.content.Context
import com.esde.companion.data.activity.ProcessActivityVisibilityRepository
import com.esde.companion.data.apps.PackageManagerAppsRepository
import com.esde.companion.data.context.FileLastKnownContextRepository
import com.esde.companion.data.gamelist.ReactiveGameDescriptionRepository
import com.esde.companion.data.log.ReactiveEsdeLogRepository
import com.esde.companion.data.log.SharedEsdeLogRepository
import com.esde.companion.data.media.ReactiveCustomSystemImageRepository
import com.esde.companion.data.media.ReactiveCustomSystemLogoRepository
import com.esde.companion.data.media.ReactiveGameMediaRepository
import com.esde.companion.data.media.ReactiveSystemMediaRepository
import com.esde.companion.data.music.ExoMusicPlayerController
import com.esde.companion.data.music.ReactiveMusicLibraryRepository
import com.esde.companion.data.settings.FileAppDrawerSettingsRepository
import com.esde.companion.data.settings.FileDockSettingsRepository
import com.esde.companion.data.settings.FileOnboardingRepository
import com.esde.companion.data.settings.FileWidgetLayoutRepository
import com.esde.companion.data.video.ProcessVideoPlaybackStateRepository
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.currentGameReference
import com.esde.companion.domain.music.MusicPlaybackCoordinator
import com.esde.companion.domain.repository.ActivityVisibilityRepository
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import com.esde.companion.domain.repository.CustomSystemImageRepository
import com.esde.companion.domain.repository.CustomSystemLogoRepository
import com.esde.companion.domain.repository.DockSettingsRepository
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.repository.GameDescriptionRepository
import com.esde.companion.domain.repository.GameMediaRepository
import com.esde.companion.domain.repository.InstalledAppsRepository
import com.esde.companion.domain.repository.LastKnownContextRepository
import com.esde.companion.domain.repository.MusicLibraryRepository
import com.esde.companion.domain.repository.MusicPlayerController
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.repository.SystemMediaRepository
import com.esde.companion.domain.repository.VideoPlaybackStateRepository
import com.esde.companion.domain.repository.WidgetLayoutRepository
import com.esde.companion.domain.usecase.CompleteOnboardingUseCase
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveDockAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockMaxAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockOpacityUseCase
import com.esde.companion.domain.usecase.ObserveDockSizeUseCase
import com.esde.companion.domain.usecase.ObserveDrawerOpacityUseCase
import com.esde.companion.domain.usecase.ObserveGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveHiddenAppsUseCase
import com.esde.companion.domain.usecase.ObserveImageTransitionModeUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveLastGameReferenceUseCase
import com.esde.companion.domain.usecase.ObserveLastSystemShortNameUseCase
import com.esde.companion.domain.usecase.ObserveLogoTransitionModeUseCase
import com.esde.companion.domain.usecase.ObserveMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.ObserveMusicEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.ObserveOnboardingCompleteUseCase
import com.esde.companion.domain.usecase.ObserveOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayEnabledUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveThemePreferenceUseCase
import com.esde.companion.domain.usecase.ObserveVideoAspectRatioModeUseCase
import com.esde.companion.domain.usecase.ObserveVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.ObserveVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.ObserveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.ObserveWidgetsLockedUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemImageUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveGameDescriptionUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import com.esde.companion.domain.usecase.ResolveRandomSystemMediaUseCase
import com.esde.companion.domain.usecase.SaveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.SetDockAppsUseCase
import com.esde.companion.domain.usecase.SetDockEnabledUseCase
import com.esde.companion.domain.usecase.SetDockMaxAppsUseCase
import com.esde.companion.domain.usecase.SetDockOpacityUseCase
import com.esde.companion.domain.usecase.SetDockSizeUseCase
import com.esde.companion.domain.usecase.SetDrawerOpacityUseCase
import com.esde.companion.domain.usecase.SetGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.SetGridColumnsUseCase
import com.esde.companion.domain.usecase.SetHiddenAppsUseCase
import com.esde.companion.domain.usecase.SetImageTransitionModeUseCase
import com.esde.companion.domain.usecase.SetLastGameReferenceUseCase
import com.esde.companion.domain.usecase.SetLastSystemShortNameUseCase
import com.esde.companion.domain.usecase.SetLogoTransitionModeUseCase
import com.esde.companion.domain.usecase.SetMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.SetMusicEnabledUseCase
import com.esde.companion.domain.usecase.SetMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.SetOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.SetOverlayEnabledUseCase
import com.esde.companion.domain.usecase.SetScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.SetThemePreferenceUseCase
import com.esde.companion.domain.usecase.SetVideoAspectRatioModeUseCase
import com.esde.companion.domain.usecase.SetVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.SetVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.SetVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.SetWidgetsLockedUseCase
import com.esde.companion.domain.usecase.ValidateEsdeLogFolderUseCase
import com.esde.companion.domain.usecase.ValidateEsdeMediaFolderUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

    val activityVisibilityRepository: ActivityVisibilityRepository = ProcessActivityVisibilityRepository()

    val videoPlaybackStateRepository: VideoPlaybackStateRepository = ProcessVideoPlaybackStateRepository()

    private val logRepository: EsdeLogRepository = SharedEsdeLogRepository(
        inner = ReactiveEsdeLogRepository(logFolderPath = onboardingRepository.observeLogFolderPath()),
        scope = applicationScope,
    )

    // Same reactive-to-Settings pattern as logRepository, for the media folder.
    private val gameMediaRepository: GameMediaRepository =
        ReactiveGameMediaRepository(mediaFolderPath = onboardingRepository.observeMediaFolderPath())

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

    private val musicLibraryRepository: MusicLibraryRepository =
        ReactiveMusicLibraryRepository(musicFolderPath = onboardingRepository.observeCustomMusicFolderPath())

    private val musicPlayerController: MusicPlayerController = ExoMusicPlayerController(appContext)

    val resolveCustomSystemImageUseCase = ResolveCustomSystemImageUseCase(customSystemImageRepository)
    val resolveCustomSystemLogoUseCase = ResolveCustomSystemLogoUseCase(customSystemLogoRepository)

    private val installedAppsRepository: InstalledAppsRepository = PackageManagerAppsRepository(appContext)
    val observeInstalledAppsUseCase = ObserveInstalledAppsUseCase(installedAppsRepository)

    // App Drawer display/visibility settings (hidden apps, opacity, grid columns) - a
    // distinct concern from onboarding, kept in its own repository/DataStore. See
    // AppDrawerSettingsRepository.
    private val appDrawerSettingsRepository: AppDrawerSettingsRepository = FileAppDrawerSettingsRepository(appContext)

    private val widgetLayoutRepository: WidgetLayoutRepository = FileWidgetLayoutRepository(appContext)

    val observeWidgetCanvasUseCase = ObserveWidgetCanvasUseCase(widgetLayoutRepository)
    val saveWidgetCanvasUseCase = SaveWidgetCanvasUseCase(widgetLayoutRepository)
    val observeWidgetsLockedUseCase = ObserveWidgetsLockedUseCase(widgetLayoutRepository)
    val setWidgetsLockedUseCase = SetWidgetsLockedUseCase(widgetLayoutRepository)

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

    val observeDrawerOpacityUseCase = ObserveDrawerOpacityUseCase(appDrawerSettingsRepository)
    val setDrawerOpacityUseCase = SetDrawerOpacityUseCase(appDrawerSettingsRepository)

    val observeGridColumnsUseCase = ObserveGridColumnsUseCase(appDrawerSettingsRepository)
    val setGridColumnsUseCase = SetGridColumnsUseCase(appDrawerSettingsRepository)

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
    val observeDockOpacityUseCase = ObserveDockOpacityUseCase(dockSettingsRepository)
    val setDockOpacityUseCase = SetDockOpacityUseCase(dockSettingsRepository)
    val observeDockAppsUseCase = ObserveDockAppsUseCase(dockSettingsRepository)
    val setDockAppsUseCase = SetDockAppsUseCase(dockSettingsRepository)

    val observeAppStateUseCase = ObserveAppStateUseCase(logRepository, applicationScope)
    val observeConnectionStateUseCase = ObserveConnectionStateUseCase(logRepository, observeAppStateUseCase)
    val resolveGameMediaUseCase = ResolveGameMediaUseCase(gameMediaRepository)
    val resolveGameDescriptionUseCase = ResolveGameDescriptionUseCase(gameDescriptionRepository)
    val resolveRandomSystemMediaUseCase = ResolveRandomSystemMediaUseCase(systemMediaRepository)

    val validateEsdeLogFolderUseCase = ValidateEsdeLogFolderUseCase(onboardingRepository)
    val validateEsdeMediaFolderUseCase = ValidateEsdeMediaFolderUseCase(onboardingRepository)
    val completeOnboardingUseCase = CompleteOnboardingUseCase(onboardingRepository)
    val observeOnboardingCompleteUseCase = ObserveOnboardingCompleteUseCase(onboardingRepository)

    val observeOverlayEnabledUseCase = ObserveOverlayEnabledUseCase(onboardingRepository)
    val setOverlayEnabledUseCase = SetOverlayEnabledUseCase(onboardingRepository)
    val observeGamePlayingBehaviorUseCase = ObserveGamePlayingBehaviorUseCase(onboardingRepository)
    val setGamePlayingBehaviorUseCase = SetGamePlayingBehaviorUseCase(onboardingRepository)
    val observeScreensaverBehaviorUseCase = ObserveScreensaverBehaviorUseCase(onboardingRepository)
    val setScreensaverBehaviorUseCase = SetScreensaverBehaviorUseCase(onboardingRepository)
    val observeThemePreferenceUseCase = ObserveThemePreferenceUseCase(onboardingRepository)
    val setThemePreferenceUseCase = SetThemePreferenceUseCase(onboardingRepository)
    val observeImageTransitionModeUseCase = ObserveImageTransitionModeUseCase(onboardingRepository)
    val setImageTransitionModeUseCase = SetImageTransitionModeUseCase(onboardingRepository)
    val observeLogoTransitionModeUseCase = ObserveLogoTransitionModeUseCase(onboardingRepository)
    val setLogoTransitionModeUseCase = SetLogoTransitionModeUseCase(onboardingRepository)
    val observeVideoPlaybackEnabledUseCase = ObserveVideoPlaybackEnabledUseCase(onboardingRepository)
    val setVideoPlaybackEnabledUseCase = SetVideoPlaybackEnabledUseCase(onboardingRepository)
    val observeVideoDelaySecondsUseCase = ObserveVideoDelaySecondsUseCase(onboardingRepository)
    val setVideoDelaySecondsUseCase = SetVideoDelaySecondsUseCase(onboardingRepository)
    val observeVideoAudioEnabledUseCase = ObserveVideoAudioEnabledUseCase(onboardingRepository)
    val setVideoAudioEnabledUseCase = SetVideoAudioEnabledUseCase(onboardingRepository)
    val observeVideoAspectRatioModeUseCase = ObserveVideoAspectRatioModeUseCase(onboardingRepository)
    val setVideoAspectRatioModeUseCase = SetVideoAspectRatioModeUseCase(onboardingRepository)

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

    val musicPlaybackCoordinator = MusicPlaybackCoordinator(
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
