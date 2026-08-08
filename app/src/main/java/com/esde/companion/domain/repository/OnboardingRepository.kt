package com.esde.companion.domain.repository

import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
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
     * Settings > Video Playback: whether game videos auto-play while AppState is
     * BrowsingGame. Defaults to false - opt-in, same as other non-default display
     * behaviors.
     */
    suspend fun setVideoPlaybackEnabled(enabled: Boolean)

    fun observeVideoPlaybackEnabled(): Flow<Boolean>

    /** Delay in seconds before playback starts once a video becomes eligible to play. */
    suspend fun setVideoDelaySeconds(seconds: Int)

    fun observeVideoDelaySeconds(): Flow<Int>

    /** Whether video audio is audible; false mutes playback entirely. Defaults to true. */
    suspend fun setVideoAudioEnabled(enabled: Boolean)

    fun observeVideoAudioEnabled(): Flow<Boolean>

    /** Master toggle for background music. Defaults to true. */
    suspend fun setMusicEnabled(enabled: Boolean)

    fun observeMusicEnabled(): Flow<Boolean>

    /** Whether music plays while AppState is BrowsingSystem. Defaults to true. */
    suspend fun setMusicPlayWhileBrowsingSystems(enabled: Boolean)

    fun observeMusicPlayWhileBrowsingSystems(): Flow<Boolean>

    /** Whether music plays while AppState is BrowsingGame. Defaults to true. */
    suspend fun setMusicPlayWhileBrowsingGames(enabled: Boolean)

    fun observeMusicPlayWhileBrowsingGames(): Flow<Boolean>

    /** Whether music plays while AppState is Screensaver. Defaults to true. */
    suspend fun setMusicPlayDuringScreensaver(enabled: Boolean)

    fun observeMusicPlayDuringScreensaver(): Flow<Boolean>

    /**
     * How background music reacts while a game video is audibly playing. Defaults to
     * [MusicDuckingMode.LowerVolume].
     */
    suspend fun setMusicDuckingMode(mode: MusicDuckingMode)

    fun observeMusicDuckingMode(): Flow<MusicDuckingMode>

    /**
     * Settings > UI Settings > Overlay Opacity: shared background opacity for every
     * translucent overlay surface - the App Drawer, the App Dock, the music FAB's expanded
     * track/controls panel, and the Settings/music-FAB/Edit-Widgets corner buttons. One
     * master value rather than a separate slider per surface, 0-100, standard convention:
     * 0 = fully transparent, 100 = fully opaque. Defaults to 80.
     */
    suspend fun setOverlayOpacityPercent(percent: Int)

    fun observeOverlayOpacityPercent(): Flow<Int>

    suspend fun saveCustomMusicFolderPath(path: String)

    fun observeCustomMusicFolderPath(): Flow<String?>

    suspend fun clearCustomMusicFolderPath()

    /**
     * Settings > Other Settings: whether ES-DE Companion should close itself when ES-DE
     * fires a quit event. Defaults to false - opt-in, since this app is normally expected
     * to keep running continuously on the secondary display regardless of ES-DE's state.
     */
    suspend fun setCloseCompanionOnQuitEnabled(enabled: Boolean)

    fun observeCloseCompanionOnQuitEnabled(): Flow<Boolean>

    /**
     * Settings > Other Settings: whether the Settings gear (CornerFab) is shown on the
     * main screen. Defaults to true - this replaces what was previously always-on
     * behavior. Settings stays reachable regardless of this setting via the main
     * screen's long-press menu.
     */
    suspend fun setSettingsFabVisible(visible: Boolean)

    fun observeSettingsFabVisible(): Flow<Boolean>

    /**
     * Settings > Other Settings: whether ES-DE Companion should launch ES-DE itself, on
     * the other display, as soon as Companion starts up. Defaults to false - opt-in, same
     * reasoning as [setCloseCompanionOnQuitEnabled].
     */
    suspend fun setLaunchEsdeOnStartEnabled(enabled: Boolean)

    fun observeLaunchEsdeOnStartEnabled(): Flow<Boolean>

    /**
     * Settings > Other Settings: whether ES-DE Companion writes a debug log (parsed
     * events, state transitions, media resolution outcomes) to help diagnose reported
     * issues. Defaults to false - opt-in, since it's a diagnostic aid rather than a
     * normal-use feature.
     */
    suspend fun setDebugLoggingEnabled(enabled: Boolean)

    fun observeDebugLoggingEnabled(): Flow<Boolean>
}
