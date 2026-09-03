package com.esde.companion.ui.gameguides

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideHtmlDocumentBuilderTest {
    @Test
    fun `applyLazyImageLoading adds loading lazy to a plain img tag`() {
        val result = applyLazyImageLoading("""<img src="p1_0.jpg">""")

        assertEquals("""<img loading="lazy" src="p1_0.jpg">""", result)
    }

    @Test
    fun `applyLazyImageLoading preserves other attributes`() {
        val result = applyLazyImageLoading("""<img src="p1_0.jpg" alt="Screenshot" width="640">""")

        assertEquals("""<img loading="lazy" src="p1_0.jpg" alt="Screenshot" width="640">""", result)
    }

    @Test
    fun `applyLazyImageLoading replaces a pre-existing loading attribute rather than duplicating it`() {
        val result = applyLazyImageLoading("""<img loading="eager" src="p1_0.jpg">""")

        assertEquals("""<img loading="lazy" src="p1_0.jpg">""", result)
        assertEquals(1, Regex("loading=").findAll(result).count())
    }

    @Test
    fun `applyLazyImageLoading handles multiple images in one body`() {
        val body = """<p>Text</p><img src="a.jpg"><p>More</p><img src="b.jpg">"""
        val expected = """<p>Text</p><img loading="lazy" src="a.jpg"><p>More</p><img loading="lazy" src="b.jpg">"""

        assertEquals(expected, applyLazyImageLoading(body))
    }

    @Test
    fun `applyLazyImageLoading leaves body with no images untouched`() {
        val body = "<p>Just text, no images.</p>"

        assertEquals(body, applyLazyImageLoading(body))
    }

    @Test
    fun `buildGuideHtmlDocument marks images lazy and includes the background loader script`() {
        val document = buildGuideHtmlDocument("""<img src="p1_0.jpg">""", isDarkTheme = false, fontScale = 1f)

        assertTrue(document.contains("""<img loading="lazy" src="p1_0.jpg">"""))
        assertTrue(document.contains("""querySelectorAll('img[loading="lazy"]')"""))
    }

    @Test
    fun `buildGuideHtmlDocument still includes the loader script for an image-free body`() {
        val document = buildGuideHtmlDocument("<p>Just text.</p>", isDarkTheme = true, fontScale = 1f)

        assertFalse(document.contains("<img"))
        assertTrue(document.contains("""querySelectorAll('img[loading="lazy"]')"""))
    }
}
