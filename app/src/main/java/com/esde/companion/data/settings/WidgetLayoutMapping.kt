package com.esde.companion.data.settings

import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.WidgetType

private fun ScaleMode.toDto() = name
private fun String.toScaleMode() = ScaleMode.valueOf(this)

private fun WidgetType.toDto(): WidgetTypeDto = when (this) {
    is WidgetType.SystemLogo -> WidgetTypeDto.SystemLogo(scaleMode.toDto())
    is WidgetType.SystemMedia -> WidgetTypeDto.SystemMedia(mediaType.name, scaleMode.toDto())
    is WidgetType.GameMedia -> WidgetTypeDto.GameMedia(mediaType.name, scaleMode.toDto())
    is WidgetType.ColorBackground -> WidgetTypeDto.ColorBackground(colorArgb, alpha)
}

private fun WidgetTypeDto.toDomain(): WidgetType = when (this) {
    is WidgetTypeDto.SystemLogo -> WidgetType.SystemLogo(scaleMode.toScaleMode())
    is WidgetTypeDto.SystemMedia -> WidgetType.SystemMedia(MediaType.valueOf(mediaType), scaleMode.toScaleMode())
    is WidgetTypeDto.GameMedia -> WidgetType.GameMedia(MediaType.valueOf(mediaType), scaleMode.toScaleMode())
    is WidgetTypeDto.ColorBackground -> WidgetType.ColorBackground(colorArgb, alpha)
}

private fun PlacedWidget.toDto() = PlacedWidgetDto(id, widgetType.toDto(), gridColumn, gridRow, columnSpan, rowSpan, zIndex)

private fun PlacedWidgetDto.toDomain() =
    PlacedWidget(id, widgetType.toDomain(), gridColumn, gridRow, columnSpan, rowSpan, zIndex)

internal fun List<PlacedWidget>.toDtoList() = map { it.toDto() }
internal fun List<PlacedWidgetDto>.toDomainList() = map { it.toDomain() }