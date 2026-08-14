package com.esde.companion.data.storage

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SelfHealingDirectoryWatcherTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private class RecordingWatcher : DirectoryWatcher {
        var startCount = 0
        var stopCount = 0

        override fun startWatching() {
            startCount++
        }

        override fun stopWatching() {
            stopCount++
        }
    }

    private fun watcherFor(
        targetPath: String,
        onFactoryInvoked: (RecordingWatcher) -> Unit = {},
    ): SelfHealingDirectoryWatcher =
        SelfHealingDirectoryWatcher(
            targetPath = targetPath,
            mask = 0,
            onChange = {},
            watcherFactory = { _, _, _ -> RecordingWatcher().also(onFactoryInvoked) },
        )

    @Test
    fun `startWatching does not construct a delegate when the parent directory does not exist`() {
        var factoryInvocations = 0
        val targetPath = File(tempFolder.root, "not-created-yet/es_log.txt").absolutePath

        val watcher = watcherFor(targetPath) { factoryInvocations++ }
        watcher.startWatching()

        assertEquals(0, factoryInvocations)
    }

    @Test
    fun `recheck arms a delegate once the parent directory appears`() {
        var factoryInvocations = 0
        var lastDelegate: RecordingWatcher? = null
        val parentDir = File(tempFolder.root, "sd-card")
        val targetPath = File(parentDir, "es_log.txt").absolutePath

        val watcher =
            watcherFor(targetPath) { delegate ->
                factoryInvocations++
                lastDelegate = delegate
            }
        watcher.startWatching()
        assertEquals(0, factoryInvocations)

        parentDir.mkdirs()
        watcher.recheck()

        assertEquals(1, factoryInvocations)
        assertEquals(1, lastDelegate?.startCount)
    }

    @Test
    fun `recheck without forceRearm is a no-op when already armed`() {
        var factoryInvocations = 0
        val targetPath = File(tempFolder.newFolder("existing"), "es_log.txt").absolutePath

        val watcher = watcherFor(targetPath) { factoryInvocations++ }
        watcher.startWatching()
        assertEquals(1, factoryInvocations)

        watcher.recheck()
        watcher.recheck()

        assertEquals(1, factoryInvocations)
    }

    @Test
    fun `recheck with forceRearm tears down and reconstructs an already-armed delegate`() {
        val delegates = mutableListOf<RecordingWatcher>()
        val targetPath = File(tempFolder.newFolder("existing"), "es_log.txt").absolutePath

        val watcher = watcherFor(targetPath) { delegates += it }
        watcher.startWatching()
        assertEquals(1, delegates.size)

        watcher.recheck(forceRearm = true)

        assertEquals(2, delegates.size)
        assertEquals(1, delegates[0].stopCount)
        assertEquals(1, delegates[1].startCount)
    }

    @Test
    fun `stopWatching then recheck does not rearm`() {
        var factoryInvocations = 0
        val targetPath = File(tempFolder.newFolder("existing"), "es_log.txt").absolutePath

        val watcher = watcherFor(targetPath) { factoryInvocations++ }
        watcher.startWatching()
        assertEquals(1, factoryInvocations)

        watcher.stopWatching()
        watcher.recheck(forceRearm = true)

        assertEquals(1, factoryInvocations)
    }
}
