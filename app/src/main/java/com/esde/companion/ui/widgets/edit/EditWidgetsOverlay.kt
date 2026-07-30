package com.esde.companion.ui.widgets.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.ui.widgets.gridDimensionsFor

/**
 * Full-screen edit-mode overlay - not a nav destination, same pattern as the App Drawer
 * and Settings. Opaque, so whatever's happening on the live canvas underneath
 * (WidgetOverlay, still composed and reacting to AppState) is simply never visible while
 * editing - no separate state-freezing needed for that to hold true.
 *
 * This slice covers entry/exit and the canvas switcher only - widgets render as labeled
 * placeholders at their saved grid position. Real content preview, drag, resize, add, and
 * remove are later slices on top of this one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWidgetsOverlay(
    viewModel: EditWidgetsViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("Edit Widgets") },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        actions = {
                            IconButton(onClick = onDone) {
                                Icon(imageVector = Icons.Filled.Check, contentDescription = "Done")
                            }
                        },
                    )
                    CanvasSwitcher(
                        viewModel = viewModel,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            },
        ) { innerPadding ->
            val widgets by viewModel.widgets.collectAsStateWithLifecycle()

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                val grid = remember(maxWidth, maxHeight) { gridDimensionsFor(maxWidth, maxHeight) }
                val cellWidth = maxWidth / grid.columns
                val cellHeight = maxHeight / grid.rows

                if (widgets.isEmpty()) {
                    Text(text = "No widgets yet - tap + to add one", modifier = Modifier.padding(24.dp))
                }

                for (widget in widgets) {
                    PlaceholderWidgetBox(
                        widget = widget,
                        modifier = Modifier
                            .offset(x = cellWidth * widget.gridColumn, y = cellHeight * widget.gridRow)
                            .size(width = cellWidth * widget.columnSpan, height = cellHeight * widget.rowSpan),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CanvasSwitcher(viewModel: EditWidgetsViewModel, modifier: Modifier = Modifier) {
    val selected by viewModel.selectedCanvas.collectAsStateWithLifecycle()

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        StateGroup.entries.forEachIndexed { index, group ->
            SegmentedButton(
                selected = selected == group,
                onClick = { viewModel.selectCanvas(group) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = StateGroup.entries.size),
            ) {
                Text(group.name)
            }
        }
    }
}

@Composable
private fun PlaceholderWidgetBox(widget: PlacedWidget, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = widget.widgetType.label(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun WidgetType.label(): String = when (this) {
    is WidgetType.SystemLogo -> "System Logo"
    is WidgetType.SystemMedia -> "System: ${mediaType.name}"
    is WidgetType.GameMedia -> "Game: ${mediaType.name}"
    is WidgetType.ColorBackground -> "Color Background"
}