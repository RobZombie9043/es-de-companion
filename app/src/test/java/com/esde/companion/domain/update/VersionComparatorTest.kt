package com.esde.companion.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test
    fun `equal versions compare equal`() {
        assertEquals(0, VersionComparator.compare("0.7.0", "0.7.0"))
    }

    @Test
    fun `equal versions with the same suffix compare equal`() {
        assertEquals(0, VersionComparator.compare("0.7.0-RC1", "0.7.0-RC1"))
    }

    @Test
    fun `major version difference dominates regardless of suffix`() {
        assertTrue(VersionComparator.compare("1.0.0", "0.9.9-anything") > 0)
    }

    @Test
    fun `minor version difference dominates regardless of suffix`() {
        assertTrue(VersionComparator.compare("0.6.0-pre-release", "0.7.0-alpha") < 0)
    }

    @Test
    fun `patch version difference dominates regardless of suffix`() {
        assertTrue(VersionComparator.compare("0.7.1", "0.7.0-RC1") > 0)
    }

    @Test
    fun `no-suffix build is newer than a suffixed build at the same numeric version`() {
        assertTrue(VersionComparator.compare("0.7.0", "0.7.0-RC1") > 0)
        assertTrue(VersionComparator.compare("0.7.0-RC1", "0.7.0") < 0)
    }

    @Test
    fun `two suffixed builds at the same numeric version fall back to a string compare`() {
        assertTrue(VersionComparator.compare("0.7.0-alpha", "0.7.0-beta") < 0)
        assertTrue(VersionComparator.compare("0.7.0-beta", "0.7.0-alpha") > 0)
    }

    @Test
    fun `suffix string fallback is case-insensitive`() {
        assertEquals(0, VersionComparator.compare("0.7.0-RC1", "0.7.0-rc1"))
    }

    @Test
    fun `leading v is stripped so tag names compare the same as bare version names`() {
        assertEquals(0, VersionComparator.compare("v0.7.0", "0.7.0"))
        assertEquals(0, VersionComparator.compare("V0.7.0-alpha", "0.7.0-alpha"))
    }

    @Test
    fun `malformed input does not throw`() {
        VersionComparator.compare("", "0.7.0")
        VersionComparator.compare("garbage", "0.7.0")
        VersionComparator.compare("1", "0.7.0")
        VersionComparator.compare("1.2", "0.7.0")
    }

    @Test
    fun `isNewer is true when the candidate is a higher version`() {
        assertTrue(VersionComparator.isNewer("0.8.0", "0.7.0-RC1"))
    }

    @Test
    fun `isNewer is false when the candidate is the same or an older version`() {
        assertFalse(VersionComparator.isNewer("0.7.0-RC1", "0.7.0-RC1"))
        assertFalse(VersionComparator.isNewer("0.6.0", "0.7.0-RC1"))
    }
}
