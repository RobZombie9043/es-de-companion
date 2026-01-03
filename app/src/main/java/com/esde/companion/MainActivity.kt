package com.esde.companion

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
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
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.EditText
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

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: RelativeLayout
    private lateinit var gameImageView: ImageView
    private lateinit var marqueeImageView: ImageView
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

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var gestureDetector: GestureDetectorCompat

    private var fileObserver: FileObserver? = null
    private var isSystemScrollActive = false
    private var currentGameName: String? = null  // Display name from ES-DE
    private var currentGameFilename: String? = null  // Filename
    private var currentSystemName: String? = null  // Current system
    private var allApps = listOf<ResolveInfo>()  // Store all apps for search filtering
    private var hasWindowFocus = true  // Track if app has window focus (is on top)
    private var isGamePlaying = false  // Track if game is running on other screen
    private var playingGameFilename: String? = null  // Filename of currently playing game
    private var isScreensaverActive = false  // Track if ES-DE screensaver is running
    private var screensaverGameFilename: String? = null  // Current screensaver game filename
    private var screensaverGameName: String? = null  // Current screensaver game name
    private var screensaverSystemName: String? = null  // Current screensaver system name

    // Video playback variables
    private var player: ExoPlayer? = null
    private lateinit var videoView: PlayerView
    private var videoDelayHandler: Handler? = null
    private var videoDelayRunnable: Runnable? = null
    private var currentVideoPath: String? = null

    // Flag to skip reload in onResume (used when returning from settings with no changes)
    private var skipNextReload = false

    // Flag to track if marquee is showing text drawable (needs WRAP_CONTENT)
    private var marqueeShowingText = false

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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("ESDESecondScreenPrefs", MODE_PRIVATE)
        appLaunchPrefs = AppLaunchPreferences(this)

        rootLayout = findViewById(R.id.rootLayout)
        gameImageView = findViewById(R.id.gameImageView)
        marqueeImageView = findViewById(R.id.marqueeImageView)
        dimmingOverlay = findViewById(R.id.dimmingOverlay)
        appDrawer = findViewById(R.id.appDrawer)
        appRecyclerView = findViewById(R.id.appRecyclerView)
        appSearchBar = findViewById(R.id.appSearchBar)
        searchClearButton = findViewById(R.id.searchClearButton)
        drawerBackButton = findViewById(R.id.drawerBackButton)
        settingsButton = findViewById(R.id.settingsButton)
        androidSettingsButton = findViewById(R.id.androidSettingsButton)
        videoView = findViewById(R.id.videoView)

        // Log display information at startup
        logDisplayInfo()

        setupAppDrawer()
        setupSearchBar()
        setupGestureDetector()  // Must be after setupAppDrawer so bottomSheetBehavior is initialized
        setupDrawerBackButton()
        setupSettingsButton()
        setupAndroidSettingsButton()

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

        // Check if all 7 scripts are present with correct names
        val hasCorrectScripts = checkForCorrectScripts()

        // Launch setup wizard if:
        // 1. Setup not completed, OR
        // 2. Missing permissions, OR
        // 3. Scripts are missing/outdated
        if (!hasCompletedSetup || !hasPermission || !hasCorrectScripts) {
            // Delay slightly to let UI settle
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, SettingsActivity::class.java)
                intent.putExtra("AUTO_START_WIZARD", true)
                settingsLauncher.launch(intent)
            }, 1000)
        }
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
                "esde_game_system.txt"
            ),
            "system-select/esdecompanion-system-select.sh" to listOf(
                "LOG_DIR=\"/storage/emulated/0/ES-DE Companion/logs\"",
                "esde_system_name.txt"
            ),
            "game-start/esdecompanion-game-start.sh" to listOf(
                "LOG_DIR=\"/storage/emulated/0/ES-DE Companion/logs\"",
                "esde_gamestart_filename.txt",
                "esde_gamestart_name.txt",
                "esde_gamestart_system.txt"
            ),
            "game-end/esdecompanion-game-end.sh" to listOf(
                "LOG_DIR=\"/storage/emulated/0/ES-DE Companion/logs\"",
                "esde_gameend_filename.txt",
                "esde_gameend_name.txt",
                "esde_gameend_system.txt"
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
                "esde_screensavergameselect_system.txt"
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
                    if (!content.contains(expected)) {
                        return false
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

    override fun onPause() {
        super.onPause()
        // Cancel any pending video delay timers (prevent video loading while in settings)
        videoDelayRunnable?.let { videoDelayHandler?.removeCallbacks(it) }
        // Pause video playback when app goes to background
        player?.pause()
    }

    override fun onResume() {
        super.onResume()

        // Close drawer if it's open (user is returning from Settings or an app)
        // This happens after Settings/app is visible, so no animation is seen
        if (::bottomSheetBehavior.isInitialized &&
            bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        // Update video volume in case audio setting changed
        val audioEnabled = prefs.getBoolean("video_audio_enabled", false)
        player?.volume = if (audioEnabled) 1f else 0f

        // Resume video playback if paused
        player?.play()

        // Clear search bar
        if (::appSearchBar.isInitialized) {
            appSearchBar.text.clear()
        }

        // Reload grid layout in case column count changed
        val columnCount = prefs.getInt("column_count", 4)
        appRecyclerView.layoutManager = GridLayoutManager(this, columnCount)

        // Update marquee size based on logo size setting
        updateMarqueeSize()

        // If marquee is showing text drawable, restore WRAP_CONTENT
        if (marqueeShowingText) {
            val layoutParams = marqueeImageView.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            marqueeImageView.layoutParams = layoutParams
        }

        // Reload images based on current state (don't change modes)
        // Skip reload if returning from settings with no changes
        if (skipNextReload) {
            skipNextReload = false
            android.util.Log.d("MainActivity", "Skipping reload - no settings changed")
        } else {
            if (isSystemScrollActive) {
                loadSystemImage()
            } else {
                loadGameInfo()
            }
        }
    }

    private fun updateMarqueeSize() {
        val logoSize = prefs.getString("logo_size", "medium") ?: "medium"
        val layoutParams = marqueeImageView.layoutParams

        when (logoSize) {
            "small" -> {
                layoutParams.width = dpToPx(225)
                layoutParams.height = dpToPx(300)
            }
            "large" -> {
                layoutParams.width = dpToPx(375)
                layoutParams.height = dpToPx(500)
            }
            else -> { // medium
                layoutParams.width = dpToPx(300)
                layoutParams.height = dpToPx(400)
            }
        }

        marqueeImageView.layoutParams = layoutParams
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
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
        val path = customPath ?: "${Environment.getExternalStorageDirectory()}/ES-DE/downloaded_media/system_images"
        android.util.Log.d("ESDESecondScreen", "System image path: $path")
        return path
    }

    private fun getSystemLogosPath(): String {
        val customPath = prefs.getString("system_logos_path", null)
        val path = customPath ?: "${Environment.getExternalStorageDirectory()}/ES-DE/downloaded_media/system_logos"
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
        // Try to load from assets first (since you added default_background.webp)
        try {
            val assetPath = "fallback/default_background.webp"
            Glide.with(this)
                .load(android.net.Uri.parse("file:///android_asset/$assetPath"))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(gameImageView)
            android.util.Log.d("MainActivity", "Loaded fallback image from assets")
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Failed to load fallback image, using solid color", e)
            // Final fallback: solid color
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
                // No animation - instant display
                Glide.with(this)
                    .load(imageFile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
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
                // Fade only - no scale
                Glide.with(this)
                    .load(imageFile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
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
                            // Apply fade-in animation only
                            targetView.alpha = 0f
                            targetView.animate()
                                .alpha(1f)
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
            else -> {
                // "scale_fade" - default with scale + fade
                Glide.with(this)
                    .load(imageFile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
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
                            // Apply scale + fade-in animation
                            targetView.alpha = 0f
                            targetView.scaleX = scaleAmount
                            targetView.scaleY = scaleAmount
                            targetView.animate()
                                .alpha(1f)
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
                    val swipeThreshold = 50
                    val velocityThreshold = 50

                    if (abs(diffY) > swipeThreshold && abs(velocityY) > velocityThreshold) {
                        if (diffY < 0) {
                            // Swipe up
                            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                                return true
                            }
                        } else {
                            // Swipe down
                            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                                return true
                            }
                        }
                    }
                }
                return false
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Only intercept gestures when the drawer is hidden
        val currentState = bottomSheetBehavior.state
        if (ev.action == MotionEvent.ACTION_DOWN) {
            android.util.Log.d("MainActivity", "Touch down, drawer state: $currentState")
        }

        if (currentState == BottomSheetBehavior.STATE_HIDDEN) {
            gestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun setupAppDrawer() {
        bottomSheetBehavior = BottomSheetBehavior.from(appDrawer)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.skipCollapsed = true  // Skip collapsed state, go straight to expanded

        // Add callback to detect drawer state changes
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    // Drawer just opened - show hint if first time
                    showSettingsPulseHint()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Not needed
            }
        })

        // Post to ensure view is laid out before setting state
        appDrawer.post {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            android.util.Log.d("MainActivity", "AppDrawer state set to HIDDEN: ${bottomSheetBehavior.state}")
        }

        val columnCount = prefs.getInt("column_count", 4)
        appRecyclerView.layoutManager = GridLayoutManager(this, columnCount)

        // Get hidden apps from preferences
        val hiddenApps = prefs.getStringSet("hidden_apps", setOf()) ?: setOf()

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        allApps = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
            .filter { !hiddenApps.contains(it.activityInfo?.packageName ?: "") }  // Filter out hidden apps
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }  // Case-insensitive sort

        appRecyclerView.adapter = AppAdapter(allApps, packageManager,
            onAppClick = { app ->
                launchApp(app)
            },
            onAppLongClick = { app, view ->
                showAppOptionsDialog(app)
            }
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

                val filteredApps = if (query.isEmpty()) {
                    allApps
                } else {
                    allApps.filter { app ->
                        app.loadLabel(packageManager).toString().lowercase().contains(query)
                    }
                }

                appRecyclerView.adapter = AppAdapter(filteredApps, packageManager,
                    onAppClick = { app ->
                        launchApp(app)
                    },
                    onAppLongClick = { app, view ->
                        showAppOptionsDialog(app)
                    }
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
                            when (path) {
                                "esde_system_name.txt" -> {
                                    // Ignore if screensaver is active
                                    if (isScreensaverActive) {
                                        android.util.Log.d("MainActivity", "System scroll ignored - screensaver active")
                                        return@postDelayed
                                    }
                                    android.util.Log.d("MainActivity", "System scroll detected")
                                    loadSystemImageDebounced()
                                }
                                "esde_game_filename.txt" -> {
                                    // Ignore if screensaver is active
                                    if (isScreensaverActive) {
                                        android.util.Log.d("MainActivity", "Game scroll ignored - screensaver active")
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
                                    android.util.Log.d("MainActivity", "Screensaver end detected")
                                    isScreensaverActive = false
                                    screensaverGameFilename = null
                                    screensaverGameName = null
                                    screensaverSystemName = null
                                    handleScreensaverEnd()
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
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        fileObserver?.stopWatching()
        unregisterReceiver(appChangeReceiver)
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
            // Stop any videos when app loses focus
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
    private fun loadSystemLogoFromAssets(systemName: String): android.graphics.drawable.Drawable? {
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
                // Check formats in priority order: SVG (best quality) -> PNG -> JPG -> WebP
                val extensions = listOf("svg", "png", "jpg", "jpeg", "webp")

                for (ext in extensions) {
                    val logoFile = File(userLogosDir, "$baseFileName.$ext")
                    if (logoFile.exists()) {
                        android.util.Log.d("MainActivity", "Loading logo from user path: $logoFile")

                        return when (ext) {
                            "svg" -> {
                                // Load SVG
                                val svg = com.caverock.androidsvg.SVG.getFromInputStream(logoFile.inputStream())
                                android.graphics.drawable.PictureDrawable(svg.renderToPicture())
                            }
                            else -> {
                                // Load bitmap formats (PNG, JPG, WebP)
                                val bitmap = android.graphics.BitmapFactory.decodeFile(logoFile.absolutePath)
                                android.graphics.drawable.BitmapDrawable(resources, bitmap)
                            }
                        }
                    }
                }
            }

            // Fall back to built-in SVG assets
            val svgPath = "system_logos/$baseFileName.svg"
            val svg = com.caverock.androidsvg.SVG.getFromAsset(assets, svgPath)
            android.graphics.drawable.PictureDrawable(svg.renderToPicture())
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Failed to load logo for $systemName", e)
            null
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
        try {
            // Stop any video playback when switching to system view
            releasePlayer()

            val logsDir = File(getLogsPath())
            val systemFile = File(logsDir, "esde_system_name.txt")
            if (!systemFile.exists()) return

            // Clear marquee when loading system image (systems don't have marquees)
            marqueeImageView.visibility = View.GONE
            Glide.with(this).clear(marqueeImageView)
            marqueeImageView.setImageDrawable(null)

            val systemName = systemFile.readText().trim()

            // Store current system name for later reference
            currentSystemName = systemName
            currentGameName = null  // Clear game info when in system view
            currentGameFilename = null
            val imageFile = File(getSystemImagePath(), "$systemName.webp")

            var imageToUse: File? = imageFile

            if (!imageFile.exists()) {
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

                // Don't look for game marquee - we'll use built-in system logo instead
            }

            if (imageToUse != null && imageToUse.exists()) {
                isSystemScrollActive = true
                loadImageWithAnimation(imageToUse, gameImageView)

                // Use built-in system logo as marquee overlay
                if (prefs.getBoolean("system_logo_enabled", true)) {
                    val logoDrawable = loadSystemLogoFromAssets(systemName)
                    if (logoDrawable != null) {
                        // Restore fixed size for actual logo images
                        updateMarqueeSize()

                        marqueeImageView.visibility = View.VISIBLE
                        marqueeImageView.setImageDrawable(logoDrawable)
                        marqueeShowingText = false
                    }
                }
            } else {
                // No custom image and no game images found
                // Show built-in logo centered on solid background
                val logoDrawable = loadSystemLogoFromAssets(systemName)
                if (logoDrawable != null) {
                    isSystemScrollActive = true

                    // Set solid background color
                    loadFallbackBackground() // Use fallback image instead of solid color

                    // Show logo as overlay (respects logo size setting)
                    if (prefs.getBoolean("system_logo_enabled", true)) {
                        // Restore fixed size for actual logo images
                        updateMarqueeSize()

                        marqueeImageView.visibility = View.VISIBLE
                        marqueeImageView.setImageDrawable(logoDrawable)
                        marqueeShowingText = false
                    }
                } else {
                    // No built-in logo found - show fallback with or without text
                    isSystemScrollActive = true
                    loadFallbackBackground() // Always show fallback (regardless of logo setting)

                    val logoSize = prefs.getString("logo_size", "medium") ?: "medium"
                    if (prefs.getBoolean("system_logo_enabled", true)) {
                        // Logo enabled - show text overlay
                        val textDrawable = createTextDrawable(systemName, logoSize)

                        // Use WRAP_CONTENT for text to show at full size
                        val layoutParams = marqueeImageView.layoutParams
                        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        marqueeImageView.layoutParams = layoutParams

                        marqueeImageView.visibility = View.VISIBLE
                        marqueeImageView.setImageDrawable(textDrawable)
                        marqueeShowingText = true
                    } else {
                        // Logo disabled - just show fallback, no overlay
                        marqueeImageView.visibility = View.GONE
                        marqueeShowingText = false
                    }
                }
            }

        } catch (e: Exception) {
            // Don't clear images on exception - keep last valid images
            android.util.Log.e("MainActivity", "Error loading system image", e)
        }
    }

    private fun loadGameInfo() {
        isSystemScrollActive = false

        try {
            val logsDir = File(getLogsPath())
            val gameFile = File(logsDir, "esde_game_filename.txt")
            if (!gameFile.exists()) return

            val gameNameRaw = gameFile.readText().trim()
            val gameName = gameNameRaw.substringBeforeLast('.')

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


            // Try to find game-specific artwork first
            val gameImage = findGameImage(systemName, gameName, gameNameRaw)

            var gameImageLoaded = false

            if (gameImage != null && gameImage.exists()) {
                // Game has its own artwork - use it
                loadImageWithAnimation(gameImage, gameImageView)
                gameImageLoaded = true
            } else {
                // Game has no artwork - check for game marquee to display on dark background
                val marqueeFile = findMarqueeImage(systemName, gameName, gameNameRaw)

                if (marqueeFile != null && marqueeFile.exists()) {
                    // Game has marquee - show it centered on dark background
                    loadFallbackBackground() // Use fallback image instead of solid color

                    // Load marquee content (even if video is playing - just keep it hidden)
                    if (prefs.getBoolean("game_logo_enabled", true)) {
                        // Restore fixed size for actual marquee images
                        updateMarqueeSize()

                        loadImageWithAnimation(marqueeFile, marqueeImageView)
                        // Only show if video is NOT playing
                        marqueeImageView.visibility = if (isVideoPlaying()) View.GONE else View.VISIBLE
                        marqueeShowingText = false
                    }
                    gameImageLoaded = true
                } else {
                    // No artwork and no marquee - show fallback with or without text
                    loadFallbackBackground() // Always load fallback (regardless of logo setting)

                    if (prefs.getBoolean("game_logo_enabled", true)) {
                        // Logo enabled - show game name as text overlay
                        val displayName = currentGameName ?: gameName
                        val logoSize = prefs.getString("logo_size", "medium") ?: "medium"
                        val textDrawable = createTextDrawable(displayName, logoSize)

                        // Use WRAP_CONTENT for text to show at full size
                        val layoutParams = marqueeImageView.layoutParams
                        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        marqueeImageView.layoutParams = layoutParams

                        marqueeImageView.setImageDrawable(textDrawable)
                        // Only show if video is NOT playing
                        marqueeImageView.visibility = if (isVideoPlaying()) View.GONE else View.VISIBLE
                        marqueeShowingText = true
                    } else {
                        // Logo disabled - just show fallback, no text
                        marqueeImageView.visibility = View.GONE
                        marqueeShowingText = false
                    }
                    gameImageLoaded = true
                }
            }
            // Handle marquee separately when game has its own artwork
            // Note: gameImageLoaded is always true here by design - we've loaded fallback, marquee, or actual image
            if (gameImageLoaded && gameImage != null && gameImage.exists()) {
                val marqueeFile = findMarqueeImage(systemName, gameName, gameNameRaw)
                if (marqueeFile != null && marqueeFile.exists()) {
                    if (!prefs.getBoolean("game_logo_enabled", true)) {
                        marqueeImageView.visibility = View.GONE
                        Glide.with(this).clear(marqueeImageView)
                        marqueeImageView.setImageDrawable(null)
                    } else {
                        // Restore fixed size for actual marquee images
                        updateMarqueeSize()

                        // Load marquee content
                        loadImageWithAnimation(marqueeFile, marqueeImageView)
                        // Only show if video is NOT playing
                        marqueeImageView.visibility = if (isVideoPlaying()) View.GONE else View.VISIBLE
                        marqueeShowingText = false
                    }
                } else {
                    // Game has no marquee - clear it (don't show wrong marquee from previous game)
                    if (prefs.getBoolean("game_logo_enabled", true)) {
                        // Only clear if logo is supposed to be shown
                        // If logo is off or video is playing, it's already hidden
                        Glide.with(this).clear(marqueeImageView)
                        marqueeImageView.setImageDrawable(null)
                    }
                }
            }
            // If gameImageLoaded is false, we couldn't load new game data
            // Keep last marquee displayed (prevents disappearing during fast scroll)

            // Handle video playback for the current game
            // Pass both stripped name and raw filename (like images do)
            handleVideoForGame(currentSystemName, gameName, gameNameRaw)

        } catch (e: Exception) {
            // Don't clear images on exception - keep last valid images
            // This prevents black screen during ES-DE fast scroll when no data available
            android.util.Log.e("MainActivity", "Error loading game info", e)
        }
    }

    private fun findGameImage(systemName: String, gameName: String, fullGameName: String): File? {
        val extensions = listOf("jpg", "png", "webp")
        val mediaBase = File(getMediaBasePath(), systemName)
        val imagePref = prefs.getString("image_preference", "fanart") ?: "fanart"
        val dirs = if (imagePref == "screenshot") {
            listOf("screenshots", "fanart")
        } else {
            listOf("fanart", "screenshots")
        }

        for (dirName in dirs) {
            val file = findImageInDir(File(mediaBase, dirName), gameName, fullGameName, extensions)
            if (file != null) return file
        }
        return null
    }

    private fun findMarqueeImage(systemName: String, gameName: String, fullGameName: String): File? {
        val extensions = listOf("jpg", "png", "webp")
        return findImageInDir(File(getMediaBasePath(), "$systemName/marquees"), gameName, fullGameName, extensions)
    }

    private fun findImageInDir(dir: File, strippedName: String, rawName: String, extensions: List<String>): File? {
        for (name in listOf(strippedName, rawName)) {
            for (ext in extensions) {
                val file = File(dir, "$name.$ext")
                if (file.exists()) return file
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
     * Launch app on top display (display ID 0)
     */
    /**
     * Show app options dialog with launch position toggles
     * Note: "Top" now means "This screen" (same as companion)
     *       "Bottom" now means "Other screen" (opposite of companion)
     */
    private fun showAppOptionsDialog(app: ResolveInfo) {
        val packageName = app.activityInfo?.packageName ?: return
        val appName = app.loadLabel(packageManager).toString()

        // Inflate custom dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_app_options, null)
        val dialogAppName = dialogView.findViewById<TextView>(R.id.dialogAppName)
        val btnAppInfo = dialogView.findViewById<MaterialButton>(R.id.btnAppInfo)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.launchPositionChipGroup)
        val chipLaunchTop = dialogView.findViewById<Chip>(R.id.chipLaunchTop)  // "This screen"
        val chipLaunchBottom = dialogView.findViewById<Chip>(R.id.chipLaunchBottom)  // "Other screen"

        // Set app name
        dialogAppName.text = appName

        // Get current launch position and set initial chip state
        val currentPosition = appLaunchPrefs.getLaunchPosition(packageName)
        if (currentPosition == AppLaunchPreferences.POSITION_TOP) {
            chipLaunchTop.isChecked = true  // "This screen"
        } else {
            chipLaunchBottom.isChecked = true  // "Other screen"
        }

        // Create dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // App Info button click
        btnAppInfo.setOnClickListener {
            openAppInfo(packageName)
            dialog.dismiss()
        }

        // Listen for chip selection changes
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            when {
                checkedIds.contains(R.id.chipLaunchTop) -> {
                    // TOP = "This screen" (same display as companion)
                    appLaunchPrefs.setLaunchPosition(packageName, AppLaunchPreferences.POSITION_TOP)
                    android.util.Log.d("MainActivity", "Set $appName to launch on THIS screen")
                }
                checkedIds.contains(R.id.chipLaunchBottom) -> {
                    // BOTTOM = "Other screen" (opposite display from companion)
                    appLaunchPrefs.setLaunchPosition(packageName, AppLaunchPreferences.POSITION_BOTTOM)
                    android.util.Log.d("MainActivity", "Set $appName to launch on OTHER screen")
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

    /**
     * Handle game start event (game launched on other screen)
     */
    private fun handleGameStart() {
        // Update display based on user preference
        val gameLaunchBehavior = prefs.getString("game_launch_behavior", "default_image") ?: "default_image"

        when (gameLaunchBehavior) {
            "game_image" -> {
                // Load and show the launched game's image and marquee
                val filename = playingGameFilename
                if (filename != null) {
                    // Load the game's artwork
                    val systemName = currentSystemName
                    if (systemName != null) {
                        val gameName = filename.substringBeforeLast('.')
                        val gameImage = findGameImage(systemName, gameName, filename)
                        if (gameImage != null) {
                            Glide.with(this)
                                .load(gameImage)
                                .into(gameImageView)
                        } else {
                            loadFallbackBackground()
                        }

                        // Load the game's marquee
                        if (prefs.getBoolean("game_logo_enabled", true)) {
                            val marqueeImage = findMarqueeImage(systemName, gameName, filename)
                            if (marqueeImage != null) {
                                Glide.with(this)
                                    .load(marqueeImage)
                                    .into(marqueeImageView)
                                marqueeImageView.visibility = View.VISIBLE
                            } else {
                                marqueeImageView.visibility = View.GONE
                            }
                        } else {
                            marqueeImageView.visibility = View.GONE
                        }
                    }
                }

                gameImageView.visibility = View.VISIBLE
                videoView.visibility = View.GONE
            }
            "black_screen" -> {
                // Show plain black screen - no logos or images
                gameImageView.setImageDrawable(null)
                gameImageView.setBackgroundColor(android.graphics.Color.BLACK)
                gameImageView.visibility = View.VISIBLE

                // Clear and hide marquee completely
                marqueeImageView.setImageDrawable(null)
                marqueeImageView.visibility = View.GONE
                Glide.with(this).clear(marqueeImageView)

                videoView.visibility = View.GONE
            }
            else -> { // "default_image"
                // Show default fallback image with the game's marquee
                loadFallbackBackground()
                gameImageView.visibility = View.VISIBLE
                videoView.visibility = View.GONE

                // Load and show the game's marquee if enabled
                if (prefs.getBoolean("game_logo_enabled", true)) {
                    val filename = playingGameFilename
                    val systemName = currentSystemName
                    if (filename != null && systemName != null) {
                        val gameName = filename.substringBeforeLast('.')
                        val marqueeImage = findMarqueeImage(systemName, gameName, filename)
                        if (marqueeImage != null) {
                            Glide.with(this)
                                .load(marqueeImage)
                                .into(marqueeImageView)
                            marqueeImageView.visibility = View.VISIBLE
                        } else {
                            marqueeImageView.visibility = View.GONE
                        }
                    } else {
                        marqueeImageView.visibility = View.GONE
                    }
                } else {
                    marqueeImageView.visibility = View.GONE
                }
            }
        }

        // Stop any videos
        releasePlayer()
    }

    /**
     * Handle game end event - return to normal browsing display
     */
    private fun handleGameEnd() {
        // Return to normal display - reload the current game/system
        if (isSystemScrollActive) {
            loadSystemImage()
        } else {
            loadGameInfo()
        }
    }

    // ========== SCREENSAVER FUNCTIONS ==========

    /**
     * Handle screensaver start event
     */
    private fun handleScreensaverStart() {
        // Stop any videos
        releasePlayer()

        val screensaverBehavior = prefs.getString("screensaver_behavior", "default_image") ?: "default_image"

        when (screensaverBehavior) {
            "game_image" -> {
                // Load screensaver game if available
                if (screensaverGameFilename != null && screensaverSystemName != null) {
                    // Load the screensaver game's artwork
                    val gameName = screensaverGameFilename!!.substringBeforeLast('.')
                    val gameImage = findGameImage(screensaverSystemName!!, gameName, screensaverGameFilename!!)
                    if (gameImage != null) {
                        Glide.with(this)
                            .load(gameImage)
                            .into(gameImageView)
                    } else {
                        gameImageView.setImageDrawable(null)
                        gameImageView.setBackgroundColor(android.graphics.Color.BLACK)
                    }

                    // Load the screensaver game's marquee
                    if (prefs.getBoolean("game_logo_enabled", true)) {
                        val marqueeImage = findMarqueeImage(screensaverSystemName!!, gameName, screensaverGameFilename!!)
                        if (marqueeImage != null) {
                            Glide.with(this)
                                .load(marqueeImage)
                                .into(marqueeImageView)
                            marqueeImageView.visibility = View.VISIBLE
                        } else {
                            marqueeImageView.visibility = View.GONE
                        }
                    } else {
                        marqueeImageView.visibility = View.GONE
                    }
                } else {
                    // No screensaver game data yet (dim/black mode or just started)
                    // Show black screen - will update if game select events fire
                    gameImageView.setImageDrawable(null)
                    gameImageView.setBackgroundColor(android.graphics.Color.BLACK)
                    marqueeImageView.setImageDrawable(null)
                    marqueeImageView.visibility = View.GONE
                }

                gameImageView.visibility = View.VISIBLE
                videoView.visibility = View.GONE
            }
            "black_screen" -> {
                // Show plain black screen - no logos or images
                gameImageView.setImageDrawable(null)
                gameImageView.setBackgroundColor(android.graphics.Color.BLACK)
                gameImageView.visibility = View.VISIBLE

                // Clear and hide marquee completely
                marqueeImageView.setImageDrawable(null)
                marqueeImageView.visibility = View.GONE
                Glide.with(this).clear(marqueeImageView)

                videoView.visibility = View.GONE
            }
            else -> { // "default_image"
                // Show default fallback image with current screensaver game's marquee (if available)
                loadFallbackBackground()
                gameImageView.visibility = View.VISIBLE
                videoView.visibility = View.GONE

                // Show the screensaver game's marquee if available, otherwise fall back to browsing state marquee
                if (prefs.getBoolean("game_logo_enabled", true)) {
                    // First try to show screensaver game marquee (slideshow/video mode)
                    if (screensaverGameFilename != null && screensaverSystemName != null) {
                        val gameName = screensaverGameFilename!!.substringBeforeLast('.')
                        val marqueeImage = findMarqueeImage(screensaverSystemName!!, gameName, screensaverGameFilename!!)
                        if (marqueeImage != null) {
                            Glide.with(this)
                                .load(marqueeImage)
                                .into(marqueeImageView)
                            marqueeImageView.visibility = View.VISIBLE
                        } else {
                            marqueeImageView.visibility = View.GONE
                        }
                    } else {
                        // No screensaver game data (dim/black mode) - show browsing state marquee
                        if (isSystemScrollActive) {
                            // Show system logo
                            val systemName = currentSystemName
                            if (systemName != null && prefs.getBoolean("system_logo_enabled", true)) {
                                val logoDrawable = loadSystemLogoFromAssets(systemName)
                                if (logoDrawable != null) {
                                    updateMarqueeSize()
                                    marqueeImageView.setImageDrawable(logoDrawable)
                                    marqueeImageView.visibility = View.VISIBLE
                                    marqueeShowingText = false
                                } else {
                                    marqueeImageView.visibility = View.GONE
                                }
                            } else {
                                marqueeImageView.visibility = View.GONE
                            }
                        } else {
                            // Show game marquee from browsing state
                            val filename = currentGameFilename
                            val systemName = currentSystemName
                            if (filename != null && systemName != null) {
                                val gameName = filename.substringBeforeLast('.')
                                val marqueeImage = findMarqueeImage(systemName, gameName, filename)
                                if (marqueeImage != null) {
                                    Glide.with(this)
                                        .load(marqueeImage)
                                        .into(marqueeImageView)
                                    marqueeImageView.visibility = View.VISIBLE
                                } else {
                                    marqueeImageView.visibility = View.GONE
                                }
                            } else {
                                marqueeImageView.visibility = View.GONE
                            }
                        }
                    }
                } else {
                    marqueeImageView.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Handle screensaver end event - return to normal browsing display
     */
    private fun handleScreensaverEnd() {
        // Return to normal display - reload the current game/system
        if (isSystemScrollActive) {
            loadSystemImage()
        } else {
            loadGameInfo()
        }
    }

    /**
     * Handle screensaver game select event (for slideshow/video screensavers)
     */
    private fun handleScreensaverGameSelect() {
        val screensaverBehavior = prefs.getString("screensaver_behavior", "default_image") ?: "default_image"

        if (screensaverGameFilename != null && screensaverSystemName != null) {
            val gameName = screensaverGameFilename!!.substringBeforeLast('.')

            when (screensaverBehavior) {
                "game_image" -> {
                    // Load the screensaver game's artwork and marquee
                    val gameImage = findGameImage(
                        screensaverSystemName!!,
                        gameName,
                        screensaverGameFilename!!
                    )

                    if (gameImage != null && gameImage.exists()) {
                        loadImageWithAnimation(gameImage, gameImageView)

                        // Load marquee if enabled
                        if (prefs.getBoolean("game_logo_enabled", true)) {
                            val marqueeFile = findMarqueeImage(
                                screensaverSystemName!!,
                                gameName,
                                screensaverGameFilename!!
                            )

                            if (marqueeFile != null && marqueeFile.exists()) {
                                updateMarqueeSize()
                                loadImageWithAnimation(marqueeFile, marqueeImageView)
                                marqueeImageView.visibility = View.VISIBLE
                                marqueeShowingText = false
                            } else {
                                // No marquee - show game name as text if logo enabled
                                val displayName = screensaverGameName ?: gameName
                                val logoSize = prefs.getString("logo_size", "medium") ?: "medium"
                                val textDrawable = createTextDrawable(displayName, logoSize)

                                val layoutParams = marqueeImageView.layoutParams
                                layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                marqueeImageView.layoutParams = layoutParams

                                marqueeImageView.setImageDrawable(textDrawable)
                                marqueeImageView.visibility = View.VISIBLE
                                marqueeShowingText = true
                            }
                        } else {
                            marqueeImageView.visibility = View.GONE
                        }
                    } else {
                        // No game image - show fallback with marquee/text
                        loadFallbackBackground()

                        if (prefs.getBoolean("game_logo_enabled", true)) {
                            val marqueeFile = findMarqueeImage(
                                screensaverSystemName!!,
                                gameName,
                                screensaverGameFilename!!
                            )

                            if (marqueeFile != null && marqueeFile.exists()) {
                                updateMarqueeSize()
                                loadImageWithAnimation(marqueeFile, marqueeImageView)
                                marqueeImageView.visibility = View.VISIBLE
                                marqueeShowingText = false
                            } else {
                                val displayName = screensaverGameName ?: gameName
                                val logoSize = prefs.getString("logo_size", "medium") ?: "medium"
                                val textDrawable = createTextDrawable(displayName, logoSize)

                                val layoutParams = marqueeImageView.layoutParams
                                layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                marqueeImageView.layoutParams = layoutParams

                                marqueeImageView.setImageDrawable(textDrawable)
                                marqueeImageView.visibility = View.VISIBLE
                                marqueeShowingText = true
                            }
                        } else {
                            marqueeImageView.visibility = View.GONE
                        }
                    }
                }
                "default_image" -> {
                    // Keep fallback image, but update marquee to screensaver game's marquee
                    if (prefs.getBoolean("game_logo_enabled", true)) {
                        val marqueeFile = findMarqueeImage(
                            screensaverSystemName!!,
                            gameName,
                            screensaverGameFilename!!
                        )

                        if (marqueeFile != null && marqueeFile.exists()) {
                            updateMarqueeSize()
                            loadImageWithAnimation(marqueeFile, marqueeImageView)
                            marqueeImageView.visibility = View.VISIBLE
                            marqueeShowingText = false
                        } else {
                            marqueeImageView.visibility = View.GONE
                        }
                    } else {
                        marqueeImageView.visibility = View.GONE
                    }
                }
                // "black_screen" mode doesn't update anything
            }
        }
    }

    // ========== VIDEO PLAYBACK FUNCTIONS ==========

    /**
     * Check if video is enabled in settings
     */
    private fun isVideoEnabled(): Boolean {
        return prefs.getBoolean("video_enabled", false)
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
        val progress = prefs.getInt("video_delay", 0) // 0-10
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
        if (systemName == null || strippedName == null) return null

        val mediaPath = prefs.getString("media_path", "/storage/emulated/0/ES-DE/downloaded_media")
            ?: return null

        val videoExtensions = listOf("mp4", "mkv", "avi", "wmv", "mov", "webm")
        val videoDir = File(mediaPath, "$systemName/videos")

        if (!videoDir.exists()) {
            android.util.Log.d("MainActivity", "Video directory does not exist: ${videoDir.absolutePath}")
            return null
        }

        // Try both stripped name and raw name (like images do)
        for (name in listOf(strippedName, rawName)) {
            if (name == null) continue
            for (ext in videoExtensions) {
                val videoFile = File(videoDir, "$name.$ext")
                if (videoFile.exists()) {
                    android.util.Log.d("MainActivity", "Found video: ${videoFile.absolutePath}")
                    return videoFile.absolutePath
                }
            }
        }

        android.util.Log.d("MainActivity", "No video found for $strippedName in $videoDir")
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

            // Set volume based on settings
            val audioEnabled = prefs.getBoolean("video_audio_enabled", false)
            player?.volume = if (audioEnabled) 1f else 0f
            android.util.Log.d("MainActivity", "Video audio: ${if (audioEnabled) "ON" else "OFF"}")

            // Create media item
            val mediaItem = MediaItem.fromUri(videoPath)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true
            player?.repeatMode = Player.REPEAT_MODE_ONE // Loop video

            // Hide the game image view and marquee so video is visible
            gameImageView.visibility = View.GONE
            marqueeImageView.visibility = View.GONE

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

                    // Restore marquee visibility if game logo is enabled
                    // Note: The marquee content will be set by loadGameInfo when called
                    if (prefs.getBoolean("game_logo_enabled", true)) {
                        marqueeImageView.visibility = View.VISIBLE
                    }
                }
                .start()
        } else {
            // No player, just hide the video view and show image view
            videoView.visibility = View.GONE
            gameImageView.visibility = View.VISIBLE
            currentVideoPath = null

            // Restore marquee visibility if game logo is enabled
            if (prefs.getBoolean("game_logo_enabled", true)) {
                marqueeImageView.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Handle video loading with delay
     */
    private fun handleVideoForGame(systemName: String?, strippedName: String?, rawName: String?) {
        // Cancel any pending video load
        videoDelayRunnable?.let { videoDelayHandler?.removeCallbacks(it) }

        // Don't play videos if:
        // 1. App doesn't have window focus (game launched on top of companion)
        // 2. Game is playing on other screen
        // 3. Screensaver is active
        if (!hasWindowFocus) {
            android.util.Log.d("MainActivity", "Video blocked - app doesn't have focus (game on top)")
            releasePlayer()
            return
        }

        if (isGamePlaying) {
            android.util.Log.d("MainActivity", "Video blocked - game playing on other screen")
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
                    // Double-check focus, game state, and screensaver before loading video
                    if (hasWindowFocus && !isGamePlaying && !isScreensaverActive) {
                        loadVideo(videoPath)
                    } else {
                        android.util.Log.d("MainActivity", "Video delayed load cancelled - focus lost, game playing, or screensaver active")
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
}
