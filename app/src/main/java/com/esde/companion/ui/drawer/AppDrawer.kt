package com.esde.companion.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.esde.companion.data.apps.AppIconLoader
import com.esde.companion.data.apps.AppLauncher
import com.esde.companion.data.apps.SecondaryDisplayResolver
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LaunchLocation

private val MENU_SHAPE = RoundedCornerShape(16.dp)

/**
 * Full-screen grid of launchable apps. Opening/closing and the drag gesture that drives
 * it live in MainScreen; this renders the (already-filtered) app list it's handed,
 * launches whatever was tapped, and reflects the user's opacity/column preferences from
 * [AppDrawerViewModel] (App Drawer settings - see SettingsScreen's AppDrawer category).
 *
 * Single tap launches at whatever location was last used for that app - see
 * [AppDrawerViewModel.otherScreenLaunchApps] - defaulting to this screen for an app
 * that's never been launched from here. Double tap launches on whichever screen the app
 * isn't currently set to, toggling and persisting the preference - so double-tapping
 * twice in a row returns to the original location. When toggling from "this screen" to
 * "other screen" but no secondary display is present, it falls back to a same-screen
 * launch without updating the preference, so double-tap is never a dead gesture.
 * Long-press opens a menu with the same two launch options plus App Info and Hide App
 * - see [AppLongPressMenu].
 */

@Composable
fun AppDrawer(
    viewModel: AppDrawerViewModel,
    onAppLaunched: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val otherScreenLaunchApps by viewModel.otherScreenLaunchApps.collectAsStateWithLifecycle()
    val drawerOpacityPercent by viewModel.drawerOpacityPercent.collectAsStateWithLifecycle()
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        modifier = modifier
            .fillMaxSize()
            // Standard convention: 0% = fully transparent, 100% = fully opaque.
            .background(Color.Black.copy(alpha = drawerOpacityPercent / 100f)),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(installedApps, key = { it.packageName }) { app ->
            val isOtherScreenPreferred = otherScreenLaunchApps.contains(app.packageName)

            AppDrawerItem(
                app = app,
                isOtherScreenPreferred = isOtherScreenPreferred,
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
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppDrawerItem(
    app: InstalledApp,
    isOtherScreenPreferred: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onLaunchThisScreen: () -> Unit,
    onLaunchOtherScreen: () -> Unit,
    onAppInfo: () -> Unit,
    onHideApp: () -> Unit,
) {
    val context = LocalContext.current
    val icon by produceState<Any?>(initialValue = null, key1 = app.packageName) {
        value = AppIconLoader.loadIcon(context, app.packageName)
    }
    var menuExpanded by remember { mutableStateOf(false) }

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
                    onLongClick = { menuExpanded = true },
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
                color = Color.White,
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
            onDismiss = { menuExpanded = false },
            onLaunchThisScreen = onLaunchThisScreen,
            onLaunchOtherScreen = onLaunchOtherScreen,
            onAppInfo = onAppInfo,
            onHideApp = onHideApp,
        )
    }
}

/**
 * Long-press context menu for a single App Drawer entry. Uses Material3's [DropdownMenu]
 * rather than a custom overlay - it already themes from [MaterialTheme.colorScheme] the
 * same way the rest of the app's surfaces do, and handles positioning/outside-tap
 * dismissal for free. The checkmark reflects [isOtherScreenPreferred] so the menu itself
 * doubles as a way to see (not just set) the app's current launch location.
 */
@Composable
private fun AppLongPressMenu(
    expanded: Boolean,
    appLabel: String,
    isOtherScreenPreferred: Boolean,
    onDismiss: () -> Unit,
    onLaunchThisScreen: () -> Unit,
    onLaunchOtherScreen: () -> Unit,
    onAppInfo: () -> Unit,
    onHideApp: () -> Unit,
) {
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
        DropdownMenuItem(
            text = { Text("Hide App") },
            leadingIcon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null) },
            onClick = { onDismiss(); onHideApp() },
        )
    }
}