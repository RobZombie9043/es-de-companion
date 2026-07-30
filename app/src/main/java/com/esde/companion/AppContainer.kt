package com.esde.companion

import android.content.Context
import com.esde.companion.data.apps.PackageManagerAppsRepository
import com.esde.companion.data.log.ReactiveEsdeLogRepository
import com.esde.companion.data.media.ReactiveGameMediaRepository
import com.esde.companion.data.media.ReactiveSystemMediaRepository
import com.esde.companion.data.settings.FileAppDrawerSettingsRepository
import com.esde.companion.data.settings.FileOnboardingRepository
import com.esde.companion.data.settings.FileWidgetLayoutRepository
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.repository.GameMediaRepository
import com.esde.companion.domain.repository.InstalledAppsRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.repository.SystemMediaRepository
import com.esde.companion.domain.repository.WidgetLayoutRepository
import com.esde.companion.domain.usecase.CompleteOnboardingUseCase
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveDrawerOpacityUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveHiddenAppsUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveOnboardingCompleteUseCase
import com.esde.companion.domain.usecase.ObserveOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayEnabledUseCase
import com.esde.companion.domain.usecase.ObserveThemePreferenceUseCase
import com.esde.companion.domain.usecase.ObserveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.ObserveWidgetsLockedUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import com.esde.companion.domain.usecase.ResolveRandomSystemMediaUseCase
import com.esde.companion.domain.usecase.SaveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.SetDrawerOpacityUseCase
import com.esde.companion.domain.usecase.SetGridColumnsUseCase
import com.esde.companion.domain.usecase.SetHiddenAppsUseCase
import com.esde.companion.domain.usecase.SetOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.SetOverlayEnabledUseCase
import com.esde.companion.domain.usecase.SetThemePreferenceUseCase
import com.esde.companion.domain.usecase.SetWidgetsLockedUseCase
import com.esde.companion.domain.usecase.ValidateEsdeLogFolderUseCase
import com.esde.companion.domain.usecase.ValidateEsdeMediaFolderUseCase

/**
 * Minimal hand-rolled composition root. At this project's current scale (single module,
 * one developer) this is a deliberate, simpler alternative to a DI framework - see
 * CLAUDE.md. If/when the dependency graph grows enough to justify it, this can be
 * replaced with Hilt without changing anything below the ViewModel layer, since
 * everything here is already expressed as interfaces / constructor dependencies.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val onboardingRepository: OnboardingRepository = FileOnboardingRepository(appContext)

    // Reacts to the user changing the ES-DE folder later via Settings, without requiring
    // the process to restart - see ReactiveEsdeLogRepository.
    private val logRepository: EsdeLogRepository =
        ReactiveEsdeLogRepository(logFolderPath = onboardingRepository.observeLogFolderPath())

    // Same reactive-to-Settings pattern as logRepository, for the media folder.
    private val gameMediaRepository: GameMediaRepository =
        ReactiveGameMediaRepository(mediaFolderPath = onboardingRepository.observeMediaFolderPath())

    private val systemMediaRepository: SystemMediaRepository =
        ReactiveSystemMediaRepository(mediaFolderPath = onboardingRepository.observeMediaFolderPath())

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

    val observeHiddenAppsUseCase = ObserveHiddenAppsUseCase(appDrawerSettingsRepository)
    val setHiddenAppsUseCase = SetHiddenAppsUseCase(appDrawerSettingsRepository)

    val observeOtherScreenLaunchAppsUseCase = ObserveOtherScreenLaunchAppsUseCase(appDrawerSettingsRepository)
    val setOtherScreenLaunchAppsUseCase = SetOtherScreenLaunchAppsUseCase(appDrawerSettingsRepository)

    val observeDrawerOpacityUseCase = ObserveDrawerOpacityUseCase(appDrawerSettingsRepository)
    val setDrawerOpacityUseCase = SetDrawerOpacityUseCase(appDrawerSettingsRepository)

    val observeGridColumnsUseCase = ObserveGridColumnsUseCase(appDrawerSettingsRepository)
    val setGridColumnsUseCase = SetGridColumnsUseCase(appDrawerSettingsRepository)

    val observeAppStateUseCase = ObserveAppStateUseCase(logRepository)
    val observeConnectionStateUseCase = ObserveConnectionStateUseCase(logRepository, observeAppStateUseCase)
    val resolveGameMediaUseCase = ResolveGameMediaUseCase(gameMediaRepository)
    val resolveRandomSystemMediaUseCase = ResolveRandomSystemMediaUseCase(systemMediaRepository)

    val validateEsdeLogFolderUseCase = ValidateEsdeLogFolderUseCase(onboardingRepository)
    val validateEsdeMediaFolderUseCase = ValidateEsdeMediaFolderUseCase(onboardingRepository)
    val completeOnboardingUseCase = CompleteOnboardingUseCase(onboardingRepository)
    val observeOnboardingCompleteUseCase = ObserveOnboardingCompleteUseCase(onboardingRepository)

    val observeOverlayEnabledUseCase = ObserveOverlayEnabledUseCase(onboardingRepository)
    val setOverlayEnabledUseCase = SetOverlayEnabledUseCase(onboardingRepository)

    val observeThemePreferenceUseCase = ObserveThemePreferenceUseCase(onboardingRepository)
    val setThemePreferenceUseCase = SetThemePreferenceUseCase(onboardingRepository)
}