package com.esde.companion.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.data.apps.AppLauncher
import com.esde.companion.data.apps.SecondaryDisplayResolver
import com.esde.companion.domain.model.AppFolder
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LaunchLocation

private const val POPUP_GRID_COLUMNS = 3

/**
 * Contents of an open App Drawer folder - a small grid of its member apps, rendered via
 * the same [AppDrawerItem] used at the top level (`isInsideFolder = true`, so its
 * long-press menu offers "Remove from Folder" instead of "Add to Folder"). Hosted inside
 * the shared-element overlay built by [AppDrawer] - this composable owns only the content
 * and its own step-back [BackHandler], not the scrim/shared-transition machinery itself.
 *
 * Registers its own local [BackHandler], composed only while this popup is part of the
 * composition (i.e. while a folder is open). Compose dispatches back presses LIFO by
 * composition order, so this automatically takes priority over the App Drawer's own
 * drawer-closing `BackHandler` in MainScreen, with no changes needed there - same pattern
 * as `LongPressSettingsMenu`'s own `BackHandler`.
 *
 * Long-pressing the title opens [FolderNameDialog] pre-filled with the current name,
 * calling [AppDrawerViewModel.renameFolder] on confirm - the same dialog "New Folder"
 * uses, just with an existing name to edit instead of a blank one.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderContentsPopup(
    viewModel: AppDrawerViewModel,
    folder: AppFolder,
    apps: List<InstalledApp>,
    onAppLaunched: () -> Unit,
    onDismiss: () -> Unit,
) {
    val otherScreenLaunchApps by viewModel.otherScreenLaunchApps.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val contentColor = drawerContentColor()
    var showRenameDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            showRenameDialog = true
                        },
                    ),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close folder")
            }
        }
        HorizontalDivider()

        LazyVerticalGrid(
            columns = GridCells.Fixed(POPUP_GRID_COLUMNS),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(apps, key = { it.packageName }) { app ->
                val isOtherScreenPreferred = otherScreenLaunchApps.contains(app.packageName)

                AppDrawerItem(
                    app = app,
                    isOtherScreenPreferred = isOtherScreenPreferred,
                    isInsideFolder = true,
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
                    onAddToFolder = {},
                    onRemoveFromFolder = { viewModel.removeAppFromFolder(folder.id, app.packageName) },
                )
            }
        }
    }

    if (showRenameDialog) {
        FolderNameDialog(
            title = "Rename folder",
            initialName = folder.name,
            confirmLabel = "Rename",
            onConfirm = { newName ->
                viewModel.renameFolder(folder.id, newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }
}
