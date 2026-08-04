package com.esde.companion.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsdeEventAnchorTest {

    @Test
    fun `screensaver events are never startup anchors`() {
        assertFalse(EsdeEvent.ScreensaverStart("manual").isStartupAnchor())
        assertFalse(EsdeEvent.ScreensaverGameSelect("p", "n", "s", "sf").isStartupAnchor())
        assertFalse(EsdeEvent.ScreensaverEnd("cancel").isStartupAnchor())
    }

    @Test
    fun `system and game events are startup anchors`() {
        assertTrue(EsdeEvent.SystemSelect("s", "sf", "p").isStartupAnchor())
        assertTrue(EsdeEvent.GameSelect("p", "n", "s", "sf").isStartupAnchor())
        assertTrue(EsdeEvent.GameStart("p", "n", "s", "sf").isStartupAnchor())
        assertTrue(EsdeEvent.GameEnd("p", "n", "s", "sf").isStartupAnchor())
    }

    @Test
    fun `quit is a startup anchor`() {
        assertTrue(EsdeEvent.Quit.isStartupAnchor())
    }

    @Test
    fun `reload is a startup anchor`() {
        assertTrue(EsdeEvent.Reload.isStartupAnchor())
    }
}