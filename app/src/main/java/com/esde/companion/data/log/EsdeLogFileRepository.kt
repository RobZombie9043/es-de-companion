package com.esde.companion.data.log

import android.os.FileObserver
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.parser.EsdeEventParser
import com.esde.companion.domain.repository.EsdeLogRepository
import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tails es_log.txt using [FileObserver] for near-instant pickup of new writes, with a
 * slow interval poll running alongside it purely as a safety net.
 *
 * [FileObserver] is not fully trustworthy on its own - events can be missed after Doze,
 * and behavior varies across OEM skins - so it is treated as a latency optimization, not
 * the sole source of truth. The fallback poll (default 3s) guarantees we never stall
 * indefinitely even if every inotify event is dropped.
 *
 * We watch the *parent directory*, not the log file itself, and filter events by file
 * name. Watching the file directly ties the inotify watch to its current inode; if ES-DE
 * ever deletes-and-recreates es_log.txt on restart (rather than truncating it in place)
 * that watch goes dead silently. Watching the directory survives both truncate-in-place
 * and delete/recreate.
 *
 * On startup we deliberately do NOT replay the whole existing file as a burst of events -
 * that would make the reducer walk through every historical state in quick succession,
 * which is visible (and wrong) in the UI. Instead we scan once for the most recent
 * parseable event, emit only that, and start tailing from end-of-file from then on.
 *
 * Known limitation, left as-is deliberately rather than over-engineered up front:
 * [RandomAccessFile.readLine] decodes bytes as ISO-8859-1, not UTF-8. Game/system names
 * with non-Latin-1 characters may render incorrectly. Worth revisiting if it turns out
 * to matter in practice.
 */
class EsdeLogFileRepository(
    private val logFilePath: String,
    private val parser: EsdeEventParser = EsdeEventParser(),
    private val fallbackPollIntervalMs: Long = DEFAULT_FALLBACK_POLL_INTERVAL_MS,
) : EsdeLogRepository {

    override fun observeEvents(): Flow<EsdeEvent> = channelFlow {
        var position = 0L

        val startupFile = File(logFilePath)
        if (startupFile.exists()) {
            findLatestEvent(startupFile)?.let { send(it) }
            position = startupFile.length()
        }

        // Both the FileObserver callback and the fallback ticker just post "something may
        // have changed, go check" - CONFLATED because we always read from `position` to
        // current end-of-file regardless of how many signals coalesce into one wakeup.
        val checkSignal = Channel<Unit>(capacity = Channel.CONFLATED)

        val fileObserver = watchDirectoryFor(logFilePath) { checkSignal.trySend(Unit) }
        fileObserver.startWatching()

        val fallbackJob = launch {
            while (isActive) {
                delay(fallbackPollIntervalMs)
                checkSignal.trySend(Unit)
            }
        }

        try {
            for (signal in checkSignal) {
                val file = File(logFilePath)
                if (!file.exists()) continue

                val length = file.length()

                if (length < position) {
                    // Smaller than what we've already read: ES-DE restarted and wrote
                    // a fresh log. Start over from the beginning.
                    position = 0L
                }

                if (length > position) {
                    position = readNewLines(file, position) { event -> send(event) }
                }
            }
        } finally {
            fallbackJob.cancel()
            fileObserver.stopWatching()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Watches [targetPath]'s parent directory and invokes [onChange] for any create/write/
     * move event on an entry matching its file name. See class doc for why this watches
     * the directory rather than the file itself.
     */
    private fun watchDirectoryFor(targetPath: String, onChange: () -> Unit): FileObserver {
        val targetFile = File(targetPath)
        val parentDir = targetFile.parentFile ?: File("/")
        val targetName = targetFile.name
        val mask = FileObserver.MODIFY or
                FileObserver.CLOSE_WRITE or
                FileObserver.CREATE or
                FileObserver.MOVED_TO

        return object : FileObserver(parentDir, mask) {
            override fun onEvent(event: Int, path: String?) {
                if (path == null || path == targetName) onChange()
            }
        }
    }

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
        // Safety net only now, not the primary mechanism - FileObserver handles the
        // latency-sensitive path, so this can afford to be slow.
        const val DEFAULT_FALLBACK_POLL_INTERVAL_MS = 3_000L
    }
}