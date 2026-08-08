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
    /**
     * [logoTransitionMode]/[glintEnabled] are this widget's own per-instance transition/
     * glint config (Configure Widget dialog) - see WidgetContent.kt's WidgetType.logoTransitionMode/
     * glintEnabled extension properties for how these are read uniformly across every
     * logo-style variant, and AnimatedLogoImage for what they drive.
     */
    data class SystemLogo(
        val scaleMode: ScaleMode,
        val effects: ImageEffects = ImageEffects(),
        val logoTransitionMode: LogoTransitionMode = LogoTransitionMode.None,
        val glintEnabled: Boolean = false,
    ) : WidgetType()

    /**
     * A whole-system image sourced from the Custom System Images folder (Settings >
     * Setup): a literal `<systemShortName>.<ext>` file. Falls back to a random FanArt,
     * then a random Screenshot, from ES-DE's own media if no custom file is found - see
     * WidgetContentResolver.
     *
     * [panZoomEnabled] is a per-widget opt-in for a continuous slow Ken Burns-style
     * zoom/pan while displayed - see WidgetType.supportsPanZoom for eligibility (only
     * offered when scaleMode is Fill) and PanZoomImage.kt for the animation itself.
     *
     * [imageTransitionMode] is this widget's own per-instance transition config
     * (Configure Widget dialog) - see WidgetType.imageTransitionMode's extension property
     * kdoc and allowsFadeTransition for the scaleMode-Fit exclusion.
     */
    data class SystemImage(
        val scaleMode: ScaleMode,
        val effects: ImageEffects = ImageEffects(),
        val panZoomEnabled: Boolean = false,
        val imageTransitionMode: ImageTransitionMode = ImageTransitionMode.None,
    ) : WidgetType()

    /**
     * [mediaType] decides whether this instance is logo-style (Marquees, see [isLogoStyle])
     * or opaque backdrop-style - only the fields relevant to whichever it is get read at
     * render/Configure-dialog time (see WidgetContent.kt's extension properties); the
     * others are simply unused for that instance, same reasoning as [SystemLogo] carrying
     * fields [ColorBackground] doesn't need.
     */
    data class SystemMedia(
        val mediaType: MediaType,
        val scaleMode: ScaleMode,
        val effects: ImageEffects = ImageEffects(),
        val panZoomEnabled: Boolean = false,
        val imageTransitionMode: ImageTransitionMode = ImageTransitionMode.None,
        val logoTransitionMode: LogoTransitionMode = LogoTransitionMode.None,
        val glintEnabled: Boolean = false,
    ) : WidgetType()

    /** See [SystemMedia]'s kdoc - same logo-style-vs-opaque field split, just for the
     * Playing/game canvas's media catalog instead of System's. */
    data class GameMedia(
        val mediaType: MediaType,
        val scaleMode: ScaleMode,
        val effects: ImageEffects = ImageEffects(),
        val panZoomEnabled: Boolean = false,
        val imageTransitionMode: ImageTransitionMode = ImageTransitionMode.None,
        val logoTransitionMode: LogoTransitionMode = LogoTransitionMode.None,
        val glintEnabled: Boolean = false,
    ) : WidgetType()

    /**
     * A single image file the user picked directly (via a file picker), independent of
     * whatever system/game is currently browsed - unlike every other image-backed variant
     * above, [path] is fixed, user-chosen, persisted data rather than something re-derived
     * from AppState at resolve time. Available on both the System and Playing canvases
     * since it has no dependency on either's identity. An empty [path] means the widget
     * was added but no image has been picked yet - resolves to WidgetContent.Empty (see
     * WidgetContentResolver). Never logo-style (see [isLogoStyle]), so only carries
     * [imageTransitionMode], not the logo-related fields.
     */
    data class CustomImage(
        val path: String,
        val scaleMode: ScaleMode,
        val effects: ImageEffects = ImageEffects(),
        val panZoomEnabled: Boolean = false,
        val imageTransitionMode: ImageTransitionMode = ImageTransitionMode.None,
    ) : WidgetType()

    data class ColorBackground(val colorArgb: Long, val alpha: Float) : WidgetType()

    /**
     * A game's description, scrolled if it doesn't fit the widget's placed bounds. No
     * scaleMode/ImageEffects - it isn't image-backed, same reasoning as ColorBackground.
     */
    data class GameDescription(
        val fontSizeSp: Float = 16f,
        val textColorArgb: Long = 0xFFFFFFFF,
        val backgroundColorArgb: Long = 0xFF000000,
        val backgroundAlpha: Float = 0.5f,
    ) : WidgetType()
}
