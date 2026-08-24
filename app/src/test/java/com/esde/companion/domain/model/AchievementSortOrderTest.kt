package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementSortOrderTest {
    @Suppress("LongParameterList")
    private fun achievement(
        id: Long,
        title: String,
        points: Int = 0,
        unlocked: Boolean = false,
        displayOrder: Int = 0,
        numAwarded: Int = 0,
        type: AchievementType? = null,
    ) = AchievementItem(
        id = id,
        title = title,
        description = "",
        points = points,
        badgeUrl = null,
        unlocked = unlocked,
        unlockedAt = null,
        displayOrder = displayOrder,
        numAwarded = numAwarded,
        type = type,
    )

    @Test
    fun `UnlockedFirst puts unlocked achievements first, falling back to display order`() {
        val a = achievement(1, "A", unlocked = false, displayOrder = 0)
        val b = achievement(2, "B", unlocked = true, displayOrder = 2)
        val c = achievement(3, "C", unlocked = true, displayOrder = 1)

        val result = listOf(a, b, c).sortedByAchievementOrder(AchievementSortOrder.UnlockedFirst)

        assertEquals(listOf(c, b, a), result)
    }

    @Test
    fun `DisplayOrderFirst and DisplayOrderLast are opposite orderings`() {
        val a = achievement(1, "A", displayOrder = 2)
        val b = achievement(2, "B", displayOrder = 0)
        val c = achievement(3, "C", displayOrder = 1)
        val achievements = listOf(a, b, c)

        assertEquals(listOf(b, c, a), achievements.sortedByAchievementOrder(AchievementSortOrder.DisplayOrderFirst))
        assertEquals(listOf(a, c, b), achievements.sortedByAchievementOrder(AchievementSortOrder.DisplayOrderLast))
    }

    @Test
    fun `WonByMost and WonByLeast sort by numAwarded`() {
        val a = achievement(1, "A", numAwarded = 10)
        val b = achievement(2, "B", numAwarded = 50)
        val c = achievement(3, "C", numAwarded = 1)
        val achievements = listOf(a, b, c)

        assertEquals(listOf(b, a, c), achievements.sortedByAchievementOrder(AchievementSortOrder.WonByMost))
        assertEquals(listOf(c, a, b), achievements.sortedByAchievementOrder(AchievementSortOrder.WonByLeast))
    }

    @Test
    fun `PointsMost and PointsLeast sort by points`() {
        val a = achievement(1, "A", points = 5)
        val b = achievement(2, "B", points = 25)
        val achievements = listOf(a, b)

        assertEquals(listOf(b, a), achievements.sortedByAchievementOrder(AchievementSortOrder.PointsMost))
        assertEquals(listOf(a, b), achievements.sortedByAchievementOrder(AchievementSortOrder.PointsLeast))
    }

    @Test
    fun `TitleAToZ and TitleZToA sort case-insensitively`() {
        val a = achievement(1, "banana")
        val b = achievement(2, "Apple")
        val achievements = listOf(a, b)

        assertEquals(listOf(b, a), achievements.sortedByAchievementOrder(AchievementSortOrder.TitleAToZ))
        assertEquals(listOf(a, b), achievements.sortedByAchievementOrder(AchievementSortOrder.TitleZToA))
    }

    @Test
    fun `TypeAscending and TypeDescending treat a standard (null-type) achievement as sorting before named types`() {
        val standard = achievement(1, "Standard", type = null)
        val missable = achievement(2, "Missable", type = AchievementType.Missable)
        val progression = achievement(3, "Progression", type = AchievementType.Progression)
        val achievements = listOf(progression, missable, standard)

        val ascending = achievements.sortedByAchievementOrder(AchievementSortOrder.TypeAscending)
        assertEquals(standard, ascending.first())

        val descending = achievements.sortedByAchievementOrder(AchievementSortOrder.TypeDescending)
        assertEquals(standard, descending.last())
    }
}
