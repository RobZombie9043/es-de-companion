package com.esde.companion.data

import java.util.UUID

data class Widget(
    val id: String = UUID.randomUUID().toString(),
    val imageType: ImageType,
    val imagePath: String,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var zIndex: Int = 0,
    var backgroundOpacity: Float = 0.2f,
    var scaleType: ScaleType = ScaleType.FIT,
    val widgetContext: WidgetContext = WidgetContext.GAME,
    var xPercent: Float? = null,
    var yPercent: Float? = null,
    var widthPercent: Float? = null,
    var heightPercent: Float? = null,
    var solidColor: String? = null  // NEW: Store color as hex string (e.g., "#FF5733")
) {
    enum class ImageType {
        MARQUEE,
        BOX_2D,
        BOX_3D,
        MIX_IMAGE,
        BACK_COVER,
        PHYSICAL_MEDIA,
        SCREENSHOT,
        FANART,
        TITLE_SCREEN,
        SYSTEM_LOGO,
        GAME_DESCRIPTION,
        COLOR_BACKGROUND,
        CUSTOM_IMAGE,
        RANDOM_FANART,
        RANDOM_SCREENSHOT,
        SYSTEM_IMAGE
    }

    enum class WidgetContext {
        GAME,    // Shows in game view
        SYSTEM   // Shows in system view
    }

    enum class ScaleType {
        FIT,      // Fit to container with no cropping (default)
        CROP      // Fill container with cropping (centerCrop)
    }

    /**
     * Convert absolute pixels to percentages based on screen dimensions
     */
    fun toPercentages(screenWidth: Int, screenHeight: Int) {
        xPercent = (x / screenWidth) * 100f
        yPercent = (y / screenHeight) * 100f
        widthPercent = (width / screenWidth) * 100f
        heightPercent = (height / screenHeight) * 100f
    }

    /**
     * Convert percentages to absolute pixels based on screen dimensions
     */
    fun fromPercentages(screenWidth: Int, screenHeight: Int) {
        // Only apply percentages if they exist (for migration)
        if (xPercent != null && yPercent != null && widthPercent != null && heightPercent != null) {
            x = (xPercent!! / 100f) * screenWidth
            y = (yPercent!! / 100f) * screenHeight
            width = (widthPercent!! / 100f) * screenWidth
            height = (heightPercent!! / 100f) * screenHeight
        }
    }
}