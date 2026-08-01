package com.esde.companion.domain.repository

import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.model.ScreenBehavior
import kotlinx.coroutines.flow.Flow

/**
 * Everything onboarding/settings needs: default path suggestions, folder validation,
 * and persistence of the user's chosen paths. How paths are actually persisted (DataStore,
 * in this app) and how folders are checked (plain File I/O) is entirely a data-layer
 * implementation detail.
 *
 * Re-enterable by design: the save/observe methods are not "first run only" - a Settings
 * screen reuses these same methods to change folders later.
 */
interface OnboardingRepository {

    fun defaultLogFolderPath(): String
    fun defaultMediaFolderPath(): String

    suspend fun validateLogFolder(path: String): LogFolderValidation
    suspend fun validateMediaFolder(path: String): MediaFolderValidation

    suspend fun saveLogFolderPath(path: String)
    suspend fun saveMediaFolderPath(path: String)

    fun observeLogFolderPath(): Flow<String?>
    fun observeMediaFolderPath(): Flow<String?>

    suspend fun saveCustomSystemImagesFolderPath(path: String)
    fun observeCustomSystemImagesFolderPath(): Flow<String?>
    suspend fun clearCustomSystemImagesFolderPath()

    suspend fun saveCustomLogosFolderPath(path: String)
    fun observeCustomLogosFolderPath(): Flow<String?>
    suspend fun clearCustomLogosFolderPath()

    suspend fun markOnboardingComplete()
    fun observeOnboardingComplete(): Flow<Boolean>

    /**
     * Whether the on-screen state overlay (AppState/args/cover-image info drawn on top
     * of the main screen) is shown. Defaults to true when never explicitly set - this is
     * currently the only content the main screen has, so it should be visible out of the
     * box. Purely a display preference; has no bearing on the state pipeline itself.
     */
    suspend fun setOverlayEnabled(enabled: Boolean)
    fun observeOverlayEnabled(): Flow<Boolean>

    /**
     * The user's selected color theme. Defaults to [ThemePreference.Default] when never
     * explicitly set.
     */
    suspend fun setThemePreference(preference: ThemePreference)
    fun observeThemePreference(): Flow<ThemePreference>

    /**
     * How the main screen should react while a game is actively being played
     * (AppState.PlayingGame). Defaults to [ScreenBehavior.Nothing].
     */
    suspend fun setGamePlayingBehavior(behavior: ScreenBehavior)
    fun observeGamePlayingBehavior(): Flow<ScreenBehavior>

    /**
     * How the main screen should react while the ES-DE screensaver is active
     * (AppState.Screensaver). Defaults to [ScreenBehavior.Nothing].
     */
    suspend fun setScreensaverBehavior(behavior: ScreenBehavior)
    fun observeScreensaverBehavior(): Flow<ScreenBehavior>

    /**
     * Settings > UI Settings > Video Playback: whether game videos auto-play while
     * AppState is BrowsingGame. Defaults to false - opt-in, same as other non-default
     * display behaviors.
     */
    suspend fun setVideoPlaybackEnabled(enabled: Boolean)
    fun observeVideoPlaybackEnabled(): Flow<Boolean>

    /** Delay in seconds before playback starts once a video becomes eligible to play. */
    suspend fun setVideoDelaySeconds(seconds: Int)
    fun observeVideoDelaySeconds(): Flow<Int>

    /** Whether video audio is audible; false mutes playback entirely. Defaults to true. */
    suspend fun setVideoAudioEnabled(enabled: Boolean)
    fun observeVideoAudioEnabled(): Flow<Boolean>
}