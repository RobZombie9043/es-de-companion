package com.esde.companion.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.ui.main.CrossfadeAsyncImage
import java.io.File

/**
 * Renders [widgets] on a grid derived from the available space (see [gridDimensionsFor]),
 * positioning/sizing each by its saved grid coordinates. [contentByWidgetId] supplies
 * what each widget should currently show - resolved separately (see WidgetContentResolver)
 * so this composable stays purely about layout, not resolution.
 *
 * Overlap between widgets is allowed by design - stacking order follows PlacedWidget.zIndex,
 * not list order, so callers don't need to pre-sort [widgets].
 */
@Composable
fun WidgetCanvas(
    widgets: List<PlacedWidget>,
    contentByWidgetId: Map<String, WidgetContent>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val grid = remember(maxWidth, maxHeight) { gridDimensionsFor(maxWidth, maxHeight) }
        val cellWidth = maxWidth / grid.columns
        val cellHeight = maxHeight / grid.rows

        for (widget in widgets) {
            key(widget.id) {
                val content = contentByWidgetId[widget.id] ?: WidgetContent.Empty
                WidgetContentView(
                    content = content,
                    modifier = Modifier
                        .offset(x = cellWidth * widget.gridColumn, y = cellHeight * widget.gridRow)
                        .size(width = cellWidth * widget.columnSpan, height = cellHeight * widget.rowSpan)
                        .zIndex(widget.zIndex.toFloat()),
                )
            }
        }
    }
}

@Composable
private fun WidgetContentView(content: WidgetContent, modifier: Modifier = Modifier) {
    when (content) {
        WidgetContent.Empty -> Unit

        is WidgetContent.Color ->
            Box(modifier = modifier.background(Color(content.colorArgb).copy(alpha = content.alpha)))

        is WidgetContent.Image ->
            if (content.crossfade) {
                CrossfadeAsyncImage(
                    model = if (content.isAsset) content.path else File(content.path),
                    contentDescription = null,
                    contentScale = content.scaleMode.toContentScale(),
                    modifier = modifier,
                )
            } else {
                AsyncImage(
                    model = if (content.isAsset) content.path else File(content.path),
                    contentDescription = null,
                    contentScale = content.scaleMode.toContentScale(),
                    modifier = modifier,
                )
            }

        is WidgetContent.SystemLogoAsset ->
            AsyncImage(
                model = content.assetPath,
                contentDescription = null,
                contentScale = content.scaleMode.toContentScale(),
                modifier = modifier,
            )
    }
}

private fun ScaleMode.toContentScale(): ContentScale = when (this) {
    ScaleMode.Fit -> ContentScale.Fit
    ScaleMode.Fill -> ContentScale.Crop
}