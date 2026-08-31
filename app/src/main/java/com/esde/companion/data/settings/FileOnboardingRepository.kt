package com.esde.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.FabSlot
import com.esde.companion.domain.model.FabType
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

private const val MIN_PERCENT = 0
private const val MAX_PERCENT = 100

/**
 * DataStore-backed [OnboardingRepository]. Folder existence/log-file checks are plain
 * File I/O (this app isn't Play-Store distributed, so MANAGE_EXTERNAL_STORAGE is granted
 * at runtime during onboarding rather than using SAF - see conversation history / the
 * folder-picker screens for why).
 *
 * [context] is expected to be an application context (see AppContainer), not an Activity
 * context - this repository can outlive any single Activity.
 */
class FileOnboardingRepository(
    private val context: Context,
) : OnboardingRepository {
    override fun defaultLogFolderPath(): String = DEFAULT_ESDE_ROOT

    override fun defaultMediaFolderPath(): String = "$DEFAULT_ESDE_ROOT/downloaded_media"

    override suspend fun validateLogFolder(path: String): LogFolderValidation =
        withContext(Dispatchers.IO) {
            val folder = File(path)
            if (!folder.isDirectory) {
                LogFolderValidation.FolderNotFound
            } else {
                val settingsFile = File(folder, "settings/es_settings.xml")
                LogFolderValidation.FolderFound(settingsFileFound = settingsFile.isFile)
            }
        }

    override suspend fun validateMediaFolder(path: String): MediaFolderValidation =
        withContext(Dispatchers.IO) {
            if (File(path).isDirectory) {
                MediaFolderValidation.FolderFound
            } else {
                MediaFolderValidation.FolderNotFound
            }
        }

    override suspend fun saveLogFolderPath(path: String) {
        context.onboardingDataStore.edit { it[LOG_FOLDER_PATH_KEY] = path }
    }

    override suspend fun saveMediaFolderPath(path: String) {
        context.onboardingDataStore.edit { it[MEDIA_FOLDER_PATH_KEY] = path }
    }

    override fun observeLogFolderPath(): Flow<String?> {
        return context.onboardingDataStore.data.map { it[LOG_FOLDER_PATH_KEY] }
    }

    override fun observeMediaFolderPath(): Flow<String?> =
        context.onboardingDataStore.data.map {
            it[MEDIA_FOLDER_PATH_KEY]
        }

    override suspend fun saveCustomSystemImagesFolderPath(path: String) {
        context.onboardingDataStore.edit { it[CUSTOM_SYSTEM_IMAGES_FOLDER_PATH_KEY] = path }
    }

    override fun observeCustomSystemImagesFolderPath(): Flow<String?> =
        context.onboardingDataStore.data.map { it[CUSTOM_SYSTEM_IMAGES_FOLDER_PATH_KEY] }

    override suspend fun clearCustomSystemImagesFolderPath() {
        context.onboardingDataStore.edit { it.remove(CUSTOM_SYSTEM_IMAGES_FOLDER_PATH_KEY) }
    }

    override suspend fun saveCustomLogosFolderPath(path: String) {
        context.onboardingDataStore.edit { it[CUSTOM_LOGOS_FOLDER_PATH_KEY] = path }
    }

    override fun observeCustomLogosFolderPath(): Flow<String?> =
        context.onboardingDataStore.data.map {
            it[CUSTOM_LOGOS_FOLDER_PATH_KEY]
        }

    override suspend fun clearCustomLogosFolderPath() {
        context.onboardingDataStore.edit { it.remove(CUSTOM_LOGOS_FOLDER_PATH_KEY) }
    }

    override suspend fun markOnboardingComplete() {
        context.onboardingDataStore.edit { it[ONBOARDING_COMPLETE_KEY] = true }
    }

    override fun observeOnboardingComplete(): Flow<Boolean> =
        context.onboardingDataStore.data.map {
            it[ONBOARDING_COMPLETE_KEY] ?: false
        }

    override suspend fun setThemePreference(preference: ThemePreference) {
        context.onboardingDataStore.edit { it[THEME_PREFERENCE_KEY] = preference.name }
    }

    override fun observeThemePreference(): Flow<ThemePreference> =
        context.onboardingDataStore.data.map { prefs ->
            // Falls back to Auto for both the unset case and any unrecognized stored
            // value (e.g. an old build's Ocean/Sunset/Forest/Neon entries that no longer
            // exist), rather than throwing and crashing the whole preferences flow.
            prefs[THEME_PREFERENCE_KEY]?.let { stored ->
                runCatching { ThemePreference.valueOf(stored) }.getOrNull()
            } ?: ThemePreference.Auto
        }

    override suspend fun setGamePlayingBehavior(behavior: ScreenBehavior) {
        context.onboardingDataStore.edit { it[GAME_PLAYING_BEHAVIOR_KEY] = behavior.name }
    }

    override fun observeGamePlayingBehavior(): Flow<ScreenBehavior> =
        context.onboardingDataStore.data.map { prefs ->
            // Falls back to Nothing for both the unset case and any unrecognized stored
            // value - same reasoning as observeThemePreference.
            prefs[GAME_PLAYING_BEHAVIOR_KEY]?.let { stored ->
                runCatching { ScreenBehavior.valueOf(stored) }.getOrNull()
            } ?: ScreenBehavior.Nothing
        }

    override suspend fun setGamePlayingDimPercent(percent: Int) {
        context.onboardingDataStore.edit {
            it[GAME_PLAYING_DIM_PERCENT_KEY] = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)
        }
    }

    override fun observeGamePlayingDimPercent(): Flow<Int> =
        context.onboardingDataStore.data.map { it[GAME_PLAYING_DIM_PERCENT_KEY] ?: DEFAULT_DIM_PERCENT }

    override suspend fun setScreensaverBehavior(behavior: ScreenBehavior) {
        context.onboardingDataStore.edit { it[SCREENSAVER_BEHAVIOR_KEY] = behavior.name }
    }

    override fun observeScreensaverBehavior(): Flow<ScreenBehavior> =
        context.onboardingDataStore.data.map { prefs ->
            prefs[SCREENSAVER_BEHAVIOR_KEY]?.let { stored ->
                runCatching { ScreenBehavior.valueOf(stored) }.getOrNull()
            } ?: ScreenBehavior.Nothing
        }

    override suspend fun setScreensaverDimPercent(percent: Int) {
        context.onboardingDataStore.edit {
            it[SCREENSAVER_DIM_PERCENT_KEY] = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)
        }
    }

    override fun observeScreensaverDimPercent(): Flow<Int> =
        context.onboardingDataStore.data.map { it[SCREENSAVER_DIM_PERCENT_KEY] ?: DEFAULT_DIM_PERCENT }

    override suspend fun setMusicEnabled(enabled: Boolean) {
        context.onboardingDataStore.edit { it[MUSIC_ENABLED_KEY] = enabled }
    }

    override fun observeMusicEnabled(): Flow<Boolean> =
        context.onboardingDataStore.data.map {
            it[MUSIC_ENABLED_KEY] ?: false
        }

    override suspend fun setMusicPlayWhileBrowsingSystems(enabled: Boolean) {
        context.onboardingDataStore.edit { it[MUSIC_PLAY_WHILE_BROWSING_SYSTEMS_KEY] = enabled }
    }

    override fun observeMusicPlayWhileBrowsingSystems(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[MUSIC_PLAY_WHILE_BROWSING_SYSTEMS_KEY] ?: true }

    override suspend fun setMusicPlayWhileBrowsingGames(enabled: Boolean) {
        context.onboardingDataStore.edit { it[MUSIC_PLAY_WHILE_BROWSING_GAMES_KEY] = enabled }
    }

    override fun observeMusicPlayWhileBrowsingGames(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[MUSIC_PLAY_WHILE_BROWSING_GAMES_KEY] ?: true }

    override suspend fun setMusicPlayDuringScreensaver(enabled: Boolean) {
        context.onboardingDataStore.edit { it[MUSIC_PLAY_DURING_SCREENSAVER_KEY] = enabled }
    }

    override fun observeMusicPlayDuringScreensaver(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[MUSIC_PLAY_DURING_SCREENSAVER_KEY] ?: false }

    override suspend fun setMusicDuckingMode(mode: MusicDuckingMode) {
        context.onboardingDataStore.edit { it[MUSIC_DUCKING_MODE_KEY] = mode.name }
    }

    override fun observeMusicDuckingMode(): Flow<MusicDuckingMode> =
        context.onboardingDataStore.data.map { prefs ->
            // Falls back to LowerVolume for both the unset case and any unrecognized
            // stored value - same reasoning as observeThemePreference.
            prefs[MUSIC_DUCKING_MODE_KEY]?.let { stored ->
                runCatching { MusicDuckingMode.valueOf(stored) }.getOrNull()
            } ?: MusicDuckingMode.LowerVolume
        }

    override suspend fun setOverlayOpacityPercent(percent: Int) {
        context.onboardingDataStore.edit { it[OVERLAY_OPACITY_KEY] = percent.coerceIn(MIN_PERCENT, MAX_PERCENT) }
    }

    override fun observeOverlayOpacityPercent(): Flow<Int> =
        context.onboardingDataStore.data.map { it[OVERLAY_OPACITY_KEY] ?: DEFAULT_OVERLAY_OPACITY_PERCENT }

    override suspend fun saveCustomMusicFolderPath(path: String) {
        context.onboardingDataStore.edit { it[CUSTOM_MUSIC_FOLDER_PATH_KEY] = path }
    }

    override fun observeCustomMusicFolderPath(): Flow<String?> =
        context.onboardingDataStore.data.map {
            it[CUSTOM_MUSIC_FOLDER_PATH_KEY]
        }

    override suspend fun clearCustomMusicFolderPath() {
        context.onboardingDataStore.edit { it.remove(CUSTOM_MUSIC_FOLDER_PATH_KEY) }
    }

    override suspend fun setCloseCompanionOnQuitEnabled(enabled: Boolean) {
        context.onboardingDataStore.edit { it[CLOSE_COMPANION_ON_QUIT_ENABLED_KEY] = enabled }
    }

    override fun observeCloseCompanionOnQuitEnabled(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[CLOSE_COMPANION_ON_QUIT_ENABLED_KEY] ?: false }

    override suspend fun setFabAssignments(assignments: FabAssignments) {
        context.onboardingDataStore.edit {
            it.putFabSlot(FAB_TOP_START_KEY, FAB_TOP_START_CUSTOM_APP_KEY, assignments.topStart)
            it.putFabSlot(FAB_TOP_END_KEY, FAB_TOP_END_CUSTOM_APP_KEY, assignments.topEnd)
            it.putFabSlot(FAB_BOTTOM_START_KEY, FAB_BOTTOM_START_CUSTOM_APP_KEY, assignments.bottomStart)
            it.putFabSlot(FAB_BOTTOM_END_KEY, FAB_BOTTOM_END_CUSTOM_APP_KEY, assignments.bottomEnd)
        }
    }

    private fun MutablePreferences.putFabSlot(
        typeKey: Preferences.Key<String>,
        customAppKey: Preferences.Key<String>,
        slot: FabSlot,
    ) {
        this[typeKey] = slot.type.name
        if (slot.customAppPackageName != null) this[customAppKey] = slot.customAppPackageName else remove(customAppKey)
    }

    override fun observeFabAssignments(): Flow<FabAssignments> =
        combine(
            observeFabSlot(FAB_TOP_START_KEY, FAB_TOP_START_CUSTOM_APP_KEY, FabAssignments.Default.topStart),
            observeFabSlot(FAB_TOP_END_KEY, FAB_TOP_END_CUSTOM_APP_KEY, FabAssignments.Default.topEnd),
            observeFabSlot(FAB_BOTTOM_START_KEY, FAB_BOTTOM_START_CUSTOM_APP_KEY, FabAssignments.Default.bottomStart),
            observeFabSlot(FAB_BOTTOM_END_KEY, FAB_BOTTOM_END_CUSTOM_APP_KEY, FabAssignments.Default.bottomEnd),
        ) { topStart, topEnd, bottomStart, bottomEnd ->
            FabAssignments(topStart = topStart, topEnd = topEnd, bottomStart = bottomStart, bottomEnd = bottomEnd)
        }

    // Falls back to `default` for both the unset case and any unrecognized stored value -
    // same reasoning as observeThemePreference. customAppKey is only ever meaningful when
    // the resolved type is CustomApp, but is read unconditionally regardless - a leftover
    // value from a prior CustomApp selection, still present after switching to a different
    // type and back, is harmless since FabSlot itself only assigns it meaning in that case.
    private fun observeFabSlot(
        typeKey: Preferences.Key<String>,
        customAppKey: Preferences.Key<String>,
        default: FabSlot,
    ): Flow<FabSlot> =
        context.onboardingDataStore.data.map { prefs ->
            val stored = prefs[typeKey]
            val type = stored?.let { runCatching { FabType.valueOf(it) }.getOrNull() } ?: default.type
            FabSlot(type = type, customAppPackageName = prefs[customAppKey])
        }

    override suspend fun setLaunchEsdeOnStartEnabled(enabled: Boolean) {
        context.onboardingDataStore.edit { it[LAUNCH_ESDE_ON_START_ENABLED_KEY] = enabled }
    }

    override fun observeLaunchEsdeOnStartEnabled(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[LAUNCH_ESDE_ON_START_ENABLED_KEY] ?: false }

    override suspend fun setDebugLoggingEnabled(enabled: Boolean) {
        context.onboardingDataStore.edit { it[DEBUG_LOGGING_ENABLED_KEY] = enabled }
    }

    override fun observeDebugLoggingEnabled(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[DEBUG_LOGGING_ENABLED_KEY] ?: false }

    override suspend fun setUpdateAchievementsOnScreensaverEnabled(enabled: Boolean) {
        context.onboardingDataStore.edit { it[UPDATE_ACHIEVEMENTS_ON_SCREENSAVER_ENABLED_KEY] = enabled }
    }

    override fun observeUpdateAchievementsOnScreensaverEnabled(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[UPDATE_ACHIEVEMENTS_ON_SCREENSAVER_ENABLED_KEY] ?: true }

    override suspend fun setUpdateGameGuidesOnScreensaverEnabled(enabled: Boolean) {
        context.onboardingDataStore.edit { it[UPDATE_GAME_GUIDES_ON_SCREENSAVER_ENABLED_KEY] = enabled }
    }

    override fun observeUpdateGameGuidesOnScreensaverEnabled(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[UPDATE_GAME_GUIDES_ON_SCREENSAVER_ENABLED_KEY] ?: false }

    override suspend fun setPlaytimeStatsHardcoreModeEnabled(enabled: Boolean) {
        context.onboardingDataStore.edit { it[PLAYTIME_STATS_HARDCORE_MODE_ENABLED_KEY] = enabled }
    }

    override fun observePlaytimeStatsHardcoreModeEnabled(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[PLAYTIME_STATS_HARDCORE_MODE_ENABLED_KEY] ?: false }

    override suspend fun setBluetoothPermissionRequested(requested: Boolean) {
        context.onboardingDataStore.edit { it[BLUETOOTH_PERMISSION_REQUESTED_KEY] = requested }
    }

    override fun observeBluetoothPermissionRequested(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[BLUETOOTH_PERMISSION_REQUESTED_KEY] ?: false }

    private companion object {
        const val DEFAULT_ESDE_ROOT = "/storage/emulated/0/ES-DE"
        const val DEFAULT_OVERLAY_OPACITY_PERCENT = 80
        const val DEFAULT_DIM_PERCENT = 50

        val LOG_FOLDER_PATH_KEY = stringPreferencesKey("log_folder_path")
        val MEDIA_FOLDER_PATH_KEY = stringPreferencesKey("media_folder_path")
        val CUSTOM_SYSTEM_IMAGES_FOLDER_PATH_KEY = stringPreferencesKey("custom_system_images_folder_path")
        val CUSTOM_LOGOS_FOLDER_PATH_KEY = stringPreferencesKey("custom_logos_folder_path")
        val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
        val THEME_PREFERENCE_KEY = stringPreferencesKey("theme_preference")
        val GAME_PLAYING_BEHAVIOR_KEY = stringPreferencesKey("game_playing_behavior")
        val GAME_PLAYING_DIM_PERCENT_KEY = intPreferencesKey("game_playing_dim_percent")
        val SCREENSAVER_BEHAVIOR_KEY = stringPreferencesKey("screensaver_behavior")
        val SCREENSAVER_DIM_PERCENT_KEY = intPreferencesKey("screensaver_dim_percent")
        val MUSIC_ENABLED_KEY = booleanPreferencesKey("music_enabled")
        val MUSIC_PLAY_WHILE_BROWSING_SYSTEMS_KEY = booleanPreferencesKey("music_play_while_browsing_systems")
        val MUSIC_PLAY_WHILE_BROWSING_GAMES_KEY = booleanPreferencesKey("music_play_while_browsing_games")
        val MUSIC_PLAY_DURING_SCREENSAVER_KEY = booleanPreferencesKey("music_play_during_screensaver")
        val MUSIC_DUCKING_MODE_KEY = stringPreferencesKey("music_ducking_mode")
        val OVERLAY_OPACITY_KEY = intPreferencesKey("overlay_opacity_percent")
        val CUSTOM_MUSIC_FOLDER_PATH_KEY = stringPreferencesKey("custom_music_folder_path")
        val CLOSE_COMPANION_ON_QUIT_ENABLED_KEY = booleanPreferencesKey("close_companion_on_quit_enabled")
        val FAB_TOP_START_KEY = stringPreferencesKey("fab_top_start")
        val FAB_TOP_END_KEY = stringPreferencesKey("fab_top_end")
        val FAB_BOTTOM_START_KEY = stringPreferencesKey("fab_bottom_start")
        val FAB_BOTTOM_END_KEY = stringPreferencesKey("fab_bottom_end")
        val FAB_TOP_START_CUSTOM_APP_KEY = stringPreferencesKey("fab_top_start_custom_app")
        val FAB_TOP_END_CUSTOM_APP_KEY = stringPreferencesKey("fab_top_end_custom_app")
        val FAB_BOTTOM_START_CUSTOM_APP_KEY = stringPreferencesKey("fab_bottom_start_custom_app")
        val FAB_BOTTOM_END_CUSTOM_APP_KEY = stringPreferencesKey("fab_bottom_end_custom_app")
        val LAUNCH_ESDE_ON_START_ENABLED_KEY = booleanPreferencesKey("launch_esde_on_start_enabled")
        val DEBUG_LOGGING_ENABLED_KEY = booleanPreferencesKey("debug_logging_enabled")
        val UPDATE_ACHIEVEMENTS_ON_SCREENSAVER_ENABLED_KEY =
            booleanPreferencesKey("update_achievements_on_screensaver_enabled")
        val UPDATE_GAME_GUIDES_ON_SCREENSAVER_ENABLED_KEY =
            booleanPreferencesKey("update_game_guides_on_screensaver_enabled")
        val PLAYTIME_STATS_HARDCORE_MODE_ENABLED_KEY = booleanPreferencesKey("playtime_stats_hardcore_mode_enabled")
        val BLUETOOTH_PERMISSION_REQUESTED_KEY = booleanPreferencesKey("bluetooth_permission_requested")
    }
}
