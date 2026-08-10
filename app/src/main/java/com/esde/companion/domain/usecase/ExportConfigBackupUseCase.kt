package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AppConfigBackup
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import com.esde.companion.domain.repository.AppFolderRepository
import com.esde.companion.domain.repository.ConfigBackupRepository
import com.esde.companion.domain.repository.DockSettingsRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.repository.WidgetLayoutRepository
import kotlinx.coroutines.flow.first

/**
 * Captures every setting [AppConfigBackup] covers into one serialized snapshot, for
 * Settings > Setup > Backup & Restore's Export Backup action. Each value is read via a
 * single `.first()` off its repository's observe `Flow` - a one-shot snapshot, not a live
 * subscription, matching what "export the current state" means.
 */
class ExportConfigBackupUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val appDrawerSettingsRepository: AppDrawerSettingsRepository,
    private val appFolderRepository: AppFolderRepository,
    private val dockSettingsRepository: DockSettingsRepository,
    private val widgetLayoutRepository: WidgetLayoutRepository,
    private val configBackupRepository: ConfigBackupRepository,
) {
    suspend operator fun invoke(): String {
        val snapshot =
            AppConfigBackup(
                logFolderPath = onboardingRepository.observeLogFolderPath().first(),
                mediaFolderPath = onboardingRepository.observeMediaFolderPath().first(),
                customSystemImagesFolderPath = onboardingRepository.observeCustomSystemImagesFolderPath().first(),
                customLogosFolderPath = onboardingRepository.observeCustomLogosFolderPath().first(),
                customMusicFolderPath = onboardingRepository.observeCustomMusicFolderPath().first(),
                themePreference = onboardingRepository.observeThemePreference().first(),
                gamePlayingBehavior = onboardingRepository.observeGamePlayingBehavior().first(),
                screensaverBehavior = onboardingRepository.observeScreensaverBehavior().first(),
                overlayOpacityPercent = onboardingRepository.observeOverlayOpacityPercent().first(),
                fabAssignments = onboardingRepository.observeFabAssignments().first(),
                videoPlaybackEnabled = onboardingRepository.observeVideoPlaybackEnabled().first(),
                videoDelaySeconds = onboardingRepository.observeVideoDelaySeconds().first(),
                videoAudioEnabled = onboardingRepository.observeVideoAudioEnabled().first(),
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
                widgetCanvases = StateGroup.entries.associateWith { widgetLayoutRepository.observeCanvas(it).first() },
            )
        return configBackupRepository.serialize(snapshot)
    }
}
