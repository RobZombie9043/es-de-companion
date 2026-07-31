package com.esde.companion.domain.model

/** How an image-backed widget's content should be fit into its placed bounds. */
enum class ScaleMode {
    Fit,
    Fill,
}

/**
 * Blur and darken post-processing for an image-backed widget. Both knobs are stored as
 * normalized 0f..1f "amount" values - [blurAmount] maps to a fixed max blur radius,
 * [darkenAmount] is used directly as the opacity of a black overlay (useful for muting
 * a background image so an overlaid logo/widget reads more clearly against it). See
 * IMAGE_EFFECTS_MAX_BLUR / DarkenOverlay in WidgetCanvas.kt. Defaults are a no-op so
 * existing persisted widgets deserialize with effects off rather than needing a
 * migration.
 */
data class ImageEffects(
    val blurAmount: Float = 0f,
    val darkenAmount: Float = 0f,
)

/**
 * The catalog of widgets available to place on a canvas. Placement (grid position, size,
 * z-index) deliberately lives on PlacedWidget, not here - a widget's kind/config is
 * independent of where it happens to be put.
 *
 * ColorBackground is the only variant with no image content, so it's the only one
 * without a scaleMode or ImageEffects.
 */
sealed class WidgetType {
    data class SystemLogo(val scaleMode: ScaleMode, val effects: ImageEffects = ImageEffects()) : WidgetType()
    data class SystemMedia(val mediaType: MediaType, val scaleMode: ScaleMode, val effects: ImageEffects = ImageEffects()) : WidgetType()
    data class GameMedia(val mediaType: MediaType, val scaleMode: ScaleMode, val effects: ImageEffects = ImageEffects()) : WidgetType()
    data class ColorBackground(val colorArgb: Long, val alpha: Float) : WidgetType()
}