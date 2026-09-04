package com.esde.companion.domain.model

/**
 * What the `WidgetType.PlaytimeStats` widget should currently show - mirrors
 * [AchievementSummaryWidgetState]'s Loading/Unavailable/Loaded shape exactly, since both
 * widgets are resolved from the same underlying [GameAchievementSummary] fetch (RA's
 * `GetGameInfoAndUserProgress` response already carries both achievements and playtime
 * stats) - see `WidgetsViewModel.resolveAchievementData`. `null` from the resolving lookup
 * (not a variant of this sealed class) means "not eligible at all" - no widget on the
 * canvas, no current game, or signed out - and resolves to `WidgetContent.Empty` (hidden
 * entirely), see `WidgetContentResolver`'s PlaytimeStats branch.
 */
sealed class PlaytimeStatsWidgetState {
    /** Nothing cached yet and a fetch is in flight. */
    data object Loading : PlaytimeStatsWidgetState()

    /** The current game has no RetroAchievements match, or RA has no playtime-stats data
     * for it at all (too few players have reached any milestone yet, or none logged). A
     * [Loaded] state whose individual [GamePlaytimeStats] fields are null is a distinct,
     * narrower case - see [GamePlaytimeStats]'s kdoc - rendered per-line at display time,
     * not folded into this. */
    data object Unavailable : PlaytimeStatsWidgetState()

    data class Loaded(
        val stats: GamePlaytimeStats,
        val isRefreshing: Boolean,
    ) : PlaytimeStatsWidgetState()
}
