package com.esde.companion.managers

import android.content.Context
import android.net.Uri
import android.util.DisplayMetrics
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.esde.companion.data.Widget

/**
 * ═══════════════════════════════════════════════════════════
 * WIDGET CREATION MANAGER
 * ═══════════════════════════════════════════════════════════
 * Manages creation of special widget types (solid color, custom image).
 *
 * FEATURES:
 * - Color picker dialog for solid color widgets
 * - Image picker with URI to file path conversion
 * - Widget creation logic for special types
 *
 * ARCHITECTURE:
 * - Follows standard manager pattern
 * - Uses ContentResolver for URI to path conversion
 * - Lifecycle: init in onCreate -> use -> no cleanup needed
 * ═══════════════════════════════════════════════════════════
 */
class WidgetCreationManager(
    private val activity: AppCompatActivity,
    private val context: Context
) {

    companion object {
        private const val TAG = "WidgetCreationManager"
    }

    // ========== IMAGE PICKER ==========

    private var pendingImagePickerCallback: ((String) -> Unit)? = null

    private val imagePickerLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                val filePath = getPathFromUri(uri)
                if (filePath != null) {
                    pendingImagePickerCallback?.invoke(filePath)
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "Unable to access selected image. Please choose an image from your device storage.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            pendingImagePickerCallback = null
        }

    /**
     * Launch image picker and invoke callback with selected file path.
     */
    fun launchImagePicker(callback: (String) -> Unit) {
        pendingImagePickerCallback = callback
        imagePickerLauncher.launch("image/*")
    }

    // ========== COLOR PICKER ==========

    /**
     * Show color picker dialog with preset colors and custom hex input.
     */
    fun showColorPickerDialog(onColorSelected: (String) -> Unit) {
        val colors = listOf(
            "#FF0000" to "Red",
            "#00FF00" to "Green",
            "#0000FF" to "Blue",
            "#FFFF00" to "Yellow",
            "#FF00FF" to "Magenta",
            "#00FFFF" to "Cyan",
            "#FFA500" to "Orange",
            "#800080" to "Purple",
            "#FFFFFF" to "White",
            "#000000" to "Black",
            "#808080" to "Gray",
            "#FFC0CB" to "Pink",
            "custom" to "Custom Hex Color..."  // NEW: Custom option
        )

        val colorNames = colors.map { it.second }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Select Widget Color")
            .setItems(colorNames) { _, which ->
                if (which == colors.size - 1) {
                    // Custom hex color selected - show input dialog
                    showCustomHexDialog(onColorSelected)
                } else {
                    // Preset color selected
                    val selectedColor = colors[which].first
                    onColorSelected(selectedColor)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show custom hex color input dialog.
     */
    private fun showCustomHexDialog(onColorSelected: (String) -> Unit) {
        val input = android.widget.EditText(context).apply {
            hint = "Enter hex color (e.g., #FF5733)"
            inputType = android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setText("#")
            setSelection(1) // Place cursor after the #
        }

        val container = android.widget.FrameLayout(context).apply {
            setPadding(60, 20, 60, 20)
            addView(input)
        }

        AlertDialog.Builder(context)
            .setTitle("Custom Hex Color")
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                val hexColor = input.text.toString().trim()

                // Validate hex color format
                if (isValidHexColor(hexColor)) {
                    onColorSelected(hexColor.uppercase())
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "Invalid hex color format. Use #RRGGBB (e.g., #FF5733)",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    // Show the dialog again to let user try again
                    showCustomHexDialog(onColorSelected)
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Return to main color picker
                showColorPickerDialog(onColorSelected)
            }
            .show()
    }

    /**
     * Validate hex color format.
     */
    private fun isValidHexColor(color: String): Boolean {
        // Must start with # and be followed by exactly 6 hex digits
        val hexPattern = "^#[0-9A-Fa-f]{6}$".toRegex()
        return hexPattern.matches(color)
    }

    // ========== WIDGET CREATION HELPERS ==========

    /**
     * Create a color background widget.
     */
    fun createColorBackgroundWidget(
        color: String,
        displayMetrics: DisplayMetrics,
        zIndex: Int,
        widgetContext: Widget.WidgetContext
    ): Widget {
        return Widget(
            imageType = Widget.ImageType.COLOR_BACKGROUND,
            imagePath = "",  // Not used for solid color
            solidColor = color,
            backgroundOpacity = 1.0f,  // Default 100% opacity (fully opaque)
            x = (displayMetrics.widthPixels / 2f) - 150f,
            y = (displayMetrics.heightPixels / 2f) - 150f,
            width = 300f,
            height = 300f,
            zIndex = zIndex,
            widgetContext = widgetContext
        )
    }

    /**
     * Create a custom image widget.
     */
    fun createCustomImageWidget(
        imagePath: String,
        displayMetrics: DisplayMetrics,
        zIndex: Int,
        widgetContext: Widget.WidgetContext
    ): Widget {
        return Widget(
            imageType = Widget.ImageType.CUSTOM_IMAGE,
            imagePath = imagePath,
            x = (displayMetrics.widthPixels / 2f) - 150f,
            y = (displayMetrics.heightPixels / 2f) - 150f,
            width = 300f,
            height = 300f,
            zIndex = zIndex,
            widgetContext = widgetContext
        )
    }

    // ========== URI TO FILE PATH CONVERSION ==========

    /**
     * Convert content:// URI to real file path.
     * Uses ContentResolver to query MediaStore.
     */
    private fun getPathFromUri(uri: Uri): String? {
        android.util.Log.d(TAG, "getPathFromUri - Input URI: $uri")
        android.util.Log.d(TAG, "URI scheme: ${uri.scheme}")
        android.util.Log.d(TAG, "URI authority: ${uri.authority}")

        // Handle file:// URIs directly
        if (uri.scheme == "file") {
            return uri.path
        }

        // Handle content:// URIs
        if (uri.scheme == "content") {
            // Try DocumentsContract first (modern approach)
            if (android.provider.DocumentsContract.isDocumentUri(context, uri)) {
                when (uri.authority) {
                    // External storage provider
                    "com.android.externalstorage.documents" -> {
                        val docId = android.provider.DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":")
                        if (split.size >= 2) {
                            val type = split[0]
                            if ("primary".equals(type, ignoreCase = true)) {
                                return "${android.os.Environment.getExternalStorageDirectory()}/${split[1]}"
                            }
                        }
                    }
                    // Downloads provider
                    "com.android.providers.downloads.documents" -> {
                        val docId = android.provider.DocumentsContract.getDocumentId(uri)
                        if (docId.startsWith("raw:")) {
                            return docId.substring(4)
                        }
                    }
                    // Media provider
                    "com.android.providers.media.documents" -> {
                        val docId = android.provider.DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":")
                        if (split.size >= 2) {
                            val type = split[0]
                            val contentUri = when (type) {
                                "image" -> android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                "video" -> android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                "audio" -> android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                else -> null
                            }

                            if (contentUri != null) {
                                val selection = "_id=?"
                                val selectionArgs = arrayOf(split[1])
                                return getDataColumn(contentUri, selection, selectionArgs)
                            }
                        }
                    }
                }
            }

            // Fallback: Try standard MediaStore query
            return getDataColumn(uri, null, null)
        }

        android.util.Log.w(TAG, "Unable to resolve URI to file path")
        return null
    }

    /**
     * Query ContentResolver for the _data column.
     */
    private fun getDataColumn(uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        val column = "_data"
        val projection = arrayOf(column)

        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    cursor.getString(columnIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get data column from URI", e)
            null
        }
    }
}