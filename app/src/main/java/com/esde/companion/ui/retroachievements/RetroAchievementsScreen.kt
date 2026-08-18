package com.esde.companion.ui.retroachievements

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.esde.companion.domain.model.MatchMethod
import com.esde.companion.ui.CORNER_BUTTON_EDGE_PADDING
import com.esde.companion.ui.theme.LocalIsDarkTheme
import com.esde.companion.ui.widgets.fallbackBackgroundAssetPath

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
 *
 * Styled like the main menu rather than a plain settings-style page: the same themed
 * fallback background image widgets use when they have nothing real to show (see
 * [fallbackBackgroundAssetPath]) as a full-bleed backdrop, with each achievement rendered
 * as its own translucent rounded "button" (see [AchievementRow]) at the shared Settings >
 * UI Settings Overlay Opacity value, [overlayOpacityPercent] - the same
 * black-in-dark/white-in-light translucent-surface convention [com.esde.companion.ui.CornerFab]
 * and the App Dock/music panel already use, rather than a flat Material background color.
 */
@Composable
fun RetroAchievementsScreen(
    viewModel: RetroAchievementsViewModel,
    overlayOpacityPercent: Int,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolution by viewModel.resolution.collectAsStateWithLifecycle()
    val fetch by viewModel.fetch.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val displayField by viewModel.displayField.collectAsStateWithLifecycle()
    var showCorrectionDialog by remember { mutableStateOf(false) }

    // Only composed while this screen is showing (AnimatedVisibility in MainActivity), so
    // it's registered later than MainScreen's own BackHandler and wins Compose's LIFO back
    // dispatch - without it, back would fall through to MainScreen's handler underneath and
    // do nothing, same fix as GameManualScreen's.
    BackHandler(onBack = onExit)

    // A correction is only meaningful once a system/game is actually in play - not while
    // signed out, no game selected, or the system has no RetroAchievements console mapping.
    val canCorrect =
        resolution is RetroAchievementsResolutionState.Found ||
            resolution == RetroAchievementsResolutionState.NoMatch

    // Hash support needs an actual resolved RA gameId (see RetroAchievementsViewModel.lastGameId),
    // which only exists once resolution has landed on Found - NoMatch has nothing to look up.
    val canShowHashSupport = resolution is RetroAchievementsResolutionState.Found

    // Same "needs a resolved gameId" gate as canShowHashSupport - kept as its own named val for
    // clarity even though the underlying condition is currently identical.
    val canRefresh = resolution is RetroAchievementsResolutionState.Found

    // One theme-derived content color for everything in this screen (title text, icons,
    // achievement titles, messages) - white against the dark background image, black
    // against the light one - rather than each piece of text/icon picking its own default
    // Material color, which wouldn't reliably contrast with an arbitrary backdrop image.
    val contentColor = themedContentColor()

    Box(modifier = modifier.fillMaxSize().blockAppDrawerSwipeFallthrough()) {
        AsyncImage(
            model = fallbackBackgroundAssetPath(LocalIsDarkTheme.current),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Column(modifier = Modifier.fillMaxSize()) {
                val menuActions =
                    OptionsMenuActions(
                        canCorrect = canCorrect,
                        onRequestCorrection = {
                            viewModel.onSearchQueryChanged("")
                            showCorrectionDialog = true
                        },
                        canShowHashSupport = canShowHashSupport,
                        onRequestHashSupport = viewModel::onRequestHashSupport,
                        canRefresh = canRefresh,
                        onRequestRefresh = viewModel::onRefreshRequested,
                    )
                RetroAchievementsTopBar(onExit = onExit, menuActions = menuActions)
                val listControls =
                    AchievementListControls(
                        sort = DropdownSelection(sortOrder, viewModel::onSortOrderChanged),
                        filter = DropdownSelection(filter, viewModel::onFilterChanged),
                        display = DropdownSelection(displayField, viewModel::onDisplayFieldChanged),
                        overlayOpacityPercent = overlayOpacityPercent,
                    )
                RetroAchievementsBody(
                    resolution = resolution,
                    fetch = fetch,
                    listControls = listControls,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    RetroAchievementsDialogs(
        viewModel = viewModel,
        showCorrectionDialog = showCorrectionDialog,
        onCorrectionDialogDismissed = { showCorrectionDialog = false },
    )
}

/** Both non-inline dialogs this screen can show, pulled out so [RetroAchievementsScreen] itself stays short. */
@Composable
private fun RetroAchievementsDialogs(
    viewModel: RetroAchievementsViewModel,
    showCorrectionDialog: Boolean,
    onCorrectionDialogDismissed: () -> Unit,
) {
    if (showCorrectionDialog) {
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
        GameCorrectionDialog(
            query = searchQuery,
            results = searchResults,
            onQueryChanged = viewModel::onSearchQueryChanged,
            onGameSelected = { candidate ->
                viewModel.onGameCorrected(candidate)
                onCorrectionDialogDismissed()
            },
            onDismiss = onCorrectionDialogDismissed,
        )
    }

    val hashSupport by viewModel.hashSupport.collectAsStateWithLifecycle()
    if (hashSupport != HashSupportState.Hidden) {
        HashSupportDialog(state = hashSupport, onDismiss = viewModel::onHashSupportDismissed)
    }
}

/**
 * Bundles [OptionsMenu]'s three independent enabled/action pairs into one param - the same
 * "bundle related params to stay under detekt's LongParameterList limit" convention
 * [AchievementListControls]/`RetroAchievementsDetailUseCases` use.
 */
private data class OptionsMenuActions(
    val canCorrect: Boolean,
    val onRequestCorrection: () -> Unit,
    val canShowHashSupport: Boolean,
    val onRequestHashSupport: () -> Unit,
    val canRefresh: Boolean,
    val onRequestRefresh: () -> Unit,
)

@Composable
private fun RetroAchievementsTopBar(
    onExit: () -> Unit,
    menuActions: OptionsMenuActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = CORNER_BUTTON_EDGE_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "Achievements", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        // Kebab sits left of Close, but its menu still opens flush with the true screen
        // edge (see OptionsMenu's kdoc) rather than sliding left along with the icon.
        val showKebab = menuActions.canCorrect || menuActions.canShowHashSupport || menuActions.canRefresh
        if (showKebab) {
            OptionsMenu(menuActions)
        }
        IconButton(onClick = onExit) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
        }
    }
}

/**
 * The kebab button sits to the left of Close, but its [DropdownMenu] is offset rightward
 * by [ICON_BUTTON_WIDTH] - one corner button's width - so the popup still opens at the
 * same screen position it did when the kebab itself was the rightmost element (flush with
 * CORNER_BUTTON_EDGE_PADDING from the true edge), rather than anchoring to the kebab's own
 * now-more-central position. The icon and its menu are wrapped in the same [Box] - not
 * siblings directly in [RetroAchievementsTopBar]'s [Row] - so the [DropdownMenu] anchors
 * to this button's own position rather than wherever the [Row] would otherwise place an
 * empty composable, the same structure [com.esde.companion.ui.widgets.edit.EditWidgetsOverlay]'s
 * options button uses to open reliably next to itself instead of drifting off to one side.
 */
@Composable
private fun OptionsMenu(actions: OptionsMenuActions) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Achievement options")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = ICON_BUTTON_WIDTH, y = 0.dp),
            shape = MENU_SHAPE,
        ) {
            if (actions.canCorrect) {
                DropdownMenuItem(
                    text = { Text("Change Game") },
                    leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                    onClick = {
                        expanded = false
                        actions.onRequestCorrection()
                    },
                )
            }
            if (actions.canShowHashSupport) {
                DropdownMenuItem(
                    text = { Text("Supported Hashes") },
                    leadingIcon = { Icon(imageVector = Icons.Filled.Fingerprint, contentDescription = null) },
                    onClick = {
                        expanded = false
                        actions.onRequestHashSupport()
                    },
                )
            }
            if (actions.canRefresh) {
                DropdownMenuItem(
                    text = { Text("Refresh") },
                    leadingIcon = { Icon(imageVector = Icons.Filled.Refresh, contentDescription = null) },
                    onClick = {
                        expanded = false
                        actions.onRequestRefresh()
                    },
                )
            }
        }
    }
}

@Composable
private fun RetroAchievementsBody(
    resolution: RetroAchievementsResolutionState,
    fetch: RetroAchievementsFetchState,
    listControls: AchievementListControls,
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
            RetroAchievementsFetchBody(
                fetch = fetch,
                listControls = listControls,
                isHashMatched = resolution.method == MatchMethod.RomHash,
                modifier = modifier,
            )
    }
}

private val ICON_BUTTON_WIDTH = 48.dp // Material3 IconButton's default touch target width.
private val MENU_SHAPE = RoundedCornerShape(16.dp)
