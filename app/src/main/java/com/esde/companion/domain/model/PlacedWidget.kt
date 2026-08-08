package com.esde.companion.domain.model

/**
 * A single widget instance placed on a canvas: what it is (WidgetType) plus where/how
 * big it is (grid position) and its stacking order relative to other widgets on the same
 * canvas. Overlap between placed widgets is allowed by design, so zIndex is what makes
 * stacking order well-defined when widgets share space.
 *
 * [id] is stable and independent of position/type, so edit-mode drag/resize/reorder can
 * update a widget in place without losing its Compose recomposition identity - same
 * reasoning as CLAUDE.md's CrossfadeAsyncImage gotcha, generalized to a dynamic list.
 */
data class PlacedWidget(
    val id: String,
    val widgetType: WidgetType,
    val gridColumn: Int,
    val gridRow: Int,
    val columnSpan: Int,
    val rowSpan: Int,
    val zIndex: Int,
)
