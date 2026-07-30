package com.esde.companion.domain.model

sealed class WidgetContent {
    data object Empty : WidgetContent()

    /**
     * [crossfade] is false for transparent overlay-style media (currently just
     * Marquees) - see CrossfadeAsyncImage's kdoc and CLAUDE.md's note on the equivalent
     * fixed-display logo case. [isAsset] distinguishes a real on-disk media file (wrapped
     * in File(...) before being passed to Coil) from the bundled fallback background
     * asset (an app-asset path, passed to Coil as a raw string) - see
     * resolveMediaWidgetContent.
     */
    data class Image(
        val path: String,
        val scaleMode: ScaleMode,
        val crossfade: Boolean,
        val isAsset: Boolean,
    ) : WidgetContent()

    data class SystemLogoAsset(val assetPath: String, val scaleMode: ScaleMode) : WidgetContent()
    data class Color(val colorArgb: Long, val alpha: Float) : WidgetContent()
}

/** Media types that are transparent overlay-style content rather than an opaque
 * backdrop-style image - these must not be crossfaded (see WidgetContent.Image). */
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
 * free of platform-specific paths.
 */
fun resolveMediaWidgetContent(
    mediaType: MediaType,
    scaleMode: ScaleMode,
    lookup: (MediaType) -> String?,
    fallbackBackgroundAssetPath: String?,
): WidgetContent {
    val path = lookup(mediaType) ?: MEDIA_FALLBACKS[mediaType]?.let(lookup)
    if (path != null) {
        return WidgetContent.Image(path, scaleMode, crossfade = mediaType !in TRANSPARENT_MEDIA_TYPES, isAsset = false)
    }

    if (mediaType in BACKGROUND_FALLBACK_ELIGIBLE && fallbackBackgroundAssetPath != null) {
        return WidgetContent.Image(fallbackBackgroundAssetPath, scaleMode, crossfade = true, isAsset = true)
    }

    return WidgetContent.Empty
}