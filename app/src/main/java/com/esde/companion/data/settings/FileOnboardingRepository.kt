package com.esde.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.model.VideoAspectRatioMode
import com.esde.companion.domain.repository.OnboardingRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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
                val logFile = File(folder, "logs/es_log.txt")
                LogFolderValidation.FolderFound(logFileFound = logFile.isFile)
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

    override fun observeLogFolderPath(): Flow<String?> =
        context.onboardingDataStore.data.map { it[LOG_FOLDER_PATH_KEY] }

    override fun observeMediaFolderPath(): Flow<String?> =
        context.onboardingDataStore.data.map { it[MEDIA_FOLDER_PATH_KEY] }

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
        context.onboardingDataStore.data.map { it[CUSTOM_LOGOS_FOLDER_PATH_KEY] }

    override suspend fun clearCustomLogosFolderPath() {
        context.onboardingDataStore.edit { it.remove(CUSTOM_LOGOS_FOLDER_PATH_KEY) }
    }

    override suspend fun markOnboardingComplete() {
        context.onboardingDataStore.edit { it[ONBOARDING_COMPLETE_KEY] = true }
    }

    override fun observeOnboardingComplete(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[ONBOARDING_COMPLETE_KEY] ?: false }

    override suspend fun setOverlayEnabled(enabled: Boolean) {
        context.onboardingDataStore.edit { it[OVERLAY_ENABLED_KEY] = enabled }
    }

    override fun observeOverlayEnabled(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[OVERLAY_ENABLED_KEY] ?: true }

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

    override suspend fun setScreensaverBehavior(behavior: ScreenBehavior) {
        context.onboardingDataStore.edit { it[SCREENSAVER_BEHAVIOR_KEY] = behavior.name }
    }

    override fun observeScreensaverBehavior(): Flow<ScreenBehavior> =
        context.onboardingDataStore.data.map { prefs ->
            prefs[SCREENSAVER_BEHAVIOR_KEY]?.let { stored ->
                runCatching { ScreenBehavior.valueOf(stored) }.getOrNull()
            } ?: ScreenBehavior.Nothing
        }

    override suspend fun setVideoPlaybackEnabled(enabled: Boolean) {
        context.onboardingDataStore.edit { it[VIDEO_PLAYBACK_ENABLED_KEY] = enabled }
    }

    override fun observeVideoPlaybackEnabled(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[VIDEO_PLAYBACK_ENABLED_KEY] ?: false }

    override suspend fun setVideoDelaySeconds(seconds: Int) {
        context.onboardingDataStore.edit { it[VIDEO_DELAY_SECONDS_KEY] = seconds.coerceIn(0, MAX_VIDEO_DELAY_SECONDS) }
    }

    override fun observeVideoDelaySeconds(): Flow<Int> =
        context.onboardingDataStore.data.map { it[VIDEO_DELAY_SECONDS_KEY] ?: 0 }

    override suspend fun setVideoAudioEnabled(enabled: Boolean) {
        context.onboardingDataStore.edit { it[VIDEO_AUDIO_ENABLED_KEY] = enabled }
    }

    override fun observeVideoAudioEnabled(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[VIDEO_AUDIO_ENABLED_KEY] ?: true }

    override suspend fun setVideoAspectRatioMode(mode: VideoAspectRatioMode) {
        context.onboardingDataStore.edit { it[VIDEO_ASPECT_RATIO_MODE_KEY] = mode.name }
    }

    override fun observeVideoAspectRatioMode(): Flow<VideoAspectRatioMode> =
        context.onboardingDataStore.data.map { prefs ->
            // Falls back to Cover for both the unset case and any unrecognized stored
            // value - same reasoning as observeThemePreference.
            prefs[VIDEO_ASPECT_RATIO_MODE_KEY]?.let { stored ->
                runCatching { VideoAspectRatioMode.valueOf(stored) }.getOrNull()
            } ?: VideoAspectRatioMode.Cover
        }

    private companion object {
        const val DEFAULT_ESDE_ROOT = "/storage/emulated/0/ES-DE"
        const val MAX_VIDEO_DELAY_SECONDS = 10

        val LOG_FOLDER_PATH_KEY = stringPreferencesKey("log_folder_path")
        val MEDIA_FOLDER_PATH_KEY = stringPreferencesKey("media_folder_path")
        val CUSTOM_SYSTEM_IMAGES_FOLDER_PATH_KEY = stringPreferencesKey("custom_system_images_folder_path")
        val CUSTOM_LOGOS_FOLDER_PATH_KEY = stringPreferencesKey("custom_logos_folder_path")
        val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
        val OVERLAY_ENABLED_KEY = booleanPreferencesKey("overlay_enabled")
        val THEME_PREFERENCE_KEY = stringPreferencesKey("theme_preference")
        val GAME_PLAYING_BEHAVIOR_KEY = stringPreferencesKey("game_playing_behavior")
        val SCREENSAVER_BEHAVIOR_KEY = stringPreferencesKey("screensaver_behavior")
        val VIDEO_PLAYBACK_ENABLED_KEY = booleanPreferencesKey("video_playback_enabled")
        val VIDEO_DELAY_SECONDS_KEY = intPreferencesKey("video_delay_seconds")
        val VIDEO_AUDIO_ENABLED_KEY = booleanPreferencesKey("video_audio_enabled")
        val VIDEO_ASPECT_RATIO_MODE_KEY = stringPreferencesKey("video_aspect_ratio_mode")
    }
}