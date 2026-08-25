package com.esde.companion.ui.retroachievements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.esde.companion.domain.model.RetroAchievementsCandidateGame

/**
 * The manual correction picker - opened from [RetroAchievementsScreen]'s top bar kebab
 * menu (see its kdoc for why this isn't a persistently-visible button). Backed by
 * [RetroAchievementsViewModel.searchQuery]/[RetroAchievementsViewModel.searchResults],
 * which scope the search to the current game's system (see
 * [com.esde.companion.domain.usecase.SearchRetroAchievementsGamesUseCase]).
 */
@Composable
internal fun GameCorrectionDialog(
    query: String,
    results: List<RetroAchievementsCandidateGame>,
    onQueryChanged: (String) -> Unit,
    onGameSelected: (RetroAchievementsCandidateGame) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select the correct game") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.heightIn(max = CORRECTION_DIALOG_LIST_HEIGHT).padding(top = 12.dp)) {
                    items(results, key = { it.gameId }) { candidate ->
                        CandidateRow(candidate = candidate, onClick = { onGameSelected(candidate) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun CandidateRow(
    candidate: RetroAchievementsCandidateGame,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(model = candidate.iconUrl, contentDescription = null, modifier = Modifier.size(CANDIDATE_ICON_SIZE))
        Text(text = candidate.title, style = MaterialTheme.typography.bodyMedium)
    }
}

private val CANDIDATE_ICON_SIZE = 32.dp
private val CORRECTION_DIALOG_LIST_HEIGHT = 320.dp
