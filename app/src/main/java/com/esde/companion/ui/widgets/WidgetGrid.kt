package com.esde.companion.ui.widgets

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.GridDimensions

private val TARGET_CELL_SIZE = 24.dp

/** Derives the widget grid's column/row count from the available canvas size, sized so
 * cells come out roughly square regardless of the screen's actual resolution/aspect
 * ratio - see GridDimensions kdoc. */
fun gridDimensionsFor(
    maxWidth: Dp,
    maxHeight: Dp,
): GridDimensions =
    GridDimensions(
        columns = (maxWidth / TARGET_CELL_SIZE).toInt().coerceAtLeast(1),
        rows = (maxHeight / TARGET_CELL_SIZE).toInt().coerceAtLeast(1),
    )
