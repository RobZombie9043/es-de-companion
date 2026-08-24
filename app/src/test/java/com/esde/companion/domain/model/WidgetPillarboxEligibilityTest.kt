package com.esde.companion.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPillarboxEligibilityTest {
    @Test
    fun `Video supports pillarbox only when scaleMode is Fit (Contain)`() {
        assertTrue(WidgetType.Video(scaleMode = ScaleMode.Fit).supportsPillarbox)
        assertFalse(WidgetType.Video(scaleMode = ScaleMode.Fill).supportsPillarbox)
    }

    @Test
    fun `non-Video widget types never support pillarbox`() {
        assertFalse(WidgetType.SystemImage(ScaleMode.Fit).supportsPillarbox)
        assertFalse(WidgetType.GameMedia(MediaType.FanArt, ScaleMode.Fit).supportsPillarbox)
        assertFalse(WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 1f).supportsPillarbox)
    }
}
