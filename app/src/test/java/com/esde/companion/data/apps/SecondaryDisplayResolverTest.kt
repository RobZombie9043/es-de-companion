package com.esde.companion.data.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecondaryDisplayResolverTest {
    @Test
    fun `returns the id of the first display that is not the current one`() {
        assertEquals(2, SecondaryDisplayResolver.pickSecondary(listOf(1, 2, 3), currentId = 1))
    }

    @Test
    fun `skips the current display even when it is not first`() {
        assertEquals(2, SecondaryDisplayResolver.pickSecondary(listOf(2, 1, 3), currentId = 1))
    }

    @Test
    fun `returns null when the only display is the current one`() {
        assertNull(SecondaryDisplayResolver.pickSecondary(listOf(1), currentId = 1))
    }

    @Test
    fun `returns null for an empty display list`() {
        assertNull(SecondaryDisplayResolver.pickSecondary(emptyList(), currentId = 1))
    }
}
