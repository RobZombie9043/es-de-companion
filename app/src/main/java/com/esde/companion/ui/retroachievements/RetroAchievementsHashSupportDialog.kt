package com.esde.companion.ui.retroachievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.GameHashSupport

/**
 * The "Supported Hashes" picker - opened from [RetroAchievementsScreen]'s top bar kebab menu
 * (see [OptionsMenu]), same low-key placement as [GameCorrectionDialog]. Shows the current
 * ROM's own hash (parsed from gamelist.xml, see [GetGameHashSupportUseCase]) alongside every
 * hash RA has on file for the already-resolved game, with a checkmark next to whichever
 * supported hash matches - so a user unsure why a ROM hash-matched (or didn't) can see the
 * actual values being compared, rather than just "matched"/"not matched" in the abstract.
 */
@Composable
internal fun HashSupportDialog(
    state: HashSupportState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supported Game Hashes") },
        text = {
            when (state) {
                HashSupportState.Hidden -> Unit
                HashSupportState.Loading ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                is HashSupportState.Loaded -> HashSupportContent(state.support)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun HashSupportContent(support: GameHashSupport) {
    val currentHash = support.currentHash
    val normalizedCurrentHash = currentHash?.trim()?.lowercase()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "This game's hash: ${currentHash ?: "Unknown"}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Hashes RetroAchievements supports for this game:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        if (support.supportedHashes.isEmpty()) {
            Text(text = "None on file.", style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = HASH_LIST_HEIGHT)) {
                items(support.supportedHashes) { hash ->
                    HashRow(hash = hash, isMatch = hash.trim().lowercase() == normalizedCurrentHash)
                }
            }
        }
    }
}

@Composable
private fun HashRow(
    hash: String,
    isMatch: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = hash, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (isMatch) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Matches this game's hash",
                tint = MATCH_TINT_GREEN,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private val HASH_LIST_HEIGHT = 240.dp

@Suppress("MagicNumber")
private val MATCH_TINT_GREEN = Color(0xFF4CAF50)
