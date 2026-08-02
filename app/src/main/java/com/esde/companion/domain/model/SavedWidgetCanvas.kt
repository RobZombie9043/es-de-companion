package com.esde.companion.domain.model

/**
 * A persisted canvas, tagged with the grid dimensions it was saved under - needed by
 * ObserveWidgetCanvasUseCase to rescale placements (see [rescaleWidgetsToGrid]) when the
 * app is running on a differently-sized grid than the one the layout was authored on.
 *
 * [grid] is null only for canvases saved before this field existed (or when [widgets] is
 * empty, i.e. nothing saved yet) - treated as "assume it already matches the current
 * grid" rather than guessing, since the true original grid is unrecoverable for that data.
 */
data class SavedWidgetCanvas(
    val grid: GridDimensions?,
    val widgets: List<PlacedWidget>,
)
