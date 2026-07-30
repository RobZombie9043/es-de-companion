package com.esde.companion.domain.model

/** How an image-backed widget's content should be fit into its placed bounds. */
enum class ScaleMode {
    Fit,
    Fill,
}

/**
 * The catalog of widgets available to place on a canvas. Placement (grid position, size,
 * z-index) deliberately lives on PlacedWidget, not here - a widget's kind/config is
 * independent of where it happens to be put.
 *
 * ColorBackground is the only variant with no image content, so it's the only one
 * without a scaleMode.
 */
sealed class WidgetType {
    data class SystemLogo(val scaleMode: ScaleMode) : WidgetType()
    data class SystemMedia(val mediaType: MediaType, val scaleMode: ScaleMode) : WidgetType()
    data class GameMedia(val mediaType: MediaType, val scaleMode: ScaleMode) : WidgetType()
    data class ColorBackground(val colorArgb: Long, val alpha: Float) : WidgetType()
}