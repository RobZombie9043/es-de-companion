package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPanZoomEligibilityTest {
    private val panZoomEligibleMediaTypes = setOf(MediaType.FanArt, MediaType.Screenshots, MediaType.TitleScreens)

    // --- supportsPanZoom: non-media variants ---------------------------------------------

    @Test
    fun `SystemImage supports pan-zoom only when scaleMode is Fill`() {
        assertTrue(WidgetType.SystemImage(ScaleMode.Fill).supportsPanZoom)
        assertFalse(WidgetType.SystemImage(ScaleMode.Fit).supportsPanZoom)
    }

    @Test
    fun `CustomImage supports pan-zoom only when scaleMode is Fill`() {
        assertTrue(WidgetType.CustomImage("path", ScaleMode.Fill).supportsPanZoom)
        assertFalse(WidgetType.CustomImage("path", ScaleMode.Fit).supportsPanZoom)
    }

    @Test
    fun `SystemLogo, ColorBackground, and GameDescription never support pan-zoom`() {
        assertFalse(WidgetType.SystemLogo(ScaleMode.Fill).supportsPanZoom)
        assertFalse(WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 1f).supportsPanZoom)
        assertFalse(WidgetType.GameDescription().supportsPanZoom)
    }

    // --- supportsPanZoom: SystemMedia/GameMedia across every MediaType, both ScaleModes --

    @Test
    fun `SystemMedia supports pan-zoom only for FanArt, Screenshots, TitleScreens with Fill scaleMode`() {
        for (mediaType in MediaType.entries) {
            val fill = WidgetType.SystemMedia(mediaType, ScaleMode.Fill)
            val fit = WidgetType.SystemMedia(mediaType, ScaleMode.Fit)
            assertEquals("SystemMedia($mediaType, Fill)", mediaType in panZoomEligibleMediaTypes, fill.supportsPanZoom)
            assertFalse("SystemMedia($mediaType, Fit)", fit.supportsPanZoom)
        }
    }

    @Test
    fun `GameMedia supports pan-zoom only for FanArt, Screenshots, TitleScreens with Fill scaleMode`() {
        for (mediaType in MediaType.entries) {
            val fill = WidgetType.GameMedia(mediaType, ScaleMode.Fill)
            val fit = WidgetType.GameMedia(mediaType, ScaleMode.Fit)
            assertEquals("GameMedia($mediaType, Fill)", mediaType in panZoomEligibleMediaTypes, fill.supportsPanZoom)
            assertFalse("GameMedia($mediaType, Fit)", fit.supportsPanZoom)
        }
    }

    @Test
    fun `Marquees and instant-only media types are never pan-zoom eligible`() {
        val ineligible =
            setOf(
                MediaType.Marquees,
                MediaType.Covers,
                MediaType.ThreeDBoxes,
                MediaType.MixImages,
                MediaType.BackCovers,
                MediaType.PhysicalMedia,
                MediaType.Videos,
                MediaType.Manuals,
                MediaType.Custom,
            )
        for (mediaType in ineligible) {
            assertFalse("GameMedia($mediaType)", WidgetType.GameMedia(mediaType, ScaleMode.Fill).supportsPanZoom)
        }
    }

    // --- panZoomActive: eligibility AND the stored flag ----------------------------------

    @Test
    fun `panZoomActive is false when eligible but the stored flag is off`() {
        val widget = WidgetType.SystemImage(ScaleMode.Fill, panZoomEnabled = false)
        assertFalse(widget.panZoomActive)
    }

    @Test
    fun `panZoomActive is true when eligible and the stored flag is on`() {
        val widget = WidgetType.SystemImage(ScaleMode.Fill, panZoomEnabled = true)
        assertTrue(widget.panZoomActive)
    }

    @Test
    fun `panZoomActive ignores a stale enabled flag once scaleMode makes the widget ineligible`() {
        val widget = WidgetType.SystemImage(ScaleMode.Fit, panZoomEnabled = true)
        assertFalse(widget.panZoomActive)
    }

    @Test
    fun `panZoomActive ignores a stale enabled flag for an ineligible media type`() {
        val widget = WidgetType.GameMedia(MediaType.Covers, ScaleMode.Fill, panZoomEnabled = true)
        assertFalse(widget.panZoomActive)
    }
}
