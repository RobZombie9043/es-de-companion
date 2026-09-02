package com.esde.companion.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.esde.companion.data.apps.AppIconLoader
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.ui.theme.LocalIsDarkTheme

/**
 * Shared corner radius for the background panel behind every settings item (category
 * rows, folder settings, toggles, the theme picker) so they read as a consistent set of
 * cards rather than each item picking its own rounding.
 */
internal val SettingsItemShape = RoundedCornerShape(16.dp)

/** Shared opacity for every settings item's background - fully opaque, not translucent. */
internal const val SETTINGS_PANEL_ALPHA = 1f

/**
 * Fades a conditional settings row in/out instead of the instant pop a plain
 * `if (condition) { Row(...) }` gives. A standalone composable (not inlined at each Column/Row-
 * receiver call site) so its `AnimatedVisibility` call has no ambient scope receiver in lexical
 * scope - avoids the overload-ambiguity compile error already hit for `ColumnScope`/`RowScope`/
 * `BoxScope` cases elsewhere in this app (see `GameGuidesBrowserScreen.kt`'s `DownloadButton`,
 * `SystemStatusIcons.kt`'s `StatusIcon`, `CornerButtonMetrics.kt`'s `FabAnimatedVisibility`).
 */
@Composable
internal fun SettingsRowVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
        content()
    }
}

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
            // Arrangement.SpaceBetween reserves no minimum gap here - a weight(1f) child
            // already expands to fill all remaining width on its own, leaving nothing for
            // SpaceBetween to distribute, so long/wrapping description text could butt right
            // up against the Switch with zero breathing room. spacedBy guarantees a real gap
            // regardless of how much of the row the weighted text column's content fills.
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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

/** A non-app, pinned choice shown above the installed-apps list in [SelectAppDialog] - e.g.
 * Game Launch Override's "Use System Default"/"None" entries. Not used by every call site
 * (see [FabControlSetting][com.esde.companion.ui.settings.UISettingsContent]'s picker, which
 * always needs a specific app and passes none). */
internal data class PinnedAppEntry(
    val label: String,
    val onSelected: () -> Unit,
)

/**
 * Shared "pick an installed app" dialog - used by the FAB Control CustomApp picker and Game
 * Launch Override's system-default/per-game pickers. [pinnedEntries] renders above the
 * installed-apps list for non-app choices those callers need (Game Launch Override's "Use
 * System Default"/"None"); empty by default for callers (like FAB Control) that always need a
 * specific app.
 */
@Composable
internal fun SelectAppDialog(
    installedApps: List<InstalledApp>,
    onAppPicked: (String) -> Unit,
    onDismiss: () -> Unit,
    pinnedEntries: List<PinnedAppEntry> = emptyList(),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select app") },
        text = {
            // Same fillMaxWidth-on-every-row reasoning as AppDock's AddAppDialog - without
            // it, varying label widths make the dialog visibly wobble side to side while
            // scrolling.
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(pinnedEntries) { entry -> PinnedAppRow(entry = entry) }
                items(installedApps, key = { it.packageName }) { app ->
                    SelectAppRow(app = app, onClick = { onAppPicked(app.packageName) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun PinnedAppRow(entry: PinnedAppEntry) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = entry.onSelected)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SelectAppRow(
    app: InstalledApp,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val icon by produceState<Any?>(initialValue = null, key1 = app.packageName) {
        value = AppIconLoader.loadIcon(context, app.packageName)
    }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(model = icon, contentDescription = null, modifier = Modifier.size(40.dp))
        Text(text = app.label, style = MaterialTheme.typography.bodyLarge)
    }
}
