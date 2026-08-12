package com.esde.companion.data.backup

import com.esde.companion.data.settings.CanvasDto
import kotlinx.serialization.Serializable

/**
 * Wire format for [com.esde.companion.domain.model.AppConfigBackup]. Enums are stored as
 * their plain `.name` string, same convention `FileOnboardingRepository`/
 * `FileDockSettingsRepository` already use for DataStore, so restoring falls back to a
 * sensible default on an unrecognized/renamed enum value instead of failing the whole
 * restore - see `ConfigBackupMapping`'s `toEnumOrDefault`.
 *
 * [widgetCanvases] is keyed by [com.esde.companion.domain.model.StateGroup]'s `.name`
 * rather than the enum itself, matching every other enum field here - JSON object keys are
 * strings regardless, and this keeps the convention uniform rather than relying on
 * kotlinx.serialization's enum-as-map-key support for just this one field.
 *
 * FAB corners are four flat type/customApp field pairs, mirroring exactly how
 * `FileOnboardingRepository` already stores them as eight independent DataStore keys,
 * rather than introducing a nested `FabSlotDto` this one caller would be the only user of.
 */
@Serializable
internal data class ConfigBackupDto(
    val version: Int,
    val logFolderPath: String? = null,
    val mediaFolderPath: String? = null,
    val customSystemImagesFolderPath: String? = null,
    val customLogosFolderPath: String? = null,
    val customMusicFolderPath: String? = null,
    val themePreference: String,
    val gamePlayingBehavior: String,
    val gamePlayingDimPercent: Int = DEFAULT_DIM_PERCENT,
    val screensaverBehavior: String,
    val screensaverDimPercent: Int = DEFAULT_DIM_PERCENT,
    val overlayOpacityPercent: Int,
    val fabTopStartType: String,
    val fabTopStartCustomApp: String? = null,
    val fabTopEndType: String,
    val fabTopEndCustomApp: String? = null,
    val fabBottomStartType: String,
    val fabBottomStartCustomApp: String? = null,
    val fabBottomEndType: String,
    val fabBottomEndCustomApp: String? = null,
    val videoPlaybackEnabled: Boolean,
    val videoDelaySeconds: Int,
    val videoAudioEnabled: Boolean,
    val musicEnabled: Boolean,
    val musicPlayWhileBrowsingSystems: Boolean,
    val musicPlayWhileBrowsingGames: Boolean,
    val musicPlayDuringScreensaver: Boolean,
    val musicDuckingMode: String,
    val closeCompanionOnQuitEnabled: Boolean,
    val launchEsdeOnStartEnabled: Boolean,
    val debugLoggingEnabled: Boolean,
    val hiddenApps: Set<String>,
    val gridColumns: Int,
    val otherScreenLaunchApps: Set<String>,
    val sortFoldersOnTop: Boolean,
    val showSearchBar: Boolean,
    val folders: List<AppFolderDto>,
    val dockEnabled: Boolean,
    val dockMaxApps: Int,
    val dockSize: String,
    val dockApps: List<String>,
    val widgetCanvases: Map<String, CanvasDto>,
    val gameMatchOverrides: List<GameMatchOverrideDto> = emptyList(),
)

@Serializable
internal data class AppFolderDto(
    val id: String,
    val name: String,
    val memberPackageNames: Set<String>,
)

@Serializable
internal data class GameMatchOverrideDto(
    val systemShortName: String,
    val romPath: String,
    val raGameId: Long,
)

/** Matches [com.esde.companion.data.settings.FileOnboardingRepository]'s own default,
 * used when decoding a pre-Dim-slider backup that has no dim percent fields at all. */
private const val DEFAULT_DIM_PERCENT = 50
