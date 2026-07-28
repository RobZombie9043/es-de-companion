package com.esde.companion.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.domain.model.EsdeConnectionState

/**
 * Top-level screen for the main (post-onboarding) app. Branches on [EsdeConnectionState]
 * rather than AppState directly, so "es_log.txt not found" can be shown distinctly from
 * any particular AppState - see EsdeConnectionState's kdoc for why that split exists.
 */
@Composable
fun MainScreen(viewModel: MainViewModel, onOpenSettings: () -> Unit) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val coverImageStatus by viewModel.coverImageStatus.collectAsStateWithLifecycle()
    MainScreenContent(
        connectionState = connectionState,
        coverImageStatus = coverImageStatus,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    connectionState: EsdeConnectionState,
    coverImageStatus: CoverImageStatus?,
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
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = connectionState.toDisplayText() + coverImageStatus.toDisplayText(),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun EsdeConnectionState.toDisplayText(): String = when (this) {
    is EsdeConnectionState.LogFileNotFound -> "es_log.txt not found"
    is EsdeConnectionState.Connected -> appState.toDisplayText()
}