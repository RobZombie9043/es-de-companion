package com.esde.companion.domain.model

/**
 * A game's rating parsed from its system's gamelist.xml, if any. ES-DE stores <rating> as
 * a decimal string from 0.0 to 1.0 (e.g. "0.800000") - [value] is that raw 0f..1f score,
 * already parsed and clamped; converting it to a 5-star scale is a rendering concern (see
 * WidgetContentResolver's WidgetType.Rating branch), not something this model does itself.
 * [gamelistPath] mirrors GameDescription's field - the specific gamelist.xml [value] was
 * (or would have been) parsed from, null only when no gamelist.xml could be found.
 */
data class GameRating(val value: Float?, val gamelistPath: String? = null)
