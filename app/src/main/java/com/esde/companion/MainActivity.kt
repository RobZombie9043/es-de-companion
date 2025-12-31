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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.EditText
import android.widget.TextView
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
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
    private var allApps = listOf<ResolveInfo>()  // Store all apps for search filtering

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
            }
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

        setupAppDrawer()
        setupSearchBar()
        setupGestureDetector()  // Must be after setupAppDrawer so bottomSheetBehavior is initialized
        setupDrawerBackButton()
        setupSettingsButton()
        setupAndroidSettingsButton()

        // Apply drawer transparency
        updateDrawerTransparency()

        val sdcard = Environment.getExternalStorageDirectory()
        val systemScrollFile = File(sdcard, "ES-DE/logs/esde_system_scroll.txt")
        val gameScrollFile = File(sdcard, "ES-DE/logs/esde_game_scroll.txt")

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

        // If setup not complete or missing permissions, launch settings with wizard
        if (!hasCompletedSetup || !hasPermission) {
            // Delay slightly to let UI settle
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, SettingsActivity::class.java)
                intent.putExtra("AUTO_START_WIZARD", true)
                settingsLauncher.launch(intent)
            }, 1000)
        }
    }

    override fun onResume() {
        super.onResume()

        // Close app drawer and clear search when returning from a launched app
        if (::bottomSheetBehavior.isInitialized) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            appSearchBar.text.clear()
        }

        // Reload grid layout in case column count changed
        val columnCount = prefs.getInt("column_count", 4)
        appRecyclerView.layoutManager = GridLayoutManager(this, columnCount)

        // Update marquee size based on logo size setting
        updateMarqueeSize()

        // Reload images based on current state (don't change modes)
        if (isSystemScrollActive) {
            loadSystemImage()
        } else {
            loadGameInfo()
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
        val path = customPath ?: "${Environment.getExternalStorageDirectory()}/esdesecondscreen/systems"
        android.util.Log.d("ESDESecondScreen", "System image path: $path")
        return path
    }

    private fun getCrossfadeDuration(): Int {
        return when (prefs.getString("crossfade", "off")) {
            "off" -> 1  // Use 1ms instead of 0 for truly instant swap
            else -> 150
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

        val effectType = if (animationStyle == "custom") {
            prefs.getString("animation_effect", "scale_fade") ?: "scale_fade"
        } else {
            animationStyle
        }

        when (if (animationStyle == "custom") effectType else animationStyle) {
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
            // Use the settings launcher instead of startActivity
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
        val sdcard = Environment.getExternalStorageDirectory()
        val watchDir = File(sdcard, "ES-DE/logs")

        fileObserver = object : FileObserver(watchDir, MODIFY or CLOSE_WRITE) {
            private var lastEventTime = 0L

            override fun onEvent(event: Int, path: String?) {
                if (path != null && (path == "esde_game_scroll.txt" || path == "esde_system_scroll.txt")) {
                    // Debounce: ignore events that happen too quickly
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastEventTime < 100) {
                        return
                    }
                    lastEventTime = currentTime

                    runOnUiThread {
                        // Small delay to ensure file is fully written
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val systemScrollFile = File(watchDir, "esde_system_scroll.txt")
                            val gameScrollFile = File(watchDir, "esde_game_scroll.txt")

                            // Determine which mode based on which file was modified
                            when (path) {
                                "esde_system_scroll.txt" -> {
                                    android.util.Log.d("MainActivity", "System scroll detected")
                                    loadSystemImage()
                                }
                                "esde_game_scroll.txt" -> {
                                    android.util.Log.d("MainActivity", "Game scroll detected")
                                    loadGameInfo()
                                }
                            }
                        }, 50) // 50ms delay to ensure file is written
                    }
                }
            }
        }
        fileObserver?.startWatching()
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
    }

    private fun loadSystemImage() {
        try {
            val sdcard = Environment.getExternalStorageDirectory()
            val systemFile = File(sdcard, "ES-DE/logs/esde_system_scroll.txt")
            if (!systemFile.exists()) return

            val systemName = systemFile.readText().trim()
            val imageFile = File(getSystemImagePath(), "$systemName.webp")

            var imageToUse: File? = imageFile
            var marqueeToUse: File? = null

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

                if (imageToUse != null) {
                    val baseName = imageToUse.nameWithoutExtension
                    val marqueeDir = File(mediaBase, "marquees")
                    if (marqueeDir.exists() && marqueeDir.isDirectory) {
                        val matchingMarquees = marqueeDir.listFiles { f ->
                            f.extension.lowercase() in listOf("jpg", "png", "webp") &&
                                    f.nameWithoutExtension == baseName
                        } ?: emptyArray()
                        if (matchingMarquees.isNotEmpty()) {
                            marqueeToUse = matchingMarquees[0]
                        }
                    }
                }
            }

            if (imageToUse != null && imageToUse.exists()) {
                isSystemScrollActive = true
                loadImageWithAnimation(imageToUse, gameImageView)
            } else {
                isSystemScrollActive = false
                Glide.with(this).clear(gameImageView)
                gameImageView.setImageDrawable(null)
            }

            if (marqueeToUse != null && marqueeToUse.exists()) {
                val logoSize = prefs.getString("logo_size", "medium") ?: "medium"
                if (logoSize == "off") {
                    marqueeImageView.visibility = View.GONE
                    Glide.with(this).clear(marqueeImageView)
                    marqueeImageView.setImageDrawable(null)
                } else {
                    marqueeImageView.visibility = View.VISIBLE
                    loadImageWithAnimation(marqueeToUse, marqueeImageView)
                }
            } else {
                val logoSize = prefs.getString("logo_size", "medium") ?: "medium"
                if (logoSize == "off") {
                    marqueeImageView.visibility = View.GONE
                }
                Glide.with(this).clear(marqueeImageView)
                marqueeImageView.setImageDrawable(null)
            }

        } catch (e: Exception) {
            isSystemScrollActive = false
            gameImageView.setImageDrawable(null)
            marqueeImageView.setImageDrawable(null)
        }
    }

    private fun loadGameInfo() {
        isSystemScrollActive = false

        try {
            val sdcard = Environment.getExternalStorageDirectory()
            val gameFile = File(sdcard, "ES-DE/logs/esde_game_scroll.txt")
            if (!gameFile.exists()) return

            val gameNameRaw = gameFile.readText().trim()
            val gameName = gameNameRaw.substringBeforeLast('.')

            val systemFile = File(sdcard, "ES-DE/logs/esde_system.txt")
            if (!systemFile.exists()) return
            val systemName = systemFile.readText().trim()

            val gameImage = findGameImage(sdcard, systemName, gameName, gameNameRaw)
                ?: File(getSystemImagePath(), "$systemName.webp")

            if (!gameImage.exists()) {
                val mediaBase = File(getMediaBasePath(), systemName)
                val imagePref = prefs.getString("image_preference", "fanart") ?: "fanart"
                val prioritizedFolders = if (imagePref == "screenshot") {
                    listOf("screenshots", "fanart")
                } else {
                    listOf("fanart", "screenshots")
                }
                val allImages = mutableListOf<File>()
                prioritizedFolders.forEach { dirName ->
                    val dir = File(mediaBase, dirName)
                    if (dir.exists() && dir.isDirectory) {
                        allImages.addAll(dir.listFiles { f ->
                            f.extension.lowercase() in listOf("jpg", "png", "webp")
                        } ?: emptyArray())
                    }
                }
                if (allImages.isNotEmpty()) {
                    loadImageWithAnimation(allImages.random(), gameImageView)
                } else {
                    Glide.with(this).clear(gameImageView)
                    gameImageView.setImageDrawable(null)
                }
            } else {
                loadImageWithAnimation(gameImage, gameImageView)
            }

            val marqueeFile = findMarqueeImage(sdcard, systemName, gameName, gameNameRaw)
            if (marqueeFile != null && marqueeFile.exists()) {
                val logoSize = prefs.getString("logo_size", "medium") ?: "medium"
                if (logoSize == "off") {
                    marqueeImageView.visibility = View.GONE
                    Glide.with(this).clear(marqueeImageView)
                    marqueeImageView.setImageDrawable(null)
                } else {
                    marqueeImageView.visibility = View.VISIBLE
                    loadImageWithAnimation(marqueeFile, marqueeImageView)
                }
            } else {
                val logoSize = prefs.getString("logo_size", "medium") ?: "medium"
                if (logoSize == "off") {
                    marqueeImageView.visibility = View.GONE
                }
                Glide.with(this).clear(marqueeImageView)
                marqueeImageView.setImageDrawable(null)
            }

        } catch (e: Exception) {
            gameImageView.setImageDrawable(null)
            marqueeImageView.setImageDrawable(null)
        }
    }

    private fun findGameImage(sdcard: File, systemName: String, gameName: String, fullGameName: String): File? {
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

    private fun findMarqueeImage(sdcard: File, systemName: String, gameName: String, fullGameName: String): File? {
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
     * Launch an app on the appropriate display based on user preferences
     */
    private fun launchApp(app: ResolveInfo) {
        val packageName = app.activityInfo?.packageName ?: return
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            // Check if app should launch on top or bottom display
            if (appLaunchPrefs.shouldLaunchOnTop(packageName)) {
                launchOnTopDisplay(launchIntent)
            } else {
                launchOnBottomDisplay(launchIntent)
            }
        }
    }

    /**
     * Launch app on top display (display ID 0)
     */
    private fun launchOnTopDisplay(intent: Intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                android.util.Log.d("MainActivity", "Launching on top display (ID: 0)")
                val options = ActivityOptions.makeBasic()
                options.launchDisplayId = 0  // Top display
                startActivity(intent, options.toBundle())
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error launching on top display, using default", e)
                startActivity(intent)
            }
        } else {
            startActivity(intent)
        }
    }

    /**
     * Launch app on bottom display (display ID 1, or default if not available)
     */
    private fun launchOnBottomDisplay(intent: Intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val displayManager = getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
                val displays = displayManager.displays

                android.util.Log.d("MainActivity", "Available displays: ${displays.size}")
                displays.forEachIndexed { index, display ->
                    android.util.Log.d("MainActivity", "Display $index: ID=${display.displayId}, Name=${display.name}")
                }

                // Try to find the bottom display
                // For dual-screen devices, the second display is usually ID 1
                val bottomDisplay = displays.firstOrNull { it.displayId == 1 }

                if (bottomDisplay != null) {
                    android.util.Log.d("MainActivity", "Launching on bottom display (ID: ${bottomDisplay.displayId})")
                    val options = ActivityOptions.makeBasic()
                    options.launchDisplayId = bottomDisplay.displayId
                    startActivity(intent, options.toBundle())
                } else {
                    // Fallback: if no secondary display, just launch normally
                    android.util.Log.d("MainActivity", "No secondary display found, launching on default display")
                    startActivity(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error launching on bottom display, using default", e)
                // Fallback to default launch
                startActivity(intent)
            }
        } else {
            startActivity(intent)
        }
    }

    /**
     * Show app options dialog with launch position toggles
     */
    private fun showAppOptionsDialog(app: ResolveInfo) {
        val packageName = app.activityInfo?.packageName ?: return
        val appName = app.loadLabel(packageManager).toString()

        // Inflate custom dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_app_options, null)
        val dialogAppName = dialogView.findViewById<TextView>(R.id.dialogAppName)
        val btnAppInfo = dialogView.findViewById<MaterialButton>(R.id.btnAppInfo)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.launchPositionChipGroup)
        val chipLaunchTop = dialogView.findViewById<Chip>(R.id.chipLaunchTop)
        val chipLaunchBottom = dialogView.findViewById<Chip>(R.id.chipLaunchBottom)

        // Set app name
        dialogAppName.text = appName

        // Get current launch position and set initial chip state
        val currentPosition = appLaunchPrefs.getLaunchPosition(packageName)
        if (currentPosition == AppLaunchPreferences.POSITION_TOP) {
            chipLaunchTop.isChecked = true
        } else {
            chipLaunchBottom.isChecked = true
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
                    appLaunchPrefs.setLaunchPosition(packageName, AppLaunchPreferences.POSITION_TOP)
                    android.util.Log.d("MainActivity", "Set $appName to launch on TOP")
                }
                checkedIds.contains(R.id.chipLaunchBottom) -> {
                    appLaunchPrefs.setLaunchPosition(packageName, AppLaunchPreferences.POSITION_BOTTOM)
                    android.util.Log.d("MainActivity", "Set $appName to launch on BOTTOM")
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
}