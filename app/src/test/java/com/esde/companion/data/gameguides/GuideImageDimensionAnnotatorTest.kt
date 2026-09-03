package com.esde.companion.data.gameguides

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class GuideImageDimensionAnnotatorTest {
    @Test
    fun `injects width and height when dimensions resolve`() {
        val result =
            annotateImageDimensions(
                bodyHtml = """<img src="images/p0_0.jpg">""",
                mediaDirectoryPath = "/guides/g1",
            ) { 1080 to 720 }

        assertEquals("""<img width="1080" height="720" src="images/p0_0.jpg">""", result)
    }

    @Test
    fun `leaves a tag that already has width untouched`() {
        val body = """<img width="640" src="images/p0_0.jpg">"""

        val result = annotateImageDimensions(body, "/guides/g1") { 1080 to 720 }

        assertEquals(body, result)
    }

    @Test
    fun `leaves a tag untouched when dimensions can't be resolved`() {
        val body = """<img src="images/missing.jpg">"""

        val result = annotateImageDimensions(body, "/guides/g1") { null }

        assertEquals(body, result)
    }

    @Test
    fun `resolves multiple images independently`() {
        val body = """<p>Text</p><img src="images/a.jpg"><p>More</p><img src="images/b.jpg">"""

        val result =
            annotateImageDimensions(body, "/guides/g1") { file ->
                when (file.name) {
                    "a.jpg" -> 100 to 200
                    "b.jpg" -> 300 to 400
                    else -> null
                }
            }

        val expected =
            """<p>Text</p><img width="100" height="200" src="images/a.jpg">""" +
                """<p>More</p><img width="300" height="400" src="images/b.jpg">"""
        assertEquals(expected, result)
    }

    @Test
    fun `passes the resolved local file path to the dimension reader`() {
        var receivedPath: String? = null

        annotateImageDimensions("""<img src="images/p0_0.jpg">""", "/guides/g1") { file ->
            receivedPath = file.path
            null
        }

        assertEquals(File("/guides/g1", "images/p0_0.jpg").path, receivedPath)
    }

    @Test
    fun `leaves a body with no images untouched`() {
        val body = "<p>Just text, no images.</p>"

        val result = annotateImageDimensions(body, "/guides/g1") { 1080 to 720 }

        assertEquals(body, result)
    }
}
