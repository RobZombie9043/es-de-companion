package com.esde.companion.data.update

import android.content.Context
import com.esde.companion.domain.model.ApkAsset
import com.esde.companion.domain.model.DownloadState
import com.esde.companion.domain.model.ReleaseFetchResult
import com.esde.companion.domain.repository.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val GITHUB_OWNER = "RobZombie9043"
private const val GITHUB_REPO = "es-de-companion"
private const val API_BASE = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO"

// GitHub's API 403s anonymous requests with no User-Agent header.
private const val USER_AGENT = "ES-DE-Companion-UpdateChecker"

private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 15_000
private const val DOWNLOAD_BUFFER_SIZE = 8192
private const val PERCENT_MULTIPLIER = 100

/**
 * Talks to the GitHub Releases API to check for/fetch release info, and downloads a
 * release's `.apk` asset into this app's private cache dir. The sole networked
 * repository in this app - see CLAUDE.md's "What NOT to Do" for why this is a narrow,
 * deliberate exception rather than a general backend/API layer.
 */
class GitHubUpdateRepository(
    private val context: Context,
) : UpdateRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchLatestRelease(): ReleaseFetchResult = fetchRelease("$API_BASE/releases/latest")

    override suspend fun fetchReleaseForVersion(versionName: String): ReleaseFetchResult =
        fetchRelease("$API_BASE/releases/tags/v$versionName")

    private suspend fun fetchRelease(url: String): ReleaseFetchResult =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection =
                    (URL(url).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", USER_AGENT)
                        setRequestProperty("Accept", "application/vnd.github+json")
                        connectTimeout = CONNECT_TIMEOUT_MS
                        readTimeout = READ_TIMEOUT_MS
                    }

                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val body = connection.inputStream.use { it.reader().readText() }
                        ReleaseFetchResult.Success(json.decodeFromString<GitHubReleaseDto>(body).toDomain())
                    }
                    HttpURLConnection.HTTP_NOT_FOUND -> ReleaseFetchResult.NotFound
                    else -> ReleaseFetchResult.NetworkError("Unexpected response: ${connection.responseCode}")
                }
            } catch (e: IOException) {
                ReleaseFetchResult.NetworkError(e.message ?: "Network error")
            } catch (e: SerializationException) {
                ReleaseFetchResult.NetworkError(e.message ?: "Malformed response")
            } finally {
                connection?.disconnect()
            }
        }

    override fun downloadApk(asset: ApkAsset): Flow<DownloadState> =
        flow {
            var connection: HttpURLConnection? = null
            try {
                connection =
                    (URL(asset.downloadUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", USER_AGENT)
                        connectTimeout = CONNECT_TIMEOUT_MS
                        readTimeout = READ_TIMEOUT_MS
                        instanceFollowRedirects = true
                    }

                val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: asset.sizeBytes
                val destinationDir = File(context.cacheDir, "apk_updates").apply { mkdirs() }
                val destinationFile = File(destinationDir, asset.fileName)

                var downloadedBytes = 0L
                var lastEmittedPercent = -1

                connection.inputStream.use { input ->
                    destinationFile.outputStream().use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read

                            val percent =
                                if (totalBytes > 0) ((downloadedBytes * PERCENT_MULTIPLIER) / totalBytes).toInt() else 0
                            if (percent != lastEmittedPercent) {
                                lastEmittedPercent = percent
                                emit(DownloadState.Progress(percent))
                            }
                        }
                    }
                }

                emit(DownloadState.Success(destinationFile.absolutePath))
            } catch (e: IOException) {
                emit(DownloadState.Failed(e.message ?: "Download failed"))
            } finally {
                connection?.disconnect()
            }
        }.flowOn(Dispatchers.IO)
}
