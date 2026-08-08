package com.esde.companion.domain.model

import kotlin.math.roundToInt

/**
 * A fresh install's starting canvas for [stateGroup], sized to [grid] - mirrors what the
 * fixed backdrop+overlay display showed before the widget system replaced it. The logo/
 * marquee widget is centered and sized as a fraction of the *smaller* grid dimension so
 * it stays square-looking and reasonably proportioned across very different aspect ratios.
 */
fun defaultCanvas(
    stateGroup: StateGroup,
    grid: GridDimensions,
): List<PlacedWidget> {
    val logoColumnSpan = (grid.columns * LOGO_WIDTH_FRACTION).roundToInt().coerceAtLeast(1)
    val logoRowSpan = (grid.rows * LOGO_HEIGHT_FRACTION).roundToInt().coerceAtLeast(1)
    val logoColumn = (grid.columns - logoColumnSpan) / 2
    val logoRow = (grid.rows - logoRowSpan) / 2

    return when (stateGroup) {
        StateGroup.System ->
            listOf(
                PlacedWidget(
                    id = "default-system-fanart",
                    widgetType =
                        WidgetType.SystemImage(
                            ScaleMode.Fill,
                            ImageEffects(darkenAmount = DEFAULT_FANART_DARKEN),
                            panZoomEnabled = true,
                        ),
                    gridColumn = 0,
                    gridRow = 0,
                    columnSpan = grid.columns,
                    rowSpan = grid.rows,
                    zIndex = 0,
                ),
                PlacedWidget(
                    id = "default-system-logo",
                    widgetType =
                        WidgetType.SystemLogo(
                            ScaleMode.Fit,
                            logoTransitionMode = LogoTransitionMode.Slide,
                            glintEnabled = true,
                        ),
                    gridColumn = logoColumn,
                    gridRow = logoRow,
                    columnSpan = logoColumnSpan,
                    rowSpan = logoRowSpan,
                    zIndex = 1,
                ),
            )

        StateGroup.Playing ->
            listOf(
                PlacedWidget(
                    id = "default-playing-fanart",
                    widgetType =
                        WidgetType.GameMedia(
                            MediaType.FanArt,
                            ScaleMode.Fill,
                            ImageEffects(darkenAmount = DEFAULT_FANART_DARKEN),
                            panZoomEnabled = true,
                        ),
                    gridColumn = 0,
                    gridRow = 0,
                    columnSpan = grid.columns,
                    rowSpan = grid.rows,
                    zIndex = 0,
                ),
                PlacedWidget(
                    id = "default-playing-marquee",
                    widgetType =
                        WidgetType.GameMedia(
                            MediaType.Marquees,
                            ScaleMode.Fit,
                            logoTransitionMode = LogoTransitionMode.Slide,
                            glintEnabled = true,
                        ),
                    gridColumn = logoColumn,
                    gridRow = logoRow,
                    columnSpan = logoColumnSpan,
                    rowSpan = logoRowSpan,
                    zIndex = 1,
                ),
            )
    }
}

// 14/22 and 7/19 - matches the size the logo/marquee widget ends up at by default.
private const val LOGO_WIDTH_FRACTION = 0.6363636363636364
private const val LOGO_HEIGHT_FRACTION = 0.3684210526315789

// Mutes the default background image slightly so the overlaid logo/marquee reads more
// clearly against it out of the box - see ImageEffects.darkenAmount.
private const val DEFAULT_FANART_DARKEN = 0.1f
