package com.esde.companion.ui.widgets

import com.esde.companion.domain.model.NavigationDirection
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.WidgetContent

sealed class WidgetCanvasState {
    /** Idle, or grid dimensions not measured yet - nothing to show. */
    data object None : WidgetCanvasState()

    data class Showing(
        val widgets: List<PlacedWidget>,
        val contentByWidgetId: Map<String, WidgetContent>,
        val navigationDirection: NavigationDirection?,
    ) : WidgetCanvasState()
}