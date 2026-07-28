package com.esde.companion.data.log

import android.os.FileObserver
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.isStartupAnchor
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
            findEventsSinceLastAnchor(startupFile).forEach { send(it) }
            position = startupFile.length()
        }

        // Both the FileObserver callback and the fallback ticker just post "something may
        // have changed, go check" - CONFLATED because we always read from `position` to
        // current end-of-file regardless of how many signals coalesce into one wakeup.
        val checkSignal = Channel<Unit>(capacity = Channel.CONFLATED)

        val fileObserver = watchDirectoryFor(logFilePath, WRITE_EVENTS_MASK) { checkSignal.trySend(Unit) }
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
     * Whether es_log.txt currently exists. Runs its own independent FileObserver/poll
     * loop rather than sharing state with [observeEvents] - the two are collected
     * independently (see ObserveConnectionStateUseCase), and existence-tracking has no
     * need for read position, so keeping it separate is simpler than threading existence
     * state through the tailing loop above.
     */
    override fun observeLogFileExists(): Flow<Boolean> = channelFlow {
        var lastKnown = File(logFilePath).exists()
        send(lastKnown)

        val checkSignal = Channel<Unit>(capacity = Channel.CONFLATED)
        val fileObserver = watchDirectoryFor(logFilePath, EXISTENCE_EVENTS_MASK) { checkSignal.trySend(Unit) }
        fileObserver.startWatching()

        val fallbackJob = launch {
            while (isActive) {
                delay(fallbackPollIntervalMs)
                checkSignal.trySend(Unit)
            }
        }

        try {
            for (signal in checkSignal) {
                val exists = File(logFilePath).exists()
                if (exists != lastKnown) {
                    lastKnown = exists
                    send(exists)
                }
            }
        } finally {
            fallbackJob.cancel()
            fileObserver.stopWatching()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Watches [targetPath]'s parent directory and invokes [onChange] for any event in
     * [mask] on an entry matching its file name. See class doc for why this watches the
     * directory rather than the file itself.
     */
    private fun watchDirectoryFor(targetPath: String, mask: Int, onChange: () -> Unit): FileObserver {
        val targetFile = File(targetPath)
        val parentDir = targetFile.parentFile ?: File("/")
        val targetName = targetFile.name

        return object : FileObserver(parentDir, mask) {
            override fun onEvent(event: Int, path: String?) {
                if (path == null || path == targetName) onChange()
            }
        }
    }

    /**
     * Finds the most recent "anchor" event (see [isStartupAnchor]) and returns it plus
     * every event after it, in original order. Replaying just the anchor itself and
     * relying on it being self-contained would be enough on its own, but returning the
     * full suffix means later events (e.g. a screensaver session that has since ended)
     * fold through AppStateReducer exactly as they would have live, restoring
     * previousState/mode correctly instead of losing that context.
     *
     * Reads backward from the end of the file in growing windows for the same reason
     * described on [EsdeLogFileRepository] generally - a full linear scan from byte 0 is
     * slow for a log that's grown over a real session. Widens until an anchor is found or
     * the whole file has been read; if no anchor exists anywhere in the file, returns an
     * empty list and startup correctly falls back to [AppState.Idle].
     */
    private fun findEventsSinceLastAnchor(file: File): List<EsdeEvent> {
        val fileLength = file.length()
        if (fileLength == 0L) return emptyList()

        RandomAccessFile(file, "r").use { raf ->
            var windowSize = INITIAL_TAIL_WINDOW_BYTES
            while (true) {
                val startPos = maxOf(0L, fileLength - windowSize)
                val bytesToRead = (fileLength - startPos).toInt()

                raf.seek(startPos)
                val buffer = ByteArray(bytesToRead)
                raf.readFully(buffer)

                // Same encoding assumption noted on the class kdoc.
                val text = String(buffer, Charsets.ISO_8859_1)

                // startPos > 0 means the first line in this window may be a fragment cut
                // off mid-line by the window boundary, not a real line boundary - drop it
                // rather than risk parsing a truncated line.
                val lines = text.lineSequence().let { if (startPos > 0) it.drop(1) else it }
                val events = lines.mapNotNull { parser.parseLine(it) }.toList()

                val anchorIndex = events.indexOfLast { it.isStartupAnchor() }
                if (anchorIndex != -1) return events.subList(anchorIndex, events.size)
                if (startPos == 0L) return emptyList()

                windowSize *= TAIL_WINDOW_GROWTH_FACTOR
            }
        }
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

        // Tail-window read for findLatestEvent(): 64KB comfortably covers "last event
        // fired a while ago" for realistic ES-DE session logs; doubles on retry and
        // ultimately falls back to the whole file, so correctness never depends on
        // this guess being right, only speed does.
        const val INITIAL_TAIL_WINDOW_BYTES = 64L * 1024
        const val TAIL_WINDOW_GROWTH_FACTOR = 4L

        val WRITE_EVENTS_MASK = FileObserver.MODIFY or
                FileObserver.CLOSE_WRITE or
                FileObserver.CREATE or
                FileObserver.MOVED_TO

        val EXISTENCE_EVENTS_MASK = FileObserver.CREATE or
                FileObserver.DELETE or
                FileObserver.MOVED_TO or
                FileObserver.MOVED_FROM
    }
}