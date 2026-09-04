package com.esde.companion.domain.model

/**
 * What the `WidgetType.AchievementSummary` widget should currently show - mirrors
 * `RetroAchievementsFetchState`'s Loading/Loaded/NotFound shape for the full achievement
 * screen, at the smaller scale this widget needs. `null` from the resolving lookup (not a
 * variant of this sealed class) means "not eligible at all" - no widget on the canvas, no
 * current game, or signed out - and resolves to `WidgetContent.Empty` (hidden entirely),
 * see `WidgetContentResolver`'s AchievementSummary branch.
 */
sealed class AchievementSummaryWidgetState {
    /** Nothing cached yet and a fetch is in flight - see `WidgetsViewModel.resolveAchievementSummary`. */
    data object Loading : AchievementSummaryWidgetState()

    /** The current game has no RetroAchievements match, or its matched entry has zero achievements. */
    data object Unavailable : AchievementSummaryWidgetState()

    data class Loaded(
        val unlockedCount: Int,
        val totalCount: Int,
        val earnedPoints: Int,
        val totalPoints: Int,
        val completionPercent: Float,
        val isRefreshing: Boolean,
    ) : AchievementSummaryWidgetState()
}
