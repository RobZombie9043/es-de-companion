package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetImageTransitionEligibilityTest {
    // --- supportsImageTransition -----------------------------------------------------------

    @Test
    fun `SystemImage and CustomImage always support the Image Transitions picker`() {
        assertTrue(WidgetType.SystemImage(ScaleMode.Fill).supportsImageTransition)
        assertTrue(WidgetType.SystemImage(ScaleMode.Fit).supportsImageTransition)
        assertTrue(WidgetType.CustomImage("path", ScaleMode.Fill).supportsImageTransition)
        assertTrue(WidgetType.CustomImage("path", ScaleMode.Fit).supportsImageTransition)
    }

    @Test
    fun `SystemMedia supports the picker for every media type except Marquees`() {
        for (mediaType in MediaType.entries) {
            val widget = WidgetType.SystemMedia(mediaType, ScaleMode.Fill)
            assertEquals(mediaType.toString(), mediaType != MediaType.Marquees, widget.supportsImageTransition)
        }
    }

    @Test
    fun `GameMedia supports the picker except for Marquees and box-art-style media`() {
        val excluded =
            setOf(
                MediaType.Marquees,
                MediaType.Covers,
                MediaType.ThreeDBoxes,
                MediaType.MixImages,
                MediaType.BackCovers,
                MediaType.PhysicalMedia,
            )
        for (mediaType in MediaType.entries) {
            val widget = WidgetType.GameMedia(mediaType, ScaleMode.Fill)
            assertEquals(mediaType.toString(), mediaType !in excluded, widget.supportsImageTransition)
        }
    }

    @Test
    fun `SystemLogo, ColorBackground, GameDescription, and Rating never support the picker`() {
        assertFalse(WidgetType.SystemLogo(ScaleMode.Fill).supportsImageTransition)
        assertFalse(WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 1f).supportsImageTransition)
        assertFalse(WidgetType.GameDescription().supportsImageTransition)
        assertFalse(WidgetType.Rating().supportsImageTransition)
    }

    // --- allowsFadeTransition ----------------------------------------------------------------

    @Test
    fun `allowsFadeTransition is true only when scaleMode is Fill`() {
        assertTrue(WidgetType.SystemImage(ScaleMode.Fill).allowsFadeTransition)
        assertFalse(WidgetType.SystemImage(ScaleMode.Fit).allowsFadeTransition)
        assertTrue(WidgetType.CustomImage("path", ScaleMode.Fill).allowsFadeTransition)
        assertFalse(WidgetType.CustomImage("path", ScaleMode.Fit).allowsFadeTransition)
        assertTrue(WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill).allowsFadeTransition)
        assertFalse(WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fit).allowsFadeTransition)
        assertTrue(WidgetType.GameMedia(MediaType.Screenshots, ScaleMode.Fill).allowsFadeTransition)
        assertFalse(WidgetType.GameMedia(MediaType.Screenshots, ScaleMode.Fit).allowsFadeTransition)
    }

    // --- imageTransitionActive ---------------------------------------------------------------

    @Test
    fun `imageTransitionActive is the stored mode when Fill-scaled and eligible`() {
        val widget = WidgetType.SystemImage(ScaleMode.Fill, imageTransitionMode = ImageTransitionMode.Fade)
        assertEquals(ImageTransitionMode.Fade, widget.imageTransitionActive)
    }

    @Test
    fun `imageTransitionActive forces None when scaleMode is Fit even if Fade is stored`() {
        val widget = WidgetType.SystemImage(ScaleMode.Fit, imageTransitionMode = ImageTransitionMode.Fade)
        assertEquals(ImageTransitionMode.None, widget.imageTransitionActive)
    }

    @Test
    fun `imageTransitionActive forces None for box-art GameMedia even when Fill-scaled and Fade stored`() {
        val widget = WidgetType.GameMedia(MediaType.Covers, ScaleMode.Fill, imageTransitionMode = ImageTransitionMode.Fade)
        assertEquals(ImageTransitionMode.None, widget.imageTransitionActive)
    }

    @Test
    fun `imageTransitionActive is the stored mode for eligible GameMedia`() {
        val widget = WidgetType.GameMedia(MediaType.Screenshots, ScaleMode.Fill, imageTransitionMode = ImageTransitionMode.Fade)
        assertEquals(ImageTransitionMode.Fade, widget.imageTransitionActive)
    }

    // --- imageTransitionMode / logoTransitionMode / glintEnabled read-through ---------------

    @Test
    fun `imageTransitionMode reads through for every opaque image-backed variant`() {
        assertEquals(
            ImageTransitionMode.Fade,
            WidgetType.SystemImage(ScaleMode.Fill, imageTransitionMode = ImageTransitionMode.Fade).imageTransitionMode,
        )
        assertEquals(
            ImageTransitionMode.Fade,
            WidgetType.CustomImage("p", ScaleMode.Fill, imageTransitionMode = ImageTransitionMode.Fade).imageTransitionMode,
        )
        assertEquals(
            ImageTransitionMode.Fade,
            WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill, imageTransitionMode = ImageTransitionMode.Fade).imageTransitionMode,
        )
        assertEquals(
            ImageTransitionMode.Fade,
            WidgetType.GameMedia(MediaType.Screenshots, ScaleMode.Fill, imageTransitionMode = ImageTransitionMode.Fade).imageTransitionMode,
        )
        assertEquals(ImageTransitionMode.None, WidgetType.SystemLogo(ScaleMode.Fill).imageTransitionMode)
        assertEquals(ImageTransitionMode.None, WidgetType.ColorBackground(0xFF000000, 1f).imageTransitionMode)
    }

    @Test
    fun `logoTransitionMode and glintEnabled read through for every logo-style variant`() {
        val logo = WidgetType.SystemLogo(ScaleMode.Fill, logoTransitionMode = LogoTransitionMode.Slide, glintEnabled = true)
        assertEquals(LogoTransitionMode.Slide, logo.logoTransitionMode)
        assertTrue(logo.glintEnabled)

        val marquee =
            WidgetType.GameMedia(
                MediaType.Marquees,
                ScaleMode.Fit,
                logoTransitionMode = LogoTransitionMode.Scale,
                glintEnabled = true,
            )
        assertEquals(LogoTransitionMode.Scale, marquee.logoTransitionMode)
        assertTrue(marquee.glintEnabled)

        assertEquals(LogoTransitionMode.None, WidgetType.SystemImage(ScaleMode.Fill).logoTransitionMode)
        assertFalse(WidgetType.SystemImage(ScaleMode.Fill).glintEnabled)
    }
}
