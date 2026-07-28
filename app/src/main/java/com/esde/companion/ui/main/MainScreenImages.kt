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

/**
 * Shown for both [MainScreenImageState.SystemBackdrop] and [MainScreenImageState.GameBackdrop]
 * when there's no real fanart/screenshot to show - a bundled asset rather than falling back
 * to a plain themed background, so the main screen always has a backdrop behind whatever
 * logo/marquee is overlaid on top of it.
 */
private const val FALLBACK_BACKGROUND_ASSET = "file:///android_asset/fallback/default_background.webp"

@Composable
fun MainScreenImages(state: MainScreenImageState, modifier: Modifier = Modifier) {
    when (state) {
        MainScreenImageState.None -> Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {}

        is MainScreenImageState.SystemBackdrop -> BackdropWithOverlay(
            backdropModel = state.fanartPath?.let { File(it) } ?: FALLBACK_BACKGROUND_ASSET,
            modifier = modifier,
        ) {
            LogoOverlay(model = state.systemLogoAssetPath)
        }

        is MainScreenImageState.GameBackdrop -> BackdropWithOverlay(
            backdropModel = state.backdropPath?.let { File(it) } ?: FALLBACK_BACKGROUND_ASSET,
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
    backdropModel: Any?,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (backdropModel != null) {
            AsyncImage(
                model = backdropModel,
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