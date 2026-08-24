package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementDisplayFieldTest {
    private val achievement =
        AchievementItem(
            id = 1L,
            title = "title",
            description = "",
            points = 10,
            badgeUrl = null,
            unlocked = true,
            unlockedAt = null,
            numAwarded = 50,
            numAwardedHardcore = 20,
        )

    @Test
    fun `UnlockRate formats as a percentage with two decimal places`() {
        val value = AchievementDisplayField.UnlockRate.valueFor(achievement, totalPlayers = 200)

        assertEquals("25.00%", value)
    }

    @Test
    fun `UnlockRate shows an em dash rather than dividing by zero when totalPlayers is zero`() {
        val value = AchievementDisplayField.UnlockRate.valueFor(achievement, totalPlayers = 0)

        assertEquals("—", value)
    }

    @Test
    fun `Points reports the achievement's own points`() {
        assertEquals("10", AchievementDisplayField.Points.valueFor(achievement, totalPlayers = 200))
    }

    @Test
    fun `TotalUnlocks reports numAwarded`() {
        assertEquals("50", AchievementDisplayField.TotalUnlocks.valueFor(achievement, totalPlayers = 200))
    }

    @Test
    fun `HardcoreUnlocks reports numAwardedHardcore`() {
        assertEquals("20", AchievementDisplayField.HardcoreUnlocks.valueFor(achievement, totalPlayers = 200))
    }
}
