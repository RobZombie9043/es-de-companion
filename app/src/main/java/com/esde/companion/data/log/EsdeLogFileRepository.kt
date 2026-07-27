package com.esde.companion.data.log

import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.parser.EsdeEventParser
import com.esde.companion.domain.repository.EsdeLogRepository
import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/**
 * Tails es_log.txt by polling for size changes and reading only newly appended bytes.
 *
 * On startup we deliberately do NOT replay the whole existing file as a burst of events -
 * that would make the reducer walk through every historical state in quick succession,
 * which is visible (and wrong) in the UI. Instead we scan once for the most recent
 * parseable event, emit only that, and start tailing from end-of-file from then on.
 *
 * ES-DE writes a fresh log on every restart (truncated/recreated, not appended forever) -
 * this is handled below by detecting a file size smaller than our last-read position and
 * resetting to the start.
 *
 * Known limitation, left as-is deliberately rather than over-engineered up front:
 * [RandomAccessFile.readLine] decodes bytes as ISO-8859-1, not UTF-8. Game/system names
 * with non-Latin-1 characters may render incorrectly. Worth revisiting if it turns out
 * to matter in practice.
 */
class EsdeLogFileRepository(
    private val logFilePath: String,
    private val parser: EsdeEventParser = EsdeEventParser(),
    private val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
) : EsdeLogRepository {

    override fun observeEvents(): Flow<EsdeEvent> = flow {
        var position = 0L

        val startupFile = File(logFilePath)
        if (startupFile.exists()) {
            findLatestEvent(startupFile)?.let { emit(it) }
            position = startupFile.length()
        }

        while (currentCoroutineContext().isActive) {
            val file = File(logFilePath)

            if (file.exists()) {
                val length = file.length()

                if (length < position) {
                    // Smaller than what we've already read: ES-DE restarted and wrote
                    // a fresh log. Start over from the beginning.
                    position = 0L
                }

                if (length > position) {
                    position = readNewLines(file, position) { event -> emit(event) }
                }
            }

            delay(pollIntervalMs)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Reads the file from the start purely to find the most recently fired event, without
     * emitting every event along the way. Used only once, at startup, so the UI's initial
     * state reflects reality immediately instead of replaying history.
     */
    private fun findLatestEvent(file: File): EsdeEvent? {
        var latest: EsdeEvent? = null
        RandomAccessFile(file, "r").use { raf ->
            var line = raf.readLine()
            while (line != null) {
                parser.parseLine(line)?.let { latest = it }
                line = raf.readLine()
            }
        }
        return latest
    }

    private suspend fun readNewLines(
        file: File,
        fromPosition: Long,
        onEvent: suspend (EsdeEvent) -> Unit,
    ): Long {
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(fromPosition)
            var line = raf.readLine()
            while (line != null) {
                parser.parseLine(line)?.let { onEvent(it) }
                line = raf.readLine()
            }
            return raf.filePointer
        }
    }

    private companion object {
        const val DEFAULT_POLL_INTERVAL_MS = 1_000L
    }
}