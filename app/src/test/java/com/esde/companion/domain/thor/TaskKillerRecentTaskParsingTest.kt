package com.esde.companion.domain.thor

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskKillerRecentTaskParsingTest {
    @Test
    fun `no matching line yields no task ids`() {
        val output =
            """
            * Recent #0: Task{f5c6b21 #101 type=standard A=10123:com.other.app}
            * Recent #1: Task{a1b2c3d #102 type=standard A=10050:com.android.settings}
            """.trimIndent()
        assertEquals(emptyList<Int>(), parseRecentTaskIds(output, "org.libretro.retroarch"))
    }

    @Test
    fun `single matching line yields the task id, not the Recent list position`() {
        val output =
            """
            * Recent #0: Task{f5c6b21 #101 type=standard A=10123:com.other.app}
            * Recent #1: Task{a1b2c3d #183 type=standard A=10200:org.libretro.retroarch}
            """.trimIndent()
        assertEquals(listOf(183), parseRecentTaskIds(output, "org.libretro.retroarch"))
    }

    @Test
    fun `every task belonging to the package is returned, not just the first`() {
        val output =
            """
            * Recent #0: Task{f5c6b21 #101 type=standard A=10123:org.libretro.retroarch}
            * Recent #1: Task{a1b2c3d #102 type=standard A=10050:com.android.settings}
            * Recent #2: Task{c9d8e7f #109 type=standard A=10123:org.libretro.retroarch}
            """.trimIndent()
        assertEquals(listOf(101, 109), parseRecentTaskIds(output, "org.libretro.retroarch"))
    }

    @Test
    fun `a package name that is a prefix of another package does not false-match`() {
        val output =
            """
            * Recent #0: Task{f5c6b21 #101 type=standard A=10123:org.libretro.retroarch.beta}
            """.trimIndent()
        assertEquals(emptyList<Int>(), parseRecentTaskIds(output, "org.libretro.retroarch"))
    }

    @Test
    fun `a task with no declared taskAffinity uses I= instead of A=, and still matches`() {
        val output =
            """
            * Recent #2: Task{70c0c24 #17157 type=standard I=dev.imranr.obtainium/.MainActivity}
            """.trimIndent()
        assertEquals(listOf(17157), parseRecentTaskIds(output, "dev.imranr.obtainium"))
    }
}
