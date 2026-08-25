package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetCornerRadiusEligibilityTest {
    // --- supportsCornerRadius: transparent cutout content is excluded ---------------------

    @Test
    fun `SystemLogo never supports corner radius`() {
        assertFalse(WidgetType.SystemLogo(ScaleMode.Fit).supportsCornerRadius)
    }

    @Test
    fun `SystemMedia and GameMedia support corner radius for every MediaType except Marquees`() {
        for (mediaType in MediaType.entries) {
            val expected = mediaType != MediaType.Marquees
            assertEquals(
                "SystemMedia($mediaType)",
                expected,
                WidgetType.SystemMedia(mediaType, ScaleMode.Fill).supportsCornerRadius,
            )
            assertEquals(
                "GameMedia($mediaType)",
                expected,
                WidgetType.GameMedia(mediaType, ScaleMode.Fill).supportsCornerRadius,
            )
        }
    }

    @Test
    fun `every non-logo-style, non-media variant supports corner radius`() {
        assertTrue(WidgetType.SystemImage(ScaleMode.Fill).supportsCornerRadius)
        assertTrue(WidgetType.CustomImage("path", ScaleMode.Fill).supportsCornerRadius)
        assertTrue(WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 1f).supportsCornerRadius)
        assertTrue(WidgetType.GameDescription().supportsCornerRadius)
        assertTrue(WidgetType.Rating().supportsCornerRadius)
        assertTrue(WidgetType.Video().supportsCornerRadius)
    }

    // --- cornerRadius: the stored value, with defense-in-depth against stale data ---------

    @Test
    fun `cornerRadius reads the stored value for a supported variant`() {
        val widget = WidgetType.SystemImage(ScaleMode.Fill, cornerRadius = CornerRadius.Large)
        assertEquals(CornerRadius.Large, widget.cornerRadius)
    }

    @Test
    fun `cornerRadius is always None for SystemLogo`() {
        assertEquals(CornerRadius.None, WidgetType.SystemLogo(ScaleMode.Fit).cornerRadius)
    }

    @Test
    fun `cornerRadius ignores a stale stored value once the media type becomes Marquees`() {
        // Typed as the WidgetType base, not the concrete GameMedia subtype - a smart-cast
        // (or statically subtype-typed) reference would resolve .cornerRadius to the raw
        // member field instead of this extension, same as WidgetCanvas.kt's actual render
        // call site (widgetType: WidgetType, never narrowed) that this test exercises.
        val widget: WidgetType =
            WidgetType.GameMedia(MediaType.Marquees, ScaleMode.Fit, cornerRadius = CornerRadius.Medium)
        assertEquals(CornerRadius.None, widget.cornerRadius)
    }
}
