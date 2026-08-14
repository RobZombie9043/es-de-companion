package com.esde.companion.ui.retroachievements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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

/**
 * A labeled, single-select dropdown chip - shows [title] as a non-interactive header followed
 * by a divider, then one [DropdownMenuItem] per option. The closed chip's own label is derived
 * from whichever [options] entry matches [selection]'s current value, falling back to [title]
 * if nothing matches, so the chip can never show a label unrelated to the actual selection.
 */
@Composable
internal fun <T> SingleSelectDropdownChip(
    title: String,
    options: List<DropdownOption<T>>,
    selection: DropdownSelection<T>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val chipLabel = options.firstOrNull { it.value == selection.value }?.label ?: title
    Box(modifier = modifier) {
        SelectionChip(label = chipLabel, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, shape = MENU_SHAPE) {
            DropdownMenuTitle(title)
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        selection.onValueChanged(option.value)
                    },
                )
            }
        }
    }
}

/**
 * A labeled, multi-select dropdown chip - each option is a checkbox row that toggles in place
 * without closing the menu, since picking several values is the whole point of a multi-select.
 * The closed chip reads [title] when nothing is selected (the "show everything" default), the
 * one label when exactly one value is picked, or the first-selected label (in [options]
 * declaration order, not `Set` iteration order, so it can't reshuffle unpredictably) plus a
 * "+N" count otherwise.
 */
@Composable
internal fun <T> MultiSelectDropdownChip(
    title: String,
    options: List<DropdownOption<T>>,
    selection: DropdownSelection<Set<T>>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOptions = options.filter { it.value in selection.value }
    val chipLabel =
        when (selectedOptions.size) {
            0 -> title
            1 -> selectedOptions.first().label
            else -> "${selectedOptions.first().label} +${selectedOptions.size - 1}"
        }
    Box(modifier = modifier) {
        SelectionChip(label = chipLabel, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, shape = MENU_SHAPE) {
            DropdownMenuTitle(title)
            options.forEach { option ->
                val checked = option.value in selection.value
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
                    onClick = {
                        val updated =
                            if (checked) selection.value - option.value else selection.value + option.value
                        selection.onValueChanged(updated)
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectionChip(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = CHIP_SHAPE,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * A non-interactive header inside a [DropdownMenu] - plain [Text] rather than a
 * [DropdownMenuItem], so it can't be focused or tapped. Uses [MaterialTheme]'s own
 * `onSurfaceVariant` explicitly rather than inheriting `LocalContentColor` - both
 * RetroAchievements screens set that to pure white/black for legibility over their background
 * image, which would visibly mismatch the [DropdownMenuItem]s below (Material's own
 * `onSurface`) if this inherited it instead.
 */
@Composable
private fun DropdownMenuTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    HorizontalDivider()
}

private val CHIP_SHAPE = RoundedCornerShape(8.dp)
private val MENU_SHAPE = RoundedCornerShape(16.dp)
