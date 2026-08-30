package com.esde.companion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Settings > Game Guides > "Add Guide" > one system's games - reuses
 * [GameLaunchOverrideViewModel]'s own systems/games listing (the exact "Launch App on Game
 * Start" > "Manage Systems & Games" mechanism this feature was asked to mirror) purely for
 * its gamelist.xml scan, not for anything launch-override-specific: this screen only needs a
 * plain "tap a game to pick it" row, unlike [GameLaunchOverrideGamesScreen]'s per-game app
 * picker. [onGameSelected] carries both the game's ES-DE-relative ROM path (see
 * [com.esde.companion.domain.model.identifies]'s kdoc for why that's enough to key a
 * downloaded guide by) and its scraped display name (for the GameFAQs search query and the
 * guide's own title-cleaning).
 */
@Composable
fun AddGuideGamesScreen(
    viewModel: GameLaunchOverrideViewModel,
    systemShortName: String,
    onGameSelected: (relativeRomPath: String, gameName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(systemShortName) { viewModel.onSystemSelected(systemShortName) }

    val games = remember(uiState.currentSystemGames) { uiState.currentSystemGames.sortedBy { it.name.lowercase() } }

    if (uiState.isLoadingGames && games.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (games.isEmpty()) {
        EmptyMessage("No games found for this system.", modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(games, key = { it.relativeRomPath }) { game ->
            DrillDownRow(
                title = game.name,
                subtitle = game.relativeRomPath,
                onClick = { onGameSelected(game.relativeRomPath, game.name) },
            )
        }
    }
}
