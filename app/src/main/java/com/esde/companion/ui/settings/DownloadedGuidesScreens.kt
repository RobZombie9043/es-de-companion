@file:Suppress("TooManyFunctions")

package com.esde.companion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.GameGuideFormat
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.identifies
import java.util.Locale
import kotlin.math.roundToInt

private const val BYTES_PER_KB = 1024.0
private const val BYTES_PER_MB = BYTES_PER_KB * 1024.0

/**
 * Settings > Game Guides > "Browse Downloaded Guides" - three drill-down levels (system,
 * then game, then that game's own downloaded guides) over
 * [DownloadedGuidesViewModel.allGuides], the same [SettingsItemShape]-row/[LazyColumn] shape
 * `GameLaunchOverrideScreens.kt`'s own system/game drill-down already uses. A game has no
 * scraped display name available here (this only ever sees
 * [DownloadedGameGuide.gameReference], not ES-DE's own gamelist data), so
 * [displayNameForRomPath] falls back to the ROM's own filename - good enough to tell
 * downloaded games apart for storage management, even if it doesn't match ES-DE's real title.
 */
@Composable
fun DownloadedGuidesSystemsScreen(
    viewModel: DownloadedGuidesViewModel,
    onSystemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allGuides by viewModel.allGuides.collectAsStateWithLifecycle()
    val systems =
        remember(allGuides) {
            allGuides
                .groupBy { it.gameReference.systemShortName }
                .map { (systemShortName, guides) -> systemShortName to guides.size }
                .sortedBy { it.first.lowercase() }
        }

    if (systems.isEmpty()) {
        EmptyMessage("No guides downloaded yet.", modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(systems, key = { it.first }) { (systemShortName, guideCount) ->
            DrillDownRow(
                title = systemShortName,
                subtitle = guideCountLabel(guideCount),
                onClick = { onSystemSelected(systemShortName) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

/** One system's downloaded games - [systemShortName] identifies which system, [onGameSelected]
 * carries the game's own [com.esde.companion.domain.model.GameReference.romPath] onward. */
@Composable
fun DownloadedGuidesGamesScreen(
    viewModel: DownloadedGuidesViewModel,
    systemShortName: String,
    onGameSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allGuides by viewModel.allGuides.collectAsStateWithLifecycle()
    val games =
        remember(allGuides, systemShortName) {
            groupGuidesByGame(allGuides.filter { it.gameReference.systemShortName == systemShortName })
                .map { group ->
                    val romPath = group.first().gameReference.romPath
                    Triple(romPath, displayNameForRomPath(romPath), group.size)
                }
                .sortedBy { it.second.lowercase() }
        }

    if (games.isEmpty()) {
        EmptyMessage("No guides downloaded for this system.", modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(games, key = { it.first }) { (romPath, displayName, guideCount) ->
            DrillDownRow(
                title = displayName,
                subtitle = guideCountLabel(guideCount),
                onClick = { onGameSelected(romPath) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

/** One game's own downloaded guides - [systemShortName]/[romPath] together identify the game,
 * the same two fields [com.esde.companion.domain.model.identifies] compares (deliberately not
 * full [com.esde.companion.domain.model.GameReference] equality - see that function's kdoc for
 * why). */
@Composable
fun DownloadedGuidesListScreen(
    viewModel: DownloadedGuidesViewModel,
    systemShortName: String,
    romPath: String,
    onOpenGuide: (DownloadedGameGuide) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allGuides by viewModel.allGuides.collectAsStateWithLifecycle()
    val guides =
        remember(allGuides, systemShortName, romPath) {
            val target = GameReference(systemShortName, romPath)
            allGuides
                .filter { it.gameReference.identifies(target) }
                .sortedBy { it.title.lowercase() }
        }

    if (guides.isEmpty()) {
        EmptyMessage("No guides remain for this game.", modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(guides, key = { it.id }) { guide ->
            DownloadedGuideRow(
                guide = guide,
                onOpen = { onOpenGuide(guide) },
                onDelete = { viewModel.onDeleteGuide(guide.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
internal fun DrillDownRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun DownloadedGuideRow(
    guide: DownloadedGameGuide,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
                Text(text = downloadedGuideSubtitle(guide), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    showDeleteConfirmation = true
                },
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }

    // Same "sits right next to the tappable row" mis-tap risk the sibling "Clear All
    // Downloaded Guides" action (ClearAllGuidesConfirmationDialog) is already guarded
    // against - this row's own delete button gets the same protection.
    if (showDeleteConfirmation) {
        DeleteGuideConfirmationDialog(
            guideTitle = guide.title,
            onConfirm = {
                showDeleteConfirmation = false
                onDelete()
            },
            onDismiss = { showDeleteConfirmation = false },
        )
    }
}

@Composable
private fun DeleteGuideConfirmationDialog(
    guideTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this guide?") },
        text = { Text("\"$guideTitle\" will be removed from offline storage. This can't be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Groups [guides] (already filtered to one system) into per-game buckets using
 * [identifies] rather than a plain `groupBy { romPath }` - a guide added via the in-game FAB
 * (ES-DE's live absolute romPath) and one added for the same game via Settings > Add Guide (a
 * gamelist.xml-relative romPath) would otherwise land in two separate buckets even though
 * [FileGameGuideLibraryRepository.observeGuidesFor] already treats them as the same game.
 * Linear/O(n²), same as this codebase's other "expected count per device is small" grouping
 * (see [FileGameGuideLibraryRepository]'s own kdoc) - `identifies` isn't a hashable equality
 * key, so a `groupBy` can't be used directly.
 */
private fun groupGuidesByGame(guides: List<DownloadedGameGuide>): List<List<DownloadedGameGuide>> {
    val groups = mutableListOf<MutableList<DownloadedGameGuide>>()
    for (guide in guides) {
        val group = groups.find { it.first().gameReference.identifies(guide.gameReference) }
        if (group != null) group.add(guide) else groups.add(mutableListOf(guide))
    }
    return groups
}

private fun guideCountLabel(count: Int): String = if (count == 1) "1 guide" else "$count guides"

internal fun displayNameForRomPath(romPath: String): String {
    return romPath.substringAfterLast('/').substringBeforeLast('.').ifBlank { romPath }
}

private fun downloadedGuideSubtitle(guide: DownloadedGameGuide): String {
    return "${guide.format.shortLabel()} · ${formatGuideSize(guide.sizeBytes)}"
}

private fun GameGuideFormat.shortLabel(): String =
    when (this) {
        GameGuideFormat.PlainText -> "TXT"
        GameGuideFormat.Html -> "HTML"
        GameGuideFormat.Pdf -> "PDF"
        GameGuideFormat.Image -> "IMG"
    }

private fun formatGuideSize(bytes: Long): String =
    when {
        bytes < BYTES_PER_KB -> "$bytes B"
        bytes < BYTES_PER_MB -> "${(bytes / BYTES_PER_KB).roundToInt()} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes / BYTES_PER_MB)
    }
