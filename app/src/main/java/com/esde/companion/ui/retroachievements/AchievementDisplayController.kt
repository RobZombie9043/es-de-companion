package com.esde.companion.ui.retroachievements

import com.esde.companion.domain.model.AchievementDisplayField
import com.esde.companion.domain.model.AchievementFilterOption
import com.esde.companion.domain.model.AchievementSortOrder
import com.esde.companion.domain.usecase.GetAchievementCommentsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Owns one achievement list's local display controls (sort/filter/display-field) and its
 * tap-to-expand comments accordion - identical logic [RetroAchievementsViewModel] (the live
 * game) and [RetroAchievementsSystemGamesViewModel] (the system-browse drill-down) each
 * duplicated for their own achievement list. Takes [scope] rather than owning a ViewModel's
 * own `viewModelScope`, so it's constructible and unit-testable independent of any
 * ViewModel/Android lifecycle - each owning ViewModel passes its own `viewModelScope` in.
 *
 * [onTargetChanged] must be called by the owner whenever the underlying achievement
 * summary's target (game/gameId) changes, so a stale expanded comments row can't survive
 * past the summary it belonged to - same reasoning as the owners' own `lastKnownGame`/
 * `lastGameId` resets.
 */
class AchievementDisplayController(
    private val getAchievementComments: GetAchievementCommentsUseCase,
    scope: CoroutineScope,
) {
    private val _sortOrder = MutableStateFlow(AchievementSortOrder.DisplayOrderFirst)
    val sortOrder: StateFlow<AchievementSortOrder> = _sortOrder

    private val _filter = MutableStateFlow<Set<AchievementFilterOption>>(emptySet())
    val filter: StateFlow<Set<AchievementFilterOption>> = _filter

    private val _displayField = MutableStateFlow(AchievementDisplayField.UnlockRate)
    val displayField: StateFlow<AchievementDisplayField> = _displayField

    // Single-expand accordion for the achievement list's tap-to-show-comments row - see
    // ExpandedAchievementComments' kdoc for the torn-read bug bundling id+fetch-state avoids.
    private val _expanded = MutableStateFlow<ExpandedAchievementComments?>(null)
    val expanded: StateFlow<ExpandedAchievementComments?> = _expanded

    init {
        scope.launch {
            _expanded.map { it?.achievementId }.distinctUntilChanged().collectLatest { achievementId ->
                if (achievementId == null) return@collectLatest
                val comments = getAchievementComments(achievementId).toCommentsFetchState()
                _expanded.value = ExpandedAchievementComments(achievementId, comments)
            }
        }
    }

    fun onSortOrderChanged(order: AchievementSortOrder) {
        _sortOrder.value = order
    }

    fun onFilterChanged(filter: Set<AchievementFilterOption>) {
        _filter.value = filter
    }

    fun onDisplayFieldChanged(displayField: AchievementDisplayField) {
        _displayField.value = displayField
    }

    /**
     * Toggles the tapped achievement's comments section open/closed - only one open at a time.
     * Sets the new achievementId and a [CommentsFetchState.Loading] placeholder in the same
     * atomic write (see [ExpandedAchievementComments]'s kdoc), so the row never briefly renders
     * with a mismatched previous achievement's comments content.
     */
    fun onAchievementTapped(achievementId: Long) {
        _expanded.value =
            if (_expanded.value?.achievementId == achievementId) {
                null
            } else {
                ExpandedAchievementComments(achievementId, CommentsFetchState.Loading)
            }
    }

    fun onTargetChanged() {
        _expanded.value = null
    }
}
