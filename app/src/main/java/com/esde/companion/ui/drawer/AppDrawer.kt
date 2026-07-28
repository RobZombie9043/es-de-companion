package com.esde.companion.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import com.esde.companion.domain.model.InstalledApp

/**
 * Full-screen grid of launchable apps. Purely presentational - opening/closing and the
 * drag gesture that drives it live in MainScreen, this only knows how to render the app
 * list it's handed and launch whatever was tapped.
 */
@Composable
fun AppDrawer(
    viewModel: AppDrawerViewModel,
    onAppLaunched: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 88.dp),
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(installedApps, key = { it.packageName }) { app ->
            AppDrawerItem(
                app = app,
                onClick = {
                    AppLauncher.launch(context, app.packageName)
                    onAppLaunched()
                },
            )
        }
    }
}

@Composable
private fun AppDrawerItem(app: InstalledApp, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon by produceState<Any?>(initialValue = null, key1 = app.packageName) {
        value = AppIconLoader.loadIcon(context, app.packageName)
    }

    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AsyncImage(
            model = icon,
            contentDescription = app.label,
            modifier = Modifier.size(56.dp),
        )
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
}