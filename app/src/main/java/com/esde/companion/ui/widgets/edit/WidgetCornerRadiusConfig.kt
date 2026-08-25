package com.esde.companion.ui.widgets.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.CornerRadius

/**
 * Only shown when [com.esde.companion.domain.model.supportsCornerRadius] is true - hidden
 * entirely (rather than shown-but-inert) for transparent cutout content (SystemLogo,
 * Marquee-style media) that has nothing opaque behind it for rounding to visibly affect,
 * same reasoning as [PillarboxConfig].
 *
 * A dropdown, not a [androidx.compose.material3.SingleChoiceSegmentedButtonRow] - at 4
 * options ([CornerRadius.entries]) a segmented row stops fitting comfortably, the same
 * threshold that moved the Game Playing Screen Behavior picker and FabTypeDropdown to a
 * dropdown (see UISettingsContent.kt's SEGMENTED_ROW_TO_DROPDOWN_THRESHOLD).
 */
@Composable
internal fun CornerRadiusConfig(
    current: CornerRadius,
    onSelect: (CornerRadius) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Rounded Corners", style = MaterialTheme.typography.titleSmall)
        Box {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = current.displayLabel(), style = MaterialTheme.typography.bodyMedium)
                    Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                CornerRadius.entries.forEach { radius ->
                    DropdownMenuItem(
                        text = { Text(radius.displayLabel()) },
                        onClick = {
                            expanded = false
                            onSelect(radius)
                        },
                    )
                }
            }
        }
    }
}

private fun CornerRadius.displayLabel(): String =
    when (this) {
        CornerRadius.None -> "None"
        CornerRadius.Small -> "Small"
        CornerRadius.Medium -> "Medium"
        CornerRadius.Large -> "Large"
    }
