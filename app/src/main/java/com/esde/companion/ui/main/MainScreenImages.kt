package com.esde.companion.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.io.File

private const val FALLBACK_BACKGROUND_ASSET = "file:///android_asset/fallback/default_background.webp"

@Composable
fun MainScreenImages(state: MainScreenImageState, modifier: Modifier = Modifier) {
    val backdropModel: Any = when (state) {
        MainScreenImageState.None -> FALLBACK_BACKGROUND_ASSET
        is MainScreenImageState.SystemBackdrop -> state.fanartPath?.let { File(it) } ?: FALLBACK_BACKGROUND_ASSET
        is MainScreenImageState.GameBackdrop -> state.backdropPath?.let { File(it) } ?: FALLBACK_BACKGROUND_ASSET
    }

    val logoModel: Any? = when (state) {
        is MainScreenImageState.SystemBackdrop -> state.systemLogoAssetPath
        is MainScreenImageState.GameBackdrop -> state.logoPath?.let { File(it) }
        MainScreenImageState.None -> null
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Opaque backdrop: crossfades, since a hard swap here would show the empty
        // background behind it for a frame (the original white-flash problem).
        CrossfadeAsyncImage(
            model = backdropModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Logo: transparent PNG/SVG, not full-bleed - there's nothing opaque behind it
        // to flash to, so a hard swap on a single persistent AsyncImage is enough. Using
        // CrossfadeAsyncImage here instead briefly showed both the outgoing and incoming
        // logo layered on top of each other, since neither has an opaque background to
        // hide the other during the overlap.
        AsyncImage(
            model = logoModel,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.6f)
                .fillMaxHeight(0.25f),
        )
    }
}