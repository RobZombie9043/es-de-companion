package com.esde.companion.data.gameguides

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val CONNECT_TIMEOUT_MS = 8_000
private const val READ_TIMEOUT_MS = 8_000
private const val MAX_CONCURRENT_DOWNLOADS = 12
private const val DEFAULT_MIME_TYPE = "image/jpeg"

// A single oversized image (a rare full-resolution screenshot rather than a web-optimized
// one) is skipped outright - same benign "left pointing at its original URL" fallback as any
// other failed download - rather than let it dominate the page-wide budget below.
private const val MAX_SINGLE_IMAGE_BYTES = 1_500_000

// Confirmed crash cause: a Zelda Dungeon chapter page can carry 70-180 real screenshots
// (vs. GameFAQs' mostly-text guides, which never exercised this path at any real scale).
// Embedding all of them as base64 built one ~53MB string that failed to allocate on top of
// everything else already on the heap (OutOfMemoryError in
// GuidePageContentProcessor.substituteImageSrcs, "51228856 free bytes... 48MB until OOM").
// Capping the total raw bytes embedded per page bounds each chapter's contribution to the
// guide's in-memory page list - the images past the budget just keep pointing at their
// original network URL, same as any other failed/skipped download, rather than crash the
// whole download.
private const val MAX_EMBEDDED_BYTES_PER_PAGE = 6_000_000

/**
 * Downloads guide images as base64 data URIs via real concurrent native HTTP connections -
 * the same [HttpURLConnection] tool `data/update/GitHubUpdateRepository.kt` already uses to
 * download the update APK, just fetching image bytes by URL instead of a GitHub API
 * response, not a new networking mechanism.
 *
 * Replaces an earlier approach that fetched images from inside the browser [WebView]'s own
 * JS (`fetch()`), sandboxed by that WebView's per-origin connection limit and sharing its
 * single rendering thread with page navigation - confirmed on a real 19-chapter guide taking
 * 5-10 minutes to download versus a competitor app finishing the same guide in seconds. Real
 * concurrent sockets outside the WebView, bounded only by [MAX_CONCURRENT_DOWNLOADS], removes
 * both bottlenecks.
 */
class NativeImageDownloader {
    /** Best-effort - any URL that fails, times out, is individually oversized, or would push
     * the page past [MAX_EMBEDDED_BYTES_PER_PAGE] is simply absent from the returned map, left
     * pointing at its original (network) URL by whichever caller substitutes these in. The
     * budget is applied in [urls]' own order (its callers pass images in DOM order), not
     * download-completion order, so which images get embedded doesn't depend on network
     * timing. */
    suspend fun downloadAsDataUris(urls: List<String>): Map<String, String> =
        withContext(Dispatchers.IO) {
            val semaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)
            val downloads =
                urls.distinct()
                    .map { url -> async { url to semaphore.withPermit { downloadOne(url) } } }
                    .awaitAll()
            withinPageBudget(downloads)
        }

    /** Walks [downloads] in order, keeping images until their combined raw size would exceed
     * [MAX_EMBEDDED_BYTES_PER_PAGE] - everything after that point is dropped, same fallback as
     * an individually failed download. */
    private fun withinPageBudget(downloads: List<Pair<String, DownloadedImage?>>): Map<String, String> {
        var budgetRemaining = MAX_EMBEDDED_BYTES_PER_PAGE
        val result = mutableMapOf<String, String>()
        for ((url, image) in downloads) {
            if (image == null || image.rawByteCount > budgetRemaining) continue
            result[url] = image.dataUri
            budgetRemaining -= image.rawByteCount
        }
        return result
    }

    private fun downloadOne(url: String): DownloadedImage? {
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
                    val mimeType = imageMimeType(connection.contentType, url)
                    val dataUri = "data:$mimeType;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
                    DownloadedImage(dataUri, bytes.size)
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

    private fun imageMimeType(
        contentType: String?,
        url: String,
    ): String {
        val declared = contentType?.substringBefore(';')?.trim()?.takeIf { it.startsWith("image/") }
        if (declared != null) return declared
        return when (url.substringAfterLast('.', "").substringBefore('?').lowercase()) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            else -> DEFAULT_MIME_TYPE
        }
    }
}

private data class DownloadedImage(
    val dataUri: String,
    val rawByteCount: Int,
)
