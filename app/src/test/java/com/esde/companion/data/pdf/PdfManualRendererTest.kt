package com.esde.companion.data.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfManualRendererTest {
    @Test
    fun `targetWidthPx of zero returns null`() {
        assertNull(scaledSize(pageWidth = 100, pageHeight = 200, targetWidthPx = 0))
    }

    @Test
    fun `targetWidthPx of negative returns null`() {
        assertNull(scaledSize(pageWidth = 100, pageHeight = 200, targetWidthPx = -10))
    }

    @Test
    fun `equal target width preserves aspect ratio at scale 1`() {
        assertEquals(100 to 200, scaledSize(pageWidth = 100, pageHeight = 200, targetWidthPx = 100))
    }

    @Test
    fun `scaling up computes height proportionally`() {
        assertEquals(200 to 400, scaledSize(pageWidth = 100, pageHeight = 200, targetWidthPx = 200))
    }

    @Test
    fun `scaling down truncates height rather than rounding`() {
        // scale = 1/3; 100 * (1/3) = 33.33... -> 33, not 33 rounded up to 34.
        assertEquals(100 to 33, scaledSize(pageWidth = 300, pageHeight = 100, targetWidthPx = 100))
    }

    @Test
    fun `a computed height of zero is coerced up to 1`() {
        assertEquals(1 to 1, scaledSize(pageWidth = 1000, pageHeight = 1, targetWidthPx = 1))
    }
}
