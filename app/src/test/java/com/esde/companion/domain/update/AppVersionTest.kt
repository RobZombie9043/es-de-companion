package com.esde.companion.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppVersionTest {
    @Test
    fun `parses a plain major-minor-patch version`() {
        val version = AppVersion.parse("0.7.0")

        assertEquals(0, version.major)
        assertEquals(7, version.minor)
        assertEquals(0, version.patch)
        assertNull(version.suffix)
    }

    @Test
    fun `strips a leading v or V and extracts a suffix`() {
        val lower = AppVersion.parse("v0.7.0-alpha")
        val upper = AppVersion.parse("V0.7.0-alpha")

        assertEquals(0, lower.major)
        assertEquals(7, lower.minor)
        assertEquals(0, lower.patch)
        assertEquals("alpha", lower.suffix)
        assertEquals("alpha", upper.suffix)
    }

    @Test
    fun `partial numeric prefixes default missing components to 0`() {
        val majorOnly = AppVersion.parse("1")
        val majorMinor = AppVersion.parse("1.2")

        assertEquals(1, majorOnly.major)
        assertEquals(0, majorOnly.minor)
        assertEquals(0, majorOnly.patch)

        assertEquals(1, majorMinor.major)
        assertEquals(2, majorMinor.minor)
        assertEquals(0, majorMinor.patch)
    }

    @Test
    fun `a fully non-numeric string degrades to an all-zero version with the whole string as suffix`() {
        val version = AppVersion.parse("garbage")

        assertEquals(0, version.major)
        assertEquals(0, version.minor)
        assertEquals(0, version.patch)
        assertEquals("garbage", version.suffix)
    }

    @Test
    fun `an empty string has a null suffix, not an empty one`() {
        val version = AppVersion.parse("")

        assertEquals(0, version.major)
        assertNull(version.suffix)
    }

    @Test
    fun `dash plus and dot separators are all stripped from the leading edge of the suffix`() {
        assertEquals("RC1", AppVersion.parse("0.7.0-RC1").suffix)
        assertEquals("build5", AppVersion.parse("0.7.0+build5").suffix)
        assertEquals("RC1", AppVersion.parse("0.7.0.RC1").suffix)
    }

    @Test
    fun `raw preserves the original untrimmed input regardless of other fields`() {
        assertEquals(" v0.7.0-alpha ", AppVersion.parse(" v0.7.0-alpha ").raw)
        assertEquals("garbage", AppVersion.parse("garbage").raw)
        assertEquals("", AppVersion.parse("").raw)
    }
}
