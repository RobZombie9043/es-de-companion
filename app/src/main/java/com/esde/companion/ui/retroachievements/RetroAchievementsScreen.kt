package com.esde.companion.ui.retroachievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.esde.companion.domain.model.AchievementItem
import com.esde.companion.domain.model.GameAchievementSummary
import kotlin.math.roundToInt

/**
 * Full-screen achievement summary, shown via a plain [androidx.compose.runtime.saveable.rememberSaveable]
 * boolean in MainActivity (see the RetroAchievements FAB) - same "not a nav destination"
 * placement as [com.esde.companion.ui.manual.GameManualScreen]. Resolution and fetch are
 * independent stages (see [RetroAchievementsUiState.kt]) so a network failure never reads
 * as "wrong game" and vice versa.
 *
 * The manual correction picker ([GameCorrectionDialog]) is deliberately tucked behind the
 * top bar's kebab menu (see [RetroAchievementsTopBar]), the same "small options button
 * rather than persistent chrome" idiom [com.esde.companion.ui.widgets.edit.EditWidgetsOverlay]
 * uses - the option to fix a wrong/missing match needs to exist, but title matching is
 * right often enough that it shouldn't compete for attention with the achievement list.
 */
@Composable
fun RetroAchievementsScreen(
    viewModel: RetroAchievementsViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolution by viewModel.resolution.collectAsStateWithLifecycle()
    val fetch by viewModel.fetch.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    var showCorrectionDialog by remember { mutableStateOf(false) }

    // A correction is only meaningful once a system/game is actually in play - not while
    // signed out, no game selected, or the system has no RetroAchievements console mapping.
    val canCorrect =
        resolution is RetroAchievementsResolutionState.Found ||
            resolution == RetroAchievementsResolutionState.NoMatch

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            RetroAchievementsTopBar(
                onExit = onExit,
                canCorrect = canCorrect,
                onRequestCorrection = {
                    viewModel.onSearchQueryChanged("")
                    showCorrectionDialog = true
                },
            )
            RetroAchievementsBody(resolution = resolution, fetch = fetch, modifier = Modifier.fillMaxSize())
        }
    }

    if (showCorrectionDialog) {
        GameCorrectionDialog(
            query = searchQuery,
            results = searchResults,
            onQueryChanged = viewModel::onSearchQueryChanged,
            onGameSelected = { candidate ->
                viewModel.onGameCorrected(candidate)
                showCorrectionDialog = false
            },
            onDismiss = { showCorrectionDialog = false },
        )
    }
}

@Composable
private fun RetroAchievementsTopBar(
    onExit: () -> Unit,
    canCorrect: Boolean,
    onRequestCorrection: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "Achievements", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (canCorrect) {
            OptionsMenu(onRequestCorrection = onRequestCorrection)
        }
        IconButton(onClick = onExit) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
        }
    }
}

/**
 * The kebab button and its menu are wrapped in the same [Box] - not siblings directly in
 * [RetroAchievementsTopBar]'s [Row] - so the [DropdownMenu] anchors to this button's own
 * position rather than wherever the [Row] would otherwise place an empty composable, the
 * same structure [com.esde.companion.ui.widgets.edit.EditWidgetsOverlay]'s options button
 * uses to open reliably next to itself instead of drifting off to one side.
 */
@Composable
private fun OptionsMenu(onRequestCorrection: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Achievement options")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MENU_SHAPE,
        ) {
            DropdownMenuItem(
                text = { Text("Change Game") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRequestCorrection()
                },
            )
        }
    }
}

@Composable
private fun RetroAchievementsBody(
    resolution: RetroAchievementsResolutionState,
    fetch: RetroAchievementsFetchState,
    modifier: Modifier,
) {
    when (resolution) {
        RetroAchievementsResolutionState.NotSignedIn -> {
            val message = "Sign in to RetroAchievements (Settings > RetroAchievements) to see achievements."
            RetroAchievementsMessage(message, modifier)
        }
        RetroAchievementsResolutionState.NoGame ->
            RetroAchievementsMessage("No game is currently selected.", modifier)
        RetroAchievementsResolutionState.UnsupportedSystem ->
            RetroAchievementsMessage("RetroAchievements doesn't support this system.", modifier)
        RetroAchievementsResolutionState.NoMatch ->
            RetroAchievementsMessage("No RetroAchievements entry found for this game.", modifier)
        is RetroAchievementsResolutionState.Found ->
            RetroAchievementsFetchBody(fetch = fetch, modifier = modifier)
    }
}

@Composable
private fun RetroAchievementsFetchBody(
    fetch: RetroAchievementsFetchState,
    modifier: Modifier,
) {
    when (fetch) {
        RetroAchievementsFetchState.Idle, RetroAchievementsFetchState.Loading ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is RetroAchievementsFetchState.Loaded ->
            AchievementSummaryList(summary = fetch.summary, modifier = modifier)
        RetroAchievementsFetchState.NotFound ->
            RetroAchievementsMessage("This game's RetroAchievements entry couldn't be found.", modifier)
        is RetroAchievementsFetchState.NetworkError ->
            RetroAchievementsMessage("Couldn't load achievements: ${fetch.message}", modifier)
    }
}

@Composable
private fun RetroAchievementsMessage(
    text: String,
    modifier: Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@Composable
private fun AchievementSummaryList(
    summary: GameAchievementSummary,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        AchievementSummaryHeader(summary)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(summary.achievements, key = { it.id }) { achievement -> AchievementRow(achievement) }
        }
    }
}

@Composable
private fun AchievementSummaryHeader(summary: GameAchievementSummary) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = summary.gameTitle, style = MaterialTheme.typography.titleMedium)
        val completionPercent = summary.completionPercent.roundToInt()
        val pointsText = "${summary.earnedPoints} / ${summary.totalPoints} points - $completionPercent%"
        Text(text = pointsText, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AchievementRow(achievement: AchievementItem) {
    val contentAlpha = if (achievement.unlocked) 1f else UNEARNED_ALPHA
    Row(
        modifier = Modifier.fillMaxWidth().alpha(contentAlpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = achievement.badgeUrl,
            contentDescription = null,
            modifier = Modifier.size(ACHIEVEMENT_BADGE_SIZE),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = achievement.title, style = MaterialTheme.typography.bodyLarge)
            Text(text = achievement.description, style = MaterialTheme.typography.bodySmall)
        }
        Text(text = "${achievement.points}", style = MaterialTheme.typography.labelLarge)
    }
}

private val ACHIEVEMENT_BADGE_SIZE = 48.dp
private val MENU_SHAPE = RoundedCornerShape(16.dp)
private const val UNEARNED_ALPHA = 0.5f
