package com.esde.companion.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.domain.model.EsdeConnectionState

/**
 * Top-level screen for the main (post-onboarding) app. The state/args/cover-image info
 * is shown as a [StateOverlay] floating over the rest of the screen, rather than being
 * the screen's entire content - it's independent of whatever else eventually lives here
 * (widgets, media, etc.) and can be toggled off from Settings.
 */
@Composable
fun MainScreen(viewModel: MainViewModel, onOpenSettings: () -> Unit) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val coverImageStatus by viewModel.coverImageStatus.collectAsStateWithLifecycle()
    val overlayEnabled by viewModel.overlayEnabled.collectAsStateWithLifecycle()
    MainScreenContent(
        connectionState = connectionState,
        coverImageStatus = coverImageStatus,
        overlayEnabled = overlayEnabled,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    connectionState: EsdeConnectionState,
    coverImageStatus: CoverImageStatus?,
    overlayEnabled: Boolean,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Base layer for the main screen itself - currently just background, since
            // nothing else lives here yet (see CLAUDE.md: widgets/media are iterative,
            // not part of this slice). The overlay below is deliberately independent of
            // whatever ends up here.
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}

            AnimatedVisibility(
                visible = overlayEnabled,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            ) {
                StateOverlay(
                    connectionState = connectionState,
                    coverImageStatus = coverImageStatus,
                )
            }
        }
    }
}