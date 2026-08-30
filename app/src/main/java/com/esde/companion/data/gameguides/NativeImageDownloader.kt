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
    /** Best-effort - any URL that fails or times out is simply absent from the returned map,
     * left pointing at its original (network) URL by whichever caller substitutes these in. */
    suspend fun downloadAsDataUris(urls: List<String>): Map<String, String> =
        withContext(Dispatchers.IO) {
            val semaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)
            urls.distinct()
                .map { url -> async { url to semaphore.withPermit { downloadOne(url) } } }
                .awaitAll()
                .mapNotNull { (url, dataUri) -> dataUri?.let { url to it } }
                .toMap()
        }

    private fun downloadOne(url: String): String? {
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
                val mimeType = imageMimeType(connection.contentType, url)
                "data:$mimeType;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
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
