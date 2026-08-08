package com.esde.companion.domain.model

/** The widget grid's size for the current display, in cells. Computed from the actual
 * screen dp size at a fixed target cell size - see WidgetGrid.kt (ui layer) - so cells
 * come out roughly square regardless of the device's resolution/aspect ratio. */
data class GridDimensions(val columns: Int, val rows: Int)
