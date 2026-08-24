package com.esde.companion.data.settings

import android.os.FileObserver
import com.esde.companion.data.storage.DirectoryWatcher
import com.esde.companion.data.storage.SelfHealingDirectoryWatcher
import com.esde.companion.domain.model.EsdeEventScriptSettings
import com.esde.companion.domain.parser.EsdeSettingsParser
import com.esde.companion.domain.repository.EsdeInstallationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads ES-DE's own settings/es_settings.xml and checks for the legacy
 * esdecompanion-*.sh script files the old app used to write into ES-DE's scripts/ folder
 * (see CLAUDE.md - that mechanism is gone, these files are pure leftover clutter now).
 */
class FileEsdeInstallationRepository(
    private val fallbackPollIntervalMs: Long = DEFAULT_FALLBACK_POLL_INTERVAL_MS,
    private val watchDirectory: (targetPath: String, mask: Int, onChange: () -> Unit) -> DirectoryWatcher =
        { path, mask, onChange -> SelfHealingDirectoryWatcher(path, mask, onChange) },
    // Emissions mean "storage mount state may have changed, re-evaluate" - see
    // StorageMountEvents/EsdeLogFileRepository's matching parameter.
    private val storageEvents: Flow<Unit> = emptyFlow(),
) : EsdeInstallationRepository {
    override suspend fun readMediaDirectory(esdeRootPath: String): String? =
        withContext(Dispatchers.IO) {
            readSettingsXml(esdeRootPath)?.let { xml ->
                EsdeSettingsParser.findStringValue(xml, "MediaDirectory")
            }
        }

    override suspend fun readEventScriptSettings(esdeRootPath: String): EsdeEventScriptSettings? =
        withContext(Dispatchers.IO) {
            parseEventScriptSettings(esdeRootPath)
        }

    /**
     * Watches settings/es_settings.xml's parent directory (same "watch the directory, not
     * the file" reasoning as [com.esde.companion.data.log.EsdeLogFileRepository] - ES-DE
     * may rewrite the file rather than edit it in place) with a slow poll running
     * alongside as a safety net, the same combination used there.
     */
    override fun observeEventScriptSettings(esdeRootPath: String): Flow<EsdeEventScriptSettings?> =
        channelFlow {
            var lastKnown = parseEventScriptSettings(esdeRootPath)
            send(lastKnown)

            val checkSignal = Channel<CheckTrigger>(capacity = Channel.CONFLATED)

            val settingsFile = File(esdeRootPath, SETTINGS_XML_RELATIVE_PATH)
            val fileObserver =
                watchDirectory(settingsFile.absolutePath, WRITE_EVENTS_MASK) {
                    checkSignal.trySend(CheckTrigger.Observer)
                }
            fileObserver.startWatching()

            val fallbackJob =
                launch {
                    while (isActive) {
                        delay(fallbackPollIntervalMs)
                        checkSignal.trySend(CheckTrigger.Fallback)
                    }
                }

            val storageEventsJob =
                launch {
                    storageEvents.collect {
                        fileObserver.recheck(forceRearm = true)
                        checkSignal.trySend(CheckTrigger.StorageChanged)
                    }
                }

            try {
                for (trigger in checkSignal) {
                    val current = parseEventScriptSettings(esdeRootPath)
                    if (trigger == CheckTrigger.Fallback && current != lastKnown) {
                        // The fallback poll caught a real change the directory watcher
                        // should have caught but didn't - direct proof it's gone stale.
                        fileObserver.recheck(forceRearm = true)
                    }
                    lastKnown = current
                    send(current)
                }
            } finally {
                storageEventsJob.cancel()
                fallbackJob.cancel()
                fileObserver.stopWatching()
            }
        }.flowOn(Dispatchers.IO)

    private fun parseEventScriptSettings(esdeRootPath: String): EsdeEventScriptSettings? =
        readSettingsXml(esdeRootPath)?.let { xml ->
            EsdeEventScriptSettings(
                customEventScripts = EsdeSettingsParser.findBoolValue(xml, "CustomEventScripts") ?: false,
                customEventScriptsBrowsing =
                    EsdeSettingsParser.findBoolValue(xml, "CustomEventScriptsBrowsing") ?: false,
                debugMode = EsdeSettingsParser.findBoolValue(xml, "DebugMode") ?: false,
                debugSkipInputLogging = EsdeSettingsParser.findBoolValue(xml, "DebugSkipInputLogging") ?: false,
            )
        }

    override suspend fun findLegacyScriptFiles(esdeRootPath: String): List<String> =
        withContext(Dispatchers.IO) {
            LEGACY_SCRIPT_RELATIVE_PATHS
                .map { File(esdeRootPath, it) }
                .filter { it.isFile }
                .map { it.absolutePath }
        }

    override suspend fun deleteLegacyScriptFiles(esdeRootPath: String) {
        withContext(Dispatchers.IO) {
            LEGACY_SCRIPT_RELATIVE_PATHS.forEach { File(esdeRootPath, it).delete() }
        }
    }

    private fun readSettingsXml(esdeRootPath: String): String? {
        val file = File(esdeRootPath, SETTINGS_XML_RELATIVE_PATH)
        return if (file.isFile) file.readText() else null
    }

    private companion object {
        const val SETTINGS_XML_RELATIVE_PATH = "settings/es_settings.xml"

        // Safety net only - FileObserver handles the latency-sensitive path, same
        // reasoning as EsdeLogFileRepository's fallback poll.
        const val DEFAULT_FALLBACK_POLL_INTERVAL_MS = 3_000L

        val WRITE_EVENTS_MASK =
            FileObserver.MODIFY or
                FileObserver.CLOSE_WRITE or
                FileObserver.CREATE or
                FileObserver.MOVED_TO

        val LEGACY_SCRIPT_RELATIVE_PATHS =
            listOf(
                "scripts/game-end/esdecompanion-game-end.sh",
                "scripts/game-start/esdecompanion-game-start.sh",
                "scripts/game-select/esdecompanion-game-select.sh",
                "scripts/screensaver-end/esdecompanion-screensaver-end.sh",
                "scripts/screensaver-game-select/esdecompanion-screensaver-game-select.sh",
                "scripts/screensaver-start/esdecompanion-screensaver-start.sh",
                "scripts/system-select/esdecompanion-system-select.sh",
            )
    }
}

/**
 * Which signal source woke up [FileEsdeInstallationRepository.observeEventScriptSettings]'s
 * check loop - see [com.esde.companion.data.log.EsdeLogFileRepository]'s matching type for
 * why [StorageChanged] is kept distinct from [Fallback].
 */
private enum class CheckTrigger { Observer, Fallback, StorageChanged }
