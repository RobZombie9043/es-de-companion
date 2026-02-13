package com.esde.companion

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.Environment
import android.os.FileObserver
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.view.animation.DecelerateInterpolator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import android.app.ActivityOptions
import android.provider.Settings
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import java.io.File
import kotlin.math.abs
import androidx.media3.ui.PlayerView
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.esde.companion.managers.MusicManager
import com.esde.companion.data.AppConstants
import com.esde.companion.data.AppState
import com.esde.companion.data.Widget
import com.esde.companion.data.SavedBrowsingState
import com.esde.companion.data.ScreensaverGame
import com.esde.companion.data.getCurrentGameFilename
import com.esde.companion.data.getCurrentSystemName
import com.esde.companion.managers.AppLaunchManager
import com.esde.companion.managers.ImageManager
import com.esde.companion.managers.MediaManager
import com.esde.companion.managers.PreferencesManager
import com.esde.companion.managers.ScriptManager
import com.esde.companion.managers.VideoManager
import com.esde.companion.managers.WidgetCreationManager
import com.esde.companion.managers.WidgetManager
import com.esde.companion.ui.AppAdapter
import com.esde.companion.ui.GridOverlayView
import com.esde.companion.ui.ResizableWidgetContainer
import com.esde.companion.ui.WidgetView

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: RelativeLayout
    private lateinit var gameImageView: ImageView
    private lateinit var dimmingOverlay: View
    private lateinit var appDrawer: View
    private lateinit var appRecyclerView: RecyclerView
    private lateinit var appSearchBar: EditText
    private lateinit var searchClearButton: ImageButton
    private lateinit var drawerBackButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var androidSettingsButton: ImageButton
    private lateinit var prefsManager: PreferencesManager
    private lateinit var appLaunchPrefs: AppLaunchManager
    private lateinit var mediaManager: MediaManager
    private lateinit var imageManager: ImageManager
    private lateinit var videoManager: VideoManager
    lateinit var widgetCreationManager: WidgetCreationManager

    private lateinit var songTitleOverlay: LinearLayout
    private lateinit var songTitleText: TextView
    private lateinit var musicPlayPauseButton: ImageButton
    private lateinit var musicNextButton: ImageButton
    private var songTitleHandler: Handler? = null
    private var songTitleRunnable: Runnable? = null
    private lateinit var musicManager: MusicManager
    // Track if we're navigating to another activity in our app (Settings, SystemSelection)
    private var isNavigatingInternally = false

    private lateinit var blackOverlay: View
    private var isBlackOverlayShown = false

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var gestureDetector: GestureDetectorCompat

    // Widget system
    private lateinit var widgetContainer: ResizableWidgetContainer
    private lateinit var widgetManager: WidgetManager
    private var gridOverlayView: GridOverlayView? = null
    private val activeWidgets = mutableListOf<WidgetView>()
    private var widgetsLocked = false
    private var snapToGrid = false
    private val gridSize = AppConstants.UI.GRID_SIZE
    private var showGrid = false
    private var isInteractingWithWidget = false
    private var longPressHandler: Handler? = null
    private var longPressRunnable: Runnable? = null
    private var longPressTriggered = false
    private var touchDownX = 0f
    private var touchDownY = 0f
    private val LONG_PRESS_TIMEOUT by lazy {
        ViewConfiguration.getLongPressTimeout().toLong()
    }
    private var widgetMenuShowing = false
    private var widgetMenuDialog: android.app.AlertDialog? = null

    // This tracks state alongside existing booleans during migration
    private var state: AppState = AppState.SystemBrowsing("")
        set(value) {
            val oldState = field
            field = value

            // Log state changes for debugging
            android.util.Log.d("MainActivity", "━━━ STATE CHANGE ━━━")
            android.util.Log.d("MainActivity", "FROM: $oldState")
            android.util.Log.d("MainActivity", "TO:   $value")
            android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━")

            musicManager.onStateChanged(value)
        }

    private var fileObserver: FileObserver? = null
    private var allApps = listOf<ResolveInfo>()  // Store all apps for search filtering
    private var hasWindowFocus = true  // Track if app has window focus (is on top)
    // Note: hasWindowFocus is window-level state, not app state
    private var isLaunchingFromScreensaver = false  // Track if we're launching game from screensaver
    private var screensaverInitialized = false  // Track if screensaver has loaded its first game

    // Video playback variables
    private lateinit var videoView: PlayerView
    private var volumeChangeReceiver: BroadcastReceiver? = null
    private var isActivityVisible = true  // Track onStart/onStop - most reliable signal

    // Flag to skip reload in onResume (used when returning from settings with no changes)
    private var skipNextReload = false

    // Double-tap detection variables
    private var tapCount = 0
    private var lastTapTime = 0L
    // Two-finger tap detection for music controls
    private var twoFingerTapCount = 0
    private var lastTwoFingerTapTime = 0L
    private val TWO_FINGER_TAP_TIMEOUT by lazy {
        ViewConfiguration.getDoubleTapTimeout().toLong()  // Standard Android double-tap timeout (~300ms)
    }
    // Standard Android double-tap timeout (max time between taps)
    private val DOUBLE_TAP_TIMEOUT by lazy {
        ViewConfiguration.getDoubleTapTimeout().toLong() // Default: 300ms
    }
    // Custom minimum interval to prevent accidental activations (100ms)
    // This is intentionally higher than Android's internal 40ms hardware filter:
    // - 40ms filters touch controller artifacts (hardware-level)
    // - 100ms filters user errors like screen brushing (UX-level)
    // Still imperceptible to users while significantly reducing false positives
    private val MIN_TAP_INTERVAL = AppConstants.Timing.DOUBLE_TAP_MIN_INTERVAL

    // Scripts verification
    private var isWaitingForScriptVerification = false
    private var scriptVerificationHandler: Handler? = null
    private var scriptVerificationRunnable: Runnable? = null
    private var currentVerificationDialog: AlertDialog? = null
    private var currentErrorDialog: AlertDialog? = null
    private val SCRIPT_VERIFICATION_TIMEOUT = AppConstants.Timing.SCRIPT_VERIFICATION_TIMEOUT

    // Dynamic debouncing for fast scrolling - separate tracking for systems and games
    private val imageLoadHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var imageLoadRunnable: Runnable? = null
    private var lastSystemScrollTime = 0L
    private var lastGameScrollTime = 0L
    private var gameInfoJob: kotlinx.coroutines.Job? = null

    // System scrolling: Enable debouncing to reduce rapid updates
    private val SYSTEM_FAST_SCROLL_THRESHOLD = AppConstants.Timing.SYSTEM_FAST_SCROLL_THRESHOLD
    private val SYSTEM_FAST_SCROLL_DELAY = AppConstants.Timing.SYSTEM_FAST_SCROLL_DELAY
    private val SYSTEM_SLOW_SCROLL_DELAY = AppConstants.Timing.SYSTEM_SLOW_SCROLL_DELAY

    // Game scrolling: No debouncing for instant response
    private val GAME_FAST_SCROLL_THRESHOLD = AppConstants.Timing.GAME_FAST_SCROLL_THRESHOLD
    private val GAME_FAST_SCROLL_DELAY = AppConstants.Timing.GAME_FAST_SCROLL_DELAY
    private val GAME_SLOW_SCROLL_DELAY = AppConstants.Timing.GAME_SLOW_SCROLL_DELAY

    // Filter out game-select on game-start and game-end
    // NOTE: These are runtime state (not constants), so they stay as regular variables
    private var lastGameStartTime = 0L
    private var lastGameEndTime = 0L

    private val GAME_EVENT_DEBOUNCE = AppConstants.Timing.GAME_EVENT_DEBOUNCE

    // Broadcast receiver for app install/uninstall events
    private val appChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_REPLACED -> {
                    // Refresh app drawer when apps are installed/uninstalled
                    setupAppDrawer()
                }
            }
        }
    }

    // Settings launcher to handle when visual settings change
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val showWidgetTutorial =
                result.data?.getBooleanExtra("SHOW_WIDGET_TUTORIAL", false) ?: false
            if (showWidgetTutorial) {
                // Delay slightly to let UI settle after settings closes
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showWidgetSystemTutorial(fromUpdate = false)
                }, AppConstants.Timing.WIZARD_DELAY)
            }
            val needsRecreate = result.data?.getBooleanExtra("NEEDS_RECREATE", false) ?: false
            val appsHiddenChanged =
                result.data?.getBooleanExtra("APPS_HIDDEN_CHANGED", false) ?: false
            val musicSettingsChanged = result.data?.getBooleanExtra("MUSIC_SETTINGS_CHANGED", false) ?: false
            val musicMasterToggleChanged = result.data?.getBooleanExtra("MUSIC_MASTER_TOGGLE_CHANGED", false) ?: false
            val closeDrawer = result.data?.getBooleanExtra("CLOSE_DRAWER", false) ?: false
            val videoSettingsChanged =
                result.data?.getBooleanExtra("VIDEO_SETTINGS_CHANGED", false) ?: false
            val logoSizeChanged = result.data?.getBooleanExtra("LOGO_SIZE_CHANGED", false) ?: false
            val mediaPathChanged =
                result.data?.getBooleanExtra("MEDIA_PATH_CHANGED", false) ?: false
            val imagePreferenceChanged =
                result.data?.getBooleanExtra("IMAGE_PREFERENCE_CHANGED", false) ?: false
            val logoTogglesChanged =
                result.data?.getBooleanExtra("LOGO_TOGGLES_CHANGED", false) ?: false
            val gameLaunchBehaviorChanged =
                result.data?.getBooleanExtra("GAME_LAUNCH_BEHAVIOR_CHANGED", false) ?: false
            val screensaverBehaviorChanged =
                result.data?.getBooleanExtra("SCREENSAVER_BEHAVIOR_CHANGED", false) ?: false
            val startVerification =
                result.data?.getBooleanExtra("START_SCRIPT_VERIFICATION", false) ?: false
            val customBackgroundChanged =
                result.data?.getBooleanExtra("CUSTOM_BACKGROUND_CHANGED", false) ?: false

            // Close drawer if requested (before recreate to avoid visual glitch)
            if (closeDrawer && ::bottomSheetBehavior.isInitialized) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }

            if (needsRecreate) {
                // Recreate the activity to apply visual changes (dimming, blur, drawer transparency)
                recreate()
            } else if (appsHiddenChanged) {
                // Refresh app drawer to apply hidden apps changes
                setupAppDrawer()
            } else if (gameLaunchBehaviorChanged && state is AppState.GamePlaying) {
                // Game launch behavior changed while game is playing - update display
                handleGameStart()
                // Skip reload in onResume to prevent override
                skipNextReload = true
            } else if (screensaverBehaviorChanged && state is AppState.Screensaver) {
                // Screensaver behavior changed while screensaver is active - update display
                applyScreensaverBehaviorChange()
                // Skip reload in onResume to prevent override
                skipNextReload = true
            } else if (imagePreferenceChanged) {
                // Image preference changed - reload appropriate view
                if (state is AppState.GamePlaying) {
                    // Game is playing - update game launch display
                    android.util.Log.d("MainActivity", "Image preference changed during gameplay - reloading display")
                    handleGameStart()
                    skipNextReload = true
                } else if (state is AppState.SystemBrowsing) {
                    // In system view - reload system image with new preference
                    android.util.Log.d("MainActivity", "Image preference changed in system view - reloading system image")
                    loadSystemImage()
                    skipNextReload = true
                } else {
                    // In game browsing view - reload game image with new preference
                    android.util.Log.d("MainActivity", "Image preference changed in game view - reloading game image")
                    loadGameInfo()
                    skipNextReload = true
                }
            } else if (customBackgroundChanged) {
                // Custom background changed - reload to apply changes
                if (state is AppState.SystemBrowsing) {
                    loadSystemImage()
                } else if (state !is AppState.GamePlaying) {
                    // Only reload if not playing - if playing, customBackgroundChanged won't affect display
                    loadGameInfo()
                } else {
                    // Game is playing - skip reload since game launch behavior controls display
                    skipNextReload = true
                }
            } else if (videoSettingsChanged || logoSizeChanged || mediaPathChanged || logoTogglesChanged) {
                // Settings that affect displayed content changed - reload to apply changes
                if (state is AppState.SystemBrowsing) {
                    loadSystemImage()
                } else if (state !is AppState.GamePlaying) {
                    // Only reload if not playing - if playing, these settings don't affect game launch display
                    loadGameInfo()
                } else {
                    // Game is playing - skip reload
                    skipNextReload = true
                }
            } else if (musicMasterToggleChanged) {
                // Music MASTER TOGGLE changed - re-evaluate for both ON and OFF
                val musicEnabled = prefsManager.musicEnabled

                if (!musicEnabled) {
                    // Music was turned OFF - stop it
                    android.util.Log.d("MainActivity", "Music master toggle changed to OFF - stopping music")
                    hideSongTitle()
                    musicManager.onStateChanged(state)
                } else {
                    // Music was turned ON - evaluate if it should play for current state
                    android.util.Log.d("MainActivity", "Music master toggle changed to ON - evaluating music state")
                    musicManager.onStateChanged(state)
                }

                // CRITICAL FIX: If in GameBrowsing, reload to ensure UI is correct
                // (video might be playing and needs to be stopped, widgets hidden, etc.)
                if (state is AppState.GameBrowsing) {
                    android.util.Log.d("MainActivity", "Music master toggle changed in GameBrowsing - reloading game display")
                    loadGameInfo()
                    skipNextReload = true
                } else {
                    skipNextReload = true
                }
            } else if (musicSettingsChanged) {
                // Other music settings changed (not master toggle) - re-evaluate music for current state
                android.util.Log.d("MainActivity", "Music settings changed (not master) - re-evaluating music state")
                val songTitleEnabled = prefsManager.musicSongTitleEnabled

                // Check if song title display was toggled off
                if (!songTitleEnabled) {
                    hideSongTitle()
                }

                // Re-evaluate music for current state with new settings
                // This will start/stop music based on the new per-state toggles
                musicManager.onStateChanged(state)

                // CRITICAL FIX: If in GameBrowsing, reload to ensure UI is correct
                // (video might be playing and needs to be stopped, widgets hidden, etc.)
                if (state is AppState.GameBrowsing) {
                    android.util.Log.d("MainActivity", "Music settings changed in GameBrowsing - reloading game display")
                    loadGameInfo()
                    skipNextReload = true
                } else {
                    skipNextReload = true
                }
            } else {
                // No settings changed that require reload
                // However, if we're in GameBrowsing state, we should still reload
                // to ensure video plays properly when returning from Settings
                if (state is AppState.GameBrowsing) {
                    android.util.Log.d("MainActivity", "No settings changed but in GameBrowsing - allowing reload for video")
                    skipNextReload = false
                } else {
                    android.util.Log.d("MainActivity", "No settings changed - skipping reload")
                    skipNextReload = true
                }
            }
            // Note: Video audio changes are handled automatically in onResume

            // Start script verification if requested
            if (startVerification) {
                // Delay slightly to let UI settle
                Handler(Looper.getMainLooper()).postDelayed({
                    startScriptVerification()
                }, AppConstants.Timing.WIZARD_DELAY)
            }
        }
    }

    /**
     * Set up music manager callbacks.
     */
    private fun setupMusicCallbacks() {
        musicManager.setOnSongChangedListener { songName ->
            showSongTitle(songName)
        }

        musicManager.setOnMusicStoppedListener {
            hideSongTitle()
            updateMusicControls()
        }

        musicManager.setOnPlaybackStateChangedListener { isPlaying ->
            updateMusicControls()
        }
    }

    /**
     * Update music controls visibility and state based on MusicManager.
     */
    private fun updateMusicControls() {
        val hasActiveMusic = musicManager.isPlaying() || musicManager.isPaused()

        if (hasActiveMusic) {
            musicPlayPauseButton.setImageResource(
                if (musicManager.isPlaying()) R.drawable.ic_pause
                else R.drawable.ic_play
            )
        }

        android.util.Log.d("MainActivity", "Music controls updated: hasActiveMusic=$hasActiveMusic, isPlaying=${musicManager.isPlaying()}")
    }

    /**
     * Show song title overlay with current settings.
     */
    private fun showSongTitle(songName: String) {
        // Check if feature is enabled
        if (!prefsManager.musicSongTitleEnabled) {
            android.util.Log.d("MainActivity", "Song title display disabled in settings")
            return
        }

        // Update text
        songTitleText.text = songName

        // Show overlay with timeout
        showSongTitleOverlay()
    }

    /**
     * Show the song title overlay with configured timeout.
     */
    private fun showSongTitleOverlay() {
        // Cancel any pending hide
        songTitleRunnable?.let { songTitleHandler?.removeCallbacks(it) }

        // Apply background opacity setting
        val opacity = prefsManager.musicSongTitleOpacity
        val alpha = (opacity * 255 / 100).coerceIn(0, 255)
        val hexAlpha = String.format("%02x", alpha)
        val backgroundColor = android.graphics.Color.parseColor("#${hexAlpha}000000")
        songTitleOverlay.setBackgroundColor(backgroundColor)

        // Fade in
        songTitleOverlay.visibility = View.VISIBLE
        songTitleOverlay.animate()
            .alpha(1.0f)
            .setDuration(AppConstants.Timing.FADE_ANIMATION_DURATION)
            .start()

        // Get display duration
        val durationSetting = prefsManager.musicSongTitleDuration

        // If infinite (15), don't schedule fade out
        if (durationSetting == 15) {
            android.util.Log.d("MainActivity", "Song title set to infinite display")
            return
        }

        // Calculate duration: 0->2s, 1->4s, 2->6s, ... 14->30s
        val displayDuration = ((durationSetting + 1) * AppConstants.Timing.SONG_TITLE_STEP_SECONDS) * 1000L

        // Schedule fade out
        songTitleRunnable = Runnable {
            hideSongTitleOverlay()
        }
        songTitleHandler?.postDelayed(songTitleRunnable!!, displayDuration)

        android.util.Log.d("MainActivity", "Song title will auto-hide after ${displayDuration}ms")
    }

    /**
     * Hide the song title overlay with fade animation.
     */
    private fun hideSongTitleOverlay() {
        songTitleRunnable?.let { songTitleHandler?.removeCallbacks(it) }

        songTitleOverlay.animate()
            .alpha(0.0f)
            .setDuration(AppConstants.Timing.FADE_ANIMATION_DURATION)
            .withEndAction {
                songTitleOverlay.visibility = View.GONE
            }
            .start()

        android.util.Log.d("MainActivity", "Song title overlay hidden")
    }

    private fun updateDimmingOverlay() {
        val dimmingPercent = prefsManager.dimmingLevel

        // Convert percentage (0-100) to hex alpha (00-FF)
        val alpha = (dimmingPercent * 255 / 100).coerceIn(0, 255)
        val hexAlpha = String.format("%02x", alpha)
        val colorString = "#${hexAlpha}000000"

        val color = android.graphics.Color.parseColor(colorString)
        dimmingOverlay.setBackgroundColor(color)

        // Force the view to redraw
        dimmingOverlay.invalidate()
        dimmingOverlay.requestLayout()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        enableImmersiveMode()

        prefsManager = PreferencesManager(this)
        appLaunchPrefs = AppLaunchManager(this)
        mediaManager = MediaManager(prefsManager)
        imageManager = ImageManager(this, prefsManager)
        widgetCreationManager = WidgetCreationManager(this)

        // Initialize VideoManager (must be after videoView is initialized)
        // Note: videoView initialization happens in findViewById calls below

        // Initialize MusicManager
        musicManager = MusicManager(prefsManager)
        setupMusicCallbacks()

        // Check if we should show widget tutorial for updating users
        // Skip if setup not completed - will show after verification instead
        if (prefsManager.setupCompleted) {
            checkAndShowWidgetTutorialForUpdate()
        }

        rootLayout = findViewById(R.id.rootLayout)
        gameImageView = findViewById(R.id.gameImageView)
        dimmingOverlay = findViewById(R.id.dimmingOverlay)
        appDrawer = findViewById(R.id.appDrawer)
        appRecyclerView = findViewById(R.id.appRecyclerView)
        appSearchBar = findViewById(R.id.appSearchBar)
        searchClearButton = findViewById(R.id.searchClearButton)
        drawerBackButton = findViewById(R.id.drawerBackButton)
        settingsButton = findViewById(R.id.settingsButton)
        androidSettingsButton = findViewById(R.id.androidSettingsButton)
        videoView = findViewById(R.id.videoView)
        blackOverlay = findViewById(R.id.blackOverlay)
        // Initialize VideoManager now that videoView is ready
        videoManager = VideoManager(this, prefsManager, mediaManager, videoView)
        // Music UI components
        songTitleText = findViewById(R.id.songTitleText)
        songTitleOverlay = findViewById(R.id.songTitleOverlay)
        musicPlayPauseButton = findViewById(R.id.musicPlayPauseButton)
        musicNextButton = findViewById(R.id.musicNextButton)
        songTitleHandler = Handler(Looper.getMainLooper())

        // Initialize widget system
        widgetContainer = findViewById(R.id.widgetContainer)
        widgetManager = WidgetManager(this)
        // Load lock state
        widgetsLocked = prefsManager.widgetsLocked
        // Load snap to grid state
        snapToGrid = prefsManager.snapToGrid
        // Load show grid state
        showGrid = prefsManager.showGrid

        // Set initial position off-screen (above the top)
        val displayHeight = resources.displayMetrics.heightPixels.toFloat()
        blackOverlay.translationY = -displayHeight

        // Log display information at startup
        logDisplayInfo()

        setupAppDrawer()
        setupSearchBar()
        setupGestureDetector()  // Must be after setupAppDrawer so bottomSheetBehavior is initialized
        setupDrawerBackButton()
        setupSettingsButton()
        setupAndroidSettingsButton()
        setupMusicControlButtons()

        // Apply drawer transparency
        updateDrawerTransparency()

        val logsDir = File(getLogsPath())
        android.util.Log.d("MainActivity", "Logs directory: ${logsDir.absolutePath}")
        android.util.Log.d("MainActivity", "Logs directory exists: ${logsDir.exists()}")

        val systemScrollFile = File(logsDir, AppConstants.Paths.SYSTEM_NAME_LOG)
        val gameScrollFile = File(logsDir, AppConstants.Paths.GAME_FILENAME_LOG)

        android.util.Log.d("MainActivity", "System scroll file: ${systemScrollFile.absolutePath}")
        android.util.Log.d("MainActivity", "System scroll file exists: ${systemScrollFile.exists()}")
        android.util.Log.d("MainActivity", "Game scroll file: ${gameScrollFile.absolutePath}")
        android.util.Log.d("MainActivity", "Game scroll file exists: ${gameScrollFile.exists()}")

        // Check which file was modified most recently to determine which mode to use
        val systemScrollExists = systemScrollFile.exists()
        val gameScrollExists = gameScrollFile.exists()

        if (systemScrollExists && gameScrollExists) {
            // Both exist, check which was modified last
            if (systemScrollFile.lastModified() > gameScrollFile.lastModified()) {
                loadSystemImage()
            } else {
                loadGameInfo()
            }
        } else if (systemScrollExists) {
            loadSystemImage()
        } else {
            loadGameInfo()
        }

        updateDimmingOverlay()

        startFileMonitoring()
        setupBackHandling()

        // Apply blur effect if set
        updateBlurEffect()

        // Register broadcast receiver for app changes
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(appChangeReceiver, intentFilter)

        // Auto-launch setup wizard if needed
        checkAndLaunchSetupWizard()

        // Register volume change listener for real-time updates
        registerVolumeListener()
        registerSecondaryVolumeObserver()

        // Create default widgets on first launch
        createDefaultWidgets()
    }

    /**
     * Update the app state and keep legacy state variables in sync.
     *
     * During migration, this updates both the new state and old boolean variables.
     * Once migration is complete, we'll remove the legacy variable updates.
     */
    private fun updateState(newState: AppState) {
        state = newState
        // Legacy variable sync removed - all functions now use state directly
    }

    private fun checkAndShowWidgetTutorialForUpdate() {
        android.util.Log.d("MainActivity", "=== checkAndShowWidgetTutorialForUpdate CALLED ===")
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val currentVersion = packageInfo.versionName ?: "0.4.3"
            android.util.Log.d("MainActivity", "Current version from package: $currentVersion")

            val lastSeenVersion = prefsManager.tutorialVersionShown
            android.util.Log.d("MainActivity", "Last seen version from prefs: $lastSeenVersion")

            val hasSeenWidgetTutorial = prefsManager.widgetTutorialShown
            android.util.Log.d("MainActivity", "Has seen widget tutorial: $hasSeenWidgetTutorial")

            // Check if default widgets were created (indicates not a fresh install)
            val hasCreatedDefaultWidgets = prefsManager.defaultWidgetsCreated
            android.util.Log.d("MainActivity", "Has created default widgets: $hasCreatedDefaultWidgets")

            // NEW LOGIC:
            // Show tutorial if:
            // 1. User hasn't seen it yet AND
            // 2. EITHER they're updating from an older version OR they have default widgets (not fresh install)
            val isOlderVersion = lastSeenVersion != "0.0.0" && isVersionLessThan(lastSeenVersion, currentVersion)
            val shouldShowTutorial = !hasSeenWidgetTutorial && (isOlderVersion || hasCreatedDefaultWidgets)

            android.util.Log.d("MainActivity", "Should show tutorial: $shouldShowTutorial")
            android.util.Log.d("MainActivity", "  - hasSeenWidgetTutorial: $hasSeenWidgetTutorial")
            android.util.Log.d("MainActivity", "  - isOlderVersion: $isOlderVersion")
            android.util.Log.d("MainActivity", "  - hasCreatedDefaultWidgets: $hasCreatedDefaultWidgets")

            if (shouldShowTutorial) {
                android.util.Log.d("MainActivity", "✓ Showing widget tutorial")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showWidgetSystemTutorial(fromUpdate = true)
                }, AppConstants.Timing.TUTORIAL_DELAY)
            }

            // Always update the version tracking
            prefsManager.tutorialVersionShown = currentVersion
            android.util.Log.d("MainActivity", "Saved current version to prefs: $currentVersion")

        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in version check for widget tutorial", e)
        }
    }

    /**
     * Compare two version strings (e.g., "0.3.3" < "0.3.4")
     * Returns true if v1 < v2
     */
    private fun isVersionLessThan(v1: String, v2: String): Boolean {
        try {
            val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
            val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }

            // Compare each part
            for (i in 0 until maxOf(parts1.size, parts2.size)) {
                val p1 = parts1.getOrNull(i) ?: 0
                val p2 = parts2.getOrNull(i) ?: 0

                if (p1 < p2) return true
                if (p1 > p2) return false
            }

            // Versions are equal
            return false
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error comparing versions: $v1 vs $v2", e)
            return false
        }
    }

    private fun checkAndLaunchSetupWizard() {
        // Check if setup has been completed
        val hasCompletedSetup = prefsManager.setupCompleted

        // Check if permissions are granted (Android 13+ simplified)
        val hasPermission = Environment.isExternalStorageManager()

        // Launch setup wizard immediately if:
        // 1. Setup not completed, OR
        // 2. Missing permissions
        if (!hasCompletedSetup || !hasPermission) {
            android.util.Log.d("MainActivity", "Setup incomplete or missing permissions - launching wizard immediately")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, SettingsActivity::class.java)
                intent.putExtra("AUTO_START_WIZARD", true)
                settingsLauncher.launch(intent)
            }, AppConstants.Timing.SETTINGS_DELAY)
            return
        }

        // For script check, wait for SD card if needed (setup is complete and has permissions)
        checkScriptsWithRetry()
    }

    /**
     * Show comprehensive widget system tutorial dialog
     * @param fromUpdate - True if showing because user updated the app
     */
    private fun showWidgetSystemTutorial(fromUpdate: Boolean = false) {
        // Get current version dynamically
        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "0.4.3"  // Fallback version
        }

        // Create custom title view with emoji
        val titleContainer = android.widget.LinearLayout(this)
        titleContainer.orientation = android.widget.LinearLayout.HORIZONTAL
        titleContainer.setPadding(60, 40, 60, 20)
        titleContainer.gravity = android.view.Gravity.CENTER

        val titleText = android.widget.TextView(this)
        titleText.text = if (fromUpdate) "🆕 Widget Overlay System" else "📐 Widget Overlay System"
        titleText.textSize = 24f
        titleText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
        titleText.gravity = android.view.Gravity.CENTER

        titleContainer.addView(titleText)

        // Create scrollable message view
        val scrollView = android.widget.ScrollView(this)
        val messageText = android.widget.TextView(this)

        val updatePrefix = if (fromUpdate) {
            "New in version 0.4.0+! The widget overlay system lets you create customizable displays on top of game artwork.\n\n"
        } else {
            ""
        }

        messageText.text = """
${updatePrefix}🎨 What Are Widgets?

Widgets are overlay elements that display game/system artwork like marquees, box art, screenshots, and more. You can position and size them however you want!

🔓 Widget Edit Mode

Widgets are LOCKED by default to prevent accidental changes. To edit widgets:

1. Long-press anywhere on screen → Widget menu appears
2. Toggle "Widget Edit Mode: OFF" to ON
3. Now you can create, move, resize, and delete widgets

➕ Creating Widgets

1. Unlock widgets (see above)
2. Open widget menu (long-press screen)
3. Tap "Add Widget"
4. Choose widget type (Marquee, Box Art, Screenshot, etc.)

✏️ Editing Widgets

Select: Tap a widget to select it (shows purple border)
Move: Drag selected widget to reposition
Resize: Drag the corner handles (⌙ shapes) on selected widgets
Delete: Tap the X button on selected widget
Settings: Tap the ⚙ button for layer ordering options

📐 Grid System

Snap to Grid: Makes positioning precise and aligned
Show Grid: Visual grid overlay to help with alignment

Both options in the widget menu!

🔒 Important: Lock Widgets When Done

After arranging your widgets, toggle Edit Mode back to OFF. This prevents accidental changes during normal use.

💡 Tips

• Widgets are context-aware - create separate layouts for games vs systems
• Use "Bring to Front" / "Send to Back" to layer widgets
• Each widget updates automatically when you browse in ES-DE
• System logos work for both built-in and custom system logos

Access this help anytime from the widget menu!
"""

        messageText.textSize = 16f
        messageText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
        messageText.setPadding(60, 20, 60, 20)

        scrollView.addView(messageText)

        // Create "don't show again" checkbox
        val checkboxContainer = android.widget.LinearLayout(this)
        checkboxContainer.orientation = android.widget.LinearLayout.HORIZONTAL
        checkboxContainer.setPadding(60, 10, 60, 20)
        checkboxContainer.gravity = android.view.Gravity.CENTER_VERTICAL

        val checkbox = android.widget.CheckBox(this)
        checkbox.text = "Don't show this automatically again"
        checkbox.setTextColor(android.graphics.Color.parseColor("#999999"))
        checkbox.textSize = 14f

        checkboxContainer.addView(checkbox)

        // Create main container
        val mainContainer = android.widget.LinearLayout(this)
        mainContainer.orientation = android.widget.LinearLayout.VERTICAL
        mainContainer.addView(scrollView)
        mainContainer.addView(checkboxContainer)

        // Show dialog
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(titleContainer)
            .setView(mainContainer)
            .setPositiveButton("Got It!") { _, _ ->
                // Mark as shown
                prefsManager.widgetTutorialShown = true

                // If user checked "don't show again", mark preference
                if (checkbox.isChecked) {
                    prefsManager.widgetTutorialDontShowAuto = true
                }
            }
            .setCancelable(true)
            .create()

        // Dark theme for dialog
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#1A1A1A"))
            )
        }

        dialog.show()

        android.util.Log.d("MainActivity", "Widget tutorial dialog shown (fromUpdate: $fromUpdate)")
    }

    /**
     * Check for scripts with retry logic to handle SD card mounting delays
     */
    private fun checkScriptsWithRetry(attempt: Int = 0, maxAttempts: Int = 5) {
        val scriptsPath = prefsManager.scriptsPath.ifEmpty { null }

        // If no custom scripts path is set, scripts are likely on internal storage
        // Check immediately without retry
        if (scriptsPath == null || scriptsPath.startsWith("/storage/emulated/0")) {
            android.util.Log.d("MainActivity", "Scripts on internal storage - checking immediately")
            val hasCorrectScripts = checkForCorrectScripts()
            if (!hasCorrectScripts) {
                android.util.Log.d("MainActivity", "Scripts missing/outdated on internal storage - showing dialog")

                // Check if scripts exist at all (missing vs outdated)
                val scriptsDir = File(scriptsPath ?: AppConstants.Paths.DEFAULT_SCRIPTS_PATH)
                val gameSelectScript = File(scriptsDir, "game-select/esdecompanion-game-select.sh")

                if (gameSelectScript.exists()) {
                    // Scripts exist but are outdated - show update dialog
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        showScriptsUpdateAvailableDialog()
                    }, AppConstants.Timing.SETTINGS_DELAY)
                } else {
                    // Scripts missing - launch full wizard
                    launchSetupWizardForScripts()
                }
            }
            return
        }

        // Custom path set - might be on SD card
        // Check if path is accessible (SD card mounted)
        val scriptsDir = File(scriptsPath)
        val isAccessible = scriptsDir.exists() && scriptsDir.canRead()

        if (!isAccessible && attempt < maxAttempts) {
            // SD card not mounted yet - wait and retry
            val delayMs = ((attempt + 1) * AppConstants.Timing.SD_MOUNT_RETRY_BASE_DELAY)
                .coerceAtMost(AppConstants.Timing.SD_MOUNT_RETRY_MAX_DELAY)

            android.util.Log.d("MainActivity", "Scripts path not accessible (attempt ${attempt + 1}/$maxAttempts) - waiting ${delayMs}ms for SD card mount: $scriptsPath")

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                checkScriptsWithRetry(attempt + 1, maxAttempts)
            }, delayMs)
            return
        }

        // Either accessible now or max attempts reached - check scripts
        val hasCorrectScripts = checkForCorrectScripts()

        if (!hasCorrectScripts) {
            if (isAccessible) {
                // Path is accessible but scripts are missing/invalid
                android.util.Log.d("MainActivity", "Scripts missing/outdated on accessible path")

                // Check if scripts exist at all (missing vs outdated)
                val gameSelectScript = File(scriptsDir, "game-select/esdecompanion-game-select.sh")

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (gameSelectScript.exists()) {
                        // Scripts exist but are outdated - show update dialog
                        showScriptsUpdateAvailableDialog()
                    } else {
                        // Scripts missing - launch full wizard
                        launchSetupWizardForScripts()
                    }
                }, AppConstants.Timing.SETTINGS_DELAY)
            } else {
                // Max attempts reached and still not accessible
                // SD card might not be mounted - show a helpful message
                android.util.Log.w("MainActivity", "Scripts path not accessible after $maxAttempts attempts: $scriptsPath")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showSdCardNotMountedDialog(scriptsPath)
                }, AppConstants.Timing.SETTINGS_DELAY)
            }
        } else {
            android.util.Log.d("MainActivity", "Scripts found and valid - no wizard needed")
        }
    }

    /**
     * Launch setup wizard specifically for script issues
     */
    private fun launchSetupWizardForScripts() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("AUTO_START_WIZARD", true)
            settingsLauncher.launch(intent)
        }, AppConstants.Timing.SETTINGS_DELAY)
    }

    /**
     * Show dialog when SD card is not mounted
     */
    private fun showSdCardNotMountedDialog(scriptsPath: String) {
        AlertDialog.Builder(this)
            .setTitle("SD Card Not Detected")
            .setMessage("Your scripts folder appears to be on an SD card that is not currently accessible:\n\n$scriptsPath\n\nPlease ensure:\n• The SD card is properly inserted\n• The device has finished booting\n• The SD card is mounted\n\nThe app will work once the SD card becomes accessible.")
            .setPositiveButton("Open Settings") { _, _ ->
                settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
            }
            .setNegativeButton("Dismiss", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun checkForCorrectScripts(): Boolean {
        val scriptsDir = File(prefsManager.scriptsPath)
        return ScriptManager.areScriptsValid(scriptsDir)
    }

    /**
     * Show dialog when old scripts are detected
     */
    private fun showScriptsUpdateAvailableDialog() {
        AlertDialog.Builder(this)
            .setTitle("Script Update Available")
            .setMessage("Your ES-DE integration scripts need to be updated.\n\n" +
                    "Changes in this update:\n" +
                    "• Fixes handling of game names with embedded quotes\n" +
                    "• Improved POSIX shell compatibility\n" +
                    "• Better special character handling\n" +
                    "• Example: Games like 'Real Deal' Boxing now work correctly\n\n" +
                    "Would you like to update the scripts now?")
            .setPositiveButton("Update Scripts") { _, _ ->
                updateScriptsDirectly()
            }
            .setNegativeButton("Later") { _, _ ->
                Toast.makeText(
                    this,
                    "You can update scripts anytime from Settings",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setIcon(android.R.drawable.ic_dialog_info)
            .show()
    }

    /**
     * Update scripts directly without going through wizard
     * Now uses ScriptManager for centralized logic
     */
    private fun updateScriptsDirectly() {
        val scriptsPath = prefsManager.scriptsPath

        val result = ScriptManager.createAllScripts(File(scriptsPath))

        Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()

        if (result.success) {
            android.util.Log.d("MainActivity", "Scripts updated successfully")
        } else {
            android.util.Log.e("MainActivity", result.message)
        }
    }

    private fun createDefaultWidgets() {
        // Check if we've already created default widgets
        val hasCreatedDefaults = prefsManager.defaultWidgetsCreated
        if (hasCreatedDefaults) {
            android.util.Log.d("MainActivity", "Default widgets already created on previous launch")
            return
        }

        android.util.Log.d("MainActivity", "First launch - creating default widgets")

        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val centerY = displayMetrics.heightPixels / 2f

        // Calculate responsive widget sizes from screen percentages
        val systemLogoWidth = displayMetrics.widthPixels * AppConstants.UI.DEFAULT_WIDGET_WIDTH_PERCENT
        val systemLogoHeight = displayMetrics.heightPixels * AppConstants.UI.DEFAULT_WIDGET_HEIGHT_PERCENT

        val gameMarqueeWidth = displayMetrics.widthPixels * AppConstants.UI.DEFAULT_WIDGET_WIDTH_PERCENT
        val gameMarqueeHeight = displayMetrics.heightPixels * AppConstants.UI.DEFAULT_WIDGET_HEIGHT_PERCENT

        // Create default system logo widget (centered)
        val systemLogoWidget = Widget(
            imageType = Widget.ImageType.SYSTEM_LOGO,
            imagePath = "",
            x = centerX - (systemLogoWidth / 2),
            y = centerY - (systemLogoHeight / 2),
            width = systemLogoWidth,
            height = systemLogoHeight,
            zIndex = 0,
            widgetContext = Widget.WidgetContext.SYSTEM
        )

        // Create default game marquee widget (centered)
        val gameMarqueeWidget = Widget(
            imageType = Widget.ImageType.MARQUEE,
            imagePath = "",
            x = centerX - (gameMarqueeWidth / 2),
            y = centerY - (gameMarqueeHeight / 2),
            width = gameMarqueeWidth,
            height = gameMarqueeHeight,
            zIndex = 0,
            widgetContext = Widget.WidgetContext.GAME
        )

        // Save both widgets
        val defaultWidgets = listOf(systemLogoWidget, gameMarqueeWidget)
        widgetManager.saveWidgets(defaultWidgets)

        // Mark that we've created default widgets
        prefsManager.defaultWidgetsCreated = true

        android.util.Log.d("MainActivity", "Created ${defaultWidgets.size} default widgets")
    }

    /**
     * Sanitize a full game path to just the filename for media lookup
     * Handles:
     * - Subfolders: "subfolder/game.zip" -> "game.zip"
     * - Backslashes: "game\file.zip" -> "gamefile.zip"
     * - Multiple path separators
     */
    private fun sanitizeGameFilename(fullPath: String): String {
        // Remove backslashes (screensaver case)
        var cleaned = fullPath.replace("\\", "")

        // Get just the filename (after last forward slash)
        cleaned = cleaned.substringAfterLast("/")

        return cleaned
    }

    override fun onPause() {
        super.onPause()
        // Cancel any pending video delay timers (prevent video loading while in settings)
        videoManager.cancelVideoDelay()
        // Stop and release video player when app goes to background
        // This fixes video playback issues on devices with identical display names (e.g., Ayaneo Pocket DS)
        releasePlayer()
    }

    override fun onResume() {
        super.onResume()

        // Close drawer if it's open (user is returning from Settings or an app)
        // This happens after Settings/app is visible, so no animation is seen
        if (::bottomSheetBehavior.isInitialized &&
            bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        // Update video volume based on current system volume
        updateVideoVolume()

        // Sync music controls with MusicManager state
        updateMusicControls()

        // Clear search bar
        if (::appSearchBar.isInitialized) {
            appSearchBar.text.clear()
        }

        // Reload grid layout in case column count changed
        val columnCount = prefsManager.columnCount
        appRecyclerView.layoutManager = GridLayoutManager(this, columnCount)

        // Reload images and videos based on current state (don't change modes)
        // Skip reload if returning from settings with no changes
        if (skipNextReload) {
            skipNextReload = false
            android.util.Log.d("MainActivity", "Skipping reload - no settings changed")
        } else {
            // Don't reload if game is playing or screensaver is active
            // This prevents unnecessary video loading during these states
            if (state is AppState.GamePlaying) {
                android.util.Log.d("MainActivity", "Skipping reload - game playing")
            } else if (state is AppState.Screensaver) {
                android.util.Log.d("MainActivity", "Skipping reload - screensaver active")
            } else {
                // Normal reload - this will reload both images and videos
                if (state is AppState.SystemBrowsing) {
                    loadSystemImage()
                } else {
                    loadGameInfo()  // This calls handleVideoForGame() internally
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isActivityVisible = true
        android.util.Log.d("MainActivity", "Activity VISIBLE (onStart) - videos allowed if other conditions met")

        // Reset internal navigation flag when returning
        isNavigatingInternally = false

        // Resume music if we were truly in background
        musicManager.onActivityVisible()
    }

    override fun onStop() {
        super.onStop()
        isActivityVisible = false
        android.util.Log.d("MainActivity", "Activity NOT VISIBLE (onStop) - blocking videos")
        releasePlayer()

        // Only pause music if we're ACTUALLY going to background
        // Don't pause if navigating to Settings or other internal activities
        if (!isNavigatingInternally) {
            android.util.Log.d("MainActivity", "Going to background - pausing music")
            musicManager.onActivityInvisible()
            hideSongTitle()
        } else {
            android.util.Log.d("MainActivity", "Internal navigation detected - keeping music playing")
        }
    }

    private fun updateBlurEffect() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val blurRadius = prefsManager.blurLevel

            if (blurRadius > 0) {
                val blurEffect = android.graphics.RenderEffect.createBlurEffect(
                    blurRadius.toFloat(),
                    blurRadius.toFloat(),
                    android.graphics.Shader.TileMode.CLAMP
                )
                gameImageView.setRenderEffect(blurEffect)
            } else {
                gameImageView.setRenderEffect(null)
            }
        }
    }

    private fun updateDrawerTransparency() {
        val transparencyPercent = prefsManager.drawerTransparency
        // Convert percentage (0-100) to hex alpha (00-FF)
        val alpha = (transparencyPercent * 255 / 100).coerceIn(0, 255)
        val hexAlpha = String.format("%02x", alpha)
        val colorString = "#${hexAlpha}000000"

        val color = android.graphics.Color.parseColor(colorString)
        appDrawer.setBackgroundColor(color)
    }

    private fun getMediaBasePath(): String {
        val customPath = if (prefsManager.mediaPath.isEmpty()) null else prefsManager.mediaPath
        val path = customPath ?: AppConstants.Paths.DEFAULT_MEDIA_PATH
        android.util.Log.d("ESDESecondScreen", "Media base path: $path")
        return path
    }

    private fun getSystemImagePath(): String {
        val customPath = if (prefsManager.systemPath.isEmpty()) null else prefsManager.systemPath
        val path = customPath ?: "${Environment.getExternalStorageDirectory()}/ES-DE Companion/system_images"
        android.util.Log.d("ESDESecondScreen", "System image path: $path")
        return path
    }

    private fun getSystemLogosPath(): String {
        val customPath = if (prefsManager.systemLogosPath.isEmpty()) null else prefsManager.systemLogosPath
        val path = customPath ?: "${Environment.getExternalStorageDirectory()}/ES-DE Companion/system_logos"
        android.util.Log.d("ESDESecondScreen", "System logos path: $path")
        return path
    }

    private fun getLogsPath(): String {
        // Always use fixed internal storage location for logs
        // FileObserver requires internal storage for reliable monitoring (SD cards not supported)
        // This path is NOT user-configurable due to this technical limitation
        return AppConstants.Paths.DEFAULT_LOGS_PATH
    }

    private fun loadFallbackBackground(forceCustomImageOnly: Boolean = false) {
        android.util.Log.d("MainActivity", "═══ loadFallbackBackground CALLED (forceCustomImageOnly=$forceCustomImageOnly) ═══")

        // CRITICAL: Don't check solid color when forcing custom image only
        // When forceCustomImageOnly=true (screensaver/game launch "default_image" behavior),
        // we skip the solid color check and go straight to custom background image
        //
        // NOTE: This function is context-independent - it only loads the custom background
        // or built-in fallback. Solid color handling should be done by the caller
        // (loadSystemImage or loadGameInfo) before calling this function.
        if (!forceCustomImageOnly) {
            android.util.Log.d("MainActivity", "loadFallbackBackground: Solid color should be handled by caller, not here")
        }

        // Check if user has set a custom background
        val customBackgroundPath = prefsManager.customBackgroundPath.ifEmpty { null }
        android.util.Log.d("MainActivity", "Custom background path: $customBackgroundPath")

        if (customBackgroundPath != null) {
            try {
                val file = File(customBackgroundPath)
                android.util.Log.d("MainActivity", "File exists: ${file.exists()}, canRead: ${file.canRead()}")

                if (file.exists() && file.canRead()) {
                    // ImageManager will automatically skip animation if same image is already loaded
                    loadImageWithAnimation(file, gameImageView) {
                        android.util.Log.d("MainActivity", "✓ Loaded custom background")
                    }

                    return
                } else {
                    android.util.Log.w("MainActivity", "Custom background file not accessible: $customBackgroundPath")
                }
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Error loading custom background, using built-in default", e)
            }
        }

        // No custom background or loading failed - use built-in default
        android.util.Log.d("MainActivity", "Loading built-in fallback background")
        loadBuiltInFallbackBackground()

    }

    private fun loadBuiltInFallbackBackground() {
        try {
            val assetPath = "fallback/default_background.webp"
            // Copy asset to cache for loading
            val fallbackFile = File(cacheDir, "default_background.webp")
            if (!fallbackFile.exists()) {
                assets.open(assetPath).use { input ->
                    fallbackFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // ImageManager will automatically skip animation if same image is already loaded
            loadImageWithAnimation(fallbackFile, gameImageView) {
                android.util.Log.d("MainActivity", "Loaded built-in fallback image from assets")
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Failed to load built-in fallback image, using solid color", e)
            // Final fallback: solid color (no animation possible)
            gameImageView.setBackgroundColor(android.graphics.Color.parseColor("#1A1A1A"))
            gameImageView.setImageDrawable(null)
        }
    }

    /**
     * Load an image with animation based on user preference
     */
    private fun loadImageWithAnimation(
        imageFile: File,
        targetView: ImageView,
        onComplete: (() -> Unit)? = null
    ) {
        // Use ImageManager for consistent loading
        imageManager.loadGameBackground(
            imageView = targetView,
            imagePath = imageFile.absolutePath,
            applyBlur = true,  // ImageManager will check prefsManager.blurLevel
            applyTransition = true,  // ImageManager will use prefsManager.animationStyle
            onLoaded = { onComplete?.invoke() },
            onFailed = {
                android.util.Log.w("MainActivity", "Failed to load image: ${imageFile.absolutePath}")
                onComplete?.invoke()
            }
        )
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x

                // Check if it's more vertical than horizontal
                if (abs(diffY) > abs(diffX)) {
                    // Vertical fling
                    if (abs(diffY) > 100 && abs(velocityY) > 100) {
                        if (diffY < 0) {  // diffY < 0 means swipe UP
                            // Swipe up - open drawer
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                            return true
                        }
                    }
                }
                return false
            }
        })
    }

    /**
     * Set up click listeners for music control buttons.
     */
    private fun setupMusicControlButtons() {
        musicPlayPauseButton.setOnClickListener {
            toggleMusicPlayback()
        }

        musicNextButton.setOnClickListener {
            playNextTrack()
        }
    }

    /**
     * Toggle music playback between play and pause.
     */
    private fun toggleMusicPlayback() {
        if (musicManager.isPlaying()) {
            musicManager.pauseMusic()
            android.util.Log.d("MainActivity", "Music paused via button")
        } else {
            musicManager.resumeMusic()
            android.util.Log.d("MainActivity", "Music resumed via button")
        }
        // UI update handled by MusicManager callback
    }

    /**
     * Skip to next track in playlist.
     */
    private fun playNextTrack() {
        musicManager.skipToNextTrack()
        android.util.Log.d("MainActivity", "Skipped to next track via button")
    }

    private fun isTouchOnWidget(x: Float, y: Float): Boolean {
        for (widgetView in activeWidgets) {
            val location = IntArray(2)
            widgetView.getLocationOnScreen(location)
            val widgetX = location[0].toFloat()
            val widgetY = location[1].toFloat()

            if (x >= widgetX && x <= widgetX + widgetView.width &&
                y >= widgetY && y <= widgetY + widgetView.height) {
                return true
            }
        }
        return false
    }

    /**
     * Cancel any pending long press - called by WidgetView when interaction starts
     */
    fun cancelLongPress() {
        longPressRunnable?.let {
            longPressHandler?.removeCallbacks(it)
            longPressTriggered = false
        }
    }

    /**
     * Show the black overlay instantly (no animation)
     */
    private fun showBlackOverlay() {
        android.util.Log.d("MainActivity", "Showing black overlay")
        isBlackOverlayShown = true

        // Stop video immediately
        releasePlayer()

        musicManager.onBlackOverlayChanged(true)

        // Show overlay instantly without animation
        blackOverlay.visibility = View.VISIBLE
        blackOverlay.translationY = 0f
    }

    /**
     * Hide the black overlay instantly (no animation)
     */
    private fun hideBlackOverlay() {
        android.util.Log.d("MainActivity", "Hiding black overlay")
        isBlackOverlayShown = false

        musicManager.onBlackOverlayChanged(false)

        // Hide overlay instantly without animation
        blackOverlay.visibility = View.GONE

        val displayHeight = resources.displayMetrics.heightPixels.toFloat()
        blackOverlay.translationY = -displayHeight

        // Re-evaluate music for current state (in case user scrolled while overlay was shown)
        musicManager.onStateChanged(state)

        // Reload video if applicable (don't reload images)
        when (val s = state) {
            is AppState.GameBrowsing -> {
                // In GameBrowsing, we ALWAYS have systemName and gameFilename (non-null)
                val gameName = s.gameFilename.substringBeforeLast('.')
                handleVideoForGame(s.systemName, gameName, s.gameFilename)
            }
            else -> {
                // Not in game browsing mode - don't play video
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Check if black overlay feature is enabled
        val blackOverlayEnabled = prefsManager.blackOverlayEnabled

        // Check drawer state first
        val drawerState = bottomSheetBehavior.state
        val isDrawerOpen = drawerState == BottomSheetBehavior.STATE_EXPANDED ||
                drawerState == BottomSheetBehavior.STATE_SETTLING

        // Two-finger tap to toggle song title visibility
        if (!isDrawerOpen) {
            val musicEnabled = prefsManager.musicEnabled
            val songTitleEnabled = prefsManager.musicSongTitleEnabled

            android.util.Log.d("MainActivity", "Two-finger check: pointerCount=${ev.pointerCount}, action=${ev.action}, musicEnabled=$musicEnabled, songTitleEnabled=$songTitleEnabled, hasActiveMusic=${musicManager.isPlaying() || musicManager.isPaused()}")

            // Only process two-finger gestures when music and song title are enabled
            if (musicEnabled && songTitleEnabled && ev.pointerCount == 2 && ev.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                val currentTime = System.currentTimeMillis()
                val timeSinceLast = currentTime - lastTwoFingerTapTime

                android.util.Log.d("MainActivity", "Two-finger tap detected: timeSinceLast=${timeSinceLast}ms")

                // Reset if too much time passed
                if (timeSinceLast > TWO_FINGER_TAP_TIMEOUT) {
                    twoFingerTapCount = 0
                }

                twoFingerTapCount++
                lastTwoFingerTapTime = currentTime

                android.util.Log.d("MainActivity", "Two-finger tap count: $twoFingerTapCount")

                // Trigger on first two-finger tap
                if (twoFingerTapCount >= 1) {
                    android.util.Log.d("MainActivity", "Two-finger tap confirmed - toggling song title")
                    twoFingerTapCount = 0

                    // Check if music is active (playing or paused, not completely stopped)
                    if (musicManager.isPlaying() || musicManager.isPaused()) {
                        // Toggle based on current visibility
                        val isVisible = songTitleOverlay.visibility == View.VISIBLE
                        android.util.Log.d("MainActivity", "Current overlay visibility: ${songTitleOverlay.visibility}, isVisible=$isVisible")

                        if (isVisible) {
                            // Currently visible - hide it
                            android.util.Log.d("MainActivity", "Hiding visible song title")
                            hideSongTitleOverlay()
                        } else {
                            // Currently hidden - show it with timeout
                            android.util.Log.d("MainActivity", "Showing hidden song title with timeout")
                            showSongTitleOverlay()
                        }
                    } else {
                        android.util.Log.d("MainActivity", "Music not active - ignoring two-finger tap")
                    }

                    return true
                }
            }
        }
        // ========== TWO-FINGER TAP FOR MUSIC CONTROLS END ==========

        // Handle black overlay double-tap detection ONLY when drawer is closed and feature is enabled
        if (!isDrawerOpen && blackOverlayEnabled) {
            if (ev.action == MotionEvent.ACTION_DOWN) {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastTap = currentTime - lastTapTime

                android.util.Log.d("MainActivity", "Double-tap detection: timeSinceLastTap=${timeSinceLastTap}ms, tapCount=$tapCount")

                // Reset tap count if too much time has passed
                if (timeSinceLastTap > DOUBLE_TAP_TIMEOUT) {
                    android.util.Log.d("MainActivity", "Tap timeout exceeded (${timeSinceLastTap}ms > ${DOUBLE_TAP_TIMEOUT}ms) - resetting tap count")
                    tapCount = 0
                }

                // Only count tap if enough time has passed since last tap OR it's the first tap
                // (prevents accidental fast touches like brushing the screen)
                if (lastTapTime == 0L || timeSinceLastTap >= MIN_TAP_INTERVAL) {
                    tapCount++
                    lastTapTime = currentTime

                    android.util.Log.d("MainActivity", "Tap registered - new tapCount=$tapCount")

                    // Check for double-tap
                    if (tapCount >= 2) {
                        android.util.Log.d("MainActivity", "Double-tap threshold reached! Toggling black overlay")
                        tapCount = 0 // Reset counter

                        // Toggle black overlay
                        if (isBlackOverlayShown) {
                            hideBlackOverlay()
                        } else {
                            showBlackOverlay()
                        }
                        return true
                    }
                } else {
                    // Tap was too fast after previous tap - ignore it
                    android.util.Log.d("MainActivity", "Tap IGNORED - too fast (${timeSinceLastTap}ms < ${MIN_TAP_INTERVAL}ms)")
                }
            }
        }

        // If overlay is shown, consume all touches
        if (isBlackOverlayShown) {
            return true
        }

        // Handle long press for widget menu (works anywhere, even on widgets)
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = ev.x
                touchDownY = ev.y
                longPressTriggered = false

                // Cancel any existing callbacks first
                longPressRunnable?.let {
                    longPressHandler?.removeCallbacks(it)
                }

                // Allow long press in system view too
                if (!widgetMenuShowing && drawerState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (longPressHandler == null) {
                        longPressHandler = Handler(android.os.Looper.getMainLooper())
                    }
                    longPressRunnable = Runnable {
                        if (!longPressTriggered && !widgetMenuShowing) {
                            longPressTriggered = true
                            widgetMenuShowing = true
                            showCreateWidgetMenu()
                        }
                    }
                    longPressHandler?.postDelayed(longPressRunnable!!, LONG_PRESS_TIMEOUT)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // Cancel long press if finger moves beyond touch slop threshold
                val deltaX = kotlin.math.abs(ev.x - touchDownX)
                val deltaY = kotlin.math.abs(ev.y - touchDownY)
                val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

                if (deltaX > touchSlop || deltaY > touchSlop) {
                    longPressRunnable?.let {
                        longPressHandler?.removeCallbacks(it)
                        longPressTriggered = false
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // CHANGED: Always cancel the callback on finger lift
                longPressRunnable?.let {
                    longPressHandler?.removeCallbacks(it)
                }

                if (longPressTriggered) {
                    // Long press was triggered, consume this event
                    longPressTriggered = false  // ADDED: Reset immediately
                    return true
                }
            }
        }

        // Handle tapping outside widgets to deselect them
        if (ev.action == MotionEvent.ACTION_UP && !longPressTriggered) {
            if (!isTouchOnWidget(ev.x, ev.y)) {
                // Tapped outside any widget - deselect all
                activeWidgets.forEach { it.deselect() }
            }
        }

        // Track widget interaction state for gesture detector
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                isInteractingWithWidget = isTouchOnWidget(ev.x, ev.y) && isWidgetSelected(ev.x, ev.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isInteractingWithWidget = false
            }
        }

        // Only use gesture detector if NOT actively interacting with a SELECTED widget AND drawer is hidden
        if (drawerState == BottomSheetBehavior.STATE_HIDDEN && !isInteractingWithWidget) {
            gestureDetector.onTouchEvent(ev)
        }

        return super.dispatchTouchEvent(ev)
    }

    private fun setupAppDrawer() {
        bottomSheetBehavior = BottomSheetBehavior.from(appDrawer)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.skipCollapsed = true

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    showSettingsPulseHint()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        appDrawer.post {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            android.util.Log.d("MainActivity", "AppDrawer state set to HIDDEN: ${bottomSheetBehavior.state}")
        }

        val columnCount = prefsManager.columnCount
        appRecyclerView.layoutManager = GridLayoutManager(this, columnCount)

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val hiddenApps = prefsManager.hiddenApps
        allApps = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
            .filter { !hiddenApps.contains(it.activityInfo?.packageName ?: "") }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

        // Pass hiddenApps to adapter
        appRecyclerView.adapter = AppAdapter(
            allApps,
            packageManager,
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app, view -> showAppOptionsDialog(app) },
            appLaunchPrefs = appLaunchPrefs,
            hiddenApps = hiddenApps  // ADD THIS LINE
        )

        android.util.Log.d("MainActivity", "AppDrawer setup complete, initial state: ${bottomSheetBehavior.state}")
    }

    /**
     * Show pulsing animation on settings button when drawer opens
     * Only shows the first 3 times the drawer is opened (total, not per session)
     */
    private fun showSettingsPulseHint() {
        // Only show if user has completed setup
        if (!prefsManager.setupCompleted) return

        // Check how many times hint has been shown (max 3 times total)
        val hintCount = prefsManager.settingsHintCount
        if (hintCount >= 3) return

        // Increment the hint counter
        prefsManager.settingsHintCount = hintCount + 1

        // Delay slightly so drawer animation completes first
        Handler(Looper.getMainLooper()).postDelayed({
            // Create pulsing animation (3 pulses)
            val pulseCount = 3
            var currentPulse = 0

            fun doPulse() {
                if (currentPulse >= pulseCount) return

                settingsButton.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(400)
                    .withEndAction {
                        settingsButton.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(400)
                            .withEndAction {
                                currentPulse++
                                if (currentPulse < pulseCount) {
                                    // Small delay between pulses
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        doPulse()
                                    }, 200)
                                }
                            }
                            .start()
                    }
                    .start()
            }

            doPulse()

            // Show a subtle toast as well
            Toast.makeText(
                this,
                "Tip: Tap ☰ to open the app settings",
                Toast.LENGTH_LONG
            ).show()

        }, 800) // Wait for drawer to fully open
    }

    private fun setupSearchBar() {
        // Setup clear button click listener
        searchClearButton.setOnClickListener {
            appSearchBar.text.clear()
        }

        appSearchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.lowercase() ?: ""

                // Show/hide clear button based on whether there's text
                searchClearButton.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE

                val hiddenApps = prefsManager.hiddenApps

                val filteredApps = if (query.isEmpty()) {
                    // No search query - show only visible apps (current behavior)
                    allApps
                } else {
                    // Has search query - search ALL apps including hidden ones
                    val mainIntent = Intent(Intent.ACTION_MAIN, null)
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

                    packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
                        .filter { app ->
                            app.loadLabel(packageManager).toString().lowercase().contains(query)
                        }
                        .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
                }

                // Update adapter with filtered results - pass hiddenApps
                appRecyclerView.adapter = AppAdapter(
                    filteredApps,
                    packageManager,
                    onAppClick = { app ->
                        launchApp(app)
                    },
                    onAppLongClick = { app, view ->
                        showAppOptionsDialog(app)
                    },
                    appLaunchPrefs = appLaunchPrefs,
                    hiddenApps = hiddenApps  // ADD THIS LINE
                )
            }
        })
    }

    private fun setupSettingsButton() {
        settingsButton.setOnClickListener {
            // Log current display when settings is opened
            val currentDisplay = getCurrentDisplayId()
            android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            android.util.Log.d("MainActivity", "SETTINGS BUTTON CLICKED")
            android.util.Log.d("MainActivity", "Companion currently on display: $currentDisplay")

            // Also log all available displays
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                try {
                    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
                    val displays = displayManager.displays
                    android.util.Log.d("MainActivity", "All available displays:")
                    displays.forEachIndexed { index, display ->
                        android.util.Log.d("MainActivity", "  Display $index: ID=${display.displayId}, Name='${display.name}'")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error listing displays", e)
                }
            }

            android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // Mark that we're navigating internally (don't pause music)
            isNavigatingInternally = true

            // Don't close the drawer - just launch Settings over it
            // The drawer will still be there when returning, but that's okay
            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupDrawerBackButton() {
        drawerBackButton.setOnClickListener {
            // Clear search and close the app drawer
            appSearchBar.text.clear()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun setupAndroidSettingsButton() {
        androidSettingsButton.setOnClickListener {
            // Android Settings is NOT internal navigation - pause music
            // (This is the system settings app, not our SettingsActivity)
            isNavigatingInternally = false

            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }

    private fun startFileMonitoring() {
        val watchDir = File(getLogsPath())
        android.util.Log.d("MainActivity", "Starting file monitoring on: ${watchDir.absolutePath}")
        android.util.Log.d("MainActivity", "Watch directory exists: ${watchDir.exists()}")

        // Create logs directory if it doesn't exist
        if (!watchDir.exists()) {
            watchDir.mkdirs()
            android.util.Log.d("MainActivity", "Created logs directory")
        }

        fileObserver = object : FileObserver(watchDir, MODIFY or CLOSE_WRITE) {
            private var lastEventTime = 0L

            override fun onEvent(event: Int, path: String?) {
                if (path != null && (path == AppConstants.Paths.GAME_FILENAME_LOG || path == AppConstants.Paths.SYSTEM_NAME_LOG ||
                            path == AppConstants.Paths.GAME_START_FILENAME_LOG || path == AppConstants.Paths.GAME_END_FILENAME_LOG ||
                            path == AppConstants.Paths.SCREENSAVER_START_LOG || path == AppConstants.Paths.SCREENSAVER_END_LOG ||
                            path == AppConstants.Paths.SCREENSAVER_GAME_FILENAME_LOG)) {
                    // Debounce: ignore events that happen too quickly
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastEventTime < 50) {
                        return
                    }
                    lastEventTime = currentTime

                    runOnUiThread {
                        // Small delay to ensure file is fully written
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            // Check if we're waiting for script verification OR error dialog is showing
                            if (isWaitingForScriptVerification) {
                                stopScriptVerification(true)  // Success!
                            } else if (currentErrorDialog != null) {
                                // User was looking at error dialog when they browsed in ES-DE
                                // Dismiss error dialog and show success
                                currentErrorDialog?.dismiss()
                                currentErrorDialog = null
                                onScriptVerificationSuccess()
                            }

                            when (path) {
                                AppConstants.Paths.SYSTEM_NAME_LOG -> {
                                    // Ignore if launching from screensaver (game-select event between screensaver-end and game-start)
                                    if (isLaunchingFromScreensaver) {
                                        android.util.Log.d("MainActivity", "Game scroll ignored - launching from screensaver")
                                        return@postDelayed
                                    }

                                    // Ignore if screensaver is active
                                    if (state is AppState.Screensaver) {
                                        android.util.Log.d("MainActivity", "System scroll ignored - screensaver active")
                                        return@postDelayed
                                    }
                                    android.util.Log.d("MainActivity", "System scroll detected")
                                    loadSystemImageDebounced()
                                }
                                AppConstants.Paths.GAME_FILENAME_LOG -> {
                                    // Ignore if launching from screensaver (game-select event between screensaver-end and game-start)
                                    if (isLaunchingFromScreensaver) {
                                        android.util.Log.d("MainActivity", "Game scroll ignored - launching from screensaver")
                                        return@postDelayed
                                    }

                                    // Ignore if screensaver is active
                                    if (state is AppState.Screensaver) {
                                        android.util.Log.d("MainActivity", "Game scroll ignored - screensaver active")
                                        return@postDelayed
                                    }

                                    // ADDED: Ignore game-select events that happen shortly after game-start or game-end
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastGameStartTime < GAME_EVENT_DEBOUNCE) {
                                        android.util.Log.d("MainActivity", "Game scroll ignored - too soon after game start")
                                        return@postDelayed
                                    }
                                    // Only debounce game start, not game end (users often want to scroll immediately after exiting)
                                    // Game end already sets state to GameBrowsing, so scrolling is safe

                                    // Read the game filename
                                    val gameFile = File(watchDir, AppConstants.Paths.GAME_FILENAME_LOG)
                                    if (gameFile.exists()) {
                                        val gameFilename = gameFile.readText().trim()

                                        // Ignore if this is the same game that's currently playing
                                        if (state is AppState.GamePlaying && gameFilename == (state as AppState.GamePlaying).gameFilename) {
                                            android.util.Log.d("MainActivity", "Game scroll ignored - same as playing game: $gameFilename")
                                            return@postDelayed
                                        }
                                    }

                                    android.util.Log.d("MainActivity", "Game scroll detected")
                                    loadGameInfoDebounced()
                                }
                                AppConstants.Paths.GAME_START_FILENAME_LOG -> {
                                    android.util.Log.d("MainActivity", "Game start detected")
                                    handleGameStart()
                                }
                                AppConstants.Paths.GAME_END_FILENAME_LOG -> {
                                    android.util.Log.d("MainActivity", "Game end detected")
                                    handleGameEnd()
                                }
                                AppConstants.Paths.SCREENSAVER_START_LOG -> {
                                    android.util.Log.d("MainActivity", "Screensaver start detected")
                                    handleScreensaverStart()
                                }
                                AppConstants.Paths.SCREENSAVER_END_LOG -> {
                                    // Read the screensaver end reason
                                    val screensaverEndFile = File(watchDir, AppConstants.Paths.SCREENSAVER_END_LOG)
                                    val endReason = if (screensaverEndFile.exists()) {
                                        screensaverEndFile.readText().trim()
                                    } else {
                                        "cancel"
                                    }

                                    android.util.Log.d("MainActivity", "Screensaver end detected: $endReason")
                                    handleScreensaverEnd(endReason)
                                }
                                AppConstants.Paths.SCREENSAVER_GAME_FILENAME_LOG -> {
                                    // DEFENSIVE FIX: Auto-initialize screensaver state if screensaver-start event was missed
                                    if (state !is AppState.Screensaver) {
                                        android.util.Log.w("MainActivity", "⚠️ FALLBACK: Screensaver game-select fired without screensaver-start event!")
                                        android.util.Log.w("MainActivity", "Auto-initializing screensaver state as defensive fallback")
                                        android.util.Log.d("MainActivity", "Current state before fallback: $state")

                                        // Create saved state for screensaver from current state
                                        val savedState = when (val s = state) {
                                            is AppState.SystemBrowsing -> {
                                                SavedBrowsingState.InSystemView(s.systemName)
                                            }
                                            is AppState.GameBrowsing -> {
                                                SavedBrowsingState.InGameView(
                                                    systemName = s.systemName,
                                                    gameFilename = s.gameFilename,
                                                    gameName = s.gameName
                                                )
                                            }
                                            else -> {
                                                // Fallback for unexpected states
                                                android.util.Log.w("MainActivity", "Unexpected state when screensaver fallback: $state")
                                                SavedBrowsingState.InSystemView(state.getCurrentSystemName() ?: "")
                                            }
                                        }

                                        // Apply screensaver behavior preferences BEFORE updating state
                                        val screensaverBehavior = prefsManager.screensaverBehavior
                                        android.util.Log.d("MainActivity", "Applying screensaver behavior: $screensaverBehavior")

                                        // Handle black screen preference
                                        if (screensaverBehavior == "black_screen") {
                                            android.util.Log.d("MainActivity", "Black screen behavior - clearing display")
                                            Glide.with(this@MainActivity).clear(gameImageView)
                                            gameImageView.setImageDrawable(null)
                                            gameImageView.visibility = View.GONE
                                            videoView.visibility = View.GONE
                                            releasePlayer()
                                            gridOverlayView?.visibility = View.GONE
                                        }

                                        // Stop any videos (matches handleScreensaverStart)
                                        releasePlayer()

                                        // CRITICAL: Clear all widgets immediately
                                        widgetContainer.removeAllViews()
                                        activeWidgets.clear()
                                        android.util.Log.d("MainActivity", "Fallback screensaver start - all widgets cleared")

                                        // Update grid overlay for screensaver state
                                        widgetContainer.visibility = View.VISIBLE
                                        updateGridOverlay()

                                        // CRITICAL: Reset initialization flag so first game event will initialize properly
                                        screensaverInitialized = false
                                        android.util.Log.d("MainActivity", "Reset screensaverInitialized flag - next game will be first")

                                        // NOW update state to Screensaver
                                        updateState(
                                            AppState.Screensaver(
                                                currentGame = null,  // No game selected yet
                                                previousState = savedState
                                            )
                                        )

                                        android.util.Log.d("MainActivity", "Fallback initialization complete - waiting for game data")
                                    }

                                    // Read screensaver game info and update state
                                    val filenameFile = File(watchDir, AppConstants.Paths.SCREENSAVER_GAME_FILENAME_LOG)
                                    val nameFile = File(watchDir, AppConstants.Paths.SCREENSAVER_GAME_NAME_LOG)
                                    val systemFile = File(watchDir, AppConstants.Paths.SCREENSAVER_GAME_SYSTEM_LOG)

                                    var gameFilename: String? = null
                                    var gameName: String? = null
                                    var systemName: String? = null

                                    if (filenameFile.exists()) {
                                        gameFilename = filenameFile.readText().trim()
                                    }
                                    if (nameFile.exists()) {
                                        gameName = nameFile.readText().trim()
                                    }
                                    if (systemFile.exists()) {
                                        systemName = systemFile.readText().trim()
                                    }

                                    // DEFENSIVE: Validate game data before updating state
                                    if (gameFilename.isNullOrBlank() || systemName.isNullOrBlank()) {
                                        android.util.Log.w("MainActivity", "⚠️ Incomplete screensaver game data - skipping update")
                                        android.util.Log.w("MainActivity", "  gameFilename: '${gameFilename ?: "NULL"}'")
                                        android.util.Log.w("MainActivity", "  systemName: '${systemName ?: "NULL"}'")
                                        return@postDelayed
                                    }

                                    // Update screensaver state with current game
                                    if (state is AppState.Screensaver) {
                                        val screensaverState = state as AppState.Screensaver
                                        updateState(screensaverState.copy(
                                            currentGame = ScreensaverGame(
                                                gameFilename = gameFilename,
                                                gameName = gameName,
                                                systemName = systemName
                                            )
                                        ))
                                        android.util.Log.d("MainActivity", "Screensaver game updated: $gameName ($gameFilename) - $systemName")
                                    } else {
                                        android.util.Log.w("MainActivity", "⚠️ Not in screensaver state after fallback init - state is: $state")
                                    }

                                    handleScreensaverGameSelect()
                                }
                            }
                        }, 50) // 50ms delay to ensure file is written
                    }
                }
            }
        }
        fileObserver?.startWatching()
        android.util.Log.d("MainActivity", "FileObserver started")
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                    // Close the app drawer if it's open
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                } else {
                    // Do nothing - stay on home screen
                    // This prevents cycling through recent apps when pressing back
                }
            }
        })
    }

    /**
     * Start waiting for script activity after configuration
     * Shows a "waiting" dialog and watches for first log update
     */
    fun startScriptVerification() {
        isWaitingForScriptVerification = true

        // Show "waiting" dialog
        showScriptVerificationDialog()

        // Set timeout
        if (scriptVerificationHandler == null) {
            scriptVerificationHandler = Handler(Looper.getMainLooper())
        }

        scriptVerificationRunnable = Runnable {
            if (isWaitingForScriptVerification) {
                // Timeout - scripts not working
                onScriptVerificationFailed()
            }
        }

        scriptVerificationHandler?.postDelayed(scriptVerificationRunnable!!, SCRIPT_VERIFICATION_TIMEOUT)
        android.util.Log.d("MainActivity", "Started script verification (15s timeout)")
    }

    /**
     * Stop verification (call when first log update detected)
     */
    private fun stopScriptVerification(success: Boolean) {
        scriptVerificationRunnable?.let {
            scriptVerificationHandler?.removeCallbacks(it)
        }
        isWaitingForScriptVerification = false

        // Dismiss waiting dialog if showing
        currentVerificationDialog?.dismiss()
        currentVerificationDialog = null

        // Dismiss error dialog if showing (user browsed while error was visible)
        currentErrorDialog?.dismiss()
        currentErrorDialog = null

        if (success) {
            onScriptVerificationSuccess()
        }
    }

    /**
     * Show dialog while waiting for script activity
     */
    private fun showScriptVerificationDialog() {
        currentVerificationDialog = AlertDialog.Builder(this)
            .setTitle("🔍 Checking Connection...")
            .setMessage("Waiting for ES-DE to send data...\n\n" +
                    "Please browse to a game or system in ES-DE now.\n\n" +
                    "This verifies that ES-DE scripts are working correctly.")
            .setCancelable(false)
            .setNegativeButton("Skip Check") { dialog, _ ->
                stopScriptVerification(false)
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Called when first log update is detected during verification
     */
    private fun onScriptVerificationSuccess() {
        runOnUiThread {
            Toast.makeText(
                this,
                "✓ Connection successful! ES-DE is communicating properly.",
                Toast.LENGTH_LONG
            ).show()

            // Check if this is first time seeing widget tutorial after setup
            val hasSeenWidgetTutorial = prefsManager.widgetTutorialShown
            val hasCompletedSetup = prefsManager.setupCompleted

            if (!hasSeenWidgetTutorial && hasCompletedSetup) {
                // Show widget tutorial after successful verification following setup
                android.util.Log.d("MainActivity", "Showing widget tutorial after setup verification")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showWidgetSystemTutorial(fromUpdate = false)
                }, AppConstants.Timing.SETTINGS_DELAY)  // 1 second after verification success
            }
        }
    }

    /**
     * Called when verification times out (no log updates)
     */
    private fun onScriptVerificationFailed() {
        runOnUiThread {
            currentVerificationDialog?.dismiss()

            // Create custom title view with X button
            val titleContainer = android.widget.LinearLayout(this)
            titleContainer.orientation = android.widget.LinearLayout.HORIZONTAL
            titleContainer.setPadding(60, 40, 20, 20)
            titleContainer.gravity = android.view.Gravity.CENTER_VERTICAL

            val titleText = android.widget.TextView(this)
            titleText.text = "⚠️ No Data Received"
            titleText.textSize = 20f
            titleText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
            titleText.layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            val closeButton = android.widget.ImageButton(this)
            closeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            closeButton.background = null
            closeButton.setPadding(20, 20, 20, 20)

            titleContainer.addView(titleText)
            titleContainer.addView(closeButton)

            val dialog = AlertDialog.Builder(this)
                .setCustomTitle(titleContainer)
                .setMessage("ES-DE Companion hasn't received any data from ES-DE.\n\n" +
                        "Common issues:\n\n" +
                        "1. Scripts folder path is incorrect\n" +
                        "   → Scripts must be in ES-DE's scripts folder\n\n" +
                        "2. Custom Event Scripts not enabled in ES-DE\n" +
                        "   → Main Menu > Other Settings > Toggle both:\n" +
                        "     • Custom Event Scripts: ON\n" +
                        "     • Browsing Custom Events: ON\n\n" +
                        "3. ES-DE not running or not browsing games\n" +
                        "   → Make sure you're scrolling through games\n\n" +
                        "What would you like to do?")
                .setNegativeButton("Restart Setup") { _, _ ->
                    currentErrorDialog = null  // Clear reference
                    // Launch settings with auto-start wizard flag
                    val intent = Intent(this, SettingsActivity::class.java)
                    intent.putExtra("AUTO_START_WIZARD", true)
                    settingsLauncher.launch(intent)
                }
                .setPositiveButton("Try Again") { _, _ ->
                    currentErrorDialog = null  // Clear reference
                    startScriptVerification()
                }
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create()

            closeButton.setOnClickListener {
                dialog.dismiss()
                currentErrorDialog = null  // Clear reference when manually closed
            }

            currentErrorDialog = dialog  // Store reference to error dialog
            dialog.show()
        }
    }

    override fun onDestroy() {
        // Cleanup managers first
        imageManager.cleanup()
        videoManager.cleanup()
        musicManager.cleanup()

        super.onDestroy()
        // Stop script verification
        scriptVerificationRunnable?.let { scriptVerificationHandler?.removeCallbacks(it) }
        currentVerificationDialog?.dismiss()
        currentErrorDialog?.dismiss()
        fileObserver?.stopWatching()
        unregisterReceiver(appChangeReceiver)
        unregisterVolumeListener()
        unregisterSecondaryVolumeObserver()
        // Cancel any pending image loads
        imageLoadRunnable?.let { imageLoadHandler.removeCallbacks(it) }
        // Release video player
        releasePlayer()
        // Cancel song title callbacks
        songTitleRunnable?.let { songTitleHandler?.removeCallbacks(it) }
    }

    /**
     * Hide song title overlay (called when music stops).
     */
    private fun hideSongTitle() {
        hideSongTitleOverlay()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hasWindowFocus = hasFocus

        if (hasFocus) {
            android.util.Log.d("MainActivity", "Window focus gained - restoring immersive mode")
            enableImmersiveMode()
        } else {
            android.util.Log.d("MainActivity", "Window focus lost (ignoring for video blocking)")
        }
    }

    /**
     * Enable immersive fullscreen mode to hide navigation and status bars.
     * Uses WindowInsetsController for Android 11+ (API 30+), falls back to
     * deprecated flags for Android 10 (API 29).
     */
    @Suppress("DEPRECATION")
    private fun enableImmersiveMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Modern approach for Android 11+ (API 30+)
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                // Hide both status bar and navigation bar
                controller.hide(
                    android.view.WindowInsets.Type.statusBars() or
                            android.view.WindowInsets.Type.navigationBars()
                )
                // Set behavior to show bars temporarily on swipe, then auto-hide
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Fallback for Android 10 (API 29)
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    /**
     * Debounced wrapper for loadSystemImage - delays loading based on scroll speed
     * Systems use debouncing to reduce rapid updates when scrolling quickly
     */
    private fun loadSystemImageDebounced() {
        // Calculate time since last system scroll event
        val currentTime = System.currentTimeMillis()
        val timeSinceLastScroll = currentTime - lastSystemScrollTime
        lastSystemScrollTime = currentTime

        // Determine if user is fast scrolling systems
        val isFastScrolling = timeSinceLastScroll < SYSTEM_FAST_SCROLL_THRESHOLD
        val delay = if (isFastScrolling) SYSTEM_FAST_SCROLL_DELAY else SYSTEM_SLOW_SCROLL_DELAY

        // Cancel any pending image load
        imageLoadRunnable?.let { imageLoadHandler.removeCallbacks(it) }

        // Schedule new image load with appropriate delay
        imageLoadRunnable = Runnable {
            loadSystemImage()
        }

        if (delay > 0) {
            imageLoadHandler.postDelayed(imageLoadRunnable!!, delay)
        } else {
            // Load immediately if no delay configured
            imageLoadRunnable!!.run()
        }
    }

    /**
     * Debounced wrapper for loadGameInfo - loads immediately with no delay
     * Games use instant loading for responsive browsing experience
     */
    private fun loadGameInfoDebounced() {
        // Calculate time since last game scroll event
        val currentTime = System.currentTimeMillis()
        val timeSinceLastScroll = currentTime - lastGameScrollTime
        lastGameScrollTime = currentTime

        // Determine if user is fast scrolling games
        val isFastScrolling = timeSinceLastScroll < GAME_FAST_SCROLL_THRESHOLD
        val delay = if (isFastScrolling) GAME_FAST_SCROLL_DELAY else GAME_SLOW_SCROLL_DELAY

        // Cancel any pending image load
        imageLoadRunnable?.let { imageLoadHandler.removeCallbacks(it) }

        // Schedule new image load with appropriate delay (0 for games = instant)
        imageLoadRunnable = Runnable {
            loadGameInfo()
        }

        if (delay > 0) {
            imageLoadHandler.postDelayed(imageLoadRunnable!!, delay)
        } else {
            // Load immediately for games (default behavior)
            imageLoadRunnable!!.run()
        }
    }

    /**
     * Load a built-in system logo SVG from assets folder
     * Handles both regular systems and ES-DE auto-collections
     * Returns drawable if found, null otherwise
     *
     * For bitmap-based custom logos (PNG, JPG, WEBP, GIF), returns null and expects
     * caller to use Glide for loading (which handles animated formats automatically)
     */
    fun loadSystemLogoFromAssets(systemName: String, width: Int = -1, height: Int = -1): android.graphics.drawable.Drawable? {
        return try {
            // Handle ES-DE auto-collections
            val baseFileName = when (systemName.lowercase()) {
                "allgames" -> "auto-allgames"
                "favorites" -> "auto-favorites"
                "lastplayed" -> "auto-lastplayed"
                else -> systemName.lowercase()
            }

            // First check user-provided system logos path with multiple format support
            val userLogosDir = File(getSystemLogosPath())
            if (userLogosDir.exists() && userLogosDir.isDirectory) {
                val extensions = listOf("svg", "png", "jpg", "jpeg", "webp", "gif")

                for (ext in extensions) {
                    val logoFile = File(userLogosDir, "$baseFileName.$ext")
                    if (logoFile.exists()) {
                        android.util.Log.d("MainActivity", "Found custom logo: $logoFile (extension: $ext)")

                        return when (ext) {
                            "svg" -> {
                                // Handle SVG files directly (as before)
                                android.util.Log.d("MainActivity", "Loading SVG logo from user path")
                                val svg = com.caverock.androidsvg.SVG.getFromInputStream(logoFile.inputStream())

                                if (width > 0 && height > 0) {
                                    // Create bitmap at target dimensions
                                    val bitmap = android.graphics.Bitmap.createBitmap(
                                        width,
                                        height,
                                        android.graphics.Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = android.graphics.Canvas(bitmap)

                                    val viewBox = svg.documentViewBox
                                    if (viewBox != null) {
                                        // SVG has viewBox - let AndroidSVG handle scaling
                                        svg.setDocumentWidth(width.toFloat())
                                        svg.setDocumentHeight(height.toFloat())
                                        svg.renderToCanvas(canvas)
                                        android.util.Log.d("MainActivity", "User SVG ($baseFileName) with viewBox rendered at ${width}x${height}")
                                    } else {
                                        // No viewBox - manually scale using document dimensions
                                        val docWidth = svg.documentWidth
                                        val docHeight = svg.documentHeight

                                        if (docWidth > 0 && docHeight > 0) {
                                            val scaleX = width.toFloat() / docWidth
                                            val scaleY = height.toFloat() / docHeight
                                            val scale = minOf(scaleX, scaleY)

                                            val scaledWidth = docWidth * scale
                                            val scaledHeight = docHeight * scale
                                            val translateX = (width - scaledWidth) / 2f
                                            val translateY = (height - scaledHeight) / 2f

                                            canvas.translate(translateX, translateY)
                                            canvas.scale(scale, scale)
                                            svg.renderToCanvas(canvas)
                                            android.util.Log.d("MainActivity", "User SVG ($baseFileName) no viewBox, scaled from ${docWidth}x${docHeight} to ${width}x${height}, scale: $scale")
                                        }
                                    }

                                    // Return drawable with no intrinsic dimensions
                                    object : android.graphics.drawable.BitmapDrawable(resources, bitmap) {
                                        override fun getIntrinsicWidth(): Int = -1
                                        override fun getIntrinsicHeight(): Int = -1
                                    }
                                } else {
                                    android.graphics.drawable.PictureDrawable(svg.renderToPicture())
                                }
                            }
                            else -> {
                                // For bitmap formats (PNG, JPG, WEBP, GIF), return null
                                // Caller will use Glide to load them, which supports animation
                                android.util.Log.d("MainActivity", "Bitmap-based custom logo detected - delegating to Glide for loading")
                                return null  // Signal to caller to use Glide
                            }
                        }
                    }
                }
            }

            // Fall back to built-in SVG assets
            val svgPath = "system_logos/$baseFileName.svg"
            val svg = try {
                com.caverock.androidsvg.SVG.getFromAsset(assets, svgPath)
            } catch (e: java.io.FileNotFoundException) {
                android.util.Log.w("MainActivity", "No built-in logo found for system: $baseFileName - using text fallback")
                return createTextFallbackDrawable(baseFileName, width, height)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error loading built-in logo for $baseFileName", e)
                return createTextFallbackDrawable(baseFileName, width, height)
            }

            if (width > 0 && height > 0) {
                // Create bitmap at target dimensions
                val bitmap = android.graphics.Bitmap.createBitmap(
                    width,
                    height,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)

                val viewBox = svg.documentViewBox
                if (viewBox != null) {
                    // SVG has viewBox - let AndroidSVG handle scaling
                    svg.setDocumentWidth(width.toFloat())
                    svg.setDocumentHeight(height.toFloat())
                    svg.renderToCanvas(canvas)
                    android.util.Log.d("MainActivity", "Built-in SVG ($baseFileName) with viewBox rendered at ${width}x${height}")
                } else {
                    // No viewBox - manually scale using document dimensions
                    val docWidth = svg.documentWidth
                    val docHeight = svg.documentHeight

                    if (docWidth > 0 && docHeight > 0) {
                        val scaleX = width.toFloat() / docWidth
                        val scaleY = height.toFloat() / docHeight
                        val scale = minOf(scaleX, scaleY)

                        val scaledWidth = docWidth * scale
                        val scaledHeight = docHeight * scale
                        val translateX = (width - scaledWidth) / 2f
                        val translateY = (height - scaledHeight) / 2f

                        canvas.translate(translateX, translateY)
                        canvas.scale(scale, scale)
                        svg.renderToCanvas(canvas)
                        android.util.Log.d("MainActivity", "Built-in SVG ($baseFileName) no viewBox, scaled from ${docWidth}x${docHeight} to ${width}x${height}, scale: $scale")
                    }
                }

                // Return drawable with no intrinsic dimensions
                object : android.graphics.drawable.BitmapDrawable(resources, bitmap) {
                    override fun getIntrinsicWidth(): Int = -1
                    override fun getIntrinsicHeight(): Int = -1
                }
            } else {
                android.graphics.drawable.PictureDrawable(svg.renderToPicture())
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Failed to load logo for $systemName", e)
            // Return text-based drawable as fallback
            createTextFallbackDrawable(systemName, width, height)
        }
    }

    /**
     * Create a text-based drawable as fallback for marquee images when no image is available
     * @param gameName The game name to display
     * @param width Target width in pixels (default 800 for marquees)
     * @param height Target height in pixels (default 300 for marquees)
     * @return A drawable with centered text on transparent background
     */
    fun createMarqueeTextFallback(
        gameName: String,
        width: Int = 800,
        height: Int = 300
    ): android.graphics.drawable.Drawable {
        // Clean up game name for display
        val displayName = gameName
            .replaceFirst(Regex("\\.[^.]+$"), "") // Remove file extension
            .replace(Regex("[_-]"), " ") // Replace underscores/hyphens with spaces
            .replace(Regex("\\s+"), " ") // Normalize multiple spaces
            .trim()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        val bitmap = android.graphics.Bitmap.createBitmap(
            width,
            height,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)

        // Leave background transparent (no background drawing)

        // Configure text paint
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = height * 0.20f // Start with 20% of height
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        // Word wrap logic with line limit
        val maxWidth = width * 1.0f
        val lineHeight = paint.textSize * 1.2f
        val maxLines = (height * 0.9f / lineHeight).toInt().coerceAtLeast(1) // Calculate how many lines fit

        val words = displayName.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    if (lines.size >= maxLines) break // Stop if we've reached max lines
                }
                currentLine = word
            }
        }

        // Handle the last line with ellipsis if needed
        if (currentLine.isNotEmpty()) {
            if (lines.size >= maxLines) {
                // Truncate last line with ellipsis
                val lastLine = lines[maxLines - 1]
                var truncated = lastLine
                while (paint.measureText("$truncated...") > maxWidth && truncated.isNotEmpty()) {
                    truncated = truncated.dropLast(1).trimEnd()
                }
                lines[maxLines - 1] = "$truncated..."
            } else {
                lines.add(currentLine)
            }
        }

        // Draw lines centered vertically
        val totalHeight = lines.size * lineHeight
        var yPos = (height - totalHeight) / 2f + lineHeight * 0.8f

        for (line in lines) {
            canvas.drawText(line, width / 2f, yPos, paint)
            yPos += lineHeight
        }

        return android.graphics.drawable.BitmapDrawable(resources, bitmap)
    }

    private fun createTextFallbackDrawable(
        systemName: String,
        width: Int = -1,
        height: Int = -1
    ): android.graphics.drawable.Drawable {
        // Clean up system name for display
        val displayName = systemName
            .replace("auto-", "")
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        // Create a bitmap to draw text on
        val targetWidth = if (width > 0) width else 400
        val targetHeight = if (height > 0) height else 200

        val bitmap = android.graphics.Bitmap.createBitmap(
            targetWidth,
            targetHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)

        // Configure text paint
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = targetHeight * 0.35f // Scale text to ~35% of height
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        // Draw text centered
        val xPos = targetWidth / 2f
        val yPos = (targetHeight / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(displayName, xPos, yPos, paint)

        return android.graphics.drawable.BitmapDrawable(resources, bitmap)
    }

    /**
     * Load a scaled bitmap to prevent out-of-memory errors with large images
     * @param imagePath Path to the image file
     * @param maxWidth Maximum width in pixels
     * @param maxHeight Maximum height in pixels
     * @return Scaled bitmap
     */
    private fun loadScaledBitmap(imagePath: String, maxWidth: Int, maxHeight: Int): android.graphics.Bitmap? {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = android.graphics.BitmapFactory.Options()
            options.inJustDecodeBounds = true
            android.graphics.BitmapFactory.decodeFile(imagePath, options)

            // Calculate inSampleSize
            val imageHeight = options.outHeight
            val imageWidth = options.outWidth
            var inSampleSize = 1

            if (imageHeight > maxHeight || imageWidth > maxWidth) {
                val halfHeight = imageHeight / 2
                val halfWidth = imageWidth / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) >= maxHeight && (halfWidth / inSampleSize) >= maxWidth) {
                    inSampleSize *= 2
                }
            }

            android.util.Log.d("MainActivity", "Loading image: $imagePath")
            android.util.Log.d("MainActivity", "  Original size: ${imageWidth}x${imageHeight}")
            android.util.Log.d("MainActivity", "  Sample size: $inSampleSize")
            android.util.Log.d("MainActivity", "  Target size: ~${imageWidth/inSampleSize}x${imageHeight/inSampleSize}")

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            options.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 // Use less memory

            return android.graphics.BitmapFactory.decodeFile(imagePath, options)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error loading scaled bitmap: $imagePath", e)
            return null
        }
    }

    /**
     * Get a signature for an image file to invalidate cache when file changes
     * Uses file's last modified time to detect changes
     */
    private fun getFileSignature(file: File): String {
        return if (file.exists()) {
            // Combine multiple signals for better cache invalidation
            "${file.lastModified()}_${file.length()}"
        } else {
            "0"
        }
    }

    /**
     * Create a text drawable for system name when no logo exists
     * Size is based on logo size setting
     */
    private fun createTextDrawable(systemName: String, logoSize: String): android.graphics.drawable.Drawable {
        // Determine text size based on logo size setting
        val textSizePx = when (logoSize) {
            "small" -> 90f
            "medium" -> 120f
            "large" -> 150f
            else -> 120f // default to medium
        }

        // Define max width wider than logo container sizes to reduce wrapping
        val maxWidthDp = when (logoSize) {
            "small" -> 400    // Back to original
            "large" -> 600    // Back to original
            else -> 500       // Back to original (medium)
        }
        val maxWidth = (maxWidthDp * resources.displayMetrics.density).toInt()

        // Create paint for text
        val textPaint = android.text.TextPaint().apply {
            color = android.graphics.Color.WHITE
            textSize = textSizePx
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }

        // Format system name (capitalize, replace underscores)
        val displayName = systemName
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

        // Create StaticLayout for multi-line text support
        val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.text.StaticLayout.Builder.obtain(
                displayName,
                0,
                displayName.length,
                textPaint,
                maxWidth
            )
                .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(8f, 1.0f) // Add some line spacing (8px extra)
                .setIncludePad(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            android.text.StaticLayout(
                displayName,
                textPaint,
                maxWidth,
                android.text.Layout.Alignment.ALIGN_CENTER,
                1.0f,
                8f,
                true
            )
        }

        // Calculate bitmap dimensions with generous padding
        val horizontalPadding = 100
        val verticalPadding = 60
        val width = staticLayout.width + (horizontalPadding * 2)
        val height = staticLayout.height + (verticalPadding * 2)

        // Create bitmap and draw text
        val bitmap = android.graphics.Bitmap.createBitmap(
            width,
            height,
            android.graphics.Bitmap.Config.ARGB_8888
        )

        val canvas = android.graphics.Canvas(bitmap)

        // Center the text layout on the canvas
        canvas.save()
        canvas.translate(
            horizontalPadding.toFloat(),
            verticalPadding.toFloat()
        )
        staticLayout.draw(canvas)
        canvas.restore()

        return android.graphics.drawable.BitmapDrawable(resources, bitmap)
    }


    private fun loadSystemImage() {
        // Don't reload images if game is currently playing - respect game launch behavior
        if (state is AppState.GamePlaying) {
            android.util.Log.d("MainActivity", "loadSystemImage blocked - game is playing, maintaining game launch display")
            return
        }

        try {
            // Stop any video playback when switching to system view
            releasePlayer()

            val logsDir = File(getLogsPath())
            val systemFile = File(logsDir, AppConstants.Paths.SYSTEM_NAME_LOG)
            if (!systemFile.exists()) return

            val systemName = systemFile.readText().trim()

            // Update state tracking
            updateState(AppState.SystemBrowsing(systemName))

            // CRITICAL: Check if solid color or custom image is selected for system view BEFORE checking for custom images
            val systemImagePref = prefsManager.systemViewBackgroundType
            if (systemImagePref == "solid_color") {
                val solidColor = prefsManager.systemBackgroundColor
                android.util.Log.d("MainActivity", "System view solid color selected - using color: ${String.format("#%06X", 0xFFFFFF and solidColor)}")
                val drawable = android.graphics.drawable.ColorDrawable(solidColor)
                gameImageView.setImageDrawable(drawable)
                gameImageView.visibility = View.VISIBLE

                // Update system widgets after setting solid color
                updateWidgetsForCurrentSystem()
                showWidgets()
                return
            }

            if (systemImagePref == "custom_image") {
                android.util.Log.d("MainActivity", "System view custom image selected - loading custom background")
                loadFallbackBackground(forceCustomImageOnly = true)

                // Update system widgets after loading custom image
                updateWidgetsForCurrentSystem()
                showWidgets()
                return
            }

            // Handle ES-DE auto-collections
            val baseFileName = when (systemName.lowercase()) {
                "all" -> "auto-allgames"
                "favorites" -> "auto-favorites"
                "recent" -> "auto-lastplayed"
                else -> systemName.lowercase()
            }

            // Check for custom system image with multiple format support
            var imageToUse: File? = null
            val systemImagePath = getSystemImagePath()
            val imageExtensions = listOf("webp", "png", "jpg", "jpeg", "gif")

            for (ext in imageExtensions) {
                val imageFile = File(systemImagePath, "$baseFileName.$ext")
                if (imageFile.exists()) {
                    imageToUse = imageFile
                    break
                }
            }

            if (imageToUse == null) {
                val prioritizedFolders = if (systemImagePref == "screenshot") {
                    listOf("screenshots", "fanart")
                } else {
                    listOf("fanart", "screenshots")
                }
                for (folder in prioritizedFolders) {
                    val randomImage = mediaManager.getRandomImageFromSystemFolder(systemName, folder)
                    if (randomImage != null) {
                        imageToUse = randomImage
                        break
                    }
                }
            }

            if (imageToUse != null && imageToUse.exists()) {
                // Check if this is a custom system image (from system_images folder)
                val isCustomSystemImage = imageToUse.absolutePath.contains(getSystemImagePath())

                if (isCustomSystemImage) {
                    // Check if it's an animated format
                    val extension = imageToUse.extension.lowercase()
                    val isAnimatedFormat = extension in listOf("webp", "gif")

                    if (isAnimatedFormat) {
                        // Use loadImageWithAnimation for animated formats (supports animation)
                        android.util.Log.d("MainActivity", "Loading animated custom system image via Glide")
                        loadImageWithAnimation(imageToUse, gameImageView)
                    } else {
                        // Load static custom system image with downscaling via ImageManager
                        android.util.Log.d("MainActivity", "Loading custom system image with downscaling")
                        imageManager.loadLargeImage(
                            imageView = gameImageView,
                            imagePath = imageToUse.absolutePath,
                            maxWidth = 1920,
                            maxHeight = 1080,
                            onLoaded = {
                                android.util.Log.d("MainActivity", "Custom system image loaded successfully")
                            },
                            onFailed = {
                                android.util.Log.e("MainActivity", "Failed to load custom system image, using fallback")
                                loadFallbackBackground()
                            }
                        )
                    }
                } else {
                    // Normal game artwork - use Glide with animation
                    loadImageWithAnimation(imageToUse, gameImageView)
                }
            } else {
                // No custom image and no game images found - show fallback
                loadFallbackBackground()
            }

            // Update system widgets after loading system image
            updateWidgetsForCurrentSystem()
            showWidgets()

        } catch (e: Exception) {
            // Don't clear images on exception - keep last valid images
            android.util.Log.e("MainActivity", "Error loading system image", e)
        }
    }

    private fun loadGameInfo() {
        // Don't reload images if game is currently playing - respect game launch behavior
        if (state is AppState.GamePlaying) {
            android.util.Log.d("MainActivity", "loadGameInfo blocked - game is playing, maintaining game launch display")
            return
        }

        // Cancel any in-flight coroutine from a previous scroll event so stale
        // results from earlier games can't overwrite the current game's assets
        val hadActiveJob = gameInfoJob?.isActive == true
        gameInfoJob?.cancel()
        if (hadActiveJob) android.util.Log.d("MainActivity", "loadGameInfo - cancelled in-flight job")

        // Launch coroutine to handle async file reading without blocking UI
        gameInfoJob = lifecycleScope.launch {
            try {
                val logsDir = File(getLogsPath())
                val gameFile = File(logsDir, AppConstants.Paths.GAME_FILENAME_LOG)
                if (!gameFile.exists()) return@launch

                // Use retry logic for filename too - FileObserver can fire before file is fully written
                val gameNameRaw = readNonBlankTextAsync(gameFile)
                if (gameNameRaw.isNullOrBlank()) {
                    android.util.Log.w("MainActivity", "Game filename empty after retries - skipping load")
                    return@launch
                }
                val gameName = sanitizeGameFilename(gameNameRaw).substringBeforeLast('.')

                // Read display name (may not exist yet due to race condition)
                val gameDisplayNameFile = File(logsDir, AppConstants.Paths.GAME_NAME_LOG)
                val gameDisplayName = readNonBlankTextAsync(gameDisplayNameFile) ?: gameName

                // CRITICAL: Wait for system file with retry logic (race condition fix)
                val systemFile = File(logsDir, AppConstants.Paths.GAME_SYSTEM_LOG)
                val systemName = readNonBlankTextAsync(systemFile)

                if (systemName == null) {
                    android.util.Log.w("MainActivity", "System name not available after retries - skipping game load")
                    return@launch
                }

                // Update state tracking (must be on main thread)
                withContext(Dispatchers.Main) {
                    updateState(
                        AppState.GameBrowsing(
                        systemName = systemName,
                        gameFilename = gameNameRaw,
                        gameName = gameDisplayName
                    ))

                    // CRITICAL: Check if solid color or custom image is selected for game view BEFORE trying to load game images
                    val gameImagePref = prefsManager.gameViewBackgroundType

                    if (gameImagePref == "solid_color") {
                        val solidColor = prefsManager.gameBackgroundColor
                        val drawable = android.graphics.drawable.ColorDrawable(solidColor)
                        gameImageView.setImageDrawable(drawable)
                    } else if (gameImagePref == "custom_image") {
                        // Custom image selected - always show custom background or built-in default
                        android.util.Log.d("MainActivity", "Game view custom image selected - loading custom background")
                        loadFallbackBackground(forceCustomImageOnly = true)
                    } else {
                        // Fanart or Screenshot preference - try to find game-specific artwork
                        val gameImage = findGameImage(systemName, gameNameRaw)

                        if (gameImage != null && gameImage.exists()) {
                            // Game has its own artwork - use it
                            loadImageWithAnimation(gameImage, gameImageView)
                        } else {
                            // No game artwork - show fallback background
                            loadFallbackBackground()
                        }
                    }

                    // Check if instant video will play (delay = 0)
                    val videoPath = mediaManager.findVideoFile(systemName, gameNameRaw)
                    val videoDelay = getVideoDelay()
                    val instantVideoWillPlay = videoPath != null && isVideoEnabled() && widgetsLocked && videoDelay == 0L

                    android.util.Log.d("MainActivity", "loadGameInfo - Video check:")
                    android.util.Log.d("MainActivity", "  videoPath: $videoPath")
                    android.util.Log.d("MainActivity", "  videoDelay: ${videoDelay}ms")
                    android.util.Log.d("MainActivity", "  instantVideoWillPlay: $instantVideoWillPlay")

                    // When user scrolls to a new game, stop the old game's video immediately
                    if (videoManager.stopCurrentVideoForNewGame()) {
                        android.util.Log.d("MainActivity", "Stopped old video - showing game image")
                        gameImageView.visibility = View.VISIBLE
                    }

                    // Update game widgets after determining video status
                    updateWidgetsForCurrentGame()

                    // Hide gameImageView BEFORE starting instant video to prevent background flash
                    if (instantVideoWillPlay) {
                        gameImageView.visibility = View.GONE
                        android.util.Log.d("MainActivity", "Pre-hiding gameImageView for instant video")
                    }

                    // Handle video playback for the current game
                    handleVideoForGame(systemName, gameName, gameNameRaw)

                    // Hide widgets ONLY if instant video is playing (delay = 0)
                    when (state) {
                        is AppState.GameBrowsing -> {
                            if (instantVideoWillPlay) {
                                hideWidgets()
                                android.util.Log.d("MainActivity", "Hiding widgets - instant video playing")
                            } else {
                                android.util.Log.d("MainActivity", "Keeping widgets shown - no instant video (delay=${videoDelay}ms)")
                            }
                        }
                        is AppState.Screensaver -> {
                            android.util.Log.d("MainActivity", "Not showing widgets - Screensaver active")
                        }
                        else -> {
                            android.util.Log.d("MainActivity", "Not showing widgets - state: $state")
                        }
                    }
                }

            } catch (e: Exception) {
                // Don't clear images on exception - keep last valid images
                android.util.Log.e("MainActivity", "Error loading game info", e)
            }
        }
    }

    /**
     * Read text from file with retry logic for race conditions.
     * CRITICAL: Uses coroutine delay instead of Thread.sleep to avoid blocking UI thread.
     *
     * @param file The file to read
     * @param retries Number of retry attempts (default: 5)
     * @param delayMs Delay between retries in milliseconds (default: 50ms)
     * @return The trimmed text content, or null if file never becomes readable
     */
    private suspend fun readNonBlankTextAsync(file: File, retries: Int = 5, delayMs: Long = 50): String? {
        repeat(retries) {
            if (file.exists()) {
                val text = file.readText().trim()
                if (text.isNotBlank()) return text
            }
            kotlinx.coroutines.delay(delayMs)
        }
        return null
    }

    private fun findGameImage(
        systemName: String,
        fullGamePath: String
    ): File? {
        // Get image preference
        val imagePref = prefsManager.gameViewBackgroundType

        // Return null if solid color is selected - handled in loadGameInfo()
        if (imagePref == "solid_color") {
            return null
        }

        val preferScreenshot = (imagePref == "screenshot")
        return mediaManager.findGameBackgroundImage(systemName, fullGamePath, preferScreenshot)
    }

    /**
     * Get the display ID that this activity is currently running on
     */
    /**
     * Log display information for debugging
     */
    private fun logDisplayInfo() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                val displayManager = getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
                val displays = displayManager.displays

                android.util.Log.d("MainActivity", "═══════════════════════════════════")
                android.util.Log.d("MainActivity", "DISPLAY INFORMATION AT STARTUP")
                android.util.Log.d("MainActivity", "═══════════════════════════════════")
                android.util.Log.d("MainActivity", "Total displays: ${displays.size}")
                displays.forEachIndexed { index, display ->
                    android.util.Log.d("MainActivity", "Display $index:")
                    android.util.Log.d("MainActivity", "  - ID: ${display.displayId}")
                    android.util.Log.d("MainActivity", "  - Name: ${display.name}")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        android.util.Log.d("MainActivity", "  - State: ${display.state}")
                    }
                }

                val currentDisplayId = getCurrentDisplayId()
                android.util.Log.d("MainActivity", "Companion app is on display: $currentDisplayId")
                android.util.Log.d("MainActivity", "═══════════════════════════════════")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error logging display info", e)
            }
        }
    }

    private fun getCurrentDisplayId(): Int {
        val displayId = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // Android 11+ - use display property
                val id1 = display?.displayId ?: -1
                android.util.Log.d("MainActivity", "  Method 1 (display): $id1")

                // Also try getting from window
                val id2 = window?.decorView?.display?.displayId ?: -1
                android.util.Log.d("MainActivity", "  Method 2 (window.decorView.display): $id2")

                // Use the non-negative one, prefer window method
                if (id2 >= 0) id2 else id1
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                // Android 4.2+ - use windowManager
                @Suppress("DEPRECATION")
                val id = windowManager.defaultDisplay.displayId
                android.util.Log.d("MainActivity", "  Method 3 (windowManager.defaultDisplay): $id")
                id
            } else {
                android.util.Log.d("MainActivity", "  Method 4 (fallback to 0)")
                0
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error getting display ID", e)
            0
        }

        android.util.Log.d("MainActivity", "getCurrentDisplayId() FINAL returning: $displayId (SDK: ${android.os.Build.VERSION.SDK_INT})")
        return if (displayId < 0) 0 else displayId
    }

    /**
     * Launch an app on the appropriate display based on user preferences
     */
    private fun launchApp(app: ResolveInfo) {
        val packageName = app.activityInfo?.packageName ?: return

        // Don't close drawer - just launch the app
        // Drawer will remain in its current state

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            val currentDisplayId = getCurrentDisplayId()
            val shouldLaunchOnTop = appLaunchPrefs.shouldLaunchOnTop(packageName)

            android.util.Log.d("MainActivity", "═══ LAUNCH REQUEST ═══")
            android.util.Log.d("MainActivity", "Companion detected on display: $currentDisplayId")
            android.util.Log.d("MainActivity", "User preference: ${if (shouldLaunchOnTop) "THIS screen" else "OTHER screen"}")

            // Get all available displays
            val targetDisplayId = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                try {
                    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
                    val displays = displayManager.displays

                    if (shouldLaunchOnTop) {
                        // Launch on THIS screen (same as companion)
                        android.util.Log.d("MainActivity", "Targeting THIS screen (display $currentDisplayId)")
                        currentDisplayId
                    } else {
                        // Launch on OTHER screen (find the display that's NOT current)
                        val otherDisplay = displays.firstOrNull { it.displayId != currentDisplayId }
                        if (otherDisplay != null) {
                            android.util.Log.d("MainActivity", "Targeting OTHER screen (display ${otherDisplay.displayId})")
                            otherDisplay.displayId
                        } else {
                            android.util.Log.w("MainActivity", "No other display found! Using current display")
                            currentDisplayId
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error finding target display", e)
                    currentDisplayId
                }
            } else {
                currentDisplayId
            }

            android.util.Log.d("MainActivity", "FINAL target: Display $targetDisplayId")
            android.util.Log.d("MainActivity", "═════════════════════")

            launchOnDisplay(launchIntent, targetDisplayId)

            // Close the app drawer after launching
            if (::bottomSheetBehavior.isInitialized) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }

    /**
     * Launch app on a specific display ID
     */
    private fun launchOnDisplay(intent: Intent, displayId: Int) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val displayManager = getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
                val displays = displayManager.displays

                android.util.Log.d("MainActivity", "launchOnDisplay: Requesting display $displayId")
                android.util.Log.d("MainActivity", "launchOnDisplay: Available displays: ${displays.size}")
                displays.forEachIndexed { index, display ->
                    android.util.Log.d("MainActivity", "  Display $index: ID=${display.displayId}, Name=${display.name}")
                }

                val targetDisplay = displays.firstOrNull { it.displayId == displayId }

                if (targetDisplay != null) {
                    android.util.Log.d("MainActivity", "✓ Found target display $displayId - Launching now")
                    val options = ActivityOptions.makeBasic()
                    options.launchDisplayId = displayId
                    startActivity(intent, options.toBundle())
                } else {
                    android.util.Log.w("MainActivity", "✗ Display $displayId not found! Launching on default")
                    startActivity(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error launching on display $displayId, using default", e)
                startActivity(intent)
            }
        } else {
            android.util.Log.d("MainActivity", "SDK < O, launching on default display")
            startActivity(intent)
        }
    }

    /**
     * Show app options dialog with launch position toggles
     * Note: "Top" now means "This screen" (same as companion)
     *       "Bottom" now means "Other screen" (opposite of companion)
     */
    private fun showAppOptionsDialog(app: ResolveInfo) {
        val packageName = app.activityInfo?.packageName ?: return
        val appName = app.loadLabel(packageManager).toString()

        val dialogView = layoutInflater.inflate(R.layout.dialog_app_options, null)
        val dialogAppName = dialogView.findViewById<TextView>(R.id.dialogAppName)
        val btnAppInfo = dialogView.findViewById<MaterialButton>(R.id.btnAppInfo)
        val btnHideApp = dialogView.findViewById<MaterialButton>(R.id.btnHideApp)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.launchPositionChipGroup)
        val chipLaunchTop = dialogView.findViewById<Chip>(R.id.chipLaunchTop)
        val chipLaunchBottom = dialogView.findViewById<Chip>(R.id.chipLaunchBottom)

        dialogAppName.text = appName

        // Check if app is currently hidden and update button
        val hiddenApps = prefsManager.hiddenApps.toMutableSet()
        val isHidden = hiddenApps.contains(packageName)

        if (isHidden) {
            btnHideApp.text = "Unhide App"
            btnHideApp.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#4CAF50")
            )
        } else {
            btnHideApp.text = "Hide App"
            btnHideApp.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#CF6679")
            )
        }

        // Set initial chip state
        val currentPosition = appLaunchPrefs.getLaunchPosition(packageName)
        if (currentPosition == AppLaunchManager.POSITION_TOP) {
            chipLaunchTop.isChecked = true
        } else {
            chipLaunchBottom.isChecked = true
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // App Info button
        btnAppInfo.setOnClickListener {
            openAppInfo(packageName)
            dialog.dismiss()
        }

        // Hide/Unhide App button
        btnHideApp.setOnClickListener {
            val currentHiddenApps = prefsManager.hiddenApps.toMutableSet()
            val currentlyHidden = currentHiddenApps.contains(packageName)

            if (currentlyHidden) {
                // Unhide - no confirmation
                currentHiddenApps.remove(packageName)
                prefsManager.hiddenApps = currentHiddenApps
                dialog.dismiss()

                val mainIntent = Intent(Intent.ACTION_MAIN, null)
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                val updatedHiddenApps = prefsManager.hiddenApps
                allApps = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
                    .filter { !updatedHiddenApps.contains(it.activityInfo?.packageName ?: "") }
                    .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

                (appRecyclerView.adapter as? AppAdapter)?.updateApps(allApps)
                Toast.makeText(this, "\"$appName\" shown in app drawer", Toast.LENGTH_SHORT).show()
            } else {
                // Hide - show confirmation
                AlertDialog.Builder(this)
                    .setTitle("Hide App")
                    .setMessage("Hide \"$appName\" from the app drawer?\n\nYou can unhide it later from Settings → App Drawer → Manage Apps, or by searching for it.")
                    .setPositiveButton("Hide") { _, _ ->
                        currentHiddenApps.add(packageName)
                        prefsManager.hiddenApps = currentHiddenApps
                        dialog.dismiss()

                        val mainIntent = Intent(Intent.ACTION_MAIN, null)
                        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                        val updatedHiddenApps = prefsManager.hiddenApps
                        allApps = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
                            .filter { !updatedHiddenApps.contains(it.activityInfo?.packageName ?: "") }
                            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

                        (appRecyclerView.adapter as? AppAdapter)?.updateApps(allApps)
                        Toast.makeText(this, "\"$appName\" hidden from app drawer", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_menu_delete)
                    .show()
            }
        }

        // Chip selection listener - save preference AND launch app
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            when {
                checkedIds.contains(R.id.chipLaunchTop) -> {
                    // Save preference
                    appLaunchPrefs.setLaunchPosition(packageName, AppLaunchManager.POSITION_TOP)
                    android.util.Log.d("MainActivity", "Set $appName to launch on THIS screen")

                    // Launch the app
                    launchApp(app)

                    // Close dialog and drawer
                    dialog.dismiss()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

                    // Refresh indicators
                    (appRecyclerView.adapter as? AppAdapter)?.refreshIndicators()
                }
                checkedIds.contains(R.id.chipLaunchBottom) -> {
                    // Save preference
                    appLaunchPrefs.setLaunchPosition(packageName, AppLaunchManager.POSITION_BOTTOM)
                    android.util.Log.d("MainActivity", "Set $appName to launch on OTHER screen")

                    // Launch the app
                    launchApp(app)

                    // Close dialog and drawer
                    dialog.dismiss()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

                    // Refresh indicators
                    (appRecyclerView.adapter as? AppAdapter)?.refreshIndicators()
                }
            }
        }

        dialog.show()
    }

    /**
     * Open system app info screen
     */
    private fun openAppInfo(packageName: String) {
        try {
            // This launches Android system settings - pause music
            isNavigatingInternally = false

            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to open app info", e)
        }
    }

    // ========== GAME STATE FUNCTIONS ==========

    private fun handleGameStart() {
        lastGameStartTime = System.currentTimeMillis()

        android.util.Log.d("MainActivity", "gameImageView.visibility at game start: ${gameImageView.visibility}")
        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        android.util.Log.d("MainActivity", "GAME START HANDLER")
        android.util.Log.d("MainActivity", "Current state: $state")

        // Read game info from log files written by ES-DE scripts
        val logsDir = File(getLogsPath())
        val filenameFile = File(logsDir, AppConstants.Paths.GAME_START_FILENAME_LOG)
        val nameFile = File(logsDir, AppConstants.Paths.GAME_START_NAME_LOG)
        val systemFile = File(logsDir, AppConstants.Paths.GAME_START_SYSTEM_LOG)

        // Read and sanitize the filenames
        val rawGameFilename = if (filenameFile.exists()) filenameFile.readText().trim() else null
        val gameFilename = rawGameFilename?.let { sanitizeGameFilename(it) }
        val gameName = if (nameFile.exists()) nameFile.readText().trim() else null
        val systemName = if (systemFile.exists()) systemFile.readText().trim() else null

        android.util.Log.d("MainActivity", "Game start from logs:")
        android.util.Log.d("MainActivity", "  Raw filename: $rawGameFilename")
        android.util.Log.d("MainActivity", "  Sanitized filename: $gameFilename")
        android.util.Log.d("MainActivity", "  Game name: $gameName")
        android.util.Log.d("MainActivity", "  System: $systemName")

        // Get the game launch behavior
        val gameLaunchBehavior = prefsManager.gameLaunchBehavior
        android.util.Log.d("MainActivity", "Game launch behavior: $gameLaunchBehavior")

        // CRITICAL: If black screen, clear everything IMMEDIATELY
        if (gameLaunchBehavior == "black_screen") {
            applyBlackScreenGameLaunch()
        }

        // Use log file data if available, otherwise fall back to current state
        val gameInfo = if (gameFilename != null && systemName != null) {
            Pair(systemName, gameFilename)
        } else {
            android.util.Log.w("MainActivity", "Log files missing or incomplete, using state fallback")
            extractGameInfoFromState()
        }

        // Update state to GamePlaying
        if (gameInfo != null) {
            val (sysName, filename) = gameInfo
            updateState(
                AppState.GamePlaying(
                systemName = sysName,
                gameFilename = filename
            ))
            android.util.Log.d("MainActivity", "State updated to GamePlaying: $filename")
        } else {
            android.util.Log.e("MainActivity", "Could not determine game info for GamePlaying state")
        }

        // Handle screensaver transition - if display is already correct, skip
        if (handleGameStartFromScreensaver(gameLaunchBehavior)) {
            android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            return
        }

        // Apply game launch behavior
        applyGameLaunchBehavior(gameLaunchBehavior, gameInfo)

        // Stop any videos
        releasePlayer()

        // Clear screensaver launch flag
        isLaunchingFromScreensaver = false

        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    // ========== START: Game Start Handler Extraction ==========

    /**
     * Handle black screen game launch behavior
     */
    private fun applyBlackScreenGameLaunch() {
        android.util.Log.d("MainActivity", "Black screen behavior - clearing display immediately")
        Glide.with(this).clear(gameImageView)
        gameImageView.setImageDrawable(null)
        gameImageView.visibility = View.GONE
        videoView.visibility = View.GONE
        hideWidgets()
        releasePlayer()
    }

    /**
     * Extract game info from current state for game launch
     * @return Pair of (systemName, gameFilename) or null if unavailable
     */
    private fun extractGameInfoFromState(): Pair<String, String>? {
        return when (val s = state) {
            is AppState.GameBrowsing -> {
                // Normal game launch from browsing
                Pair(s.systemName, s.gameFilename)
            }
            is AppState.Screensaver -> {
                // Game launch from screensaver
                s.currentGame?.let { game ->
                    Pair(game.systemName, game.gameFilename)
                }
            }
            else -> {
                // Shouldn't happen, but try to read from log files as fallback
                android.util.Log.w("MainActivity", "Game start from unexpected state: $state")
                tryReadGameInfoFromLogs()
            }
        }
    }

    /**
     * Fallback: Try to read game info from log files
     * @return Pair of (systemName, gameFilename) or null if unavailable
     */
    private fun tryReadGameInfoFromLogs(): Pair<String, String>? {
        val logsDir = File(getLogsPath())
        val gameFile = File(logsDir, AppConstants.Paths.GAME_FILENAME_LOG)
        val systemFile = File(logsDir, AppConstants.Paths.GAME_SYSTEM_LOG)

        return if (gameFile.exists() && systemFile.exists()) {
            Pair(systemFile.readText().trim(), gameFile.readText().trim())
        } else {
            null
        }
    }

    /**
     * Handle game start when coming from screensaver
     * @return true if display is already correct and no further action needed
     */
    private fun handleGameStartFromScreensaver(gameLaunchBehavior: String): Boolean {
        if (!isLaunchingFromScreensaver) {
            return false
        }

        android.util.Log.d("MainActivity", "Game start from screensaver")

        val screensaverBehavior = prefsManager.screensaverBehavior

        // If both behaviors match, display is already correct
        if (screensaverBehavior == gameLaunchBehavior) {
            android.util.Log.d("MainActivity", "Same behavior ($gameLaunchBehavior) - keeping current display")
            isLaunchingFromScreensaver = false
            return true // Skip further processing
        }

        android.util.Log.d("MainActivity", "Different behaviors - screensaver: $screensaverBehavior, game: $gameLaunchBehavior")
        return false // Need to update display
    }

    /**
     * Apply game launch behavior based on settings
     */
    private fun applyGameLaunchBehavior(gameLaunchBehavior: String, gameInfo: Pair<String, String>?) {
        when (gameLaunchBehavior) {
            "black_screen" -> {
                // Already handled at the top of handleGameStart()
                android.util.Log.d("MainActivity", "Black screen - already cleared")
            }
            "default_image" -> {
                android.util.Log.d("MainActivity", "Default image behavior - loading fallback")
                loadFallbackBackground(forceCustomImageOnly = true)
                gameImageView.visibility = View.VISIBLE
                videoView.visibility = View.GONE

                // Load and show widgets directly (can't use updateWidgetsForCurrentGame because state is GamePlaying)
                if (gameInfo != null) {
                    val (systemName, gameFilename) = gameInfo
                    android.util.Log.d("MainActivity", "Loading widgets for default_image behavior")
                    loadGameWidgets(systemName, gameFilename)
                    showWidgets()
                }
            }
            "game_image" -> {
                android.util.Log.d("MainActivity", "Game image behavior - keeping current game display")

                if (gameInfo != null) {
                    val (systemName, gameFilename) = gameInfo

                    // Check if solid color is selected for game view
                    val gameImagePref = prefsManager.gameViewBackgroundType
                    if (gameImagePref == "solid_color") {
                        val solidColor = prefsManager.gameBackgroundColor
                        android.util.Log.d("MainActivity", "Game view solid color selected - using color: ${String.format("#%06X", 0xFFFFFF and solidColor)}")
                        val drawable = android.graphics.drawable.ColorDrawable(solidColor)
                        gameImageView.setImageDrawable(drawable)
                        gameImageView.visibility = View.VISIBLE
                        videoView.visibility = View.GONE

                        // Load and show widgets
                        android.util.Log.d("MainActivity", "Loading widgets for game_image behavior (solid color)")
                        loadGameWidgets(systemName, gameFilename)
                        showWidgets()
                    } else if (gameImagePref == "custom_image") {
                        android.util.Log.d("MainActivity", "Game view custom image selected - loading custom background")
                        loadFallbackBackground(forceCustomImageOnly = true)
                        gameImageView.visibility = View.VISIBLE
                        videoView.visibility = View.GONE

                        // Load and show widgets
                        android.util.Log.d("MainActivity", "Loading widgets for game_image behavior (custom image)")
                        loadGameWidgets(systemName, gameFilename)
                        showWidgets()
                    } else {
                        // Fanart or Screenshot preference
                        val gameImage = findGameImage(systemName, gameFilename)

                        if (gameImage != null && gameImage.exists()) {
                            android.util.Log.d("MainActivity", "Loading game image: ${gameImage.name}")
                            loadImageWithAnimation(gameImage, gameImageView)
                        } else {
                            android.util.Log.d("MainActivity", "No game artwork found, using fallback")
                            loadFallbackBackground()
                        }

                        gameImageView.visibility = View.VISIBLE
                        videoView.visibility = View.GONE

                        // Load and show widgets
                        android.util.Log.d("MainActivity", "Loading widgets for game_image behavior (fanart/screenshot)")
                        loadGameWidgets(systemName, gameFilename)
                        showWidgets()
                    }
                } else {
                    android.util.Log.d("MainActivity", "No game info available, using fallback")
                    loadFallbackBackground()
                    gameImageView.visibility = View.VISIBLE
                    videoView.visibility = View.GONE
                }
            }
        }
    }

// ========== END: Game Start Handler Extraction ==========

    /**
     * Handle game end event - return to normal browsing display
     */
    private fun handleGameEnd() {
        lastGameEndTime = System.currentTimeMillis()

        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        android.util.Log.d("MainActivity", "GAME END EVENT")
        android.util.Log.d("MainActivity", "Current state: $state")
        android.util.Log.d("MainActivity", "gameImageView visibility: ${gameImageView.visibility}")

        // Read game info from log files written by ES-DE scripts
        val logsDir = File(getLogsPath())
        val filenameFile = File(logsDir, AppConstants.Paths.GAME_END_FILENAME_LOG)
        val nameFile = File(logsDir, AppConstants.Paths.GAME_END_NAME_LOG)
        val systemFile = File(logsDir, AppConstants.Paths.GAME_END_SYSTEM_LOG)

        // Read and sanitize the filenames
        val rawGameFilename = if (filenameFile.exists()) filenameFile.readText().trim() else null
        val gameFilename = rawGameFilename?.let { sanitizeGameFilename(it) }
        val gameName = if (nameFile.exists()) nameFile.readText().trim() else null
        val systemName = if (systemFile.exists()) systemFile.readText().trim() else null

        android.util.Log.d("MainActivity", "Game end from logs:")
        android.util.Log.d("MainActivity", "  Raw filename: $rawGameFilename")
        android.util.Log.d("MainActivity", "  Sanitized filename: $gameFilename")
        android.util.Log.d("MainActivity", "  Game name: $gameName")
        android.util.Log.d("MainActivity", "  System: $systemName")

        // Update state - transition from GamePlaying to GameBrowsing
        // Return to browsing the game that was just playing
        if (state is AppState.GamePlaying) {
            val playingState = state as AppState.GamePlaying
            updateState(
                AppState.GameBrowsing(
                systemName = systemName ?: playingState.systemName,
                gameFilename = gameFilename ?: playingState.gameFilename,
                gameName = gameName  // Use parsed name from script
            ))
            android.util.Log.d("MainActivity", "State updated to GameBrowsing: $gameFilename")
        } else {
            android.util.Log.w("MainActivity", "Game end but not in GamePlaying state: $state")
        }

        // Determine how to handle display after game end
        val gameLaunchBehavior = prefsManager.gameLaunchBehavior
        android.util.Log.d("MainActivity", "Game launch behavior: $gameLaunchBehavior")

        // Now handle the reload based on state and behavior
        when (state) {
            is AppState.GameBrowsing -> {
                // After game end, we're browsing the game we just played
                if (gameLaunchBehavior == "game_image") {
                    // If behavior is game_image, display is already correct
                    android.util.Log.d("MainActivity", "Game image behavior - display already correct")
                } else {
                    // Otherwise reload to show normal game browsing view
                    android.util.Log.d("MainActivity", "Reloading display (behavior: $gameLaunchBehavior)")
                    loadGameInfo()
                }
            }
            is AppState.SystemBrowsing -> {
                // In system view - reload system image
                android.util.Log.d("MainActivity", "Reloading system image after game end")
                loadSystemImage()
            }
            else -> {
                // Shouldn't happen, but handle gracefully
                android.util.Log.w("MainActivity", "Unexpected state after game end: $state")
                loadGameInfo()
            }
        }

        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    // ========== SCREENSAVER FUNCTIONS ==========

    /**
     * Handle screensaver start event
     */
    private fun handleScreensaverStart() {
        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        android.util.Log.d("MainActivity", "SCREENSAVER START")
        android.util.Log.d("MainActivity", "Current state before: $state")

        // Create saved state from current state
        val previousState = when (val s = state) {
            is AppState.SystemBrowsing -> {
                SavedBrowsingState.InSystemView(
                    systemName = s.systemName
                )
            }
            is AppState.GameBrowsing -> {
                SavedBrowsingState.InGameView(
                    systemName = s.systemName,
                    gameFilename = s.gameFilename,
                    gameName = s.gameName
                )
            }
            else -> {
                // Fallback for unexpected states
                android.util.Log.w("MainActivity", "Unexpected state when screensaver starts: $state")
                SavedBrowsingState.InSystemView(state.getCurrentSystemName() ?: "")
            }
        }

        // Update state to Screensaver
        updateState(
            AppState.Screensaver(
            currentGame = null,  // No game selected yet
            previousState = previousState
        ))

        android.util.Log.d("MainActivity", "Saved previous state: $previousState")

        val screensaverBehavior = prefsManager.screensaverBehavior
        android.util.Log.d("MainActivity", "Screensaver behavior preference: $screensaverBehavior")
        android.util.Log.d("MainActivity", "Current state after: $state")
        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Reset screensaver initialization flag
        screensaverInitialized = false

        // CRITICAL: If black screen, clear everything IMMEDIATELY
        if (screensaverBehavior == "black_screen") {
            android.util.Log.d("MainActivity", "Black screen behavior - clearing display immediately")
            Glide.with(this).clear(gameImageView)
            gameImageView.setImageDrawable(null)
            gameImageView.visibility = View.GONE
            videoView.visibility = View.GONE
            hideWidgets()
            releasePlayer()
            // Hide grid for black screen
            gridOverlayView?.visibility = View.GONE
            return  // Exit early, don't process anything else
        }

        when (screensaverBehavior) {
            "game_image" -> {
                // Game images will be loaded by handleScreensaverGameSelect events
                android.util.Log.d("MainActivity", "Screensaver behavior: game_image - waiting for game select events")
                android.util.Log.d("MainActivity", "  - Will load game images when screensaver-game-select events arrive")
                android.util.Log.d("MainActivity", "  - gameImageView visibility: ${gameImageView.visibility}")
                android.util.Log.d("MainActivity", "  - videoView visibility: ${videoView.visibility}")
            }
            "default_image" -> {
                // Show default/fallback image immediately
                android.util.Log.d("MainActivity", "Screensaver behavior: default_image")
                // loadFallbackBackground()
                // gameImageView.visibility = View.VISIBLE
                // videoView.visibility = View.GONE
            }
        }

        // Stop any videos
        releasePlayer()

        // CRITICAL: Clear all widgets immediately when screensaver starts
        // Game widgets will be loaded by handleScreensaverGameSelect when first game is selected
        widgetContainer.removeAllViews()
        activeWidgets.clear()
        android.util.Log.d("MainActivity", "Screensaver started - all widgets cleared, waiting for game-select")

        // Update grid overlay for screensaver state (for game_image and default_image)
        widgetContainer.visibility = View.VISIBLE
        updateGridOverlay()
    }

    /**
     * Apply screensaver behavior change while screensaver is already active.
     * Unlike handleScreensaverStart(), this preserves the current screensaver game.
     */
    private fun applyScreensaverBehaviorChange() {
        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        android.util.Log.d("MainActivity", "SCREENSAVER BEHAVIOR CHANGE")
        android.util.Log.d("MainActivity", "Current state: $state")

        val screensaverBehavior = prefsManager.screensaverBehavior
        android.util.Log.d("MainActivity", "New screensaver behavior: $screensaverBehavior")

        // Get current screensaver game (if any)
        val screensaverGame = if (state is AppState.Screensaver) {
            (state as AppState.Screensaver).currentGame
        } else {
            null
        }

        when (screensaverBehavior) {
            "black_screen" -> {
                android.util.Log.d("MainActivity", "Switching to black screen")
                Glide.with(this).clear(gameImageView)
                gameImageView.setImageDrawable(null)
                gameImageView.visibility = View.GONE
                videoView.visibility = View.GONE
                hideWidgets()
                releasePlayer()
                gridOverlayView?.visibility = View.GONE
            }
            "default_image" -> {
                android.util.Log.d("MainActivity", "Switching to default image")
                loadFallbackBackground(forceCustomImageOnly = true)
                gameImageView.visibility = View.VISIBLE
                videoView.visibility = View.GONE

                // Show current game widgets if we have a game
                if (screensaverGame != null) {
                    loadGameWidgets(screensaverGame.systemName, screensaverGame.gameFilename)
                    showWidgets()
                } else {
                    hideWidgets()
                }

                releasePlayer()
            }
            "game_image" -> {
                android.util.Log.d("MainActivity", "Switching to game image")

                // If we have a current screensaver game, load it
                if (screensaverGame != null) {
                    // Check if solid color is selected for game view
                    val gameImagePref = prefsManager.gameViewBackgroundType
                    if (gameImagePref == "solid_color") {
                        val solidColor = prefsManager.gameBackgroundColor
                        android.util.Log.d("MainActivity", "Game view solid color selected - using color: ${String.format("#%06X", 0xFFFFFF and solidColor)}")
                        val drawable = android.graphics.drawable.ColorDrawable(solidColor)
                        gameImageView.setImageDrawable(drawable)
                        gameImageView.visibility = View.VISIBLE
                        videoView.visibility = View.GONE

                        // Load and show widgets
                        loadGameWidgets(screensaverGame.systemName, screensaverGame.gameFilename)
                        showWidgets()
                    } else if (gameImagePref == "custom_image") {
                        android.util.Log.d("MainActivity", "Game view custom image selected - loading custom background")
                        loadFallbackBackground(forceCustomImageOnly = true)
                        gameImageView.visibility = View.VISIBLE
                        videoView.visibility = View.GONE

                        // Load and show widgets
                        loadGameWidgets(screensaverGame.systemName, screensaverGame.gameFilename)
                        showWidgets()
                    } else {
                        // Fanart or Screenshot preference
                        val gameImage = findGameImage(screensaverGame.systemName, screensaverGame.gameFilename)

                        if (gameImage != null && gameImage.exists()) {
                            android.util.Log.d("MainActivity", "Loading current screensaver game image: ${gameImage.name}")
                            loadImageWithAnimation(gameImage, gameImageView)
                        } else {
                            android.util.Log.d("MainActivity", "No game artwork found, using fallback")
                            loadFallbackBackground()
                        }

                        gameImageView.visibility = View.VISIBLE
                        videoView.visibility = View.GONE

                        // Load and show widgets
                        loadGameWidgets(screensaverGame.systemName, screensaverGame.gameFilename)
                        showWidgets()
                    }
                } else {
                    // No game selected yet - just wait
                    android.util.Log.d("MainActivity", "No screensaver game yet - display will update on next game-select")
                }

                releasePlayer()
            }
        }

        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /**
     * Handle screensaver end event - return to normal browsing display
     * @param reason The reason for screensaver ending: "cancel", "game-jump", or "game-start"
     */
    private fun handleScreensaverEnd(reason: String?) {
        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        android.util.Log.d("MainActivity", "SCREENSAVER END: reason=$reason")
        android.util.Log.d("MainActivity", "Current state: $state")

        // Get previous state from screensaver
        val previousState = if (state is AppState.Screensaver) {
            (state as AppState.Screensaver).previousState
        } else {
            // Fallback if state tracking wasn't initialized
            android.util.Log.w("MainActivity", "Not in Screensaver state, using fallback")
            SavedBrowsingState.InSystemView(state.getCurrentSystemName() ?: "")
        }

        // Get current screensaver game info (if any)
        val screensaverGame = if (state is AppState.Screensaver) {
            (state as AppState.Screensaver).currentGame
        } else {
            null
        }

        android.util.Log.d("MainActivity", "Previous state before screensaver: $previousState")
        android.util.Log.d("MainActivity", "Current screensaver game: $screensaverGame")
        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Reset screensaver initialization flag
        screensaverInitialized = false

        if (reason != null) {
            when (reason) {
                "game-start" -> {
                    // CRITICAL: Set flag IMMEDIATELY to block FileObserver reloads during transition
                    isLaunchingFromScreensaver = true

                    // User is launching a game from screensaver
                    android.util.Log.d("MainActivity", "Screensaver end - game starting, waiting for game-start event")
                    android.util.Log.d("MainActivity", "isLaunchingFromScreensaver flag set - blocking intermediate reloads")

                    // Update state - transition to GameBrowsing (waiting for GamePlaying)
                    if (screensaverGame != null) {
                        updateState(
                            AppState.GameBrowsing(
                            systemName = screensaverGame.systemName,
                            gameFilename = screensaverGame.gameFilename,
                            gameName = screensaverGame.gameName
                        ))
                        android.util.Log.d("MainActivity", "Transitioned to GameBrowsing: ${screensaverGame.gameFilename}")
                    } else {
                        android.util.Log.w("MainActivity", "No screensaver game info available")
                    }

                    // The game-start event will handle the display
                    // Flag will be cleared in handleGameStart()
                }
                "game-jump" -> {
                    // User jumped to a different game while in screensaver
                    // The game is now the selected game, so image can be retained
                    android.util.Log.d("MainActivity", "Screensaver end - game-jump, retaining current image")

                    // Update state - transition to GameBrowsing
                    if (screensaverGame != null) {
                        updateState(
                            AppState.GameBrowsing(
                            systemName = screensaverGame.systemName,
                            gameFilename = screensaverGame.gameFilename,
                            gameName = screensaverGame.gameName
                        ))
                        android.util.Log.d("MainActivity", "Transitioned to GameBrowsing: ${screensaverGame.gameFilename}")
                    } else {
                        android.util.Log.w("MainActivity", "No screensaver game info available")
                    }

                    // The current screensaver game image is already showing, so don't reload
                }
                "cancel" -> {
                    // User cancelled screensaver (pressed back or timeout)
                    // Return to the browsing state from before screensaver started
                    android.util.Log.d("MainActivity", "Screensaver end - cancel, returning to previous state")

                    // Return to previous state
                    when (previousState) {
                        is SavedBrowsingState.InSystemView -> {
                            android.util.Log.d("MainActivity", "Returning to system view: ${previousState.systemName}")

                            // Update state first
                            updateState(AppState.SystemBrowsing(previousState.systemName))

                            // Then reload display
                            loadSystemImage()
                        }
                        is SavedBrowsingState.InGameView -> {
                            android.util.Log.d("MainActivity", "Returning to game view: ${previousState.gameFilename}")

                            // Update state first
                            updateState(
                                AppState.GameBrowsing(
                                systemName = previousState.systemName,
                                gameFilename = previousState.gameFilename,
                                gameName = previousState.gameName
                            ))

                            // Then reload display
                            loadGameInfo()
                        }
                    }
                }
                else -> {
                    // Unknown reason - default to cancel behavior
                    android.util.Log.w("MainActivity", "Screensaver end - unknown reason: $reason, defaulting to cancel behavior")

                    // Return to previous state (same as cancel)
                    when (previousState) {
                        is SavedBrowsingState.InSystemView -> {
                            android.util.Log.d("MainActivity", "Returning to system view: ${previousState.systemName}")

                            updateState(AppState.SystemBrowsing(previousState.systemName))
                            loadSystemImage()
                        }
                        is SavedBrowsingState.InGameView -> {
                            android.util.Log.d("MainActivity", "Returning to game view: ${previousState.gameFilename}")

                            updateState(
                                AppState.GameBrowsing(
                                systemName = previousState.systemName,
                                gameFilename = previousState.gameFilename,
                                gameName = previousState.gameName
                            ))
                            loadGameInfo()
                        }
                    }
                }
            }
        }

        // Don't show/update widgets here - let loadSystemImage() or loadGameInfo() handle it
    }
    /**
     * Handle screensaver game select event (for slideshow/video screensavers)
     */
    private fun handleScreensaverGameSelect() {
        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        android.util.Log.d("MainActivity", "SCREENSAVER GAME SELECT EVENT")
        android.util.Log.d("MainActivity", "Current state: $state")

        val screensaverBehavior = prefsManager.screensaverBehavior
        android.util.Log.d("MainActivity", "Screensaver behavior: $screensaverBehavior")

        // Get current screensaver game from state
        val screensaverGame = if (state is AppState.Screensaver) {
            (state as AppState.Screensaver).currentGame
        } else {
            android.util.Log.w("MainActivity", "Not in screensaver state!")
            null
        }

        android.util.Log.d("MainActivity", "Screensaver game: ${screensaverGame?.gameFilename}")
        android.util.Log.d("MainActivity", "Screensaver initialized: $screensaverInitialized")

        // If black screen, don't load anything
        if (screensaverBehavior == "black_screen") {
            android.util.Log.d("MainActivity", "Black screen - ignoring screensaver game select")
            return
        }

        val isFirstGame = !screensaverInitialized

        if (isFirstGame) {
            android.util.Log.d("MainActivity", "Screensaver: First game event received - initializing display")
            screensaverInitialized = true
        }

        if (screensaverGame != null) {
            val gameName = screensaverGame.gameFilename.substringBeforeLast('.')

            when (screensaverBehavior) {
                "game_image" -> {
                    android.util.Log.d("MainActivity", "Processing game_image behavior")
                    android.util.Log.d("MainActivity", "  - System: ${screensaverGame.systemName}")
                    android.util.Log.d("MainActivity", "  - Game: ${screensaverGame.gameName ?: gameName}")
                    android.util.Log.d("MainActivity", "  - Filename: ${screensaverGame.gameFilename}")

                    // Check if solid color is selected for game view
                    val gameImagePref = prefsManager.gameViewBackgroundType
                    if (gameImagePref == "solid_color") {
                        val solidColor = prefsManager.gameBackgroundColor
                        android.util.Log.d("MainActivity", "  - Game view solid color selected - using color: ${String.format("#%06X", 0xFFFFFF and solidColor)}")
                        val drawable = android.graphics.drawable.ColorDrawable(solidColor)
                        gameImageView.setImageDrawable(drawable)
                        gameImageView.visibility = View.VISIBLE
                        videoView.visibility = View.GONE
                    } else if (gameImagePref == "custom_image") {
                        android.util.Log.d("MainActivity", "  - Game view custom image selected - loading custom background")
                        loadFallbackBackground(forceCustomImageOnly = true)
                        gameImageView.visibility = View.VISIBLE
                        videoView.visibility = View.GONE
                    } else {
                        // Fanart or Screenshot preference - try to find game artwork
                        val gameImage = findGameImage(
                            screensaverGame.systemName,
                            screensaverGame.gameFilename
                        )

                        android.util.Log.d("MainActivity", "  - Found image path: $gameImage")
                        android.util.Log.d("MainActivity", "  - Image exists: ${gameImage?.exists()}")

                        if (gameImage != null && gameImage.exists()) {
                            android.util.Log.d("MainActivity", "  ✓ Loading game image via loadImageWithAnimation()")
                            android.util.Log.d("MainActivity", "  - Before load - gameImageView visibility: ${gameImageView.visibility}")
                            loadImageWithAnimation(gameImage, gameImageView)
                        } else {
                            android.util.Log.d("MainActivity", "  - No game artwork found, falling back to custom background")
                            loadFallbackBackground()
                        }

                        // Make sure views are visible
                        gameImageView.visibility = View.VISIBLE
                        videoView.visibility = View.GONE
                        android.util.Log.d("MainActivity", "  - After load - gameImageView visibility: ${gameImageView.visibility}")
                        android.util.Log.d("MainActivity", "  - After load - videoView visibility: ${videoView.visibility}")
                    }

                    // Use existing function to load game widgets with correct images
                    android.util.Log.d("MainActivity", "Loading widgets for screensaver game")
                    updateWidgetsForCurrentGame()
                }
                "default_image" -> {
                    android.util.Log.d("MainActivity", "Processing default_image behavior")

                    loadFallbackBackground(forceCustomImageOnly = true)

                    // Make sure views are visible
                    gameImageView.visibility = View.VISIBLE
                    videoView.visibility = View.GONE
                    android.util.Log.d("MainActivity", "  - gameImageView visibility: ${gameImageView.visibility}")
                    android.util.Log.d("MainActivity", "  - videoView visibility: ${videoView.visibility}")

                    // Use existing function to load game widgets with correct images
                    android.util.Log.d("MainActivity", "Loading widgets for screensaver game")
                    updateWidgetsForCurrentGame()
                }
            }
        } else {
            android.util.Log.w("MainActivity", "No screensaver game info available")
        }

        android.util.Log.d("MainActivity", "Screensaver game select complete")
        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun updateWidgetsForScreensaverGame() {
        android.util.Log.d("MainActivity", "═══ updateWidgetsForScreensaverGame START ═══")

        // Clear existing widgets but preserve grid overlay
        val childCount = widgetContainer.childCount
        for (i in childCount - 1 downTo 0) {
            val child = widgetContainer.getChildAt(i)
            if (child !is GridOverlayView) {
                widgetContainer.removeView(child)
            }
        }
        activeWidgets.clear()

        val systemName = if (state is AppState.Screensaver) {
            (state as AppState.Screensaver).currentGame?.systemName
        } else {
            null
        }
        val gameFilename = if (state is AppState.Screensaver) {
            (state as AppState.Screensaver).currentGame?.gameFilename
        } else {
            null
        }

        if (systemName != null && gameFilename != null) {
            // Load saved widgets and update with screensaver game images
            val allWidgets = widgetManager.loadWidgets()
            android.util.Log.d("MainActivity", "Loaded ${allWidgets.size} widgets for screensaver")

            // Filter for GAME context widgets only
            val gameWidgets = allWidgets.filter { it.widgetContext == Widget.WidgetContext.GAME }
            android.util.Log.d("MainActivity", "Loaded ${gameWidgets.size} game widgets for screensaver")

            // Sort widgets by z-index before processing
            val sortedWidgets = gameWidgets.sortedBy { it.zIndex }  // ✅ FIX: Use gameWidgets
            android.util.Log.d("MainActivity", "Sorted ${sortedWidgets.size} widgets by z-index")

            sortedWidgets.forEachIndexed { index, widget ->
                android.util.Log.d("MainActivity", "Processing screensaver widget $index: type=${widget.imageType}, zIndex=${widget.zIndex}")

                val gameName = sanitizeGameFilename(gameFilename).substringBeforeLast('.')
                val imageFile = when (widget.imageType) {
                    Widget.ImageType.MARQUEE ->
                        findImageInFolder(systemName, gameFilename, "marquees")
                    Widget.ImageType.BOX_2D ->
                        findImageInFolder(systemName, gameFilename, "covers")
                    Widget.ImageType.BOX_3D ->
                        findImageInFolder(systemName, gameFilename, "3dboxes")
                    Widget.ImageType.MIX_IMAGE ->
                        findImageInFolder(systemName, gameFilename, "miximages")
                    Widget.ImageType.BACK_COVER ->
                        findImageInFolder(systemName, gameFilename, "backcovers")
                    Widget.ImageType.PHYSICAL_MEDIA ->
                        findImageInFolder(systemName, gameFilename, "physicalmedia")
                    Widget.ImageType.SCREENSHOT ->
                        findImageInFolder(systemName, gameFilename, "screenshots")
                    Widget.ImageType.FANART ->
                        findImageInFolder(systemName, gameFilename, "fanart")
                    Widget.ImageType.TITLE_SCREEN ->
                        findImageInFolder(systemName, gameFilename, "titlescreens")
                    Widget.ImageType.GAME_DESCRIPTION -> null  // Text widget, handled separately
                    Widget.ImageType.SYSTEM_LOGO -> null
                    Widget.ImageType.COLOR_BACKGROUND -> null  // NEW: No file needed
                    Widget.ImageType.CUSTOM_IMAGE -> null      // NEW: Path already set
                    Widget.ImageType.RANDOM_FANART -> null     // NEW: Path set in system widget loading
                    Widget.ImageType.RANDOM_SCREENSHOT -> null // NEW: Path set in system widget loading
                }

                // ALWAYS create the widget, even if image doesn't exist
                val widgetToAdd = when {
                    // NEW: Handle description text widget for screensaver
                    widget.imageType == Widget.ImageType.GAME_DESCRIPTION -> {
                        val description = getGameDescription(systemName, gameFilename)
                        android.util.Log.d("MainActivity", "  Updating screensaver description widget: ${description?.take(50)}")
                        widget.copy(imagePath = description ?: "")
                    }
                    // Handle image widgets
                    imageFile != null && imageFile.exists() -> {
                        android.util.Log.d("MainActivity", "  Creating screensaver widget with new image")
                        widget.copy(imagePath = imageFile.absolutePath)
                    }
                    // No image found
                    else -> {
                        android.util.Log.d("MainActivity", "  No screensaver image found for widget type ${widget.imageType}, using empty path")
                        if (widget.imageType == Widget.ImageType.MARQUEE) {
                            widget.copy(imagePath = "marquee://$gameName")
                        } else {
                            widget.copy(imagePath = "")
                        }
                    }
                }

                addWidgetToScreenWithoutSaving(widgetToAdd)
                android.util.Log.d("MainActivity", "  Screensaver widget added to screen")
            }

            android.util.Log.d("MainActivity", "Total screensaver widgets added: ${activeWidgets.size}")

            // Make sure container is visible
            widgetContainer.visibility = View.VISIBLE
        }

        android.util.Log.d("MainActivity", "═══ updateWidgetsForScreensaverGame END ═══")
    }

    // ========== VIDEO PLAYBACK FUNCTIONS ==========

    /**
     * Check if video is enabled in settings
     */
    private fun isVideoEnabled(): Boolean {
        return prefsManager.videoEnabled
    }

    private fun updateVideoVolume() {
        videoManager.updateVideoVolume()
    }

    /**
     * Register listener for system volume changes
     * Listens for both standard volume and Ayn Thor's secondary screen volume
     */
    private fun registerVolumeListener() {
        volumeChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "android.media.VOLUME_CHANGED_ACTION" -> {
                        // Standard volume changed (top screen)
                        android.util.Log.d("MainActivity", "Volume change detected - updating video volume")
                        updateVideoVolume()
                    }
                    Settings.ACTION_SOUND_SETTINGS -> {
                        // Sound settings changed (might include secondary screen volume)
                        android.util.Log.d("MainActivity", "Sound settings changed - updating video volume")
                        updateVideoVolume()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            // Note: Settings.System changes don't broadcast reliably, so we also check in onResume
        }
        registerReceiver(volumeChangeReceiver, filter)
        android.util.Log.d("MainActivity", "Volume change listener registered")
    }

    /**
     * Unregister volume listener
     */
    private fun unregisterVolumeListener() {
        volumeChangeReceiver?.let {
            try {
                unregisterReceiver(it)
                android.util.Log.d("MainActivity", "Volume change listener unregistered")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error unregistering volume listener", e)
            }
        }
        volumeChangeReceiver = null
    }

    // Add this variable at the top of MainActivity class
    private var secondaryVolumeObserver: android.database.ContentObserver? = null

// Add this function near the volume functions
    /**
     * Register observer for secondary screen volume changes (Ayn Thor)
     */
    private fun registerSecondaryVolumeObserver() {
        try {
            secondaryVolumeObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    android.util.Log.d("MainActivity", "Secondary screen volume changed - updating video volume")
                    updateVideoVolume()
                }
            }

            // Observe the secondary_screen_volume_level setting
            contentResolver.registerContentObserver(
                Settings.System.getUriFor("secondary_screen_volume_level"),
                false,
                secondaryVolumeObserver!!
            )

            android.util.Log.d("MainActivity", "Secondary volume observer registered")
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Could not register secondary volume observer (not an Ayn Thor?)", e)
        }
    }

    /**
     * Unregister secondary volume observer
     */
    private fun unregisterSecondaryVolumeObserver() {
        secondaryVolumeObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
                android.util.Log.d("MainActivity", "Secondary volume observer unregistered")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error unregistering secondary volume observer", e)
            }
        }
        secondaryVolumeObserver = null
    }

    /**
     * Check if video is currently playing
     */
    private fun isVideoPlaying(): Boolean {
        return videoView.player != null && videoView.visibility == View.VISIBLE
    }

    /**
     * Get video delay in milliseconds
     */
    private fun getVideoDelay(): Long {
        val progress = prefsManager.videoDelay
        return (progress * AppConstants.Timing.VIDEO_DELAY_MULTIPLIER)
    }

    /**
     * Release video player
     */
    private fun releasePlayer() {
        // Stop video with animation
        videoView.animate().cancel()

        videoView.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                videoManager.stopVideo()

                // Show the game image view again
                gameImageView.visibility = View.VISIBLE

                // Show widgets when game is playing
                if (state is AppState.GamePlaying) {
                    val hasWidgets = widgetManager.loadWidgets().isNotEmpty()
                    if (hasWidgets) {
                        showWidgets()
                    }
                }
            }
            .start()
    }

    /**
     * Handle video loading with delay
     */
    private fun handleVideoForGame(systemName: String?, strippedName: String?, rawName: String?): Boolean {
        // When scrolling rapidly through games, we need to cancel pending video delays
        // BEFORE any validation checks, so old videos don't start playing
        videoManager.cancelVideoDelay()

        // Only trust isActivityVisible (onStart/onStop) - it's the only truly reliable signal
        if (!isActivityVisible) {
            android.util.Log.d("MainActivity", "Video blocked - activity not visible (onStop called)")
            releasePlayer()
            return false
        }

        // Block videos if game is playing
        if (state is AppState.GamePlaying) {
            android.util.Log.d("MainActivity", "Video blocked - game is playing (ES-DE event)")
            releasePlayer()
            return false
        }

        // Block videos during screensaver
        if (state is AppState.Screensaver) {
            android.util.Log.d("MainActivity", "Video blocked - screensaver active")
            releasePlayer()
            return false
        }

        // Check if video is enabled in settings
        if (!isVideoEnabled()) {
            releasePlayer()
            return false
        }

        // Block videos during widget edit mode
        if (!widgetsLocked) {
            android.util.Log.d("MainActivity", "Video blocked - widget edit mode active")
            releasePlayer()
            return false
        }

        // Block videos when black overlay is shown
        if (isBlackOverlayShown) {
            android.util.Log.d("MainActivity", "Video blocked - black overlay shown")
            releasePlayer()
            return false
        }

        // Validate required parameters
        if (systemName == null || rawName == null) {
            android.util.Log.d("MainActivity", "Video blocked - missing systemName or rawName")
            releasePlayer()
            return false
        }

        // Use VideoManager to play video with delay
        val videoWillPlay = videoManager.playVideoWithDelay(
            systemName = systemName,
            gameFilename = rawName,
            onStarted = {
                android.util.Log.d("MainActivity", "Video started playing")

                // Hide the game image view so video is visible
                gameImageView.visibility = View.GONE

                // Hide widgets when video plays
                hideWidgets()

                musicManager.onVideoStarted()

                // Apply animation to video view
                applyVideoAnimation()
            },
            onEnded = {
                android.util.Log.d("MainActivity", "Video playback ended")

                // Show game image view again
                gameImageView.visibility = View.VISIBLE

                // Show widgets again if applicable
                if (state is AppState.GamePlaying) {
                    val hasWidgets = widgetManager.loadWidgets().isNotEmpty()
                    if (hasWidgets) {
                        showWidgets()
                    }
                }

                musicManager.onVideoEnded()
            }
        )

        if (videoWillPlay) {
            android.util.Log.d("MainActivity", "Video scheduled for playback: $systemName / $rawName")
        } else {
            android.util.Log.d("MainActivity", "No video found for: $systemName / $rawName")
        }

        return videoWillPlay
    }

    /**
     * Apply animation to video view based on user preferences
     */
    private fun applyVideoAnimation() {
        val animationStyle = prefsManager.animationStyle
        val duration = prefsManager.animationDuration
        val scaleAmount = prefsManager.animationScale / 100f

        videoView.visibility = View.VISIBLE

        when (animationStyle) {
            "none" -> {
                // No animation - instant display
                videoView.alpha = 1f
                videoView.scaleX = 1f
                videoView.scaleY = 1f
            }
            "fade" -> {
                // Fade only - no scale
                videoView.alpha = 0f
                videoView.scaleX = 1f
                videoView.scaleY = 1f
                videoView.animate()
                    .alpha(1f)
                    .setDuration(duration.toLong())
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            else -> {
                // "scale_fade" - default with scale + fade
                videoView.alpha = 0f
                videoView.scaleX = scaleAmount
                videoView.scaleY = scaleAmount
                videoView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(duration.toLong())
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }
    }

    private fun loadWidgets() {
        // Clear existing widgets
        widgetContainer.removeAllViews()
        activeWidgets.clear()

        // Load saved widgets - use WithoutSaving to avoid corrupting storage during load
        val widgets = widgetManager.loadWidgets()
        widgets.forEach { widget ->
            addWidgetToScreenWithoutSaving(widget)
        }
    }

    private fun addWidgetToScreen(widget: Widget) {
        val widgetView = WidgetView(
            this,
            widget,
            onDelete = { view ->
                // Remove from container and active list
                widgetContainer.removeView(view)
                activeWidgets.remove(view)

                // Delete from persistent storage
                widgetManager.deleteWidget(view.widget.id)

                android.util.Log.d("MainActivity", "Widget deleted: ${view.widget.id}")
            },
            onUpdate = { updatedWidget ->
                // Save all widgets with the updated widget
                saveAllWidgets()

                android.util.Log.d("MainActivity", "Widget updated: ${updatedWidget.id}")
            },
            imageManager = imageManager
        )

        // Apply current lock state to new widget
        widgetView.setLocked(widgetsLocked)

        // Apply current snap to grid state
        widgetView.setSnapToGrid(snapToGrid, gridSize)

        activeWidgets.add(widgetView)
        widgetContainer.addView(widgetView)
    }

    private fun removeWidget(widgetView: WidgetView) {
        widgetContainer.removeView(widgetView)
        activeWidgets.remove(widgetView)
        widgetManager.deleteWidget(widgetView.widget.id)
    }

    private fun showCreateWidgetMenu() {
        // If dialog already exists and is showing, don't create another
        if (widgetMenuDialog?.isShowing == true) {
            android.util.Log.d("MainActivity", "Widget menu already showing, ignoring")
            return
        }

        // Deselect all widgets first
        activeWidgets.forEach { it.deselect() }

        // Inflate the custom dialog view
        val dialogView = layoutInflater.inflate(R.layout.dialog_widget_menu, null)

        // Get references to chips
        val chipLockWidgets = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipLockWidgets)
        val chipSnapToGrid = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipSnapToGrid)
        val chipShowGrid = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipShowGrid)
        val widgetOptionsContainer = dialogView.findViewById<LinearLayout>(R.id.widgetOptionsContainer)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelWidgetMenu)
        val btnHelp = dialogView.findViewById<android.widget.Button>(R.id.btnWidgetHelp)

        // Set chip states and text
        chipLockWidgets.isChecked = !widgetsLocked  // Inverted: checked = edit mode ON
        chipLockWidgets.text = if (widgetsLocked) "Widget Edit Mode: OFF" else "Widget Edit Mode: ON"

        chipSnapToGrid.isChecked = snapToGrid
        chipSnapToGrid.text = if (snapToGrid) "⊞ Snap to Grid: ON" else "⊞ Snap to Grid: OFF"

        chipShowGrid.isChecked = showGrid
        chipShowGrid.text = if (showGrid) "⊞ Show Grid: ON" else "⊞ Show Grid: OFF"

        // Create the dialog
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .setOnDismissListener {
                widgetMenuShowing = false
                widgetMenuDialog = null
                android.util.Log.d("MainActivity", "Widget menu dismissed, flags reset")

                // Refresh widgets based on current view state
                when (state) {
                    is AppState.SystemBrowsing -> {
                        // In system view - refresh system widgets
                        android.util.Log.d("MainActivity", "Refreshing system widgets after dialog dismiss")
                        updateWidgetsForCurrentSystem()
                    }
                    is AppState.GameBrowsing, is AppState.Screensaver -> {
                        // In game view or screensaver - refresh game widgets
                        android.util.Log.d("MainActivity", "Refreshing game widgets after dialog dismiss")
                        updateWidgetsForCurrentGame()
                    }
                    else -> {
                        android.util.Log.d("MainActivity", "Not in browsing state - skipping widget refresh")
                    }
                }
            }
            .create()

        // Chip click listeners
        chipLockWidgets.setOnClickListener {
            toggleWidgetLock()
            chipLockWidgets.text = if (widgetsLocked) "Widget Edit Mode: OFF" else "Widget Edit Mode: ON"
        }

        chipSnapToGrid.setOnClickListener {
            toggleSnapToGrid()
            chipSnapToGrid.text = if (snapToGrid) "⊞ Snap to Grid: ON" else "⊞ Snap to Grid: OFF"
        }

        chipShowGrid.setOnClickListener {
            toggleShowGrid()
            chipShowGrid.text = if (showGrid) "⊞ Show Grid: ON" else "⊞ Show Grid: OFF"
        }

        btnHelp.setOnClickListener {
            dialog.dismiss()
            showWidgetSystemTutorial(fromUpdate = false)
        }

        // Populate widget options based on current view
        val widgetOptions = if (state is AppState.SystemBrowsing) {
            // System view - system logo + random artwork + color/image widgets
            listOf(
                "System Logo" to Widget.ImageType.SYSTEM_LOGO,
                "Random Fanart" to Widget.ImageType.RANDOM_FANART,
                "Random Screenshot" to Widget.ImageType.RANDOM_SCREENSHOT,
                "Color Background" to Widget.ImageType.COLOR_BACKGROUND,
                "Custom Image" to Widget.ImageType.CUSTOM_IMAGE
            )
        } else {
            // Game view - all game image types + color/image widgets
            listOf(
                "Marquee" to Widget.ImageType.MARQUEE,
                "2D Box" to Widget.ImageType.BOX_2D,
                "3D Box" to Widget.ImageType.BOX_3D,
                "Mix Image" to Widget.ImageType.MIX_IMAGE,
                "Back Cover" to Widget.ImageType.BACK_COVER,
                "Physical Media" to Widget.ImageType.PHYSICAL_MEDIA,
                "Screenshot" to Widget.ImageType.SCREENSHOT,
                "Fanart" to Widget.ImageType.FANART,
                "Title Screen" to Widget.ImageType.TITLE_SCREEN,
                "Game Description" to Widget.ImageType.GAME_DESCRIPTION,
                "Color Background" to Widget.ImageType.COLOR_BACKGROUND,
                "Custom Image" to Widget.ImageType.CUSTOM_IMAGE
            )
        }

        // Add each widget option as a styled item
        widgetOptions.forEach { (label, imageType) ->
            val itemView = layoutInflater.inflate(R.layout.item_widget_option, widgetOptionsContainer, false)
            val textView = itemView.findViewById<TextView>(R.id.widgetOptionText)
            textView.text = label

            itemView.setOnClickListener {
                // Check if locked before creating
                if (widgetsLocked) {
                    android.widget.Toast.makeText(
                        this,
                        "Cannot create widgets while locked. Unlock widgets first.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    createWidget(imageType)
                    dialog.dismiss()
                }
            }

            widgetOptionsContainer.addView(itemView)
        }

        // Cancel button
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        widgetMenuDialog = dialog
        dialog.show()
        android.util.Log.d("MainActivity", "Widget menu dialog created and shown")
    }

    private fun toggleSnapToGrid() {
        snapToGrid = !snapToGrid

        // Update all active widgets with the new snap state
        activeWidgets.forEach { it.setSnapToGrid(snapToGrid, gridSize) }

        // Save snap state to preferences
        prefsManager.snapToGrid = snapToGrid
    }

    private fun toggleShowGrid() {
        showGrid = !showGrid
        updateGridOverlay()

        // Save show grid state to preferences
        prefsManager.showGrid = showGrid

        android.util.Log.d("MainActivity", "Show grid toggled: $showGrid")
    }

    private fun toggleWidgetLock() {
        widgetsLocked = !widgetsLocked

        // Update all active widgets with the new lock state
        activeWidgets.forEach { it.setLocked(widgetsLocked) }

        val message = if (widgetsLocked) {
            "Widgets locked - they can no longer be moved, resized, or deleted"
        } else {
            "Widgets unlocked - tap to select, drag to move, resize from corner"
        }

        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()

        // Save lock state to preferences
        prefsManager.widgetsLocked = widgetsLocked

        // Handle video playback when toggling widget lock
        if (widgetsLocked) {
            // Locked (edit mode OFF) - videos can now play if conditions allow
            // No need to reload - video check happens automatically on next event
            android.util.Log.d("MainActivity", "Widget edit mode OFF - videos now allowed")
        } else {
            // Unlocked (edit mode ON) - stop any playing videos
            android.util.Log.d("MainActivity", "Widget edit mode ON - blocking videos")
            releasePlayer()
        }
    }

    private fun createWidget(imageType: Widget.ImageType) {
        val displayMetrics = resources.displayMetrics
        val nextZIndex = (activeWidgets.maxOfOrNull { it.widget.zIndex } ?: -1) + 1

        // Determine current context
        val currentContext = if (state is AppState.SystemBrowsing) {
            Widget.WidgetContext.SYSTEM
        } else {
            Widget.WidgetContext.GAME
        }

        // Handle Color Background widget
        if (imageType == Widget.ImageType.COLOR_BACKGROUND) {
            widgetCreationManager.showColorPickerDialog { selectedColor ->
                val widget = widgetCreationManager.createColorBackgroundWidget(
                    color = selectedColor,
                    displayMetrics = displayMetrics,
                    zIndex = nextZIndex,
                    widgetContext = currentContext
                )
                widget.toPercentages(displayMetrics.widthPixels, displayMetrics.heightPixels)  // ADD THIS LINE
                addWidgetToScreen(widget)
                saveAllWidgets()

                android.widget.Toast.makeText(
                    this,
                    "Color background widget created!",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        // Handle Custom Image widget
        if (imageType == Widget.ImageType.CUSTOM_IMAGE) {
            widgetCreationManager.launchImagePicker { imagePath ->
                val widget = widgetCreationManager.createCustomImageWidget(
                    imagePath = imagePath,
                    displayMetrics = displayMetrics,
                    zIndex = nextZIndex,
                    widgetContext = currentContext
                )
                widget.toPercentages(displayMetrics.widthPixels, displayMetrics.heightPixels)  // ADD THIS LINE
                addWidgetToScreen(widget)
                saveAllWidgets()

                android.widget.Toast.makeText(
                    this,
                    "Custom image widget created!",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        // Handle Random Fanart and Random Screenshot widgets (system view only)
        if (imageType == Widget.ImageType.RANDOM_FANART || imageType == Widget.ImageType.RANDOM_SCREENSHOT) {
            val systemName = state.getCurrentSystemName()

            if (systemName == null) {
                android.widget.Toast.makeText(
                    this,
                    "No system selected",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Find a random image from the appropriate folder
            val folderName = if (imageType == Widget.ImageType.RANDOM_FANART) "fanart" else "screenshots"
            val randomImage = mediaManager.getRandomImageFromSystemFolder(systemName, folderName)

            if (randomImage == null) {
                android.widget.Toast.makeText(
                    this,
                    "No ${folderName} found for ${systemName}. Scrape artwork in ES-DE first.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            }

            // Create widget with special "random://" prefix to indicate it needs refresh on system change
            val widget = Widget(
                imageType = imageType,
                imagePath = "random://$systemName",  // Special prefix for random selection
                x = displayMetrics.widthPixels / 2f - 150f,
                y = displayMetrics.heightPixels / 2f - 200f,
                width = 300f,
                height = 400f,
                zIndex = nextZIndex,
                widgetContext = Widget.WidgetContext.SYSTEM
            )

            widget.toPercentages(displayMetrics.widthPixels, displayMetrics.heightPixels)
            addWidgetToScreen(widget)
            saveAllWidgets()

            val widgetTypeName = if (imageType == Widget.ImageType.RANDOM_FANART) "Random fanart" else "Random screenshot"
            android.widget.Toast.makeText(
                this,
                "$widgetTypeName widget created!",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        if (imageType == Widget.ImageType.SYSTEM_LOGO) {
            // Creating system widget
            val systemName = state.getCurrentSystemName()

            if (systemName == null) {
                android.widget.Toast.makeText(
                    this,
                    "No system selected",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }

            // System logos always use builtin:// prefix - WidgetView handles finding custom vs built-in
            // Normalize system name to handle auto-collections
            val normalizedName = normalizeSystemName(systemName)
            val widget = Widget(
                imageType = Widget.ImageType.SYSTEM_LOGO,
                imagePath = "builtin://$normalizedName",
                x = displayMetrics.widthPixels / 2f - 150f,
                y = displayMetrics.heightPixels / 2f - 200f,
                width = 300f,
                height = 400f,
                zIndex = nextZIndex,
                widgetContext = Widget.WidgetContext.SYSTEM
            )

            widget.toPercentages(displayMetrics.widthPixels, displayMetrics.heightPixels)

            addWidgetToScreen(widget)

            // Save the widget immediately so it persists when dialog dismisses
            saveAllWidgets()

            android.widget.Toast.makeText(
                this,
                "System logo widget created!",
                android.widget.Toast.LENGTH_LONG
            ).show()

        } else {
            // Creating game widget
            val systemName = state.getCurrentSystemName()
            val gameFilename = state.getCurrentGameFilename()

            if (systemName == null || gameFilename == null) {
                android.widget.Toast.makeText(
                    this,
                    "No game selected. Browse to a game first.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Find the appropriate game image
            val gameName = sanitizeGameFilename(gameFilename).substringBeforeLast('.')
            val imageFile = when (imageType) {
                Widget.ImageType.MARQUEE ->
                    findImageInFolder(systemName, gameFilename, "marquees")
                Widget.ImageType.BOX_2D ->
                    findImageInFolder(systemName, gameFilename, "covers")
                Widget.ImageType.BOX_3D ->
                    findImageInFolder(systemName, gameFilename, "3dboxes")
                Widget.ImageType.MIX_IMAGE ->
                    findImageInFolder(systemName, gameFilename, "miximages")
                Widget.ImageType.BACK_COVER ->
                    findImageInFolder(systemName, gameFilename, "backcovers")
                Widget.ImageType.PHYSICAL_MEDIA ->
                    findImageInFolder(systemName, gameFilename, "physicalmedia")
                Widget.ImageType.SCREENSHOT ->
                    findImageInFolder(systemName, gameFilename, "screenshots")
                Widget.ImageType.FANART ->
                    findImageInFolder(systemName, gameFilename, "fanart")
                Widget.ImageType.TITLE_SCREEN ->
                    findImageInFolder(systemName, gameFilename, "titlescreens")
                Widget.ImageType.GAME_DESCRIPTION -> null
                else -> null
            }

            // Special handling for game description (text widget)
            if (imageType == Widget.ImageType.GAME_DESCRIPTION) {
                val description = getGameDescription(systemName, gameFilename)

                val widget = Widget(
                    imageType = Widget.ImageType.GAME_DESCRIPTION,
                    imagePath = description ?: "",  // Store description text in imagePath
                    x = displayMetrics.widthPixels / 2f - 300f,
                    y = displayMetrics.heightPixels / 2f - 200f,
                    width = 600f,
                    height = 400f,
                    zIndex = nextZIndex,
                    widgetContext = Widget.WidgetContext.GAME
                )

                widget.toPercentages(displayMetrics.widthPixels, displayMetrics.heightPixels)

                addWidgetToScreen(widget)

                // Save the widget immediately so it persists when dialog dismisses
                saveAllWidgets()

                android.widget.Toast.makeText(
                    this,
                    if (description != null) "Game description widget created!"
                    else "No description available for this game",
                    android.widget.Toast.LENGTH_LONG
                ).show()

                return
            }

            // Existing validation for image-based widgets
            if (imageFile == null || !imageFile.exists()) {
                val typeName = when (imageType) {
                    Widget.ImageType.MARQUEE -> "marquee"
                    Widget.ImageType.BOX_2D -> "2D box"
                    Widget.ImageType.BOX_3D -> "3D box"
                    Widget.ImageType.MIX_IMAGE -> "mix image"
                    Widget.ImageType.BACK_COVER -> "back cover"
                    Widget.ImageType.PHYSICAL_MEDIA -> "physical media"
                    Widget.ImageType.SCREENSHOT -> "screenshot"
                    Widget.ImageType.FANART -> "fanart"
                    Widget.ImageType.TITLE_SCREEN -> "title screen"
                    Widget.ImageType.GAME_DESCRIPTION -> "game description"
                    else -> "image"
                }
                android.widget.Toast.makeText(
                    this,
                    "No $typeName image found for this game",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }

            val widget = Widget(
                imageType = imageType,
                imagePath = imageFile.absolutePath,
                x = displayMetrics.widthPixels / 2f - 150f,
                y = displayMetrics.heightPixels / 2f - 200f,
                width = 300f,
                height = 400f,
                zIndex = nextZIndex,
                widgetContext = Widget.WidgetContext.GAME
            )

            widget.toPercentages(displayMetrics.widthPixels, displayMetrics.heightPixels)

            addWidgetToScreen(widget)

            // Save the widget immediately so it persists when dialog dismisses
            saveAllWidgets()

            android.widget.Toast.makeText(
                this,
                "Widget created! Tap to select, drag to move, resize from corners",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    fun findSystemLogo(systemName: String): String? {
        // Handle ES-DE auto-collections
        val baseFileName = when (systemName.lowercase()) {
            "all" -> "auto-allgames"
            "favorites" -> "auto-favorites"
            "recent" -> "auto-lastplayed"
            else -> systemName.lowercase()
        }
        android.util.Log.d("MainActivity", "Finding system logo for: $baseFileName")

        // Check for custom system logos (always check the path, regardless of preference)
        val customLogoDir = File(getSystemLogosPath())
        android.util.Log.d("MainActivity", "Checking custom logos path: ${customLogoDir.absolutePath}")

        if (customLogoDir.exists() && customLogoDir.isDirectory) {
            // Try different extensions (including GIF for animation support)
            val extensions = listOf("svg", "png", "jpg", "jpeg", "webp", "gif")
            for (ext in extensions) {
                val logoFile = File(customLogoDir, "$baseFileName.$ext")
                if (logoFile.exists()) {
                    android.util.Log.d("MainActivity", "Found custom system logo: ${logoFile.absolutePath}")
                    return logoFile.absolutePath
                }
            }
        }

        // Fall back to built-in assets
        // Return special marker that WidgetView will recognize to load from assets
        android.util.Log.d("MainActivity", "Using built-in system logo for $baseFileName")
        return "builtin://$baseFileName"
    }

    /**
     * Normalize system names to handle ES-DE auto-collections
     * Maps various collection names to their standard auto-* format
     */
    private fun normalizeSystemName(systemName: String): String {
        return when (systemName.lowercase()) {
            "all", "allgames" -> "auto-allgames"
            "favorites" -> "auto-favorites"
            "recent", "lastplayed" -> "auto-lastplayed"
            else -> systemName.lowercase()
        }
    }

    fun updateWidgetsForCurrentSystem() {
        android.util.Log.d("MainActivity", "═══ updateWidgetsForCurrentSystem START ═══")
        android.util.Log.d("MainActivity", "Current state: $state")

        // Don't update system widgets during screensaver
        if (state is AppState.Screensaver) {
            android.util.Log.d("MainActivity", "Screensaver active - skipping system widget update")
            return
        }

        val systemName = state.getCurrentSystemName()

        if (systemName != null) {
            // Load saved widgets
            val allWidgets = widgetManager.loadWidgets()

            // Filter for SYSTEM context widgets only
            val systemWidgets = allWidgets.filter { it.widgetContext == Widget.WidgetContext.SYSTEM }
            android.util.Log.d("MainActivity", "Loaded ${systemWidgets.size} system widgets from storage")

            // Clear existing widget views but preserve grid overlay
            val childCount = widgetContainer.childCount
            for (i in childCount - 1 downTo 0) {
                val child = widgetContainer.getChildAt(i)
                if (child !is GridOverlayView) {
                    widgetContainer.removeView(child)
                }
            }
            activeWidgets.clear()
            android.util.Log.d("MainActivity", "Cleared widget container (preserved grid)")

            // Sort widgets by z-index
            val sortedWidgets = systemWidgets.sortedBy { it.zIndex }
            android.util.Log.d("MainActivity", "Sorted ${sortedWidgets.size} system widgets by z-index")

            // Process each widget
            sortedWidgets.forEachIndexed { index, widget ->
                android.util.Log.d("MainActivity", "Processing system widget $index: type=${widget.imageType}, zIndex=${widget.zIndex}")

                val widgetToAdd = when (widget.imageType) {
                    // Handle random artwork widgets
                    Widget.ImageType.RANDOM_FANART -> {
                        val folderName = "fanart"
                        val randomImage = mediaManager.getRandomImageFromSystemFolder(systemName, folderName)

                        if (randomImage != null) {
                            widget.copy(imagePath = randomImage.absolutePath)
                        } else {
                            android.util.Log.w("MainActivity", "No fanart found for $systemName")
                            widget.copy(imagePath = "")  // Will show empty widget
                        }
                    }

                    Widget.ImageType.RANDOM_SCREENSHOT -> {
                        val folderName = "screenshots"
                        val randomImage = mediaManager.getRandomImageFromSystemFolder(systemName, folderName)

                        if (randomImage != null) {
                            widget.copy(imagePath = randomImage.absolutePath)
                        } else {
                            android.util.Log.w("MainActivity", "No screenshots found for $systemName")
                            widget.copy(imagePath = "")
                        }
                    }

                    Widget.ImageType.SYSTEM_LOGO -> {
                        // System logos always use builtin:// prefix
                        val normalizedName = normalizeSystemName(systemName)
                        widget.copy(imagePath = "builtin://$normalizedName")
                    }

                    else -> widget  // Color background, custom image - use as-is
                }

                addWidgetToScreenWithoutSaving(widgetToAdd)
                android.util.Log.d("MainActivity", "  System widget added to screen")
            }

            android.util.Log.d("MainActivity", "Total system widgets added: ${activeWidgets.size}")
            android.util.Log.d("MainActivity", "Widget container children: ${widgetContainer.childCount}")

            // Make sure container is visible and grid is updated
            widgetContainer.visibility = View.VISIBLE
            updateGridOverlay()
            android.util.Log.d("MainActivity", "Widget container visibility: ${widgetContainer.visibility}")
        } else {
            android.util.Log.d("MainActivity", "System name is null - not updating widgets")
        }

        android.util.Log.d("MainActivity", "═══ updateWidgetsForCurrentSystem END ═══")
    }

    private fun findImageInFolder(
        systemName: String,
        gameFilename: String,
        folder: String
    ): File? {
        return mediaManager.findImageInFolder(systemName, gameFilename, folder)
    }

    /**
     * Parse game description from ES-DE gamelist.xml
     * Returns null if not found or any error occurs
     */
    private fun getGameDescription(systemName: String, gameFilename: String): String? {
        try {
            // Get scripts path and navigate to ES-DE folder
            val scriptsPath = prefsManager.scriptsPath
                ?: return null

            // Get ES-DE root folder (parent of scripts folder)
            val scriptsDir = File(scriptsPath)
            val esdeRoot = scriptsDir.parentFile ?: return null

            // Build path to gamelist.xml: ~/ES-DE/gamelists/<systemname>/gamelist.xml
            val gamelistFile = File(esdeRoot, "gamelists/$systemName/gamelist.xml")

            android.util.Log.d("MainActivity", "Looking for gamelist: ${gamelistFile.absolutePath}")

            if (!gamelistFile.exists()) {
                android.util.Log.d("MainActivity", "Gamelist file not found for system: $systemName")
                return null
            }

            // Parse XML to find the game's description
            val xmlContent = gamelistFile.readText()

            // Build the relative path as ES-DE stores it in gamelist.xml.
            // ES-DE stores paths as "./game.zip" for root-level games and
            // "./subfolder/game.zip" for subfolder games.
            // gameFilename may arrive as a full absolute path (e.g.
            // "/storage/.../ROMs/psx/sub/game.zip"), a relative subfolder path
            // (e.g. "sub/game.zip"), or just a bare filename ("game.zip").
            // We normalise it to the relative form ES-DE uses in <path> tags.
            val normalizedPath = gameFilename.replace("\\", "/")

            // Extract the relative portion after the system name segment so we
            // end up with exactly what ES-DE writes after the leading "./".
            // e.g. "/storage/.../psx/sub/game.zip" → "sub/game.zip"
            //      "sub/game.zip"                  → "sub/game.zip"
            //      "game.zip"                      → "game.zip"
            val relativeGamePath: String = run {
                val segments = normalizedPath.split("/").filter { it.isNotEmpty() }
                val sysIndex = segments.indexOf(systemName)
                if (sysIndex != -1 && sysIndex + 1 < segments.size) {
                    // Absolute path: take everything after the system folder
                    segments.drop(sysIndex + 1).joinToString("/")
                } else if (normalizedPath.startsWith("/")) {
                    // Absolute but system name not found – fall back to bare filename
                    normalizedPath.substringAfterLast("/")
                } else {
                    // Already relative (e.g. "sub/game.zip" or "game.zip")
                    normalizedPath
                }
            }

            android.util.Log.d("MainActivity", "Searching for game: '$relativeGamePath'")

            // ES-DE only encodes & as &amp; in <path> tags
            val relativePathEncoded = relativeGamePath.replace("&", "&amp;")

            var pathMatch: MatchResult? = null

            // Strategy 1: Encoded & variant (most common for paths containing &)
            if (relativePathEncoded != relativeGamePath) {
                android.util.Log.d("MainActivity", "  Trying &amp; encoded: '$relativePathEncoded'")
                val pattern1 = "<path>\\./\\Q$relativePathEncoded\\E</path>".toRegex()
                pathMatch = pattern1.find(xmlContent)
            }

            // Strategy 2: Exact relative path match
            if (pathMatch == null) {
                android.util.Log.d("MainActivity", "  Trying exact: '$relativeGamePath'")
                val pattern2 = "<path>\\./\\Q$relativeGamePath\\E</path>".toRegex()
                pathMatch = pattern2.find(xmlContent)
            }

            // Strategy 3: Filename-only fallback (handles edge cases where the full
            // path couldn't be reconstructed but the filename is unique in the list)
            if (pathMatch == null) {
                val bareFilename = relativeGamePath.substringAfterLast("/")
                val bareEncoded = bareFilename.replace("&", "&amp;")
                android.util.Log.d("MainActivity", "  Trying filename-only fallback: '$bareFilename'")
                // Match any path ending with this filename (covers any subfolder depth)
                val pattern3 = "<path>\\./(?:[^<]*/)?\\Q$bareEncoded\\E</path>".toRegex()
                pathMatch = pattern3.find(xmlContent)
                if (pathMatch == null && bareEncoded != bareFilename) {
                    val pattern4 = "<path>\\./(?:[^<]*/)?\\Q$bareFilename\\E</path>".toRegex()
                    pathMatch = pattern4.find(xmlContent)
                }
            }

            if (pathMatch == null) {
                android.util.Log.d("MainActivity", "Game not found in gamelist: $relativeGamePath")

                // Debug: Show similar paths from the gamelist to help diagnose
                val allPathsPattern = "<path>\\./([^<]+)</path>".toRegex()
                val allPaths = allPathsPattern.findAll(xmlContent).map { it.groupValues[1] }.toList()
                android.util.Log.d("MainActivity", "Total paths in gamelist: ${allPaths.size}")

                // Find paths that start with the same first word (use bare filename for matching)
                val bareFilename = relativeGamePath.substringAfterLast("/")
                val firstWord = bareFilename.split(" ", "'", "-").firstOrNull()?.take(10) ?: ""
                if (firstWord.isNotEmpty()) {
                    val similarPaths = allPaths.filter { it.substringAfterLast("/").startsWith(firstWord, ignoreCase = true) }
                    if (similarPaths.isNotEmpty()) {
                        android.util.Log.d("MainActivity", "Found ${similarPaths.size} path(s) starting with '$firstWord':")
                        similarPaths.take(5).forEach { path ->
                            android.util.Log.d("MainActivity", "  - '$path'")
                        }
                    }
                }

                return null
            }

            android.util.Log.d("MainActivity", "Game found in gamelist!")

            // Find the <desc> tag after this <path> tag
            val gameStartIndex = pathMatch.range.first

            // Search for <desc>...</desc> within this game entry (before next <game> tag)
            val remainingXml = xmlContent.substring(gameStartIndex)
            val nextGameIndex = remainingXml.indexOf("<game>", startIndex = 1)
            val searchSpace = if (nextGameIndex > 0) {
                remainingXml.substring(0, nextGameIndex)
            } else {
                remainingXml
            }

            // Extract description text between <desc> and </desc>
            val descPattern = "<desc>([\\s\\S]*?)</desc>".toRegex()
            val descMatch = descPattern.find(searchSpace)

            return if (descMatch != null) {
                // Decode XML entities in the description text (not in path)
                val rawDescription = descMatch.groupValues[1].trim()
                val description = decodeXmlEntities(rawDescription)
                android.util.Log.d("MainActivity", "Found description: ${description.take(100)}...")
                description
            } else {
                android.util.Log.d("MainActivity", "No <desc> tag found for game: $relativeGamePath")
                null
            }

        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error parsing gamelist.xml", e)
            return null
        }
    }

    /**
     * Decode XML entities to regular characters for display
     */
    private fun decodeXmlEntities(text: String): String {
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
    }

    fun updateWidgetsForCurrentGame() {
        android.util.Log.d("MainActivity", "═══ updateWidgetsForCurrentGame START ═══")
        android.util.Log.d("MainActivity", "Current state: $state")

        // Show widgets in game browsing or screensaver modes
        when (state) {
            is AppState.GameBrowsing, is AppState.Screensaver -> {
                // Continue with widget loading
            }
            else -> {
                hideWidgetsForState()
                return
            }
        }

        // Get current game context from state
        val systemName = state.getCurrentSystemName()
        val gameFilename = state.getCurrentGameFilename()

        if (systemName != null && gameFilename != null) {
            loadGameWidgets(systemName, gameFilename)
        } else if (state is AppState.SystemBrowsing) {
            showSystemViewState()
        } else {
            android.util.Log.d("MainActivity", "System or game filename is null - not updating widgets")
        }

        android.util.Log.d("MainActivity", "═══ updateWidgetsForCurrentGame END ═══")
    }

    /**
     * Clear widget container while preserving grid overlay
     */
    private fun clearWidgetContainer() {
        val childCount = widgetContainer.childCount
        for (i in childCount - 1 downTo 0) {
            val child = widgetContainer.getChildAt(i)
            if (child !is GridOverlayView) {
                widgetContainer.removeView(child)
            }
        }
        activeWidgets.clear()
        android.util.Log.d("MainActivity", "Cleared widget container (preserved grid)")
    }

    /**
     * Find appropriate image file for widget based on type
     */
    private fun findWidgetImageFile(
        widget: Widget,
        systemName: String,
        gameName: String,
        gameFilename: String
    ): File? {
        val primaryFile = when (widget.imageType) {
            Widget.ImageType.MARQUEE ->
                findImageInFolder(systemName, gameFilename, "marquees")
            Widget.ImageType.BOX_2D ->
                findImageInFolder(systemName, gameFilename, "covers")
            Widget.ImageType.BOX_3D ->
                findImageInFolder(systemName, gameFilename, "3dboxes")
            Widget.ImageType.MIX_IMAGE ->
                findImageInFolder(systemName, gameFilename, "miximages")
            Widget.ImageType.BACK_COVER ->
                findImageInFolder(systemName, gameFilename, "backcovers")
            Widget.ImageType.PHYSICAL_MEDIA ->
                findImageInFolder(systemName, gameFilename, "physicalmedia")
            Widget.ImageType.SCREENSHOT ->
                findImageInFolder(systemName, gameFilename, "screenshots")
            Widget.ImageType.FANART ->
                findImageInFolder(systemName, gameFilename, "fanart")
            Widget.ImageType.TITLE_SCREEN ->
                findImageInFolder(systemName, gameFilename, "titlescreens")
            Widget.ImageType.GAME_DESCRIPTION -> null  // Text widget
            Widget.ImageType.SYSTEM_LOGO -> null
            Widget.ImageType.COLOR_BACKGROUND -> null  // NEW: No file needed
            Widget.ImageType.CUSTOM_IMAGE -> null      // NEW: Path already set
            Widget.ImageType.RANDOM_FANART -> null     // NEW: Path set dynamically
            Widget.ImageType.RANDOM_SCREENSHOT -> null // NEW: Path set dynamically
        }
        // If primary found, return it
        if (primaryFile != null) return primaryFile

        // Fallback logic for FANART and SCREENSHOT only
        return when (widget.imageType) {
            Widget.ImageType.FANART -> {
                android.util.Log.d("MainActivity", "  Fanart not found, trying screenshot fallback")
                findImageInFolder(systemName, gameFilename, "screenshots")
            }
            Widget.ImageType.SCREENSHOT -> {
                android.util.Log.d("MainActivity", "  Screenshot not found, trying fanart fallback")
                findImageInFolder(systemName, gameFilename, "fanart")
            }
            else -> null // No fallback for other types
        }
    }

    /**
     * Create widget instance with appropriate image path or description
     */
    private fun createWidgetForGame(
        widget: Widget,
        systemName: String,
        gameName: String,
        gameFilename: String
    ): Widget {
        android.util.Log.d("MainActivity", "  Looking for images for: $gameName")

        // Handle special widget types that don't need image file lookup
        when (widget.imageType) {
            Widget.ImageType.GAME_DESCRIPTION -> {
                val description = getGameDescription(systemName, gameFilename)
                android.util.Log.d("MainActivity", "  Updating description widget: ${description?.take(50)}")
                return widget.copy(imagePath = description ?: "")
            }
            Widget.ImageType.COLOR_BACKGROUND -> {
                android.util.Log.d("MainActivity", "  Color background widget - using as-is")
                return widget
            }
            Widget.ImageType.CUSTOM_IMAGE -> {
                android.util.Log.d("MainActivity", "  Custom image widget - using as-is")
                return widget
            }
            else -> {
                // Continue to normal image file lookup for game artwork widgets
            }
        }

        val imageFile = findWidgetImageFile(widget, systemName, gameName, gameFilename)

        android.util.Log.d("MainActivity", "  Image file: ${imageFile?.absolutePath ?: "NULL"}")
        android.util.Log.d("MainActivity", "  Image exists: ${imageFile?.exists()}")

        return when {
            imageFile != null && imageFile.exists() -> {
                android.util.Log.d("MainActivity", "  Creating widget with new image")
                widget.copy(imagePath = imageFile.absolutePath)
            }
            else -> {
                android.util.Log.d("MainActivity", "  No valid image found, using empty path")
                if (widget.imageType == Widget.ImageType.MARQUEE) {
                    widget.copy(imagePath = "marquee://$gameName")
                } else {
                    widget.copy(imagePath = "")
                }
            }
        }
    }

    /**
     * Load and display all game widgets for current game
     */
    private fun loadGameWidgets(systemName: String, gameFilename: String) {
        // Load saved widgets
        val allWidgets = widgetManager.loadWidgets()

        // Filter for GAME context widgets only
        val gameWidgets = allWidgets.filter { it.widgetContext == Widget.WidgetContext.GAME }
        android.util.Log.d("MainActivity", "Loaded ${gameWidgets.size} game widgets from storage")

        // Clear existing widget views
        clearWidgetContainer()

        // Sort widgets by z-index
        val sortedWidgets = gameWidgets.sortedBy { it.zIndex }
        android.util.Log.d("MainActivity", "Sorted ${sortedWidgets.size} game widgets by z-index")

        // Reload all widgets with current game images
        val gameName = sanitizeGameFilename(gameFilename).substringBeforeLast('.')

        sortedWidgets.forEachIndexed { index, widget ->
            android.util.Log.d("MainActivity", "Processing widget $index: type=${widget.imageType}, zIndex=${widget.zIndex}")

            val widgetToAdd = createWidgetForGame(widget, systemName, gameName, gameFilename)
            addWidgetToScreenWithoutSaving(widgetToAdd)
            android.util.Log.d("MainActivity", "  Widget added to screen")
        }

        android.util.Log.d("MainActivity", "Total widgets added: ${activeWidgets.size}")
        android.util.Log.d("MainActivity", "Widget container children: ${widgetContainer.childCount}")

        // Make sure container is visible
        widgetContainer.visibility = View.VISIBLE
        updateGridOverlay()
        android.util.Log.d("MainActivity", "Widget container visibility: ${widgetContainer.visibility}")
    }

    /**
     * Handle system view state (show grid but no game widgets)
     */
    private fun showSystemViewState() {
        android.util.Log.d("MainActivity", "System view - showing grid only")

        // Clear game widgets
        widgetContainer.removeAllViews()
        activeWidgets.clear()

        // Keep container visible and show grid if enabled
        widgetContainer.visibility = View.VISIBLE
        updateGridOverlay()

        android.util.Log.d("MainActivity", "System view setup complete")
    }

    /**
     * Hide widgets when not in appropriate state
     */
    private fun hideWidgetsForState() {
        android.util.Log.d("MainActivity", "Not in game view - hiding widgets")
        widgetContainer.visibility = View.GONE

        // Only hide grid if showGrid is actually off
        if (!showGrid) {
            gridOverlayView?.visibility = View.GONE
        }
    }

    private fun addWidgetToScreenWithoutSaving(widget: Widget) {
        // Create a variable to hold the widget view reference
        var widgetViewRef: WidgetView? = null

        val widgetView = WidgetView(
            this,
            widget,
            onDelete = { view ->
                removeWidget(view)
            },
            onUpdate = { updatedWidget ->
                // Update the widget in storage
                val allWidgets = widgetManager.loadWidgets().toMutableList()
                val widgetIndex = allWidgets.indexOfFirst { it.id == updatedWidget.id }
                if (widgetIndex != -1) {
                    allWidgets[widgetIndex] = updatedWidget
                    widgetManager.saveWidgets(allWidgets)
                    Log.d(
                        "MainActivity",
                        "Widget ${updatedWidget.id} updated: pos=(${updatedWidget.x}, ${updatedWidget.y}), size=(${updatedWidget.width}, ${updatedWidget.height})"
                    )
                }
            },
            imageManager = imageManager
        )

        widgetViewRef = widgetView

        // Apply current lock state to new widget
        widgetView.setLocked(widgetsLocked)

        // Apply current snap to grid state
        widgetView.setSnapToGrid(snapToGrid, gridSize)

        activeWidgets.add(widgetView)
        widgetContainer.addView(widgetView)
    }

    private fun hideWidgets() {
        // Remove all widget views but keep grid if it should be shown
        val childCount = widgetContainer.childCount
        for (i in childCount - 1 downTo 0) {
            val child = widgetContainer.getChildAt(i)
            if (child !is GridOverlayView) {
                widgetContainer.removeView(child)
            }
        }
        activeWidgets.clear()

        // Only hide container if grid is also off
        if (!showGrid) {
            widgetContainer.visibility = View.GONE
        }

        android.util.Log.d("MainActivity", "Hiding widgets, showGrid=$showGrid, container visibility=${widgetContainer.visibility}")
    }

    private fun showWidgets() {
        // Show widgets/grid in all views (game browsing, gameplay, system view, screensaver)
        widgetContainer.visibility = View.VISIBLE
        updateGridOverlay()
        android.util.Log.d("MainActivity", "Showing widgets/grid")
    }

    fun saveAllWidgets() {
        android.util.Log.d("MainActivity", "saveAllWidgets called, active widgets count: ${activeWidgets.size}")

        // Do not save during screensaver - activeWidgets reflects screensaver game widgets,
        // not the user's configured widgets for either context
        if (state is AppState.Screensaver) {
            android.util.Log.d("MainActivity", "saveAllWidgets skipped - screensaver active")
            return
        }

        // Do not save if activeWidgets is empty - screensaver clears widgets on start,
        // so any save attempt before they are reloaded on exit would wipe stored widgets
        if (activeWidgets.isEmpty()) {
            android.util.Log.d("MainActivity", "saveAllWidgets skipped - activeWidgets is empty")
            return
        }

        // Load ALL existing widgets
        val allExistingWidgets = widgetManager.loadWidgets().toMutableList()

        // Determine which context we're currently in
        val currentContext = if (state is AppState.SystemBrowsing) {
            Widget.WidgetContext.SYSTEM
        } else {
            Widget.WidgetContext.GAME
        }

        // Remove widgets of the CURRENT context only
        val widgetsToKeep = allExistingWidgets.filter { it.widgetContext != currentContext }

        // Add current active widgets (they're all from the current context)
        val updatedWidgets = widgetsToKeep + activeWidgets.map { it.widget }

        widgetManager.saveWidgets(updatedWidgets)
        android.util.Log.d("MainActivity", "Widgets saved")
    }

    private fun updateGridOverlay() {
        if (showGrid) {
            // Make container visible when showing grid (needed for system view)
            if (widgetContainer.visibility != View.VISIBLE) {
                widgetContainer.visibility = View.VISIBLE
            }

            // Always recreate grid overlay to ensure it's properly attached
            if (gridOverlayView != null && gridOverlayView?.parent != null) {
                widgetContainer.removeView(gridOverlayView)
                gridOverlayView = null
            }

            // Create fresh grid overlay
            gridOverlayView = GridOverlayView(this, gridSize)
            val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            widgetContainer.addView(gridOverlayView, 0)
            android.util.Log.d("MainActivity", "Grid overlay recreated and added")
        } else {
            // Remove grid overlay completely
            if (gridOverlayView != null) {
                widgetContainer.removeView(gridOverlayView)
                gridOverlayView = null
                android.util.Log.d("MainActivity", "Grid overlay removed")
            }
        }
    }

    private fun isWidgetSelected(x: Float, y: Float): Boolean {
        for (widgetView in activeWidgets) {
            val location = IntArray(2)
            widgetView.getLocationOnScreen(location)
            val widgetX = location[0].toFloat()
            val widgetY = location[1].toFloat()

            if (x >= widgetX && x <= widgetX + widgetView.width &&
                y >= widgetY && y <= widgetY + widgetView.height) {
                // Check if this widget is actually selected
                return widgetView.isWidgetSelected
            }
        }
        return false
    }

    fun moveWidgetForward(widgetView: WidgetView) {
        // Find the widget with the next higher z-index
        val currentZ = widgetView.widget.zIndex
        val nextHigherWidget = activeWidgets
            .filter { it.widget.zIndex > currentZ }
            .minByOrNull { it.widget.zIndex }

        if (nextHigherWidget != null) {
            // Swap z-indices
            val temp = widgetView.widget.zIndex
            widgetView.widget.zIndex = nextHigherWidget.widget.zIndex
            nextHigherWidget.widget.zIndex = temp

            reorderWidgetsByZIndex()
            android.util.Log.d("MainActivity", "Widget moved forward to z-index ${widgetView.widget.zIndex}")
        } else {
            android.widget.Toast.makeText(this, "Already at front", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun moveWidgetBackward(widgetView: WidgetView) {
        // Find the widget with the next lower z-index
        val currentZ = widgetView.widget.zIndex
        val nextLowerWidget = activeWidgets
            .filter { it.widget.zIndex < currentZ }
            .maxByOrNull { it.widget.zIndex }

        if (nextLowerWidget != null) {
            // Swap z-indices
            val temp = widgetView.widget.zIndex
            widgetView.widget.zIndex = nextLowerWidget.widget.zIndex
            nextLowerWidget.widget.zIndex = temp

            reorderWidgetsByZIndex()
            android.util.Log.d("MainActivity", "Widget moved backward to z-index ${widgetView.widget.zIndex}")
        } else {
            android.widget.Toast.makeText(this, "Already at back", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun reorderWidgetsByZIndex() {
        // Sort widgets by z-index
        val sortedWidgets = activeWidgets.sortedBy { it.widget.zIndex }

        // Remove only widget views (preserve grid overlay)
        val childCount = widgetContainer.childCount
        for (i in childCount - 1 downTo 0) {
            val child = widgetContainer.getChildAt(i)
            if (child !is GridOverlayView) {
                widgetContainer.removeView(child)
            }
        }

        // Re-add widgets in sorted order (lower z-index = added first = appears behind)
        // Grid overlay was added at index 0, so widgets will be added after it
        sortedWidgets.forEach { widgetView ->
            widgetContainer.addView(widgetView)
        }

        // Save the updated z-indices
        saveAllWidgetsWithZIndex()
    }

    private fun saveAllWidgetsWithZIndex() {
        if (state is AppState.Screensaver) {
            android.util.Log.d("MainActivity", "saveAllWidgetsWithZIndex skipped - screensaver active")
            return
        }
        // Load ALL existing widgets
        val allExistingWidgets = widgetManager.loadWidgets().toMutableList()
        android.util.Log.d("MainActivity", "saveAllWidgetsWithZIndex: Loaded ${allExistingWidgets.size} existing widgets from storage")

        // Determine which context we're currently in
        val currentContext = if (state is AppState.SystemBrowsing) {
            Widget.WidgetContext.SYSTEM
        } else {
            Widget.WidgetContext.GAME
        }
        android.util.Log.d("MainActivity", "saveAllWidgetsWithZIndex: Current context: $currentContext")

        // Remove widgets of the CURRENT context only
        val widgetsToKeep = allExistingWidgets.filter { it.widgetContext != currentContext }
        android.util.Log.d("MainActivity", "saveAllWidgetsWithZIndex: Keeping ${widgetsToKeep.size} widgets from OTHER context")

        // Add current active widgets (they're all from the current context)
        val updatedWidgets = widgetsToKeep + activeWidgets.map { it.widget }
        android.util.Log.d("MainActivity", "saveAllWidgetsWithZIndex: Total widgets to save: ${updatedWidgets.size}")

        widgetManager.saveWidgets(updatedWidgets)
        android.util.Log.d("MainActivity", "saveAllWidgetsWithZIndex: Saved ${updatedWidgets.size} widgets with z-indices")
    }

    fun deselectAllWidgets() {
        activeWidgets.forEach { it.deselect() }
    }

    companion object {
        const val COLUMN_COUNT_KEY = "column_count"
    }
}