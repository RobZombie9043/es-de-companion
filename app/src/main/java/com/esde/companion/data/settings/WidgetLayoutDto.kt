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
 * for GameDescription's defaults, mirroring WidgetType.GameDescription's own defaults, and
 * for panZoomEnabled defaulting to false (see WidgetType.panZoomEnabled). imageTransitionMode/
 * logoTransitionMode/glintEnabled follow the same pattern for the per-widget transition/glint
 * config that used to live in global Settings.
 *
 * fallbackMediaType on SystemMedia/GameMedia is a tri-state string, not a plain optional
 * MediaType name - see WidgetLayoutMapping's toFallbackMediaTypeDto/toFallbackMediaType
 * for why: `null` (the key absent entirely, e.g. JSON from before this feature existed)
 * means "apply MediaType.defaultFallbackMediaType()", the literal string "None" means the
 * fallback was explicitly turned off, and anything else is a MediaType enum name.
 */
@Serializable
internal sealed class WidgetTypeDto {
    @Serializable
    data class SystemLogo(
        val scaleMode: String,
        val blurAmount: Float = 0f,
        val darkenAmount: Float = 0f,
        val logoTransitionMode: String = "None",
        val glintEnabled: Boolean = false,
    ) : WidgetTypeDto()

    @Serializable
    data class SystemImage(
        val scaleMode: String,
        val blurAmount: Float = 0f,
        val darkenAmount: Float = 0f,
        val panZoomEnabled: Boolean = false,
        val imageTransitionMode: String = "None",
    ) : WidgetTypeDto()

    @Serializable
    data class SystemMedia(
        val mediaType: String,
        val scaleMode: String,
        val blurAmount: Float = 0f,
        val darkenAmount: Float = 0f,
        val panZoomEnabled: Boolean = false,
        val imageTransitionMode: String = "None",
        val logoTransitionMode: String = "None",
        val glintEnabled: Boolean = false,
        val fallbackMediaType: String? = null,
    ) : WidgetTypeDto()

    @Serializable
    data class GameMedia(
        val mediaType: String,
        val scaleMode: String,
        val blurAmount: Float = 0f,
        val darkenAmount: Float = 0f,
        val panZoomEnabled: Boolean = false,
        val imageTransitionMode: String = "None",
        val logoTransitionMode: String = "None",
        val glintEnabled: Boolean = false,
        val fallbackMediaType: String? = null,
    ) : WidgetTypeDto()

    @Serializable
    data class CustomImage(
        val path: String,
        val scaleMode: String,
        val blurAmount: Float = 0f,
        val darkenAmount: Float = 0f,
        val panZoomEnabled: Boolean = false,
        val imageTransitionMode: String = "None",
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

    @Serializable
    data class Rating(
        val noRatingBehavior: String = "Hide",
        val filledColorArgb: Long = 0xFFFFC107,
        val outlineColorArgb: Long = 0xFFFFFFFF,
        val backgroundColorArgb: Long = 0xFF000000,
        val backgroundAlpha: Float = 0.5f,
    ) : WidgetTypeDto()

    @Serializable
    data class Video(
        val scaleMode: String,
        val audioEnabled: Boolean = true,
        val delaySeconds: Int = 0,
        val pillarboxMode: String = "Black",
        val renderAboveUi: Boolean = false,
        val loopEnabled: Boolean = true,
    ) : WidgetTypeDto()
}
