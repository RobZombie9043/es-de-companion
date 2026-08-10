package com.esde.companion.ui.drawer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private const val UNFOCUSED_ALPHA = 0.6f

/**
 * The row above the App Drawer grid: search field (with a clear button once non-empty)
 * plus shortcuts to Android's system Settings and this app's own settings menu. Restores
 * the pre-rebuild (v0.6.0) drawer header; shown/hidden by the Settings > App Drawer and
 * Dock > "Show Search Bar" toggle - see [AppDrawerViewModel.showSearchBar].
 *
 * Fully state-hoisted: [searchQuery] lives in [AppDrawerViewModel] so the filter it
 * drives (see buildDrawerItems' search branch) and the clear-on-drawer-close reset both
 * happen outside this composable. [contentColor] must be the value AppDrawer already
 * computed at its top scope - reading drawerContentColor() here would resurrect the
 * stale-color-after-live-theme-switch bug that parameter-passing exists to avoid.
 */
@Composable
internal fun AppDrawerHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenAndroidSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fieldColors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = contentColor,
                unfocusedTextColor = contentColor,
                cursorColor = contentColor,
                focusedBorderColor = contentColor,
                unfocusedBorderColor = contentColor.copy(alpha = UNFOCUSED_ALPHA),
                focusedPlaceholderColor = contentColor.copy(alpha = UNFOCUSED_ALPHA),
                unfocusedPlaceholderColor = contentColor.copy(alpha = UNFOCUSED_ALPHA),
                focusedTrailingIconColor = contentColor,
                unfocusedTrailingIconColor = contentColor,
            )
        val fieldModifier =
            Modifier
                .weight(1f)
                .fillMaxWidth()
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = fieldModifier,
            placeholder = { Text("Search apps...") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear Search")
                    }
                }
            },
            singleLine = true,
            colors = fieldColors,
        )
        IconButton(onClick = onOpenAndroidSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Android Settings", tint = contentColor)
        }
        IconButton(onClick = onOpenAppSettings) {
            Icon(Icons.Filled.Menu, contentDescription = "App Settings", tint = contentColor)
        }
    }
}
