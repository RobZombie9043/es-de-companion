package com.esde.companion.ui.widgets

import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.domain.model.isLogoStyle
import java.io.File

/**
 * The widget whose image best represents "the backdrop" of a canvas - the largest
 * non-logo-style image widget (ties: lowest zIndex, then id, for determinism). Logo-style
 * widgets (system logo, marquees) are excluded even though they can also resolve to
 * [WidgetContent.Image] (custom logo/marquee images) - they're a transparent overlay, not
 * the full-bleed backdrop this is used to hold steady across a canvas swap.
 */
internal fun backdropWidgetOf(
    widgets: List<PlacedWidget>,
    contentByWidgetId: Map<String, WidgetContent>,
): PlacedWidget? =
    widgets
        .filter { widget -> !widget.widgetType.isLogoStyle && contentByWidgetId[widget.id] is WidgetContent.Image }
        .minWithOrNull(compareBy({ -(it.columnSpan * it.rowSpan) }, { it.zIndex }, { it.id }))

/**
 * True when swapping from [displayed] to [target] would dispose and remount a different
 * backdrop widget instance (a different id) - exactly the case where CrossfadeAsyncImage's
 * own pre-decode protection is bypassed by a fresh Compose mount (see CLAUDE.md's
 * CrossfadeAsyncImage "Known Gotchas" entry). Ordinary same-canvas navigation (same backdrop
 * id) or a target with no image backdrop at all need no hold.
 */
internal fun canvasSwapNeedsHold(
    displayed: WidgetCanvasState.Showing,
    target: WidgetCanvasState.Showing,
): Boolean {
    val targetBackdrop = backdropWidgetOf(target.widgets, target.contentByWidgetId) ?: return false
    val displayedBackdrop = backdropWidgetOf(displayed.widgets, displayed.contentByWidgetId)
    return targetBackdrop.id != displayedBackdrop?.id
}

/**
 * Mirrors [WidgetContentView]'s own `is WidgetContent.Image ->` model resolution
 * (WidgetCanvas.kt) so the pre-decode request in [WidgetOverlay] targets the exact same Coil
 * model the real render will use - a mismatch here would preload the wrong thing and the
 * flash-avoidance would do nothing.
 */
internal fun backdropModelOf(
    content: WidgetContent,
    isDarkTheme: Boolean,
): Any? {
    val image = content as? WidgetContent.Image ?: return null
    return if (image.isAsset) fallbackBackgroundAssetPath(isDarkTheme) else File(image.path)
}
