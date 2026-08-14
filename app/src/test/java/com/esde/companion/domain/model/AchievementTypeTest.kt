package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AchievementTypeTest {
    @Test
    fun `known RA type strings map to their AchievementType, case-insensitively`() {
        assertEquals(AchievementType.Missable, "missable".toAchievementType())
        assertEquals(AchievementType.Missable, "MISSABLE".toAchievementType())
        assertEquals(AchievementType.Progression, "progression".toAchievementType())
        assertEquals(AchievementType.WinCondition, "win_condition".toAchievementType())
    }

    @Test
    fun `a null or blank type is a standard achievement`() {
        assertNull(null.toAchievementType())
        assertNull("".toAchievementType())
    }

    @Test
    fun `an unrecognized type string is treated as standard rather than failing`() {
        assertNull("some-future-type".toAchievementType())
    }
}
