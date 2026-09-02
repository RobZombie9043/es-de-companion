package com.esde.companion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.domain.model.GameLaunchOverride
import com.esde.companion.domain.model.GamelistSystemSummary
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.parser.GamelistGameEntry

/**
 * Settings > UI Settings > Game Launch Override > systems list - every system with a
 * gamelist.xml at the standard location (see `GamelistLibraryRepository`). Tapping a system
 * drills into [GameLaunchOverrideGamesScreen]. [SourceNote] sits below the shared page header
 * (see `LongPressSettingsMenu`'s `SettingsMenuHeader`) so users don't mistake this for a full
 * ES-DE system/library browser.
 */
@Composable
fun GameLaunchOverrideSystemsScreen(
    viewModel: GameLaunchOverrideViewModel,
    onSystemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onSystemsScreenShown() }

    Column(modifier = modifier.fillMaxSize()) {
        SourceNote()
        when {
            uiState.isLoadingSystems -> LoadingIndicator(Modifier.weight(1f))
            uiState.systems.isEmpty() ->
                EmptyMessage("No systems with a gamelist.xml were found.", Modifier.weight(1f))
            else ->
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(uiState.systems, key = { it.shortName }) { system ->
                        SystemRow(system = system, onClick = { onSystemSelected(system.shortName) })
                    }
                }
        }
    }
}

@Composable
private fun SourceNote() {
    Text(
        text = "Systems and games are populated from gamelist.xml files.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SystemRow(
    system: GamelistSystemSummary,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = system.shortName, style = MaterialTheme.typography.bodyLarge)
                val gameCountText = if (system.gameCount == 1) "1 game" else "${system.gameCount} games"
                Text(text = gameCountText, style = MaterialTheme.typography.bodySmall)
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

/**
 * Settings > UI Settings > Game Launch Override > one system's games, sorted alphabetically by
 * name. The system-wide default app picker (see [SystemDefaultRow]) is the first row of the
 * same scrollable list as the games themselves (see [GameRow]) rather than a fixed header above
 * it - it scrolls out of view along with everything else, leaving the full screen height for
 * browsing games in a large collection. [systemShortName] drives
 * [GameLaunchOverrideViewModel.onSystemSelected] via [LaunchedEffect], so re-entering this page
 * for a different system (browsing back to the systems list and into another one) always loads
 * that system's own games.
 */
@Composable
fun GameLaunchOverrideGamesScreen(
    viewModel: GameLaunchOverrideViewModel,
    systemShortName: String,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(systemShortName) { viewModel.onSystemSelected(systemShortName) }

    val games = remember(uiState.currentSystemGames) { uiState.currentSystemGames.sortedBy { it.name.lowercase() } }

    if (uiState.isLoadingGames && games.isEmpty()) {
        LoadingIndicator(modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            SystemDefaultRow(
                currentPackageName = uiState.systemDefaults[systemShortName],
                installedApps = uiState.installedApps,
                onChanged = { packageName -> viewModel.onSystemDefaultChanged(systemShortName, packageName) },
            )
        }
        if (games.isEmpty()) {
            item {
                Text(
                    text = "No games found for this system.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            }
        } else {
            items(games, key = { it.relativeRomPath }) { game ->
                val override =
                    uiState.gameOverrides.firstOrNull {
                        it.systemShortName == systemShortName && it.relativeRomPath == game.relativeRomPath
                    }
                val actions =
                    GameOverrideActions(
                        onAppSelected = { packageName ->
                            viewModel.onGameOverrideChanged(systemShortName, game.relativeRomPath, packageName)
                        },
                        onNoneSelected = {
                            viewModel.onGameOverrideChanged(systemShortName, game.relativeRomPath, null)
                        },
                        onUseDefaultSelected = {
                            viewModel.onGameOverrideClearedToDefault(systemShortName, game.relativeRomPath)
                        },
                    )
                GameRow(
                    game = game,
                    override = override,
                    systemDefaultPackageName = uiState.systemDefaults[systemShortName],
                    installedApps = uiState.installedApps,
                    actions = actions,
                )
            }
        }
    }
}

@Composable
private fun SystemDefaultRow(
    currentPackageName: String?,
    installedApps: List<InstalledApp>,
    onChanged: (String?) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val currentLabel = currentPackageName?.let { pkg -> installedApps.labelFor(pkg) } ?: "None"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsLabel(icon = Icons.Filled.SportsEsports, text = "System Default")
            Surface(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = SettingsItemShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = currentLabel, style = MaterialTheme.typography.bodyMedium)
                    Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
                }
            }
        }
    }

    if (showPicker) {
        val pinnedEntries =
            listOf(
                PinnedAppEntry("None") {
                    onChanged(null)
                    showPicker = false
                },
            )
        SelectAppDialog(
            installedApps = installedApps,
            pinnedEntries = pinnedEntries,
            onAppPicked = { packageName ->
                onChanged(packageName)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

/** Bundles [GameRow]'s three override-picker callbacks into one parameter, keeping that
 * composable's own parameter count under detekt's LongParameterList threshold - same reasoning
 * as [UISettingsContent]'s `DimAmountControl`. */
private data class GameOverrideActions(
    val onAppSelected: (String) -> Unit,
    val onNoneSelected: () -> Unit,
    val onUseDefaultSelected: () -> Unit,
)

@Composable
private fun GameRow(
    game: GamelistGameEntry,
    override: GameLaunchOverride?,
    systemDefaultPackageName: String?,
    installedApps: List<InstalledApp>,
    actions: GameOverrideActions,
) {
    var showPicker by remember { mutableStateOf(false) }
    val effectiveLabel =
        when {
            override == null -> "Default: ${systemDefaultPackageName?.let { installedApps.labelFor(it) } ?: "None"}"
            override.packageName == null -> "None"
            else -> installedApps.labelFor(override.packageName)
        }

    Surface(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = game.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = effectiveLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showPicker) {
        val pinnedEntries =
            listOf(
                PinnedAppEntry("Use System Default") {
                    actions.onUseDefaultSelected()
                    showPicker = false
                },
                PinnedAppEntry("None") {
                    actions.onNoneSelected()
                    showPicker = false
                },
            )
        SelectAppDialog(
            installedApps = installedApps,
            pinnedEntries = pinnedEntries,
            onAppPicked = { packageName ->
                actions.onAppSelected(packageName)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

private fun List<InstalledApp>.labelFor(packageName: String): String {
    return firstOrNull { it.packageName == packageName }?.label ?: packageName
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun EmptyMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}
