package com.esde.companion.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.domain.model.AppState

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    MainScreenContent(appState = appState)
}

/**
 * Pure, stateless rendering of [appState] - kept separate from [MainScreen] so it can
 * be previewed/tested without a ViewModel or real event stream behind it.
 */
@Composable
private fun MainScreenContent(appState: AppState) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = appState.toDisplayText(),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
