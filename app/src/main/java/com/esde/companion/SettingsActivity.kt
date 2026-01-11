package com.esde.companion

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
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
class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
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
    private lateinit var imagePreferenceChipGroup: ChipGroup
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

    private var pathSelectionType = PathSelection.MEDIA

    enum class PathSelection {
        MEDIA, SYSTEM, SCRIPTS, SYSTEM_LOGOS, CUSTOM_BACKGROUND
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
                    prefs.edit().putString(MEDIA_PATH_KEY, path).apply()
                    updateMediaPathDisplay()

                    // Mark that media path changed (only if not in wizard)
                    if (!isInSetupWizard) {
                        mediaPathChanged = true
                    }

                    // Warn if path doesn't look like ES-DE downloaded_media folder
                    if (!path.contains("downloaded_media", ignoreCase = true)) {
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
                    prefs.edit().putString(SYSTEM_PATH_KEY, path).apply()
                    updateSystemPathDisplay()

                    // Continue wizard if active
                    if (isInSetupWizard) {
                        continueSetupWizard()
                    }
                }
                PathSelection.SCRIPTS -> {
                    prefs.edit().putString(SCRIPTS_PATH_KEY, path).apply()
                    updateScriptsPathDisplay()

                    // Warn if path doesn't look like ES-DE scripts folder
                    if (!path.contains("ES-DE", ignoreCase = true) || !path.contains("scripts", ignoreCase = true)) {
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
                    prefs.edit().putString("system_logos_path", path).apply()
                    systemLogosPathText.text = path
                    Toast.makeText(this, "System logos path updated", Toast.LENGTH_SHORT).show()
                }
                PathSelection.CUSTOM_BACKGROUND -> {
                    // This shouldn't be called since we use imagePicker instead
                    // But need to handle it for exhaustive when
                    android.util.Log.w("SettingsActivity", "directoryPicker called for CUSTOM_BACKGROUND - should use imagePicker instead")
                }
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
                            prefs.edit().putString(CUSTOM_BACKGROUND_KEY, path).apply()
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

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (isInSetupWizard) {
                continueSetupWizard()
            } else {
                // Permission granted, create scripts
                createScriptFiles()
            }
        } else {
            // Permission denied, show explanation
            showPermissionDeniedDialog()
            if (isInSetupWizard) {
                isInSetupWizard = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_settings)
            android.util.Log.d("SettingsActivity", "Layout inflated successfully")

            // Setup swipe gesture to exit settings
            setupSwipeGesture()

            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            android.util.Log.d("SettingsActivity", "Prefs initialized")

            // Store initial values
            initialDimming = prefs.getInt(DIMMING_KEY, 25)
            initialBlur = prefs.getInt(BLUR_KEY, 0)
            initialDrawerTransparency = prefs.getInt(DRAWER_TRANSPARENCY_KEY, 70)

            setupWizardButton = findViewById(R.id.setupWizardButton)
            setupWizardButton.setOnClickListener {
                startSetupWizard()
            }

            mediaPathText = findViewById(R.id.mediaPathText)
            mediaStatusText = findViewById(R.id.mediaStatusText)
            mediaStatusDescription = findViewById(R.id.mediaStatusDescription)
            selectMediaPathButton = findViewById(R.id.selectMediaPathButton)
            systemPathText = findViewById(R.id.systemPathText)
            systemLogosPathText = findViewById(R.id.systemLogosPathText)
            selectSystemPathButton = findViewById(R.id.selectSystemPathButton)
            selectSystemLogosPathButton = findViewById(R.id.selectSystemLogosPathButton)
            scriptsPathText = findViewById(R.id.scriptsPathText)
            selectScriptsPathButton = findViewById(R.id.selectScriptsPathButton)
            createScriptsButton = findViewById(R.id.createScriptsButton)
            scriptsStatusText = findViewById(R.id.scriptsStatusText)
            scriptsStatusDescription = findViewById(R.id.scriptsStatusDescription)
            columnCountSeekBar = findViewById(R.id.columnCountSeekBar)
            columnCountText = findViewById(R.id.columnCountText)
            hideAppsButton = findViewById(R.id.hideAppsButton)
            hideAppsButton.setOnClickListener {
                showHideAppsDialog()
            }
            dimmingSeekBar = findViewById(R.id.dimmingSeekBar)
            dimmingText = findViewById(R.id.dimmingText)
            blurSeekBar = findViewById(R.id.blurSeekBar)
            blurText = findViewById(R.id.blurText)
            drawerTransparencySeekBar = findViewById(R.id.drawerTransparencySeekBar)
            drawerTransparencyText = findViewById(R.id.drawerTransparencyText)
            android.util.Log.d("SettingsActivity", "All views found")

            animationStyleChipGroup = findViewById<ChipGroup>(R.id.animationStyleChipGroup)
            android.util.Log.d("SettingsActivity", "Animation style chip group found")

            customAnimationSettings = findViewById<LinearLayout>(R.id.customAnimationSettings)
            animationDurationSeekBar = findViewById<SeekBar>(R.id.animationDurationSeekBar)
            animationDurationText = findViewById<TextView>(R.id.animationDurationText)
            animationScaleSeekBar = findViewById<SeekBar>(R.id.animationScaleSeekBar)
            animationScaleText = findViewById<TextView>(R.id.animationScaleText)
            android.util.Log.d("SettingsActivity", "Custom animation controls found")

            imagePreferenceChipGroup = findViewById<ChipGroup>(R.id.imagePreferenceChipGroup)
            android.util.Log.d("SettingsActivity", "Image preference chip group found")

            // Initialize video settings
            videoSupportChipGroup = findViewById(R.id.videoSupportChipGroup)
            videoSettings = findViewById(R.id.videoSettings)
            videoDelaySeekBar = findViewById(R.id.videoDelaySeekBar)
            videoDelayText = findViewById(R.id.videoDelayText)
            videoAudioChipGroup = findViewById(R.id.videoAudioChipGroup)
            gameLaunchBehaviorChipGroup = findViewById(R.id.gameLaunchBehaviorChipGroup)
            screensaverBehaviorChipGroup = findViewById(R.id.screensaverBehaviorChipGroup)
            blackOverlayChipGroup = findViewById(R.id.blackOverlayChipGroup)
            android.util.Log.d("SettingsActivity", "Video settings found")

            customBackgroundPathText = findViewById(R.id.customBackgroundPathText)
            customBackgroundStatusText = findViewById(R.id.customBackgroundStatusText)
            customBackgroundStatusDescription = findViewById(R.id.customBackgroundStatusDescription)
            selectCustomBackgroundButton = findViewById(R.id.selectCustomBackgroundButton)
            clearCustomBackgroundButton = findViewById(R.id.clearCustomBackgroundButton)

            // Initialize version text
            versionText = findViewById(R.id.versionText)
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val versionName = packageInfo.versionName
                versionText.text = "ES-DE Companion v$versionName"
            } catch (e: Exception) {
                versionText.text = "ES-DE Companion"
            }

            selectMediaPathButton.setOnClickListener {
                pathSelectionType = PathSelection.MEDIA
                directoryPicker.launch(null)
            }

            selectSystemPathButton.setOnClickListener {
                pathSelectionType = PathSelection.SYSTEM
                directoryPicker.launch(null)
            }

            selectScriptsPathButton.setOnClickListener {
                pathSelectionType = PathSelection.SCRIPTS
                directoryPicker.launch(null)
            }

            selectSystemLogosPathButton.setOnClickListener {
                pathSelectionType = PathSelection.SYSTEM_LOGOS
                directoryPicker.launch(null)
            }

            createScriptsButton.setOnClickListener {
                checkAndRequestPermissions()
            }

            selectCustomBackgroundButton.setOnClickListener {
                pathSelectionType = PathSelection.CUSTOM_BACKGROUND
                // Create intent to pick image file
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/jpg", "image/png", "image/webp"))
                }
                customBackgroundPicker.launch(intent)
            }

            clearCustomBackgroundButton.setOnClickListener {
                // Clear custom background
                prefs.edit().remove(CUSTOM_BACKGROUND_KEY).apply()
                updateCustomBackgroundDisplay()
                customBackgroundChanged = true
                Toast.makeText(this, "Custom background cleared", Toast.LENGTH_SHORT).show()
            }

            setupColumnCountSlider()
            android.util.Log.d("SettingsActivity", "Column count setup")
            setupDimmingSlider()
            android.util.Log.d("SettingsActivity", "Dimming setup")
            setupBlurSlider()
            android.util.Log.d("SettingsActivity", "Blur setup")
            setupDrawerTransparencySlider()
            android.util.Log.d("SettingsActivity", "Drawer transparency setup")
            android.util.Log.d("SettingsActivity", "Logo size setup")
            setupAnimationStyleChips()
            android.util.Log.d("SettingsActivity", "Animation style chips setup")
            setupCustomAnimationControls()
            android.util.Log.d("SettingsActivity", "Custom animation controls setup")
            setupImagePreferenceChips()
            android.util.Log.d("SettingsActivity", "Image preference chips setup")
            setupVideoSettings()
            android.util.Log.d("SettingsActivity", "Video settings setup")
            setupGameLaunchBehavior()
            android.util.Log.d("SettingsActivity", "Game launch behavior setup")
            setupScreensaverBehavior()
            setupBlackOverlay()
            android.util.Log.d("SettingsActivity", "Screensaver behavior setup")

            updateMediaPathDisplay()
            updateSystemPathDisplay()
            updateScriptsPathDisplay()
            updateCustomBackgroundDisplay()

            // Check if we should auto-start wizard from MainActivity
            val autoStartWizard = intent.getBooleanExtra("AUTO_START_WIZARD", false)

            if (autoStartWizard) {
                // Start wizard immediately
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (!isInSetupWizard) {
                        // Remove the if/else for startStep - just always start from beginning
                        startSetupWizard()
                    }
                }, 500)
            } else {
                // Normal auto-start check (for when settings opened directly)
                checkAndAutoStartWizard()
            }

            // Handle back button press
            // Track initial hidden apps state
            val initialHiddenApps = prefs.getStringSet("hidden_apps", setOf()) ?: setOf()

            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Check if dimming, blur, or drawer transparency changed
                    val currentDimming = prefs.getInt(DIMMING_KEY, 25)
                    val currentBlur = prefs.getInt(BLUR_KEY, 0)
                    val currentDrawerTransparency = prefs.getInt(DRAWER_TRANSPARENCY_KEY, 70)
                    val currentHiddenApps = prefs.getStringSet("hidden_apps", setOf()) ?: setOf()

                    val intent = Intent()
                    if (currentDimming != initialDimming || currentBlur != initialBlur || currentDrawerTransparency != initialDrawerTransparency) {
                        // Signal that MainActivity should recreate itself to apply visual changes
                        intent.putExtra("NEEDS_RECREATE", true)
                    }
                    if (currentHiddenApps != initialHiddenApps) {
                        // Signal that hidden apps changed
                        intent.putExtra("APPS_HIDDEN_CHANGED", true)
                    }
                    // Signal if video settings changed
                    if (videoSettingsChanged) {
                        intent.putExtra("VIDEO_SETTINGS_CHANGED", true)
                    }
                    // Signal if logo size changed
                    if (logoSizeChanged) {
                        intent.putExtra("LOGO_SIZE_CHANGED", true)
                    }
                    // Signal if media path changed
                    if (mediaPathChanged) {
                        intent.putExtra("MEDIA_PATH_CHANGED", true)
                    }
                    // Signal if image preference changed
                    if (imagePreferenceChanged) {
                        intent.putExtra("IMAGE_PREFERENCE_CHANGED", true)
                    }
                    // Signal if logo toggles changed
                    if (logoTogglesChanged) {
                        intent.putExtra("LOGO_TOGGLES_CHANGED", true)
                    }
                    // Signal if game launch behavior changed
                    if (gameLaunchBehaviorChanged) {
                        intent.putExtra("GAME_LAUNCH_BEHAVIOR_CHANGED", true)
                    }
                    // Signal if screensaver behavior changed
                    if (screensaverBehaviorChanged) {
                        intent.putExtra("SCREENSAVER_BEHAVIOR_CHANGED", true)
                    }
                    // Signal if custom background changed
                    if (customBackgroundChanged) {
                        intent.putExtra("CUSTOM_BACKGROUND_CHANGED", true)
                    }
                    // Always signal to close drawer when returning from settings
                    intent.putExtra("CLOSE_DRAWER", true)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            })

            android.util.Log.d("SettingsActivity", "onCreate completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error in onCreate", e)
            e.printStackTrace()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        // If we're in quick setup at step 1, check if permission was just granted
        if (isInSetupWizard && setupStep == 1) {
            val hasPermission = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                }
                else -> true
            }

            if (hasPermission) {
                // Permission granted, continue wizard automatically
                continueSetupWizard()
            }
        }
    }

    private fun setupColumnCountSlider() {
        val currentColumns = prefs.getInt(COLUMN_COUNT_KEY, 4)
        columnCountSeekBar.min = 2
        columnCountSeekBar.max = 8
        columnCountSeekBar.progress = currentColumns
        columnCountText.text = "$currentColumns"

        columnCountSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                columnCountText.text = "$progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    prefs.edit().putInt(COLUMN_COUNT_KEY, it.progress).apply()
                }
            }
        })
    }

    private fun setupDimmingSlider() {
        val currentDimming = prefs.getInt(DIMMING_KEY, 25)
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
                    prefs.edit().putInt(DIMMING_KEY, percentage).commit()
                }
            }
        })
    }

    private fun setupBlurSlider() {
        val currentBlur = prefs.getInt(BLUR_KEY, 0)
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
                    prefs.edit().putInt(BLUR_KEY, it.progress).commit()
                }
            }
        })
    }

    private fun setupDrawerTransparencySlider() {
        val currentTransparency = prefs.getInt(DRAWER_TRANSPARENCY_KEY, 70)
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
                    prefs.edit().putInt(DRAWER_TRANSPARENCY_KEY, percentage).commit()
                }
            }
        })
    }

    private fun setupAnimationStyleChips() {
        val currentStyle = prefs.getString("animation_style", "scale_fade") ?: "scale_fade"

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
                prefs.edit().putString("animation_style", style).apply()

                // Show/hide custom settings with animation
                if (style == "custom") {
                    customAnimationSettings.visibility = View.VISIBLE
                    customAnimationSettings.alpha = 0f
                    customAnimationSettings.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                } else {
                    customAnimationSettings.animate()
                        .alpha(0f)
                        .setDuration(150)
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
        val currentDuration = prefs.getInt("animation_duration", 250)
        animationDurationSeekBar.max = 40  // 0-40 = 100ms-500ms (10ms steps)
        animationDurationSeekBar.progress = (currentDuration - 100) / 10
        animationDurationText.text = "${currentDuration}ms"

        animationDurationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val duration = 100 + (progress * 10)  // 100ms to 500ms
                animationDurationText.text = "${duration}ms"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val duration = 100 + (it.progress * 10)
                    prefs.edit().putInt("animation_duration", duration).apply()
                }
            }
        })

        // Scale Amount Slider (85% - 100%, in 1% steps)
        val currentScale = prefs.getInt("animation_scale", 95)
        animationScaleSeekBar.max = 15  // 0-15 = 85%-100%
        animationScaleSeekBar.progress = currentScale - 85
        animationScaleText.text = "${currentScale}%"

        animationScaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = 85 + progress  // 85% to 100%
                animationScaleText.text = "${scale}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val scale = 85 + it.progress
                    prefs.edit().putInt("animation_scale", scale).apply()
                }
            }
        })
    }

    private fun setupImagePreferenceChips() {
        val currentPref = prefs.getString(IMAGE_PREFERENCE_KEY, "fanart") ?: "fanart"

        val chipToCheck = when (currentPref) {
            "screenshot" -> R.id.imagePrefScreenshot
            else -> R.id.imagePrefFanart
        }
        imagePreferenceChipGroup.check(chipToCheck)

        imagePreferenceChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val preference = when (checkedIds[0]) {
                    R.id.imagePrefScreenshot -> "screenshot"
                    R.id.imagePrefFanart -> "fanart"
                    else -> "fanart"
                }
                prefs.edit().putString(IMAGE_PREFERENCE_KEY, preference).apply()
                // Mark that image preference changed
                imagePreferenceChanged = true
            }
        }
    }

    private fun setupVideoSettings() {
        // Load saved video enabled state (default: false/off)
        val videoEnabled = prefs.getBoolean(VIDEO_ENABLED_KEY, false)

        // Set initial chip selection
        val chipToCheck = if (videoEnabled) R.id.videoOn else R.id.videoOff
        videoSupportChipGroup.check(chipToCheck)

        // Show/hide video settings box based on initial state
        videoSettings.visibility = if (videoEnabled) View.VISIBLE else View.GONE

        // Setup video on/off listener
        videoSupportChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val enabled = checkedIds[0] == R.id.videoOn
                prefs.edit().putBoolean(VIDEO_ENABLED_KEY, enabled).apply()

                // Show/hide video settings box
                videoSettings.visibility = if (enabled) View.VISIBLE else View.GONE

                // Mark that video settings changed
                videoSettingsChanged = true
            }
        }

        // Setup video delay slider (0-5 seconds in 0.5s increments = 0-10 on seekbar)
        val savedDelay = prefs.getInt(VIDEO_DELAY_KEY, 4) // Default: 4 (2 seconds)
        videoDelaySeekBar.progress = savedDelay
        updateVideoDelayText(savedDelay)

        videoDelaySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateVideoDelayText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    prefs.edit().putInt(VIDEO_DELAY_KEY, it.progress).apply()
                    // Mark that video settings changed
                    videoSettingsChanged = true
                }
            }
        })

        // Setup video audio chips
        val audioEnabled = prefs.getBoolean(VIDEO_AUDIO_ENABLED_KEY, false)
        val audioChipToCheck = if (audioEnabled) R.id.videoAudioOn else R.id.videoAudioOff
        videoAudioChipGroup.check(audioChipToCheck)

        videoAudioChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val enabled = checkedIds[0] == R.id.videoAudioOn
                prefs.edit().putBoolean(VIDEO_AUDIO_ENABLED_KEY, enabled).apply()
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
        // Load saved game launch behavior (default: "default_image")
        val gameLaunchBehavior = prefs.getString(GAME_LAUNCH_BEHAVIOR_KEY, "game_image") ?: "game_image"

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
                prefs.edit().putString(GAME_LAUNCH_BEHAVIOR_KEY, behavior).apply()
                // Mark as changed
                gameLaunchBehaviorChanged = true
            }
        }
    }

    private fun setupScreensaverBehavior() {
        // Load saved screensaver behavior (default: "default_image")
        val screensaverBehavior = prefs.getString(SCREENSAVER_BEHAVIOR_KEY, "game_image") ?: "game_image"

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
                prefs.edit().putString(SCREENSAVER_BEHAVIOR_KEY, behavior).apply()
                // Mark as changed
                screensaverBehaviorChanged = true
            }
        }
    }

    private fun setupBlackOverlay() {
        // Load saved black overlay enabled state (default: false/off)
        val blackOverlayEnabled = prefs.getBoolean(BLACK_OVERLAY_ENABLED_KEY, false)

        // Set initial chip selection
        val chipToCheck = if (blackOverlayEnabled) R.id.blackOverlayOn else R.id.blackOverlayOff
        blackOverlayChipGroup.check(chipToCheck)

        // Setup listener
        blackOverlayChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val enabled = checkedIds[0] == R.id.blackOverlayOn
                prefs.edit().putBoolean(BLACK_OVERLAY_ENABLED_KEY, enabled).apply()
            }
        }
    }

    private fun updateMediaPathDisplay() {
        val path = prefs.getString(MEDIA_PATH_KEY, "/storage/emulated/0/ES-DE/downloaded_media") ?: "/storage/emulated/0/ES-DE/downloaded_media"
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
        val path = prefs.getString(SYSTEM_PATH_KEY, null)
        val defaultPath = "${Environment.getExternalStorageDirectory()}/ES-DE Companion/system_images"
        systemPathText.text = path ?: "Default: $defaultPath"

        // Setup System Logos Path
        val systemLogosPath = prefs.getString(
            "system_logos_path",
            null
        )
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

    private fun updateScriptsPathDisplay() {
        val path = prefs.getString(SCRIPTS_PATH_KEY, "/storage/emulated/0/ES-DE/scripts") ?: "/storage/emulated/0/ES-DE/scripts"
        scriptsPathText.text = path

        val scriptsDir = java.io.File(path)

        // Define required scripts with their expected content signatures
        val requiredScripts = mapOf(
            "game-select/esdecompanion-game-select.sh" to listOf(
                "LOG_DIR=\"/storage/emulated/0/ES-DE Companion/logs\"",
                "esde_game_filename.txt",
                "esde_game_name.txt",
                "esde_game_system.txt",
                "!basename"  // Should NOT contain basename (old version)
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
                "!basename",
                "!clean_filename"
            ),
            "game-end/esdecompanion-game-end.sh" to listOf(
                "LOG_DIR=\"/storage/emulated/0/ES-DE Companion/logs\"",
                "esde_gameend_filename.txt",
                "esde_gameend_name.txt",
                "esde_gameend_system.txt",
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
                "!basename",
                "!clean_filename"
            )
        )

        // Validate each script
        var validScripts = 0
        var outdatedScripts = mutableListOf<String>()
        var invalidScripts = mutableListOf<String>()
        var missingScripts = mutableListOf<String>()

        for ((scriptPath, expectedContent) in requiredScripts) {
            val scriptFile = java.io.File(scriptsDir, scriptPath)
            val scriptName = scriptPath.substringAfterLast("/")

            if (!scriptFile.exists()) {
                missingScripts.add(scriptName)
            } else {
                // Check content
                try {
                    val content = scriptFile.readText()
                    var isValid = true

                    for (expected in expectedContent) {
                        if (expected.startsWith("!")) {
                            // Negative check - should NOT contain this
                            val unwantedString = expected.substring(1)
                            if (content.contains(unwantedString)) {
                                isValid = false
                                outdatedScripts.add(scriptName)
                                break
                            }
                        } else {
                            // Positive check - SHOULD contain this
                            if (!content.contains(expected)) {
                                isValid = false
                                break
                            }
                        }
                    }

                    if (isValid) {
                        validScripts++
                    } else if (!outdatedScripts.contains(scriptName)) {
                        invalidScripts.add(scriptName)
                    }
                } catch (e: Exception) {
                    invalidScripts.add(scriptName)
                }
            }
        }

        // Update status based on validation results
        when {
            validScripts == 7 -> {
                // All 7 scripts exist with correct content - green indicator
                scriptsStatusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                scriptsStatusText.text = "●"
                scriptsStatusDescription.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                scriptsStatusDescription.text = "✓ All 7 scripts valid"
            }
            outdatedScripts.isNotEmpty() -> {
                // Scripts exist but are outdated - yellow indicator with specific message
                scriptsStatusText.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                scriptsStatusText.text = "●"
                scriptsStatusDescription.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                scriptsStatusDescription.text = "⚠ ${outdatedScripts.size} outdated (update for subfolder support)"
            }
            validScripts > 0 -> {
                // Some scripts valid - yellow indicator
                scriptsStatusText.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                scriptsStatusText.text = "●"
                scriptsStatusDescription.setTextColor(android.graphics.Color.parseColor("#FFC107"))

                val issues = mutableListOf<String>()
                if (missingScripts.isNotEmpty()) issues.add("${missingScripts.size} missing")
                if (invalidScripts.isNotEmpty()) issues.add("${invalidScripts.size} invalid")

                scriptsStatusDescription.text = "⚠ $validScripts/7 valid (${issues.joinToString(", ")})"
            }
            else -> {
                // No valid scripts - gray/red indicator
                scriptsStatusText.setTextColor(android.graphics.Color.parseColor("#CF6679"))
                scriptsStatusText.text = "●"
                scriptsStatusDescription.setTextColor(android.graphics.Color.parseColor("#CF6679"))

                if (missingScripts.size == 7) {
                    scriptsStatusDescription.text = "Scripts not found"
                } else if (invalidScripts.isNotEmpty() || outdatedScripts.isNotEmpty()) {
                    scriptsStatusDescription.text = "⚠ Scripts found but invalid/outdated"
                } else {
                    scriptsStatusDescription.text = "Scripts missing or invalid"
                }
            }
        }
    }

    private fun updateCustomBackgroundDisplay() {
        val customBackgroundPath = prefs.getString(CUSTOM_BACKGROUND_KEY, null)

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
     * Update scripts directly in settings (similar to MainActivity version)
     */
    private fun updateScriptsInSettings() {
        val scriptsPath = prefs.getString(SCRIPTS_PATH_KEY, "/storage/emulated/0/ES-DE/scripts")
            ?: "/storage/emulated/0/ES-DE/scripts"

        try {
            val scriptsDir = java.io.File(scriptsPath)

            // Create all script subdirectories
            val gameSelectDir = java.io.File(scriptsDir, "game-select")
            val systemSelectDir = java.io.File(scriptsDir, "system-select")
            val gameStartDir = java.io.File(scriptsDir, "game-start")
            val gameEndDir = java.io.File(scriptsDir, "game-end")
            val screensaverStartDir = java.io.File(scriptsDir, "screensaver-start")
            val screensaverEndDir = java.io.File(scriptsDir, "screensaver-end")
            val screensaverGameSelectDir = java.io.File(scriptsDir, "screensaver-game-select")

            gameSelectDir.mkdirs()
            systemSelectDir.mkdirs()
            gameStartDir.mkdirs()
            gameEndDir.mkdirs()
            screensaverStartDir.mkdirs()
            screensaverEndDir.mkdirs()
            screensaverGameSelectDir.mkdirs()

            // 1. esdecompanion-game-select.sh
            val gameSelectScriptFile = java.io.File(gameSelectDir, "esdecompanion-game-select.sh")
            gameSelectScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_game_filename.txt"
echo -n "${'$'}2" > "${'$'}LOG_DIR/esde_game_name.txt"
echo -n "${'$'}3" > "${'$'}LOG_DIR/esde_game_system.txt"
""")
            gameSelectScriptFile.setExecutable(true)

            // 2. esdecompanion-system-select.sh
            val systemSelectScriptFile = java.io.File(systemSelectDir, "esdecompanion-system-select.sh")
            systemSelectScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

printf "%s" "${'$'}1" > "${'$'}LOG_DIR/esde_system_name.txt" &
""")
            systemSelectScriptFile.setExecutable(true)

            // 3. esdecompanion-game-start.sh
            val gameStartScriptFile = java.io.File(gameStartDir, "esdecompanion-game-start.sh")
            gameStartScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_gamestart_filename.txt"
echo -n "${'$'}2" > "${'$'}LOG_DIR/esde_gamestart_name.txt"
echo -n "${'$'}3" > "${'$'}LOG_DIR/esde_gamestart_system.txt"
""")
            gameStartScriptFile.setExecutable(true)

            // 4. esdecompanion-game-end.sh
            val gameEndScriptFile = java.io.File(gameEndDir, "esdecompanion-game-end.sh")
            gameEndScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_gameend_filename.txt"
echo -n "${'$'}2" > "${'$'}LOG_DIR/esde_gameend_name.txt"
echo -n "${'$'}3" > "${'$'}LOG_DIR/esde_gameend_system.txt"
""")
            gameEndScriptFile.setExecutable(true)

            // 5. esdecompanion-screensaver-start.sh
            val screensaverStartScriptFile = java.io.File(screensaverStartDir, "esdecompanion-screensaver-start.sh")
            screensaverStartScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_screensaver_start.txt"

""")
            screensaverStartScriptFile.setExecutable(true)

            // 6. esdecompanion-screensaver-end.sh
            val screensaverEndScriptFile = java.io.File(screensaverEndDir, "esdecompanion-screensaver-end.sh")
            screensaverEndScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_screensaver_end.txt"

""")
            screensaverEndScriptFile.setExecutable(true)

            // 7. esdecompanion-screensaver-game-select.sh
            val screensaverGameSelectScriptFile = java.io.File(screensaverGameSelectDir, "esdecompanion-screensaver-game-select.sh")
            screensaverGameSelectScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_screensavergameselect_filename.txt"
echo -n "${'$'}2" > "${'$'}LOG_DIR/esde_screensavergameselect_name.txt"
echo -n "${'$'}3" > "${'$'}LOG_DIR/esde_screensavergameselect_system.txt"
""")
            screensaverGameSelectScriptFile.setExecutable(true)

            // Update the display
            updateScriptsPathDisplay()

            // Show success message
            Toast.makeText(
                this,
                "Scripts updated successfully!",
                Toast.LENGTH_LONG
            ).show()

            // Offer verification
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                showScriptsPathChangedVerificationPrompt()
            }, 500)

        } catch (e: Exception) {
            // Show error message
            Toast.makeText(
                this,
                "Error updating scripts: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("SettingsActivity", "Error updating scripts", e)
        }
    }

    private fun checkAndRequestPermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ - Check if we have MANAGE_EXTERNAL_STORAGE
                if (Environment.isExternalStorageManager()) {
                    createScriptFiles()
                } else {
                    showManageStoragePermissionDialog()
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-10 - Check WRITE_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    createScriptFiles()
                } else {
                    storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            else -> {
                // Android 5 and below - no runtime permissions needed
                createScriptFiles()
            }
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    }
                } else {
                    storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
        val scriptsPath = prefs.getString(SCRIPTS_PATH_KEY, "/storage/emulated/0/ES-DE/scripts") ?: "/storage/emulated/0/ES-DE/scripts"

        try {
            val scriptsDir = java.io.File(scriptsPath)

            // Check if any scripts already exist
            val scriptFiles = listOf(
                java.io.File(scriptsDir, "game-select/esdecompanion-game-select.sh"),
                java.io.File(scriptsDir, "system-select/esdecompanion-system-select.sh"),
                java.io.File(scriptsDir, "game-start/esdecompanion-game-start.sh"),
                java.io.File(scriptsDir, "game-end/esdecompanion-game-end.sh"),
                java.io.File(scriptsDir, "screensaver-start/esdecompanion-screensaver-start.sh"),
                java.io.File(scriptsDir, "screensaver-end/esdecompanion-screensaver-end.sh"),
                java.io.File(scriptsDir, "screensaver-game-select/esdecompanion-screensaver-game-select.sh")
            )

            val existingScripts = scriptFiles.filter { it.exists() }

            if (existingScripts.isNotEmpty()) {
                // Scripts exist, show warning
                val scriptNames = existingScripts.map { it.name }

                AlertDialog.Builder(this)
                    .setTitle("Scripts Already Exist")
                    .setMessage("The following script files already exist:\n\n" +
                            scriptNames.joinToString("\n") { "• $it" } +
                            "\n\nOverwriting them will replace any custom modifications you may have made.\n\n" +
                            "Do you want to overwrite the existing scripts?")
                    .setPositiveButton("Overwrite") { _, _ ->
                        // User confirmed, proceed with creation
                        val gameSelectScript = java.io.File(scriptsDir, "game-select/esdecompanion-game-select.sh")
                        val systemSelectScript = java.io.File(scriptsDir, "system-select/esdecompanion-system-select.sh")
                        writeScriptFiles(scriptsDir, gameSelectScript, systemSelectScript)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        // User cancelled
                        android.widget.Toast.makeText(
                            this,
                            "Script creation cancelled",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()

                        // If in wizard, still continue to next step
                        if (isInSetupWizard) {
                            continueSetupWizard()
                        }
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            } else {
                // No existing scripts, create them
                val gameSelectScript = java.io.File(scriptsDir, "game-select/esdecompanion-game-select.sh")
                val systemSelectScript = java.io.File(scriptsDir, "system-select/esdecompanion-system-select.sh")
                writeScriptFiles(scriptsDir, gameSelectScript, systemSelectScript)
            }

        } catch (e: Exception) {
            // Show error message
            android.widget.Toast.makeText(
                this,
                "Error checking scripts: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("SettingsActivity", "Error checking scripts", e)
        }
    }

    private fun writeScriptFiles(scriptsDir: java.io.File, gameSelectScript: java.io.File, systemSelectScript: java.io.File) {
        try {
            // First, check for and delete old-style scripts (only the original 2)
            val oldScripts = listOf(
                java.io.File(scriptsDir, "game-select/game-select.sh"),
                java.io.File(scriptsDir, "system-select/system-select.sh")
            )

            var deletedOldScripts = 0
            var failedToDelete = mutableListOf<String>()

            for (oldScript in oldScripts) {
                if (oldScript.exists()) {
                    try {
                        if (oldScript.delete()) {
                            deletedOldScripts++
                        } else {
                            failedToDelete.add(oldScript.name)
                            android.util.Log.w("SettingsActivity", "Failed to delete old script: ${oldScript.name}")
                        }
                    } catch (e: Exception) {
                        failedToDelete.add(oldScript.name)
                        android.util.Log.e("SettingsActivity", "Exception deleting old script: ${oldScript.name}", e)
                    }
                }
            }

            // Create all script subdirectories
            val gameSelectDir = java.io.File(scriptsDir, "game-select")
            val systemSelectDir = java.io.File(scriptsDir, "system-select")
            val gameStartDir = java.io.File(scriptsDir, "game-start")
            val gameEndDir = java.io.File(scriptsDir, "game-end")
            val screensaverStartDir = java.io.File(scriptsDir, "screensaver-start")
            val screensaverEndDir = java.io.File(scriptsDir, "screensaver-end")
            val screensaverGameSelectDir = java.io.File(scriptsDir, "screensaver-game-select")

            gameSelectDir.mkdirs()
            systemSelectDir.mkdirs()
            gameStartDir.mkdirs()
            gameEndDir.mkdirs()
            screensaverStartDir.mkdirs()
            screensaverEndDir.mkdirs()
            screensaverGameSelectDir.mkdirs()

            // 1. esdecompanion-game-select.sh - REMOVE filename sanitization
            val gameSelectScriptFile = java.io.File(gameSelectDir, "esdecompanion-game-select.sh")
            gameSelectScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_game_filename.txt"
echo -n "${'$'}2" > "${'$'}LOG_DIR/esde_game_name.txt"
echo -n "${'$'}3" > "${'$'}LOG_DIR/esde_game_system.txt"
""")
            gameSelectScriptFile.setExecutable(true)

            // 2. esdecompanion-system-select.sh
            val systemSelectScriptFile = java.io.File(systemSelectDir, "esdecompanion-system-select.sh")
            systemSelectScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

printf "%s" "${'$'}1" > "${'$'}LOG_DIR/esde_system_name.txt" &
""")
            systemSelectScriptFile.setExecutable(true)

            // 3. esdecompanion-game-start.sh - REMOVE sanitization
            val gameStartScriptFile = java.io.File(gameStartDir, "esdecompanion-game-start.sh")
            gameStartScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_gamestart_filename.txt"
echo -n "${'$'}2" > "${'$'}LOG_DIR/esde_gamestart_name.txt"
echo -n "${'$'}3" > "${'$'}LOG_DIR/esde_gamestart_system.txt"
""")
            gameStartScriptFile.setExecutable(true)

            // 4. esdecompanion-game-end.sh - REMOVE sanitization
            val gameEndScriptFile = java.io.File(gameEndDir, "esdecompanion-game-end.sh")
            gameEndScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_gameend_filename.txt"
echo -n "${'$'}2" > "${'$'}LOG_DIR/esde_gameend_name.txt"
echo -n "${'$'}3" > "${'$'}LOG_DIR/esde_gameend_system.txt"
""")
            gameEndScriptFile.setExecutable(true)

            // 5. esdecompanion-screensaver-start.sh
            val screensaverStartScriptFile = java.io.File(screensaverStartDir, "esdecompanion-screensaver-start.sh")
            screensaverStartScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_screensaver_start.txt"

""")
            screensaverStartScriptFile.setExecutable(true)

            // 6. esdecompanion-screensaver-end.sh
            val screensaverEndScriptFile = java.io.File(screensaverEndDir, "esdecompanion-screensaver-end.sh")
            screensaverEndScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_screensaver_end.txt"

""")
            screensaverEndScriptFile.setExecutable(true)

            // 7. esdecompanion-screensaver-game-select.sh - REMOVE sanitization
            val screensaverGameSelectScriptFile = java.io.File(screensaverGameSelectDir, "esdecompanion-screensaver-game-select.sh")
            screensaverGameSelectScriptFile.writeText("""#!/bin/bash

LOG_DIR="/storage/emulated/0/ES-DE Companion/logs"
mkdir -p "${'$'}LOG_DIR"

echo -n "${'$'}1" > "${'$'}LOG_DIR/esde_screensavergameselect_filename.txt"
echo -n "${'$'}2" > "${'$'}LOG_DIR/esde_screensavergameselect_name.txt"
echo -n "${'$'}3" > "${'$'}LOG_DIR/esde_screensavergameselect_system.txt"
""")
            screensaverGameSelectScriptFile.setExecutable(true)

            // Show success message with cleanup info
            val successMessage = when {
                deletedOldScripts > 0 && failedToDelete.isNotEmpty() ->
                    "All 7 scripts created successfully!\n\nCleaned up $deletedOldScripts old script(s).\n\nWarning: Could not delete ${failedToDelete.joinToString()}"
                deletedOldScripts > 0 ->
                    "All 7 scripts created successfully!\n\nCleaned up $deletedOldScripts old script(s)."
                failedToDelete.isNotEmpty() ->
                    "All 7 scripts created successfully!\n\nWarning: Could not delete old scripts: ${failedToDelete.joinToString()}"
                else ->
                    "All 7 scripts created successfully!"
            }

            android.widget.Toast.makeText(
                this,
                successMessage,
                android.widget.Toast.LENGTH_LONG
            ).show()

            // Update the status display
            updateScriptsPathDisplay()

            // Continue wizard if active
            if (isInSetupWizard) {
                continueSetupWizard()
            } else {
                // Not in wizard - user manually created scripts
                // Offer to verify
                showScriptsCreatedVerificationPrompt()
            }

        } catch (e: Exception) {
            // Show error message
            android.widget.Toast.makeText(
                this,
                "Error creating scripts: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("SettingsActivity", "Error creating scripts", e)
        }
    }

    private fun checkAndAutoStartWizard() {
        // Check if permissions are granted
        val hasPermission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> true
        }

        // Check if this is first launch
        val hasCompletedSetup = prefs.getBoolean("setup_completed", false)

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
                // Step 1: Check permissions
                val hasPermission = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                        ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    }
                    else -> true
                }

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
                        prefs.edit().putString(SCRIPTS_PATH_KEY, "/storage/emulated/0/ES-DE/scripts").apply()
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
                        val scriptsPath = prefs.getString(SCRIPTS_PATH_KEY, "/storage/emulated/0/ES-DE/scripts") ?: "/storage/emulated/0/ES-DE/scripts"
                        val scriptsDir = java.io.File(scriptsPath)
                        val scriptFiles = listOf(
                            java.io.File(scriptsDir, "game-select/esdecompanion-game-select.sh"),
                            java.io.File(scriptsDir, "system-select/esdecompanion-system-select.sh"),
                            java.io.File(scriptsDir, "game-start/esdecompanion-game-start.sh"),
                            java.io.File(scriptsDir, "game-end/esdecompanion-game-end.sh"),
                            java.io.File(scriptsDir, "screensaver-start/esdecompanion-screensaver-start.sh"),
                            java.io.File(scriptsDir, "screensaver-end/esdecompanion-screensaver-end.sh"),
                            java.io.File(scriptsDir, "screensaver-game-select/esdecompanion-screensaver-game-select.sh")
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
                        prefs.edit().putString(MEDIA_PATH_KEY, "/storage/emulated/0/ES-DE/downloaded_media").apply()
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
                prefs.edit().putBoolean("setup_completed", true).apply()

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
        val hiddenApps = prefs.getStringSet("hidden_apps", setOf()) ?: setOf()
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
                prefs.edit().putStringSet("hidden_apps", newHiddenApps).apply()

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

• Swipe up anywhere to open the app drawer
• Tap the hamburger button (☰) to access the app settings
• Long-press any app to choose which screen it launches on
• Swipe right in settings to quickly close it

📱 Using with ES-DE:

• Browse games in ES-DE to see artwork on this screen
• Game videos will play if enabled in settings
• System logos appear when browsing systems

🏠 Recommended Setup:

For the best dual-screen experience, use Mjolnir home screen manager to run this app alongside ES-DE:

https://github.com/blacksheepmvp/mjolnir

You can always re-run this setup from the settings screen.

Enjoy your enhanced retro gaming experience! ✨
        """.trimIndent()

        messageText.setPadding(60, 20, 60, 40)
        messageText.textSize = 15f
        messageText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
        messageText.setLineSpacing(8f, 1.0f)

        scrollView.addView(messageText)

        // Set max height for scroll view
        val displayMetrics = resources.displayMetrics
        val maxHeight = (displayMetrics.heightPixels * 0.5).toInt()
        val params = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.height = maxHeight
        scrollView.layoutParams = params

        AlertDialog.Builder(this)
            .setCustomTitle(titleContainer)
            .setView(scrollView)
            .setPositiveButton("Got it!") { _, _ ->
                if (triggerVerification) {
                    // Signal MainActivity to start verification
                    val intent = Intent()
                    intent.putExtra("START_SCRIPT_VERIFICATION", true)
                    setResult(Activity.RESULT_OK, intent)
                }
                finish()
            }
            .setCancelable(false)
            .show()
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