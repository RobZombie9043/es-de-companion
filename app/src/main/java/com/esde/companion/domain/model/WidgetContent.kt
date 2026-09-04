package com.esde.companion.domain.model

sealed class WidgetContent {
    data object Empty : WidgetContent()

    /**
     * [isTransparentOverlay] is true for transparent overlay-style media (currently
     * just Marquees, and custom system logos) - see CrossfadeAsyncImage's kdoc and
     * CLAUDE.md's note on the equivalent fixed-display logo case. This is purely a
     * content-classification fact (what kind of image this is), decided once at resolve
     * time; it decides which of ImageTransitionMode/LogoTransitionMode applies at
     * render time (see WidgetContentView), not whether/how it animates - that's a
     * separate, user-configurable Settings decision. [isAsset] distinguishes a real
     * on-disk media file (wrapped in File(...) before being passed to Coil) from the
     * bundled fallback background asset (an app-asset path, passed to Coil as a raw
     * string) - see resolveMediaWidgetContent. [effects] carries the widget's
     * configured blur/tint through to rendering (see WidgetContentView).
     */
    data class Image(
        val path: String,
        val scaleMode: ScaleMode,
        val isTransparentOverlay: Boolean,
        val isAsset: Boolean,
        val effects: ImageEffects = ImageEffects(),
    ) : WidgetContent()

    data class SystemLogoAsset(
        val assetPath: String,
        val scaleMode: ScaleMode,
        val effects: ImageEffects = ImageEffects(),
    ) : WidgetContent()

    data class Color(val colorArgb: Long, val alpha: Float) : WidgetContent()

    data class Text(
        val text: String,
        val fontSizeSp: Float,
        val textColorArgb: Long,
        val backgroundColorArgb: Long,
        val backgroundAlpha: Float,
    ) : WidgetContent()

    /**
     * Shown by a logo-style widget (system logo, game/system marquee - see [isLogoStyle])
     * when no actual logo/marquee image is available - the system or game's plain display
     * name, so the widget reads as "empty content" rather than a literal blank rectangle.
     * Deliberately not [Text]: that type's font/color/background fields are a
     * user-configurable style meant for the GameDescription widget, whereas this has no
     * per-widget configuration - it always renders with WidgetContentView's own fixed
     * fallback styling.
     */
    data class NameFallback(val text: String) : WidgetContent()

    /** [starCount] is 0f..5f, already converted from the game's raw 0f..1f gamelist.xml
     * rating - see WidgetContentResolver's Rating branch. Star fill is whole-star only
     * (no half-star rendering), decided by rounding - see RatingStars. */
    data class Rating(
        val starCount: Float,
        val filledColorArgb: Long,
        val outlineColorArgb: Long,
        val backgroundColorArgb: Long,
        val backgroundAlpha: Float,
    ) : WidgetContent()

    /** A resolved game video path, ready to play - see WidgetContentResolver's Video
     * branch. Fields mirror [WidgetType.Video] exactly (this is a snapshot of that config
     * plus the resolved [path]), threaded straight through to WidgetVideoContent. */
    data class Video(
        val path: String,
        val scaleMode: ScaleMode,
        val audioEnabled: Boolean,
        val delaySeconds: Int,
        val pillarboxMode: PillarboxMode,
        val renderAboveUi: Boolean,
        val loopEnabled: Boolean,
        val cornerRadius: CornerRadius,
    ) : WidgetContent()

    /**
     * A resolved RetroAchievements progress snapshot for the current game - see
     * WidgetContentResolver's AchievementSummary branch. [state] carries Loading/Unavailable/
     * Loaded (see [AchievementSummaryWidgetState]'s kdoc) - raw numeric fields inside `Loaded`
     * rather than a pre-formatted string, formatting happens at render time, same layering as
     * `AchievementStatsHeader` in `ui/retroachievements/RetroAchievementsSummaryContent.kt`.
     * Style fields mirror [WidgetType.AchievementSummary] exactly (this is a snapshot of that
     * config plus the resolved [state]).
     */
    data class AchievementSummary(
        val state: AchievementSummaryWidgetState,
        val fontSizeSp: Float,
        val textColorArgb: Long,
        val backgroundColorArgb: Long,
        val backgroundAlpha: Float,
        val cornerRadius: CornerRadius,
    ) : WidgetContent()
}

/** Media types that are transparent overlay-style content rather than an opaque
 * backdrop-style image - these use LogoTransitionMode, not ImageTransitionMode,
 * at render time (see WidgetContent.Image.isTransparentOverlay). */
private val TRANSPARENT_MEDIA_TYPES = setOf(MediaType.Marquees)

/**
 * Whether widgets of this type always render through AnimatedLogoImage/LogoTransitionMode,
 * even when nothing currently resolves for them (missing system logo asset, a game with
 * no marquee, etc). WidgetContentView routes these through a single AnimatedLogoImage call
 * site regardless of [WidgetContent.Empty] vs a real path, so that composable's identity -
 * and therefore any in-flight transition state - survives toggling between "found" and
 * "not found" as the browsed system/game changes. Without this, a widget going from blank
 * to a real logo would remount AnimatedLogoImage fresh and skip its enter animation.
 */
val WidgetType.isLogoStyle: Boolean
    get() =
        when (this) {
            is WidgetType.SystemLogo -> true
            is WidgetType.GameMedia -> mediaType in TRANSPARENT_MEDIA_TYPES
            is WidgetType.SystemMedia -> mediaType in TRANSPARENT_MEDIA_TYPES
            is WidgetType.SystemImage,
            is WidgetType.CustomImage,
            is WidgetType.ColorBackground,
            is WidgetType.GameDescription,
            is WidgetType.Rating,
            is WidgetType.Video,
            is WidgetType.AchievementSummary,
            -> false
        }

/**
 * Whether this widget type's Configure dialog should offer the Rounded Corners picker at
 * all - every widget type except [WidgetType.SystemLogo], and except [WidgetType.SystemMedia]/
 * [WidgetType.GameMedia] instances that are logo-style (Marquees) - see [isLogoStyle].
 * Purely structural, mirroring [supportsPanZoom]'s pattern: a transparent cutout logo/
 * marquee has nothing opaque behind it for rounding to visibly affect, so it's hidden
 * entirely rather than shown-but-inert.
 */
val WidgetType.supportsCornerRadius: Boolean
    get() = this !is WidgetType.SystemLogo && !isLogoStyle

/**
 * This widget instance's own per-widget [CornerRadius] (Configure Widget dialog) - see
 * [supportsCornerRadius] for which variants/configurations actually offer this. Re-checked
 * at render time (not just trusting the stored value) as defense-in-depth against stale
 * persisted data, e.g. a SystemMedia widget's mediaType switched to Marquees after being
 * configured with rounded corners - same reasoning as [panZoomActive].
 */
val WidgetType.cornerRadius: CornerRadius
    get() =
        when (this) {
            is WidgetType.SystemLogo -> CornerRadius.None
            is WidgetType.SystemImage -> cornerRadius
            is WidgetType.SystemMedia -> if (isLogoStyle) CornerRadius.None else cornerRadius
            is WidgetType.GameMedia -> if (isLogoStyle) CornerRadius.None else cornerRadius
            is WidgetType.CustomImage -> cornerRadius
            is WidgetType.ColorBackground -> cornerRadius
            is WidgetType.GameDescription -> cornerRadius
            is WidgetType.Rating -> cornerRadius
            is WidgetType.Video -> cornerRadius
            is WidgetType.AchievementSummary -> cornerRadius
        }

/** Game-view box-art-style media where each new game's art should snap in instantly
 * rather than fade, even when the widget's own per-widget imageTransitionMode (Configure
 * Widget dialog) is set to Fade - these read as "flipping" to a different game's box art,
 * not a scene changing, so a cross-dissolve between two unrelated pieces of box art looks
 * like a smear rather than a clean cut the way a fade between full-bleed fanart/screenshots
 * does. */
private val INSTANT_ONLY_MEDIA_TYPES =
    setOf(
        MediaType.Covers,
        MediaType.ThreeDBoxes,
        MediaType.MixImages,
        MediaType.BackCovers,
        MediaType.PhysicalMedia,
    )

/**
 * Whether this widget type must always render with an instant (no-fade) transition,
 * overriding the widget's own per-widget imageTransitionMode - see [INSTANT_ONLY_MEDIA_TYPES].
 * Checked by WidgetContentView before applying imageTransitionMode's fade duration to a
 * non-logo-style [WidgetContent.Image].
 */
val WidgetType.forcesInstantImageTransition: Boolean
    get() = this is WidgetType.GameMedia && mediaType in INSTANT_ONLY_MEDIA_TYPES

/**
 * This widget instance's own per-widget [ImageTransitionMode] (Configure Widget dialog),
 * for whichever [WidgetType] variants are opaque/backdrop-style - see
 * [supportsImageTransition] for which variants and configurations actually use this at
 * render/Configure-dialog time; irrelevant instances (logo-style, non-image) simply read
 * [ImageTransitionMode.None] here without it ever being displayed or applied.
 */
val WidgetType.imageTransitionMode: ImageTransitionMode
    get() =
        when (this) {
            is WidgetType.SystemImage -> imageTransitionMode
            is WidgetType.SystemMedia -> imageTransitionMode
            is WidgetType.GameMedia -> imageTransitionMode
            is WidgetType.CustomImage -> imageTransitionMode
            else -> ImageTransitionMode.None
        }

/**
 * This widget instance's own per-widget [LogoTransitionMode] (Configure Widget dialog),
 * for whichever [WidgetType] variants are logo-style - see [WidgetType.isLogoStyle].
 */
val WidgetType.logoTransitionMode: LogoTransitionMode
    get() =
        when (this) {
            is WidgetType.SystemLogo -> logoTransitionMode
            is WidgetType.SystemMedia -> logoTransitionMode
            is WidgetType.GameMedia -> logoTransitionMode
            else -> LogoTransitionMode.None
        }

/**
 * This widget instance's own per-widget Logo Glint toggle (Configure Widget dialog), for
 * whichever [WidgetType] variants are logo-style - see [WidgetType.isLogoStyle].
 */
val WidgetType.glintEnabled: Boolean
    get() =
        when (this) {
            is WidgetType.SystemLogo -> glintEnabled
            is WidgetType.SystemMedia -> glintEnabled
            is WidgetType.GameMedia -> glintEnabled
            else -> false
        }

/**
 * Whether this widget type's Configure dialog should offer the Image Transitions picker
 * at all - purely structural, mirroring [supportsPanZoom]'s pattern. Excludes logo-style
 * content (routed through Logo Transitions/AnimatedLogoImage instead, see [isLogoStyle])
 * and [forcesInstantImageTransition] box-art-style GameMedia (permanently locked to
 * instant with zero user choice, ever - hidden entirely rather than shown-but-disabled,
 * same reasoning as [supportsPanZoom] hiding rather than graying out).
 */
val WidgetType.supportsImageTransition: Boolean
    get() =
        when (this) {
            is WidgetType.SystemImage, is WidgetType.CustomImage -> true
            is WidgetType.SystemMedia -> !isLogoStyle
            is WidgetType.GameMedia -> !isLogoStyle && !forcesInstantImageTransition
            is WidgetType.SystemLogo,
            is WidgetType.ColorBackground,
            is WidgetType.GameDescription,
            is WidgetType.Rating,
            is WidgetType.Video,
            is WidgetType.AchievementSummary,
            -> false
        }

/**
 * Whether Fade is currently selectable within the Image Transitions picker - unlike
 * [supportsImageTransition] (which decides whether the picker exists at all), this is
 * reactive to the widget's own [ScaleMode]: a letterboxed ([ScaleMode.Fit]) image swap
 * reads as a hard content change rather than a scene transition, so Fade is excluded
 * whenever Fit is selected and re-offered the moment the person switches back to Fill -
 * see WidgetCanvas.kt's [imageTransitionActive] for how a stale Fade value left over from
 * before a Fit switch is ignored rather than needing to be actively cleared.
 */
val WidgetType.allowsFadeTransition: Boolean
    get() =
        when (this) {
            is WidgetType.SystemImage -> scaleMode == ScaleMode.Fill
            is WidgetType.CustomImage -> scaleMode == ScaleMode.Fill
            is WidgetType.SystemMedia -> scaleMode == ScaleMode.Fill
            is WidgetType.GameMedia -> scaleMode == ScaleMode.Fill
            else -> false
        }

/**
 * The [ImageTransitionMode] that should actually apply at render time -
 * [WidgetType.imageTransitionMode] unless this instance can't use Fade right now
 * ([forcesInstantImageTransition] or [allowsFadeTransition] is false), in which case it's
 * forced to [ImageTransitionMode.None] regardless of the stored value. Checked at render
 * time (not just trusting the stored value) as defense-in-depth against stale persisted
 * data, the same reasoning as [panZoomActive].
 */
val WidgetType.imageTransitionActive: ImageTransitionMode
    get() = if (forcesInstantImageTransition || !allowsFadeTransition) ImageTransitionMode.None else imageTransitionMode

/** Media types eligible for the pan-zoom ambient animation - full-bleed backdrop-style
 * media only. Explicitly enumerated (rather than derived by subtracting
 * TRANSPARENT_MEDIA_TYPES/INSTANT_ONLY_MEDIA_TYPES from MediaType.entries) so a future
 * catalog addition doesn't silently become pan-zoom-eligible without a deliberate choice. */
private val PAN_ZOOM_ELIGIBLE_MEDIA_TYPES = setOf(MediaType.FanArt, MediaType.Screenshots, MediaType.TitleScreens)

/**
 * Whether this widget type's Configure dialog should offer the pan-zoom toggle at all -
 * purely structural (ignores the widget's own stored [WidgetType.panZoomEnabled]-style
 * flag). Excludes SystemLogo/Marquees (logo-style, rendered via AnimatedLogoImage, which
 * never applies pan-zoom), non-image variants, and [ScaleMode.Fit]-scaled widgets - a
 * letterboxed image has empty bars that would visibly pan/zoom along with the photo,
 * which looks broken rather than cinematic, so pan-zoom is Fill-only.
 */
val WidgetType.supportsPanZoom: Boolean
    get() =
        when (this) {
            is WidgetType.SystemImage -> scaleMode == ScaleMode.Fill
            is WidgetType.CustomImage -> scaleMode == ScaleMode.Fill
            is WidgetType.SystemMedia -> scaleMode == ScaleMode.Fill && mediaType in PAN_ZOOM_ELIGIBLE_MEDIA_TYPES
            is WidgetType.GameMedia -> scaleMode == ScaleMode.Fill && mediaType in PAN_ZOOM_ELIGIBLE_MEDIA_TYPES
            is WidgetType.SystemLogo,
            is WidgetType.ColorBackground,
            is WidgetType.GameDescription,
            is WidgetType.Rating,
            is WidgetType.Video,
            is WidgetType.AchievementSummary,
            -> false
        }

/**
 * Whether pan-zoom should actually render right now - [supportsPanZoom] AND the user's
 * stored per-widget toggle. Re-checked at render time (not just trusting the stored flag)
 * as defense-in-depth against stale persisted data - e.g. a widget's scaleMode switched
 * from Fill to Fit while panZoomEnabled stayed true simply stops animating rather than
 * needing its flag actively cleared, the same reasoning as [forcesInstantImageTransition].
 */
val WidgetType.panZoomActive: Boolean
    get() =
        supportsPanZoom &&
            when (this) {
                is WidgetType.SystemImage -> panZoomEnabled
                is WidgetType.SystemMedia -> panZoomEnabled
                is WidgetType.GameMedia -> panZoomEnabled
                is WidgetType.CustomImage -> panZoomEnabled
                else -> false
            }

/**
 * The Fallback Artwork options a Configure Widget dialog should offer for a widget of
 * this [MediaType], in display order - the primary type is never included (it wouldn't
 * be a "fallback"), and a trailing `null` always represents the explicit "None" choice.
 * Empty for every MediaType without a sensible fallback (the dialog hides the picker
 * entirely in that case - see [WidgetType.supportsFallbackArtwork]).
 *
 * FanArt and Screenshots fall back to each other, and ThreeDBoxes falls back to Covers
 * (Box Cover) - the only three MediaTypes with a configurable fallback; every other type
 * is exact-or-empty, see widget system design notes for why a general fallback chain per
 * type was deliberately not built.
 */
fun MediaType.fallbackMediaTypeOptions(): List<MediaType?> =
    when (this) {
        MediaType.FanArt -> listOf(MediaType.Screenshots, null)
        MediaType.Screenshots -> listOf(MediaType.FanArt, null)
        MediaType.ThreeDBoxes -> listOf(MediaType.Covers, null)
        else -> emptyList()
    }

/**
 * The Fallback Artwork default for a freshly-created widget of this [MediaType] - the
 * first (non-"None") entry of [fallbackMediaTypeOptions]. Also what a widget persisted
 * before this feature existed resolves to on load (its DTO's fallbackMediaType field is
 * simply absent from old JSON) - see WidgetLayoutMapping, this is deliberately the same
 * default both times so pre-existing FanArt/Screenshots widgets keep behaving exactly as
 * they always did (previously a fixed, non-configurable fallback) rather than silently
 * losing it.
 */
fun MediaType.defaultFallbackMediaType(): MediaType? = fallbackMediaTypeOptions().firstOrNull()

/**
 * Whether this widget type's Configure dialog should offer the Fallback Artwork picker
 * at all - only SystemMedia/GameMedia instances whose [MediaType] has any
 * [fallbackMediaTypeOptions]. Purely structural, mirroring [supportsPanZoom]'s pattern.
 */
val WidgetType.supportsFallbackArtwork: Boolean
    get() =
        when (this) {
            is WidgetType.SystemMedia -> mediaType.fallbackMediaTypeOptions().isNotEmpty()
            is WidgetType.GameMedia -> mediaType.fallbackMediaTypeOptions().isNotEmpty()
            else -> false
        }

/**
 * This widget instance's own per-widget Fallback Artwork choice (Configure Widget
 * dialog) - see [supportsFallbackArtwork] for which variants/mediaTypes actually offer
 * this. `null` means no fallback: either "None" was explicitly chosen, or this instance
 * doesn't support Fallback Artwork at all.
 */
val WidgetType.fallbackMediaType: MediaType?
    get() =
        when (this) {
            is WidgetType.SystemMedia -> fallbackMediaType
            is WidgetType.GameMedia -> fallbackMediaType
            else -> null
        }

/** Only these two types fall back further to a generic background image when neither
 * they nor their [fallbackMediaType] has a real file - they're the ones used as
 * full-bleed backdrops, where "show nothing" would leave the screen visibly broken
 * rather than just a missing small widget. Independent of the per-widget Fallback
 * Artwork config - applies purely based on the primary [MediaType], even if that
 * widget's own fallback is set to "None". */
private val BACKGROUND_FALLBACK_ELIGIBLE = setOf(MediaType.FanArt, MediaType.Screenshots)

/**
 * The two fallback sources [resolveMediaWidgetContent] can use when [MediaType][mediaType]
 * itself doesn't resolve, bundled together purely to keep that function's parameter count
 * down. [mediaType] is the widget's own per-instance Fallback Artwork choice (see
 * [WidgetType.fallbackMediaType]); [backgroundAssetPath] is the generic background asset
 * (an Android asset path, supplied by the caller rather than hardcoded here to keep this
 * file platform-path-free) - only ever tried for [BACKGROUND_FALLBACK_ELIGIBLE] media types.
 */
data class MediaWidgetFallback(
    val mediaType: MediaType? = null,
    val backgroundAssetPath: String? = null,
)

/**
 * Resolves a MediaType-backed widget's content given a lookup of what's actually
 * available. [lookup] is supplied by the caller (game or system media resolution) so
 * this stays pure and source-agnostic. [effects] is threaded straight through to
 * whichever WidgetContent.Image gets returned - the fallback background image gets the
 * same configured blur/tint as the real media would have, so switching between "real
 * photo found" and "fallback asset" doesn't visibly change the effect the person
 * configured. See [MediaWidgetFallback] for [fallback]'s two-tier meaning.
 */
fun resolveMediaWidgetContent(
    mediaType: MediaType,
    scaleMode: ScaleMode,
    lookup: (MediaType) -> String?,
    effects: ImageEffects = ImageEffects(),
    fallback: MediaWidgetFallback = MediaWidgetFallback(),
): WidgetContent {
    val path = lookup(mediaType) ?: fallback.mediaType?.let(lookup)
    if (path != null) {
        return WidgetContent.Image(
            path,
            scaleMode,
            isTransparentOverlay = mediaType in TRANSPARENT_MEDIA_TYPES,
            isAsset = false,
            effects = effects,
        )
    }

    val backgroundPath = fallback.backgroundAssetPath.takeIf { mediaType in BACKGROUND_FALLBACK_ELIGIBLE }
    return if (backgroundPath != null) {
        WidgetContent.Image(backgroundPath, scaleMode, isTransparentOverlay = false, isAsset = true, effects = effects)
    } else {
        WidgetContent.Empty
    }
}
