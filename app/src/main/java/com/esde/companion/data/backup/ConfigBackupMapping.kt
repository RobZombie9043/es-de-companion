package com.esde.companion.data.backup

import com.esde.companion.data.settings.CanvasDto
import com.esde.companion.data.settings.canvasDtoOf
import com.esde.companion.data.settings.toDomain
import com.esde.companion.domain.model.AppConfigBackup
import com.esde.companion.domain.model.AppFolder
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.FabSlot
import com.esde.companion.domain.model.FabType
import com.esde.companion.domain.model.GameMatchOverride
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.SavedWidgetCanvas
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.ThemePreference

internal fun AppConfigBackup.toDto(): ConfigBackupDto =
    ConfigBackupDto(
        version = version,
        logFolderPath = logFolderPath,
        mediaFolderPath = mediaFolderPath,
        customSystemImagesFolderPath = customSystemImagesFolderPath,
        customLogosFolderPath = customLogosFolderPath,
        customMusicFolderPath = customMusicFolderPath,
        themePreference = themePreference.name,
        gamePlayingBehavior = gamePlayingBehavior.name,
        gamePlayingDimPercent = gamePlayingDimPercent,
        screensaverBehavior = screensaverBehavior.name,
        screensaverDimPercent = screensaverDimPercent,
        overlayOpacityPercent = overlayOpacityPercent,
        fabTopStartType = fabAssignments.topStart.type.name,
        fabTopStartCustomApp = fabAssignments.topStart.customAppPackageName,
        fabTopEndType = fabAssignments.topEnd.type.name,
        fabTopEndCustomApp = fabAssignments.topEnd.customAppPackageName,
        fabBottomStartType = fabAssignments.bottomStart.type.name,
        fabBottomStartCustomApp = fabAssignments.bottomStart.customAppPackageName,
        fabBottomEndType = fabAssignments.bottomEnd.type.name,
        fabBottomEndCustomApp = fabAssignments.bottomEnd.customAppPackageName,
        videoPlaybackEnabled = videoPlaybackEnabled,
        videoDelaySeconds = videoDelaySeconds,
        videoAudioEnabled = videoAudioEnabled,
        musicEnabled = musicEnabled,
        musicPlayWhileBrowsingSystems = musicPlayWhileBrowsingSystems,
        musicPlayWhileBrowsingGames = musicPlayWhileBrowsingGames,
        musicPlayDuringScreensaver = musicPlayDuringScreensaver,
        musicDuckingMode = musicDuckingMode.name,
        closeCompanionOnQuitEnabled = closeCompanionOnQuitEnabled,
        launchEsdeOnStartEnabled = launchEsdeOnStartEnabled,
        debugLoggingEnabled = debugLoggingEnabled,
        hiddenApps = hiddenApps,
        gridColumns = gridColumns,
        otherScreenLaunchApps = otherScreenLaunchApps,
        sortFoldersOnTop = sortFoldersOnTop,
        showSearchBar = showSearchBar,
        folders = folders.map { it.toDto() },
        dockEnabled = dockEnabled,
        dockMaxApps = dockMaxApps,
        dockSize = dockSize.name,
        dockApps = dockApps,
        widgetCanvases = widgetCanvases.toDtoMap(),
        gameMatchOverrides = gameMatchOverrides.map { it.toDto() },
        updateAchievementsOnScreensaverEnabled = updateAchievementsOnScreensaverEnabled,
    )

internal fun ConfigBackupDto.toDomain(): AppConfigBackup {
    val fabAssignments =
        FabAssignments(
            topStart = FabSlot(fabTopStartType.toEnumOrDefault(FabType.None), fabTopStartCustomApp),
            topEnd = FabSlot(fabTopEndType.toEnumOrDefault(FabType.None), fabTopEndCustomApp),
            bottomStart = FabSlot(fabBottomStartType.toEnumOrDefault(FabType.None), fabBottomStartCustomApp),
            bottomEnd = FabSlot(fabBottomEndType.toEnumOrDefault(FabType.None), fabBottomEndCustomApp),
        )
    return AppConfigBackup(
        version = version,
        logFolderPath = logFolderPath,
        mediaFolderPath = mediaFolderPath,
        customSystemImagesFolderPath = customSystemImagesFolderPath,
        customLogosFolderPath = customLogosFolderPath,
        customMusicFolderPath = customMusicFolderPath,
        themePreference = themePreference.toEnumOrDefault(ThemePreference.Auto),
        gamePlayingBehavior = gamePlayingBehavior.toEnumOrDefault(ScreenBehavior.Nothing),
        gamePlayingDimPercent = gamePlayingDimPercent,
        screensaverBehavior = screensaverBehavior.toEnumOrDefault(ScreenBehavior.Nothing),
        screensaverDimPercent = screensaverDimPercent,
        overlayOpacityPercent = overlayOpacityPercent,
        fabAssignments = fabAssignments,
        videoPlaybackEnabled = videoPlaybackEnabled,
        videoDelaySeconds = videoDelaySeconds,
        videoAudioEnabled = videoAudioEnabled,
        musicEnabled = musicEnabled,
        musicPlayWhileBrowsingSystems = musicPlayWhileBrowsingSystems,
        musicPlayWhileBrowsingGames = musicPlayWhileBrowsingGames,
        musicPlayDuringScreensaver = musicPlayDuringScreensaver,
        musicDuckingMode = musicDuckingMode.toEnumOrDefault(MusicDuckingMode.LowerVolume),
        closeCompanionOnQuitEnabled = closeCompanionOnQuitEnabled,
        launchEsdeOnStartEnabled = launchEsdeOnStartEnabled,
        debugLoggingEnabled = debugLoggingEnabled,
        hiddenApps = hiddenApps,
        gridColumns = gridColumns,
        otherScreenLaunchApps = otherScreenLaunchApps,
        sortFoldersOnTop = sortFoldersOnTop,
        showSearchBar = showSearchBar,
        folders = folders.map { it.toDomain() },
        dockEnabled = dockEnabled,
        dockMaxApps = dockMaxApps,
        dockSize = dockSize.toEnumOrDefault(DockSize.Medium),
        dockApps = dockApps,
        widgetCanvases = widgetCanvases.toDomainMap(),
        gameMatchOverrides = gameMatchOverrides.map { it.toDomain() },
        updateAchievementsOnScreensaverEnabled = updateAchievementsOnScreensaverEnabled,
    )
}

/**
 * Canvases with no grid saved yet (see [SavedWidgetCanvas]'s kdoc) are omitted entirely
 * rather than encoded - nothing meaningful to persist, same reasoning
 * [com.esde.companion.domain.usecase.RestoreConfigBackupUseCase] uses to skip them on the
 * way back in.
 */
private fun Map<StateGroup, SavedWidgetCanvas>.toDtoMap(): Map<String, CanvasDto> =
    mapNotNull { (stateGroup, canvas) ->
        val grid = canvas.grid ?: return@mapNotNull null
        stateGroup.name to canvasDtoOf(canvas.widgets, grid)
    }.toMap()

private fun Map<String, CanvasDto>.toDomainMap(): Map<StateGroup, SavedWidgetCanvas> =
    StateGroup.entries.associateWith { stateGroup ->
        this[stateGroup.name]?.toDomain() ?: SavedWidgetCanvas(grid = null, widgets = emptyList())
    }

private inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T {
    return runCatching { enumValueOf<T>(this) }.getOrDefault(default)
}

private fun AppFolder.toDto() = AppFolderDto(id = id, name = name, memberPackageNames = memberPackageNames)

private fun AppFolderDto.toDomain() = AppFolder(id = id, name = name, memberPackageNames = memberPackageNames)

private fun GameMatchOverride.toDto(): GameMatchOverrideDto {
    return GameMatchOverrideDto(systemShortName = systemShortName, romPath = romPath, raGameId = raGameId)
}

private fun GameMatchOverrideDto.toDomain(): GameMatchOverride {
    return GameMatchOverride(systemShortName = systemShortName, romPath = romPath, raGameId = raGameId)
}
