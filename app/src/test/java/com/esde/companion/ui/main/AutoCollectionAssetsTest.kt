package com.esde.companion.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoCollectionAssetsTest {

    @Test
    fun `maps each known auto collection to its asset name`() {
        assertEquals("auto-allgames", systemLogoAssetName("all"))
        assertEquals("auto-favorites", systemLogoAssetName("favorites"))
        assertEquals("auto-lastplayed", systemLogoAssetName("recent"))
    }

    @Test
    fun `real systems pass through unchanged`() {
        assertEquals("dreamcast", systemLogoAssetName("dreamcast"))
        assertEquals("gba", systemLogoAssetName("gba"))
    }
}