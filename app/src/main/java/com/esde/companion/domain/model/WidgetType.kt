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
 * How much a widget's opaque content is corner-rounded (Configure Widget dialog) - see
 * WidgetType.supportsCornerRadius for which variants offer this at all (transparent
 * cutout content - SystemLogo, Marquee-style media - has nothing opaque behind it for
 * rounding to visibly affect, so it's excluded rather than shown-but-inert). The actual
 * dp values these map to live in the ui layer (see ui/widgets/WidgetCornerRadius.kt),
 * matching this project's small, reused set of corner-radius constants elsewhere in the
 * app rather than a free-form dp value.
 */
enum class CornerRadius {
    None,
    Small,
    Medium,
    Large,
}

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
     *
     * No [CornerRadius] field - always transparent cutout content, see
     * WidgetType.supportsCornerRadius.
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
        val cornerRadius: CornerRadius = CornerRadius.None,
    ) : WidgetType()

    /**
     * [mediaType] decides whether this instance is logo-style (Marquees, see [isLogoStyle])
     * or opaque backdrop-style - only the fields relevant to whichever it is get read at
     * render/Configure-dialog time (see WidgetContent.kt's extension properties); the
     * others are simply unused for that instance, same reasoning as [SystemLogo] carrying
     * fields [ColorBackground] doesn't need.
     *
     * [fallbackMediaType] is this widget's own per-instance Fallback Artwork choice
     * (Configure Widget dialog) - see [MediaType.fallbackMediaTypeOptions] for which
     * MediaTypes offer this at all and what their choices are, and
     * resolveMediaWidgetContent for where it's actually applied. Defaults to
     * [MediaType.defaultFallbackMediaType] so a freshly-added widget starts with a
     * sensible fallback already selected, not "None".
     */
    data class SystemMedia(
        val mediaType: MediaType,
        val scaleMode: ScaleMode,
        val effects: ImageEffects = ImageEffects(),
        val panZoomEnabled: Boolean = false,
        val imageTransitionMode: ImageTransitionMode = ImageTransitionMode.None,
        val logoTransitionMode: LogoTransitionMode = LogoTransitionMode.None,
        val glintEnabled: Boolean = false,
        val fallbackMediaType: MediaType? = mediaType.defaultFallbackMediaType(),
        val cornerRadius: CornerRadius = CornerRadius.None,
    ) : WidgetType()

    /** See [SystemMedia]'s kdoc - same logo-style-vs-opaque field split (and the same
     * [fallbackMediaType] Fallback Artwork config), just for the Playing/game canvas's
     * media catalog instead of System's. */
    data class GameMedia(
        val mediaType: MediaType,
        val scaleMode: ScaleMode,
        val effects: ImageEffects = ImageEffects(),
        val panZoomEnabled: Boolean = false,
        val imageTransitionMode: ImageTransitionMode = ImageTransitionMode.None,
        val logoTransitionMode: LogoTransitionMode = LogoTransitionMode.None,
        val glintEnabled: Boolean = false,
        val fallbackMediaType: MediaType? = mediaType.defaultFallbackMediaType(),
        val cornerRadius: CornerRadius = CornerRadius.None,
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
        val cornerRadius: CornerRadius = CornerRadius.None,
    ) : WidgetType()

    data class ColorBackground(
        val colorArgb: Long,
        val alpha: Float,
        val cornerRadius: CornerRadius = CornerRadius.None,
    ) : WidgetType()

    /**
     * A game's description, scrolled if it doesn't fit the widget's placed bounds. No
     * scaleMode/ImageEffects - it isn't image-backed, same reasoning as ColorBackground.
     */
    data class GameDescription(
        val fontSizeSp: Float = 16f,
        val textColorArgb: Long = 0xFFFFFFFF,
        val backgroundColorArgb: Long = 0xFF000000,
        val backgroundAlpha: Float = 0.5f,
        val cornerRadius: CornerRadius = CornerRadius.None,
    ) : WidgetType()

    /**
     * A game's <rating> (ES-DE's own 0.0..1.0 scraped score) rendered as five filled/
     * unfilled stars - see WidgetContentResolver's Rating branch for the 0..1 -> 0..5
     * conversion. [noRatingBehavior] decides what to show when a game has no rating at
     * all (as opposed to a rating of exactly 0, which is a real value and always renders
     * as five unfilled stars regardless of this setting). No scaleMode/ImageEffects - not
     * image-backed, same reasoning as ColorBackground/GameDescription.
     */
    data class Rating(
        val noRatingBehavior: NoRatingBehavior = NoRatingBehavior.Hide,
        val filledColorArgb: Long = 0xFFFFC107,
        val outlineColorArgb: Long = 0xFFFFFFFF,
        val backgroundColorArgb: Long = 0xFF000000,
        val backgroundAlpha: Float = 0.5f,
        val cornerRadius: CornerRadius = CornerRadius.None,
    ) : WidgetType()

    /**
     * A game's video (ES-DE's `videos` media type), playing while the game is browsed -
     * see WidgetContentResolver's Video branch and [WidgetType.supportsPillarbox]/
     * [PillarboxMode] for the Contain-only pillarbox choice. Only ever resolves while
     * AppState.BrowsingGame (never during actual gameplay) - narrower than every other
     * GameMedia-style widget, enforced by the caller supplying WidgetContentResolver's
     * videoLookup, not by this type itself. No ImageEffects - not Coil-backed.
     *
     * [renderAboveUi] opts this specific widget instance out of the ordinary widget-canvas
     * z-order (below FABs/App Dock/App Drawer/Dim/Black covers) and into a second,
     * MainActivity-level layer drawn above literally everything else - matching where the
     * retired full-screen `VideoOverlayScreen` always used to sit. See
     * `ui/widgets/WidgetOverlay.kt`'s `TopLayerVideoWidgets` for the actual rendering, and
     * `WidgetContentView`'s Video branch for why the ordinary canvas layer renders nothing
     * for a widget with this on (avoiding two ExoPlayer instances/composables for the same
     * widget).
     *
     * [loopEnabled] controls whether playback repeats (`Player.REPEAT_MODE_ONE`) or plays
     * once and holds on its final frame (`Player.REPEAT_MODE_OFF`) - see VideoPlayerPool's
     * `borrow` in WidgetVideoContent.kt. Defaults to true, matching this widget's original
     * always-loop behavior before this setting existed.
     */
    data class Video(
        val scaleMode: ScaleMode = ScaleMode.Fit,
        val audioEnabled: Boolean = true,
        val delaySeconds: Int = 0,
        val pillarboxMode: PillarboxMode = PillarboxMode.Black,
        val renderAboveUi: Boolean = false,
        val loopEnabled: Boolean = true,
        val cornerRadius: CornerRadius = CornerRadius.None,
    ) : WidgetType()
}

/** What a Rating widget shows for a game with no <rating> in gamelist.xml at all. See
 * [WidgetType.Rating.noRatingBehavior]. */
enum class NoRatingBehavior {
    Hide,
    ShowEmptyStars,
}

/** How a [WidgetType.Video] widget fills the letterboxed space [ScaleMode.Fit] ("Contain")
 * leaves around a video whose aspect ratio doesn't match its placed bounds - a solid black
 * bar, or leaving the space transparent so whatever's behind the widget shows through. Not
 * offered under [ScaleMode.Fill] ("Cover"), which crops to fill the whole widget and never
 * has empty space - see [WidgetType.supportsPillarbox]. */
enum class PillarboxMode {
    Black,
    Transparent,
}

/** Whether this widget type's Configure dialog should offer the pillarbox picker at all -
 * only [WidgetType.Video] widgets scaled with [ScaleMode.Fit] ever have letterboxed space
 * to fill. Purely structural, mirroring [WidgetType.supportsPanZoom]'s pattern. */
val WidgetType.supportsPillarbox: Boolean
    get() = this is WidgetType.Video && scaleMode == ScaleMode.Fit
