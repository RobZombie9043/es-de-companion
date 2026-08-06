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
    get() = when (this) {
        is WidgetType.SystemLogo -> true
        is WidgetType.GameMedia -> mediaType in TRANSPARENT_MEDIA_TYPES
        is WidgetType.SystemMedia -> mediaType in TRANSPARENT_MEDIA_TYPES
        is WidgetType.SystemImage,
        is WidgetType.CustomImage,
        is WidgetType.ColorBackground,
        is WidgetType.GameDescription,
        -> false
    }

/** Game-view box-art-style media where each new game's art should snap in instantly
 * rather than fade, even when Settings > UI Settings has the global image transition set
 * to Fade - these read as "flipping" to a different game's box art, not a scene changing,
 * so a cross-dissolve between two unrelated pieces of box art looks like a smear rather
 * than a clean cut the way a fade between full-bleed fanart/screenshots does. */
private val INSTANT_ONLY_MEDIA_TYPES = setOf(
    MediaType.Covers,
    MediaType.ThreeDBoxes,
    MediaType.MixImages,
    MediaType.BackCovers,
    MediaType.PhysicalMedia,
)

/**
 * Whether this widget type must always render with an instant (no-fade) transition,
 * overriding the global ImageTransitionMode - see [INSTANT_ONLY_MEDIA_TYPES]. Checked by
 * WidgetContentView before applying imageTransitionMode's fade duration to a non-logo-style
 * [WidgetContent.Image].
 */
val WidgetType.forcesInstantImageTransition: Boolean
    get() = this is WidgetType.GameMedia && mediaType in INSTANT_ONLY_MEDIA_TYPES

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
    get() = when (this) {
        is WidgetType.SystemImage -> scaleMode == ScaleMode.Fill
        is WidgetType.CustomImage -> scaleMode == ScaleMode.Fill
        is WidgetType.SystemMedia -> scaleMode == ScaleMode.Fill && mediaType in PAN_ZOOM_ELIGIBLE_MEDIA_TYPES
        is WidgetType.GameMedia -> scaleMode == ScaleMode.Fill && mediaType in PAN_ZOOM_ELIGIBLE_MEDIA_TYPES
        is WidgetType.SystemLogo, is WidgetType.ColorBackground, is WidgetType.GameDescription -> false
    }

/**
 * Whether pan-zoom should actually render right now - [supportsPanZoom] AND the user's
 * stored per-widget toggle. Re-checked at render time (not just trusting the stored flag)
 * as defense-in-depth against stale persisted data - e.g. a widget's scaleMode switched
 * from Fill to Fit while panZoomEnabled stayed true simply stops animating rather than
 * needing its flag actively cleared, the same reasoning as [forcesInstantImageTransition].
 */
val WidgetType.panZoomActive: Boolean
    get() = supportsPanZoom && when (this) {
        is WidgetType.SystemImage -> panZoomEnabled
        is WidgetType.SystemMedia -> panZoomEnabled
        is WidgetType.GameMedia -> panZoomEnabled
        is WidgetType.CustomImage -> panZoomEnabled
        else -> false
    }

/**
 * FanArt and Screenshots fall back to each other when the primary type is missing - the
 * only two MediaTypes that do. Every other type is exact-or-empty; see widget system
 * design notes for why a general fallback chain per type was deliberately not built.
 */
private val MEDIA_FALLBACKS: Map<MediaType, MediaType> = mapOf(
    MediaType.FanArt to MediaType.Screenshots,
    MediaType.Screenshots to MediaType.FanArt,
)

/** Only these two types fall back further to a generic background image when neither
 * they nor their MEDIA_FALLBACKS partner has a real file - they're the ones used as
 * full-bleed backdrops, where "show nothing" would leave the screen visibly broken
 * rather than just a missing small widget. */
private val BACKGROUND_FALLBACK_ELIGIBLE = setOf(MediaType.FanArt, MediaType.Screenshots)

/**
 * Resolves a MediaType-backed widget's content given a lookup of what's actually
 * available. [lookup] is supplied by the caller (game or system media resolution) so
 * this stays pure and source-agnostic. [fallbackBackgroundAssetPath] is likewise supplied
 * by the caller (an Android asset path) rather than hardcoded here, keeping this function
 * free of platform-specific paths. [effects] is threaded straight through to whichever
 * WidgetContent.Image gets returned - the fallback background image gets the same
 * configured blur/tint as the real media would have, so switching between "real photo
 * found" and "fallback asset" doesn't visibly change the effect the person configured.
 */
fun resolveMediaWidgetContent(
    mediaType: MediaType,
    scaleMode: ScaleMode,
    lookup: (MediaType) -> String?,
    fallbackBackgroundAssetPath: String?,
    effects: ImageEffects = ImageEffects(),
): WidgetContent {
    val path = lookup(mediaType) ?: MEDIA_FALLBACKS[mediaType]?.let(lookup)
    if (path != null) {
        return WidgetContent.Image(path, scaleMode, isTransparentOverlay = mediaType in TRANSPARENT_MEDIA_TYPES, isAsset = false, effects = effects)
    }

    if (mediaType in BACKGROUND_FALLBACK_ELIGIBLE && fallbackBackgroundAssetPath != null) {
        return WidgetContent.Image(fallbackBackgroundAssetPath, scaleMode, isTransparentOverlay = false, isAsset = true, effects = effects)
    }

    return WidgetContent.Empty
}