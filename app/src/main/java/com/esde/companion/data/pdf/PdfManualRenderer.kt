package com.esde.companion.data.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wraps android.graphics.pdf.PdfRenderer for a single opened manual PDF. Deliberately not
 * behind a domain interface - like Coil's image decoding, this is a rendering concern
 * that stays in data/UI; domain already exposes the manual's file path via
 * GameMedia/MediaType.Manuals and has no reason to know PDFs are involved.
 *
 * PdfRenderer only allows one open Page at a time and is not thread-safe - [mutex] plus
 * closing each Page immediately after use enforces that from a single call site, since
 * callers may request the current page and a prefetched next page from coroutines
 * launched close together.
 *
 * Must be closed via [close] once the caller is done (game changed, or PlayingGame
 * ended) - holds a real file descriptor open until then. See GameManualViewModel.
 */
class PdfManualRenderer private constructor(
    private val fileDescriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) {
    private val mutex = Mutex()

    val pageCount: Int get() = renderer.pageCount

    /** Renders [index] (0-based) at roughly [targetWidthPx] wide. Null on any failure. */
    suspend fun renderPage(
        index: Int,
        targetWidthPx: Int,
    ): ImageBitmap? =
        withContext(Dispatchers.IO) {
            if (index !in 0 until renderer.pageCount || targetWidthPx <= 0) return@withContext null
            mutex.withLock {
                runCatching {
                    renderer.openPage(index).use { page ->
                        val scale = targetWidthPx.toFloat() / page.width
                        val targetHeightPx = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap.asImageBitmap()
                    }
                }.getOrNull()
            }
        }

    fun close() {
        runCatching { renderer.close() }
        runCatching { fileDescriptor.close() }
    }

    companion object {
        /** Returns null if [pdfPath] doesn't exist or can't be opened as a PDF. */
        suspend fun open(pdfPath: String): PdfManualRenderer? =
            withContext(Dispatchers.IO) {
                runCatching {
                    val fd = ParcelFileDescriptor.open(File(pdfPath), ParcelFileDescriptor.MODE_READ_ONLY)
                    PdfManualRenderer(fd, PdfRenderer(fd))
                }.getOrNull()
            }
    }
}
