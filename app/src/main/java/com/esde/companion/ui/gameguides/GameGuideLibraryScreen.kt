package com.esde.companion.ui.gameguides

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.GameGuideFormat
import com.esde.companion.ui.theme.LocalIsDarkTheme
import com.esde.companion.ui.widgets.fallbackBackgroundAssetPath
import java.util.Locale
import kotlin.math.roundToInt

private const val BYTES_PER_KB = 1024.0
private const val BYTES_PER_MB = BYTES_PER_KB * 1024.0
private const val PERCENT_MULTIPLIER = 100

// Same 16dp rounding used for every other card-like row in the app (Settings' SettingsItemShape,
// the RetroAchievements/leaderboard tiles) so a guide's row reads as the same kind of surface.
private val GUIDE_ROW_SHAPE = RoundedCornerShape(16.dp)

/** Downloaded guides for the current game (Game Guides FAB, when at least one guide already
 * exists) - reopen a saved guide, delete one, or head back into the Browser to find another. */
@Composable
fun GameGuideLibraryScreen(
    state: GameGuidesUiState.Library,
    actions: GameGuideLibraryActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onClose)

    // Surface with an explicit contentColor, not a plain Modifier.background - this screen
    // is drawn as an overlay above the main screen/widgets with no ancestor Surface of its
    // own to inherit a correct LocalContentColor from (unlike the long-press Settings menu,
    // whose wrapping Surface lives one level up in MainScreen.kt), so Text/Icons below would
    // otherwise fall back to Compose's default black content color regardless of theme (see
    // GameGuideViewerScreen's matching fix) - color is left transparent here specifically so
    // the wallpaper image behind it (same asset/light-dark selection as the Settings menu's
    // own backdrop) shows through, rather than being painted over by a solid background.
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = fallbackBackgroundAssetPath(LocalIsDarkTheme.current),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // No per-row icon/title here (deliberately removed, see GuideRow) - each row
                // only ever needs the guide's own title, but the game name is still shown
                // once, here in the header, so it's clear at a glance which game's guides
                // this list belongs to.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.gameName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    IconButton(onClick = actions.onFindAnotherGuide) {
                        Icon(Icons.Filled.Add, contentDescription = "Find another guide")
                    }
                    IconButton(onClick = actions.onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.guides, key = { it.id }) { guide ->
                        GuideRow(
                            guide = guide,
                            readingProgressFraction = state.readingProgressByGuideId[guide.id] ?: 0f,
                            onOpen = { actions.onOpenGuide(guide) },
                            onDelete = { actions.onDeleteGuide(guide) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideRow(
    guide: DownloadedGameGuide,
    readingProgressFraction: Float,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    // Surface's own onClick (not a plain Modifier.clickable) so the ripple is clipped to
    // GUIDE_ROW_SHAPE instead of the row's full rectangular bounds - the same shape/onClick
    // pairing SettingsCategoryRow/ToggleSettingRow use.
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = GUIDE_ROW_SHAPE,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = guide.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = guideSubtitle(guide, readingProgressFraction),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // weight(1f) above keeps this visible regardless of title length - guide.title
            // comes straight from the source page's own <title> (see
            // GameFaqsBrowserBridge.DETECT_SCRIPT), which for an in-line HTML guide is
            // often long enough (e.g. "... FAQ/Walkthrough by ... - GameFAQs") to otherwise
            // push this off the edge of the row instead of just truncating the title text.
            IconButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                },
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

private fun guideSubtitle(
    guide: DownloadedGameGuide,
    readingProgressFraction: Float,
): String {
    val percent = (readingProgressFraction * PERCENT_MULTIPLIER).roundToInt().coerceIn(0, PERCENT_MULTIPLIER)
    val format = if (guide.format == GameGuideFormat.Html) "HTML" else "TXT"
    return "$format · ${formatGuideSize(guide.sizeBytes)} · $percent% read"
}

private fun formatGuideSize(bytes: Long): String =
    when {
        bytes < BYTES_PER_KB -> "$bytes B"
        bytes < BYTES_PER_MB -> "${(bytes / BYTES_PER_KB).roundToInt()} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes / BYTES_PER_MB)
    }
