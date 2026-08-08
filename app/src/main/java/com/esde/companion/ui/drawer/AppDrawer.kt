package com.esde.companion.ui.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.esde.companion.data.apps.AppIconLoader
import com.esde.companion.data.apps.AppLauncher
import com.esde.companion.data.apps.SecondaryDisplayResolver
import com.esde.companion.domain.model.AppFolder
import com.esde.companion.domain.model.DrawerItem
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LaunchLocation

private val MENU_SHAPE = RoundedCornerShape(16.dp)

// Folder-open shared-element transform: the tapped tile's bounds morph into the panel's
// bounds (and back on close) via SharedTransitionLayout/sharedBounds rather than a plain
// Popup scale-from-center - see FolderDrawerItem and the overlay AnimatedVisibility below.
@OptIn(ExperimentalSharedTransitionApi::class)
private val FOLDER_SHARED_BOUNDS_TRANSFORM = BoundsTransform { _, _ ->
    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
}
private const val FOLDER_SCRIM_ALPHA = 0.32f

// Fixed size rather than a fraction of screen (contrast LongPressSettingsMenu's Popup) -
// a folder's contents are a small, bounded list, so it doesn't need to scale with screen
// size the way the full settings menu does; the grid inside scrolls if it overflows.
private val FOLDER_POPUP_WIDTH = 360.dp
private val FOLDER_POPUP_MAX_HEIGHT = 480.dp

private const val MAX_MOSAIC_ICONS = 4
private val FOLDER_MOSAIC_CELL_SIZE = 24.dp
private val FOLDER_MOSAIC_SPACING = 2.dp

/** Drawer background base color: black in dark mode, white in light mode - matches
 * AppDock's [dockBackgroundColor] so the drawer and dock read as one consistent surface. */
@Composable
internal fun drawerBackgroundColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.Black else Color.White

/** App label text color: the inverse of [drawerBackgroundColor], so labels stay readable
 * against the drawer's background in either theme. */
@Composable
internal fun drawerContentColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.White else Color.Black

/** Two-step "add to folder" flow state, hoisted here (mirrors how AppDock hoists its own
 * add-app dialog state) rather than inside AppDrawerItem, since it drives a dialog that
 * sits above the whole grid, not just one item. */
private sealed interface FolderPickerState {
    data class Picking(val packageName: String) : FolderPickerState
    data class NamingNewFolder(val packageName: String) : FolderPickerState
}

/**
 * Full-screen grid of launchable apps and folders. Opening/closing and the drag gesture
 * that drives it live in MainScreen; this renders the (already-filtered) [DrawerItem]
 * list it's handed, launches whatever was tapped, and reflects the user's opacity/column
 * preferences from [AppDrawerViewModel] (App Drawer settings - see SettingsScreen's App
 * Drawer category).
 *
 * Single tap launches at whatever location was last used for that app - see
 * [AppDrawerViewModel.otherScreenLaunchApps] - defaulting to this screen for an app
 * that's never been launched from here. Double tap launches on whichever screen the app
 * isn't currently set to, toggling and persisting the preference - so double-tapping
 * twice in a row returns to the original location. When toggling from "this screen" to
 * "other screen" but no secondary display is present, it falls back to a same-screen
 * launch without updating the preference, so double-tap is never a dead gesture.
 * Long-press opens a menu with the same two launch options plus App Info, Hide App, and
 * Add/Remove Folder - see [AppLongPressMenu].
 *
 * A folder tile collapses its members into one grid cell (see [FolderDrawerItem]); tapping
 * it expands that tile into a scrimmed panel with its contents (see [FolderContentsPopup])
 * via a `SharedTransitionLayout` container transform - the tile's bounds morph into the
 * panel's and back, rather than a `Popup` scaling from screen center. Folders are never
 * launchable, so a folder tile has no double-tap handling - just a plain tap to open and a
 * long-press to rename (see [FolderDrawerItem]). [isDrawerOpen] auto-dismisses any open
 * folder panel (and an in-progress add-to-folder dialog) when the drawer itself is dragged
 * closed, since both would otherwise float over an invisible drawer sheet - see the
 * `LaunchedEffect(isDrawerOpen)` below.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppDrawer(
    viewModel: AppDrawerViewModel,
    onAppLaunched: () -> Unit,
    onFolderOpenChanged: (Boolean) -> Unit = {},
    isDrawerOpen: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val drawerItems by viewModel.drawerItems.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val otherScreenLaunchApps by viewModel.otherScreenLaunchApps.collectAsStateWithLifecycle()
    val drawerOpacityPercent by viewModel.drawerOpacityPercent.collectAsStateWithLifecycle()
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Read once here, at AppDrawer's own top-level recomposition scope, rather than letting
    // each grid item call drawerContentColor()/drawerBackgroundColor() internally deep inside
    // SharedTransitionLayout/LazyVerticalGrid/AnimatedVisibility - items nested that far down
    // were observed to keep their OLD color after a live theme switch until some unrelated
    // recomposition (e.g. opening a folder) forced them to catch up. Computing it here and
    // passing it down as a plain parameter sidesteps that instead of chasing the exact cause.
    val contentColor = drawerContentColor()
    val backgroundColor = drawerBackgroundColor()

    var folderPickerState by remember { mutableStateOf<FolderPickerState?>(null) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var renamingFolder by remember { mutableStateOf<AppFolder?>(null) }

    val openFolder = drawerItems.filterIsInstance<DrawerItem.Folder>().find { it.folder.id == openFolderId }
    // Auto-close if the open folder disappears entirely (emptied via explicit removal,
    // see AppDrawerViewModel.removeAppFromFolder) - no dead-end UI state to escape from.
    LaunchedEffect(openFolder) {
        if (openFolder == null) openFolderId = null
    }
    LaunchedEffect(openFolderId) {
        onFolderOpenChanged(openFolderId != null)
    }
    LaunchedEffect(isDrawerOpen) {
        if (!isDrawerOpen) {
            openFolderId = null
            folderPickerState = null
            renamingFolder = null
        }
    }

    // Keeps real folder data available through the whole close animation - AnimatedVisibility
    // already keeps the overlay composed until its exit transition finishes, but openFolder
    // itself goes null the instant openFolderId does, so the closing panel needs its own copy
    // of "which folder" to render in the meantime.
    var displayedFolder by remember { mutableStateOf<DrawerItem.Folder?>(null) }
    LaunchedEffect(openFolder) {
        if (openFolder != null) displayedFolder = openFolder
    }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier
                    .fillMaxSize()
                    // Standard convention: 0% = fully transparent, 100% = fully opaque.
                    .background(backgroundColor.copy(alpha = drawerOpacityPercent / 100f)),
                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    drawerItems,
                    key = { item ->
                        when (item) {
                            is DrawerItem.App -> item.app.packageName
                            is DrawerItem.Folder -> item.folder.id
                        }
                    },
                ) { item ->
                    when (item) {
                        is DrawerItem.App -> {
                            val app = item.app
                            val isOtherScreenPreferred = otherScreenLaunchApps.contains(app.packageName)

                            AppDrawerItem(
                                app = app,
                                isOtherScreenPreferred = isOtherScreenPreferred,
                                isInsideFolder = false,
                                contentColor = contentColor,
                                onClick = {
                                    val displayId = if (isOtherScreenPreferred) {
                                        SecondaryDisplayResolver.secondaryDisplayId(context)
                                    } else {
                                        null
                                    }
                                    AppLauncher.launch(context, app.packageName, displayId = displayId)
                                    onAppLaunched()
                                },
                                onDoubleClick = {
                                    // Toggle: double-tap always does the opposite of the app's current
                                    // saved preference, not "always other screen" - so double-tapping
                                    // twice in a row round-trips back to where it started.
                                    if (isOtherScreenPreferred) {
                                        viewModel.recordLaunchLocation(app.packageName, LaunchLocation.ThisScreen)
                                        AppLauncher.launch(context, app.packageName)
                                    } else {
                                        val secondaryDisplayId = SecondaryDisplayResolver.secondaryDisplayId(context)
                                        if (secondaryDisplayId != null) {
                                            viewModel.recordLaunchLocation(app.packageName, LaunchLocation.OtherScreen)
                                        }
                                        AppLauncher.launch(context, app.packageName, displayId = secondaryDisplayId)
                                    }
                                    onAppLaunched()
                                },
                                onLaunchThisScreen = {
                                    viewModel.recordLaunchLocation(app.packageName, LaunchLocation.ThisScreen)
                                    AppLauncher.launch(context, app.packageName)
                                    onAppLaunched()
                                },
                                onLaunchOtherScreen = {
                                    viewModel.recordLaunchLocation(app.packageName, LaunchLocation.OtherScreen)
                                    val secondaryDisplayId = SecondaryDisplayResolver.secondaryDisplayId(context)
                                    AppLauncher.launch(context, app.packageName, displayId = secondaryDisplayId)
                                    onAppLaunched()
                                },
                                onAppInfo = {
                                    AppLauncher.openAppInfo(context, app.packageName)
                                    onAppLaunched()
                                },
                                onHideApp = { viewModel.hideApp(app.packageName) },
                                onAddToFolder = { folderPickerState = FolderPickerState.Picking(app.packageName) },
                                onRemoveFromFolder = {},
                            )
                        }
                        is DrawerItem.Folder -> {
                            // Fades out (no size animation) rather than disposing immediately, so
                            // the tile's own sharedBounds has a moment to hand off to the panel's -
                            // see FolderDrawerItem's kdoc for the "other tiles reflow once this
                            // finishes" tradeoff this accepts.
                            AnimatedVisibility(
                                visible = item.folder.id != openFolderId,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                FolderDrawerItem(
                                    folder = item.folder,
                                    apps = item.apps,
                                    contentColor = contentColor,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@AnimatedVisibility,
                                    onClick = { openFolderId = item.folder.id },
                                    onLongClick = { renamingFolder = item.folder },
                                )
                            }
                        }
                    }
                }
            }

            // The expanded folder panel - shares its bounds with whichever tile is currently
            // collapsed (FolderDrawerItem, above), so opening/closing morphs between the two
            // instead of a Popup scaling from screen center. Deliberately an in-tree overlay,
            // not a Popup, since sharedBounds requires both sides in the same composition/window
            // (a Popup is a genuinely separate Android window - see AppDrawer's kdoc history).
            AnimatedVisibility(
                visible = openFolderId != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val overlayScope = this
                Box(Modifier.fillMaxSize()) {
                    // Popup(focusable = true) used to block all input to the grid underneath and
                    // dismiss on an outside tap for free - both now rebuilt explicitly here.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = FOLDER_SCRIM_ALPHA))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { openFolderId = null },
                    )

                    displayedFolder?.let { folderToShow ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(FOLDER_POPUP_WIDTH)
                                .heightIn(max = FOLDER_POPUP_MAX_HEIGHT)
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = folderToShow.folder.id),
                                    animatedVisibilityScope = overlayScope,
                                    boundsTransform = FOLDER_SHARED_BOUNDS_TRANSFORM,
                                )
                                // Absorbs taps on the panel's own dead space (e.g. header padding)
                                // so they don't fall through to the scrim behind it and dismiss.
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {},
                            shape = MENU_SHAPE,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 3.dp,
                            shadowElevation = 3.dp,
                        ) {
                            FolderContentsPopup(
                                viewModel = viewModel,
                                folder = folderToShow.folder,
                                apps = folderToShow.apps,
                                onAppLaunched = {
                                    openFolderId = null
                                    onAppLaunched()
                                },
                                onDismiss = { openFolderId = null },
                            )
                        }
                    }
                }
            }
        }
    }

    when (val picker = folderPickerState) {
        is FolderPickerState.Picking -> {
            FolderPickerDialog(
                folders = folders,
                onFolderPicked = { folderId ->
                    viewModel.addAppToFolder(folderId, picker.packageName)
                    folderPickerState = null
                },
                onNewFolder = { folderPickerState = FolderPickerState.NamingNewFolder(picker.packageName) },
                onDismiss = { folderPickerState = null },
            )
        }
        is FolderPickerState.NamingNewFolder -> {
            FolderNameDialog(
                title = "New folder",
                initialName = "",
                confirmLabel = "Create",
                onConfirm = { name ->
                    viewModel.createFolderAndAddApp(name, picker.packageName)
                    folderPickerState = null
                },
                onDismiss = { folderPickerState = null },
            )
        }
        null -> Unit
    }

    renamingFolder?.let { folder ->
        FolderNameDialog(
            title = "Rename folder",
            initialName = folder.name,
            confirmLabel = "Rename",
            onConfirm = { newName ->
                viewModel.renameFolder(folder.id, newName)
                renamingFolder = null
            },
            onDismiss = { renamingFolder = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppDrawerItem(
    app: InstalledApp,
    isOtherScreenPreferred: Boolean,
    isInsideFolder: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onLaunchThisScreen: () -> Unit,
    onLaunchOtherScreen: () -> Unit,
    onAppInfo: () -> Unit,
    onHideApp: () -> Unit,
    onAddToFolder: () -> Unit,
    onRemoveFromFolder: () -> Unit,
) {
    val context = LocalContext.current
    val icon by produceState<Any?>(initialValue = null, key1 = app.packageName) {
        value = AppIconLoader.loadIcon(context, app.packageName)
    }
    var menuExpanded by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onDoubleClick = onDoubleClick,
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuExpanded = true
                    },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box {
                AsyncImage(
                    model = icon,
                    contentDescription = app.label,
                    modifier = Modifier.size(56.dp),
                )
                if (isOtherScreenPreferred) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
                    )
                }
            }
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        AppLongPressMenu(
            expanded = menuExpanded,
            appLabel = app.label,
            isOtherScreenPreferred = isOtherScreenPreferred,
            isInsideFolder = isInsideFolder,
            onDismiss = { menuExpanded = false },
            onLaunchThisScreen = onLaunchThisScreen,
            onLaunchOtherScreen = onLaunchOtherScreen,
            onAppInfo = onAppInfo,
            onHideApp = onHideApp,
            onAddToFolder = onAddToFolder,
            onRemoveFromFolder = onRemoveFromFolder,
        )
    }
}

/**
 * Long-press context menu for a single App Drawer entry. Uses Material3's [DropdownMenu]
 * rather than a custom overlay - it already themes from [MaterialTheme.colorScheme] the
 * same way the rest of the app's surfaces do, and handles positioning/outside-tap
 * dismissal for free. The checkmark reflects [isOtherScreenPreferred] so the menu itself
 * doubles as a way to see (not just set) the app's current launch location.
 *
 * [isInsideFolder] swaps "Add to Folder" for "Remove from Folder" - everything else stays
 * available in both contexts, since an app inside a folder is still an ordinary,
 * fully-launchable [InstalledApp].
 */
@Composable
internal fun AppLongPressMenu(
    expanded: Boolean,
    appLabel: String,
    isOtherScreenPreferred: Boolean,
    isInsideFolder: Boolean,
    onDismiss: () -> Unit,
    onLaunchThisScreen: () -> Unit,
    onLaunchOtherScreen: () -> Unit,
    onAppInfo: () -> Unit,
    onHideApp: () -> Unit,
    onAddToFolder: () -> Unit,
    onRemoveFromFolder: () -> Unit,
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
        DropdownMenuItem(
            text = { Text("Launch on this screen") },
            leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
            trailingIcon = {
                if (!isOtherScreenPreferred) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            },
            onClick = { onDismiss(); onLaunchThisScreen() },
        )
        DropdownMenuItem(
            text = { Text("Launch on other screen") },
            leadingIcon = { Icon(Icons.Filled.OpenInNew, contentDescription = null) },
            trailingIcon = {
                if (isOtherScreenPreferred) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            },
            onClick = { onDismiss(); onLaunchOtherScreen() },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("App Info") },
            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
            onClick = { onDismiss(); onAppInfo() },
        )
        if (isInsideFolder) {
            DropdownMenuItem(
                text = { Text("Remove from Folder") },
                leadingIcon = { Icon(Icons.Filled.FolderOff, contentDescription = null) },
                onClick = { onDismiss(); onRemoveFromFolder() },
            )
        } else {
            DropdownMenuItem(
                text = { Text("Add to Folder") },
                leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                onClick = { onDismiss(); onAddToFolder() },
            )
        }
        DropdownMenuItem(
            text = { Text("Hide App") },
            leadingIcon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null) },
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onDismiss()
                onHideApp()
            },
        )
    }
}

/** A folder tile: a mosaic of up to [MAX_MOSAIC_ICONS] member icons collapsed into one
 * grid cell, plus the folder's name. `combinedClickable` rather than plain [Modifier.clickable] -
 * folders aren't launchable and don't participate in this-screen/other-screen semantics the
 * way an app does, so there's no double-click, but long-press opens the same rename dialog
 * [FolderContentsPopup] already offers via its title, just reachable without opening the
 * folder first.
 *
 * [sharedTransitionScope]/[animatedVisibilityScope] tie this tile's bounds to the expanded
 * panel's via [SharedTransitionScope.sharedBounds] (see [AppDrawer]'s overlay), so opening
 * morphs the tile into the panel instead of a plain fade/scale. Only the outer container
 * shares bounds - the mosaic icon and name `Text` just crossfade with the rest of
 * `sharedBounds`'s default content transition, not individually shared-elemented (there's no
 * natural 1:1 destination for a 4-icon mosaic inside the panel). */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun FolderDrawerItem(
    folder: AppFolder,
    apps: List<InstalledApp>,
    contentColor: Color,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = folder.id),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = FOLDER_SHARED_BOUNDS_TRANSFORM,
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FolderMosaicIcon(apps = apps, contentColor = contentColor, modifier = Modifier.size(56.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

/** Renders up to [MAX_MOSAIC_ICONS] member icons in a 2x2 layout, reusing [AppIconLoader]
 * per app the same way a normal drawer item does. Falls back to a plain folder glyph when
 * every member has been hidden/uninstalled (see buildDrawerItems), rather than rendering
 * an empty box. */
@Composable
private fun FolderMosaicIcon(apps: List<InstalledApp>, contentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = contentColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(14.dp),
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (apps.isEmpty()) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(32.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(FOLDER_MOSAIC_SPACING)) {
                apps.take(MAX_MOSAIC_ICONS).chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(FOLDER_MOSAIC_SPACING)) {
                        row.forEach { app -> FolderMosaicMemberIcon(app) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderMosaicMemberIcon(app: InstalledApp) {
    val context = LocalContext.current
    val icon by produceState<Any?>(initialValue = null, key1 = app.packageName) {
        value = AppIconLoader.loadIcon(context, app.packageName)
    }
    AsyncImage(model = icon, contentDescription = null, modifier = Modifier.size(FOLDER_MOSAIC_CELL_SIZE))
}

@Composable
private fun FolderPickerDialog(
    folders: List<AppFolder>,
    onFolderPicked: (String) -> Unit,
    onNewFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to folder") },
        text = {
            // fillMaxWidth on both the list and each row - same "wobble" note as
            // AppDock's AddAppDialog: without it, a fast fling visibly resizes the
            // dialog as differently-sized rows scroll through.
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                item { NewFolderRow(onClick = onNewFolder) }
                items(folders, key = { it.id }) { folder ->
                    FolderPickerRow(folder = folder, onClick = { onFolderPicked(folder.id) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun NewFolderRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(40.dp))
        Text(text = "New Folder", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FolderPickerRow(folder: AppFolder, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(40.dp))
        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Shared by both "New Folder" (from [FolderPickerDialog], [initialName] blank) and
 * "Rename Folder" (long-press on a folder's title inside [FolderContentsPopup],
 * [initialName] pre-filled) - two real call sites, not a hypothetical one, so extracting
 * this is filling in an actual duplication rather than speculating. */
@Composable
internal fun FolderNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
