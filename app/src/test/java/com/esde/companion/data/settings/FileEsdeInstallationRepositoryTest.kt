package com.esde.companion.data.settings

import app.cash.turbine.test
import com.esde.companion.data.storage.DirectoryWatcher
import com.esde.companion.domain.model.EsdeEventScriptSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileEsdeInstallationRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    /** Records [recheck] calls so a test can assert self-heal rearm behavior. */
    private class RecordingDirectoryWatcher : DirectoryWatcher {
        var rechecksForced = 0

        override fun startWatching() = Unit

        override fun stopWatching() = Unit

        override fun recheck(forceRearm: Boolean) {
            if (forceRearm) rechecksForced++
        }
    }

    private object NoOpDirectoryWatcher : DirectoryWatcher {
        override fun startWatching() = Unit

        override fun stopWatching() = Unit
    }

    private fun repositoryFor(
        fallbackPollIntervalMs: Long = 3_000L,
        watchDirectory: (String, Int, () -> Unit) -> DirectoryWatcher = { _, _, _ -> NoOpDirectoryWatcher },
        storageEvents: Flow<Unit> = MutableSharedFlow(),
    ): FileEsdeInstallationRepository =
        FileEsdeInstallationRepository(
            fallbackPollIntervalMs = fallbackPollIntervalMs,
            watchDirectory = watchDirectory,
            storageEvents = storageEvents,
        )

    private fun writeSettingsXml(
        esdeRoot: File,
        customEventScripts: Boolean,
    ) {
        val settingsFile = File(esdeRoot, "settings/es_settings.xml")
        settingsFile.parentFile?.mkdirs()
        settingsFile.writeText(
            """
            <bool name="CustomEventScripts" value="$customEventScripts" />
            <bool name="CustomEventScriptsBrowsing" value="true" />
            <bool name="DebugMode" value="true" />
            <bool name="DebugSkipInputLogging" value="false" />
            """.trimIndent(),
        )
    }

    @Test
    fun `observeEventScriptSettings emits the parsed settings on subscribe`() =
        runTest {
            val esdeRoot = tempFolder.newFolder("ES-DE")
            writeSettingsXml(esdeRoot, customEventScripts = true)

            val repository = repositoryFor()

            repository.observeEventScriptSettings(esdeRoot.absolutePath).test {
                assertEquals(
                    EsdeEventScriptSettings(
                        customEventScripts = true,
                        customEventScriptsBrowsing = true,
                        debugMode = true,
                        debugSkipInputLogging = false,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a fallback-caught settings change forces the directory watcher to rearm`() =
        runTest {
            val esdeRoot = tempFolder.newFolder("ES-DE")
            writeSettingsXml(esdeRoot, customEventScripts = false)

            lateinit var capturedWatcher: RecordingDirectoryWatcher
            val repository =
                repositoryFor(
                    fallbackPollIntervalMs = 20L,
                    watchDirectory = { _, _, _ -> RecordingDirectoryWatcher().also { capturedWatcher = it } },
                )

            repository.observeEventScriptSettings(esdeRoot.absolutePath).test {
                // Not asserted here: capturedWatcher is assigned by the producer coroutine
                // (running on Dispatchers.IO, a real thread) after this first emission, so
                // there's no guarantee it has run yet the instant awaitItem() returns -
                // asserting on it only after the second item is what's actually deterministic.
                awaitItem()

                writeSettingsXml(esdeRoot, customEventScripts = true)

                assertEquals(true, awaitItem()?.customEventScripts)
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, capturedWatcher.rechecksForced)
        }

    @Test
    fun `a storage-changed signal triggers an immediate re-parse without waiting for the fallback interval`() =
        runTest {
            val esdeRoot = tempFolder.newFolder("ES-DE")
            writeSettingsXml(esdeRoot, customEventScripts = false)

            val storageEvents = MutableSharedFlow<Unit>()
            val repository =
                repositoryFor(
                    // Long enough that the fallback ticker can't plausibly fire during the
                    // test - proves the storage-events signal drove the re-parse.
                    fallbackPollIntervalMs = 3_600_000L,
                    storageEvents = storageEvents,
                )

            repository.observeEventScriptSettings(esdeRoot.absolutePath).test {
                awaitItem()

                // storageEventsJob subscribes from the producer coroutine on a real
                // Dispatchers.IO thread, asynchronously with respect to this test
                // coroutine - emitting before it has actually subscribed silently drops
                // the value (MutableSharedFlow has no buffer for a replay-0 flow with no
                // collectors), so wait for the subscription to land first.
                storageEvents.subscriptionCount.first { it > 0 }

                writeSettingsXml(esdeRoot, customEventScripts = true)
                storageEvents.emit(Unit)

                assertEquals(true, awaitItem()?.customEventScripts)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
