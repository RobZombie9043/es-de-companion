package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementFilterOptionTest {
    private fun achievement(
        id: Long,
        unlocked: Boolean,
        type: AchievementType? = null,
    ) = AchievementItem(
        id = id,
        title = "title-$id",
        description = "",
        points = 0,
        badgeUrl = null,
        unlocked = unlocked,
        unlockedAt = null,
        type = type,
    )

    @Test
    fun `an empty filter set returns every achievement unchanged`() {
        val achievements = listOf(achievement(1, unlocked = true), achievement(2, unlocked = false))

        assertEquals(achievements, achievements.filteredByAchievementFilters(emptySet()))
    }

    @Test
    fun `MissableOnly keeps only achievements typed Missable`() {
        val missable = achievement(1, unlocked = false, type = AchievementType.Missable)
        val progression = achievement(2, unlocked = false, type = AchievementType.Progression)
        val achievements = listOf(missable, progression, achievement(3, unlocked = true))

        val result = achievements.filteredByAchievementFilters(setOf(AchievementFilterOption.MissableOnly))

        assertEquals(listOf(missable), result)
    }

    @Test
    fun `LockedOnly keeps only achievements that are not yet unlocked`() {
        val locked = achievement(1, unlocked = false)
        val achievements = listOf(locked, achievement(2, unlocked = true))

        val result = achievements.filteredByAchievementFilters(setOf(AchievementFilterOption.LockedOnly))

        assertEquals(listOf(locked), result)
    }

    @Test
    fun `MissableOnly and LockedOnly together combine with AND, not OR`() {
        val missableAndLocked = achievement(1, unlocked = false, type = AchievementType.Missable)
        val missableButUnlocked = achievement(2, unlocked = true, type = AchievementType.Missable)
        val lockedButNotMissable = achievement(3, unlocked = false, type = AchievementType.Progression)
        val achievements = listOf(missableAndLocked, missableButUnlocked, lockedButNotMissable)

        val filters = setOf(AchievementFilterOption.MissableOnly, AchievementFilterOption.LockedOnly)
        val result = achievements.filteredByAchievementFilters(filters)

        assertEquals(listOf(missableAndLocked), result)
    }
}
