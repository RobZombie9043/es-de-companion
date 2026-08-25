package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserGameProgressTest {
    @Test
    fun `a hardcore mastered award maps to Mastered`() {
        assertEquals(ProgressStatus.Mastered, progressStatusFor("mastered", numAwarded = 10))
    }

    @Test
    fun `beaten-hardcore and beaten-softcore both map to Beaten`() {
        assertEquals(ProgressStatus.Beaten, progressStatusFor("beaten-hardcore", numAwarded = 10))
        assertEquals(ProgressStatus.Beaten, progressStatusFor("beaten-softcore", numAwarded = 10))
    }

    @Test
    fun `a softcore completed award maps to Beaten, not Mastered`() {
        assertEquals(ProgressStatus.Beaten, progressStatusFor("completed", numAwarded = 10))
    }

    @Test
    fun `a null award kind with no achievements awarded maps to None`() {
        assertEquals(ProgressStatus.None, progressStatusFor(null, numAwarded = 0))
    }

    @Test
    fun `a null award kind with some achievements awarded maps to Some`() {
        assertEquals(ProgressStatus.Some, progressStatusFor(null, numAwarded = 3))
    }

    @Test
    fun `an unrecognized award kind never upgrades to Beaten or Mastered`() {
        assertEquals(ProgressStatus.Some, progressStatusFor("some-future-award-kind", numAwarded = 3))
        assertEquals(ProgressStatus.None, progressStatusFor("some-future-award-kind", numAwarded = 0))
    }

    @Test
    fun `award kind matching is case-insensitive`() {
        assertEquals(ProgressStatus.Mastered, progressStatusFor("MASTERED", numAwarded = 10))
    }
}
