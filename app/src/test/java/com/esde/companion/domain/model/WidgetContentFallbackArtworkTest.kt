package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetContentFallbackArtworkTest {
    @Test
    fun `FanArt offers Screenshots then None as fallback options`() {
        assertEquals(listOf(MediaType.Screenshots, null), MediaType.FanArt.fallbackMediaTypeOptions())
    }

    @Test
    fun `Screenshots offers FanArt then None as fallback options`() {
        assertEquals(listOf(MediaType.FanArt, null), MediaType.Screenshots.fallbackMediaTypeOptions())
    }

    @Test
    fun `ThreeDBoxes offers Covers then None as fallback options`() {
        assertEquals(listOf(MediaType.Covers, null), MediaType.ThreeDBoxes.fallbackMediaTypeOptions())
    }

    @Test
    fun `every other MediaType has no fallback options`() {
        val typesWithFallback = setOf(MediaType.FanArt, MediaType.Screenshots, MediaType.ThreeDBoxes)
        MediaType.entries.filterNot { it in typesWithFallback }.forEach { mediaType ->
            assertTrue("$mediaType should have no fallback options", mediaType.fallbackMediaTypeOptions().isEmpty())
        }
    }

    @Test
    fun `default fallback for FanArt is Screenshots`() {
        assertEquals(MediaType.Screenshots, MediaType.FanArt.defaultFallbackMediaType())
    }

    @Test
    fun `default fallback for Screenshots is FanArt`() {
        assertEquals(MediaType.FanArt, MediaType.Screenshots.defaultFallbackMediaType())
    }

    @Test
    fun `default fallback for ThreeDBoxes is Covers`() {
        assertEquals(MediaType.Covers, MediaType.ThreeDBoxes.defaultFallbackMediaType())
    }

    @Test
    fun `default fallback for an unrelated MediaType is null`() {
        assertNull(MediaType.Marquees.defaultFallbackMediaType())
    }

    @Test
    fun `newly constructed SystemMedia and GameMedia default fallbackMediaType per their own MediaType`() {
        assertEquals(MediaType.Screenshots, WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill).fallbackMediaType)
        assertEquals(MediaType.FanArt, WidgetType.GameMedia(MediaType.Screenshots, ScaleMode.Fill).fallbackMediaType)
        assertEquals(MediaType.Covers, WidgetType.GameMedia(MediaType.ThreeDBoxes, ScaleMode.Fit).fallbackMediaType)
        assertNull(WidgetType.GameMedia(MediaType.Marquees, ScaleMode.Fit).fallbackMediaType)
    }

    @Test
    fun `supportsFallbackArtwork is true only for SystemMedia and GameMedia with a fallback-eligible MediaType`() {
        assertTrue(WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill).supportsFallbackArtwork)
        assertTrue(WidgetType.GameMedia(MediaType.ThreeDBoxes, ScaleMode.Fit).supportsFallbackArtwork)
        assertFalse(WidgetType.GameMedia(MediaType.Marquees, ScaleMode.Fit).supportsFallbackArtwork)
        assertFalse(WidgetType.SystemLogo(ScaleMode.Fit).supportsFallbackArtwork)
        assertFalse(WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 1f).supportsFallbackArtwork)
    }

    @Test
    fun `fallbackMediaType extension reads through to non-default variants and is null for unrelated types`() {
        val widget = WidgetType.GameMedia(MediaType.FanArt, ScaleMode.Fill, fallbackMediaType = null)
        assertNull(widget.fallbackMediaType)
        assertNull(WidgetType.SystemLogo(ScaleMode.Fit).fallbackMediaType)
    }
}
