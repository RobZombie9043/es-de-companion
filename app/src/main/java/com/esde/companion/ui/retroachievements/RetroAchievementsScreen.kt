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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.esde.companion.domain.model.AchievementItem
import com.esde.companion.domain.model.GameAchievementSummary
import com.esde.companion.domain.model.MatchMethod
import kotlin.math.roundToInt

/**
 * Full-screen achievement summary, shown via a plain [androidx.compose.runtime.saveable.rememberSaveable]
 * boolean in MainActivity (see the RetroAchievements FAB) - same "not a nav destination"
 * placement as [com.esde.companion.ui.manual.GameManualScreen]. Resolution and fetch are
 * independent stages (see [RetroAchievementsUiState.kt]) so a network failure never reads
 * as "wrong game" and vice versa.
 *
 * The match-method caption is informational only in this phase - the actual "wrong game?"
 * correction picker lands in a later PR (see CLAUDE.md's RetroAchievements section).
 */
@Composable
fun RetroAchievementsScreen(
    viewModel: RetroAchievementsViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolution by viewModel.resolution.collectAsStateWithLifecycle()
    val fetch by viewModel.fetch.collectAsStateWithLifecycle()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            RetroAchievementsTopBar(onExit = onExit)
            RetroAchievementsBody(resolution = resolution, fetch = fetch, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun RetroAchievementsTopBar(onExit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "Achievements", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onExit) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
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
            RetroAchievementsFetchBody(method = resolution.method, fetch = fetch, modifier = modifier)
    }
}

@Composable
private fun RetroAchievementsFetchBody(
    method: MatchMethod,
    fetch: RetroAchievementsFetchState,
    modifier: Modifier,
) {
    when (fetch) {
        RetroAchievementsFetchState.Idle, RetroAchievementsFetchState.Loading ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is RetroAchievementsFetchState.Loaded ->
            AchievementSummaryList(summary = fetch.summary, method = method, modifier = modifier)
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
    method: MatchMethod,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        AchievementSummaryHeader(summary, method)
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
private fun AchievementSummaryHeader(
    summary: GameAchievementSummary,
    method: MatchMethod,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = summary.gameTitle, style = MaterialTheme.typography.titleMedium)
        val completionPercent = summary.completionPercent.roundToInt()
        val pointsText = "${summary.earnedPoints} / ${summary.totalPoints} points - $completionPercent%"
        Text(text = pointsText, style = MaterialTheme.typography.bodyMedium)
        MatchMethodCaption(method)
    }
}

@Composable
private fun MatchMethodCaption(method: MatchMethod) {
    val (text, alpha) =
        when (method) {
            MatchMethod.ExactTitle -> "Matched by title" to LOW_EMPHASIS_ALPHA
            MatchMethod.NormalizedTitle -> "Matched by similar title - is this the right game?" to HIGH_EMPHASIS_ALPHA
            MatchMethod.ManualOverride -> "Matched via your correction" to LOW_EMPHASIS_ALPHA
        }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
        modifier = Modifier.padding(top = 4.dp),
    )
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
private const val UNEARNED_ALPHA = 0.5f
private const val LOW_EMPHASIS_ALPHA = 0.5f
private const val HIGH_EMPHASIS_ALPHA = 0.9f
