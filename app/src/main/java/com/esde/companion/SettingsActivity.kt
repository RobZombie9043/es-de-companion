package com.esde.companion

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.chip.ChipGroup

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var setupWizardButton: Button
    private lateinit var mediaPathText: TextView
    private lateinit var mediaStatusText: TextView
    private lateinit var mediaStatusDescription: TextView
    private lateinit var selectMediaPathButton: Button
    private lateinit var systemPathText: TextView
    private lateinit var selectSystemPathButton: Button
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
    private lateinit var logoSizeSeekBar: SeekBar
    private lateinit var logoSizeText: TextView
    private lateinit var systemLogoSwitch: androidx.appcompat.widget.SwitchCompat
    private lateinit var gameLogoSwitch: androidx.appcompat.widget.SwitchCompat
    private lateinit var animationStyleChipGroup: ChipGroup
    private lateinit var imagePreferenceChipGroup: ChipGroup
    private lateinit var customAnimationSettings: LinearLayout
    private lateinit var animationDurationSeekBar: SeekBar
    private lateinit var animationDurationText: TextView
    private lateinit var animationScaleSeekBar: SeekBar
    private lateinit var animationScaleText: TextView
    private lateinit var customEffectTypeGroup: RadioGroup

    private var initialDimming: Int = 0
    private var initialBlur: Int = 0
    private var initialDrawerTransparency: Int = 0

    private var pathSelectionType = PathSelection.MEDIA

    enum class PathSelection {
        MEDIA, SYSTEM, SCRIPTS
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

                    // Warn if path doesn't look like ES-DE downloaded_media folder
                    if (!path.contains("downloaded_media", ignoreCase = true)) {
                        showNonStandardMediaPathWarningForWizard(path)
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
                        showNonStandardScriptsPathWarningForWizard(path)
                    } else {
                        // Path looks good, continue wizard if active
                        if (isInSetupWizard) {
                            continueSetupWizard()
                        }
                    }
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
                // User confirmed, do nothing
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
            selectSystemPathButton = findViewById(R.id.selectSystemPathButton)
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
            logoSizeSeekBar = findViewById(R.id.logoSizeSeekBar)
            systemLogoSwitch = findViewById(R.id.systemLogoSwitch)
            gameLogoSwitch = findViewById(R.id.gameLogoSwitch)
            logoSizeText = findViewById(R.id.logoSizeText)
            android.util.Log.d("SettingsActivity", "All views found")

            animationStyleChipGroup = findViewById<ChipGroup>(R.id.animationStyleChipGroup)
            android.util.Log.d("SettingsActivity", "Animation style chip group found")

            customAnimationSettings = findViewById<LinearLayout>(R.id.customAnimationSettings)
            animationDurationSeekBar = findViewById<SeekBar>(R.id.animationDurationSeekBar)
            animationDurationText = findViewById<TextView>(R.id.animationDurationText)
            animationScaleSeekBar = findViewById<SeekBar>(R.id.animationScaleSeekBar)
            animationScaleText = findViewById<TextView>(R.id.animationScaleText)
            customEffectTypeGroup = findViewById<RadioGroup>(R.id.customEffectTypeGroup)
            android.util.Log.d("SettingsActivity", "Custom animation controls found")

            imagePreferenceChipGroup = findViewById<ChipGroup>(R.id.imagePreferenceChipGroup)
            android.util.Log.d("SettingsActivity", "Image preference chip group found")

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

            createScriptsButton.setOnClickListener {
                checkAndRequestPermissions()
            }

            setupColumnCountSlider()
            android.util.Log.d("SettingsActivity", "Column count setup")
            setupDimmingSlider()
            android.util.Log.d("SettingsActivity", "Dimming setup")
            setupBlurSlider()
            android.util.Log.d("SettingsActivity", "Blur setup")
            setupDrawerTransparencySlider()
            android.util.Log.d("SettingsActivity", "Drawer transparency setup")
            setupLogoSizeSlider()
            android.util.Log.d("SettingsActivity", "Logo size setup")
            setupAnimationStyleChips()
            android.util.Log.d("SettingsActivity", "Animation style chips setup")
            setupCustomAnimationControls()
            android.util.Log.d("SettingsActivity", "Custom animation controls setup")
            setupImagePreferenceChips()
            android.util.Log.d("SettingsActivity", "Image preference chips setup")

            updateMediaPathDisplay()
            updateSystemPathDisplay()
            updateScriptsPathDisplay()

            // Check if we should auto-start wizard from MainActivity
            val autoStartWizard = intent.getBooleanExtra("AUTO_START_WIZARD", false)
            if (autoStartWizard) {
                // Start wizard immediately
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (!isInSetupWizard) {
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

    private fun setupLogoSizeSlider() {
        // Setup System Logo Toggle
        val systemLogoEnabled = prefs.getBoolean("system_logo_enabled", true)
        systemLogoSwitch.isChecked = systemLogoEnabled

        systemLogoSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("system_logo_enabled", isChecked).apply()
        }

        // Setup Game Logo Toggle
        val gameLogoEnabled = prefs.getBoolean("game_logo_enabled", true)
        gameLogoSwitch.isChecked = gameLogoEnabled

        gameLogoSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("game_logo_enabled", isChecked).apply()
        }

        // Setup Shared Logo Size (Small/Medium/Large only, no Off)
        val currentSize = prefs.getString("logo_size", "medium") ?: "medium"
        val position = when (currentSize) {
            "small" -> 0
            "medium" -> 1
            "large" -> 2
            else -> 1
        }

        logoSizeSeekBar.min = 0
        logoSizeSeekBar.max = 2
        logoSizeSeekBar.progress = position
        logoSizeText.text = when (position) {
            0 -> "Small"
            1 -> "Medium"
            2 -> "Large"
            else -> "Medium"
        }

        logoSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                logoSizeText.text = when (progress) {
                    0 -> "Small"
                    1 -> "Medium"
                    2 -> "Large"
                    else -> "Medium"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val size = when (it.progress) {
                        0 -> "small"
                        1 -> "medium"
                        2 -> "large"
                        else -> "medium"
                    }
                    prefs.edit().putString("logo_size", size).apply()
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

        // Effect Type Radio Group
        val currentEffect = prefs.getString("animation_effect", "scale_fade") ?: "scale_fade"
        when (currentEffect) {
            "fade" -> customEffectTypeGroup.check(R.id.customEffectFade)
            else -> customEffectTypeGroup.check(R.id.customEffectScaleFade)
        }

        customEffectTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            val effect = when (checkedId) {
                R.id.customEffectFade -> "fade"
                R.id.customEffectScaleFade -> "scale_fade"
                else -> "scale_fade"
            }
            prefs.edit().putString("animation_effect", effect).apply()
        }
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
        systemPathText.text = path ?: "Default: /storage/emulated/0/ES-DE/downloaded_media/systems"
    }

    private fun getPathFromUri(uri: Uri): String {
        // Convert content:// URI to file path
        val path = uri.path ?: return ""

        // Handle different URI formats
        when {
            // Internal storage: content://com.android.externalstorage.documents/tree/primary:path
            path.contains("/tree/primary:") -> {
                val actualPath = path.substringAfter("primary:")
                return "/storage/emulated/0/$actualPath"
            }
            // SD Card: content://com.android.externalstorage.documents/tree/XXXX-XXXX:path
            path.contains("/tree/") && path.contains(":") -> {
                // Extract SD card ID and path
                val treePath = path.substringAfter("/tree/")
                val parts = treePath.split(":")
                if (parts.size >= 2) {
                    val sdCardId = parts[0]
                    val sdPath = parts.drop(1).joinToString(":")
                    return "/storage/$sdCardId/$sdPath"
                }
                return path
            }
            else -> {
                // Fallback
                return path.substringAfter("primary:").let {
                    if (it.startsWith("/")) it else "/$it"
                }.let {
                    "/storage/emulated/0$it"
                }
            }
        }
    }

    private fun updateScriptsPathDisplay() {
        val path = prefs.getString(SCRIPTS_PATH_KEY, "/storage/emulated/0/ES-DE/scripts") ?: "/storage/emulated/0/ES-DE/scripts"
        scriptsPathText.text = path

        // Check if both scripts exist
        val scriptsDir = java.io.File(path)
        val gameSelectScript = java.io.File(scriptsDir, "game-select/game-select.sh")
        val systemSelectScript = java.io.File(scriptsDir, "system-select/system-select.sh")

        val gameExists = gameSelectScript.exists()
        val systemExists = systemSelectScript.exists()

        when {
            gameExists && systemExists -> {
                // Both scripts exist - green indicator
                scriptsStatusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                scriptsStatusText.text = "●"
                scriptsStatusDescription.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                scriptsStatusDescription.text = "✓ Both scripts found"
            }
            gameExists || systemExists -> {
                // Only one script exists - yellow indicator
                scriptsStatusText.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                scriptsStatusText.text = "●"
                scriptsStatusDescription.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                scriptsStatusDescription.text = if (gameExists) {
                    "⚠ Only game-select.sh found"
                } else {
                    "⚠ Only system-select.sh found"
                }
            }
            else -> {
                // No scripts found - gray indicator
                scriptsStatusText.setTextColor(android.graphics.Color.parseColor("#666666"))
                scriptsStatusText.text = "●"
                scriptsStatusDescription.setTextColor(android.graphics.Color.parseColor("#666666"))
                scriptsStatusDescription.text = "Scripts not found"
            }
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
            .setMessage("Warning: The script files were not found in the selected folder.\n\n" +
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

            // Check if scripts already exist
            val gameSelectScript = java.io.File(scriptsDir, "game-select/game-select.sh")
            val systemSelectScript = java.io.File(scriptsDir, "system-select/system-select.sh")

            if (gameSelectScript.exists() || systemSelectScript.exists()) {
                // Scripts exist, show warning
                val existingScripts = mutableListOf<String>()
                if (gameSelectScript.exists()) existingScripts.add("game-select.sh")
                if (systemSelectScript.exists()) existingScripts.add("system-select.sh")

                AlertDialog.Builder(this)
                    .setTitle("Scripts Already Exist")
                    .setMessage("The following script files already exist:\n\n" +
                            existingScripts.joinToString("\n") { "• $it" } +
                            "\n\nOverwriting them will replace any custom modifications you may have made.\n\n" +
                            "Do you want to overwrite the existing scripts?")
                    .setPositiveButton("Overwrite") { _, _ ->
                        // User confirmed, proceed with creation
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
            // Create game-select subfolder
            val gameSelectDir = java.io.File(scriptsDir, "game-select")
            gameSelectDir.mkdirs()

            // Create system-select subfolder
            val systemSelectDir = java.io.File(scriptsDir, "system-select")
            systemSelectDir.mkdirs()

            // Create game-select.sh script
            gameSelectScript.writeText("""#!/bin/bash
SCRIPT_DIR="${'$'}(cd "${'$'}(dirname "${'$'}0")" && pwd)"
LOG_DIR="${'$'}SCRIPT_DIR/../../logs"
filename="${'$'}(basename "${'$'}1")"
echo -n "${'$'}filename" > "${'$'}LOG_DIR/esde_game_scroll.txt"
echo -n "${'$'}3"        > "${'$'}LOG_DIR/esde_system.txt"
""")

            // Make executable
            gameSelectScript.setExecutable(true)

            // Create system-select.sh script
            systemSelectScript.writeText("""#!/bin/bash
SCRIPT_DIR="${'$'}(cd "${'$'}(dirname "${'$'}0")" && pwd)"
LOG_DIR="${'$'}SCRIPT_DIR/../../logs"
printf "%s" "${'$'}1" > "${'$'}LOG_DIR/esde_system_scroll.txt" &
""")

            // Make executable
            systemSelectScript.setExecutable(true)

            // Show success message
            android.widget.Toast.makeText(
                this,
                "Scripts created successfully!",
                android.widget.Toast.LENGTH_LONG
            ).show()

            // Update the status display
            updateScriptsPathDisplay()

            // Continue wizard if active
            if (isInSetupWizard) {
                continueSetupWizard()
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
                    "5. Select system images folder\n" +
                    "6. Enable scripts in ES-DE\n\n" +
                    "Ready to begin?")
            .setPositiveButton("Start Setup") { _, _ ->
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
                    message = "Now select your ES-DE scripts folder.\n\nDefault location:\n/storage/emulated/0/ES-DE/scripts\n\nClick 'Select Folder' to choose, or 'Use Default' to use the default path.",
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
                        // Check if scripts exist
                        val scriptsPath = prefs.getString(SCRIPTS_PATH_KEY, "/storage/emulated/0/ES-DE/scripts") ?: "/storage/emulated/0/ES-DE/scripts"
                        val scriptsDir = java.io.File(scriptsPath)
                        val gameSelectScript = java.io.File(scriptsDir, "game-select/game-select.sh")
                        val systemSelectScript = java.io.File(scriptsDir, "system-select/system-select.sh")

                        if (gameSelectScript.exists() && systemSelectScript.exists()) {
                            // Both scripts exist, continue
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
                    message = "Select your ES-DE downloaded_media folder where game artwork is stored.\n\nDefault location:\n/storage/emulated/0/ES-DE/downloaded_media\n\nClick 'Select Folder' to choose, or 'Use Default' to use the default path.",
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
                // Step 5: Select system images
                showCustomWizardDialog(
                    title = "Step 5: System Images Folder (Optional)",
                    message = "Finally, select an optional folder with system images to display in system view.\n\n" +
                            "Note: These are user-supplied images that you provide. Image filenames should match ES-DE system shortnames (e.g., 'snes.png', 'arcade.png').\n\n" +
                            "If a system image is not found, the app will show a random game from that system instead.\n\n" +
                            "Default location:\n/storage/emulated/0/ES-DE/downloaded_media/systems\n\n" +
                            "Click 'Select Folder' to choose, or 'Use Default' if you want to use the default path.",
                    topRightText = "Use Default",
                    bottomRightText = "Select Folder",
                    onCancel = { isInSetupWizard = false },
                    onTopRight = {
                        // Use default system images path
                        prefs.edit().putString(SYSTEM_PATH_KEY, "/storage/emulated/0/ES-DE/downloaded_media/systems").apply()
                        updateSystemPathDisplay()
                        continueSetupWizard()
                    },
                    onBottomRight = {
                        pathSelectionType = PathSelection.SYSTEM
                        directoryPicker.launch(null)
                    }
                )
            }
            6 -> {
                // Step 6: Enable scripts in ES-DE
                showWizardDialogRequired(
                    "Step 6: Enable Scripts in ES-DE",
                    "Final step! You need to enable custom scripts in ES-DE:\n\n" +
                            "1. Open ES-DE\n" +
                            "2. Press START to open Main Menu\n" +
                            "3. Go to Other Settings\n" +
                            "4. Toggle ON 'Custom Event Scripts'\n" +
                            "5. Toggle ON 'Browsing Custom Events'\n\n" +
                            "Once enabled, ES-DE will send game/system information to this app!\n\n" +
                            "Click 'I've Enabled Scripts' when done.",
                    "I've Enabled Scripts"
                ) {
                    continueSetupWizard()
                }
            }
            7 -> {
                // Setup complete!
                isInSetupWizard = false

                // Mark setup as completed
                prefs.edit().putBoolean("setup_completed", true).apply()

                AlertDialog.Builder(this)
                    .setTitle("Setup Complete! 🎉")
                    .setMessage("Your app is now configured and ready to use with ES-DE!\n\n" +
                            "You can change these settings anytime from the cards below.\n\n" +
                            "It is recommended to use the Mjolnir app (https://github.com/blacksheepmvp/mjolnir) to run this companion app together with ES-DE as your home screens.")
                    .setPositiveButton("Done") { _, _ -> }
                    .show()
            }
        }
    }

    private fun showWizardDialog(title: String, message: String, buttonText: String, onContinue: () -> Unit) {
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

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(scrollView)
            .setNegativeButton("Cancel Setup") { _, _ ->
                isInSetupWizard = false
            }
            .setNeutralButton("Use Default") { _, _ ->
                // Use default system images path
                prefs.edit().putString(SYSTEM_PATH_KEY, "/storage/emulated/0/ES-DE/downloaded_media/systems").apply()
                updateSystemPathDisplay()
                continueSetupWizard()
            }
            .setPositiveButton(buttonText) { _, _ ->
                onContinue()
            }
            .setCancelable(false)
            .show()
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

    private fun checkAndRequestPermissionsForWizard() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) {
                    continueSetupWizard()
                } else {
                    showManageStoragePermissionDialog()
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    continueSetupWizard()
                } else {
                    storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            else -> {
                continueSetupWizard()
            }
        }
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

    companion object {
        const val PREFS_NAME = "ESDESecondScreenPrefs"
        const val MEDIA_PATH_KEY = "media_path"
        const val SYSTEM_PATH_KEY = "system_path"
        const val SCRIPTS_PATH_KEY = "scripts_path"
        const val COLUMN_COUNT_KEY = "column_count"
        const val LOGO_SIZE_KEY = "logo_size"
        const val CROSSFADE_KEY = "crossfade"
        const val IMAGE_PREFERENCE_KEY = "image_preference"
        const val DIMMING_KEY = "dimming"
        const val BLUR_KEY = "blur"
        const val DRAWER_TRANSPARENCY_KEY = "drawer_transparency"
    }
}