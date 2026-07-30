package com.esde.companion.data.settings

import kotlinx.serialization.Serializable

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

@Serializable
internal sealed class WidgetTypeDto {
    @Serializable
    data class SystemLogo(val scaleMode: String) : WidgetTypeDto()

    @Serializable
    data class SystemMedia(val mediaType: String, val scaleMode: String) : WidgetTypeDto()

    @Serializable
    data class GameMedia(val mediaType: String, val scaleMode: String) : WidgetTypeDto()

    @Serializable
    data class ColorBackground(val colorArgb: Long, val alpha: Float) : WidgetTypeDto()
}