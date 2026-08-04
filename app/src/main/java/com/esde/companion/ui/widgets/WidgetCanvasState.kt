package com.esde.companion.ui.widgets

import com.esde.companion.domain.model.NavigationDirection
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.WidgetContent

sealed class WidgetCanvasState {
    /** Grid dimensions not measured yet - too early to show anything. */
    data object Unmeasured : WidgetCanvasState()

    /** Measured, but no AppState-driven canvas applies right now (Idle / not connected). */
    data object Disconnected : WidgetCanvasState()

    data class Showing(
        val widgets: List<PlacedWidget>,
        val contentByWidgetId: Map<String, WidgetContent>,
        val navigationDirection: NavigationDirection?,
    ) : WidgetCanvasState()
}