package com.esde.companion.domain.gameguides

/**
 * Reported by [com.esde.companion.data.gameguides.GameFaqsBrowserBridge.downloadFullGuide]
 * while a guide is being saved, so the UI can show real per-chapter progress instead of a
 * plain indeterminate spinner for however long the download takes.
 */
sealed interface GuideDownloadProgress {
    /** A chapter page has just been fetched - [page] is 1-based, [totalPages] is the number
     * of chapters listed in the guide's own table of contents (known from the first page). */
    data class LoadingPage(val page: Int, val totalPages: Int) : GuideDownloadProgress

    /** Images/headings for one already-fetched chapter page are being processed - [page]/
     * [totalPages] mirror [LoadingPage]'s, since this phase also runs once per saved page
     * (see `GameFaqsBrowserBridge.downloadFullGuide`), not once for the whole guide. */
    data class EmbeddingImages(val page: Int, val totalPages: Int) : GuideDownloadProgress
}
