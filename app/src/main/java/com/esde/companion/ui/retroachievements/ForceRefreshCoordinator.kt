package com.esde.companion.ui.retroachievements

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lets a caller force a `collectLatest`-driven pipeline to re-run even though its own key
 * (e.g. the current game/selected game) hasn't changed - [trigger] is combined alongside
 * that key so bumping it re-enters the pipeline, and [consume] is read-and-cleared exactly
 * once per re-run to decide whether *this* run should bypass a cache. Shared by
 * [RetroAchievementsViewModel] and [RetroAchievementsSystemGamesViewModel]'s manual-refresh
 * entry points - each owns its own instance, since they refresh independent targets.
 */
class ForceRefreshCoordinator {
    private val _trigger = MutableStateFlow(0)
    val trigger: StateFlow<Int> = _trigger

    private var pending = false

    /** Bumps [trigger] and marks the next [consume] call to bypass its cache - a user-requested manual refresh. */
    fun request() {
        pending = true
        _trigger.value += 1
    }

    /**
     * Bumps [trigger] without forcing a cache bypass - for a re-resolve that's needed for
     * another reason (e.g. a game-match override was just persisted), where the new target
     * is already guaranteed to be a cache miss on its own.
     */
    fun retrigger() {
        _trigger.value += 1
    }

    /** Read-and-clear; call exactly once per pipeline re-run, same as a local `pendingForceRefresh` field. */
    fun consume(): Boolean = pending.also { pending = false }
}
