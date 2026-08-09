package com.esde.companion.data.update

import android.content.ContextWrapper
import app.cash.turbine.test
import com.esde.companion.domain.model.ApkAsset
import com.esde.companion.domain.model.DownloadState
import com.esde.companion.domain.model.ReleaseFetchResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

/**
 * A minimal single-request HTTP/1.1 server for exercising GitHubUpdateRepository's real
 * networking code against a local socket, without com.sun.net.httpserver (not resolvable
 * on this project's unit test compile classpath) or a mocking framework.
 */
private class SingleRequestHttpServer(
    private val respond: (requestPath: String, socket: Socket) -> Unit,
) {
    private val serverSocket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
    val port: Int get() = serverSocket.localPort

    private val thread =
        Thread {
            try {
                serverSocket.accept().use { socket ->
                    val requestPath = socket.getInputStream().readRequestPath()
                    respond(requestPath, socket)
                }
            } catch (_: java.io.IOException) {
                // Server socket closed (e.g. test tore down without a request) - not a failure.
            }
        }.apply {
            isDaemon = true
            start()
        }

    private fun java.io.InputStream.readRequestPath(): String {
        val reader = bufferedReader(StandardCharsets.ISO_8859_1)
        val requestLine = reader.readLine().orEmpty()
        generateSequence { reader.readLine() }.firstOrNull { it.isEmpty() }
        return requestLine.split(" ").getOrElse(1) { "/" }
    }

    fun stop() {
        serverSocket.close()
        thread.join(1_000)
    }
}

private fun OutputStream.writeHeadAndBody(
    statusCode: Int,
    body: ByteArray,
    contentLength: Long = body.size.toLong(),
) {
    val head =
        "HTTP/1.1 $statusCode OK\r\n" +
            "Content-Length: $contentLength\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "\r\n"
    write(head.toByteArray(StandardCharsets.ISO_8859_1))
    write(body)
    flush()
}

class GitHubUpdateRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeContext(private val dir: File) : ContextWrapper(null) {
        override fun getCacheDir(): File = dir
    }

    private var server: SingleRequestHttpServer? = null

    @After
    fun tearDown() {
        server?.stop()
    }

    private fun startServer(respond: (requestPath: String, socket: Socket) -> Unit): String {
        val httpServer = SingleRequestHttpServer(respond)
        server = httpServer
        return "http://127.0.0.1:${httpServer.port}"
    }

    private fun releaseJson(tagName: String = "v0.8.0") =
        """
        {
          "tag_name": "$tagName",
          "body": "Notes",
          "html_url": "https://example.com/release",
          "assets": [
            { "name": "app.apk", "browser_download_url": "https://example.com/app.apk", "size": 100 }
          ]
        }
        """.trimIndent()

    // --- fetchLatestRelease / fetchReleaseForVersion --------------------------------------

    @Test
    fun `a 200 response with a valid body maps to Success with the release's fields`() =
        runTest {
            val apiBase =
                startServer { _, socket ->
                    socket.getOutputStream().writeHeadAndBody(200, releaseJson().toByteArray(StandardCharsets.UTF_8))
                }
            val repository = GitHubUpdateRepository(FakeContext(tempFolder.newFolder()), apiBase)

            val result = repository.fetchLatestRelease()

            check(result is ReleaseFetchResult.Success)
            assertEquals("0.8.0", result.release.versionName)
            assertEquals("app.apk", result.release.apkAsset?.fileName)
        }

    @Test
    fun `a 404 response maps to NotFound`() =
        runTest {
            val apiBase = startServer { _, socket -> socket.getOutputStream().writeHeadAndBody(404, ByteArray(0)) }
            val repository = GitHubUpdateRepository(FakeContext(tempFolder.newFolder()), apiBase)

            val result = repository.fetchLatestRelease()

            assertEquals(ReleaseFetchResult.NotFound, result)
        }

    @Test
    fun `an unexpected response code maps to NetworkError containing the code`() =
        runTest {
            val apiBase = startServer { _, socket -> socket.getOutputStream().writeHeadAndBody(500, ByteArray(0)) }
            val repository = GitHubUpdateRepository(FakeContext(tempFolder.newFolder()), apiBase)

            val result = repository.fetchLatestRelease()

            check(result is ReleaseFetchResult.NetworkError)
            assertTrue(result.message.contains("500"))
        }

    @Test
    fun `malformed JSON on a 200 response maps to NetworkError`() =
        runTest {
            val apiBase =
                startServer { _, socket ->
                    val malformedBody = "{ not valid json".toByteArray(StandardCharsets.UTF_8)
                    socket.getOutputStream().writeHeadAndBody(200, malformedBody)
                }
            val repository = GitHubUpdateRepository(FakeContext(tempFolder.newFolder()), apiBase)

            val result = repository.fetchLatestRelease()

            assertTrue(result is ReleaseFetchResult.NetworkError)
        }

    @Test
    fun `a connection failure maps to NetworkError`() =
        runTest {
            val closedPort = ServerSocket(0).use { it.localPort }
            val apiBase = "http://127.0.0.1:$closedPort"
            val repository = GitHubUpdateRepository(FakeContext(tempFolder.newFolder()), apiBase)

            val result = repository.fetchLatestRelease()

            assertTrue(result is ReleaseFetchResult.NetworkError)
        }

    @Test
    fun `fetchReleaseForVersion requests the v-prefixed tag path`() =
        runTest {
            var requestedPath: String? = null
            val apiBase =
                startServer { path, socket ->
                    requestedPath = path
                    socket.getOutputStream().writeHeadAndBody(200, releaseJson().toByteArray(StandardCharsets.UTF_8))
                }
            val repository = GitHubUpdateRepository(FakeContext(tempFolder.newFolder()), apiBase)

            repository.fetchReleaseForVersion("0.7.0")

            assertEquals("/releases/tags/v0.7.0", requestedPath)
        }

    // --- downloadApk ------------------------------------------------------------------------

    @Test
    fun `a successful download emits non-decreasing progress ending in Success with the correct byte count`() =
        runTest {
            val payload = ByteArray(1_000_000) { (it % 251).toByte() }
            val apiBase = startServer { _, socket -> socket.getOutputStream().writeHeadAndBody(200, payload) }
            val cacheDir = tempFolder.newFolder()
            val repository = GitHubUpdateRepository(FakeContext(cacheDir), apiBase)
            val asset =
                ApkAsset(downloadUrl = "$apiBase/app.apk", fileName = "app.apk", sizeBytes = payload.size.toLong())

            val states = mutableListOf<DownloadState>()
            repository.downloadApk(asset).test {
                while (true) {
                    val item = awaitItem()
                    states += item
                    if (item is DownloadState.Success || item is DownloadState.Failed) break
                }
                cancelAndIgnoreRemainingEvents()
            }

            val progressPercents = states.filterIsInstance<DownloadState.Progress>().map { it.percent }
            assertEquals(progressPercents, progressPercents.sorted())
            // Only emitted on change - no two consecutive Progress states share a percent.
            assertEquals(progressPercents.distinct().size, progressPercents.size)

            val success = states.last()
            check(success is DownloadState.Success)
            val downloadedFile = File(success.filePath)
            assertEquals(payload.size.toLong(), downloadedFile.length())
            assertEquals(File(cacheDir, "apk_updates/app.apk").absolutePath, downloadedFile.absolutePath)
        }

    @Test
    fun `a connection that drops mid-stream emits Failed and no Success afterward`() =
        runTest {
            val apiBase =
                startServer { _, socket ->
                    // Declares far more bytes than are actually sent, then forces an abortive
                    // close (RST via SO_LINGER=0, rather than a graceful FIN) - the client's
                    // read past what's available should surface as an IOException.
                    socket.getOutputStream().writeHeadAndBody(200, ByteArray(500), contentLength = 1_000_000L)
                    socket.setSoLinger(true, 0)
                }
            val repository = GitHubUpdateRepository(FakeContext(tempFolder.newFolder()), apiBase)
            val asset = ApkAsset(downloadUrl = "$apiBase/app.apk", fileName = "app.apk", sizeBytes = 1_000_000L)

            val states = mutableListOf<DownloadState>()
            repository.downloadApk(asset).test {
                while (true) {
                    val item = awaitItem()
                    states += item
                    if (item is DownloadState.Success || item is DownloadState.Failed) break
                }
                cancelAndIgnoreRemainingEvents()
            }

            assertTrue(states.last() is DownloadState.Failed)
            assertTrue(states.none { it is DownloadState.Success })
        }
}
