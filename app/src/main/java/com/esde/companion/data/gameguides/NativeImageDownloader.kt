package com.esde.companion.data.gameguides

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val CONNECT_TIMEOUT_MS = 8_000
private const val READ_TIMEOUT_MS = 8_000
private const val MAX_CONCURRENT_DOWNLOADS = 12
private const val IMAGES_SUBDIRECTORY = "images"

// A single oversized image (a rare full-resolution screenshot rather than a web-optimized
// one) is skipped outright - same benign "left pointing at its original URL" fallback as any
// other failed download - rather than let a single pathological image dominate a guide's disk
// footprint. Checked against the ORIGINAL downloaded bytes, before downscaling - a legitimately
// huge source image is exactly the case downscaling exists to fix, not a reason to skip it.
private const val MAX_SINGLE_IMAGE_BYTES = 1_500_000

// GameFAQs screenshots are routinely saved at their original capture resolution (a real guide's
// images averaged ~220KB each, several past 400KB, confirmed via on-device inspection) despite
// being displayed at, at most, this app's own WebView width - full resolution buys nothing
// visually there. Re-encoding to a phone-sized max dimension and JPEG quality cut a real
// 34-image guide page from 7.4MB to a small fraction of that, directly shortening how long that
// page's worth of WebViewAssetLoader round-trips (one real request per image, see this class's
// own kdoc) takes to resolve - confirmed as the actual bottleneck (not layer type, not
// per-request overhead) via that same page consistently taking multiple seconds to load versus
// a same-guide page with only 4 images loading in well under 200ms.
private const val MAX_IMAGE_DIMENSION_PX = 1080
private const val JPEG_QUALITY = 82

/**
 * Downloads guide images as real on-disk files (under `<mediaDirectoryPath>/images/`) via real
 * concurrent native HTTP connections - the same [HttpURLConnection] tool
 * `data/update/GitHubUpdateRepository.kt` already uses to download the update APK, just
 * fetching image bytes by URL instead of a GitHub API response, not a new networking mechanism.
 *
 * Replaces an earlier approach that fetched images from inside the browser [WebView]'s own
 * JS (`fetch()`), sandboxed by that WebView's per-origin connection limit and sharing its
 * single rendering thread with page navigation - confirmed on a real 19-chapter guide taking
 * 5-10 minutes to download versus a competitor app finishing the same guide in seconds. Real
 * concurrent sockets outside the WebView, bounded only by [MAX_CONCURRENT_DOWNLOADS], removes
 * both bottlenecks.
 *
 * Also replaces a second, more recent approach that returned images as base64 `data:` URIs for
 * [GuidePageContentProcessor] to inline directly into a page's HTML text. That inflated a
 * page's own HTML by roughly a third per image (base64 overhead) plus the cost of JSON-escaping
 * and textually rebuilding that now-much-larger string twice (once per JS template
 * substitution) - confirmed on-device as an OutOfMemoryError inside
 * `GuidePageContentProcessor.substituteImageSrcsScript`'s own `StringBuilder` work for a real
 * image-heavy guide page, and a second, related OutOfMemoryError inside the *viewer's*
 * `WebView.loadDataWithBaseURL` (which re-encodes the whole already-bloated document to base64
 * a second time internally) when that same page was later opened. Writing each image to its
 * own file and referencing it by a plain relative path keeps a page's own HTML close to its
 * original scraped size regardless of how many images it embeds, and lets the WebView decode
 * each image file independently instead of parsing one giant base64 blob out of the DOM.
 */
class NativeImageDownloader {
    /** Best-effort - any URL that fails to download, times out, or is individually oversized is
     * simply absent from the returned map, left pointing at its original (network) URL by
     * whichever caller substitutes these in. Files are named `p<pageIndex>_<index>.<ext>` (by
     * [urls]' own order, which callers pass in DOM order) so re-downloading the same page never
     * collides with a previous attempt's files - [substituteImageSrcs] only ever swaps in
     * entries this run actually produced. */
    suspend fun downloadImages(
        urls: List<String>,
        mediaDirectoryPath: String,
        pageIndex: Int,
    ): Map<String, String> =
        withContext(Dispatchers.IO) {
            val imagesDir = File(mediaDirectoryPath, IMAGES_SUBDIRECTORY).apply { mkdirs() }
            val semaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)
            urls.distinct()
                .mapIndexed { index, url -> url to index }
                .map { (url, index) ->
                    async {
                        val relativePath = semaphore.withPermit { downloadOne(url, imagesDir, pageIndex, index) }
                        url to relativePath
                    }
                }
                .awaitAll()
                .mapNotNull { (url, relativePath) -> relativePath?.let { url to it } }
                .toMap()
        }

    /** Returns the saved file's path relative to [mediaDirectoryPath]'s directory (e.g.
     * `images/p0_3.jpg`) - relative, not absolute, since the viewer's own composed document
     * lives in that same directory (see `writeViewerDocument`) and resolves sibling files by
     * relative path regardless of where that directory actually sits on disk. */
    private fun downloadOne(
        url: String,
        imagesDir: File,
        pageIndex: Int,
        index: Int,
    ): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection =
                (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    instanceFollowRedirects = true
                }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                null
            } else {
                val bytes = connection.inputStream.use { it.readBytes() }
                if (bytes.size > MAX_SINGLE_IMAGE_BYTES) {
                    null
                } else {
                    val extension = imageFileExtension(connection.contentType, url)
                    val (finalBytes, finalExtension) = downscaledOrOriginal(bytes, extension)
                    val fileName = "p${pageIndex}_$index.$finalExtension"
                    File(imagesDir, fileName).writeBytes(finalBytes)
                    "$IMAGES_SUBDIRECTORY/$fileName"
                }
            }
        } catch (
            @Suppress("SwallowedException") e: IOException,
        ) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun imageFileExtension(
        contentType: String?,
        url: String,
    ): String {
        val declaredType = contentType?.substringBefore(';')?.trim()?.takeIf { it.startsWith("image/") }
        val urlExtension = url.substringAfterLast('.', "").substringBefore('?').lowercase()
        return when {
            declaredType == "image/png" || urlExtension == "png" -> "png"
            declaredType == "image/gif" || urlExtension == "gif" -> "gif"
            declaredType == "image/webp" || urlExtension == "webp" -> "webp"
            declaredType == "image/svg+xml" || urlExtension == "svg" -> "svg"
            else -> "jpg"
        }
    }

    /** Downscales+re-encodes [bytes] as JPEG when it's a raster format actually worth doing that
     * to and decodes successfully - GIFs are skipped outright (a real animation would lose its
     * frames if decoded through [BitmapFactory], which only ever reads the first one) and SVGs
     * aren't a [Bitmap] to begin with. Falls back to the original bytes/extension untouched on
     * any decode failure or when the image is already at or under [MAX_IMAGE_DIMENSION_PX] -
     * this is a size optimization, never a correctness requirement, so "couldn't shrink it"
     * degrades to "keep what was already a normal download" rather than dropping the image. */
    private fun downscaledOrOriginal(
        bytes: ByteArray,
        extension: String,
    ): Pair<ByteArray, String> {
        if (extension == "gif" || extension == "svg") return bytes to extension
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val downscaled =
            original?.let {
                try {
                    downscaleToJpeg(it)
                } finally {
                    it.recycle()
                }
            }
        return downscaled ?: (bytes to extension)
    }

    /** Null (not downscaled) when [original] is already at or under [MAX_IMAGE_DIMENSION_PX] -
     * the caller falls back to the original bytes in that case, same as a decode failure. */
    private fun downscaleToJpeg(original: Bitmap): Pair<ByteArray, String>? {
        val longestSide = maxOf(original.width, original.height)
        if (longestSide <= MAX_IMAGE_DIMENSION_PX) return null
        val scale = MAX_IMAGE_DIMENSION_PX.toFloat() / longestSide
        val scaledWidth = (original.width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (original.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true)
        return try {
            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            output.toByteArray() to "jpg"
        } finally {
            if (scaled !== original) scaled.recycle()
        }
    }
}
