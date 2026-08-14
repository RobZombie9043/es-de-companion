package com.esde.companion.domain.model

/** Same sort options RetroAchievements' own website offers on a game's achievement list. */
enum class AchievementSortOrder {
    UnlockedFirst,
    DisplayOrderFirst,
    DisplayOrderLast,
    WonByMost,
    WonByLeast,
    PointsMost,
    PointsLeast,
    TitleAToZ,
    TitleZToA,
    TypeAscending,
    TypeDescending,
}

/** Pure sort - see [AchievementSortOrder]. Ties within [UnlockedFirst] fall back to display order. */
fun List<AchievementItem>.sortedByAchievementOrder(order: AchievementSortOrder): List<AchievementItem> =
    when (order) {
        AchievementSortOrder.UnlockedFirst ->
            sortedWith(compareByDescending<AchievementItem> { it.unlocked }.thenBy { it.displayOrder })
        AchievementSortOrder.DisplayOrderFirst -> sortedBy { it.displayOrder }
        AchievementSortOrder.DisplayOrderLast -> sortedByDescending { it.displayOrder }
        AchievementSortOrder.WonByMost -> sortedByDescending { it.numAwarded }
        AchievementSortOrder.WonByLeast -> sortedBy { it.numAwarded }
        AchievementSortOrder.PointsMost -> sortedByDescending { it.points }
        AchievementSortOrder.PointsLeast -> sortedBy { it.points }
        AchievementSortOrder.TitleAToZ -> sortedBy { it.title.lowercase() }
        AchievementSortOrder.TitleZToA -> sortedByDescending { it.title.lowercase() }
        AchievementSortOrder.TypeAscending -> sortedBy { it.type?.name ?: "" }
        AchievementSortOrder.TypeDescending -> sortedByDescending { it.type?.name ?: "" }
    }
