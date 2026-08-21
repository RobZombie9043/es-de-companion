package com.esde.companion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.esde.companion.ui.theme.LocalIsDarkTheme

/**
 * Shared corner radius for the background panel behind every settings item (category
 * rows, folder settings, toggles, the theme picker) so they read as a consistent set of
 * cards rather than each item picking its own rounding.
 */
internal val SettingsItemShape = RoundedCornerShape(16.dp)

/** Shared opacity for every settings item's background - fully opaque, not translucent. */
internal const val SETTINGS_PANEL_ALPHA = 1f

/** Corner radius of the accent-colored square drawn behind each [SettingsLabel] icon. */
private val SettingsLabelIconShape = RoundedCornerShape(8.dp)

/** Thickness of the accent-colored border surrounding each [SettingsLabel] icon. */
private val SettingsLabelIconBorderWidth = 8.dp

/**
 * The two accent-square colors [SettingsLabel] swaps between light/dark mode - deliberately
 * the *other* theme's default primary tone (dark theme's in light mode and vice versa)
 * rather than [MaterialTheme.colorScheme.primary] directly, per design request.
 */
private val SettingsLabelBorderColorInLightMode = darkColorScheme().primary
private val SettingsLabelBorderColorInDarkMode = lightColorScheme().primary

/**
 * Name row shared by every settings category and every individual setting: an icon in an
 * accent-colored (purple), rounded-corner square, followed by the name itself. The icon and
 * text share one color that resolves to black in light mode / white in dark mode (see
 * [LocalIsDarkTheme]).
 */
@Composable
internal fun SettingsLabel(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    val isDark = LocalIsDarkTheme.current
    val labelColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) SettingsLabelBorderColorInDarkMode else SettingsLabelBorderColorInLightMode
    val iconSize = style.fontSize.value.dp
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = SettingsLabelIconShape, color = borderColor) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = labelColor,
                modifier =
                    Modifier
                        .padding(SettingsLabelIconBorderWidth)
                        .size(iconSize),
            )
        }
        Text(text = text, style = style, color = labelColor)
    }
}

/**
 * Per-category settings content, one composable per [SettingsCategory] plus the shared
 * category-list row and leaf setting composables (toggle/slider/picker/folder-picker
 * rows), split across one file per category (`SetupSettingsContent.kt`,
 * `UISettingsContent.kt`, etc.) with this file holding the pieces shared across more than
 * one of them. These used to be hosted by a standalone full-screen `SettingsScreen`; that
 * screen is gone - this content is now hosted inside the long-press popup instead, see
 * `LongPressSettingsMenu` in the `ui.main` package (hence `internal`, not `private`,
 * visibility throughout these files).
 */
@Composable
internal fun SettingsCategoryRow(
    category: SettingsCategory,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsLabel(icon = category.icon, text = category.title)
                Text(text = category.description, style = MaterialTheme.typography.bodySmall)
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

/**
 * Bottom-of-menu row that closes the app outright (see [MainActivity][com.esde.companion.ui.MainActivity]'s
 * `finishAndRemoveTask` call, the same one "Close Companion App on ES-DE Quit" uses).
 * Styled in the error color, distinct from [SettingsCategoryRow], so it doesn't read as
 * just another drill-down category - tapping it is terminal, not navigation.
 */
@Composable
internal fun SettingsQuitRow(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = "Quit Companion App",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/** Shared toggle row (icon/title/optional description + a Switch) used by more than one
 * settings category - App Drawer's Search Bar/Sort/Dock toggles and Sound's browsing/
 * screensaver toggles. */
@Composable
internal fun ToggleSettingRow(
    icon: ImageVector,
    title: String,
    description: String?,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (description != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    SettingsLabel(icon = icon, text = title)
                    Text(text = description, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                SettingsLabel(icon = icon, text = title, modifier = Modifier.weight(1f))
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEnabledChanged(it)
                },
            )
        }
    }
}
