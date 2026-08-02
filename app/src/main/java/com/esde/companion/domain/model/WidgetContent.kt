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
}

/** Media types that are transparent overlay-style content rather than an opaque
 * backdrop-style image - these use LogoTransitionMode, not ImageTransitionMode,
 * at render time (see WidgetContent.Image.isTransparentOverlay). */
private val TRANSPARENT_MEDIA_TYPES = setOf(MediaType.Marquees)

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