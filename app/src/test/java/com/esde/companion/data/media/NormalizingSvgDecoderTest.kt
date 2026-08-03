package com.esde.companion.data.media

import java.io.File
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizingSvgDecoderTest {

    @Test
    fun `handles a real-world Illustrator export with a DOCTYPE, switch fallback, and large embedded CDATA`() {
        val xml = File("src/main/assets/system_logos/now-playing.svg").readText(Charsets.UTF_8)

        val result = normalizeSmallViewBoxSvg(xml)

        assertTrue(result != null)
        assertTrue(result!!.startsWith("<?xml"))
        assertTrue(result.contains("<!ENTITY ns_ai"))
        assertTrue(result.contains("""<g transform="scale(6.0)">"""))
        assertTrue(result.contains("<switch>"))
        assertTrue(result.trim().endsWith("</g>\n</svg>"))
    }

    @Test
    fun `scales up a small viewBox and wraps content in a matching scale group`() {
        val xml = """<svg width="100%" height="100%" viewBox="0 0 168 62"><path d="M1 1"/></svg>"""

        val result = normalizeSmallViewBoxSvg(xml)

        assertTrue(result != null)
        val scale = NORMALIZED_LONG_EDGE / 168.0
        assertTrue(result!!.contains("""viewBox="0.0 0.0 ${168 * scale} ${62 * scale}""""))
        assertTrue(result.contains("""<g transform="scale($scale)">"""))
        assertTrue(result.contains("""<path d="M1 1"/>"""))
        assertTrue(result.trim().endsWith("</g>\n</svg>"))
    }

    @Test
    fun `preserves a non-zero viewBox origin under the same scale`() {
        val xml = """<svg viewBox="-64 344.8 482 105.2"><path d="M1 1"/></svg>"""

        val result = normalizeSmallViewBoxSvg(xml)

        val scale = NORMALIZED_LONG_EDGE / 482.0
        assertTrue(result != null)
        assertTrue(result!!.contains("""viewBox="${-64 * scale} ${344.8 * scale} ${482 * scale} ${105.2 * scale}""""))
    }

    @Test
    fun `scales width and height directly when there is no viewBox`() {
        val xml = """<svg width="150" height="60" xmlns="http://www.w3.org/2000/svg"><path d="M1 1"/></svg>"""

        val result = normalizeSmallViewBoxSvg(xml)

        val scale = NORMALIZED_LONG_EDGE / 150.0
        assertTrue(result != null)
        assertTrue(result!!.contains("""width="${150 * scale}""""))
        assertTrue(result.contains("""height="${60 * scale}""""))
    }

    @Test
    fun `leaves an already-large viewBox untouched`() {
        val xml = """<svg viewBox="0 0 3840 1265.9"><path d="M1 1"/></svg>"""

        assertNull(normalizeSmallViewBoxSvg(xml))
    }

    @Test
    fun `leaves already-large width and height untouched when there is no viewBox`() {
        val xml = """<svg width="1065" height="187"><path d="M1 1"/></svg>"""

        assertNull(normalizeSmallViewBoxSvg(xml))
    }

    @Test
    fun `returns null for a document with no svg tag`() {
        assertNull(normalizeSmallViewBoxSvg("<not-an-svg/>"))
    }

    @Test
    fun `returns null when neither a viewBox nor numeric width and height can be parsed`() {
        val xml = """<svg width="100%" height="100%"><path d="M1 1"/></svg>"""

        assertNull(normalizeSmallViewBoxSvg(xml))
    }

    @Test
    fun `preserves everything between the opening and closing tags, including defs and metadata`() {
        val xml = """
            <svg viewBox="0 0 168 62">
              <defs><clipPath id="a"><rect width="10" height="10"/></clipPath></defs>
              <sodipodi:namedview id="namedview"/>
              <path d="M1 1" clip-path="url(#a)"/>
            </svg>
        """.trimIndent()

        val result = normalizeSmallViewBoxSvg(xml)

        assertTrue(result != null)
        assertTrue(result!!.contains("""<clipPath id="a"><rect width="10" height="10"/></clipPath>"""))
        assertTrue(result.contains("""<sodipodi:namedview id="namedview"/>"""))
        assertTrue(result.contains("""<path d="M1 1" clip-path="url(#a)"/>"""))
    }
}
