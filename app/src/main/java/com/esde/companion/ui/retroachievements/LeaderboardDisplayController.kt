package com.esde.companion.ui.retroachievements

import com.esde.companion.domain.model.LeaderboardSortOrder
import com.esde.companion.domain.usecase.GetLeaderboardEntriesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Owns one leaderboard list's local sort control and its tap-to-expand entries accordion -
 * mirrors [AchievementDisplayController]'s shape exactly (see its kdoc), minus a filter/display
 * field, which don't apply to leaderboards. Shared by [RetroAchievementsViewModel] (the live game)
 * and [RetroAchievementsSystemGamesViewModel] (the system-browse drill-down), each owning its own
 * instance for its own leaderboard list.
 *
 * [onTargetChanged] must be called by the owner whenever the underlying leaderboard list's target
 * (game/gameId) changes, so a stale expanded entries row can't survive past the summary it
 * belonged to - same reasoning [AchievementDisplayController.onTargetChanged] documents.
 */
class LeaderboardDisplayController(
    private val getLeaderboardEntries: GetLeaderboardEntriesUseCase,
    scope: CoroutineScope,
) {
    private val _sortOrder = MutableStateFlow(LeaderboardSortOrder.DisplayOrderFirst)
    val sortOrder: StateFlow<LeaderboardSortOrder> = _sortOrder

    // Single-expand accordion for the leaderboard list's tap-to-show-entries row - see
    // ExpandedLeaderboardEntries' kdoc for the torn-read bug bundling id+fetch-state avoids.
    private val _expanded = MutableStateFlow<ExpandedLeaderboardEntries?>(null)
    val expanded: StateFlow<ExpandedLeaderboardEntries?> = _expanded

    init {
        scope.launch {
            _expanded.map { it?.leaderboardId }.distinctUntilChanged().collectLatest { leaderboardId ->
                if (leaderboardId == null) return@collectLatest
                val entries = getLeaderboardEntries(leaderboardId).toEntriesFetchState()
                _expanded.value = ExpandedLeaderboardEntries(leaderboardId, entries)
            }
        }
    }

    fun onSortOrderChanged(order: LeaderboardSortOrder) {
        _sortOrder.value = order
    }

    /**
     * Toggles the tapped leaderboard's entries section open/closed - only one open at a time.
     * Sets the new leaderboardId and a [LeaderboardEntriesFetchState.Loading] placeholder in the
     * same atomic write (see [ExpandedLeaderboardEntries]'s kdoc), so the row never briefly
     * renders with a mismatched previous leaderboard's entries content.
     */
    fun onLeaderboardTapped(leaderboardId: Long) {
        _expanded.value =
            if (_expanded.value?.leaderboardId == leaderboardId) {
                null
            } else {
                ExpandedLeaderboardEntries(leaderboardId, LeaderboardEntriesFetchState.Loading)
            }
    }

    fun onTargetChanged() {
        _expanded.value = null
    }
}
