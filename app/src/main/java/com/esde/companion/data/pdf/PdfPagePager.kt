package com.esde.companion.data.pdf

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Renders [renderer]'s pages one "current + next" pair at a time, reusing a previously
 * prefetched next-page bitmap instead of re-rendering it once the caller actually advances to
 * that exact page - the whole point of prefetching in the first place. Before this existed,
 * both [com.esde.companion.ui.manual.GameManualViewModel] and
 * [com.esde.companion.ui.gameguides.GameGuidePdfViewerScreen] (independently) rendered every
 * page fresh via [ManualRenderer.renderPage] regardless of whether it had just been
 * prefetched, silently discarding the resulting next-page bitmap and re-rendering the "new"
 * current page from scratch - confirmed as dead code with no actual paging-speed benefit,
 * not a working prefetch that the guide viewer was merely missing a copy of.
 *
 * [targetWidthPx] is tracked alongside the prefetched page/bitmap so a width change (a fresh
 * measured width, not just a no-op re-report of the same one) can't reuse a wrong-sized cached
 * bitmap for a page that happens to match by index alone.
 */
class PdfPagePager(private val renderer: ManualRenderer) {
    private var prefetchedPage: Int? = null
    private var prefetchedBitmap: ImageBitmap? = null
    private var prefetchedWidthPx: Int = 0

    data class Result(val currentBitmap: ImageBitmap?, val nextBitmap: ImageBitmap?)

    /** Resolves [page]'s bitmap (reusing the prior call's prefetch when [page] is exactly the
     * page that was prefetched, at the same [targetWidthPx]) and renders whichever page comes
     * after it for the following call to reuse the same way. */
    suspend fun render(
        page: Int,
        targetWidthPx: Int,
    ): Result {
        val current =
            if (page == prefetchedPage && targetWidthPx == prefetchedWidthPx) {
                prefetchedBitmap
            } else {
                renderer.renderPage(page, targetWidthPx)
            }
        val nextIndex = page + 1
        val next = if (nextIndex < renderer.pageCount) renderer.renderPage(nextIndex, targetWidthPx) else null
        prefetchedPage = nextIndex
        prefetchedBitmap = next
        prefetchedWidthPx = targetWidthPx
        return Result(current, next)
    }
}
