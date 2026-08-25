package com.esde.companion.ui.widgets.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.ui.SegmentedButtonLabel

@Composable
internal fun ImageTransitionPicker(
    current: ImageTransitionMode,
    onSelected: (ImageTransitionMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Image Transitions", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ImageTransitionMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == current,
                    onClick = { onSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ImageTransitionMode.entries.size),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = mode == current) {
                            Icon(
                                imageVector = mode.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                            )
                        }
                    },
                    label = { SegmentedButtonLabel(mode.displayLabel()) },
                )
            }
        }
    }
}

private fun ImageTransitionMode.icon(): ImageVector =
    when (this) {
        ImageTransitionMode.None -> Icons.Filled.Block
        ImageTransitionMode.Fade -> Icons.Filled.BlurOn
    }

private fun ImageTransitionMode.displayLabel(): String =
    when (this) {
        ImageTransitionMode.None -> "None"
        ImageTransitionMode.Fade -> "Fade"
    }

/**
 * Shown for every logo-style widget (SystemLogo, or SystemMedia/GameMedia with mediaType
 * Marquees - see [WidgetType.isLogoStyle]). Deliberately no Fade option - see
 * AnimatedLogoImage's kdoc for why (double-exposure on overlapping transparent images).
 */
@Composable
internal fun LogoTransitionPicker(
    current: LogoTransitionMode,
    onSelected: (LogoTransitionMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Logo Transitions", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            LogoTransitionMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == current,
                    onClick = { onSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = LogoTransitionMode.entries.size),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = mode == current) {
                            Icon(
                                imageVector = mode.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                            )
                        }
                    },
                    label = { SegmentedButtonLabel(mode.displayLabel()) },
                )
            }
        }
    }
}

private fun LogoTransitionMode.icon(): ImageVector =
    when (this) {
        LogoTransitionMode.None -> Icons.Filled.Block
        LogoTransitionMode.Slide -> Icons.Filled.ArrowForward
        LogoTransitionMode.Scale -> Icons.Filled.ZoomIn
    }

private fun LogoTransitionMode.displayLabel(): String =
    when (this) {
        LogoTransitionMode.None -> "None"
        LogoTransitionMode.Slide -> "Slide"
        LogoTransitionMode.Scale -> "Scale"
    }

/**
 * Shown alongside [LogoTransitionPicker] for the same logo-style widget types -
 * independent of [LogoTransitionMode], a periodic ambient shimmer orthogonal to whichever
 * entrance-transition mode is selected above (see AnimatedLogoImage's glint loop).
 */
@Composable
internal fun GlintConfig(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Logo Glint", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "A periodic light sweep across this widget",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}
