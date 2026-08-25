package com.esde.companion.data.log

import app.cash.turbine.test
import com.esde.companion.data.storage.DirectoryWatcher
import com.esde.companion.data.storage.SelfHealConfig
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.NavigationDirection
import com.esde.companion.domain.state.AppStateReducer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.time.Duration.Companion.seconds

/**
 * [EsdeLogFileRepository.observeEvents]/[EsdeLogFileRepository.observeLogFileExists] both
 * `flowOn(Dispatchers.IO)` deliberately (see that class's kdoc) - real disk I/O on a real
 * thread, not virtual-time work `runTest`'s scheduler can fast-forward. Turbine's
 * `awaitItem()` timeout is therefore a genuine wall-clock budget for that real thread to
 * get scheduled and do real file I/O, which the default 3s occasionally missed under
 * heavy parallel Gradle test-worker contention (confirmed as the actual cause of an
 * intermittent "No value produced in 3s" failure, not a real bug in the repository) - a
 * generous fixed budget here, not a shorter one tuned to this machine's usual speed.
 */
private val FLAKE_RESISTANT_TIMEOUT = 10.seconds

class EsdeLogFileRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    /**
     * Defaults [bootTimeMillis] to well before any real file's mtime, so tests
     * unrelated to boot-staleness detection get the pre-existing "always replay"
     * behavior without needing to know about it. Real usage wires the actual
     * SystemClock-based default on [EsdeLogFileRepository] itself; tests here go
     * through this helper instead so they never touch that Android stub.
     *
     * Also injects a no-op [DirectoryWatcher] rather than the real
     * [FileObserver]-backed default - [FileObserver]'s methods are stubbed to throw
     * under plain JUnit (no Robolectric), and observeEvents()/observeLogFileExists()
     * construct one unconditionally, so without this every test would be racing its own
     * cancellation against the producer coroutine's real Dispatchers.IO thread reaching
     * `startWatching()` first. That race was real, not hypothetical - it surfaced as an
     * intermittent failure in "startup skips replay when the file predates the current
     * boot" before this seam existed.
     */
    private fun repositoryFor(
        logFile: File,
        bootTimeMillis: () -> Long = { 0L },
    ): EsdeLogFileRepository =
        EsdeLogFileRepository(
            logFilePath = logFile.absolutePath,
            bootTimeMillis = bootTimeMillis,
            watchDirectory = { _, _, _ -> NoOpDirectoryWatcher },
        )

    private object NoOpDirectoryWatcher : DirectoryWatcher {
        override fun startWatching() = Unit

        override fun stopWatching() = Unit
    }

    /** Lets a test fire a simulated FileObserver notification on demand. */
    private class CapturingDirectoryWatcher(private val onChange: () -> Unit) : DirectoryWatcher {
        override fun startWatching() = Unit

        override fun stopWatching() = Unit

        fun fireChange() = onChange()
    }

    /**
     * Records [recheck] calls so a test can assert self-heal rearm behavior. Never fires a
     * simulated FileObserver notification of its own - see [CapturingDirectoryWatcher] for
     * that - so its constructor's `onChange` is intentionally unused, only present to match
     * the `watchDirectory` factory shape.
     */
    private class RecordingDirectoryWatcher(
        @Suppress("UNUSED_PARAMETER") onChange: () -> Unit,
    ) : DirectoryWatcher {
        var rechecksForced = 0
        var rechecksNotForced = 0

        override fun startWatching() = Unit

        override fun stopWatching() = Unit

        override fun recheck(forceRearm: Boolean) {
            if (forceRearm) rechecksForced++ else rechecksNotForced++
        }
    }

    @Test
    fun `startup replays from the last anchor event so screensaver-end resolves to the real prior state`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            logFile.writeText(
                """
                Jul 28 15:16:07 Debug:  Scripting::fireEvent(): system-select "psx" "Sony PlayStation" "/storage/E2AB-E84A/ROMs/psx" ""
                Jul 28 15:16:08 Debug:  Scripting::fireEvent(): screensaver-start "manual" "" "" ""
                Jul 28 15:16:08 Debug:  Scripting::fireEvent(): screensaver-game-select "/storage/E2AB-E84A/ROMs/gb/Yoshi's Cookie (USA, Europe).zip" "Yoshi's Cookie" "gb" "Nintendo Game Boy"
                Jul 28 15:16:10 Debug:  Scripting::fireEvent(): screensaver-end "cancel" "" "" ""
                """.trimIndent(),
            )

            val repository = repositoryFor(logFile)

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                val events = mutableListOf<EsdeEvent>()
                repeat(4) { events += awaitItem() }
                cancelAndIgnoreRemainingEvents()

                val finalState =
                    events.fold(AppState.Idle as AppState) { state, event ->
                        AppStateReducer.reduce(state, event)
                    }
                assertEquals(
                    AppState.BrowsingSystem("psx", "Sony PlayStation", "/storage/E2AB-E84A/ROMs/psx"),
                    finalState,
                )
            }
        }

    @Test
    fun `startup skips replay when the file predates the current boot`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            logFile.writeText(
                "Jul 28 15:16:07 Debug:  Scripting::fireEvent(): system-select \"psx\" \"Sony PlayStation\" " +
                    "\"/storage/E2AB-E84A/ROMs/psx\" \"\"\n",
            )

            // Simulates a reboot: the file's real mtime is "now" (test-run time), but boot
            // is reported as happening well after that - i.e. the file hasn't been touched
            // since boot, so its anchor must be a leftover from before the reboot.
            val repository = repositoryFor(logFile, bootTimeMillis = { System.currentTimeMillis() + 60_000L })

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `startup replays normally when the file was written after boot`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            logFile.writeText(
                "Jul 28 15:16:07 Debug:  Scripting::fireEvent(): system-select \"psx\" \"Sony PlayStation\" " +
                    "\"/storage/E2AB-E84A/ROMs/psx\" \"\"\n",
            )

            // Boot reported as happening well before the file's real (test-run time) mtime -
            // i.e. the file was written after boot, so its anchor is trustworthy.
            val repository = repositoryFor(logFile, bootTimeMillis = { System.currentTimeMillis() - 60_000L })

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                assertEquals(
                    EsdeEvent.SystemSelect("psx", "Sony PlayStation", "/storage/E2AB-E84A/ROMs/psx"),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `startup emits nothing when the file has no anchor event at all`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            logFile.writeText(
                "Jul 28 15:16:08 Debug:  Scripting::fireEvent(): screensaver-start \"manual\" \"\" \"\" \"\"\n",
            )

            val repository = repositoryFor(logFile)

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `startup replays a quit anchor so a game left running before ES-DE exited resolves to Idle`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            logFile.writeText(
                """
                Jul 28 15:16:07 Debug:  Scripting::fireEvent(): game-start "/roms/dc/game.chd" "Game" "dreamcast" "Sega Dreamcast"
                Aug 04 14:52:13 Debug:  Scripting::fireEvent(): quit "" "" "" ""
                Aug 04 14:52:13 Info:   ES-DE cleanly shutting down
                """.trimIndent(),
            )

            val repository = repositoryFor(logFile)

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                assertEquals(EsdeEvent.Quit, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `startup replays as an anchor so a game left running before an ungraceful restart resolves to Idle`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            logFile.writeText(
                """
                Jul 28 15:16:07 Debug:  Scripting::fireEvent(): game-start "/roms/dc/game.chd" "Game" "dreamcast" "Sega Dreamcast"
                Aug 05 17:13:32 Debug:  Scripting::fireEvent(): startup "" "" "" ""
                """.trimIndent(),
            )

            val repository = repositoryFor(logFile)

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                assertEquals(EsdeEvent.Startup, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the replayed navigation event carries the direction of its preceding controller press`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            logFile.writeText(
                """
                Aug 02 13:31:34 Debug:  Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right, value=1
                Aug 02 13:31:34 Debug:  Scripting::fireEvent(): game-select "/storage/E2AB-E84A/ROMs/mastersystem/Dragon Crystal (Europe, Brazil) (En).zip" "Dragon Crystal" "mastersystem" "Sega Master System"
                Aug 02 13:31:34 Debug:  Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right, value=0
                """.trimIndent(),
            )

            val repository = repositoryFor(logFile)

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                assertEquals(
                    EsdeEvent.GameSelect(
                        romPath = "/storage/E2AB-E84A/ROMs/mastersystem/Dragon Crystal (Europe, Brazil) (En).zip",
                        gameName = "Dragon Crystal",
                        systemShortName = "mastersystem",
                        systemFullName = "Sega Master System",
                        direction = NavigationDirection.Right,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a non-directional button press clears a prior direction, reproducing the real log excerpt's b-triggered system-select`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            logFile.writeText(
                """
                Aug 02 13:31:34 Debug:  Window::logInput(Xbox Wireless Controller): Button 11, isMappedTo=up, value=1
                Aug 02 13:31:34 Debug:  Scripting::fireEvent(): game-select "/storage/E2AB-E84A/ROMs/mastersystem/Buggy Run (Europe, Brazil) (En).zip" "Buggy Run" "mastersystem" "Sega Master System"
                Aug 02 13:31:34 Debug:  Window::logInput(Xbox Wireless Controller): Button 11, isMappedTo=up, value=0
                Aug 02 13:31:34 Debug:  Window::logInput(Xbox Wireless Controller): Button 1, isMappedTo=b, value=1
                Aug 02 13:31:34 Debug:  Scripting::fireEvent(): system-select "mastersystem" "Sega Master System" "/storage/E2AB-E84A/ROMs/mastersystem" ""
                Aug 02 13:31:35 Debug:  Window::logInput(Xbox Wireless Controller): Button 1, isMappedTo=b, value=0
                """.trimIndent(),
            )

            val repository = repositoryFor(logFile)

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                assertEquals(
                    EsdeEvent.SystemSelect(
                        systemShortName = "mastersystem",
                        systemFullName = "Sega Master System",
                        systemPath = "/storage/E2AB-E84A/ROMs/mastersystem",
                        direction = null,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `startup finds an anchor beyond the initial tail window`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            val padding = "Debug: some unrelated line\n".repeat(6_000) // ~168KB, past the 64KB initial window
            val anchorLine =
                "Jul 28 07:01:30 Debug:  Scripting::fireEvent(): " +
                    "system-select \"dreamcast\" \"Sega Dreamcast\" \"/roms/dreamcast\" \"\""
            logFile.writeText(padding + anchorLine + "\n")

            val repository = repositoryFor(logFile)

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                assertEquals(
                    EsdeEvent.SystemSelect("dreamcast", "Sega Dreamcast", "/roms/dreamcast"),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a rom path with a multi-byte UTF-8 character decodes correctly instead of as ISO-8859-1 mojibake`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            logFile.writeText(
                "Aug 03 10:00:00 Debug:  Scripting::fireEvent(): game-select " +
                    "\"/storage/E2AB-E84A/ROMs/steam/NieR_Automata™.steam\" \"NieR_Automata™\" \"steam\" \"Steam\"\n",
            )

            val repository = repositoryFor(logFile)

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                assertEquals(
                    EsdeEvent.GameSelect(
                        romPath = "/storage/E2AB-E84A/ROMs/steam/NieR_Automata™.steam",
                        gameName = "NieR_Automata™",
                        systemShortName = "steam",
                        systemFullName = "Steam",
                        direction = null,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a new line picked up only by the fallback poll invokes onFallbackPollCaughtUpdate`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            logFile.writeText(
                "Jul 28 15:16:07 Debug:  Scripting::fireEvent(): " +
                    "system-select \"psx\" \"Sony PlayStation\" \"/storage/E2AB-E84A/ROMs/psx\" \"\"\n",
            )

            // NoOpDirectoryWatcher never invokes its onChange callback, so the only signal
            // that can ever pick up the appended line below is a manual fallbackTicker tick -
            // deterministic, unlike depending on a real delay() racing Turbine's await
            // timeout (this used to pass fallbackPollIntervalMs = 20L and rely on real time,
            // which was flaky under load - see EsdeLogFileRepository's fallbackTicker kdoc).
            var fallbackCatchCount = 0
            val fallbackTicker = MutableSharedFlow<Unit>()
            val repository =
                EsdeLogFileRepository(
                    logFilePath = logFile.absolutePath,
                    bootTimeMillis = { 0L },
                    watchDirectory = { _, _, _ -> NoOpDirectoryWatcher },
                    selfHeal = SelfHealConfig(onFallbackPollCaughtUpdate = { fallbackCatchCount++ }),
                    fallbackTicker = fallbackTicker,
                )

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                assertEquals(
                    EsdeEvent.SystemSelect("psx", "Sony PlayStation", "/storage/E2AB-E84A/ROMs/psx"),
                    awaitItem(),
                )
                assertEquals(0, fallbackCatchCount)

                // See the storage-changed test's matching comment - wait for the producer's
                // ticker collector to subscribe before emitting, or the tick can be silently
                // dropped (MutableSharedFlow has no buffer for a replay-0 flow with no
                // collectors).
                fallbackTicker.subscriptionCount.first { it > 0 }

                logFile.appendText(
                    "Jul 28 15:16:10 Debug:  Scripting::fireEvent(): " +
                        "system-select \"gc\" \"Nintendo GameCube\" \"/roms/gc\" \"\"\n",
                )
                fallbackTicker.emit(Unit)

                assertEquals(EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, fallbackCatchCount)
        }

    @Test
    fun `a new line picked up by FileObserver does not invoke onFallbackPollCaughtUpdate`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            logFile.writeText(
                "Jul 28 15:16:07 Debug:  Scripting::fireEvent(): " +
                    "system-select \"psx\" \"Sony PlayStation\" \"/storage/E2AB-E84A/ROMs/psx\" \"\"\n",
            )

            var fallbackCatchCount = 0
            // A CompletableDeferred, not a lateinit var: watchDirectory() runs from the
            // producer coroutine on a real Dispatchers.IO thread, asynchronously with
            // respect to this test coroutine, so there's no guarantee it has run yet the
            // instant the first awaitItem() below returns - awaiting the deferred is what
            // makes fireChange() below deterministic rather than an
            // UninitializedPropertyAccessException race.
            val watcherReady = CompletableDeferred<CapturingDirectoryWatcher>()
            val repository =
                EsdeLogFileRepository(
                    logFilePath = logFile.absolutePath,
                    // Never fires - the only signal reaching checkSignal is the manual
                    // fireChange() below, standing in for a real FileObserver notification.
                    fallbackTicker = emptyFlow(),
                    bootTimeMillis = { 0L },
                    watchDirectory = { _, _, onChange ->
                        CapturingDirectoryWatcher(onChange).also { watcherReady.complete(it) }
                    },
                    selfHeal = SelfHealConfig(onFallbackPollCaughtUpdate = { fallbackCatchCount++ }),
                )

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                assertEquals(
                    EsdeEvent.SystemSelect("psx", "Sony PlayStation", "/storage/E2AB-E84A/ROMs/psx"),
                    awaitItem(),
                )

                logFile.appendText(
                    "Jul 28 15:16:10 Debug:  Scripting::fireEvent(): " +
                        "system-select \"gc\" \"Nintendo GameCube\" \"/roms/gc\" \"\"\n",
                )
                watcherReady.await().fireChange()

                assertEquals(EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(0, fallbackCatchCount)
        }

    @Test
    fun `a fallback-caught update forces the directory watcher to rearm`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            logFile.writeText(
                "Jul 28 15:16:07 Debug:  Scripting::fireEvent(): " +
                    "system-select \"psx\" \"Sony PlayStation\" \"/storage/E2AB-E84A/ROMs/psx\" \"\"\n",
            )

            lateinit var capturedWatcher: RecordingDirectoryWatcher
            val fallbackTicker = MutableSharedFlow<Unit>()
            val repository =
                EsdeLogFileRepository(
                    logFilePath = logFile.absolutePath,
                    bootTimeMillis = { 0L },
                    watchDirectory = { _, _, onChange ->
                        RecordingDirectoryWatcher(onChange).also { capturedWatcher = it }
                    },
                    fallbackTicker = fallbackTicker,
                )

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                // Not asserted here: capturedWatcher is assigned by the producer coroutine
                // (running on Dispatchers.IO, a real thread) after this first emission, so
                // there's no guarantee it has run yet the instant awaitItem() returns -
                // asserting on it only after the second item is what's actually deterministic.
                awaitItem()

                fallbackTicker.subscriptionCount.first { it > 0 }

                logFile.appendText(
                    "Jul 28 15:16:10 Debug:  Scripting::fireEvent(): " +
                        "system-select \"gc\" \"Nintendo GameCube\" \"/roms/gc\" \"\"\n",
                )
                fallbackTicker.emit(Unit)

                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, capturedWatcher.rechecksForced)
        }

    @Test
    fun `a storage-changed signal triggers an immediate reread without waiting for the fallback interval`() =
        runTest {
            val logFile = tempFolder.newFile("es_log.txt")
            logFile.writeText(
                "Jul 28 15:16:07 Debug:  Scripting::fireEvent(): " +
                    "system-select \"psx\" \"Sony PlayStation\" \"/storage/E2AB-E84A/ROMs/psx\" \"\"\n",
            )

            lateinit var capturedWatcher: RecordingDirectoryWatcher
            val storageEvents = MutableSharedFlow<Unit>()
            val repository =
                EsdeLogFileRepository(
                    logFilePath = logFile.absolutePath,
                    // Never fires - proves the storage-events signal, not the ticker, drove
                    // the reread.
                    fallbackTicker = emptyFlow(),
                    bootTimeMillis = { 0L },
                    watchDirectory = { _, _, onChange ->
                        RecordingDirectoryWatcher(onChange).also { capturedWatcher = it }
                    },
                    selfHeal = SelfHealConfig(storageEvents = storageEvents),
                )

            repository.observeEvents().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                awaitItem()

                // The storage-events collector subscribes from the producer coroutine on
                // a real Dispatchers.IO thread, asynchronously with respect to this test
                // coroutine - emitting before it has actually subscribed silently drops
                // the value (MutableSharedFlow has no buffer for a replay-0 flow with no
                // collectors), so wait for the subscription to land first.
                storageEvents.subscriptionCount.first { it > 0 }

                logFile.appendText(
                    "Jul 28 15:16:10 Debug:  Scripting::fireEvent(): " +
                        "system-select \"gc\" \"Nintendo GameCube\" \"/roms/gc\" \"\"\n",
                )
                storageEvents.emit(Unit)

                assertEquals(EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, capturedWatcher.rechecksForced)
        }

    @Test
    fun `observeLogFileExists rearms the directory watcher when the fallback poll catches an existence flip`() =
        runTest {
            val logFile = File(tempFolder.root, "es_log.txt")

            lateinit var capturedWatcher: RecordingDirectoryWatcher
            val fallbackTicker = MutableSharedFlow<Unit>()
            val repository =
                EsdeLogFileRepository(
                    logFilePath = logFile.absolutePath,
                    bootTimeMillis = { 0L },
                    watchDirectory = { _, _, onChange ->
                        RecordingDirectoryWatcher(onChange).also { capturedWatcher = it }
                    },
                    fallbackTicker = fallbackTicker,
                )

            repository.observeLogFileExists().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                // Not asserted here: capturedWatcher is assigned by the producer coroutine
                // (running on Dispatchers.IO, a real thread) after this first emission, so
                // there's no guarantee it has run yet the instant awaitItem() returns -
                // asserting on it only after the second item is what's actually deterministic.
                assertEquals(false, awaitItem())

                fallbackTicker.subscriptionCount.first { it > 0 }

                logFile.writeText("Jul 28 15:16:07 Debug:  Scripting::fireEvent(): startup \"\" \"\" \"\" \"\"\n")
                fallbackTicker.emit(Unit)

                assertEquals(true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, capturedWatcher.rechecksForced)
        }

    @Test
    fun `observeLogFileExists reacts to a storage-changed signal without waiting for the fallback interval`() =
        runTest {
            val logFile = File(tempFolder.root, "es_log.txt")

            val storageEvents = MutableSharedFlow<Unit>()
            val repository =
                EsdeLogFileRepository(
                    logFilePath = logFile.absolutePath,
                    fallbackTicker = emptyFlow(),
                    bootTimeMillis = { 0L },
                    watchDirectory = { _, _, _ -> NoOpDirectoryWatcher },
                    selfHeal = SelfHealConfig(storageEvents = storageEvents),
                )

            repository.observeLogFileExists().test(timeout = FLAKE_RESISTANT_TIMEOUT) {
                assertEquals(false, awaitItem())

                // See the matching comment in the observeEvents() storage-changed test
                // above - without this wait, emit() can race the producer coroutine's
                // subscription and silently drop the signal.
                storageEvents.subscriptionCount.first { it > 0 }

                logFile.writeText("Jul 28 15:16:07 Debug:  Scripting::fireEvent(): startup \"\" \"\" \"\" \"\"\n")
                storageEvents.emit(Unit)

                assertEquals(true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
