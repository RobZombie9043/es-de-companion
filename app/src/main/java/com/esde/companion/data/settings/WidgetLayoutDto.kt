package com.esde.companion.data.settings

import kotlinx.serialization.Serializable

/**
 * Wraps the persisted widget list together with the grid dimensions it was saved under,
 * so it can be rescaled if loaded on a differently-sized grid (see SavedWidgetCanvas,
 * rescaleWidgetsToGrid). This is a JSON *object* - FileWidgetLayoutRepository falls back
 * to decoding the pre-existing bare-array format (just List<PlacedWidgetDto>) for data
 * persisted before this field existed.
 */
@Serializable
internal data class CanvasDto(
    val grid: GridDimensionsDto,
    val widgets: List<PlacedWidgetDto>,
)

@Serializable
internal data class GridDimensionsDto(
    val columns: Int,
    val rows: Int,
)

@Serializable
internal data class PlacedWidgetDto(
    val id: String,
    val widgetType: WidgetTypeDto,
    val gridColumn: Int,
    val gridRow: Int,
    val columnSpan: Int,
    val rowSpan: Int,
    val zIndex: Int,
)

/**
 * blurAmount/darkenAmount default to the same no-op values as
 * ImageEffects()'s own defaults, so JSON persisted before this feature existed (missing
 * these keys entirely) decodes as "no effect" rather than needing a migration. Same idea
 * for GameDescription's defaults, mirroring WidgetType.GameDescription's own defaults.
 */
@Serializable
internal sealed class WidgetTypeDto {
    @Serializable
    data class SystemLogo(
        val scaleMode: String,
        val blurAmount: Float = 0f,
        val darkenAmount: Float = 0f,
    ) : WidgetTypeDto()

    @Serializable
    data class SystemImage(
        val scaleMode: String,
        val blurAmount: Float = 0f,
        val darkenAmount: Float = 0f,
    ) : WidgetTypeDto()

    @Serializable
    data class SystemMedia(
        val mediaType: String,
        val scaleMode: String,
        val blurAmount: Float = 0f,
        val darkenAmount: Float = 0f,
    ) : WidgetTypeDto()

    @Serializable
    data class GameMedia(
        val mediaType: String,
        val scaleMode: String,
        val blurAmount: Float = 0f,
        val darkenAmount: Float = 0f,
    ) : WidgetTypeDto()

    @Serializable
    data class ColorBackground(val colorArgb: Long, val alpha: Float) : WidgetTypeDto()

    @Serializable
    data class GameDescription(
        val fontSizeSp: Float = 16f,
        val textColorArgb: Long = 0xFFFFFFFF,
        val backgroundColorArgb: Long = 0xFF000000,
        val backgroundAlpha: Float = 0.5f,
    ) : WidgetTypeDto()
}