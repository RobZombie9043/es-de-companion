package com.esde.companion.ui.widgets.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.PillarboxMode
import com.esde.companion.ui.SegmentedButtonLabel
import kotlin.math.roundToInt

private const val MAX_VIDEO_DELAY_SECONDS = 10f
private const val VIDEO_DELAY_SLIDER_STEPS = 9

/** Per-widget "play audio" toggle for a [com.esde.companion.domain.model.WidgetType.Video]
 * widget - same shape as [PanZoomConfig]. */
@Composable
internal fun VideoAudioConfig(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Audio", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

/** Per-widget start delay (seconds before playback begins, to avoid flickering videos past
 * while quickly scrolling a game list) - ported from the retired global Settings > Video
 * Playback slider, same 0..10s range. */
@Composable
internal fun VideoDelayConfig(
    delaySeconds: Int,
    onChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Start Delay: ${if (delaySeconds == 0) "Off" else "${delaySeconds}s"}",
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = delaySeconds.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..MAX_VIDEO_DELAY_SECONDS,
            steps = VIDEO_DELAY_SLIDER_STEPS,
        )
    }
}

/** Only shown when [com.esde.companion.domain.model.WidgetType.supportsPillarbox] is true
 * (Contain-scaled Video widgets only) - Cover always fills the widget with no empty space
 * to pillarbox. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PillarboxConfig(
    current: PillarboxMode,
    onSelect: (PillarboxMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Letterbox Bars", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PillarboxMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == current,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = PillarboxMode.entries.size),
                    label = { SegmentedButtonLabel(mode.displayLabel()) },
                )
            }
        }
    }
}

private fun PillarboxMode.displayLabel(): String =
    when (this) {
        PillarboxMode.Black -> "Black"
        PillarboxMode.Transparent -> "Transparent"
    }

/** Per-widget "Render Above UI" toggle - opts this Video widget out of the ordinary
 * widget-canvas z-order (below FABs/App Dock/App Drawer/Dim/Black covers) and into a
 * second layer drawn above literally everything else, matching where the retired
 * full-screen video overlay always used to sit. See
 * [com.esde.companion.domain.model.WidgetType.Video.renderAboveUi]'s kdoc. */
@Composable
internal fun RenderAboveUiConfig(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Render Above UI", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Draws over FABs, the App Dock, and App Drawer instead of behind them",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}
