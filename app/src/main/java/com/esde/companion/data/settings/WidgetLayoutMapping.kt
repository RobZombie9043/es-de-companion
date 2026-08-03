package com.esde.companion.data.settings

import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.ImageEffects
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.SavedWidgetCanvas
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.WidgetType

private fun ScaleMode.toDto() = name
private fun String.toScaleMode() = ScaleMode.valueOf(this)

private fun WidgetType.toDto(): WidgetTypeDto = when (this) {
    is WidgetType.SystemLogo -> WidgetTypeDto.SystemLogo(scaleMode.toDto(), effects.blurAmount, effects.darkenAmount)
    is WidgetType.SystemImage -> WidgetTypeDto.SystemImage(scaleMode.toDto(), effects.blurAmount, effects.darkenAmount)
    is WidgetType.SystemMedia -> WidgetTypeDto.SystemMedia(mediaType.name, scaleMode.toDto(), effects.blurAmount, effects.darkenAmount)
    is WidgetType.GameMedia -> WidgetTypeDto.GameMedia(mediaType.name, scaleMode.toDto(), effects.blurAmount, effects.darkenAmount)
    is WidgetType.CustomImage -> WidgetTypeDto.CustomImage(path, scaleMode.toDto(), effects.blurAmount, effects.darkenAmount)
    is WidgetType.ColorBackground -> WidgetTypeDto.ColorBackground(colorArgb, alpha)
    is WidgetType.GameDescription -> WidgetTypeDto.GameDescription(fontSizeSp, textColorArgb, backgroundColorArgb, backgroundAlpha)
}

private fun WidgetTypeDto.toDomain(): WidgetType = when (this) {
    is WidgetTypeDto.SystemLogo ->
        WidgetType.SystemLogo(scaleMode.toScaleMode(), ImageEffects(blurAmount, darkenAmount))
    is WidgetTypeDto.SystemImage ->
        WidgetType.SystemImage(scaleMode.toScaleMode(), ImageEffects(blurAmount, darkenAmount))
    is WidgetTypeDto.SystemMedia ->
        WidgetType.SystemMedia(MediaType.valueOf(mediaType), scaleMode.toScaleMode(), ImageEffects(blurAmount, darkenAmount))
    is WidgetTypeDto.GameMedia ->
        WidgetType.GameMedia(MediaType.valueOf(mediaType), scaleMode.toScaleMode(), ImageEffects(blurAmount, darkenAmount))
    is WidgetTypeDto.CustomImage ->
        WidgetType.CustomImage(path, scaleMode.toScaleMode(), ImageEffects(blurAmount, darkenAmount))
    is WidgetTypeDto.ColorBackground -> WidgetType.ColorBackground(colorArgb, alpha)
    is WidgetTypeDto.GameDescription ->
        WidgetType.GameDescription(fontSizeSp, textColorArgb, backgroundColorArgb, backgroundAlpha)
}

private fun PlacedWidget.toDto() = PlacedWidgetDto(id, widgetType.toDto(), gridColumn, gridRow, columnSpan, rowSpan, zIndex)

private fun PlacedWidgetDto.toDomain() =
    PlacedWidget(id, widgetType.toDomain(), gridColumn, gridRow, columnSpan, rowSpan, zIndex)

internal fun List<PlacedWidget>.toDtoList() = map { it.toDto() }
internal fun List<PlacedWidgetDto>.toDomainList() = map { it.toDomain() }

private fun GridDimensions.toDto() = GridDimensionsDto(columns, rows)
private fun GridDimensionsDto.toDomain() = GridDimensions(columns, rows)

internal fun CanvasDto.toDomain() = SavedWidgetCanvas(grid.toDomain(), widgets.toDomainList())
internal fun canvasDtoOf(widgets: List<PlacedWidget>, grid: GridDimensions) =
    CanvasDto(grid = grid.toDto(), widgets = widgets.toDtoList())