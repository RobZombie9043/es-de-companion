package com.esde.companion.data.settings

import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.ImageEffects
import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.NoRatingBehavior
import com.esde.companion.domain.model.PillarboxMode
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.SavedWidgetCanvas
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.model.defaultFallbackMediaType

private fun ScaleMode.toDto() = name

private fun String.toScaleMode() = ScaleMode.valueOf(this)

private fun ImageTransitionMode.toDto() = name

private fun String.toImageTransitionMode() = ImageTransitionMode.valueOf(this)

private fun LogoTransitionMode.toDto() = name

private fun String.toLogoTransitionMode() = LogoTransitionMode.valueOf(this)

private fun NoRatingBehavior.toDto() = name

private fun String.toNoRatingBehavior() = NoRatingBehavior.valueOf(this)

private fun PillarboxMode.toDto() = name

private fun String.toPillarboxMode() = PillarboxMode.valueOf(this)

// See WidgetTypeDto's kdoc for the tri-state reasoning: null means "not present in the
// JSON at all" (old data, or a widget never explicitly reconfigured), the literal string
// "None" means the fallback was explicitly turned off, anything else is a MediaType name.
private const val EXPLICIT_NO_FALLBACK = "None"

private fun MediaType?.toFallbackDto(): String = this?.name ?: EXPLICIT_NO_FALLBACK

private fun String?.toFallbackMediaType(primaryMediaType: MediaType): MediaType? =
    when (this) {
        null -> primaryMediaType.defaultFallbackMediaType()
        EXPLICIT_NO_FALLBACK -> null
        else -> MediaType.valueOf(this)
    }

private fun WidgetType.toDto(): WidgetTypeDto =
    when (this) {
        is WidgetType.SystemLogo ->
            WidgetTypeDto.SystemLogo(
                scaleMode.toDto(),
                effects.blurAmount,
                effects.darkenAmount,
                logoTransitionMode.toDto(),
                glintEnabled,
            )
        is WidgetType.SystemImage ->
            WidgetTypeDto.SystemImage(
                scaleMode.toDto(),
                effects.blurAmount,
                effects.darkenAmount,
                panZoomEnabled,
                imageTransitionMode.toDto(),
            )
        is WidgetType.SystemMedia ->
            WidgetTypeDto.SystemMedia(
                mediaType.name,
                scaleMode.toDto(),
                effects.blurAmount,
                effects.darkenAmount,
                panZoomEnabled,
                imageTransitionMode.toDto(),
                logoTransitionMode.toDto(),
                glintEnabled,
                fallbackMediaType.toFallbackDto(),
            )
        is WidgetType.GameMedia ->
            WidgetTypeDto.GameMedia(
                mediaType.name,
                scaleMode.toDto(),
                effects.blurAmount,
                effects.darkenAmount,
                panZoomEnabled,
                imageTransitionMode.toDto(),
                logoTransitionMode.toDto(),
                glintEnabled,
                fallbackMediaType.toFallbackDto(),
            )
        is WidgetType.CustomImage ->
            WidgetTypeDto.CustomImage(
                path,
                scaleMode.toDto(),
                effects.blurAmount,
                effects.darkenAmount,
                panZoomEnabled,
                imageTransitionMode.toDto(),
            )
        is WidgetType.ColorBackground -> WidgetTypeDto.ColorBackground(colorArgb, alpha)
        is WidgetType.GameDescription ->
            WidgetTypeDto.GameDescription(
                fontSizeSp,
                textColorArgb,
                backgroundColorArgb,
                backgroundAlpha,
            )
        is WidgetType.Rating ->
            WidgetTypeDto.Rating(
                noRatingBehavior.toDto(),
                filledColorArgb,
                outlineColorArgb,
                backgroundColorArgb,
                backgroundAlpha,
            )
        is WidgetType.Video ->
            WidgetTypeDto.Video(
                scaleMode.toDto(),
                audioEnabled,
                delaySeconds,
                pillarboxMode.toDto(),
            )
    }

private fun WidgetTypeDto.toDomain(): WidgetType =
    when (this) {
        is WidgetTypeDto.SystemLogo ->
            WidgetType.SystemLogo(
                scaleMode.toScaleMode(),
                ImageEffects(blurAmount, darkenAmount),
                logoTransitionMode.toLogoTransitionMode(),
                glintEnabled,
            )
        is WidgetTypeDto.SystemImage ->
            WidgetType.SystemImage(
                scaleMode.toScaleMode(),
                ImageEffects(blurAmount, darkenAmount),
                panZoomEnabled,
                imageTransitionMode.toImageTransitionMode(),
            )
        is WidgetTypeDto.SystemMedia -> {
            val resolvedMediaType = MediaType.valueOf(mediaType)
            WidgetType.SystemMedia(
                resolvedMediaType,
                scaleMode.toScaleMode(),
                ImageEffects(blurAmount, darkenAmount),
                panZoomEnabled,
                imageTransitionMode.toImageTransitionMode(),
                logoTransitionMode.toLogoTransitionMode(),
                glintEnabled,
                fallbackMediaType.toFallbackMediaType(resolvedMediaType),
            )
        }
        is WidgetTypeDto.GameMedia -> {
            val resolvedMediaType = MediaType.valueOf(mediaType)
            WidgetType.GameMedia(
                resolvedMediaType,
                scaleMode.toScaleMode(),
                ImageEffects(blurAmount, darkenAmount),
                panZoomEnabled,
                imageTransitionMode.toImageTransitionMode(),
                logoTransitionMode.toLogoTransitionMode(),
                glintEnabled,
                fallbackMediaType.toFallbackMediaType(resolvedMediaType),
            )
        }
        is WidgetTypeDto.CustomImage ->
            WidgetType.CustomImage(
                path,
                scaleMode.toScaleMode(),
                ImageEffects(blurAmount, darkenAmount),
                panZoomEnabled,
                imageTransitionMode.toImageTransitionMode(),
            )
        is WidgetTypeDto.ColorBackground -> WidgetType.ColorBackground(colorArgb, alpha)
        is WidgetTypeDto.GameDescription ->
            WidgetType.GameDescription(fontSizeSp, textColorArgb, backgroundColorArgb, backgroundAlpha)
        is WidgetTypeDto.Rating -> toDomainRating()
        is WidgetTypeDto.Video -> toDomainVideo()
    }

private fun WidgetTypeDto.Rating.toDomainRating(): WidgetType.Rating =
    WidgetType.Rating(
        noRatingBehavior.toNoRatingBehavior(),
        filledColorArgb,
        outlineColorArgb,
        backgroundColorArgb,
        backgroundAlpha,
    )

private fun WidgetTypeDto.Video.toDomainVideo(): WidgetType.Video =
    WidgetType.Video(
        scaleMode.toScaleMode(),
        audioEnabled,
        delaySeconds,
        pillarboxMode.toPillarboxMode(),
    )

private fun PlacedWidget.toDto() =
    PlacedWidgetDto(
        id,
        widgetType.toDto(),
        gridColumn,
        gridRow,
        columnSpan,
        rowSpan,
        zIndex,
    )

private fun PlacedWidgetDto.toDomain() =
    PlacedWidget(
        id,
        widgetType.toDomain(),
        gridColumn,
        gridRow,
        columnSpan,
        rowSpan,
        zIndex,
    )

internal fun List<PlacedWidget>.toDtoList() = map { it.toDto() }

internal fun List<PlacedWidgetDto>.toDomainList() = map { it.toDomain() }

private fun GridDimensions.toDto() = GridDimensionsDto(columns, rows)

private fun GridDimensionsDto.toDomain() = GridDimensions(columns, rows)

internal fun CanvasDto.toDomain() = SavedWidgetCanvas(grid.toDomain(), widgets.toDomainList())

internal fun canvasDtoOf(
    widgets: List<PlacedWidget>,
    grid: GridDimensions,
) = CanvasDto(grid = grid.toDto(), widgets = widgets.toDtoList())
