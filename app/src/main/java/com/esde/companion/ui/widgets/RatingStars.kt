package com.esde.companion.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val STAR_COUNT = 5
private val STAR_PADDING = 2.dp

/**
 * Renders [starCount] (0f..5f) as five whole stars, each either filled ([filledColorArgb])
 * or unfilled/outline ([outlineColorArgb]) - no half-star rendering, per WidgetContent.Rating's
 * kdoc. Fill is decided by rounding to the nearest whole star, so e.g. 3.4 shows three
 * filled stars and 2.6 shows three.
 *
 * Star size is derived from [modifier]'s resolved constraints, not a fixed value - the
 * smaller of the available height and one-fifth of the available width (minus each star's
 * own padding), so all five stars always fit within the widget's placed bounds at any grid
 * size, and the whole row is centered - both axes - within that space rather than pinned
 * to a corner.
 */
@Composable
fun RatingStars(
    starCount: Float,
    filledColorArgb: Long,
    outlineColorArgb: Long,
    modifier: Modifier = Modifier,
) {
    val filledStars = starCount.roundToInt().coerceIn(0, STAR_COUNT)
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val availableWidthPerStar = (maxWidth - STAR_PADDING * 2 * STAR_COUNT) / STAR_COUNT
        val starSize = minOf(maxHeight, availableWidthPerStar).coerceAtLeast(0.dp)

        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            repeat(STAR_COUNT) { index ->
                val filled = index < filledStars
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarOutline,
                    contentDescription = null,
                    tint = Color(if (filled) filledColorArgb else outlineColorArgb),
                    modifier = Modifier.padding(STAR_PADDING).size(starSize),
                )
            }
        }
    }
}
