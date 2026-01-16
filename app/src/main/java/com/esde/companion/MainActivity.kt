package com.esde.companion

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.os.FileObserver
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import android.graphics.drawable.Drawable
import android.view.animation.DecelerateInterpolator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import android.app.ActivityOptions
import android.provider.Settings
import android.hardware.display.DisplayManager
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import java.io.File
import kotlin.math.abs
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import android.os.Handler
import android.os.Looper
import com.esde.companion.ResizableWidgetContainer

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
    private lateinit var prefs: SharedPreferences
    private lateinit var appLaunchPrefs: AppLaunchPreferences

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
    private val gridSize = 40f
    private var showGrid = false
    private var isInteractingWithWidget = false
    private var longPressHandler: Handler? = null
    private var longPressRunnable: Runnable? = null
    private var longPressTriggered = false
    private var touchDownX = 0f
    private var touchDownY = 0f
    private val LONG_PRESS_TIMEOUT = 500L
    private var widgetMenuShowing = false
    private var widgetMenuDialog: android.app.AlertDialog? = null

    private var fileObserver: FileObserver? = null
    private var isSystemScrollActive = false
    private var currentGameName: String? = null  // Display name from ES-DE
    private var currentGameFilename: String? = null  // Filename
    private var currentSystemName: String? = null  // Current system
    private var allApps = listOf<ResolveInfo>()  // Store all apps for search filtering
    private var isGamePlaying = false  // Track if game is running on other screen
    private var hasWindowFocus = true  // Track if app has window focus (is on top)
    private var playingGameFilename: String? = null  // Filename of currently playing game
    private var isScreensaverActive = false  // Track if ES-DE screensaver is running
    private var wasInSystemViewBeforeScreensaver = false
    private var systemBeforeScreensaver: String? = null
    private var gameFilenameBeforeScreensaver: String? = null
    private var gameNameBeforeScreensaver: String? = null
    private var screensaverGameFilename: String? = null  // Current screensaver game filename
    private var screensaverGameName: String? = null  // Current screensaver game name
    private var screensaverSystemName: String? = null  // Current screensaver system name
    private var isLaunchingFromScreensaver = false  // Track if we're launching game from screensaver
    private var screensaverInitialized = false  // Track if screensaver has loaded its first game

    // Video playback variables
    private var player: ExoPlayer? = null
    private lateinit var videoView: PlayerView
    private var videoDelayHandler: Handler? = null
    private var videoDelayRunnable: Runnable? = null
    private var currentVideoPath: String? = null
    private var volumeChangeReceiver: BroadcastReceiver? = null

    // Flag to skip reload in onResume (used when returning from settings with no changes)
    private var skipNextReload = false

    // Flag to track if marquee is showing text drawable (needs WRAP_CONTENT)
    private var marqueeShowingText = false

    // Double-tap detection variables
    private var tapCount = 0
    private var lastTapTime = 0L
    private val DOUBLE_TAP_TIMEOUT = 300L // 300ms window for double-tap

    // Scripts verification
    private var isWaitingForScriptVerification = false
    private var scriptVerificationHandler: Handler? = null
    private var scriptVerificationRunnable: Runnable? = null
    private var currentVerificationDialog: AlertDialog? = null
    private var currentErrorDialog: AlertDialog? = null
    private val SCRIPT_VERIFICATION_TIMEOUT = 15000L  // 15 seconds

    // Dynamic debouncing for fast scrolling - separate tracking for systems and games
    private val imageLoadHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var imageLoadRunnable: Runnable? = null
    private var lastSystemScrollTime = 0L
    private var lastGameScrollTime = 0L

    // System scrolling: Enable debouncing to reduce rapid updates
    private val SYSTEM_FAST_SCROLL_THRESHOLD = 250L // If scrolling faster than 250ms between changes, it's "fast"
    private val SYSTEM_FAST_SCROLL_DELAY = 300L // 300ms delay for fast system scrolling
    private val SYSTEM_SLOW_SCROLL_DELAY = 150L // 150ms delay for slow system scrolling

    // Game scrolling: No debouncing for instant response
    private val GAME_FAST_SCROLL_THRESHOLD = 250L
    private val GAME_FAST_SCROLL_DELAY = 0L // No delay for games
    private val GAME_SLOW_SCROLL_DELAY = 0L // No delay for games

    // Filter out game-select on game-start and game-end
    private var lastGameStartTime = 0L
    private var lastGameEndTime = 0L
    private val GAME_EVENT_DEBOUNCE = 2000L  // 2 seconds

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
            val needsRecreate = result.data?.getBooleanExtra("NEEDS_RECREATE", false) ?: false
            val appsHiddenChanged = result.data?.getBooleanExtra("APPS_HIDDEN_CHANGED", false) ?: false
            val closeDrawer = result.data?.getBooleanExtra("CLOSE_DRAWER", false) ?: false
            val videoSettingsChanged = result.data?.getBooleanExtra("VIDEO_SETTINGS_CHANGED", false) ?: false
            val logoSizeChanged = result.data?.getBooleanExtra("LOGO_SIZE_CHANGED", false) ?: false
            val mediaPathChanged = result.data?.getBooleanExtra("MEDIA_PATH_CHANGED", false) ?: false
            val imagePreferenceChanged = result.data?.getBooleanExtra("IMAGE_PREFERENCE_CHANGED", false) ?: false
            val logoTogglesChanged = result.data?.getBooleanExtra("LOGO_TOGGLES_CHANGED", false) ?: false
            val gameLaunchBehaviorChanged = result.data?.getBooleanExtra("GAME_LAUNCH_BEHAVIOR_CHANGED", false) ?: false
            val screensaverBehaviorChanged = result.data?.getBooleanExtra("SCREENSAVER_BEHAVIOR_CHANGED", false) ?: false
            val startVerification = result.data?.getBooleanExtra("START_SCRIPT_VERIFICATION", false) ?: false
            val customBackgroundChanged = result.data?.getBooleanExtra("CUSTOM_BACKGROUND_CHANGED", false) ?: false

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
            } else if (gameLaunchBehaviorChanged && isGamePlaying) {
                // Game launch behavior changed while game is playing - update display
                handleGameStart()
                // Skip reload in onResume to prevent override
                skipNextReload = true
            } else if (screensaverBehaviorChanged && isScreensaverActive) {
                // Screensaver behavior changed while screensaver is active - update display
                handleScreensaverStart()
                // Skip reload in onResume to prevent override
                skipNextReload = true
            } else if (customBackgroundChanged) {
                // Custom background changed - reload to apply changes
                if (isSystemScrollActive) {
                    loadSystemImage()
                } else {
                    loadGameInfo()
                }
            } else if (videoSettingsChanged || logoSizeChanged || mediaPathChanged || imagePreferenceChanged || logoTogglesChanged) {
                // Settings that affect displayed content changed - reload to apply changes
                if (isSystemScrollActive) {
                    loadSystemImage()
                } else {
                    loadGameInfo()
                }
            } else {
                // No settings changed that require reload - skip the reload in onResume
                skipNextReload = true
            }
            // Note: Video audio changes are handled automatically in onResume

            // Start script verification if requested
            if (startVerification) {
                // Delay slightly to let UI settle
                Handler(Looper.getMainLooper()).postDelayed({
                    startScriptVerification()
                }, 500)
            }
        }
    }

    private fun updateDimmingOverlay() {
        val dimmingPercent = prefs.getInt("dimming", 25)

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

        prefs = getSharedPreferences("ESDESecondScreenPrefs", MODE_PRIVATE)
        appLaunchPrefs = AppLaunchPreferences(this)

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

        // Initialize widget system
        widgetContainer = findViewById(R.id.widgetContainer)
        widgetManager = WidgetManager(this)
        // Load lock state
        widgetsLocked = prefs.getBoolean("widgets_locked", true)
        // Load snap to grid state
        snapToGrid = prefs.getBoolean("snap_to_grid", true)
        // Load show grid state
        showGrid = prefs.getBoolean("show_grid", false)

        // Create default widgets on first launch
        createDefaultWidgets()

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
        loadWidgets()

        // Apply drawer transparency
        updateDrawerTransparency()

        val logsDir = File(getLogsPath())
        android.util.Log.d("MainActivity", "Logs directory: ${logsDir.absolutePath}")
        android.util.Log.d("MainActivity", "Logs directory exists: ${logsDir.exists()}")

        val systemScrollFile = File(logsDir, "esde_system_name.txt")
        val gameScrollFile = File(logsDir, "esde_game_filename.txt")

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
    }

    private fun checkAndLaunchSetupWizard() {
        // Check if setup has been completed
        val hasCompletedSetup = prefs.getBoolean("setup_completed", false)

        // Check if permissions are granted
        val hasPermission = when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R ->
                Environment.isExternalStorageManager()
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M -> {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            else -> true
        }

        // Launch setup wizard immediately if:
        // 1. Setup not completed, OR
        // 2. Missing permissions
        if (!hasCompletedSetup || !hasPermission) {
            android.util.Log.d("MainActivity", "Setup incomplete or missing permissions - launching wizard immediately")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, SettingsActivity::class.java)
                intent.putExtra("AUTO_START_WIZARD", true)
                settingsLauncher.launch(intent)
            }, 1000)
            return
        }

        // For script check, wait for SD card if needed (setup is complete and has permissions)
        checkScriptsWithRetry()
    }

    /**
     * Check for scripts with retry logic to handle SD card mounting delays
     */
    private fun checkScriptsWithRetry(attempt: Int = 0, maxAttempts: Int = 5) {
        val scriptsPath = prefs.getString("scripts_path", null)

        // If no custom scripts path is set, scripts are likely on internal storage
        // Check immediately without retry
        if (scriptsPath == null || scriptsPath.startsWith("/storage/emulated/0")) {
            android.util.Log.d("MainActivity", "Scripts on internal storage - checking immediately")
            val hasCorrectScripts = checkForCorrectScripts()
            if (!hasCorrectScripts) {
                android.util.Log.d("MainActivity", "Scripts missing/outdated on internal storage - showing dialog")

                // Check if scripts exist at all (missing vs outdated)
                val scriptsDir = File(scriptsPath ?: "/storage/emulated/0/ES-DE/scripts")
                val gameSelectScript = File(scriptsDir, "game-select/esdecompanion-game-select.sh")

                if (gameSelectScript.exists()) {
                    // Scripts exist but are outdated - show update dialog
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        showScriptsUpdateAvailableDialog()
                    }, 1000)
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
            val delayMs = when (attempt) {
                0 -> 1000L  // 1 second
                1 -> 2000L  // 2 seconds
                2 -> 3000L  // 3 seconds
                3 -> 4000L  // 4 seconds
                else -> 5000L  // 5 seconds
            }

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
                }, 1000)
            } else {
                // Max attempts reached and still not accessible
                // SD card might not be mounted - show a helpful message
                android.util.Log.w("MainActivity", "Scripts path not accessible after $maxAttempts attempts: $scriptsPath")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showSdCardNotMountedDialog(scriptsPath)
                }, 1000)
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
        }, 1000)
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
        // Get scripts path (return false if not set)
        val scriptsPath = prefs.getString("scripts_path", null) ?: return false
        val scriptsDir = File(scriptsPath)

        // Define required scripts with their expected content signatures
        val requiredScripts = mapOf(
            "game-select/esdecompanion-game-select.sh" to listOf(
                "LOG_DIR=\"/storage/emulated/0/ES-DE Companion/logs\"",
                "esde_game_filename.txt",
                "esde_game_name.txt",
                "esde_game_system.txt",
                // NEW: Check that it's NOT using basename (old version)
                "!basename"
            ),
            "system-select/esdecompanion-system-select.sh" to listOf(
                "LOG_DIR=\"/storage/emulated/0/ES-DE Companion/logs\"",
                "esde_system_name.txt"
            ),
            "game-start/esdecompanion-game-start.sh" to listOf(
                "LOG_DIR=\"/storage/emulated/0/ES-DE Companion/logs\"",
                "esde_gamestart_filename.txt",
                "esde_gamestart_name.txt",
                "esde_gamestart_system.txt",
                // NEW: Check that it's NOT using basename or clean_filename (old version)
                "!basename",
                "!clean_filename"
            ),
            "game-end/esdecompanion-game-end.sh" to listOf(
                "LOG_DIR=\"/storage/emulated/0/ES-DE Companion/logs\"",
                "esde_gameend_filename.txt",
                "esde_gameend_name.txt",
                "esde_gameend_system.txt",
                // NEW: Check that it's NOT using basename or clean_filename (old version)
                "!basename",
                "!clean_filename"
            ),
            "screensaver-start/esdecompanion-screensaver-start.sh" to listOf(
                "LOG_DIR=\"/storage/emulated/0/ES-DE Companion/logs\"",
                "esde_screensaver_start.txt"
            ),
            "screensaver-end/esdecompanion-screensaver-end.sh" to listOf(
                "LOG_DIR=\"/storage/emulated/0/ES-DE Companion/logs\"",
                "esde_screensaver_end.txt"
            ),
            "screensaver-game-select/esdecompanion-screensaver-game-select.sh" to listOf(
                "LOG_DIR=\"/storage/emulated/0/ES-DE Companion/logs\"",
                "esde_screensavergameselect_filename.txt",
                "esde_screensavergameselect_name.txt",
                "esde_screensavergameselect_system.txt",
                // NEW: Check that it's NOT using basename or clean_filename (old version)
                "!basename",
                "!clean_filename"
            )
        )

        // Check each script exists and contains expected content
        for ((scriptPath, expectedContent) in requiredScripts) {
            val scriptFile = File(scriptsDir, scriptPath)

            // Check if file exists
            if (!scriptFile.exists()) {
                return false
            }

            // Read and validate content
            try {
                val content = scriptFile.readText()

                // Check if all expected strings are present in the content
                for (expected in expectedContent) {
                    if (expected.startsWith("!")) {
                        // Negative check - this string should NOT be present (old version)
                        val unwantedString = expected.substring(1)
                        if (content.contains(unwantedString)) {
                            android.util.Log.w("MainActivity", "Script contains old code: $scriptPath (found: $unwantedString)")
                            return false // Script is outdated
                        }
                    } else {
                        // Positive check - this string SHOULD be present
                        if (!content.contains(expected)) {
                            return false
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error reading script: $scriptPath", e)
                return false
            }
        }

        // All scripts exist with correct content
        return true
    }

    /**
     * Show dialog when old scripts are detected
     */
    private fun showScriptsUpdateAvailableDialog() {
        AlertDialog.Builder(this)
            .setTitle("Script Update Available")
            .setMessage("Your ES-DE integration scripts need to be updated to support games in subfolders.\n\n" +
                    "Changes:\n" +
                    "• Scripts now pass full file paths\n" +
                    "• App handles subfolder detection\n" +
                    "• Improves compatibility with organized ROM collections\n\n" +
                    "Would you like to update the scripts now?")
            .setPositiveButton("Update Scripts") { _, _ ->
                updateScriptsDirectly()
            }
            .setNegativeButton("Later") { _, _ ->
                // User declined - show a toast reminder
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
     */
    private fun updateScriptsDirectly() {
        val scriptsPath = prefs.getString("scripts_path", "/storage/emulated/0/ES-DE/scripts")
            ?: "/storage/emulated/0/ES-DE/scripts"

        try {
            val scriptsDir = File(scriptsPath)

            // Create all script subdirectories
            val gameSelectDir = File(scriptsDir, "game-select")
            val systemSelectDir = File(scriptsDir, "system-select")
            val gameStartDir = File(scriptsDir, "game-start")
            val gameEndDir = File(scriptsDir, "game-end")
            val screensaverStartDir = File(scriptsDir, "screensaver-start")
            val screensaverEndDir = File(scriptsDir, "screensaver-end")
            val screensaverGameSelectDir = File(scriptsDir, "screensaver-game-select")

            gameSelectDir.mkdirs()
            systemSelectDir.mkdirs()
            gameStartDir.mkdirs()
            gameEndDir.mkdirs()
            screensaverStartDir.mkdirs()
            screensaverEndDir.mkdirs()
            screensaverGameSelectDir.mkdirs()

            // 1. esdecompanion-game-select.sh
            val gameSelectScriptFile = File(gameSelectDir, "esdecompanion-game-select.sh")
            gameSelectScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_game_filename.txt"
echo -n "${'$'}2" > "${'$'}LOG_DIR/esde_game_name.txt"
echo -n "${'$'}3" > "${'$'}LOG_DIR/esde_game_system.txt"
""")
            gameSelectScriptFile.setExecutable(true)

            // 2. esdecompanion-system-select.sh
            val systemSelectScriptFile = File(systemSelectDir, "esdecompanion-system-select.sh")
            systemSelectScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

printf "%s" "${'$'}1" > "${'$'}LOG_DIR/esde_system_name.txt" &
""")
            systemSelectScriptFile.setExecutable(true)

            // 3. esdecompanion-game-start.sh
            val gameStartScriptFile = File(gameStartDir, "esdecompanion-game-start.sh")
            gameStartScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_gamestart_filename.txt"
echo -n "${'$'}2" > "${'$'}LOG_DIR/esde_gamestart_name.txt"
echo -n "${'$'}3" > "${'$'}LOG_DIR/esde_gamestart_system.txt"
""")
            gameStartScriptFile.setExecutable(true)

            // 4. esdecompanion-game-end.sh
            val gameEndScriptFile = File(gameEndDir, "esdecompanion-game-end.sh")
            gameEndScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_gameend_filename.txt"
echo -n "${'$'}2" > "${'$'}LOG_DIR/esde_gameend_name.txt"
echo -n "${'$'}3" > "${'$'}LOG_DIR/esde_gameend_system.txt"
""")
            gameEndScriptFile.setExecutable(true)

            // 5. esdecompanion-screensaver-start.sh
            val screensaverStartScriptFile = File(screensaverStartDir, "esdecompanion-screensaver-start.sh")
            screensaverStartScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_screensaver_start.txt"

""")
            screensaverStartScriptFile.setExecutable(true)

            // 6. esdecompanion-screensaver-end.sh
            val screensaverEndScriptFile = File(screensaverEndDir, "esdecompanion-screensaver-end.sh")
            screensaverEndScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_screensaver_end.txt"

""")
            screensaverEndScriptFile.setExecutable(true)

            // 7. esdecompanion-screensaver-game-select.sh
            val screensaverGameSelectScriptFile = File(screensaverGameSelectDir, "esdecompanion-screensaver-game-select.sh")
            screensaverGameSelectScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_screensavergameselect_filename.txt"
echo -n "${'$'}2" > "${'$'}LOG_DIR/esde_screensavergameselect_name.txt"
echo -n "${'$'}3" > "${'$'}LOG_DIR/esde_screensavergameselect_system.txt"
""")
            screensaverGameSelectScriptFile.setExecutable(true)

            // Show success message
            Toast.makeText(
                this,
                "Scripts updated successfully!",
                Toast.LENGTH_LONG
            ).show()

            // Start verification
            Handler(Looper.getMainLooper()).postDelayed({
                startScriptVerification()
            }, 1000)

        } catch (e: Exception) {
            // Show error message
            Toast.makeText(
                this,
                "Error updating scripts: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("MainActivity", "Error updating scripts", e)
        }
    }

    private fun createDefaultWidgets() {
        // Check if we've already created default widgets
        val hasCreatedDefaults = prefs.getBoolean("default_widgets_created", false)
        if (hasCreatedDefaults) {
            android.util.Log.d("MainActivity", "Default widgets already created on previous launch")
            return
        }

        android.util.Log.d("MainActivity", "First launch - creating default widgets")

        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val centerY = displayMetrics.heightPixels / 2f

        // System logo size (medium equivalent - adjust as needed)
        val systemLogoWidth = 800f
        val systemLogoHeight = 300f

        // Game marquee size (medium equivalent - typically wider than system logo)
        val gameMarqueeWidth = 800f
        val gameMarqueeHeight = 300f

        // Create default system logo widget (centered)
        val systemLogoWidget = OverlayWidget(
            imageType = OverlayWidget.ImageType.SYSTEM_LOGO,
            imagePath = "",  // Will be updated when system loads
            x = centerX - (systemLogoWidth / 2),
            y = centerY - (systemLogoHeight / 2),
            width = systemLogoWidth,
            height = systemLogoHeight,
            zIndex = 0,
            widgetContext = OverlayWidget.WidgetContext.SYSTEM
        )

        // Create default game marquee widget (centered)
        val gameMarqueeWidget = OverlayWidget(
            imageType = OverlayWidget.ImageType.MARQUEE,
            imagePath = "",  // Will be updated when game loads
            x = centerX - (gameMarqueeWidth / 2),
            y = centerY - (gameMarqueeHeight / 2),
            width = gameMarqueeWidth,
            height = gameMarqueeHeight,
            zIndex = 0,
            widgetContext = OverlayWidget.WidgetContext.GAME
        )

        // Save both widgets
        val defaultWidgets = listOf(systemLogoWidget, gameMarqueeWidget)
        widgetManager.saveWidgets(defaultWidgets)

        // Mark that we've created default widgets
        prefs.edit().putBoolean("default_widgets_created", true).apply()

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
        videoDelayRunnable?.let { videoDelayHandler?.removeCallbacks(it) }
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

        // Clear search bar
        if (::appSearchBar.isInitialized) {
            appSearchBar.text.clear()
        }

        // Reload grid layout in case column count changed
        val columnCount = prefs.getInt("column_count", 4)
        appRecyclerView.layoutManager = GridLayoutManager(this, columnCount)

        // Reload images and videos based on current state (don't change modes)
        // Skip reload if returning from settings with no changes
        if (skipNextReload) {
            skipNextReload = false
            android.util.Log.d("MainActivity", "Skipping reload - no settings changed")
        } else {
            // Don't reload if game is playing or screensaver is active
            // This prevents unnecessary video loading during these states
            if (isGamePlaying) {
                android.util.Log.d("MainActivity", "Skipping reload - game playing")
            } else if (isScreensaverActive) {
                android.util.Log.d("MainActivity", "Skipping reload - screensaver active")
            } else {
                // Normal reload - this will reload both images and videos
                if (isSystemScrollActive) {
                    loadSystemImage()
                } else {
                    loadGameInfo()  // This calls handleVideoForGame() internally
                }
            }
        }
    }

    private fun updateBlurEffect() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val blurRadius = prefs.getInt("blur", 0)

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
        val transparencyPercent = prefs.getInt("drawer_transparency", 70)
        // Convert percentage (0-100) to hex alpha (00-FF)
        val alpha = (transparencyPercent * 255 / 100).coerceIn(0, 255)
        val hexAlpha = String.format("%02x", alpha)
        val colorString = "#${hexAlpha}000000"

        val color = android.graphics.Color.parseColor(colorString)
        appDrawer.setBackgroundColor(color)
    }

    private fun getMediaBasePath(): String {
        val customPath = prefs.getString("media_path", null)
        val path = customPath ?: "${Environment.getExternalStorageDirectory()}/ES-DE/downloaded_media"
        android.util.Log.d("ESDESecondScreen", "Media base path: $path")
        return path
    }

    private fun getSystemImagePath(): String {
        val customPath = prefs.getString("system_path", null)
        val path = customPath ?: "${Environment.getExternalStorageDirectory()}/ES-DE Companion/system_images"
        android.util.Log.d("ESDESecondScreen", "System image path: $path")
        return path
    }

    private fun getSystemLogosPath(): String {
        val customPath = prefs.getString("system_logos_path", null)
        val path = customPath ?: "${Environment.getExternalStorageDirectory()}/ES-DE Companion/system_logos"
        android.util.Log.d("ESDESecondScreen", "System logos path: $path")
        return path
    }

    private fun getLogsPath(): String {
        // Always use fixed internal storage location for logs
        // This ensures FileObserver works reliably (doesn't work well on SD card)
        val path = "/storage/emulated/0/ES-DE Companion/logs"
        android.util.Log.d("MainActivity", "Logs path: $path")
        return path
    }
    private fun loadFallbackBackground() {
        android.util.Log.d("MainActivity", "═══ loadFallbackBackground CALLED ═══")

        // Check if user has set a custom background
        val customBackgroundPath = prefs.getString("custom_background_uri", null)
        android.util.Log.d("MainActivity", "Custom background path: $customBackgroundPath")

        if (customBackgroundPath != null) {
            try {
                val file = File(customBackgroundPath)
                android.util.Log.d("MainActivity", "File exists: ${file.exists()}, canRead: ${file.canRead()}")

                if (file.exists() && file.canRead()) {
                    // Use loadImageWithAnimation for consistent behavior
                    loadImageWithAnimation(file, gameImageView) {
                        android.util.Log.d("MainActivity", "✓ Loaded custom background successfully")
                    }
                    android.util.Log.d("MainActivity", "Loading custom background from: $customBackgroundPath")
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
            // Copy asset to cache for loadImageWithAnimation
            val fallbackFile = File(cacheDir, "default_background.webp")
            if (!fallbackFile.exists()) {
                assets.open(assetPath).use { input ->
                    fallbackFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Use loadImageWithAnimation for consistent behavior
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
        val animationStyle = prefs.getString("animation_style", "scale_fade") ?: "scale_fade"

        // Get custom settings if using custom style
        val duration = if (animationStyle == "custom") {
            prefs.getInt("animation_duration", 250)
        } else {
            250
        }

        val scaleAmount = if (animationStyle == "custom") {
            prefs.getInt("animation_scale", 95) / 100f
        } else {
            0.95f
        }

        when (animationStyle) {
            "none" -> {
                // No animation - instant display with crossfade disabled
                Glide.with(this)
                    .load(imageFile)
                    .signature(com.bumptech.glide.signature.ObjectKey(getFileSignature(imageFile))) // Cache invalidation
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .dontAnimate()  // Disable all transitions
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            onComplete?.invoke()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            onComplete?.invoke()
                            return false
                        }
                    })
                    .into(targetView)
            }
            "fade" -> {
                // Use Glide's built-in crossfade transition
                Glide.with(this)
                    .load(imageFile)
                    .signature(com.bumptech.glide.signature.ObjectKey(getFileSignature(imageFile))) // Cache invalidation
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(duration))
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            onComplete?.invoke()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            onComplete?.invoke()
                            return false
                        }
                    })
                    .into(targetView)
            }
            else -> {
                // "scale_fade" - Use Glide crossfade + custom scale animation
                Glide.with(this)
                    .load(imageFile)
                    .signature(com.bumptech.glide.signature.ObjectKey(getFileSignature(imageFile))) // Cache invalidation
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(duration))
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            onComplete?.invoke()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            // Apply scale animation on top of Glide's crossfade
                            targetView.scaleX = scaleAmount
                            targetView.scaleY = scaleAmount
                            targetView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(duration.toLong())
                                .setInterpolator(DecelerateInterpolator())
                                .withEndAction {
                                    onComplete?.invoke()
                                }
                                .start()
                            return false
                        }
                    })
                    .into(targetView)
            }
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                // Only show widget menu if drawer is closed, NOT in system view, AND not touching a widget
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN &&
                    !isSystemScrollActive) {  // Added check for system view
                    // Check if long press is on a widget
                    val isTouchingWidget = isTouchOnWidget(e.x, e.y)
                    if (!isTouchingWidget) {
                        showCreateWidgetMenu()
                    }
                }
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
     * Show the black overlay instantly (no animation)
     */
    private fun showBlackOverlay() {
        android.util.Log.d("MainActivity", "Showing black overlay")
        isBlackOverlayShown = true

        // Stop video immediately
        releasePlayer()

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

        // Hide overlay instantly without animation
        blackOverlay.visibility = View.GONE

        val displayHeight = resources.displayMetrics.heightPixels.toFloat()
        blackOverlay.translationY = -displayHeight

        // Reload video if applicable (don't reload images)
        if (!isSystemScrollActive && currentGameFilename != null && currentSystemName != null) {
            val gameName = currentGameFilename!!.substringBeforeLast('.')
            handleVideoForGame(currentSystemName, gameName, currentGameFilename)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Check if black overlay feature is enabled
        val blackOverlayEnabled = prefs.getBoolean("black_overlay_enabled", false)

        // Check drawer state first
        val drawerState = bottomSheetBehavior.state
        val isDrawerOpen = drawerState == BottomSheetBehavior.STATE_EXPANDED ||
                drawerState == BottomSheetBehavior.STATE_SETTLING

        // Handle black overlay double-tap detection ONLY when drawer is closed and feature is enabled
        if (!isDrawerOpen && blackOverlayEnabled) {
            if (ev.action == MotionEvent.ACTION_DOWN) {
                val currentTime = System.currentTimeMillis()

                // Reset tap count if too much time has passed
                if (currentTime - lastTapTime > DOUBLE_TAP_TIMEOUT) {
                    tapCount = 0
                }

                tapCount++
                lastTapTime = currentTime

                // Check for triple-tap
                if (tapCount >= 2) {
                    tapCount = 0 // Reset counter

                    // Toggle black overlay
                    if (isBlackOverlayShown) {
                        hideBlackOverlay()
                    } else {
                        showBlackOverlay()
                    }
                    return true
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

                // CHANGED: Removed !isSystemScrollActive check - allow long press in system view too
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
                // Cancel long press if finger moves
                val deltaX = kotlin.math.abs(ev.x - touchDownX)
                val deltaY = kotlin.math.abs(ev.y - touchDownY)
                if (deltaX > 10 || deltaY > 10) {
                    longPressRunnable?.let {
                        longPressHandler?.removeCallbacks(it)
                        longPressTriggered = false  // ADDED: Reset flag when movement cancels
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

        val columnCount = prefs.getInt(COLUMN_COUNT_KEY, 4)
        appRecyclerView.layoutManager = GridLayoutManager(this, columnCount)

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val hiddenApps = prefs.getStringSet("hidden_apps", setOf()) ?: setOf()
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
        if (!prefs.getBoolean("setup_completed", false)) return

        // Check how many times hint has been shown (max 3 times total)
        val hintCount = prefs.getInt("settings_hint_count", 0)
        if (hintCount >= 3) return

        // Increment the hint counter
        prefs.edit().putInt("settings_hint_count", hintCount + 1).apply()

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

                val hiddenApps = prefs.getStringSet("hidden_apps", setOf()) ?: setOf()

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
                if (path != null && (path == "esde_game_filename.txt" || path == "esde_system_name.txt" ||
                            path == "esde_gamestart_filename.txt" || path == "esde_gameend_filename.txt" ||
                            path == "esde_screensaver_start.txt" || path == "esde_screensaver_end.txt" ||
                            path == "esde_screensavergameselect_filename.txt")) {
                    // Debounce: ignore events that happen too quickly
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastEventTime < 100) {
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
                                "esde_system_name.txt" -> {
                                    // Ignore if launching from screensaver (game-select event between screensaver-end and game-start)
                                    if (isLaunchingFromScreensaver) {
                                        android.util.Log.d("MainActivity", "Game scroll ignored - launching from screensaver")
                                        return@postDelayed
                                    }

                                    // Ignore if screensaver is active
                                    if (isScreensaverActive) {
                                        android.util.Log.d("MainActivity", "System scroll ignored - screensaver active")
                                        return@postDelayed
                                    }
                                    android.util.Log.d("MainActivity", "System scroll detected")
                                    loadSystemImageDebounced()
                                }
                                "esde_game_filename.txt" -> {
                                    // Ignore if launching from screensaver (game-select event between screensaver-end and game-start)
                                    if (isLaunchingFromScreensaver) {
                                        android.util.Log.d("MainActivity", "Game scroll ignored - launching from screensaver")
                                        return@postDelayed
                                    }

                                    // Ignore if screensaver is active
                                    if (isScreensaverActive) {
                                        android.util.Log.d("MainActivity", "Game scroll ignored - screensaver active")
                                        return@postDelayed
                                    }

                                    // ADDED: Ignore game-select events that happen shortly after game-start or game-end
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastGameStartTime < GAME_EVENT_DEBOUNCE) {
                                        android.util.Log.d("MainActivity", "Game scroll ignored - too soon after game start")
                                        return@postDelayed
                                    }
                                    if (currentTime - lastGameEndTime < GAME_EVENT_DEBOUNCE) {
                                        android.util.Log.d("MainActivity", "Game scroll ignored - too soon after game end")
                                        return@postDelayed
                                    }

                                    // Read the game filename
                                    val gameFile = File(watchDir, "esde_game_filename.txt")
                                    if (gameFile.exists()) {
                                        val gameFilename = gameFile.readText().trim()

                                        // Ignore if this is the same game that's currently playing
                                        if (isGamePlaying && gameFilename == playingGameFilename) {
                                            android.util.Log.d("MainActivity", "Game scroll ignored - same as playing game: $gameFilename")
                                            return@postDelayed
                                        }
                                    }

                                    android.util.Log.d("MainActivity", "Game scroll detected")
                                    loadGameInfoDebounced()
                                }
                                "esde_gamestart_filename.txt" -> {
                                    // Read which game started
                                    val gameStartFile = File(watchDir, "esde_gamestart_filename.txt")
                                    if (gameStartFile.exists()) {
                                        playingGameFilename = gameStartFile.readText().trim()
                                        android.util.Log.d("MainActivity", "Game start detected: $playingGameFilename")
                                    } else {
                                        android.util.Log.d("MainActivity", "Game start detected (filename unknown)")
                                    }

                                    isGamePlaying = true
                                    handleGameStart()
                                }
                                "esde_gameend_filename.txt" -> {
                                    android.util.Log.d("MainActivity", "Game end detected")
                                    isGamePlaying = false
                                    playingGameFilename = null  // Clear playing game
                                    handleGameEnd()
                                }
                                "esde_screensaver_start.txt" -> {
                                    android.util.Log.d("MainActivity", "Screensaver start detected")
                                    isScreensaverActive = true
                                    handleScreensaverStart()
                                }
                                "esde_screensaver_end.txt" -> {
                                    // Read the screensaver end reason
                                    val screensaverEndFile = File(watchDir, "esde_screensaver_end.txt")
                                    val endReason = if (screensaverEndFile.exists()) {
                                        screensaverEndFile.readText().trim()
                                    } else {
                                        "cancel"
                                    }

                                    android.util.Log.d("MainActivity", "Screensaver end detected: $endReason")
                                    isScreensaverActive = false
                                    handleScreensaverEnd(endReason)
                                }
                                "esde_screensavergameselect_filename.txt" -> {
                                    // Only process if screensaver is active
                                    if (!isScreensaverActive) {
                                        return@postDelayed
                                    }

                                    // Read screensaver game info
                                    val filenameFile = File(watchDir, "esde_screensavergameselect_filename.txt")
                                    val nameFile = File(watchDir, "esde_screensavergameselect_name.txt")
                                    val systemFile = File(watchDir, "esde_screensavergameselect_system.txt")

                                    if (filenameFile.exists()) {
                                        screensaverGameFilename = filenameFile.readText().trim()
                                    }
                                    if (nameFile.exists()) {
                                        screensaverGameName = nameFile.readText().trim()
                                    }
                                    if (systemFile.exists()) {
                                        screensaverSystemName = systemFile.readText().trim()
                                    }

                                    android.util.Log.d("MainActivity", "Screensaver game: $screensaverGameName ($screensaverGameFilename) - $screensaverSystemName")
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
        videoDelayHandler = null
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hasWindowFocus = hasFocus

        if (hasFocus) {
            android.util.Log.d("MainActivity", "Window focus gained - app is on top")
        } else {
            android.util.Log.d("MainActivity", "Window focus lost - something is on top of app")
            // Stop videos when we lose focus (game launched on same screen)
            releasePlayer()
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
                val extensions = listOf("svg", "png", "jpg", "jpeg", "webp")

                for (ext in extensions) {
                    val logoFile = File(userLogosDir, "$baseFileName.$ext")
                    if (logoFile.exists()) {
                        android.util.Log.d("MainActivity", "Loading logo from user path: $logoFile")

                        return when (ext) {
                            "svg" -> {
                                // Load SVG with specific dimensions if provided
                                val svg = com.caverock.androidsvg.SVG.getFromInputStream(logoFile.inputStream())
                                if (width > 0 && height > 0) {
                                    svg.documentWidth = width.toFloat()
                                    svg.documentHeight = height.toFloat()
                                }
                                android.graphics.drawable.PictureDrawable(svg.renderToPicture())
                            }
                            else -> {
                                // Load bitmap formats (PNG, JPG, WebP) with downscaling
                                val bitmap = loadScaledBitmap(logoFile.absolutePath, 800, 1000)
                                android.graphics.drawable.BitmapDrawable(resources, bitmap)
                            }
                        }
                    }
                }
            }

            // Fall back to built-in SVG assets
            val svgPath = "system_logos/$baseFileName.svg"
            val svg = com.caverock.androidsvg.SVG.getFromAsset(assets, svgPath)

            // Set dimensions if provided
            if (width > 0 && height > 0) {
                svg.documentWidth = width.toFloat()
                svg.documentHeight = height.toFloat()
            }

            android.graphics.drawable.PictureDrawable(svg.renderToPicture())
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
            file.lastModified().toString()
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
        if (isGamePlaying) {
            android.util.Log.d("MainActivity", "loadSystemImage blocked - game is playing, maintaining game launch display")
            return
        }

        try {
            // Stop any video playback when switching to system view
            releasePlayer()

            val logsDir = File(getLogsPath())
            val systemFile = File(logsDir, "esde_system_name.txt")
            if (!systemFile.exists()) return

            val systemName = systemFile.readText().trim()

            // Store current system name for later reference
            currentSystemName = systemName
            currentGameName = null  // Clear game info when in system view
            currentGameFilename = null

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
            val imageExtensions = listOf("webp", "png", "jpg", "jpeg")

            for (ext in imageExtensions) {
                val imageFile = File(systemImagePath, "$baseFileName.$ext")
                if (imageFile.exists()) {
                    imageToUse = imageFile
                    break
                }
            }

            if (imageToUse == null) {
                val mediaBase = File(getMediaBasePath(), systemName)
                val imagePref = prefs.getString("image_preference", "fanart") ?: "fanart"
                val prioritizedFolders = if (imagePref == "screenshot") {
                    listOf("screenshots", "fanart")
                } else {
                    listOf("fanart", "screenshots")
                }
                for (folder in prioritizedFolders) {
                    val dir = File(mediaBase, folder)
                    if (dir.exists() && dir.isDirectory) {
                        val images = dir.listFiles { f ->
                            f.extension.lowercase() in listOf("jpg", "png", "webp")
                        } ?: emptyArray()
                        if (images.isNotEmpty()) {
                            imageToUse = images.random()
                            break
                        }
                    }
                }
            }

            if (imageToUse != null && imageToUse.exists()) {
                isSystemScrollActive = true

                // Check if this is a custom system image (from system_images folder)
                val isCustomSystemImage = imageToUse.absolutePath.contains(getSystemImagePath())

                if (isCustomSystemImage) {
                    // Load custom system image with downscaling to prevent OOM
                    android.util.Log.d("MainActivity", "Loading custom system image with downscaling")
                    val bitmap = loadScaledBitmap(imageToUse.absolutePath, 1920, 1080)
                    if (bitmap != null) {
                        val drawable = android.graphics.drawable.BitmapDrawable(resources, bitmap)

                        // Clear any cached images first
                        Glide.with(this).clear(gameImageView)

                        gameImageView.setImageDrawable(drawable)
                        android.util.Log.d("MainActivity", "Custom system image loaded successfully")
                    } else {
                        android.util.Log.e("MainActivity", "Failed to load custom system image, using fallback")
                        loadFallbackBackground()
                    }
                } else {
                    // Normal game artwork - use Glide with animation
                    loadImageWithAnimation(imageToUse, gameImageView)
                }
            } else {
                // No custom image and no game images found - show fallback
                isSystemScrollActive = true
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
        if (isGamePlaying) {
            android.util.Log.d("MainActivity", "loadGameInfo blocked - game is playing, maintaining game launch display")
            return
        }

        isSystemScrollActive = false

        try {
            val logsDir = File(getLogsPath())
            val gameFile = File(logsDir, "esde_game_filename.txt")
            if (!gameFile.exists()) return

            val gameNameRaw = gameFile.readText().trim()  // Full path from script
            val gameName = sanitizeGameFilename(gameNameRaw).substringBeforeLast('.')  // FIXED: Sanitize first, then strip extension

            // Read the display name from ES-DE if available
            val gameDisplayNameFile = File(logsDir, "esde_game_name.txt")
            val gameDisplayName = if (gameDisplayNameFile.exists()) {
                gameDisplayNameFile.readText().trim()
            } else {
                gameName  // Fallback to filename-based name
            }

            // Store current game info for later reference
            currentGameName = gameDisplayName
            currentGameFilename = gameNameRaw

            val systemFile = File(logsDir, "esde_game_system.txt")
            if (!systemFile.exists()) return
            val systemName = systemFile.readText().trim()

            // Store current system name
            currentSystemName = systemName

            // Check if we have widgets - if so, hide old marquee system
            val hasWidgets = widgetManager.loadWidgets().isNotEmpty()

            // Try to find game-specific artwork
            val gameImage = findGameImage(systemName, gameName, gameNameRaw)

            if (gameImage != null && gameImage.exists()) {
                // Game has its own artwork - use it
                loadImageWithAnimation(gameImage, gameImageView)
            } else {
                // No game artwork - show fallback background
                loadFallbackBackground()
            }

            // Handle video playback for the current game
            // Pass both stripped name and raw filename (like images do)
            android.util.Log.d("MainActivity", "loadGameInfo - Calling handleVideoForGame:")
            android.util.Log.d("MainActivity", "  systemName: $systemName")
            android.util.Log.d("MainActivity", "  gameName (stripped): $gameName")
            android.util.Log.d("MainActivity", "  gameNameRaw (full path): $gameNameRaw")
            handleVideoForGame(systemName, gameName, gameNameRaw)

            // Update game widgets after loading game image
            updateWidgetsForCurrentGame()
            // Don't show widgets if screensaver is active
            if (!isScreensaverActive) {
                showWidgets()
            } else {
                android.util.Log.d("MainActivity", "Screensaver active - not showing widgets from loadGameInfo")
            }

        } catch (e: Exception) {
            // Don't clear images on exception - keep last valid images
            android.util.Log.e("MainActivity", "Error loading game info", e)
        }
    }

    private fun findGameImage(systemName: String, gameName: String, fullGamePath: String): File? {
        val extensions = listOf("jpg", "png", "webp")
        val mediaBase = File(getMediaBasePath(), systemName)
        val imagePref = prefs.getString("image_preference", "fanart") ?: "fanart"
        val dirs = if (imagePref == "screenshot") {
            listOf("screenshots", "fanart")
        } else {
            listOf("fanart", "screenshots")
        }

        // Sanitize the full path to get just the filename
        val sanitizedFilename = sanitizeGameFilename(fullGamePath)
        val sanitizedName = sanitizedFilename.substringBeforeLast('.')

        for (dirName in dirs) {
            val file = findImageInDir(
                File(mediaBase, dirName),
                sanitizedName,
                sanitizedFilename,
                fullGamePath,
                extensions
            )
            if (file != null) return file
        }
        return null
    }

    private fun findImageInDir(
        dir: File,
        strippedName: String,
        rawName: String,
        fullPath: String,
        extensions: List<String>
    ): File? {
        if (!dir.exists() || !dir.isDirectory) return null

        // Extract subfolder path if present (everything before the filename)
        val subfolder = fullPath.substringBeforeLast("/", "").substringAfterLast("/", "")

        // Try in order of specificity:
        // 1. Exact subfolder match: <media_type>/<subfolder>/filename.ext
        // 2. Root level with stripped name: <media_type>/filename.ext
        // 3. Root level with raw name: <media_type>/filename.ext

        // 1. Try subfolder match if subfolder exists
        if (subfolder.isNotEmpty()) {
            val subfolderDir = File(dir, subfolder)
            if (subfolderDir.exists() && subfolderDir.isDirectory) {
                for (name in listOf(strippedName, rawName)) {
                    for (ext in extensions) {
                        val file = File(subfolderDir, "$name.$ext")
                        if (file.exists()) {
                            android.util.Log.d("MainActivity", "Found media in subfolder: ${file.absolutePath}")
                            return file
                        }
                    }
                }
            }
        }

        // 2 & 3. Try root level (original behavior)
        for (name in listOf(strippedName, rawName)) {
            for (ext in extensions) {
                val file = File(dir, "$name.$ext")
                if (file.exists()) {
                    android.util.Log.d("MainActivity", "Found media in root: ${file.absolutePath}")
                    return file
                }
            }
        }

        return null
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
        val hiddenApps = prefs.getStringSet("hidden_apps", setOf())?.toMutableSet() ?: mutableSetOf()
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
        if (currentPosition == AppLaunchPreferences.POSITION_TOP) {
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
            val currentHiddenApps = prefs.getStringSet("hidden_apps", setOf())?.toMutableSet() ?: mutableSetOf()
            val currentlyHidden = currentHiddenApps.contains(packageName)

            if (currentlyHidden) {
                // Unhide - no confirmation
                currentHiddenApps.remove(packageName)
                prefs.edit().putStringSet("hidden_apps", currentHiddenApps).apply()
                dialog.dismiss()

                val mainIntent = Intent(Intent.ACTION_MAIN, null)
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                val updatedHiddenApps = prefs.getStringSet("hidden_apps", setOf()) ?: setOf()
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
                        prefs.edit().putStringSet("hidden_apps", currentHiddenApps).apply()
                        dialog.dismiss()

                        val mainIntent = Intent(Intent.ACTION_MAIN, null)
                        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                        val updatedHiddenApps = prefs.getStringSet("hidden_apps", setOf()) ?: setOf()
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
                    appLaunchPrefs.setLaunchPosition(packageName, AppLaunchPreferences.POSITION_TOP)
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
                    appLaunchPrefs.setLaunchPosition(packageName, AppLaunchPreferences.POSITION_BOTTOM)
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

        // Get the game launch behavior ONCE at the top
        val gameLaunchBehavior = prefs.getString("game_launch_behavior", "game_image") ?: "game_image"
        android.util.Log.d("MainActivity", "Game launch behavior: $gameLaunchBehavior")

        // CRITICAL: If black screen, clear everything IMMEDIATELY before anything else
        if (gameLaunchBehavior == "black_screen") {
            android.util.Log.d("MainActivity", "Black screen behavior - clearing display immediately")
            Glide.with(this).clear(gameImageView)
            gameImageView.setImageDrawable(null)
            gameImageView.visibility = View.GONE
            videoView.visibility = View.GONE
            hideWidgets()
            releasePlayer()
        }

        // Set playing state
        isGamePlaying = true

        // Check if we came from screensaver with the same game
        val cameFromScreensaver = screensaverGameFilename != null

        if (cameFromScreensaver) {
            android.util.Log.d("MainActivity", "Game start from screensaver - same game")

            val screensaverBehavior = prefs.getString("screensaver_behavior", "game_image") ?: "game_image"

            // Only update display if NOT black (already handled at top)
            if (gameLaunchBehavior != "black_screen") {
                if (screensaverBehavior == gameLaunchBehavior) {
                    // Behaviors match - just ensure correct visibility, don't reload
                    android.util.Log.d("MainActivity", "Behaviors match ($screensaverBehavior) - keeping current display")
                    gameImageView.visibility = View.VISIBLE
                    videoView.visibility = View.GONE

                    // Keep widgets visible if they exist
                    val hasWidgets = widgetManager.loadWidgets().isNotEmpty()
                    if (hasWidgets) {
                        widgetContainer.visibility = View.VISIBLE
                        android.util.Log.d("MainActivity", "Keeping widgets visible")
                    }
                } else {
                    // Behaviors differ - update display based on game launch behavior
                    android.util.Log.d("MainActivity", "Behaviors differ (screensaver: $screensaverBehavior, launch: $gameLaunchBehavior) - updating display")

                    when (gameLaunchBehavior) {
                        "game_image" -> {
                            // Load the game's artwork
                            val filename = playingGameFilename ?: screensaverGameFilename
                            val systemName = screensaverSystemName ?: currentSystemName

                            if (filename != null && systemName != null) {
                                val gameName = sanitizeGameFilename(filename).substringBeforeLast('.')
                                val gameImage = findGameImage(systemName, gameName, filename)

                                if (gameImage != null && gameImage.exists()) {
                                    loadImageWithAnimation(gameImage, gameImageView)
                                } else {
                                    loadFallbackBackground()
                                }
                            }

                            gameImageView.visibility = View.VISIBLE
                            videoView.visibility = View.GONE

                            val hasWidgets = widgetManager.loadWidgets().isNotEmpty()
                            if (hasWidgets) {
                                widgetContainer.visibility = View.VISIBLE
                            }
                        }
                        "default_image" -> {
                            loadFallbackBackground()
                            gameImageView.visibility = View.VISIBLE
                            videoView.visibility = View.GONE

                            // Load and show widgets
                            updateWidgetsForCurrentGame()
                            showWidgets()
                        }
                    }
                }
            }
        } else {
            // Normal game launch (not from screensaver)
            android.util.Log.d("MainActivity", "Game start - normal launch")

// Check what was showing before game started
            val wasInGameView = !isSystemScrollActive && currentGameFilename != null

// Only update display if NOT black (already handled at top)
            if (gameLaunchBehavior != "black_screen") {
                // If we were already in game view with game_image behavior, keep the display
                if (wasInGameView && gameLaunchBehavior == "game_image") {
                    android.util.Log.d("MainActivity", "Already showing game image - keeping current display and reloading widgets")

                    // Keep the display but reload widgets since they may have changed
                    // This ensures widgets show up even when launching the same game again
                    updateWidgetsForCurrentGame()
                    showWidgets()

                    android.util.Log.d("MainActivity", "Widgets reloaded for same game launch")
                } else {
                    // Need to update display
                    android.util.Log.d("MainActivity", "Updating display for game launch")

                    when (gameLaunchBehavior) {
                        "game_image" -> {
                            val filename = playingGameFilename
                            val systemName = currentSystemName

                            if (filename != null && systemName != null) {
                                val gameName = sanitizeGameFilename(filename).substringBeforeLast('.')
                                val gameImage = findGameImage(systemName, gameName, filename)

                                if (gameImage != null && gameImage.exists()) {
                                    loadImageWithAnimation(gameImage, gameImageView)
                                } else {
                                    loadFallbackBackground()
                                }
                            }

                            gameImageView.visibility = View.VISIBLE
                            videoView.visibility = View.GONE

                            val hasWidgets = widgetManager.loadWidgets().isNotEmpty()
                            if (hasWidgets) {
                                widgetContainer.visibility = View.VISIBLE
                            }
                        }
                        "default_image" -> {
                            loadFallbackBackground()
                            gameImageView.visibility = View.VISIBLE
                            videoView.visibility = View.GONE

                            // Load and show widgets
                            updateWidgetsForCurrentGame()
                            showWidgets()
                        }
                    }
                }
            }
        }

        // Stop any videos
        releasePlayer()

        // Update browsing state to the game that's now playing
        if (playingGameFilename != null) {
            currentGameFilename = playingGameFilename
            currentGameName = null
            isSystemScrollActive = false
        }
        if (screensaverSystemName != null) {
            currentSystemName = screensaverSystemName
        }

        // Clear screensaver launch flag and variables
        isLaunchingFromScreensaver = false
        screensaverGameFilename = null
        screensaverGameName = null
        screensaverSystemName = null

        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /**
     * Handle game end event - return to normal browsing display
     */
    private fun handleGameEnd() {
        lastGameEndTime = System.currentTimeMillis()

        android.util.Log.d("MainActivity", "gameImageView.visibility at game end: ${gameImageView.visibility}")
        android.util.Log.d("MainActivity", "Game end detected")
        isGamePlaying = false

        // Check what behavior we should use to return
        val gameLaunchBehavior = prefs.getString("game_launch_behavior", "game_image") ?: "game_image"

        // If we're returning to the same game with game_image behavior, don't reload
        if (!isSystemScrollActive && currentGameFilename != null && gameLaunchBehavior == "game_image") {
            android.util.Log.d("MainActivity", "Returning to game view - keeping current display and widgets")
            android.util.Log.d("MainActivity", "Nothing to update - display already correct")

            // Don't touch ANYTHING - everything is already correct
            // This prevents any layout passes that could cause flashing
        } else {
            // Need to reload - different view or behavior
            android.util.Log.d("MainActivity", "Reloading display after game end")

            if (isSystemScrollActive) {
                loadSystemImage()
            } else {
                loadGameInfo()
            }
        }
    }

    // ========== SCREENSAVER FUNCTIONS ==========

    /**
     * Handle screensaver start event
     */
    private fun handleScreensaverStart() {
        android.util.Log.d("MainActivity", "Screensaver start")

        isScreensaverActive = true
        screensaverInitialized = false

        // Save complete state before screensaver
        wasInSystemViewBeforeScreensaver = isSystemScrollActive
        systemBeforeScreensaver = currentSystemName
        gameFilenameBeforeScreensaver = currentGameFilename
        gameNameBeforeScreensaver = currentGameName
        android.util.Log.d("MainActivity", "Saved pre-screensaver state: system=$systemBeforeScreensaver, game=$gameNameBeforeScreensaver, isSystemView=$wasInSystemViewBeforeScreensaver")

        val screensaverBehavior = prefs.getString("screensaver_behavior", "default_image") ?: "default_image"
        android.util.Log.d("MainActivity", "Screensaver behavior: $screensaverBehavior")

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
     * Handle screensaver end event - return to normal browsing display
     * @param reason The reason for screensaver ending: "cancel", "game-jump", or "game-start"
     */
    private fun handleScreensaverEnd(reason: String?) {
        android.util.Log.d("MainActivity", "Screensaver end detected: $reason")
        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        android.util.Log.d("MainActivity", "SCREENSAVER END: reason=$reason")
        android.util.Log.d("MainActivity", "  screensaverGameFilename: $screensaverGameFilename")
        android.util.Log.d("MainActivity", "  screensaverGameName: $screensaverGameName")
        android.util.Log.d("MainActivity", "  screensaverSystemName: $screensaverSystemName")
        android.util.Log.d("MainActivity", "  currentGameFilename: $currentGameFilename")
        android.util.Log.d("MainActivity", "  currentSystemName: $currentSystemName")
        android.util.Log.d("MainActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        isScreensaverActive = false
        screensaverInitialized = false

        if (reason != null) {
            when (reason) {
                "game-start" -> {
                    // User is launching a game from screensaver
                    android.util.Log.d("MainActivity", "Screensaver end - game starting, waiting for game-start event")
                    isLaunchingFromScreensaver = true

                    // CRITICAL FIX: Update current game context to screensaver game BEFORE game starts
                    // This ensures widgets load images from the correct system folder
                    if (screensaverSystemName != null) {
                        currentSystemName = screensaverSystemName
                        android.util.Log.d("MainActivity", "Updated currentSystemName to screensaver system: $currentSystemName")
                    }
                    if (screensaverGameFilename != null) {
                        currentGameFilename = screensaverGameFilename
                        android.util.Log.d("MainActivity", "Updated currentGameFilename to screensaver game: $currentGameFilename")
                    }

                    // Don't clear screensaver variables yet - handleGameStart needs them
                    // The game-start event will handle the display
                }
                "game-jump" -> {
                    // User jumped to a different game while in screensaver
                    // The game is now the selected game, so image can be retained
                    android.util.Log.d("MainActivity", "Screensaver end - game-jump, retaining current image")

                    // Update current game context to screensaver game
                    if (screensaverSystemName != null) {
                        currentSystemName = screensaverSystemName
                    }
                    if (screensaverGameFilename != null) {
                        currentGameFilename = screensaverGameFilename
                    }
                    if (screensaverGameName != null) {
                        currentGameName = screensaverGameName
                    }

                    // Clear screensaver variables since we're done with screensaver
                    screensaverGameFilename = null
                    screensaverGameName = null
                    screensaverSystemName = null

                    // The current screensaver game image is already showing, so don't reload
                }
                "cancel" -> {
                    // User cancelled screensaver (pressed back or timeout)
                    // Return to the browsing state from before screensaver started
                    android.util.Log.d("MainActivity", "Screensaver end - cancel, returning to browsing state")

                    // Clear screensaver variables
                    screensaverGameFilename = null
                    screensaverGameName = null
                    screensaverSystemName = null

                    // CHANGED: Check what view we were in BEFORE screensaver started
                    if (wasInSystemViewBeforeScreensaver) {
                        // Was in system view - reload system image and widgets
                        android.util.Log.d("MainActivity", "Returning to system view")
                        isSystemScrollActive = true  // Restore system view state
                        loadSystemImage()
                    } else {
                        // Was in game view - reload game info and widgets
                        android.util.Log.d("MainActivity", "Returning to game view")
                        loadGameInfo()
                    }
                }
                else -> {
                    // Unknown reason - default to cancel behavior
                    android.util.Log.w("MainActivity", "Screensaver end - unknown reason: $reason, defaulting to cancel behavior")

                    // Clear screensaver variables
                    screensaverGameFilename = null
                    screensaverGameName = null
                    screensaverSystemName = null

                    // CHANGED: Check what view we were in BEFORE screensaver started
                    if (wasInSystemViewBeforeScreensaver) {
                        // Was in system view - reload system image and widgets
                        android.util.Log.d("MainActivity", "Returning to system view")
                        isSystemScrollActive = true  // Restore system view state
                        loadSystemImage()
                    } else {
                        // Was in game view - reload game info and widgets
                        android.util.Log.d("MainActivity", "Returning to game view")
                        loadGameInfo()
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
        val screensaverBehavior = prefs.getString("screensaver_behavior", "default_image") ?: "default_image"

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

        if (screensaverGameFilename != null && screensaverSystemName != null) {
            val gameName = screensaverGameFilename!!.substringBeforeLast('.')

            when (screensaverBehavior) {
                "game_image" -> {
                    // Load the screensaver game's artwork
                    val gameImage = findGameImage(
                        screensaverSystemName!!,
                        gameName,
                        screensaverGameFilename!!
                    )

                    if (gameImage != null && gameImage.exists()) {
                        loadImageWithAnimation(gameImage, gameImageView)
                    } else {
                        loadFallbackBackground()
                    }

                    // NEW: Make sure views are visible
                    gameImageView.visibility = View.VISIBLE
                    videoView.visibility = View.GONE

                    // CRITICAL: Set current game context BEFORE loading widgets
                    // This ensures widgets load images from the correct system folder
                    currentSystemName = screensaverSystemName
                    currentGameFilename = screensaverGameFilename
                    currentGameName = gameName
                    isSystemScrollActive = false  // CRITICAL: Set to false so updateWidgetsForCurrentGame() loads game widgets
                    android.util.Log.d("MainActivity", "Set game context for widgets: system=$currentSystemName, game=$currentGameName")

                    // Use existing function to load game widgets with correct images
                    updateWidgetsForCurrentGame()
                }
                "default_image" -> {
                    loadFallbackBackground()

                    // NEW: Make sure views are visible
                    gameImageView.visibility = View.VISIBLE
                    videoView.visibility = View.GONE

                    // DEBUG: Log visibility state
                    android.util.Log.d("MainActivity", "After setting visibility: gameImageView=${gameImageView.visibility}, videoView=${videoView.visibility}")
                    android.util.Log.d("MainActivity", "gameImageView drawable: ${gameImageView.drawable}")

                    // CRITICAL: Set current game context BEFORE loading widgets
                    // This ensures widgets load images from the correct system folder
                    currentSystemName = screensaverSystemName
                    currentGameFilename = screensaverGameFilename
                    currentGameName = gameName
                    isSystemScrollActive = false  // CRITICAL: Set to false so updateWidgetsForCurrentGame() loads game widgets
                    android.util.Log.d("MainActivity", "Set game context for widgets: system=$currentSystemName, game=$currentGameName")

                    // Use existing function to load game widgets with correct images
                    updateWidgetsForCurrentGame()
                }
            }
        }
    }

    private fun updateWidgetsForScreensaverGame() {
        android.util.Log.d("MainActivity", "═══ updateWidgetsForScreensaverGame START ═══")

        // Clear existing widgets
        widgetContainer.removeAllViews()
        activeWidgets.clear()

        val systemName = screensaverSystemName
        val gameFilename = screensaverGameFilename

        if (systemName != null && gameFilename != null) {
            // Load saved widgets and update with screensaver game images
            val allWidgets = widgetManager.loadWidgets()
            android.util.Log.d("MainActivity", "Loaded ${allWidgets.size} widgets for screensaver")

            // Filter for GAME context widgets only - ADDED THIS
            val gameWidgets = allWidgets.filter { it.widgetContext == OverlayWidget.WidgetContext.GAME }
            android.util.Log.d("MainActivity", "Loaded ${gameWidgets.size} game widgets for screensaver")

            // Sort widgets by z-index before processing
            val sortedWidgets = allWidgets.sortedBy { it.zIndex }
            android.util.Log.d("MainActivity", "Sorted ${sortedWidgets.size} widgets by z-index")

            sortedWidgets.forEachIndexed { index, widget ->
                android.util.Log.d("MainActivity", "Processing screensaver widget $index: type=${widget.imageType}, zIndex=${widget.zIndex}")

                val gameName = sanitizeGameFilename(gameFilename).substringBeforeLast('.')
                val imageFile = when (widget.imageType) {
                    OverlayWidget.ImageType.MARQUEE ->
                        findImageInFolder(systemName, gameName, gameFilename, "marquees")
                    OverlayWidget.ImageType.BOX_2D ->
                        findImageInFolder(systemName, gameName, gameFilename, "covers")
                    OverlayWidget.ImageType.BOX_3D ->
                        findImageInFolder(systemName, gameName, gameFilename, "3dboxes")
                    OverlayWidget.ImageType.MIX_IMAGE ->
                        findImageInFolder(systemName, gameName, gameFilename, "miximages")
                    OverlayWidget.ImageType.BACK_COVER ->
                        findImageInFolder(systemName, gameName, gameFilename, "backcovers")
                    OverlayWidget.ImageType.PHYSICAL_MEDIA ->
                        findImageInFolder(systemName, gameName, gameFilename, "physicalmedia")
                    OverlayWidget.ImageType.SCREENSHOT ->
                        findImageInFolder(systemName, gameName, gameFilename, "screenshots")
                    OverlayWidget.ImageType.FANART ->
                        findImageInFolder(systemName, gameName, gameFilename, "fanart")
                    OverlayWidget.ImageType.TITLE_SCREEN ->
                        findImageInFolder(systemName, gameName, gameFilename, "titlescreens")
                    OverlayWidget.ImageType.GAME_DESCRIPTION -> null  // NEW: Text widget, handled separately
                    OverlayWidget.ImageType.SYSTEM_LOGO -> null
                }

                // ALWAYS create the widget, even if image doesn't exist
                val widgetToAdd = when {
                    // NEW: Handle description text widget for screensaver
                    widget.imageType == OverlayWidget.ImageType.GAME_DESCRIPTION -> {
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
                        if (widget.imageType == OverlayWidget.ImageType.MARQUEE) {
                            widget.copy(
                                imagePath = "",
                                id = "widget_${gameName}"
                            )
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
        return prefs.getBoolean("video_enabled", false)
    }

    /**
     * Update video volume based on system volume for the current display
     * This respects per-display volume controls on devices like Ayn Thor
     *
     * Ayn Thor uses:
     * - Standard STREAM_MUSIC volume for top screen (display 0)
     * - Settings.System "secondary_screen_volume_level" for bottom screen (display 1)
     */
    private fun updateVideoVolume() {
        if (player == null) return

        val audioEnabled = prefs.getBoolean("video_audio_enabled", false)

        if (!audioEnabled) {
            // User has disabled video audio - mute completely
            player?.volume = 0f
            android.util.Log.d("MainActivity", "Video audio disabled by user - volume: 0")
            return
        }

        try {
            // Get the audio manager
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

            // Determine which display we're on
            val currentDisplayId = getCurrentDisplayId()

            var normalizedVolume: Float

            if (currentDisplayId == 0) {
                // Primary display (top screen) - use standard STREAM_MUSIC volume
                val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)

                normalizedVolume = if (maxVolume > 0) {
                    currentVolume.toFloat() / maxVolume.toFloat()
                } else {
                    1f
                }

                android.util.Log.d("MainActivity", "Top screen - Using STREAM_MUSIC: $currentVolume/$maxVolume = $normalizedVolume")

            } else {
                // Secondary display (bottom screen) - use secondary_screen_volume_level
                try {
                    val secondaryVolume = Settings.System.getInt(
                        contentResolver,
                        "secondary_screen_volume_level"
                    )
                    val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)

                    normalizedVolume = if (maxVolume > 0) {
                        secondaryVolume.toFloat() / maxVolume.toFloat()
                    } else {
                        1f
                    }

                    android.util.Log.d("MainActivity", "Bottom screen - Using secondary_screen_volume_level: $secondaryVolume/$maxVolume = $normalizedVolume")

                } catch (e: Settings.SettingNotFoundException) {
                    // Setting not found - fallback to standard volume
                    android.util.Log.w("MainActivity", "secondary_screen_volume_level not found, using STREAM_MUSIC")

                    val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                    val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)

                    normalizedVolume = if (maxVolume > 0) {
                        currentVolume.toFloat() / maxVolume.toFloat()
                    } else {
                        1f
                    }
                }
            }

            // Apply the calculated volume to the video player
            player?.volume = normalizedVolume

            android.util.Log.d("MainActivity", "Video volume updated: $normalizedVolume (display: $currentDisplayId)")

        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error updating video volume", e)
            // Fallback to full volume if there's an error
            player?.volume = 1f
        }
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
        return player != null && currentVideoPath != null
    }

    /**
     * Get video delay in milliseconds
     */
    private fun getVideoDelay(): Long {
        val progress = prefs.getInt("video_delay", 4) // 4 (2 seconds)
        return (progress * 500L) // Convert to milliseconds (0-5000ms)
    }

    /**
     * Find video file for a game
     * @param systemName ES-DE system name (e.g., "snes", "arcade")
     * @param strippedName Game filename without extension (e.g., "Super Mario World")
     * @param rawName Game filename with extension (e.g., "Super Mario World.zip")
     * @return Video file path or null if not found
     */
    private fun findVideoForGame(systemName: String?, strippedName: String?, rawName: String?): String? {
        if (systemName == null || strippedName == null) {
            android.util.Log.d("MainActivity", "findVideoForGame - systemName or strippedName is null")
            return null
        }

        val mediaPath = prefs.getString("media_path", "/storage/emulated/0/ES-DE/downloaded_media")
            ?: return null

        android.util.Log.d("MainActivity", "findVideoForGame - Looking for video:")
        android.util.Log.d("MainActivity", "  systemName: $systemName")
        android.util.Log.d("MainActivity", "  strippedName: $strippedName")
        android.util.Log.d("MainActivity", "  rawName: $rawName")

        val videoExtensions = listOf("mp4", "mkv", "avi", "wmv", "mov", "webm")
        val videoDir = File(mediaPath, "$systemName/videos")

        if (!videoDir.exists()) {
            android.util.Log.d("MainActivity", "Video directory does not exist: ${videoDir.absolutePath}")
            return null
        }

        // Sanitize the raw name (which is now the full path)
        val sanitizedRawName = if (rawName != null) sanitizeGameFilename(rawName) else null

        android.util.Log.d("MainActivity", "  sanitizedRawName: $sanitizedRawName")

        // Extract subfolder if present
        val subfolder = rawName?.substringBeforeLast("/", "")?.substringAfterLast("/", "") ?: ""

        android.util.Log.d("MainActivity", "  subfolder: $subfolder")

        // Try subfolder first if it exists
        if (subfolder.isNotEmpty()) {
            val subfolderDir = File(videoDir, subfolder)
            android.util.Log.d("MainActivity", "  Checking subfolder: ${subfolderDir.absolutePath}")
            if (subfolderDir.exists() && subfolderDir.isDirectory) {
                for (name in listOfNotNull(strippedName, sanitizedRawName)) {
                    for (ext in videoExtensions) {
                        val videoFile = File(subfolderDir, "$name.$ext")
                        android.util.Log.d("MainActivity", "    Trying: ${videoFile.absolutePath}")
                        if (videoFile.exists()) {
                            android.util.Log.d("MainActivity", "Found video in subfolder: ${videoFile.absolutePath}")
                            return videoFile.absolutePath
                        }
                    }
                }
            }
        }

        // Try root level
        android.util.Log.d("MainActivity", "  Checking root level: ${videoDir.absolutePath}")
        for (name in listOfNotNull(strippedName, sanitizedRawName)) {
            for (ext in videoExtensions) {
                val videoFile = File(videoDir, "$name.$ext")
                android.util.Log.d("MainActivity", "    Trying: ${videoFile.absolutePath}")
                if (videoFile.exists()) {
                    android.util.Log.d("MainActivity", "Found video in root: ${videoFile.absolutePath}")
                    return videoFile.absolutePath
                }
            }
        }

        android.util.Log.d("MainActivity", "No video found for system: $systemName, game: $strippedName")
        return null
    }

    /**
     * Load and play video with animation based on settings
     */
    private fun loadVideo(videoPath: String) {
        try {
            // If same video is already playing, don't reload
            if (currentVideoPath == videoPath && player != null) {
                android.util.Log.d("MainActivity", "Same video already playing: $videoPath")
                return
            }

            // Stop previous player without animation
            player?.release()
            player = null

            // Create new player
            player = ExoPlayer.Builder(this).build()
            videoView.player = player

            // Set volume based on system volume
            updateVideoVolume()

            // Create media item
            val mediaItem = MediaItem.fromUri(videoPath)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true
            player?.repeatMode = Player.REPEAT_MODE_ONE // Loop video

            // Hide the game image view and marquee so video is visible
            gameImageView.visibility = View.GONE

            // Hide widgets when video plays  // ADDED
            hideWidgets()  // ADDED

            // Get animation settings (same as images)
            val animationStyle = prefs.getString("animation_style", "scale_fade") ?: "scale_fade"
            val duration = if (animationStyle == "custom") {
                prefs.getInt("animation_duration", 250)
            } else {
                250
            }
            val scaleAmount = if (animationStyle == "custom") {
                prefs.getInt("animation_scale", 95) / 100f
            } else {
                0.95f
            }

            // Show video view with animation
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

            currentVideoPath = videoPath
            android.util.Log.d("MainActivity", "Video loaded with ${animationStyle} animation (${duration}ms, scale: ${scaleAmount})")

        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error loading video: $videoPath", e)
            releasePlayer()
        }
    }

    /**
     * Release video player
     */
    private fun releasePlayer() {
        // Cancel any pending video load
        videoDelayRunnable?.let { videoDelayHandler?.removeCallbacks(it) }

        if (player != null) {
            // Cancel any ongoing animations
            videoView.animate().cancel()

            // Fade out video and release
            videoView.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    videoView.visibility = View.GONE
                    player?.release()
                    player = null
                    currentVideoPath = null

                    // Show the game image view again
                    gameImageView.visibility = View.VISIBLE

                    // Show widgets when game is playing (FIXED)
                    if (isGamePlaying) {  // Changed from !isGamePlaying
                        val hasWidgets = widgetManager.loadWidgets().isNotEmpty()
                        if (hasWidgets) {
                            showWidgets()
                        }
                    }
                }
                .start()
        } else {
            // No player, just hide the video view and show image view
            videoView.visibility = View.GONE
            gameImageView.visibility = View.VISIBLE
            currentVideoPath = null

            // Show widgets when game is playing (FIXED)
            if (isGamePlaying) {  // Changed from !isGamePlaying
                val hasWidgets = widgetManager.loadWidgets().isNotEmpty()
                if (hasWidgets) {
                    showWidgets()
                }
            }
        }
    }

    /**
     * Handle video loading with delay
     */
    private fun handleVideoForGame(systemName: String?, strippedName: String?, rawName: String?) {
        // Cancel any pending video load
        videoDelayRunnable?.let { videoDelayHandler?.removeCallbacks(it) }

        // Hybrid blocking approach to handle different device scenarios:
        //
        // Scenario 1: Game launches on SAME screen as Companion
        //   - hasWindowFocus becomes false (game window is on top)
        //   - onPause() does NOT fire (Companion is still "active" behind game)
        //   - Block: !hasWindowFocus
        //
        // Scenario 2: Game launches on OTHER screen
        //   - hasWindowFocus may be unreliable (identical display names)
        //   - onPause() DOES fire (Companion is backgrounded by system)
        //   - Block: lifecycle state check
        //
        // We block videos if EITHER condition indicates we shouldn't play:

        val isInBackground = lifecycle.currentState != androidx.lifecycle.Lifecycle.State.RESUMED

        if (!hasWindowFocus || isInBackground) {
            android.util.Log.d("MainActivity", "Video blocked - hasWindowFocus: $hasWindowFocus, isInBackground: $isInBackground")
            releasePlayer()
            return
        }

        if (isScreensaverActive) {
            android.util.Log.d("MainActivity", "Video blocked - screensaver active")
            releasePlayer()
            return
        }

        if (!isVideoEnabled()) {
            releasePlayer()
            return
        }

        // Pass the raw name (full path) to findVideoForGame
        val videoPath = findVideoForGame(systemName, strippedName, rawName)

        if (videoPath != null) {
            val delay = getVideoDelay()

            android.util.Log.d("MainActivity", "Video enabled, delay: ${delay}ms, path: $videoPath")

            if (delay == 0L) {
                // Instant - load video immediately
                loadVideo(videoPath)
            } else {
                // Delayed - show image first, then video
                releasePlayer() // Stop any current video

                if (videoDelayHandler == null) {
                    videoDelayHandler = Handler(Looper.getMainLooper())
                }

                videoDelayRunnable = Runnable {
                    // Double-check conditions before loading video after delay
                    val isStillInBackground = lifecycle.currentState != androidx.lifecycle.Lifecycle.State.RESUMED

                    if (hasWindowFocus && !isStillInBackground && !isScreensaverActive) {
                        loadVideo(videoPath)
                    } else {
                        android.util.Log.d("MainActivity", "Video delayed load cancelled - conditions changed")
                    }
                }

                videoDelayHandler?.postDelayed(videoDelayRunnable!!, delay)
            }
        } else {
            // No video found, release player
            android.util.Log.d("MainActivity", "No video found for system: $systemName, game: $strippedName")
            releasePlayer()
        }
    }

    private fun loadWidgets() {
        // Clear existing widgets
        widgetContainer.removeAllViews()
        activeWidgets.clear()

        // Load saved widgets
        val widgets = widgetManager.loadWidgets()
        widgets.forEach { widget ->
            addWidgetToScreen(widget)
        }
    }

    private fun addWidgetToScreen(widget: OverlayWidget) {
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
                    android.util.Log.d("MainActivity", "Widget ${updatedWidget.id} updated: pos=(${updatedWidget.x}, ${updatedWidget.y}), size=(${updatedWidget.width}, ${updatedWidget.height})")
                } else {
                    // New widget - add it
                    allWidgets.add(updatedWidget)
                    widgetManager.saveWidgets(allWidgets)
                    android.util.Log.d("MainActivity", "New widget ${updatedWidget.id} added")
                }
            }
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

        // Populate widget options based on current view
        val widgetOptions = if (isSystemScrollActive) {
            // System view - only system logo option
            listOf("System Logo" to OverlayWidget.ImageType.SYSTEM_LOGO)
        } else {
            // Game view - all game image types
            listOf(
                "Marquee" to OverlayWidget.ImageType.MARQUEE,
                "2D Box" to OverlayWidget.ImageType.BOX_2D,
                "3D Box" to OverlayWidget.ImageType.BOX_3D,
                "Mix Image" to OverlayWidget.ImageType.MIX_IMAGE,
                "Back Cover" to OverlayWidget.ImageType.BACK_COVER,
                "Physical Media" to OverlayWidget.ImageType.PHYSICAL_MEDIA,
                "Screenshot" to OverlayWidget.ImageType.SCREENSHOT,
                "Fanart" to OverlayWidget.ImageType.FANART,
                "Title Screen" to OverlayWidget.ImageType.TITLE_SCREEN,
                "Game Description" to OverlayWidget.ImageType.GAME_DESCRIPTION
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
        prefs.edit().putBoolean("snap_to_grid", snapToGrid).apply()
    }

    private fun toggleShowGrid() {
        showGrid = !showGrid
        updateGridOverlay()

        // Save show grid state to preferences
        prefs.edit().putBoolean("show_grid", showGrid).apply()

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
        prefs.edit().putBoolean("widgets_locked", widgetsLocked).apply()
    }

    private fun createWidget(imageType: OverlayWidget.ImageType) {
        val displayMetrics = resources.displayMetrics
        val nextZIndex = (activeWidgets.maxOfOrNull { it.widget.zIndex } ?: -1) + 1

        if (imageType == OverlayWidget.ImageType.SYSTEM_LOGO) {
            // Creating system widget
            val systemName = currentSystemName

            if (systemName == null) {
                android.widget.Toast.makeText(
                    this,
                    "No system selected",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Find system logo (custom path or built-in)
            val systemLogoPath = findSystemLogo(systemName)

            if (systemLogoPath == null) {
                android.widget.Toast.makeText(
                    this,
                    "No system logo found for $systemName",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }

            val widget = OverlayWidget(
                imageType = OverlayWidget.ImageType.SYSTEM_LOGO,
                imagePath = systemLogoPath,
                x = displayMetrics.widthPixels / 2f - 150f,
                y = displayMetrics.heightPixels / 2f - 200f,
                width = 300f,
                height = 400f,
                zIndex = nextZIndex,
                widgetContext = OverlayWidget.WidgetContext.SYSTEM
            )

            addWidgetToScreen(widget)

            android.widget.Toast.makeText(
                this,
                "System logo widget created!",
                android.widget.Toast.LENGTH_LONG
            ).show()

        } else {
            // Creating game widget
            val systemName = currentSystemName
            val gameFilename = currentGameFilename

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
                OverlayWidget.ImageType.MARQUEE ->
                    findImageInFolder(systemName, gameName, gameFilename, "marquees")
                OverlayWidget.ImageType.BOX_2D ->
                    findImageInFolder(systemName, gameName, gameFilename, "covers")
                OverlayWidget.ImageType.BOX_3D ->
                    findImageInFolder(systemName, gameName, gameFilename, "3dboxes")
                OverlayWidget.ImageType.MIX_IMAGE ->
                    findImageInFolder(systemName, gameName, gameFilename, "miximages")
                OverlayWidget.ImageType.BACK_COVER ->
                    findImageInFolder(systemName, gameName, gameFilename, "backcovers")
                OverlayWidget.ImageType.PHYSICAL_MEDIA ->
                    findImageInFolder(systemName, gameName, gameFilename, "physicalmedia")
                OverlayWidget.ImageType.SCREENSHOT ->
                    findImageInFolder(systemName, gameName, gameFilename, "screenshots")
                OverlayWidget.ImageType.FANART ->
                    findImageInFolder(systemName, gameName, gameFilename, "fanart")
                OverlayWidget.ImageType.TITLE_SCREEN ->
                    findImageInFolder(systemName, gameName, gameFilename, "titlescreens")
                OverlayWidget.ImageType.GAME_DESCRIPTION -> null
                else -> null
            }

            // Special handling for game description (text widget)
            if (imageType == OverlayWidget.ImageType.GAME_DESCRIPTION) {
                val description = getGameDescription(systemName, gameFilename)

                val widget = OverlayWidget(
                    imageType = OverlayWidget.ImageType.GAME_DESCRIPTION,
                    imagePath = description ?: "",  // Store description text in imagePath
                    x = displayMetrics.widthPixels / 2f - 300f,
                    y = displayMetrics.heightPixels / 2f - 200f,
                    width = 600f,
                    height = 400f,
                    zIndex = nextZIndex,
                    widgetContext = OverlayWidget.WidgetContext.GAME
                )

                addWidgetToScreen(widget)

                android.widget.Toast.makeText(
                    this,
                    if (description != null) "Game description widget created!" else "No description available for this game",
                    android.widget.Toast.LENGTH_LONG
                ).show()

                return
            }

            // Existing validation for image-based widgets
            if (imageFile == null || !imageFile.exists()) {
                val typeName = when (imageType) {
                    OverlayWidget.ImageType.MARQUEE -> "marquee"
                    OverlayWidget.ImageType.BOX_2D -> "2D box"
                    OverlayWidget.ImageType.BOX_3D -> "3D box"
                    OverlayWidget.ImageType.MIX_IMAGE -> "mix image"
                    OverlayWidget.ImageType.BACK_COVER -> "back cover"
                    OverlayWidget.ImageType.PHYSICAL_MEDIA -> "physical media"
                    OverlayWidget.ImageType.SCREENSHOT -> "screenshot"
                    OverlayWidget.ImageType.FANART -> "fanart"
                    OverlayWidget.ImageType.TITLE_SCREEN -> "title screen"
                    OverlayWidget.ImageType.GAME_DESCRIPTION -> "game description"
                    else -> "image"
                }
                android.widget.Toast.makeText(
                    this,
                    "No $typeName image found for this game",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }

            val widget = OverlayWidget(
                imageType = imageType,
                imagePath = imageFile.absolutePath,
                x = displayMetrics.widthPixels / 2f - 150f,
                y = displayMetrics.heightPixels / 2f - 200f,
                width = 300f,
                height = 400f,
                zIndex = nextZIndex,
                widgetContext = OverlayWidget.WidgetContext.GAME
            )

            addWidgetToScreen(widget)

            android.widget.Toast.makeText(
                this,
                "Widget created! Tap to select, drag to move, resize from corners",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun findSystemLogo(systemName: String): String? {
        // Handle ES-DE auto-collections
        val baseFileName = when (systemName.lowercase()) {
            "all" -> "auto-allgames"
            "favorites" -> "auto-favorites"
            "recent" -> "auto-lastplayed"
            else -> systemName.lowercase()
        }
        android.util.Log.d("MainActivity", "Finding system logo for: $baseFileName")

        // First check if custom system logos are enabled
        val customSystemLogosEnabled = prefs.getBoolean("custom_system_logos_enabled", false)

        if (customSystemLogosEnabled) {
            val customLogoPath = prefs.getString("custom_system_logos_path", null)
            android.util.Log.d("MainActivity", "Custom system logos enabled, path: $customLogoPath")

            if (customLogoPath != null) {
                val customLogoDir = File(customLogoPath)
                if (customLogoDir.exists() && customLogoDir.isDirectory) {
                    // Try different extensions
                    val extensions = listOf("svg", "png", "jpg", "webp")
                    for (ext in extensions) {
                        val logoFile = File(customLogoDir, "$baseFileName.$ext")
                        if (logoFile.exists()) {
                            android.util.Log.d("MainActivity", "Found custom system logo: ${logoFile.absolutePath}")
                            return logoFile.absolutePath
                        }
                    }
                }
            }
        }

        // Fall back to built-in assets
        // Return special marker that WidgetView will recognize to load from assets
        android.util.Log.d("MainActivity", "Using built-in system logo for $baseFileName")
        return "builtin://$baseFileName"  // CHANGED: Just pass system name
    }

    private fun updateWidgetsForCurrentSystem() {
        android.util.Log.d("MainActivity", "═══ updateWidgetsForCurrentSystem START ═══")
        android.util.Log.d("MainActivity", "currentSystemName: $currentSystemName")

        // Don't update system widgets during screensaver
        if (isScreensaverActive) {
            android.util.Log.d("MainActivity", "Screensaver active - skipping system widget update")
            return
        }

        val systemName = currentSystemName

        if (systemName != null) {
            // Load saved widgets
            val allWidgets = widgetManager.loadWidgets()

            // Filter for SYSTEM context widgets only
            val systemWidgets = allWidgets.filter { it.widgetContext == OverlayWidget.WidgetContext.SYSTEM }
            android.util.Log.d("MainActivity", "Loaded ${systemWidgets.size} system widgets from storage")

            // Clear existing widget views
            widgetContainer.removeAllViews()
            activeWidgets.clear()
            android.util.Log.d("MainActivity", "Cleared widget container")

            // Sort widgets by z-index
            val sortedWidgets = systemWidgets.sortedBy { it.zIndex }
            android.util.Log.d("MainActivity", "Sorted ${sortedWidgets.size} system widgets by z-index")

            // Reload all system widgets with current system logo
            sortedWidgets.forEachIndexed { index, widget ->
                android.util.Log.d("MainActivity", "Processing system widget $index: type=${widget.imageType}, zIndex=${widget.zIndex}")

                // Find system logo
                val systemLogoPath = findSystemLogo(systemName)

                android.util.Log.d("MainActivity", "  System logo path: $systemLogoPath")

                // Create widget with system logo
                val widgetToAdd = if (systemLogoPath != null) {
                    android.util.Log.d("MainActivity", "  Creating system widget with logo")
                    widget.copy(imagePath = systemLogoPath)
                } else {
                    android.util.Log.d("MainActivity", "  No system logo found, using empty path")
                    widget.copy(imagePath = "")
                }

                addWidgetToScreenWithoutSaving(widgetToAdd)
                android.util.Log.d("MainActivity", "  System widget added to screen")
            }

            android.util.Log.d("MainActivity", "Total system widgets added: ${activeWidgets.size}")
            android.util.Log.d("MainActivity", "Widget container children: ${widgetContainer.childCount}")

            // Make sure container is visible
            widgetContainer.visibility = View.VISIBLE
            android.util.Log.d("MainActivity", "Widget container visibility: ${widgetContainer.visibility}")
        } else {
            android.util.Log.d("MainActivity", "System name is null - not updating widgets")
        }

        android.util.Log.d("MainActivity", "═══ updateWidgetsForCurrentSystem END ═══")
    }

    private fun findImageInFolder(
        systemName: String,
        gameName: String,
        gameFilename: String,
        folder: String
    ): File? {
        val extensions = listOf("jpg", "png", "webp")
        val sanitizedFilename = sanitizeGameFilename(gameFilename)
        val sanitizedName = sanitizedFilename.substringBeforeLast('.')

        return findImageInDir(
            File(getMediaBasePath(), "$systemName/$folder"),
            sanitizedName,
            sanitizedFilename,
            gameFilename,
            extensions
        )
    }

    /**
     * Parse game description from ES-DE gamelist.xml
     * Returns null if not found or any error occurs
     */
    private fun getGameDescription(systemName: String, gameFilename: String): String? {
        try {
            // Get scripts path and navigate to ES-DE folder
            val scriptsPath = prefs.getString("scripts_path", "/storage/emulated/0/ES-DE/scripts")
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

            // Sanitize the game filename for comparison
            val sanitizedFilename = sanitizeGameFilename(gameFilename)

            // Look for the game entry with matching path
            // Match pattern: <path>./filename</path>
            val pathPattern = "<path>\\./\\Q$sanitizedFilename\\E</path>".toRegex()
            val pathMatch = pathPattern.find(xmlContent)

            if (pathMatch == null) {
                android.util.Log.d("MainActivity", "Game not found in gamelist: $sanitizedFilename")
                return null
            }

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
                val description = descMatch.groupValues[1].trim()
                android.util.Log.d("MainActivity", "Found description: ${description.take(100)}...")
                description
            } else {
                android.util.Log.d("MainActivity", "No description found for game: $sanitizedFilename")
                null
            }

        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error parsing gamelist.xml", e)
            return null
        }
    }

    private fun updateWidgetsForCurrentGame() {
        android.util.Log.d("MainActivity", "═══ updateWidgetsForCurrentGame START ═══")
        android.util.Log.d("MainActivity", "isSystemScrollActive: $isSystemScrollActive")
        android.util.Log.d("MainActivity", "isGamePlaying: $isGamePlaying")
        android.util.Log.d("MainActivity", "currentSystemName: $currentSystemName")
        android.util.Log.d("MainActivity", "currentGameFilename: $currentGameFilename")

        // Show widgets in game view OR during screensaver (not in system view, not during gameplay)
        if (!isSystemScrollActive) {  // Remove the !isGamePlaying check
            val systemName = currentSystemName
            val gameFilename = currentGameFilename

            if (systemName != null && gameFilename != null) {
                // Load saved widgets
                val allWidgets = widgetManager.loadWidgets()

                // Filter for GAME context widgets only - ADDED THIS
                val gameWidgets = allWidgets.filter { it.widgetContext == OverlayWidget.WidgetContext.GAME }
                android.util.Log.d("MainActivity", "Loaded ${gameWidgets.size} game widgets from storage")

                // Clear existing widget views
                widgetContainer.removeAllViews()
                activeWidgets.clear()
                android.util.Log.d("MainActivity", "Cleared widget container")

                // Sort widgets by z-index - CHANGED to use gameWidgets
                val sortedWidgets = gameWidgets.sortedBy { it.zIndex }
                android.util.Log.d("MainActivity", "Sorted ${sortedWidgets.size} game widgets by z-index")

                // Reload all widgets with current game images
                sortedWidgets.forEachIndexed { index, widget ->
                    android.util.Log.d("MainActivity", "Processing widget $index: type=${widget.imageType}, zIndex=${widget.zIndex}")

                    val gameName = sanitizeGameFilename(gameFilename).substringBeforeLast('.')
                    android.util.Log.d("MainActivity", "  Looking for images for: $gameName")

                    val imageFile = when (widget.imageType) {
                        OverlayWidget.ImageType.MARQUEE ->
                            findImageInFolder(systemName, gameName, gameFilename, "marquees")
                        OverlayWidget.ImageType.BOX_2D ->
                            findImageInFolder(systemName, gameName, gameFilename, "covers")
                        OverlayWidget.ImageType.BOX_3D ->
                            findImageInFolder(systemName, gameName, gameFilename, "3dboxes")
                        OverlayWidget.ImageType.MIX_IMAGE ->
                            findImageInFolder(systemName, gameName, gameFilename, "miximages")
                        OverlayWidget.ImageType.BACK_COVER ->
                            findImageInFolder(systemName, gameName, gameFilename, "backcovers")
                        OverlayWidget.ImageType.PHYSICAL_MEDIA ->
                            findImageInFolder(systemName, gameName, gameFilename, "physicalmedia")
                        OverlayWidget.ImageType.SCREENSHOT ->
                            findImageInFolder(systemName, gameName, gameFilename, "screenshots")
                        OverlayWidget.ImageType.FANART ->
                            findImageInFolder(systemName, gameName, gameFilename, "fanart")
                        OverlayWidget.ImageType.TITLE_SCREEN ->
                            findImageInFolder(systemName, gameName, gameFilename, "titlescreens")
                        OverlayWidget.ImageType.GAME_DESCRIPTION -> null  // NEW: Text widget, handled separately
                        OverlayWidget.ImageType.SYSTEM_LOGO -> null
                    }

                    android.util.Log.d("MainActivity", "  Image file: ${imageFile?.absolutePath ?: "NULL"}")
                    android.util.Log.d("MainActivity", "  Image exists: ${imageFile?.exists()}")

                    // ALWAYS create the widget, even if image doesn't exist
                    val widgetToAdd = when {
                        // NEW: Handle description text widget
                        widget.imageType == OverlayWidget.ImageType.GAME_DESCRIPTION -> {
                            val description = getGameDescription(systemName, gameFilename)
                            android.util.Log.d("MainActivity", "  Updating description widget: ${description?.take(50)}")
                            widget.copy(imagePath = description ?: "")
                        }
                        // Handle image widgets
                        imageFile != null && imageFile.exists() -> {
                            android.util.Log.d("MainActivity", "  Creating widget with new image")
                            widget.copy(imagePath = imageFile.absolutePath)
                        }
                        // No image found
                        else -> {
                            android.util.Log.d("MainActivity", "  No valid image found, using empty path")
                            // Store game name in widget ID for marquee text fallback
                            if (widget.imageType == OverlayWidget.ImageType.MARQUEE) {
                                widget.copy(
                                    imagePath = "",
                                    id = "widget_${gameName}"
                                )
                            } else {
                                widget.copy(imagePath = "")
                            }
                        }
                    }

                    addWidgetToScreenWithoutSaving(widgetToAdd)
                    android.util.Log.d("MainActivity", "  Widget added to screen")
                }

                android.util.Log.d("MainActivity", "Total widgets added: ${activeWidgets.size}")
                android.util.Log.d("MainActivity", "Widget container children: ${widgetContainer.childCount}")

                // Make sure container is visible
                widgetContainer.visibility = View.VISIBLE
                updateGridOverlay()
                android.util.Log.d("MainActivity", "Widget container visibility: ${widgetContainer.visibility}")
            } else {
                android.util.Log.d("MainActivity", "System or game filename is null - not updating widgets")
            }
        } else if (isSystemScrollActive) {
            // System view - show grid but no game widgets
            android.util.Log.d("MainActivity", "System view - showing grid only")

            // Clear game widgets
            widgetContainer.removeAllViews()
            activeWidgets.clear()

            // Keep container visible and show grid if enabled
            widgetContainer.visibility = View.VISIBLE
            updateGridOverlay()

            android.util.Log.d("MainActivity", "System view setup complete")
        } else {
            // Hide widgets during gameplay or other states
            android.util.Log.d("MainActivity", "Hiding widgets - wrong view state (gameplay)")
            widgetContainer.visibility = View.GONE
            gridOverlayView?.visibility = View.GONE
        }

        android.util.Log.d("MainActivity", "═══ updateWidgetsForCurrentGame END ═══")
    }

    private fun addWidgetToScreenWithoutSaving(widget: OverlayWidget) {
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
                    android.util.Log.d("MainActivity", "Widget ${updatedWidget.id} updated: pos=(${updatedWidget.x}, ${updatedWidget.y}), size=(${updatedWidget.width}, ${updatedWidget.height})")
                }
            }
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
        activeWidgets.forEachIndexed { index, widgetView ->
            android.util.Log.d("MainActivity", "Widget $index: type=${widgetView.widget.imageType}, id=${widgetView.widget.id}, context=${widgetView.widget.widgetContext}")
        }

        // Load ALL existing widgets
        val allExistingWidgets = widgetManager.loadWidgets().toMutableList()
        android.util.Log.d("MainActivity", "Loaded ${allExistingWidgets.size} existing widgets from storage")

        // Determine which context we're currently in
        val currentContext = if (isSystemScrollActive) {
            OverlayWidget.WidgetContext.SYSTEM
        } else {
            OverlayWidget.WidgetContext.GAME
        }
        android.util.Log.d("MainActivity", "Current context: $currentContext")

        // Remove widgets of the CURRENT context only
        val widgetsToKeep = allExistingWidgets.filter { it.widgetContext != currentContext }
        android.util.Log.d("MainActivity", "Keeping ${widgetsToKeep.size} widgets from OTHER context")

        // Add current active widgets (they're all from the current context)
        val updatedWidgets = widgetsToKeep + activeWidgets.map { it.widget }
        android.util.Log.d("MainActivity", "Total widgets to save: ${updatedWidgets.size}")

        widgetManager.saveWidgets(updatedWidgets)
        android.util.Log.d("MainActivity", "Widgets saved")
    }

    private fun updateGridOverlay() {
        if (showGrid && widgetContainer.visibility == View.VISIBLE) {
            // Always recreate grid overlay to ensure it's properly attached
            if (gridOverlayView != null && gridOverlayView?.parent != null) {
                // Remove existing grid if it exists
                widgetContainer.removeView(gridOverlayView)
                gridOverlayView = null
            }

            // Create fresh grid overlay
            gridOverlayView = GridOverlayView(this, gridSize)
            val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            widgetContainer.addView(gridOverlayView, 0)  // Add as first child (behind widgets)
            android.util.Log.d("MainActivity", "Grid overlay recreated and added")
        } else {
            // Remove grid overlay completely (but keep widget container visible)
            if (gridOverlayView != null) {
                widgetContainer.removeView(gridOverlayView)
                gridOverlayView = null
                android.util.Log.d("MainActivity", "Grid overlay removed")
            }
            // Don't hide the widget container - widgets should still be visible
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

    fun bringWidgetToFront(widgetView: WidgetView) {
        // Get max z-index
        val maxZ = activeWidgets.maxOfOrNull { it.widget.zIndex } ?: 0

        // Set this widget to max + 1
        widgetView.widget.zIndex = maxZ + 1

        // Reorder widgets
        reorderWidgetsByZIndex()

        android.util.Log.d("MainActivity", "Widget brought to front with z-index ${widgetView.widget.zIndex}")
    }

    fun sendWidgetToBack(widgetView: WidgetView) {
        // Get min z-index
        val minZ = activeWidgets.minOfOrNull { it.widget.zIndex } ?: 0

        // Set this widget to min - 1
        widgetView.widget.zIndex = minZ - 1

        // Reorder widgets
        reorderWidgetsByZIndex()

        android.util.Log.d("MainActivity", "Widget sent to back with z-index ${widgetView.widget.zIndex}")
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

        // Remove all from container
        widgetContainer.removeAllViews()

        // Re-add in sorted order (lower z-index = added first = appears behind)
        sortedWidgets.forEach { widgetView ->
            widgetContainer.addView(widgetView)
        }

        // Save the updated z-indices
        saveAllWidgetsWithZIndex()
    }

    private fun saveAllWidgetsWithZIndex() {
        val widgets = activeWidgets.map { it.widget }
        widgetManager.saveWidgets(widgets)
        android.util.Log.d("MainActivity", "Saved ${widgets.size} widgets with z-indices")
    }

    fun deselectAllWidgets() {
        activeWidgets.forEach { it.deselect() }
    }

    companion object {
        const val COLUMN_COUNT_KEY = "column_count"
    }
}