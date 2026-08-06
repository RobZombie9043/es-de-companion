package com.esde.companion.data.log

import android.os.FileObserver
import android.os.SystemClock
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.isStartupAnchor
import com.esde.companion.domain.model.withDirection
import com.esde.companion.domain.parser.EsdeEventParser
import com.esde.companion.domain.parser.NavigationDirectionTracker
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
 * That replay is also skipped entirely if the file predates the current boot (see
 * [bootTimeMillis]). A device reboot can auto-start this app before ES-DE has
 * (re)written es_log.txt this session, in which case any anchor found would be a
 * leftover from before the reboot - stale system/game info, not current truth.
 *

 * Lines are decoded as UTF-8, not via [RandomAccessFile.readLine] (which forces
 * ISO-8859-1 and mangles any multi-byte character - e.g. "NieR_Automata™" - into garbage
 * that then fails to match the real on-disk file name during media lookup). We read raw
 * bytes ourselves and decode with UTF-8 instead; this is safe to split into lines by
 * decoded-string line breaks because UTF-8 continuation/lead bytes never collide with
 * the ASCII '\n'/'\r' byte values used as line terminators.
 */
class EsdeLogFileRepository(
    private val logFilePath: String,
    private val parser: EsdeEventParser = EsdeEventParser(),
    private val fallbackPollIntervalMs: Long = DEFAULT_FALLBACK_POLL_INTERVAL_MS,
    private val bootTimeMillis: () -> Long = { System.currentTimeMillis() - SystemClock.elapsedRealtime() },
    private val watchDirectory: (targetPath: String, mask: Int, onChange: () -> Unit) -> DirectoryWatcher =
        ::FileObserverDirectoryWatcher,
) : EsdeLogRepository {

    override fun observeEvents(): Flow<EsdeEvent> = channelFlow {
        var position = 0L
        val directionTracker = NavigationDirectionTracker()

        val startupFile = File(logFilePath)
        if (startupFile.exists()) {
            // A device reboot can auto-start this app (see MainActivity's HOME intent
            // filter) before ES-DE has (re)written es_log.txt this boot. If the file
            // predates boot, any anchor in it is guaranteed to be from a previous
            // session - skip replay rather than surface stale system/game info.
            if (startupFile.lastModified() >= bootTimeMillis() - STALE_ANCHOR_GRACE_MS) {
                findEventsSinceLastAnchor(startupFile, directionTracker).forEach { send(it) }
            }
            position = startupFile.length()
        }

        // Both the FileObserver callback and the fallback ticker just post "something may
        // have changed, go check" - CONFLATED because we always read from `position` to
        // current end-of-file regardless of how many signals coalesce into one wakeup.
        val checkSignal = Channel<Unit>(capacity = Channel.CONFLATED)

        val fileObserver = watchDirectory(logFilePath, WRITE_EVENTS_MASK) { checkSignal.trySend(Unit) }
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
                    position = readNewLines(file, position, directionTracker) { event -> send(event) }
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
        val fileObserver = watchDirectory(logFilePath, EXISTENCE_EVENTS_MASK) { checkSignal.trySend(Unit) }
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
    private fun findEventsSinceLastAnchor(file: File, directionTracker: NavigationDirectionTracker): List<EsdeEvent> {
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

                // See class kdoc: decode as UTF-8, not ISO-8859-1.
                val text = String(buffer, Charsets.UTF_8)

                // startPos > 0 means the first line in this window may be a fragment cut
                // off mid-line by the window boundary, not a real line boundary - drop it
                // rather than risk parsing a truncated line.
                val lines = text.lineSequence().let { if (startPos > 0) it.drop(1) else it }
                val events = lines.mapNotNull { line ->
                    directionTracker.observeLine(line)
                    parser.parseLine(line)?.withDirection(directionTracker.direction)
                }.toList()

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
        directionTracker: NavigationDirectionTracker,
        onEvent: suspend (EsdeEvent) -> Unit,
    ): Long {
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(fromPosition)
            val remaining = ByteArray((raf.length() - fromPosition).toInt())
            raf.readFully(remaining)

            // Decode as UTF-8 ourselves rather than raf.readLine() - see class kdoc.
            val text = String(remaining, Charsets.UTF_8)
            for (line in text.lineSequence()) {
                directionTracker.observeLine(line)
                parser.parseLine(line)?.withDirection(directionTracker.direction)?.let { onEvent(it) }
            }
            return fromPosition + remaining.size
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

        // Jitter tolerance only, not a "how old is too old" staleness threshold - the
        // wall clock can be adjusted by a few seconds (e.g. NTP sync) right around
        // boot, which would otherwise make a genuinely fresh file look like it
        // predates bootTimeMillis()'s computed value.
        const val STALE_ANCHOR_GRACE_MS = 10_000L

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

/**
 * Seam for [EsdeLogFileRepository]'s directory-watching mechanism - injected the same way
 * as its `bootTimeMillis` parameter, so tests can substitute a no-op fake instead of
 * touching real [FileObserver]. [FileObserver]'s method bodies are stubbed to throw under
 * a plain JUnit unit test (no Robolectric, no Android runtime), so without this seam every
 * call to `observeEvents()`/`observeLogFileExists()` in a test races the producer
 * coroutine's real `Dispatchers.IO` thread against the test's own cancellation to reach
 * `startWatching()` before it throws - a genuine, observed flake, not a hypothetical one.
 */
interface DirectoryWatcher {
    fun startWatching()
    fun stopWatching()
}

/**
 * Watches [targetPath]'s parent directory and invokes [onChange] for any event in [mask]
 * on an entry matching its file name. See [EsdeLogFileRepository]'s class doc for why this
 * watches the directory rather than the file itself.
 */
private class FileObserverDirectoryWatcher(
    targetPath: String,
    mask: Int,
    onChange: () -> Unit,
) : DirectoryWatcher {
    private val targetFile = File(targetPath)
    private val observer = object : FileObserver(targetFile.parentFile ?: File("/"), mask) {
        override fun onEvent(event: Int, path: String?) {
            if (path == null || path == targetFile.name) onChange()
        }
    }

    override fun startWatching() = observer.startWatching()
    override fun stopWatching() = observer.stopWatching()
}
