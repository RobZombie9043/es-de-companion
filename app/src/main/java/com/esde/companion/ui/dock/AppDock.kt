package com.esde.companion.ui.dock

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.esde.companion.data.apps.AppIconLoader
import com.esde.companion.data.apps.AppLauncher
import com.esde.companion.data.apps.SecondaryDisplayResolver
import com.esde.companion.domain.model.APP_DRAWER_SHORTCUT_PACKAGE_NAME
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LaunchLocation

private val MENU_SHAPE = RoundedCornerShape(16.dp)
private val DOCK_SHAPE = RoundedCornerShape(28.dp)
private val VERTICAL_PADDING = 16.dp
private val HORIZONTAL_PADDING = 20.dp
private val SLOT_SPACING = 16.dp

/** Dims DockPreview just enough to read as non-interactive at a glance, without
 * desaturating/hiding it so much that it stops being useful for judging real visual
 * collisions (e.g. a bright icon over bright widget art) - see DockPreview's kdoc. */
private const val DOCK_PREVIEW_ALPHA = 0.65f

/** Dock background base color: black in dark mode, white in light mode - same
 * theme-detection approach as MusicControlsOverlay's content/background colors. */
@Composable
private fun dockBackgroundColor(): Color {
    return if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.Black else Color.White
}

/** Empty-slot border/icon color: the inverse of [dockBackgroundColor], so the "add app"
 * placeholder stays visible against the dock's background in either theme. */
@Composable
private fun dockContentColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        Color.White.copy(
            alpha = 0.3f,
        )
    } else {
        Color.Black.copy(alpha = 0.3f)
    }

/** Same inverse-of-[dockBackgroundColor] logic as [dockContentColor], full opacity - for
 * the App Drawer shortcut's glyph (Icons.Filled.Apps), which - unlike a real app's own
 * AsyncImage icon - has no built-in color of its own and needs an explicit tint to read
 * clearly against the dock's forced black/white background rather than whatever the
 * ambient content color happens to be. */
@Composable
private fun appDrawerShortcutIconTint(): Color {
    return if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.White else Color.Black
}

private fun DockSize.iconDp(): Dp =
    when (this) {
        DockSize.Small -> 40.dp
        DockSize.Medium -> 48.dp
        DockSize.Large -> 56.dp
    }

/**
 * Height of the dock bar for a given [size], derived from its icon size rather than a
 * separately hardcoded constant so MainScreen's anchor-offset math (which positions the
 * dock directly above the App Drawer sheet's top edge) can never drift out of sync with
 * the bar's own layout.
 */
fun dockBarHeight(size: DockSize): Dp = size.iconDp() + VERTICAL_PADDING * 2

/**
 * The always-available row of pinned apps at the bottom of the main screen - see
 * MainScreen for how it's anchored to slide with the App Drawer sheet. Renders nothing
 * if the dock is disabled (Settings > App Drawer and Dock).
 *
 * Always shows exactly [AppDockViewModel.maxApps] slots, even when fewer apps are
 * pinned, so there's always a discoverable empty-slot target: long-press a filled slot
 * for launch-location/reorder/remove options (see [DockItemMenu]), long-press an empty
 * slot to pin a new app. There is no separate "Manage Dock" Settings screen - all
 * editing happens here, on the live dock.
 *
 * One pinnable slot is special: [APP_DRAWER_SHORTCUT_PACKAGE_NAME] (offered at the top of
 * the "add app to dock" list, see AppDockViewModel.availableApps) isn't a real app - a tap
 * calls [onOpenAppDrawer] instead of [AppLauncher.launch], and its long-press menu skips
 * the launch-location/App Info rows that don't apply to it (see [DockItemMenu]).
 */
@Composable
fun AppDock(
    viewModel: AppDockViewModel,
    onOpenAppDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dockEnabled by viewModel.dockEnabled.collectAsStateWithLifecycle()
    val dockSize by viewModel.dockSize.collectAsStateWithLifecycle()
    val dockOpacityPercent by viewModel.dockOpacityPercent.collectAsStateWithLifecycle()
    val maxApps by viewModel.maxApps.collectAsStateWithLifecycle()
    val dockItems by viewModel.dockItems.collectAsStateWithLifecycle()
    val otherScreenLaunchApps by viewModel.otherScreenLaunchApps.collectAsStateWithLifecycle()
    val availableApps by viewModel.availableApps.collectAsStateWithLifecycle()

    if (!dockEnabled) return

    val context = LocalContext.current
    val iconDp = dockSize.iconDp()
    var showAddAppDialog by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .background(color = dockBackgroundColor().copy(alpha = dockOpacityPercent / 100f), shape = DOCK_SHAPE)
                // Claims every tap/press landing anywhere within the dock's bounds -
                // including gaps between slots and empty placeholder slots - so
                // MainScreen's whole-screen double-tap-to-blank / long-press-to-edit
                // gesture never fires here. A slot's own combinedClickable below still
                // handles its own taps normally: Compose's pointer-consumption
                // propagation means a descendant consuming a gesture prevents this (and
                // any further ancestor) from also recognizing it, so nothing double-fires.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
            horizontalArrangement = Arrangement.spacedBy(SLOT_SPACING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(maxApps) { index ->
                val app = dockItems.getOrNull(index)
                if (app == null) {
                    EmptyDockSlot(iconDp = iconDp, onAddApp = { showAddAppDialog = true })
                } else if (app.packageName == APP_DRAWER_SHORTCUT_PACKAGE_NAME) {
                    FilledDockSlot(
                        app = app,
                        iconDp = iconDp,
                        isAppDrawerShortcut = true,
                        isOtherScreenPreferred = false,
                        isLeftmost = index == 0,
                        isRightmost = index == dockItems.lastIndex,
                        onClick = onOpenAppDrawer,
                        onDoubleClick = onOpenAppDrawer,
                        onLaunchThisScreen = onOpenAppDrawer,
                        onLaunchOtherScreen = onOpenAppDrawer,
                        onAppInfo = {},
                        onMoveLeft = { viewModel.moveLeft(app.packageName) },
                        onMoveRight = { viewModel.moveRight(app.packageName) },
                        onRemove = { viewModel.removeFromDock(app.packageName) },
                    )
                } else {
                    FilledDockSlot(
                        app = app,
                        iconDp = iconDp,
                        isAppDrawerShortcut = false,
                        isOtherScreenPreferred = otherScreenLaunchApps.contains(app.packageName),
                        isLeftmost = index == 0,
                        isRightmost = index == dockItems.lastIndex,
                        onClick = {
                            val displayId =
                                if (otherScreenLaunchApps.contains(app.packageName)) {
                                    SecondaryDisplayResolver.secondaryDisplayId(context)
                                } else {
                                    null
                                }
                            AppLauncher.launch(context, app.packageName, displayId = displayId)
                        },
                        onDoubleClick = {
                            if (otherScreenLaunchApps.contains(app.packageName)) {
                                viewModel.recordLaunchLocation(app.packageName, LaunchLocation.ThisScreen)
                                AppLauncher.launch(context, app.packageName)
                            } else {
                                val secondaryDisplayId = SecondaryDisplayResolver.secondaryDisplayId(context)
                                if (secondaryDisplayId != null) {
                                    viewModel.recordLaunchLocation(app.packageName, LaunchLocation.OtherScreen)
                                }
                                AppLauncher.launch(context, app.packageName, displayId = secondaryDisplayId)
                            }
                        },
                        onLaunchThisScreen = {
                            viewModel.recordLaunchLocation(app.packageName, LaunchLocation.ThisScreen)
                            AppLauncher.launch(context, app.packageName)
                        },
                        onLaunchOtherScreen = {
                            viewModel.recordLaunchLocation(app.packageName, LaunchLocation.OtherScreen)
                            AppLauncher.launch(
                                context,
                                app.packageName,
                                displayId = SecondaryDisplayResolver.secondaryDisplayId(context),
                            )
                        },
                        onAppInfo = { AppLauncher.openAppInfo(context, app.packageName) },
                        onMoveLeft = { viewModel.moveLeft(app.packageName) },
                        onMoveRight = { viewModel.moveRight(app.packageName) },
                        onRemove = { viewModel.removeFromDock(app.packageName) },
                    )
                }
            }
        }
    }

    if (showAddAppDialog) {
        AddAppDialog(
            availableApps = availableApps,
            onAppPicked = { packageName ->
                viewModel.addToDock(packageName)
                showAddAppDialog = false
            },
            onDismiss = { showAddAppDialog = false },
        )
    }
}

/**
 * Read-only rendering of the dock's current appearance - no `clickable`/
 * `combinedClickable`/`pointerInput` anywhere in this composable or its slot helpers, so
 * every pixel is purely decorative and touches fall straight through to whatever's
 * beneath it regardless of z-order (same "no gesture modifier = pass-through" idiom as
 * MainActivity's Dim overlay and EditWidgetsOverlay's grid-line Canvas). Used by
 * EditWidgetsOverlay to preview where the live dock will actually sit/overlap while
 * editing widgets. Shares AppDock's own background/icon-sizing constants so the preview
 * can never visually drift from what the live dock looks like.
 *
 * Dimmed to [DOCK_PREVIEW_ALPHA] as the one deliberate difference from the live dock -
 * a quick, glanceable signal that it's non-interactive, without going as far as
 * grayscale/desaturation, which would make it harder to judge real visual collisions
 * (e.g. a bright icon sitting over bright widget art) - the actual point of showing it.
 */
@Composable
fun DockPreview(
    dockSize: DockSize,
    dockOpacityPercent: Int,
    maxApps: Int,
    dockItems: List<InstalledApp>,
    modifier: Modifier = Modifier,
) {
    val iconDp = dockSize.iconDp()

    Box(
        modifier =
            modifier
                .alpha(DOCK_PREVIEW_ALPHA)
                .background(color = dockBackgroundColor().copy(alpha = dockOpacityPercent / 100f), shape = DOCK_SHAPE),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
            horizontalArrangement = Arrangement.spacedBy(SLOT_SPACING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(maxApps) { index ->
                val app = dockItems.getOrNull(index)
                if (app == null) {
                    EmptyDockSlotPreview(iconDp = iconDp)
                } else {
                    FilledDockSlotPreview(app = app, iconDp = iconDp)
                }
            }
        }
    }
}

@Composable
private fun FilledDockSlotPreview(
    app: InstalledApp,
    iconDp: Dp,
) {
    if (app.packageName == APP_DRAWER_SHORTCUT_PACKAGE_NAME) {
        Icon(
            imageVector = Icons.Filled.Apps,
            contentDescription = app.label,
            tint = appDrawerShortcutIconTint(),
            modifier = Modifier.size(iconDp),
        )
        return
    }
    val context = LocalContext.current
    val icon by produceState<Any?>(initialValue = null, key1 = app.packageName) {
        value = AppIconLoader.loadIcon(context, app.packageName)
    }
    AsyncImage(model = icon, contentDescription = app.label, modifier = Modifier.size(iconDp))
}

@Composable
private fun EmptyDockSlotPreview(iconDp: Dp) {
    val tint = dockContentColor()
    Box(
        modifier =
            Modifier
                .size(iconDp)
                .border(width = 1.dp, color = tint, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = tint,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilledDockSlot(
    app: InstalledApp,
    iconDp: Dp,
    isAppDrawerShortcut: Boolean,
    isOtherScreenPreferred: Boolean,
    isLeftmost: Boolean,
    isRightmost: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onLaunchThisScreen: () -> Unit,
    onLaunchOtherScreen: () -> Unit,
    onAppInfo: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .size(iconDp)
                    .combinedClickable(
                        onClick = onClick,
                        onDoubleClick = onDoubleClick,
                        onLongClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuExpanded = true
                        },
                    ),
        ) {
            if (isAppDrawerShortcut) {
                Icon(
                    imageVector = Icons.Filled.Apps,
                    contentDescription = app.label,
                    tint = appDrawerShortcutIconTint(),
                    modifier = Modifier.size(iconDp),
                )
            } else {
                val context = LocalContext.current
                val icon by produceState<Any?>(initialValue = null, key1 = app.packageName) {
                    value = AppIconLoader.loadIcon(context, app.packageName)
                }
                AsyncImage(
                    model = icon,
                    contentDescription = app.label,
                    modifier = Modifier.size(iconDp),
                )
            }
            if (isOtherScreenPreferred) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
                )
            }
        }

        DockItemMenu(
            expanded = menuExpanded,
            appLabel = app.label,
            isAppDrawerShortcut = isAppDrawerShortcut,
            isOtherScreenPreferred = isOtherScreenPreferred,
            isLeftmost = isLeftmost,
            isRightmost = isRightmost,
            onDismiss = { menuExpanded = false },
            onLaunchThisScreen = onLaunchThisScreen,
            onLaunchOtherScreen = onLaunchOtherScreen,
            onAppInfo = onAppInfo,
            onMoveLeft = onMoveLeft,
            onMoveRight = onMoveRight,
            onRemove = onRemove,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmptyDockSlot(
    iconDp: Dp,
    onAddApp: () -> Unit,
) {
    val tint = dockContentColor()
    val hapticFeedback = LocalHapticFeedback.current
    Box(
        modifier =
            Modifier
                .size(iconDp)
                .border(width = 1.dp, color = tint, shape = CircleShape)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddApp()
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add app to dock",
            tint = tint,
        )
    }
}

/**
 * Long-press context menu for a pinned dock item - same shape as App Drawer's
 * AppLongPressMenu (this/other screen, App Info) plus dock-specific reordering/removal
 * in place of "Hide App", per the user's request that the dock's menu skip that option.
 * [isAppDrawerShortcut] skips the launch-location and App Info rows entirely - neither
 * concept applies to the App Drawer shortcut (see AppDock's kdoc), leaving just reorder/
 * remove.
 */
@Composable
private fun DockItemMenu(
    expanded: Boolean,
    appLabel: String,
    isAppDrawerShortcut: Boolean,
    isOtherScreenPreferred: Boolean,
    isLeftmost: Boolean,
    isRightmost: Boolean,
    onDismiss: () -> Unit,
    onLaunchThisScreen: () -> Unit,
    onLaunchOtherScreen: () -> Unit,
    onAppInfo: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRemove: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = MENU_SHAPE,
    ) {
        Text(
            text = appLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (!isAppDrawerShortcut) {
            DropdownMenuItem(
                text = { Text("Launch on this screen") },
                leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                trailingIcon = {
                    if (!isOtherScreenPreferred) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                onClick = {
                    onDismiss()
                    onLaunchThisScreen()
                },
            )
            DropdownMenuItem(
                text = { Text("Launch on other screen") },
                leadingIcon = { Icon(Icons.Filled.OpenInNew, contentDescription = null) },
                trailingIcon = {
                    if (isOtherScreenPreferred) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                onClick = {
                    onDismiss()
                    onLaunchOtherScreen()
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("App Info") },
                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onAppInfo()
                },
            )
            HorizontalDivider()
        }
        if (!isLeftmost) {
            DropdownMenuItem(
                text = { Text("Move Left") },
                leadingIcon = { Icon(Icons.Filled.ChevronLeft, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onMoveLeft()
                },
            )
        }
        if (!isRightmost) {
            DropdownMenuItem(
                text = { Text("Move Right") },
                leadingIcon = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onMoveRight()
                },
            )
        }
        DropdownMenuItem(
            text = { Text("Remove from Dock") },
            leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onDismiss()
                onRemove()
            },
        )
    }
}

@Composable
private fun AddAppDialog(
    availableApps: List<InstalledApp>,
    onAppPicked: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add app to dock") },
        text = {
            // fillMaxWidth on both the list and each row is required, not cosmetic -
            // without it the dialog's own width tracks whichever rows happen to be
            // laid out at a given instant, and since app labels vary in length, a fast
            // fling visibly resizes/re-centers the whole dialog every frame as
            // differently-sized rows scroll through (seen as a left-right "wobble").
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(availableApps, key = { it.packageName }) { app ->
                    AddAppRow(app = app, onClick = { onAppPicked(app.packageName) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun AddAppRow(
    app: InstalledApp,
    onClick: () -> Unit,
) {
    val isAppDrawerShortcut = app.packageName == APP_DRAWER_SHORTCUT_PACKAGE_NAME

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isAppDrawerShortcut) {
            Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = null,
                tint = appDrawerShortcutIconTint(),
                modifier = Modifier.size(40.dp),
            )
        } else {
            val context = LocalContext.current
            val icon by produceState<Any?>(initialValue = null, key1 = app.packageName) {
                value = AppIconLoader.loadIcon(context, app.packageName)
            }
            AsyncImage(model = icon, contentDescription = null, modifier = Modifier.size(40.dp))
        }
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
