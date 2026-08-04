package com.esde.companion.ui.widgets

/**
 * Generic full-bleed background shown when a widget/screen has no real image to display -
 * see WidgetContentResolver's BACKGROUND_FALLBACK_ELIGIBLE and WidgetOverlay's
 * WidgetCanvasState.Unmeasured/Disconnected cases. Two variants so it matches the current
 * color scheme rather than always rendering the dark asset in a light theme; [fallbackBackgroundAssetPath]
 * picks between them from the resolved theme at render time (see WidgetCanvas.kt).
 */
internal const val FALLBACK_BACKGROUND_ASSET_DARK = "file:///android_asset/fallback/default_background.webp"
internal const val FALLBACK_BACKGROUND_ASSET_LIGHT = "file:///android_asset/fallback/default_background_light.webp"

/** Kept as the identity marker recorded on [com.esde.companion.domain.model.WidgetContent]
 * for the generic-fallback case - domain/ViewModel code just needs a stable non-null
 * string to signal "use the fallback," not the specific themed path, which is chosen only
 * where theme information is actually available (the Compose layer). */
internal const val FALLBACK_BACKGROUND_ASSET = FALLBACK_BACKGROUND_ASSET_DARK

internal fun fallbackBackgroundAssetPath(isDarkTheme: Boolean): String =
    if (isDarkTheme) FALLBACK_BACKGROUND_ASSET_DARK else FALLBACK_BACKGROUND_ASSET_LIGHT
