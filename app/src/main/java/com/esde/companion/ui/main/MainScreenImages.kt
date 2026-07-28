package com.esde.companion.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun MainScreenImages(state: MainScreenImageState, modifier: Modifier = Modifier) {
    when (state) {
        MainScreenImageState.None -> Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {}

        is MainScreenImageState.SystemBackdrop -> BackdropWithOverlay(
            backdropPath = state.fanartPath,
            modifier = modifier,
        ) {
            LogoOverlay(model = state.systemLogoAssetPath)
        }

        is MainScreenImageState.GameBackdrop -> BackdropWithOverlay(
            backdropPath = state.backdropPath,
            modifier = modifier,
        ) {
            state.logoPath?.let { logoPath -> LogoOverlay(model = File(logoPath)) }
        }
    }
}

@Composable
private fun BoxScope.LogoOverlay(model: Any) {
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth(0.6f)
            .fillMaxHeight(0.25f),
    )
}

@Composable
private fun BackdropWithOverlay(
    backdropPath: String?,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (backdropPath != null) {
            AsyncImage(
                model = File(backdropPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
        }
        overlay()
    }
}