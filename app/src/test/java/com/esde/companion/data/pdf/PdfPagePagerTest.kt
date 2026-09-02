package com.esde.companion.data.pdf

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfPagePagerTest {
    @Suppress("EmptyFunctionBlock")
    private class FakeManualRenderer(override val pageCount: Int) : ManualRenderer {
        val renderedPages = mutableListOf<Int>()

        override suspend fun renderPage(
            index: Int,
            targetWidthPx: Int,
        ): Nothing? {
            renderedPages += index
            return null
        }

        override fun close() {}
    }

    @Test
    fun `first render renders the requested page and prefetches the next one`() =
        runTest {
            val renderer = FakeManualRenderer(pageCount = 5)
            val pager = PdfPagePager(renderer)

            pager.render(page = 0, targetWidthPx = 200)

            assertEquals(listOf(0, 1), renderer.renderedPages)
        }

    @Test
    fun `advancing to the prefetched page reuses it instead of re-rendering`() =
        runTest {
            val renderer = FakeManualRenderer(pageCount = 5)
            val pager = PdfPagePager(renderer)

            pager.render(page = 0, targetWidthPx = 200)
            renderer.renderedPages.clear()
            pager.render(page = 1, targetWidthPx = 200)

            // Only page 2 (the new prefetch) should render - page 1 was already prefetched
            // by the previous call at the same width.
            assertEquals(listOf(2), renderer.renderedPages)
        }

    @Test
    fun `jumping to a page that was not prefetched renders it fresh`() =
        runTest {
            val renderer = FakeManualRenderer(pageCount = 5)
            val pager = PdfPagePager(renderer)

            pager.render(page = 0, targetWidthPx = 200)
            renderer.renderedPages.clear()
            pager.render(page = 3, targetWidthPx = 200)

            assertEquals(listOf(3, 4), renderer.renderedPages)
        }

    @Test
    fun `a width change re-renders the current page even if its index was prefetched`() =
        runTest {
            val renderer = FakeManualRenderer(pageCount = 5)
            val pager = PdfPagePager(renderer)

            pager.render(page = 0, targetWidthPx = 200)
            renderer.renderedPages.clear()
            pager.render(page = 1, targetWidthPx = 400)

            // Page 1 was only ever prefetched at width 200 - a genuine width change must not
            // reuse that stale-sized bitmap just because the page index matches.
            assertEquals(listOf(1, 2), renderer.renderedPages)
        }

    @Test
    fun `no prefetch beyond the last page`() =
        runTest {
            val renderer = FakeManualRenderer(pageCount = 3)
            val pager = PdfPagePager(renderer)

            val result = pager.render(page = 2, targetWidthPx = 200)

            assertEquals(listOf(2), renderer.renderedPages)
            assertNull(result.nextBitmap)
        }
}
