package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameGuideFormatTest {
    @Test
    fun `txt maps to PlainText`() {
        assertEquals(GameGuideFormat.PlainText, GameGuideFormat.forFileExtension("txt"))
    }

    @Test
    fun `htm and html map to Html`() {
        assertEquals(GameGuideFormat.Html, GameGuideFormat.forFileExtension("htm"))
        assertEquals(GameGuideFormat.Html, GameGuideFormat.forFileExtension("html"))
    }

    @Test
    fun `pdf maps to Pdf`() {
        assertEquals(GameGuideFormat.Pdf, GameGuideFormat.forFileExtension("pdf"))
    }

    @Test
    fun `image extensions map to Image`() {
        listOf("png", "jpg", "jpeg", "webp", "gif", "bmp").forEach { extension ->
            assertEquals(GameGuideFormat.Image, GameGuideFormat.forFileExtension(extension))
        }
    }

    @Test
    fun `extension matching is case-insensitive`() {
        assertEquals(GameGuideFormat.Pdf, GameGuideFormat.forFileExtension("PDF"))
    }

    @Test
    fun `an unrecognized extension maps to null`() {
        assertNull(GameGuideFormat.forFileExtension("exe"))
        assertNull(GameGuideFormat.forFileExtension(""))
    }
}
