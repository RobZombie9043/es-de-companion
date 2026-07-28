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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.domain.model.EsdeConnectionState

@Composable
fun MainScreen(viewModel: MainViewModel, onOpenSettings: () -> Unit) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val coverImageStatus by viewModel.coverImageStatus.collectAsStateWithLifecycle()
    val overlayEnabled by viewModel.overlayEnabled.collectAsStateWithLifecycle()
    val mainScreenImageState by viewModel.mainScreenImageState.collectAsStateWithLifecycle()
    MainScreenContent(
        connectionState = connectionState,
        coverImageStatus = coverImageStatus,
        overlayEnabled = overlayEnabled,
        mainScreenImageState = mainScreenImageState,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    connectionState: EsdeConnectionState,
    coverImageStatus: CoverImageStatus?,
    overlayEnabled: Boolean,
    mainScreenImageState: MainScreenImageState,
    onOpenSettings: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        MainScreenImages(state = mainScreenImageState, modifier = Modifier.fillMaxSize())

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {},
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                )
            },
        ) { innerPadding ->
            AnimatedVisibility(
                visible = overlayEnabled,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    StateOverlay(
                        connectionState = connectionState,
                        coverImageStatus = coverImageStatus,
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                }
            }
        }
    }
}