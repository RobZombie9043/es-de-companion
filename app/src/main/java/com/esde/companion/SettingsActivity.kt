package com.esde.companion

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.ChipGroup
import java.io.File

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat

import com.esde.companion.data.AppConstants
import com.esde.companion.managers.PreferencesManager
import com.esde.companion.managers.ScriptManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefsManager: PreferencesManager
    private lateinit var setupWizardButton: Button
    private lateinit var mediaPathText: TextView
    private lateinit var mediaStatusText: TextView
    private lateinit var mediaStatusDescription: TextView
    private lateinit var selectMediaPathButton: Button
    private lateinit var systemPathText: TextView
    private lateinit var systemLogosPathText: TextView
    private lateinit var selectSystemPathButton: Button
    private lateinit var selectSystemLogosPathButton: Button
    private lateinit var customBackgroundPathText: TextView
    private lateinit var customBackgroundStatusText: TextView
    private lateinit var customBackgroundStatusDescription: TextView
    private lateinit var selectCustomBackgroundButton: Button
    private lateinit var clearCustomBackgroundButton: Button
    private lateinit var scriptsPathText: TextView
    private lateinit var selectScriptsPathButton: Button
    private lateinit var createScriptsButton: Button
    private lateinit var scriptsStatusText: TextView
    private lateinit var scriptsStatusDescription: TextView
    private lateinit var columnCountSeekBar: SeekBar
    private lateinit var columnCountText: TextView
    private lateinit var hideAppsButton: Button
    private lateinit var dimmingSeekBar: SeekBar
    private lateinit var dimmingText: TextView
    private lateinit var blurSeekBar: SeekBar
    private lateinit var blurText: TextView
    private lateinit var drawerTransparencySeekBar: SeekBar
    private lateinit var drawerTransparencyText: TextView
    private lateinit var animationStyleChipGroup: ChipGroup
    private lateinit var systemImagePreferenceChipGroup: ChipGroup
    private lateinit var gameImagePreferenceChipGroup: ChipGroup
    private lateinit var systemColorPickerLayout: LinearLayout
    private lateinit var gameColorPickerLayout: LinearLayout
    private lateinit var systemColorPickerButton: Button
    private lateinit var gameColorPickerButton: Button
    private lateinit var customAnimationSettings: LinearLayout
    private lateinit var animationDurationSeekBar: SeekBar
    private lateinit var animationDurationText: TextView
    private lateinit var animationScaleSeekBar: SeekBar
    private lateinit var animationScaleText: TextView
    private lateinit var versionText: TextView
    private lateinit var videoSupportChipGroup: ChipGroup
    private lateinit var videoSettings: LinearLayout
    private lateinit var videoDelaySeekBar: SeekBar
    private lateinit var videoDelayText: TextView
    private lateinit var videoAudioChipGroup: ChipGroup
    private lateinit var gameLaunchBehaviorChipGroup: ChipGroup
    private lateinit var screensaverBehaviorChipGroup: ChipGroup
    private lateinit var blackOverlayChipGroup: ChipGroup

    // ========== MUSIC INTEGRATION START ==========
    // DELETE THESE PROPERTIES when removing music feature
    private lateinit var musicMasterChipGroup: ChipGroup
    private lateinit var musicSettings: LinearLayout
    private lateinit var musicSystemChipGroup: ChipGroup
    private lateinit var musicGameChipGroup: ChipGroup
    private lateinit var musicScreensaverChipGroup: ChipGroup
    private lateinit var musicVideoChipGroup: ChipGroup
    private lateinit var musicSongTitleChipGroup: ChipGroup
    private lateinit var musicSongTitleDurationSection: LinearLayout
    private lateinit var musicSongTitleDurationSeekBar: SeekBar
    private lateinit var musicSongTitleDurationText: TextView
    private lateinit var musicSongTitleOpacitySeekBar: SeekBar
    private lateinit var musicSongTitleOpacityText: TextView
    private lateinit var musicPathText: TextView
    private lateinit var selectMusicPathButton: Button
    // ========== MUSIC INTEGRATION END ==========

    private var initialDimming: Int = 0
    private var initialBlur: Int = 0
    private var initialDrawerTransparency: Int = 0
    private var videoSettingsChanged: Boolean = false
    private var logoSizeChanged: Boolean = false
    private var mediaPathChanged: Boolean = false
    private var imagePreferenceChanged: Boolean = false
    private var logoTogglesChanged: Boolean = false
    private var gameLaunchBehaviorChanged: Boolean = false
    private var screensaverBehaviorChanged: Boolean = false
    private var customBackgroundChanged: Boolean = false
    // ========== MUSIC INTEGRATION START ==========
    private var musicSettingsChanged: Boolean = false
    private var musicMasterToggleChanged: Boolean = false // Track if master toggle specifically changed
    // ========== MUSIC INTEGRATION END ==========

    private var pathSelectionType = PathSelection.MEDIA

    enum class PathSelection {
        MEDIA, SYSTEM, SCRIPTS, SYSTEM_LOGOS, CUSTOM_BACKGROUND, MUSIC
    }

    private var isInSetupWizard = false
    private var setupStep = 0

    private val directoryPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            android.util.Log.d("SettingsActivity", "Selected URI: $it")
            android.util.Log.d("SettingsActivity", "URI path: ${it.path}")

            // Persist permissions
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // Convert URI to file path
            val path = getPathFromUri(it)
            android.util.Log.d("SettingsActivity", "Converted path: $path")

            when (pathSelectionType) {
                PathSelection.MEDIA -> {
                    prefsManager.mediaPath = path
                    updateMediaPathDisplay()

                    // Mark that media path changed (only if not in wizard)
                    if (!isInSetupWizard) {
                        mediaPathChanged = true
                    }

                    // Warn if path doesn't look like ES-DE downloaded_media folder
                    if (!path.contains(AppConstants.Paths.MEDIA_DIR, ignoreCase = true)) {
                        if (isInSetupWizard) {
                            showNonStandardMediaPathWarningForWizard(path)
                        } else {
                            showNonStandardMediaPathWarning(path)
                        }
                    } else {
                        // Path looks good, continue wizard if active
                        if (isInSetupWizard) {
                            continueSetupWizard()
                        }
                    }
                }
                PathSelection.SYSTEM -> {
                    prefsManager.systemPath = path
                    updateSystemPathDisplay()

                    // Continue wizard if active
                    if (isInSetupWizard) {
                        continueSetupWizard()
                    }
                }
                PathSelection.SCRIPTS -> {
                    prefsManager.scriptsPath = path
                    updateScriptsPathDisplay()

                    // Warn if path doesn't look like ES-DE scripts folder
                    if (!path.contains("ES-DE", ignoreCase = true) ||
    !path.contains(AppConstants.Paths.SCRIPTS_DIR.substringAfterLast('/'), ignoreCase = true)) {
                        if (isInSetupWizard) {
                            showNonStandardScriptsPathWarningForWizard(path)
                        } else {
                            showNonStandardScriptsPathWarning(path)
                        }
                    } else {
                        // Path looks good
                        if (isInSetupWizard) {
                            // In wizard - continue to next step
                            continueSetupWizard()
                        } else {
                            // Not in wizard - user manually changed path
                            // Offer to update scripts first, then verify
                            showScriptsPathChangedUpdatePrompt(path)
                        }
                    }
                }
                PathSelection.SYSTEM_LOGOS -> {
                    prefsManager.systemLogosPath = path
                    systemLogosPathText.text = path
                    Toast.makeText(this, "System logos path updated", Toast.LENGTH_SHORT).show()
                }
                PathSelection.CUSTOM_BACKGROUND -> {
                    // This shouldn't be called since we use imagePicker instead
                    // But need to handle it for exhaustive when
                    android.util.Log.w("SettingsActivity", "directoryPicker called for CUSTOM_BACKGROUND - should use imagePicker instead")
                }
                // ========== MUSIC INTEGRATION START ==========
                PathSelection.MUSIC -> {
                    prefsManager.musicPath = path
                    musicPathText.text = path
                    Toast.makeText(this, "Music path updated", Toast.LENGTH_SHORT).show()
                    android.util.Log.d("SettingsActivity", "Music path set to: $path")
                }
                // ========== MUSIC INTEGRATION END ==========
            }
        }
    }

    private val customBackgroundPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    android.util.Log.d("SettingsActivity", "Custom background selected - URI: $uri")

                    // Try to persist permissions for content:// URIs
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        android.util.Log.d("SettingsActivity", "Persisted read permission for URI")
                    } catch (e: Exception) {
                        android.util.Log.w("SettingsActivity", "Could not persist URI permissions (might be file:// URI)", e)
                    }

                    // Try to get the actual file path
                    val path = try {
                        getPathFromUri(uri)
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsActivity", "getPathFromUri failed", e)
                        null
                    }

                    android.util.Log.d("SettingsActivity", "Converted to path: $path")

                    // Verify file accessibility
                    if (path != null) {
                        val file = File(path)
                        android.util.Log.d("SettingsActivity", "File check - exists: ${file.exists()}, canRead: ${file.canRead()}, isFile: ${file.isFile}")

                        if (file.exists() && file.canRead() && file.isFile) {
                            // File is accessible - save path
                            prefsManager.customBackgroundPath = path
                            updateCustomBackgroundDisplay()
                            customBackgroundChanged = true

                            Toast.makeText(
                                this,
                                "Custom background set successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@let
                        } else {
                            android.util.Log.e("SettingsActivity", "File not accessible: $path")
                        }
                    }

                    // Path extraction or file access failed - show error with helpful message
                    AlertDialog.Builder(this)
                        .setTitle("File Not Accessible")
                        .setMessage("Could not access the selected file.\n\n" +
                                "This might happen if:\n" +
                                "• The file is on external storage without proper permissions\n" +
                                "• The file was moved or deleted\n" +
                                "• The storage location is not mounted\n\n" +
                                "Try:\n" +
                                "• Selecting a file from internal storage (/storage/emulated/0/)\n" +
                                "• Copying the image to your Pictures or Downloads folder first\n" +
                                "• Granting storage permissions in Android Settings")
                        .setPositiveButton("Try Again") { _, _ ->
                            // Let user try selecting again
                            selectCustomBackgroundButton.performClick()
                        }
                        .setNegativeButton("Cancel", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()

                } catch (e: Exception) {
                    android.util.Log.e("SettingsActivity", "Error setting custom background", e)
                    Toast.makeText(
                        this,
                        "Error accessing file: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showNonStandardMediaPathWarning(selectedPath: String) {
        AlertDialog.Builder(this)
            .setTitle("Non-Standard Path")
            .setMessage("Warning: The selected path doesn't appear to be the ES-DE downloaded_media folder.\n\n" +
                    "Selected: $selectedPath\n\n" +
                    "Expected path should contain:\n" +
                    "• downloaded_media\n\n" +
                    "The app may not find game artwork if it's not in the correct location.\n\n" +
                    "Continue anyway?")
            .setPositiveButton("Continue") { _, _ ->
                // User confirmed, do nothing
            }
            .setNegativeButton("Choose Again") { _, _ ->
                // Let user pick again
                pathSelectionType = PathSelection.MEDIA
                directoryPicker.launch(null)
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun showNonStandardMediaPathWarningForWizard(selectedPath: String) {
        AlertDialog.Builder(this)
            .setTitle("Non-Standard Path")
            .setMessage("Warning: The selected path doesn't appear to be the ES-DE downloaded_media folder.\n\n" +
                    "Selected: $selectedPath\n\n" +
                    "Expected path should contain:\n" +
                    "• downloaded_media\n\n" +
                    "The app may not find game artwork if it's not in the correct location.\n\n" +
                    "Continue anyway?")
            .setPositiveButton("Continue") { _, _ ->
                // Continue wizard
                continueSetupWizard()
            }
            .setNegativeButton("Choose Again") { _, _ ->
                // Let user pick again - don't advance wizard
                pathSelectionType = PathSelection.MEDIA
                directoryPicker.launch(null)
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setCancelable(false)
            .show()
    }

    private fun showNonStandardScriptsPathWarning(selectedPath: String) {
        AlertDialog.Builder(this)
            .setTitle("Non-Standard Path")
            .setMessage("Warning: The selected path doesn't appear to be the ES-DE scripts folder.\n\n" +
                    "Selected: $selectedPath\n\n" +
                    "Expected path should contain:\n" +
                    "• ES-DE\n" +
                    "• scripts\n\n" +
                    "ES-DE may not find the scripts if they're not in the correct location.\n\n" +
                    "Continue anyway?")
            .setPositiveButton("Continue") { _, _ ->
                // User confirmed - offer update and verification
                showScriptsPathChangedUpdatePrompt(selectedPath)
            }
            .setNegativeButton("Choose Again") { _, _ ->
                // Let user pick again
                pathSelectionType = PathSelection.SCRIPTS
                directoryPicker.launch(null)
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    // No longer needed for Android 13+ - MANAGE_EXTERNAL_STORAGE is granted via system settings
    // Kept for potential notification permission requests
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("SettingsActivity", "Notification permission granted: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_settings)
            android.util.Log.d("SettingsActivity", "Layout inflated successfully")

            prefsManager = PreferencesManager(this)
            android.util.Log.d("SettingsActivity", "Prefs initialized")

            // Store initial values
            initialDimming = prefsManager.dimmingLevel
            initialBlur = prefsManager.blurLevel
            initialDrawerTransparency = prefsManager.drawerTransparency

            // Initialize all views
            initializeViews()

            // Setup all UI components
            setupAllUIComponents()

            // Update all path displays
            updateAllPathDisplays()

            // Handle wizard auto-start
            handleWizardAutoStart()

            // Setup back button handler
            setupBackButtonHandler()

            android.util.Log.d("SettingsActivity", "onCreate completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error in onCreate", e)
            e.printStackTrace()
            finish()
        }
    }

    /**
     * Initialize all view references
     */
    private fun initializeViews() {
        setupWizardButton = findViewById(R.id.setupWizardButton)
        mediaPathText = findViewById(R.id.mediaPathText)
        mediaStatusText = findViewById(R.id.mediaStatusText)
        mediaStatusDescription = findViewById(R.id.mediaStatusDescription)
        selectMediaPathButton = findViewById(R.id.selectMediaPathButton)
        systemPathText = findViewById(R.id.systemPathText)
        systemLogosPathText = findViewById(R.id.systemLogosPathText)
        selectSystemPathButton = findViewById(R.id.selectSystemPathButton)
        selectSystemLogosPathButton = findViewById(R.id.selectSystemLogosPathButton)
        customBackgroundPathText = findViewById(R.id.customBackgroundPathText)
        customBackgroundStatusText = findViewById(R.id.customBackgroundStatusText)
        customBackgroundStatusDescription = findViewById(R.id.customBackgroundStatusDescription)
        selectCustomBackgroundButton = findViewById(R.id.selectCustomBackgroundButton)
        clearCustomBackgroundButton = findViewById(R.id.clearCustomBackgroundButton)
        scriptsPathText = findViewById(R.id.scriptsPathText)
        selectScriptsPathButton = findViewById(R.id.selectScriptsPathButton)
        createScriptsButton = findViewById(R.id.createScriptsButton)
        scriptsStatusText = findViewById(R.id.scriptsStatusText)
        scriptsStatusDescription = findViewById(R.id.scriptsStatusDescription)
        columnCountSeekBar = findViewById(R.id.columnCountSeekBar)
        columnCountText = findViewById(R.id.columnCountText)
        hideAppsButton = findViewById(R.id.hideAppsButton)
        dimmingSeekBar = findViewById(R.id.dimmingSeekBar)
        dimmingText = findViewById(R.id.dimmingText)
        blurSeekBar = findViewById(R.id.blurSeekBar)
        blurText = findViewById(R.id.blurText)
        drawerTransparencySeekBar = findViewById(R.id.drawerTransparencySeekBar)
        drawerTransparencyText = findViewById(R.id.drawerTransparencyText)
        animationStyleChipGroup = findViewById(R.id.animationStyleChipGroup)
        customAnimationSettings = findViewById(R.id.customAnimationSettings)
        animationDurationSeekBar = findViewById(R.id.animationDurationSeekBar)
        animationDurationText = findViewById(R.id.animationDurationText)
        animationScaleSeekBar = findViewById(R.id.animationScaleSeekBar)
        animationScaleText = findViewById(R.id.animationScaleText)
        systemImagePreferenceChipGroup = findViewById(R.id.systemImagePreferenceChipGroup)
        gameImagePreferenceChipGroup = findViewById(R.id.gameImagePreferenceChipGroup)
        systemColorPickerLayout = findViewById(R.id.systemColorPickerLayout)
        gameColorPickerLayout = findViewById(R.id.gameColorPickerLayout)
        systemColorPickerButton = findViewById(R.id.systemColorPickerButton)
        gameColorPickerButton = findViewById(R.id.gameColorPickerButton)
        videoSupportChipGroup = findViewById(R.id.videoSupportChipGroup)
        videoSettings = findViewById(R.id.videoSettings)
        videoDelaySeekBar = findViewById(R.id.videoDelaySeekBar)
        videoDelayText = findViewById(R.id.videoDelayText)
        videoAudioChipGroup = findViewById(R.id.videoAudioChipGroup)
        gameLaunchBehaviorChipGroup = findViewById(R.id.gameLaunchBehaviorChipGroup)
        screensaverBehaviorChipGroup = findViewById(R.id.screensaverBehaviorChipGroup)
        blackOverlayChipGroup = findViewById(R.id.blackOverlayChipGroup)
        versionText = findViewById(R.id.versionText)
        // ========== MUSIC INTEGRATION START ==========
        if (FeatureFlags.ENABLE_BACKGROUND_MUSIC) {
            musicMasterChipGroup = findViewById(R.id.musicMasterChipGroup)
            musicSettings = findViewById(R.id.musicSettings)
            musicSystemChipGroup = findViewById(R.id.musicSystemChipGroup)
            musicGameChipGroup = findViewById(R.id.musicGameChipGroup)
            musicScreensaverChipGroup = findViewById(R.id.musicScreensaverChipGroup)
            musicVideoChipGroup = findViewById(R.id.musicVideoChipGroup)
            musicSongTitleChipGroup = findViewById(R.id.musicSongTitleChipGroup)
            musicSongTitleDurationSection = findViewById(R.id.musicSongTitleDurationSection)
            musicSongTitleDurationSeekBar = findViewById(R.id.musicSongTitleDurationSeekBar)
            musicSongTitleDurationText = findViewById(R.id.musicSongTitleDurationText)
            musicSongTitleOpacitySeekBar = findViewById(R.id.musicSongTitleOpacitySeekBar)
            musicSongTitleOpacityText = findViewById(R.id.musicSongTitleOpacityText)
            musicPathText = findViewById(R.id.musicPathText)
            selectMusicPathButton = findViewById(R.id.selectMusicPathButton)
        }
        // ========== MUSIC INTEGRATION END ==========
    }

    /**
     * Setup all UI components and listeners
     */
    private fun setupAllUIComponents() {
        android.util.Log.d("SettingsActivity", "Setting up UI components")

        setupSwipeGesture()
        setupWizardButton.setOnClickListener { startSetupWizard() }
        hideAppsButton.setOnClickListener { showHideAppsDialog() }

        selectMediaPathButton.setOnClickListener {
            pathSelectionType = PathSelection.MEDIA
            directoryPicker.launch(null)
        }

        selectSystemPathButton.setOnClickListener {
            pathSelectionType = PathSelection.SYSTEM
            directoryPicker.launch(null)
        }

        selectSystemLogosPathButton.setOnClickListener {
            pathSelectionType = PathSelection.SYSTEM_LOGOS
            directoryPicker.launch(null)
        }

        selectScriptsPathButton.setOnClickListener {
            pathSelectionType = PathSelection.SCRIPTS
            directoryPicker.launch(null)
        }

        createScriptsButton.setOnClickListener {
            createScriptFiles()
        }

        selectCustomBackgroundButton.setOnClickListener {
            pathSelectionType = PathSelection.CUSTOM_BACKGROUND
            // Create intent for direct file picker (like directory picker UI)
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                // Optional: Allow multiple MIME types
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/webp"))
            }
            customBackgroundPicker.launch(intent)
        }

        // ========== MUSIC INTEGRATION START ==========
        selectMusicPathButton.setOnClickListener {
            pathSelectionType = PathSelection.MUSIC
            directoryPicker.launch(null)
        }
        // ========== MUSIC INTEGRATION END ==========

        clearCustomBackgroundButton.setOnClickListener {
            // Clear the custom background using the correct key
            prefsManager.customBackgroundPath = ""  // or null if property is nullable

            // Update display immediately
            updateCustomBackgroundDisplay()

            // Mark that background changed so MainActivity reloads on return
            customBackgroundChanged = true

            Toast.makeText(this, "Custom background cleared", Toast.LENGTH_SHORT).show()

            android.util.Log.d("SettingsActivity", "Custom background cleared - key: $CUSTOM_BACKGROUND_KEY")
        }

        setupColumnCountSlider()
        setupDimmingSlider()
        setupBlurSlider()
        setupDrawerTransparencySlider()
        setupAnimationStyleChips()
        setupCustomAnimationControls()
        setupImagePreferenceChips()
        setupVideoSettings()
        setupGameLaunchBehavior()
        setupScreensaverBehavior()
        setupBlackOverlay()

        // Set version text dynamically
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName ?: "Unknown"
            versionText.text = "ES-DE Companion v$versionName"
            android.util.Log.d("SettingsActivity", "Version text set to: $versionName")
        } catch (e: Exception) {
            versionText.text = "ES-DE Companion"
            android.util.Log.e("SettingsActivity", "Failed to get version name", e)
        }

        android.util.Log.d("SettingsActivity", "All UI components setup complete")

        // ========== MUSIC INTEGRATION START ==========
        if (FeatureFlags.ENABLE_BACKGROUND_MUSIC) {
            setupMusicSettings()
        }
        // ========== MUSIC INTEGRATION END ==========
    }

    /**
     * Update all path displays
     */
    private fun updateAllPathDisplays() {
        updateMediaPathDisplay()
        updateSystemPathDisplay()
        updateScriptsPathDisplay()
        updateCustomBackgroundDisplay()
        updateMusicPathDisplay()    // ========== MUSIC INTEGRATION
    }

    /**
     * Check if wizard should auto-start and handle accordingly
     */
    private fun handleWizardAutoStart() {
        val autoStartWizard = intent.getBooleanExtra("AUTO_START_WIZARD", false)

        if (autoStartWizard) {
            // Start wizard immediately
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!isInSetupWizard) {
                    startSetupWizard()
                }
            }, AppConstants.Timing.WIZARD_DELAY)
        } else {
            // Normal auto-start check (for when settings opened directly)
            checkAndAutoStartWizard()
        }
    }

    /**
     * Setup back button handler with state change tracking
     */
    private fun setupBackButtonHandler() {
        val initialHiddenApps = prefsManager.hiddenApps

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check all changed states
                val currentDimming = prefsManager.dimmingLevel
                val currentBlur = prefsManager.blurLevel
                val currentDrawerTransparency = prefsManager.drawerTransparency
                val currentHiddenApps = prefsManager.hiddenApps

                val intent = Intent()

                // Build intent with all state change flags
                if (currentDimming != initialDimming || currentBlur != initialBlur ||
                    currentDrawerTransparency != initialDrawerTransparency) {
                    intent.putExtra("NEEDS_RECREATE", true)
                }
                if (currentHiddenApps != initialHiddenApps) {
                    intent.putExtra("APPS_HIDDEN_CHANGED", true)
                }
                // ========== MUSIC ==========
                if (musicSettingsChanged) {
                    intent.putExtra("MUSIC_SETTINGS_CHANGED", true)
                }
                if (musicMasterToggleChanged) {
                    intent.putExtra("MUSIC_MASTER_TOGGLE_CHANGED", true)
                }
                // ===========================
                if (videoSettingsChanged) {
                    intent.putExtra("VIDEO_SETTINGS_CHANGED", true)
                }
                if (logoSizeChanged) {
                    intent.putExtra("LOGO_SIZE_CHANGED", true)
                }
                if (mediaPathChanged) {
                    intent.putExtra("MEDIA_PATH_CHANGED", true)
                }
                if (imagePreferenceChanged) {
                    intent.putExtra("IMAGE_PREFERENCE_CHANGED", true)
                }
                if (logoTogglesChanged) {
                    intent.putExtra("LOGO_TOGGLES_CHANGED", true)
                }
                if (gameLaunchBehaviorChanged) {
                    intent.putExtra("GAME_LAUNCH_BEHAVIOR_CHANGED", true)
                }
                if (screensaverBehaviorChanged) {
                    intent.putExtra("SCREENSAVER_BEHAVIOR_CHANGED", true)
                }
                if (customBackgroundChanged) {
                    intent.putExtra("CUSTOM_BACKGROUND_CHANGED", true)
                }

                intent.putExtra("CLOSE_DRAWER", true)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()

        // If we're in quick setup at step 1, check if permission was just granted
        if (isInSetupWizard && setupStep == 1) {
            val hasPermission = Environment.isExternalStorageManager()

            if (hasPermission) {
                // Permission granted, continue wizard automatically
                continueSetupWizard()
            }
        }
    }

    private fun setupColumnCountSlider() {
        val currentColumns = prefsManager.columnCount
        columnCountSeekBar.min = AppConstants.UI.MIN_COLUMN_COUNT
        columnCountSeekBar.max = AppConstants.UI.MAX_COLUMN_COUNT
        columnCountSeekBar.progress = currentColumns
        columnCountText.text = "$currentColumns"

        columnCountSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                columnCountText.text = "$progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    prefsManager.columnCount = it.progress
                }
            }
        })
    }

    private fun setupDimmingSlider() {
        val currentDimming = prefsManager.dimmingLevel
        dimmingSeekBar.min = 0
        dimmingSeekBar.max = 20  // 0-20 range = 0-100% in 5% increments
        dimmingSeekBar.progress = currentDimming / 5
        dimmingText.text = "$currentDimming%"

        dimmingSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percentage = progress * 5
                dimmingText.text = "$percentage%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val percentage = it.progress * 5
                    prefsManager.dimmingLevel = percentage
                }
            }
        })
    }

    private fun setupBlurSlider() {
        val currentBlur = prefsManager.blurLevel
        blurSeekBar.min = 0
        blurSeekBar.max = 25
        blurSeekBar.progress = currentBlur
        blurText.text = if (currentBlur == 0) "Off" else "$currentBlur"

        blurSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                blurText.text = if (progress == 0) "Off" else "$progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    prefsManager.blurLevel = it.progress
                }
            }
        })
    }

    private fun setupDrawerTransparencySlider() {
        val currentTransparency = prefsManager.drawerTransparency
        drawerTransparencySeekBar.min = 0
        drawerTransparencySeekBar.max = 20  // 0-20 range = 0-100% in 5% increments
        drawerTransparencySeekBar.progress = currentTransparency / 5
        drawerTransparencyText.text = "$currentTransparency%"

        drawerTransparencySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percentage = progress * 5
                drawerTransparencyText.text = "$percentage%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val percentage = it.progress * 5
                    prefsManager.drawerTransparency = percentage
                }
            }
        })
    }

    private fun setupAnimationStyleChips() {
        val currentStyle = prefsManager.animationStyle

        val chipToCheck = when (currentStyle) {
            "none" -> R.id.animationNone
            "fade" -> R.id.animationFade
            "scale_fade" -> R.id.animationScaleFade
            "custom" -> R.id.animationCustom
            else -> R.id.animationScaleFade
        }
        animationStyleChipGroup.check(chipToCheck)

        // Show/hide custom settings based on initial selection
        if (currentStyle == "custom") {
            customAnimationSettings.visibility = View.VISIBLE
        } else {
            customAnimationSettings.visibility = View.GONE
        }

        animationStyleChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val style = when (checkedIds[0]) {
                    R.id.animationNone -> "none"
                    R.id.animationFade -> "fade"
                    R.id.animationScaleFade -> "scale_fade"
                    R.id.animationCustom -> "custom"
                    else -> "scale_fade"
                }
                prefsManager.animationStyle = style

                // Show/hide custom settings with animation
                if (style == "custom") {
                    customAnimationSettings.visibility = View.VISIBLE
                    customAnimationSettings.alpha = 0f
                    customAnimationSettings.animate()
                        .alpha(1f)
                        .setDuration(AppConstants.Timing.SLIDE_ANIMATION_DURATION)
                        .start()
                } else {
                    customAnimationSettings.animate()
                        .alpha(0f)
                        .setDuration(AppConstants.Timing.SLIDE_ANIMATION_DURATION)
                        .withEndAction {
                            customAnimationSettings.visibility = View.GONE
                        }
                        .start()
                }
            }
        }
    }

    private fun setupCustomAnimationControls() {
        // Duration Slider (100ms - 500ms, in 10ms steps)
        val currentDuration = prefsManager.animationDuration
        val seekBarMax = (AppConstants.Timing.ANIMATION_DURATION_MAX -
                AppConstants.Timing.ANIMATION_DURATION_MIN) /
                AppConstants.Timing.ANIMATION_DURATION_STEP
        animationDurationSeekBar.max = seekBarMax
        animationDurationSeekBar.progress = (currentDuration - AppConstants.Timing.ANIMATION_DURATION_MIN) /
                AppConstants.Timing.ANIMATION_DURATION_STEP
        animationDurationText.text = "${currentDuration}ms"

        animationDurationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val duration = AppConstants.Timing.ANIMATION_DURATION_MIN +
                        (progress * AppConstants.Timing.ANIMATION_DURATION_STEP)
                animationDurationText.text = "${duration}ms"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val duration = 100 + (it.progress * 10)
                    prefsManager.animationDuration = duration
                }
            }
        })

        // Scale Amount Slider (85% - 100%, in 1% steps)
        val currentScale = prefsManager.animationScale
        animationScaleSeekBar.max = AppConstants.UI.ANIMATION_SCALE_MAX - AppConstants.UI.ANIMATION_SCALE_MIN  // 100 - 85 = 15
        animationScaleSeekBar.progress = currentScale - AppConstants.UI.ANIMATION_SCALE_MIN  // currentScale - 85
        animationScaleText.text = "${currentScale}%"

        animationScaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = AppConstants.UI.ANIMATION_SCALE_MIN + progress  // 85 + progress (0-15)
                animationScaleText.text = "${scale}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val scale = AppConstants.UI.ANIMATION_SCALE_MIN + it.progress  // 85 + progress
                    prefsManager.animationScale = scale
                }
            }
        })
    }

    private fun setupImagePreferenceChips() {

        // Setup System View Background Priority
        val systemPref = prefsManager.systemViewBackgroundType
        val systemChipToCheck = when (systemPref) {
            "screenshot" -> R.id.systemImagePrefScreenshot
            "solid_color" -> R.id.systemImagePrefSolidColor
            "custom_image" -> R.id.systemImagePrefCustom
            else -> R.id.systemImagePrefFanart
        }
        systemImagePreferenceChipGroup.check(systemChipToCheck)

        // Show/hide system color picker based on selection
        systemColorPickerLayout.visibility =
            if (systemPref == "solid_color") View.VISIBLE else View.GONE

        systemImagePreferenceChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val preference = when (checkedIds[0]) {
                    R.id.systemImagePrefScreenshot -> "screenshot"
                    R.id.systemImagePrefSolidColor -> "solid_color"
                    R.id.systemImagePrefCustom -> "custom_image"
                    R.id.systemImagePrefFanart -> "fanart"
                    else -> "fanart"
                }
                prefsManager.systemViewBackgroundType = preference

                // Show/hide color picker
                systemColorPickerLayout.visibility =
                    if (preference == "solid_color") View.VISIBLE else View.GONE

                imagePreferenceChanged = true
            }
        }

        // Setup system color picker button
        val systemColor = prefsManager.systemBackgroundColor
        systemColorPickerButton.setBackgroundColor(systemColor)

        systemColorPickerButton.setOnClickListener {
            // Get current color from prefs (in case it was changed since last read)
            val currentSystemColor = prefsManager.systemBackgroundColor
            showColorPicker(currentSystemColor, "System View Background Color") { selectedColor ->
                prefsManager.systemBackgroundColor = selectedColor
                systemColorPickerButton.setBackgroundColor(selectedColor)
                imagePreferenceChanged = true
            }
        }

        // Setup Game View Background Priority
        val gamePref = prefsManager.gameViewBackgroundType
        val gameChipToCheck = when (gamePref) {
            "screenshot" -> R.id.gameImagePrefScreenshot
            "solid_color" -> R.id.gameImagePrefSolidColor
            "custom_image" -> R.id.gameImagePrefCustom
            else -> R.id.gameImagePrefFanart
        }
        gameImagePreferenceChipGroup.check(gameChipToCheck)

        // Show/hide game color picker based on selection
        gameColorPickerLayout.visibility =
            if (gamePref == "solid_color") View.VISIBLE else View.GONE

        gameImagePreferenceChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val preference = when (checkedIds[0]) {
                    R.id.gameImagePrefScreenshot -> "screenshot"
                    R.id.gameImagePrefSolidColor -> "solid_color"
                    R.id.gameImagePrefCustom -> "custom_image"
                    R.id.gameImagePrefFanart -> "fanart"
                    else -> "fanart"
                }
                prefsManager.gameViewBackgroundType = preference

                // DEBUG: Verify preference was saved
                val savedPref = prefsManager.gameViewBackgroundType
                android.util.Log.d("SettingsActivity", "━━━ IMAGE PREFERENCE DEBUG ━━━")
                android.util.Log.d("SettingsActivity", "Chip ID selected: ${checkedIds[0]}")
                android.util.Log.d("SettingsActivity", "Preference value to save: $preference")
                android.util.Log.d("SettingsActivity", "Preference key: $GAME_IMAGE_PREFERENCE_KEY")
                android.util.Log.d("SettingsActivity", "Saved preference readback: $savedPref")
                android.util.Log.d("SettingsActivity", "━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                // Show/hide color picker
                gameColorPickerLayout.visibility =
                    if (preference == "solid_color") View.VISIBLE else View.GONE

                imagePreferenceChanged = true
            }
        }

        // Setup game color picker button
        val gameColor = prefsManager.gameBackgroundColor
        gameColorPickerButton.setBackgroundColor(gameColor)

        gameColorPickerButton.setOnClickListener {
            // Get current color from prefs (in case it was changed since last read)
            val currentGameColor = prefsManager.gameBackgroundColor
            showColorPicker(currentGameColor, "Game View Background Color") { selectedColor ->
                prefsManager.gameBackgroundColor = selectedColor
                gameColorPickerButton.setBackgroundColor(selectedColor)
                imagePreferenceChanged = true
            }
        }
    }

    private fun showColorPicker(currentColor: Int, title: String, onColorSelected: (Int) -> Unit) {
        val colors = arrayOf(
            "#000000", "#1A1A1A", "#2D2D2D", "#424242", "#616161", "#757575", "#9E9E9E",
            "#B71C1C", "#C62828", "#D32F2F", "#E53935", "#F44336", "#EF5350", "#E57373",
            "#880E4F", "#AD1457", "#C2185B", "#D81B60", "#E91E63", "#EC407A", "#F06292",
            "#4A148C", "#6A1B9A", "#7B1FA2", "#8E24AA", "#9C27B0", "#AB47BC", "#BA68C8",
            "#311B92", "#4527A0", "#512DA8", "#5E35B1", "#673AB7", "#7E57C2", "#9575CD",
            "#1A237E", "#283593", "#303F9F", "#3949AB", "#3F51B5", "#5C6BC0", "#7986CB",
            "#0D47A1", "#1565C0", "#1976D2", "#1E88E5", "#2196F3", "#42A5F5", "#64B5F6",
            "#01579B", "#0277BD", "#0288D1", "#039BE5", "#03A9F4", "#29B6F6", "#4FC3F7",
            "#006064", "#00838F", "#0097A7", "#00ACC1", "#00BCD4", "#26C6DA", "#4DD0E1",
            "#004D40", "#00695C", "#00796B", "#00897B", "#009688", "#26A69A", "#4DB6AC",
            "#1B5E20", "#2E7D32", "#388E3C", "#43A047", "#4CAF50", "#66BB6A", "#81C784",
            "#33691E", "#558B2F", "#689F38", "#7CB342", "#8BC34A", "#9CCC65", "#AED581",
            "#827717", "#9E9D24", "#AFB42B", "#C0CA33", "#CDDC39", "#D4E157", "#DCE775",
            "#F57F17", "#F9A825", "#FBC02D", "#FDD835", "#FFEB3B", "#FFEE58", "#FFF176",
            "#FF6F00", "#FF8F00", "#FFA000", "#FFB300", "#FFC107", "#FFCA28", "#FFD54F",
            "#E65100", "#EF6C00", "#F57C00", "#FB8C00", "#FF9800", "#FFA726", "#FFB74D",
            "#BF360C", "#D84315", "#E64A19", "#F4511E", "#FF5722", "#FF7043", "#FF8A65",
            "#3E2723", "#4E342E", "#5D4037", "#6D4C41", "#795548", "#8D6E63", "#A1887F",
            "#263238", "#37474F", "#455A64", "#546E7A", "#607D8B", "#78909C", "#90A4AE",
            "#FFFFFF", "#FAFAFA", "#F5F5F5", "#EEEEEE", "#E0E0E0", "#BDBDBD", "#9E9E9E"
        )

        val colorInts = colors.map { android.graphics.Color.parseColor(it) }.toIntArray()

        // Create main container layout
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        // Add hex input section
        val hexInputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val marginBottom = (12 * resources.displayMetrics.density).toInt()
            setPadding(0, 0, 0, marginBottom)
        }

        val hexInputLabel = TextView(this).apply {
            text = "Hex: "
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            val padding = (8 * resources.displayMetrics.density).toInt()
            setPadding(0, padding, padding, padding)
        }

        val hexInput = android.widget.EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            hint = "Enter hex color (e.g., #FF5722)"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.parseColor("#999999"))
            setBackgroundColor(android.graphics.Color.parseColor("#2D2D2D"))
            val padding = (12 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)

            // Pre-fill with current color - READ FROM PREFS to get latest saved value
            val latestColor = when(title) {
                "System View Background Color" -> prefsManager.systemBackgroundColor
                "Game View Background Color" -> prefsManager.gameBackgroundColor
                else -> currentColor
            }
            val currentHex = String.format("#%06X", 0xFFFFFF and latestColor)
            setText(currentHex)
            setSelection(text.length) // Move cursor to end
        }

        hexInputLayout.addView(hexInputLabel)
        hexInputLayout.addView(hexInput)
        mainLayout.addView(hexInputLayout)

        // Add color grid with proper padding to prevent clipping
        val gridView = android.widget.GridView(this).apply {
            numColumns = 7
            val spacing = (8 * resources.displayMetrics.density).toInt()
            verticalSpacing = spacing
            horizontalSpacing = spacing

            // CRITICAL: Prevent clipping by adding extra padding for selection border
            clipToPadding = false
            clipChildren = false
            val extraPadding = (12 * resources.displayMetrics.density).toInt()
            setPadding(extraPadding, 0, extraPadding, 0)
        }

        // Get the latest color from prefs for proper initial selection
        val initialSelectedColor = when(title) {
            "System View Background Color" -> prefsManager.systemBackgroundColor
            "Game View Background Color" -> prefsManager.gameBackgroundColor
            else -> currentColor
        }

        val adapter = object : android.widget.BaseAdapter() {
            private var selectedPosition = colorInts.indexOf(initialSelectedColor)

            override fun getCount() = colorInts.size
            override fun getItem(position: Int) = colorInts[position]
            override fun getItemId(position: Int) = position.toLong()

            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup?): android.view.View {
                val container = convertView as? android.widget.FrameLayout ?: android.widget.FrameLayout(this@SettingsActivity)
                container.removeAllViews()

                val size = (48 * resources.displayMetrics.density).toInt()
                container.layoutParams = android.widget.AbsListView.LayoutParams(size, size)

                // Add inner padding to prevent border clipping
                val innerPadding = (6 * resources.displayMetrics.density).toInt()
                container.setPadding(innerPadding, innerPadding, innerPadding, innerPadding)

                // Create color square
                val colorSquare = android.view.View(this@SettingsActivity).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    val border = android.graphics.drawable.GradientDrawable()
                    border.setColor(colorInts[position])
                    border.setStroke(
                        (2 * resources.displayMetrics.density).toInt(),
                        android.graphics.Color.parseColor("#666666")
                    )
                    background = border
                }

                container.addView(colorSquare)

                // Add selection indicator if this is the selected color
                if (position == selectedPosition) {
                    val selectionBorder = android.view.View(this@SettingsActivity).apply {
                        // Make it slightly smaller than container to ensure visibility
                        val margin = (-2 * resources.displayMetrics.density).toInt()
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        ).apply {
                            setMargins(margin, margin, margin, margin)
                        }
                        val border = android.graphics.drawable.GradientDrawable()
                        border.setColor(android.graphics.Color.TRANSPARENT)
                        border.setStroke(
                            (4 * resources.displayMetrics.density).toInt(),
                            android.graphics.Color.WHITE
                        )
                        background = border
                    }
                    container.addView(selectionBorder)
                }

                return container
            }

            fun updateSelection(position: Int) {
                selectedPosition = position
                notifyDataSetChanged()
            }
        }

        gridView.adapter = adapter
        mainLayout.addView(gridView)

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(mainLayout)
            .setPositiveButton("OK") { _, _ ->
                // Try to parse hex input first
                val hexText = hexInput.text.toString().trim()
                val finalColor = try {
                    if (hexText.startsWith("#") && hexText.length == 7) {
                        android.graphics.Color.parseColor(hexText)
                    } else if (hexText.length == 6) {
                        android.graphics.Color.parseColor("#$hexText")
                    } else {
                        initialSelectedColor // Invalid format, use current from prefs
                    }
                } catch (e: Exception) {
                    // If parsing fails, use the current color from prefs
                    initialSelectedColor
                }
                onColorSelected(finalColor)
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Update hex input when grid color is selected
        gridView.setOnItemClickListener { _, _, position, _ ->
            val selectedColor = colorInts[position]
            val hexString = String.format("#%06X", 0xFFFFFF and selectedColor)
            hexInput.setText(hexString)
            hexInput.setSelection(hexString.length)
            adapter.updateSelection(position)
        }

        // Update grid selection when hex is manually entered
        hexInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val hexText = s.toString().trim()
                try {
                    val parsedColor = if (hexText.startsWith("#") && hexText.length == 7) {
                        android.graphics.Color.parseColor(hexText)
                    } else if (hexText.length == 6) {
                        android.graphics.Color.parseColor("#$hexText")
                    } else {
                        return // Invalid length, don't update
                    }

                    // Find if this color exists in the grid
                    val position = colorInts.indexOf(parsedColor)
                    if (position >= 0) {
                        adapter.updateSelection(position)
                        // Scroll to show the selected color
                        gridView.smoothScrollToPosition(position)
                    } else {
                        // Color not in grid, clear selection
                        adapter.updateSelection(-1)
                    }
                } catch (e: Exception) {
                    // Invalid color format, ignore
                }
            }
        })

        dialog.show()
    }

    private fun setupVideoSettings() {
        // Load saved video enabled state (default: false/off)
        val videoEnabled = prefsManager.videoEnabled

        // Set initial chip selection
        val chipToCheck = if (videoEnabled) R.id.videoOn else R.id.videoOff
        videoSupportChipGroup.check(chipToCheck)

        // Show/hide video settings box based on initial state
        videoSettings.visibility = if (videoEnabled) View.VISIBLE else View.GONE

        // Setup video on/off listener
        videoSupportChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val enabled = checkedIds[0] == R.id.videoOn
                prefsManager.videoEnabled = enabled

                // Show/hide video settings box
                videoSettings.visibility = if (enabled) View.VISIBLE else View.GONE

                // Mark that video settings changed
                videoSettingsChanged = true
            }
        }

        // Setup video delay slider (0-5 seconds in 0.5s increments = 0-10 on seekbar)
        val savedDelay = prefsManager.videoDelay
        videoDelaySeekBar.min = AppConstants.UI.VIDEO_DELAY_MIN
        videoDelaySeekBar.max = AppConstants.UI.VIDEO_DELAY_MAX
        videoDelaySeekBar.progress = savedDelay
        updateVideoDelayText(savedDelay)

        videoDelaySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateVideoDelayText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    prefsManager.videoDelay = it.progress
                    // Mark that video settings changed
                    videoSettingsChanged = true
                }
            }
        })

        // Setup video audio chips
        val audioEnabled = prefsManager.videoAudioEnabled
        val audioChipToCheck = if (audioEnabled) R.id.videoAudioOn else R.id.videoAudioOff
        videoAudioChipGroup.check(audioChipToCheck)

        videoAudioChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val enabled = checkedIds[0] == R.id.videoAudioOn
                prefsManager.videoAudioEnabled = enabled
                // Don't mark as changed - audio is handled by onResume
            }
        }
    }

    private fun updateVideoDelayText(progress: Int) {
        val delaySeconds = progress * 0.5f
        videoDelayText.text = if (progress == 0) {
            "Instant"
        } else {
            String.format("%.1fs", delaySeconds)
        }
    }

    private fun setupGameLaunchBehavior() {
        // Load saved game launch behavior (default: "game_image")
        val gameLaunchBehavior = prefsManager.gameLaunchBehavior

        // Set initial chip selection
        val chipToCheck = when (gameLaunchBehavior) {
            "game_image" -> R.id.gameLaunchGameImage
            "black_screen" -> R.id.gameLaunchBlackScreen
            else -> R.id.gameLaunchDefaultImage
        }
        gameLaunchBehaviorChipGroup.check(chipToCheck)

        // Setup listener
        gameLaunchBehaviorChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val behavior = when (checkedIds[0]) {
                    R.id.gameLaunchGameImage -> "game_image"
                    R.id.gameLaunchBlackScreen -> "black_screen"
                    else -> "default_image"
                }
                prefsManager.gameLaunchBehavior = behavior
                // Mark as changed
                gameLaunchBehaviorChanged = true
            }
        }
    }

    private fun setupScreensaverBehavior() {
        // Load saved screensaver behavior (default: "game_image")
        val screensaverBehavior = prefsManager.screensaverBehavior

        // Set initial chip selection
        val chipToCheck = when (screensaverBehavior) {
            "game_image" -> R.id.screensaverGameImage
            "black_screen" -> R.id.screensaverBlackScreen
            else -> R.id.screensaverDefaultImage
        }
        screensaverBehaviorChipGroup.check(chipToCheck)

        // Setup listener
        screensaverBehaviorChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val behavior = when (checkedIds[0]) {
                    R.id.screensaverGameImage -> "game_image"
                    R.id.screensaverBlackScreen -> "black_screen"
                    else -> "default_image"
                }
                prefsManager.screensaverBehavior = behavior
                // Mark as changed
                screensaverBehaviorChanged = true
            }
        }
    }

    // ========== MUSIC INTEGRATION START ==========
    // DELETE THIS ENTIRE FUNCTION when removing music feature

    /**
     * Setup background music settings
     */
    private fun setupMusicSettings() {
        android.util.Log.d("SettingsActivity", "Setting up music settings")

        // Master toggle
        val musicEnabled = prefsManager.musicEnabled
        val masterChipToCheck = if (musicEnabled) R.id.musicMasterOn else R.id.musicMasterOff
        musicMasterChipGroup.check(masterChipToCheck)

        // Show/hide detailed settings based on master toggle
        musicSettings.visibility = if (musicEnabled) View.VISIBLE else View.GONE

        // Master toggle listener
        musicMasterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val enabled = checkedIds[0] == R.id.musicMasterOn
                prefsManager.musicEnabled = enabled

                // Show/hide detailed settings
                musicSettings.visibility = if (enabled) View.VISIBLE else View.GONE

                // Mark as changed
                musicSettingsChanged = true
                musicMasterToggleChanged = true // Track master toggle specifically

                android.util.Log.d("SettingsActivity", "Music master toggle: $enabled")
            }
        }

        // System view music
        val systemMusicEnabled = prefsManager.musicSystemEnabled
        val systemChipToCheck = if (systemMusicEnabled) R.id.musicSystemOn else R.id.musicSystemOff
        musicSystemChipGroup.check(systemChipToCheck)

        musicSystemChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val enabled = checkedIds[0] == R.id.musicSystemOn
                prefsManager.musicSystemEnabled = enabled
                musicSettingsChanged = true
                android.util.Log.d("SettingsActivity", "System music: $enabled")
            }
        }

        // Game view music
        val gameMusicEnabled = prefsManager.musicGameEnabled
        val gameChipToCheck = if (gameMusicEnabled) R.id.musicGameOn else R.id.musicGameOff
        musicGameChipGroup.check(gameChipToCheck)

        musicGameChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val enabled = checkedIds[0] == R.id.musicGameOn
                prefsManager.musicGameEnabled = enabled
                musicSettingsChanged = true
                android.util.Log.d("SettingsActivity", "Game music: $enabled")
            }
        }

        // Screensaver music
        val screensaverMusicEnabled = prefsManager.musicScreensaverEnabled
        val screensaverChipToCheck = if (screensaverMusicEnabled) R.id.musicScreensaverOn else R.id.musicScreensaverOff
        musicScreensaverChipGroup.check(screensaverChipToCheck)

        musicScreensaverChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val enabled = checkedIds[0] == R.id.musicScreensaverOn
                prefsManager.musicScreensaverEnabled = enabled
                musicSettingsChanged = true
                android.util.Log.d("SettingsActivity", "Screensaver music: $enabled")
            }
        }

        // Music during videos
        val videoBehavior = prefsManager.musicVideoBehavior
        val videoChipToCheck = when (videoBehavior) {
            "continue" -> R.id.musicVideoContinue
            "pause" -> R.id.musicVideoPause
            else -> R.id.musicVideoDuck
        }
        musicVideoChipGroup.check(videoChipToCheck)

        musicVideoChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val behavior = when (checkedIds[0]) {
                    R.id.musicVideoContinue -> "continue"
                    R.id.musicVideoPause -> "pause"
                    else -> "duck"
                }
                prefsManager.musicVideoBehavior = behavior
                android.util.Log.d("SettingsActivity", "Video music behavior: $behavior")
            }
        }

        android.util.Log.d("SettingsActivity", "Music settings setup complete")

        // Song title display
        val songTitleEnabled = prefsManager.musicSongTitleEnabled
        val songTitleChipToCheck = if (songTitleEnabled) R.id.musicSongTitleOn else R.id.musicSongTitleOff
        musicSongTitleChipGroup.check(songTitleChipToCheck)

        // Show/hide duration section
        musicSongTitleDurationSection.visibility = if (songTitleEnabled) View.VISIBLE else View.GONE

        musicSongTitleChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val enabled = checkedIds[0] == R.id.musicSongTitleOn
                prefsManager.musicSongTitleEnabled = enabled

                // Show/hide duration section
                musicSongTitleDurationSection.visibility = if (enabled) View.VISIBLE else View.GONE

                android.util.Log.d("SettingsActivity", "Song title display: $enabled")
            }
        }

        // Song title duration
        val songTitleDuration = prefsManager.musicSongTitleDuration // 0-15 (0=2s, 15=infinite)
        musicSongTitleDurationSeekBar.progress = songTitleDuration
        updateSongTitleDurationText(songTitleDuration)

        musicSongTitleDurationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSongTitleDurationText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    prefsManager.musicSongTitleDuration = it.progress
                }
            }
        })

        // Song title background opacity
        val songTitleOpacity = prefsManager.musicSongTitleOpacity // 0-100 (default 80%)
        val opacityProgress = songTitleOpacity / 5 // Convert 0-100 to 0-20
        musicSongTitleOpacitySeekBar.progress = opacityProgress
        updateSongTitleOpacityText(songTitleOpacity)

        musicSongTitleOpacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val opacityPercent = progress * 5 // Convert 0-20 to 0-100
                updateSongTitleOpacityText(opacityPercent)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val opacityPercent = it.progress * 5 // Convert 0-20 to 0-100
                    prefsManager.musicSongTitleOpacity = opacityPercent
                    musicSettingsChanged = true
                }
            }
        })

        android.util.Log.d("SettingsActivity", "Music settings setup complete")
    }

    private fun updateSongTitleDurationText(progress: Int) {
        // 0-14 = 2s, 4s, 6s, ... 30s
        // 15 = Infinite
        val text = if (progress == 15) {
            "∞ (Infinite)"
        } else {
            val duration = (progress + 1) * 2 // 0->2s, 1->4s, 2->6s, ... 14->30s
            "${duration}s"
        }
        musicSongTitleDurationText.text = text
    }

    private fun updateSongTitleOpacityText(progress: Int) {
        // progress is 0-100 in 5% increments (0, 5, 10, ... 95, 100)
        musicSongTitleOpacityText.text = "$progress%"
    }

    // ========== MUSIC INTEGRATION END ==========

    private fun setupBlackOverlay() {
        // Load saved black overlay enabled state (default: false/off)
        val blackOverlayEnabled = prefsManager.blackOverlayEnabled

        // Set initial chip selection
        val chipToCheck = if (blackOverlayEnabled) R.id.blackOverlayOn else R.id.blackOverlayOff
        blackOverlayChipGroup.check(chipToCheck)

        // Setup listener
        blackOverlayChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val enabled = checkedIds[0] == R.id.blackOverlayOn
                prefsManager.blackOverlayEnabled = enabled
            }
        }
    }

    private fun updateMediaPathDisplay() {
        val path = prefsManager.mediaPath
        mediaPathText.text = path

        // Check if folder exists
        val mediaDir = java.io.File(path)
        val exists = mediaDir.exists() && mediaDir.isDirectory
        val hasCorrectName = path.contains("downloaded_media", ignoreCase = true)

        when {
            exists && hasCorrectName -> {
                // Folder exists and has correct name - green indicator
                mediaStatusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                mediaStatusText.text = "●"
                mediaStatusDescription.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                mediaStatusDescription.text = "✓ Folder found"
            }
            exists && !hasCorrectName -> {
                // Folder exists but wrong name - yellow indicator
                mediaStatusText.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                mediaStatusText.text = "●"
                mediaStatusDescription.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                mediaStatusDescription.text = "⚠ Non-standard path"
            }
            else -> {
                // Folder not found - gray indicator
                mediaStatusText.setTextColor(android.graphics.Color.parseColor("#666666"))
                mediaStatusText.text = "●"
                mediaStatusDescription.setTextColor(android.graphics.Color.parseColor("#666666"))
                mediaStatusDescription.text = "Folder not found"
            }
        }
    }

    private fun updateSystemPathDisplay() {
        val path = prefsManager.systemPath.takeIf { it.isNotEmpty() }
        val defaultPath = "${Environment.getExternalStorageDirectory()}/ES-DE Companion/system_images"
        systemPathText.text = path ?: "Default: $defaultPath"

        // Setup System Logos Path
        val systemLogosPath = prefsManager.systemLogosPath.takeIf { it.isNotEmpty() }
        val defaultLogosPath = "${Environment.getExternalStorageDirectory()}/ES-DE Companion/system_logos"
        systemLogosPathText.text = systemLogosPath ?: "Default: $defaultLogosPath"
    }

    private fun getPathFromUri(uri: Uri): String {
        android.util.Log.d("SettingsActivity", "getPathFromUri - Input URI: $uri")
        android.util.Log.d("SettingsActivity", "  URI scheme: ${uri.scheme}")
        android.util.Log.d("SettingsActivity", "  URI authority: ${uri.authority}")

        // Handle file:// URIs directly
        if (uri.scheme == "file") {
            val path = uri.path ?: throw IllegalArgumentException("File URI has no path")
            android.util.Log.d("SettingsActivity", "File URI - returning path: $path")
            return path
        }

        // Handle content:// URIs
        if (uri.scheme == "content") {
            // Special handling for Google Photos / cloud providers
            if (uri.authority == "com.google.android.apps.photos.contentprovider" ||
                uri.authority?.contains("google.android.apps.docs") == true ||
                uri.authority?.contains("com.android.providers.media.documents") == true
            ) {
                android.util.Log.d(
                    "SettingsActivity",
                    "Cloud/Photos provider detected: ${uri.authority}"
                )

                // These providers require content resolver query
                val projection = arrayOf(
                    android.provider.MediaStore.MediaColumns.DATA,
                    android.provider.MediaStore.MediaColumns.DISPLAY_NAME
                )
                try {
                    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            // Try DATA column first (actual file path)
                            val dataIndex =
                                cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                            if (dataIndex >= 0) {
                                val path = cursor.getString(dataIndex)
                                if (path != null && path.isNotEmpty()) {
                                    android.util.Log.d(
                                        "SettingsActivity",
                                        "Cloud provider - got path: $path"
                                    )
                                    return path
                                }
                            }

                            // Fallback: get display name (we'll need to copy the file)
                            val nameIndex =
                                cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) {
                                val fileName = cursor.getString(nameIndex)
                                android.util.Log.d(
                                    "SettingsActivity",
                                    "Cloud file name: $fileName (needs copy)"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SettingsActivity", "Could not query cloud provider", e)
                }

                // If we get here, file is cloud-only - throw exception to trigger copy fallback
                throw IllegalArgumentException("Cloud file must be copied to local storage")
            }

            // Special handling for Downloads provider
            if (uri.authority == "com.android.providers.downloads.documents") {
                android.util.Log.d("SettingsActivity", "Downloads provider URI detected")

                // Try to get the real path via content resolver query
                val projection = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
                try {
                    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val columnIndex =
                                cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                            if (columnIndex >= 0) {
                                val path = cursor.getString(columnIndex)
                                if (path != null) {
                                    android.util.Log.d(
                                        "SettingsActivity",
                                        "Downloads provider - got path: $path"
                                    )
                                    return path
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SettingsActivity", "Could not query Downloads provider", e)
                }

                // Fallback: try constructing Downloads path
                val documentId = android.provider.DocumentsContract.getDocumentId(uri)
                android.util.Log.d("SettingsActivity", "Downloads document ID: $documentId")

                when {
                    documentId.startsWith("raw:") -> {
                        val rawPath = documentId.substringAfter("raw:")
                        android.util.Log.d("SettingsActivity", "Downloads raw path: $rawPath")
                        return rawPath
                    }

                    documentId.startsWith("msf:") -> {
                        val fileId = documentId.substringAfter("msf:")
                        android.util.Log.d("SettingsActivity", "MediaStore file ID: $fileId")

                        val downloadUri = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Files.getContentUri("external"),
                            fileId.toLongOrNull()
                                ?: throw IllegalArgumentException("Invalid file ID: $fileId")
                        )

                        try {
                            contentResolver.query(downloadUri, projection, null, null, null)
                                ?.use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        val columnIndex =
                                            cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                                        if (columnIndex >= 0) {
                                            val path = cursor.getString(columnIndex)
                                            if (path != null) {
                                                android.util.Log.d(
                                                    "SettingsActivity",
                                                    "MediaStore file path: $path"
                                                )
                                                return path
                                            }
                                        }
                                    }
                                }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "SettingsActivity",
                                "Failed to query MediaStore for file",
                                e
                            )
                        }
                    }

                    documentId.matches(Regex("\\d+")) -> {
                        val downloadUri = android.content.ContentUris.withAppendedId(
                            android.net.Uri.parse("content://downloads/public_downloads"),
                            documentId.toLong()
                        )

                        try {
                            contentResolver.query(downloadUri, projection, null, null, null)
                                ?.use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        val columnIndex =
                                            cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                                        if (columnIndex >= 0) {
                                            val path = cursor.getString(columnIndex)
                                            if (path != null) {
                                                android.util.Log.d(
                                                    "SettingsActivity",
                                                    "Downloads path: $path"
                                                )
                                                return path
                                            }
                                        }
                                    }
                                }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "SettingsActivity",
                                "Failed to query Downloads provider",
                                e
                            )
                        }
                    }
                }

                throw IllegalArgumentException("Could not resolve Downloads provider path for: $documentId")
            }

            // Special handling for Media Documents provider (Gallery, Photos app)
            if (uri.authority == "com.android.providers.media.documents") {
                android.util.Log.d("SettingsActivity", "Media documents provider detected")

                try {
                    val documentId = android.provider.DocumentsContract.getDocumentId(uri)
                    val parts = documentId.split(":")

                    if (parts.size == 2) {
                        val type = parts[0]  // "image", "video", "audio"
                        val id = parts[1]

                        android.util.Log.d(
                            "SettingsActivity",
                            "Media document - type: $type, id: $id"
                        )

                        val contentUri = when (type) {
                            "image" -> android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> null
                        }

                        if (contentUri != null) {
                            val selection = "${android.provider.MediaStore.MediaColumns._ID} = ?"
                            val selectionArgs = arrayOf(id)
                            val projection = arrayOf(android.provider.MediaStore.MediaColumns.DATA)

                            contentResolver.query(
                                contentUri,
                                projection,
                                selection,
                                selectionArgs,
                                null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val columnIndex =
                                        cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                                    if (columnIndex >= 0) {
                                        val path = cursor.getString(columnIndex)
                                        if (path != null) {
                                            android.util.Log.d(
                                                "SettingsActivity",
                                                "Media document path: $path"
                                            )
                                            return path
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SettingsActivity", "Failed to query media documents", e)
                }

                throw IllegalArgumentException("Could not resolve media document path")
            }

            // Try standard MediaStore query for other content providers
            val projection = arrayOf(android.provider.MediaStore.Images.Media.DATA)
            try {
                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex =
                            cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DATA)
                        if (columnIndex >= 0) {
                            val path = cursor.getString(columnIndex)
                            if (path != null && path.isNotEmpty()) {
                                android.util.Log.d(
                                    "SettingsActivity",
                                    "Content URI - got path from MediaStore: $path"
                                )
                                return path
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("SettingsActivity", "Could not query MediaStore for path", e)
            }
        }

        // Fallback: parse the URI path manually (for externalstorage.documents)
        val path = uri.path ?: throw IllegalArgumentException("URI has no path")
        android.util.Log.d("SettingsActivity", "Parsing URI path manually: $path")

        // Handle different URI formats
        when {
            // Internal storage tree: content://com.android.externalstorage.documents/tree/primary:path
            path.contains("/tree/primary:") -> {
                val actualPath = path.substringAfter("primary:")
                val result = "/storage/emulated/0/$actualPath"
                android.util.Log.d("SettingsActivity", "Converted tree primary path to: $result")
                return result
            }

            // Internal storage document: /document/primary:path
            path.contains("/document/primary:") -> {
                val actualPath = path.substringAfter("primary:")
                val result = "/storage/emulated/0/$actualPath"
                android.util.Log.d(
                    "SettingsActivity",
                    "Converted document primary path to: $result"
                )
                return result
            }

            // SD card tree: /tree/XXXX-XXXX:path
            path.contains("/tree/") && !path.contains("primary:") && path.contains(":") -> {
                val treePath = path.substringAfter("/tree/")
                val parts = treePath.split(":", limit = 2)
                if (parts.size == 2) {
                    val sdCardId = parts[0]
                    val sdPath = parts[1]
                    val result = "/storage/$sdCardId/$sdPath"
                    android.util.Log.d(
                        "SettingsActivity",
                        "Converted SD card tree path to: $result"
                    )
                    return result
                }
                throw IllegalArgumentException("Could not parse SD card tree path from: $path")
            }

            // SD card document: /document/XXXX-XXXX:path
            path.contains("/document/") && !path.contains("primary:") && path.contains(":") -> {
                val documentPath = path.substringAfter("/document/")
                val decodedPath = java.net.URLDecoder.decode(documentPath, "UTF-8")
                android.util.Log.d("SettingsActivity", "Decoded document path: $decodedPath")

                val parts = decodedPath.split(":", limit = 2)
                if (parts.size == 2) {
                    val firstPart = parts[0]
                    // SD card IDs are typically 4-4 hex format
                    if (firstPart.matches(Regex("[A-Z0-9]{4}-[A-Z0-9]{4}"))) {
                        val sdCardId = firstPart
                        val sdPath = parts[1]
                        val result = "/storage/$sdCardId/$sdPath"
                        android.util.Log.d(
                            "SettingsActivity",
                            "Converted SD card document path to: $result"
                        )
                        return result
                    }
                }

                // Changed: throw instead of just logging
                throw IllegalArgumentException("Document path doesn't match SD card format: $decodedPath")
            }

            // MediaStore image ID: /document/image:12345
            path.contains("/document/image:") -> {
                val imageId = path.substringAfter("image:")
                android.util.Log.d("SettingsActivity", "Found MediaStore image ID: $imageId")

                val projection = arrayOf(android.provider.MediaStore.Images.Media.DATA)
                val selection = "${android.provider.MediaStore.Images.Media._ID} = ?"
                val selectionArgs = arrayOf(imageId)

                try {
                    contentResolver.query(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val columnIndex =
                                cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DATA)
                            if (columnIndex >= 0) {
                                val path = cursor.getString(columnIndex)
                                if (path != null) {
                                    android.util.Log.d(
                                        "SettingsActivity",
                                        "Got real path for image ID: $path"
                                    )
                                    return path
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                        "SettingsActivity",
                        "Failed to query MediaStore for image ID",
                        e
                    )
                }

                throw IllegalArgumentException("Could not resolve MediaStore image path for ID: $imageId")
            }

            // Raw path format
            path.startsWith("/storage/") -> {
                android.util.Log.d("SettingsActivity", "Already a storage path: $path")
                return path
            }

            // Last resort fallback - this handles the else case
            path.contains("primary:") -> {
                val extracted = path.substringAfter("primary:")
                val result = "/storage/emulated/0/$extracted"
                android.util.Log.d("SettingsActivity", "Fallback extraction: $result")
                return result
            }

            // Ultimate fallback - throw exception if nothing matched
            else -> {
                throw IllegalArgumentException("Could not convert URI to path: $uri (path: $path)")
            }
        }
    }

    /**
     * Update scripts path display with validation
     * Now uses ScriptManager for centralized validation logic
     */
    private fun updateScriptsPathDisplay() {
        val path = prefsManager.scriptsPath
        scriptsPathText.text = path

        val scriptsDir = java.io.File(path)

        // Validate scripts using ScriptManager
        val validationResult = ScriptManager.validateScripts(scriptsDir)

        // Update status based on validation results
        when {
            validationResult.allValid -> {
                // All 7 scripts valid - green indicator
                scriptsStatusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                scriptsStatusText.text = "●"
                scriptsStatusDescription.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                scriptsStatusDescription.text = "✓ All 7 scripts valid"
            }
            validationResult.outdatedCount > 0 -> {
                // Scripts exist but are outdated - yellow indicator
                scriptsStatusText.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                scriptsStatusText.text = "●"
                scriptsStatusDescription.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                scriptsStatusDescription.text = "⚠ ${validationResult.outdatedCount} outdated (update for quote handling)"
            }
            validationResult.validCount > 0 -> {
                // Some scripts valid - yellow indicator
                scriptsStatusText.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                scriptsStatusText.text = "●"
                scriptsStatusDescription.setTextColor(android.graphics.Color.parseColor("#FFC107"))

                val issues = mutableListOf<String>()
                if (validationResult.missingCount > 0) issues.add("${validationResult.missingCount} missing")
                if (validationResult.invalidCount > 0) issues.add("${validationResult.invalidCount} invalid")

                scriptsStatusDescription.text = "⚠ ${validationResult.validCount}/7 valid (${issues.joinToString(", ")})"
            }
            else -> {
                // No valid scripts - red indicator
                scriptsStatusText.setTextColor(android.graphics.Color.parseColor("#CF6679"))
                scriptsStatusText.text = "●"
                scriptsStatusDescription.setTextColor(android.graphics.Color.parseColor("#CF6679"))

                if (validationResult.missingCount == 7) {
                    scriptsStatusDescription.text = "Scripts not found"
                } else if (validationResult.invalidCount > 0 || validationResult.outdatedCount > 0) {
                    scriptsStatusDescription.text = "⚠ Scripts found but invalid/outdated"
                } else {
                    scriptsStatusDescription.text = "Scripts missing or invalid"
                }
            }
        }
    }

    private fun updateCustomBackgroundDisplay() {
        val customBackgroundPath = prefsManager.customBackgroundPath.takeIf { it.isNotEmpty() }

        if (customBackgroundPath == null) {
            // No custom background set
            customBackgroundPathText.text = "Not set (using built-in default)"
            customBackgroundStatusText.setTextColor(android.graphics.Color.parseColor("#666666"))
            customBackgroundStatusText.text = "●"
            customBackgroundStatusDescription.setTextColor(android.graphics.Color.parseColor("#666666"))
            customBackgroundStatusDescription.text = "Using built-in default"
            return
        }

        // Custom background is set - verify it exists
        try {
            val file = File(customBackgroundPath)

            if (file.exists() && file.canRead()) {
                // File exists and accessible - green indicator
                customBackgroundPathText.text = file.name
                customBackgroundStatusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                customBackgroundStatusText.text = "●"
                customBackgroundStatusDescription.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                customBackgroundStatusDescription.text = "✓ Custom background set"
            } else {
                // File not accessible - red indicator
                customBackgroundPathText.text = file.name
                customBackgroundStatusText.setTextColor(android.graphics.Color.parseColor("#CF6679"))
                customBackgroundStatusText.text = "●"
                customBackgroundStatusDescription.setTextColor(android.graphics.Color.parseColor("#CF6679"))
                customBackgroundStatusDescription.text = "⚠ File not accessible"
            }
        } catch (e: Exception) {
            // Error checking file - red indicator
            customBackgroundPathText.text = customBackgroundPath
            customBackgroundStatusText.setTextColor(android.graphics.Color.parseColor("#CF6679"))
            customBackgroundStatusText.text = "●"
            customBackgroundStatusDescription.setTextColor(android.graphics.Color.parseColor("#CF6679"))
            customBackgroundStatusDescription.text = "⚠ Error: ${e.message}"
        }
    }

    // ========== MUSIC INTEGRATION START ==========
    private fun updateMusicPathDisplay() {
        val path = prefsManager.musicPath
        musicPathText.text = path
    }
    // ========== MUSIC INTEGRATION END ==========

    /**
     * Show dialog asking if user wants to update scripts after path change
     */
    private fun showScriptsPathChangedUpdatePrompt(selectedPath: String) {
        AlertDialog.Builder(this)
            .setTitle("Scripts Path Updated")
            .setMessage("The scripts folder path has been changed to:\n\n$selectedPath\n\n" +
                    "Would you like to update the scripts in this location now?\n\n" +
                    "• Update Scripts: Overwrites scripts with latest version\n" +
                    "• Skip: Keep existing scripts (if any)")
            .setPositiveButton("Update Scripts") { _, _ ->
                // Update scripts, then offer verification
                updateScriptsInSettings()
            }
            .setNegativeButton("Skip") { _, _ ->
                // Skip update, but still offer verification
                showScriptsPathChangedVerificationPrompt()
            }
            .setIcon(android.R.drawable.ic_dialog_info)
            .show()
    }

    /**
     * Show dialog asking if user wants to verify scripts
     */
    private fun showScriptsPathChangedVerificationPrompt() {
        AlertDialog.Builder(this)
            .setTitle("Verify Scripts?")
            .setMessage("Would you like to verify that ES-DE can execute the scripts in this location?\n\n" +
                    "This will check if ES-DE is sending game/system information to the app.")
            .setPositiveButton("Verify Now") { _, _ ->
                // Return to MainActivity with verification flag
                val intent = Intent()
                intent.putExtra("START_SCRIPT_VERIFICATION", true)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
            .setNegativeButton("Skip") { _, _ ->
                // Just update the display and stay in settings
                updateScriptsPathDisplay()
            }
            .show()
    }

    /**
     * Update scripts directly in settings
     * Now uses ScriptManager for centralized logic
     */
    private fun updateScriptsInSettings() {
        val scriptsPath = prefsManager.scriptsPath

        val result = ScriptManager.createAllScripts(File(scriptsPath))

        if (result.success) {
            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()

            // Update the display
            updateScriptsPathDisplay()

            // Offer verification
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                showScriptsPathChangedVerificationPrompt()
            }, 500)
        } else {
            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            android.util.Log.e("SettingsActivity", result.message)
        }
    }

    private fun checkAndRequestPermissions() {
        // Android 13+ only - simplified permission model
        if (Environment.isExternalStorageManager()) {
            createScriptFiles()
        } else {
            showManageStoragePermissionDialog()
        }
    }

    private fun showManageStoragePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission Required")
            .setMessage("This app needs access to external storage to create script files in the ES-DE folder.\n\nPlease enable \"Allow management of all files\" in the next screen.")
            .setPositiveButton("Grant Permission") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to general settings
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNonStandardScriptsPathWarningForWizard(selectedPath: String) {
        AlertDialog.Builder(this)
            .setTitle("Non-Standard Path")
            .setMessage("Warning: The selected path doesn't appear to be the ES-DE scripts folder.\n\n" +
                    "Selected: $selectedPath\n\n" +
                    "Expected path should contain:\n" +
                    "• ES-DE\n" +
                    "• scripts\n\n" +
                    "ES-DE may not find the scripts if they're not in the correct location.\n\n" +
                    "Continue anyway?")
            .setPositiveButton("Continue") { _, _ ->
                // Continue wizard
                continueSetupWizard()
            }
            .setNegativeButton("Choose Again") { _, _ ->
                // Let user pick again - don't advance wizard
                pathSelectionType = PathSelection.SCRIPTS
                directoryPicker.launch(null)
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setCancelable(false)
            .show()
    }

    private fun showScriptsMissingWarning() {
        AlertDialog.Builder(this)
            .setTitle("Scripts Not Found")
            .setMessage("Warning: All 7 script files were not found in the selected folder.\n\n" +
                    "Without these scripts, ES-DE will not be able to communicate game selections to this app, and game media will not be displayed.\n\n" +
                    "You can:\n" +
                    "• Go back and create the scripts\n" +
                    "• Continue anyway (you can create scripts later from settings)")
            .setPositiveButton("Continue Anyway") { _, _ ->
                // Continue wizard without scripts
                continueSetupWizard()
            }
            .setNegativeButton("Go Back") { _, _ ->
                // Go back to step 3 to create scripts
                setupStep = 2
                continueSetupWizard()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setCancelable(false)
            .show()
    }

    private fun showCustomWizardDialog(
        title: String,
        message: String,
        topRightText: String? = null,
        bottomRightText: String,
        onCancel: () -> Unit,
        onTopRight: (() -> Unit)? = null,
        onBottomRight: () -> Unit
    ) {
        // Create custom title view with X button
        val titleContainer = android.widget.LinearLayout(this)
        titleContainer.orientation = android.widget.LinearLayout.HORIZONTAL
        titleContainer.setPadding(60, 40, 20, 20)
        titleContainer.gravity = android.view.Gravity.CENTER_VERTICAL

        val titleText = android.widget.TextView(this)
        titleText.text = title
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

        val builder = AlertDialog.Builder(this)
        builder.setCustomTitle(titleContainer)
        builder.setMessage(message)

        if (topRightText != null && onTopRight != null) {
            // Left aligned button
            builder.setNegativeButton(topRightText) { _, _ -> onTopRight() }
        }

        // Right aligned button
        builder.setPositiveButton(bottomRightText) { _, _ -> onBottomRight() }

        builder.setCancelable(false)

        val dialog = builder.create()

        closeButton.setOnClickListener {
            onCancel()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPermissionWizardDialog() {
        // Create custom title view with X button
        val titleContainer = android.widget.LinearLayout(this)
        titleContainer.orientation = android.widget.LinearLayout.HORIZONTAL
        titleContainer.setPadding(60, 40, 20, 20)
        titleContainer.gravity = android.view.Gravity.CENTER_VERTICAL

        val titleText = android.widget.TextView(this)
        titleText.text = "Step 1: Storage Permissions"
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
            .setMessage("The app needs storage permissions to create script files and access media folders.\n\nClick 'Grant Permission' to open system settings. After granting permission, the wizard will automatically continue.")
            .setPositiveButton("Grant Permission") { _, _ ->
                // Android 13+ - always use MANAGE_ALL_FILES_ACCESS_PERMISSION
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
            .setCancelable(false)
            .create()

        closeButton.setOnClickListener {
            isInSetupWizard = false
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Storage permission is required to create script files. Please grant permission in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createScriptFiles() {
        val scriptsPath = prefsManager.scriptsPath

        try {
            val scriptsDir = File(scriptsPath)

            // Check if any scripts already exist
            val existingScripts = findExistingScripts(scriptsDir)

            if (existingScripts.isNotEmpty()) {
                // Scripts exist, show warning
                showScriptOverwriteDialog(scriptsDir, existingScripts)
            } else {
                // No existing scripts, create them
                val gameSelectScript = File(scriptsDir, "game-select/${AppConstants.Scripts.GAME_SELECT_SCRIPT}")
                val systemSelectScript = File(scriptsDir, "system-select/${AppConstants.Scripts.SYSTEM_SELECT_SCRIPT}")
                writeScriptFiles(scriptsDir, gameSelectScript, systemSelectScript)
            }

        } catch (e: Exception) {
            // Show error message
            Toast.makeText(
                this,
                "Error checking scripts: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("SettingsActivity", "Error checking scripts", e)
        }
    }

    private fun writeScriptFiles(scriptsDir: File, gameSelectScript: File, systemSelectScript: File) {
        try {
            // Prepare all directories
            prepareScriptDirectories(scriptsDir)

            // Delete old scripts
            val failedToDelete = deleteOldScriptFiles(scriptsDir)

            // Write all 7 script files
            writeAllScriptFiles(scriptsDir)

            // Generate and show success message
            val successMessage = getScriptCreationSuccessMessage(failedToDelete)
            Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show()

            // Update the status display
            updateScriptsPathDisplay()

            // Handle post-creation actions
            handlePostScriptCreation()

        } catch (e: Exception) {
            // Show error message
            Toast.makeText(
                this,
                "Error creating scripts: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("SettingsActivity", "Error creating scripts", e)
        }
    }

    /**
     * Check for existing scripts
     * Now delegates to ScriptManager
     * @return List of existing script File objects
     */
    private fun findExistingScripts(scriptsDir: File): List<File> {
        return ScriptManager.findExistingScripts(scriptsDir)
    }

    /**
     * Show dialog warning about overwriting existing scripts
     */
    private fun showScriptOverwriteDialog(scriptsDir: File, existingScripts: List<File>) {
        val scriptNames = existingScripts.map { it.name }

        AlertDialog.Builder(this)
            .setTitle("Scripts Already Exist")
            .setMessage("The following script files already exist:\n\n" +
                    scriptNames.joinToString("\n") { "• $it" } +
                    "\n\nOverwriting them will replace any custom modifications you may have made.\n\n" +
                    "Do you want to overwrite the existing scripts?")
            .setPositiveButton("Overwrite") { _, _ ->
                val gameSelectScript = File(scriptsDir, "game-select/${AppConstants.Scripts.GAME_SELECT_SCRIPT}")
                val systemSelectScript = File(scriptsDir, "system-select/${AppConstants.Scripts.SYSTEM_SELECT_SCRIPT}")
                writeScriptFiles(scriptsDir, gameSelectScript, systemSelectScript)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(
                    this,
                    "Script creation cancelled",
                    Toast.LENGTH_SHORT
                ).show()

                // If in wizard, still continue to next step
                if (isInSetupWizard) {
                    continueSetupWizard()
                }
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    /**
     * Prepare script directories
     * Now delegates to ScriptManager
     */
    private fun prepareScriptDirectories(scriptsDir: File) {
        ScriptManager.prepareScriptDirectories(scriptsDir)
    }

    /**
     * Delete old script files that are no longer needed
     * Now delegates to ScriptManager
     * @return List of script names that failed to delete
     */
    private fun deleteOldScriptFiles(scriptsDir: File): List<String> {
        return ScriptManager.deleteOldScriptFiles(scriptsDir)
    }

    /**
     * Write all 7 script files
     * Now delegates to ScriptManager for centralized logic
     */
    private fun writeAllScriptFiles(scriptsDir: File) {
        ScriptManager.writeAllScriptFiles(scriptsDir)
    }

    /**
     * Generate success message based on deletion results
     */
    private fun getScriptCreationSuccessMessage(failedToDelete: List<String>): String {
        return when {
            failedToDelete.isNotEmpty() ->
                "All 7 scripts created successfully!\n\nWarning: Could not delete old scripts: ${failedToDelete.joinToString()}"
            else ->
                "All 7 scripts created successfully!"
        }
    }

    /**
     * Handle post-creation actions (wizard or verification)
     */
    private fun handlePostScriptCreation() {
        if (isInSetupWizard) {
            continueSetupWizard()
        } else {
            // Not in wizard - user manually created scripts
            // Offer to verify
            showScriptsCreatedVerificationPrompt()
        }
    }

    private fun checkAndAutoStartWizard() {
        // Check if permissions are granted (Android 13+ simplified)
        val hasPermission = Environment.isExternalStorageManager()

        // Check if this is first launch
        val hasCompletedSetup = prefsManager.setupCompleted

        android.util.Log.d("SettingsActivity", "Auto-start check - hasPermission: $hasPermission, hasCompletedSetup: $hasCompletedSetup")

        // Auto-start wizard if first launch OR missing permissions
        if (!hasCompletedSetup || !hasPermission) {
            android.util.Log.d("SettingsActivity", "Auto-starting wizard")
            // Delay slightly to let UI settle
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!isInSetupWizard) {
                    startSetupWizard()
                }
            }, 500)
        } else {
            android.util.Log.d("SettingsActivity", "Wizard not needed - setup complete and has permissions")
        }
    }

    private fun startSetupWizard() {
        isInSetupWizard = true
        setupStep = 0

        // Create custom title view with X button
        val titleContainer = android.widget.LinearLayout(this)
        titleContainer.orientation = android.widget.LinearLayout.HORIZONTAL
        titleContainer.setPadding(60, 40, 20, 20)
        titleContainer.gravity = android.view.Gravity.CENTER_VERTICAL

        val titleText = android.widget.TextView(this)
        titleText.text = "Welcome to Quick Setup"
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
            .setMessage("This will guide you through setting up your app:\n\n" +
                    "1. Grant storage permissions\n" +
                    "2. Select ES-DE scripts folder\n" +
                    "3. Create script files\n" +
                    "4. Select downloaded media folder\n" +
                    "5. Enable scripts in ES-DE\n\n" +
                    "Ready to begin?")
            .setPositiveButton("Yeah man, I wanna do it") { _, _ ->
                continueSetupWizard()
            }
            .setCancelable(false)
            .create()

        closeButton.setOnClickListener {
            isInSetupWizard = false
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun continueSetupWizard() {
        setupStep++

        when (setupStep) {
            1 -> {
                // Step 1: Check permissions (Android 13+ simplified)
                val hasPermission = Environment.isExternalStorageManager()

                if (hasPermission) {
                    // Permissions already granted
                    showWizardDialogRequired(
                        "Step 1: Storage Permissions ✓",
                        "Storage permissions are already granted!\n\nThe app can access external storage to create scripts and read media files.\n\nClick 'Continue' to proceed.",
                        "Continue"
                    ) {
                        continueSetupWizard()
                    }
                } else {
                    // Need to request permissions - show dialog first
                    showPermissionWizardDialog()
                }
            }
            2 -> {
                // Step 2: Select scripts folder
                showCustomWizardDialog(
                    title = "Step 2: ES-DE Scripts Folder",
                    message = "Now select your ES-DE scripts folder.\n\nThis needs to be the scripts folder where your ES-DE application data directory has been set up:\n~/ES-DE/scripts\n\nClick 'Select Folder' to choose, or 'Use Default' to use the default internal storage path (ES-DE is setup on internal storage in the default location).",
                    topRightText = "Use Default",
                    bottomRightText = "Select Folder",
                    onCancel = { isInSetupWizard = false },
                    onTopRight = {
                        // Use default path
                        prefsManager.scriptsPath = "/storage/emulated/0/ES-DE/scripts"
                        updateScriptsPathDisplay()
                        continueSetupWizard()
                    },
                    onBottomRight = {
                        pathSelectionType = PathSelection.SCRIPTS
                        directoryPicker.launch(null)
                    }
                )
            }
            3 -> {
                // Step 3: Create scripts
                showCustomWizardDialog(
                    title = "Step 3: Create Script Files",
                    message = "Now we'll create the ES-DE integration script files in the folder you selected.\n\nClick 'Create Scripts' to continue, or 'Skip' if scripts already exist.",
                    topRightText = "Skip",
                    bottomRightText = "Create Scripts",
                    onCancel = { isInSetupWizard = false },
                    onTopRight = {
                        // Check if all 7 scripts exist
                        val scriptsPath = prefsManager.scriptsPath
                        val scriptsDir = java.io.File(scriptsPath)
                        val scriptFiles = listOf(
                            java.io.File(scriptsDir, "game-select/${AppConstants.Scripts.GAME_SELECT_SCRIPT}"),
                            java.io.File(scriptsDir, "system-select/${AppConstants.Scripts.SYSTEM_SELECT_SCRIPT}"),
                            java.io.File(scriptsDir, "game-start/${AppConstants.Scripts.GAME_START_SCRIPT}"),
                            java.io.File(scriptsDir, "game-end/${AppConstants.Scripts.GAME_END_SCRIPT}"),
                            java.io.File(scriptsDir, "screensaver-start/${AppConstants.Scripts.SCREENSAVER_START_SCRIPT}"),
                            java.io.File(scriptsDir, "screensaver-end/${AppConstants.Scripts.SCREENSAVER_END_SCRIPT}"),
                            java.io.File(scriptsDir, "screensaver-game-select/${AppConstants.Scripts.SCREENSAVER_GAME_SELECT_SCRIPT}")
                        )

                        val allExist = scriptFiles.all { it.exists() }

                        if (allExist) {
                            // All 7 scripts exist, continue
                            continueSetupWizard()
                        } else {
                            // Scripts missing, show warning
                            showScriptsMissingWarning()
                        }
                    },
                    onBottomRight = {
                        checkAndRequestPermissions()
                    }
                )
            }
            4 -> {
                // Step 4: Select downloaded_media
                showCustomWizardDialog(
                    title = "Step 4: Downloaded Media Folder",
                    message = "Select your ES-DE downloaded_media folder where game artwork is stored.\n\nDefault location:\n/storage/emulated/0/ES-DE/downloaded_media\n\nClick 'Select Folder' to choose, or 'Use Default' to use the default internal storage path (downloaded_media is in the default internal storage location).",
                    topRightText = "Use Default",
                    bottomRightText = "Select Folder",
                    onCancel = { isInSetupWizard = false },
                    onTopRight = {
                        // Use default path
                        prefsManager.mediaPath = "/storage/emulated/0/ES-DE/downloaded_media"
                        updateMediaPathDisplay()
                        continueSetupWizard()
                    },
                    onBottomRight = {
                        pathSelectionType = PathSelection.MEDIA
                        directoryPicker.launch(null)
                    }
                )
            }
            5 -> {
                // Step 5: Enable scripts in ES-DE
                showWizardDialogRequired(
                    "Step 5: Enable Scripts in ES-DE",
                    "Final step! You need to enable both custom script options in ES-DE:\n\n" +
                            "1. Open ES-DE\n" +
                            "2. Press START to open Main Menu\n" +
                            "3. Go to Other Settings\n" +
                            "4. Toggle ON 'Custom Event Scripts'\n" +
                            "5. Toggle ON 'Browsing Custom Events'\n\n" +
                            "Once both are enabled, ES-DE will send game/system information to this app!\n\n" +
                            "Click 'I've Enabled Scripts' when done.",
                    "I've Enabled Scripts"
                ) {
                    continueSetupWizard()
                }
            }
            6 -> {
                // Setup complete!
                isInSetupWizard = false

                // Mark setup as completed
                prefsManager.setupCompleted = true

                // Show comprehensive tutorial dialog
                showPostSetupTutorial(triggerVerification = true)
            }
        }
    }

    private fun showWizardDialogRequired(title: String, message: String, buttonText: String, onContinue: () -> Unit) {
        // Create custom title view with X button
        val titleContainer = android.widget.LinearLayout(this)
        titleContainer.orientation = android.widget.LinearLayout.HORIZONTAL
        titleContainer.setPadding(60, 40, 20, 20)
        titleContainer.gravity = android.view.Gravity.CENTER_VERTICAL

        val titleText = android.widget.TextView(this)
        titleText.text = title
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

        val scrollView = android.widget.ScrollView(this)
        val textView = android.widget.TextView(this)
        textView.text = message
        textView.setPadding(60, 40, 60, 40)
        textView.textSize = 16f
        textView.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))

        scrollView.addView(textView)

        // Set max height via layout params
        val displayMetrics = resources.displayMetrics
        val maxHeight = (displayMetrics.heightPixels * 0.35).toInt() // 35% of screen
        val params = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.height = maxHeight
        scrollView.layoutParams = params

        val dialog = AlertDialog.Builder(this)
            .setCustomTitle(titleContainer)
            .setView(scrollView)
            .setPositiveButton(buttonText) { _, _ ->
                onContinue()
            }
            .setCancelable(false)
            .create()

        closeButton.setOnClickListener {
            isInSetupWizard = false
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showHideAppsDialog() {
        // Get all installed apps
        val packageManager = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val apps = packageManager.queryIntentActivities(mainIntent, 0)
            .mapNotNull { resolveInfo ->
                val pkgName = resolveInfo.activityInfo?.packageName
                if (pkgName != null) {
                    AppItem(
                        name = resolveInfo.loadLabel(packageManager).toString(),
                        packageName = pkgName,
                        icon = resolveInfo.loadIcon(packageManager)
                    )
                } else {
                    null
                }
            }
            .sortedBy { it.name.lowercase() }

        // Load hidden apps from preferences
        val hiddenApps = prefsManager.hiddenApps
        val selectedApps = apps.map { !hiddenApps.contains(it.packageName) }.toBooleanArray()

        // Create ListView
        val listView = android.widget.ListView(this)
        listView.choiceMode = android.widget.ListView.CHOICE_MODE_MULTIPLE

        val adapter = object : android.widget.ArrayAdapter<AppItem>(
            this,
            android.R.layout.simple_list_item_multiple_choice,
            android.R.id.text1,
            apps
        ) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent)
                val checkBox = view.findViewById<android.widget.CheckedTextView>(android.R.id.text1)

                val app = getItem(position)
                checkBox.text = app?.name

                // Add icon to the left with uniform size
                app?.icon?.let { icon ->
                    // Create a scaled version of the icon to uniform size (48dp)
                    val iconSize = (48 * resources.displayMetrics.density).toInt()
                    icon.setBounds(0, 0, iconSize, iconSize)
                    checkBox.setCompoundDrawables(icon, null, null, null)
                    checkBox.compoundDrawablePadding = (12 * resources.displayMetrics.density).toInt()
                }

                // Ensure consistent row height
                checkBox.minHeight = (56 * resources.displayMetrics.density).toInt()
                checkBox.setPadding(
                    (16 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt()
                )

                return view
            }
        }

        listView.adapter = adapter

        // Set initial checked states
        for (i in selectedApps.indices) {
            listView.setItemChecked(i, selectedApps[i])
        }

        // Create custom centered title
        val titleView = android.widget.TextView(this)
        titleView.text = "Show/Hide Apps"
        titleView.textSize = 20f
        titleView.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
        titleView.gravity = android.view.Gravity.CENTER
        titleView.setPadding(60, 40, 60, 20)

        // Create dialog with custom buttons
        val dialog = AlertDialog.Builder(this)
            .setCustomTitle(titleView)
            .setView(listView)
            .setPositiveButton("Save") { _, _ ->
                // Save hidden apps
                val newHiddenApps = mutableSetOf<String>()
                for (i in apps.indices) {
                    if (!listView.isItemChecked(i)) {
                        newHiddenApps.add(apps[i].packageName)
                    }
                }
                android.util.Log.d("SettingsActivity", "Saving hidden apps: $newHiddenApps")
                prefsManager.hiddenApps = newHiddenApps

                // Notify MainActivity to refresh app list
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra("APPS_HIDDEN_CHANGED", true)
                })
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    data class AppItem(
        val name: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable
    )

    private fun setupSwipeGesture() {
        val scrollView = findViewById<android.widget.ScrollView>(R.id.settingsScrollView)
        val settingsBackButton = findViewById<ImageButton>(R.id.settingsBackButton)
        settingsBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 150
            private val SWIPE_VELOCITY_THRESHOLD = 150

            override fun onDown(e: MotionEvent): Boolean {
                return true // Must return true to receive subsequent events
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                // Check if horizontal swipe is dominant
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    // Check if swipe meets threshold requirements
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Right swipe - exit settings
                            android.util.Log.d("SettingsActivity", "Right swipe detected - exiting")
                            onBackPressedDispatcher.onBackPressed()
                            return true
                        }
                    }
                }
                return false
            }
        })

        // Custom touch listener that passes events to both gesture detector and ScrollView
        scrollView.setOnTouchListener { view, event ->
            val gestureHandled = gestureDetector.onTouchEvent(event)

            // If gesture was handled (swipe detected), consume the event
            if (gestureHandled) {
                true
            } else {
                // Let ScrollView handle scrolling
                view.onTouchEvent(event)
                false
            }
        }
    }

    private fun showPostSetupTutorial(triggerVerification: Boolean = false) {
        // Create custom title view with emoji
        val titleContainer = android.widget.LinearLayout(this)
        titleContainer.orientation = android.widget.LinearLayout.HORIZONTAL
        titleContainer.setPadding(60, 40, 60, 20)
        titleContainer.gravity = android.view.Gravity.CENTER

        val titleText = android.widget.TextView(this)
        titleText.text = "Setup Complete! 🎉"
        titleText.textSize = 24f
        titleText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
        titleText.gravity = android.view.Gravity.CENTER

        titleContainer.addView(titleText)

        // Create scrollable message view
        val scrollView = android.widget.ScrollView(this)
        val messageText = android.widget.TextView(this)
        messageText.text = """
Your ES-DE Companion is now configured and ready to use!

🎮 Quick Tips:

- Long press to open widget menu
- Swipe up anywhere to open the app drawer
- Tap the hamburger button (☰) to access the app settings
- Long-press any app to choose which screen it launches on
- Swipe right in settings to quickly close it

📱 Using with ES-DE:

- Browse games in ES-DE to see artwork on this screen
- Game videos will play if enabled in settings
- System logos appear when browsing systems

🏠 Recommended Setup:

For the best dual-screen experience, use Mjolnir home screen manager to run this app alongside ES-DE:

https://github.com/blacksheepmvp/mjolnir

You can always re-run this setup from the settings screen.

Enjoy your enhanced retro gaming experience!
"""
        messageText.textSize = 16f
        messageText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
        messageText.setPadding(60, 20, 60, 20)

        scrollView.addView(messageText)

        // Show dialog with single button
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(titleContainer)
            .setView(scrollView)
            .setPositiveButton("Continue") { _, _ ->
                // Close settings and trigger script verification
                if (triggerVerification) {
                    val intent = Intent()
                    intent.putExtra("START_SCRIPT_VERIFICATION", true)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                } else {
                    finish()
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
    }

    /**
     * Show prompt to verify scripts after creation
     */
    private fun showScriptsCreatedVerificationPrompt() {
        AlertDialog.Builder(this)
            .setTitle("Scripts Created Successfully")
            .setMessage("Scripts have been created!\n\n" +
                    "Would you like to verify that ES-DE can execute them?\n\n" +
                    "This will check if ES-DE is sending game/system information to the app.")
            .setPositiveButton("Verify Now") { _, _ ->
                // Return to MainActivity with verification flag
                val intent = Intent()
                intent.putExtra("START_SCRIPT_VERIFICATION", true)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
            .setNegativeButton("Later") { _, _ ->
                // Just close settings
                finish()
            }
            .show()
    }

    companion object {
        const val PREFS_NAME = "ESDESecondScreenPrefs"
        const val MEDIA_PATH_KEY = "media_path"
        const val SYSTEM_PATH_KEY = "system_path"
        const val SCRIPTS_PATH_KEY = "scripts_path"
        const val COLUMN_COUNT_KEY = "column_count"
        const val IMAGE_PREFERENCE_KEY = "image_preference"
        const val SYSTEM_IMAGE_PREFERENCE_KEY = "system_image_preference"
        const val GAME_IMAGE_PREFERENCE_KEY = "game_image_preference"
        const val SYSTEM_BACKGROUND_COLOR_KEY = "system_background_color"
        const val GAME_BACKGROUND_COLOR_KEY = "game_background_color"
        const val DIMMING_KEY = "dimming"
        const val BLUR_KEY = "blur"
        const val DRAWER_TRANSPARENCY_KEY = "drawer_transparency"
        const val VIDEO_ENABLED_KEY = "video_enabled"
        const val VIDEO_DELAY_KEY = "video_delay"
        const val VIDEO_AUDIO_ENABLED_KEY = "video_audio_enabled"
        const val GAME_LAUNCH_BEHAVIOR_KEY = "game_launch_behavior"
        const val SCREENSAVER_BEHAVIOR_KEY = "screensaver_behavior"
        const val BLACK_OVERLAY_ENABLED_KEY = "black_overlay_enabled"
        const val CUSTOM_BACKGROUND_KEY = "custom_background_uri"  // ADD THIS LINE
    }
}