package com.esde.companion.domain.repository

/**
 * Groups every settings repository [com.esde.companion.domain.usecase.ExportConfigBackupUseCase]/
 * [com.esde.companion.domain.usecase.RestoreConfigBackupUseCase] touch into one bundle, rather
 * than one constructor parameter each, keeping those use cases' own constructors within this
 * project's `LongParameterList` limit - same reasoning as
 * [com.esde.companion.data.storage.SelfHealConfig] (see CLAUDE.md's ktlint/detekt gotcha).
 * Ironically now at that same limit itself, once RetroAchievements' game-match-override
 * repository joined the six settings repositories Thor Settings already bundled here.
 */
@Suppress("LongParameterList")
class BackupRepositories(
    val onboardingRepository: OnboardingRepository,
    val appDrawerSettingsRepository: AppDrawerSettingsRepository,
    val appFolderRepository: AppFolderRepository,
    val dockSettingsRepository: DockSettingsRepository,
    val widgetLayoutRepository: WidgetLayoutRepository,
    val thorSettingsRepository: ThorSettingsRepository,
    val gameMatchOverrideRepository: GameMatchOverrideRepository,
    val gameLaunchAppRepository: GameLaunchAppRepository,
)
