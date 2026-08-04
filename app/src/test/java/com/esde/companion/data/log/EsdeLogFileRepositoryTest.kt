package com.esde.companion.data.log

import app.cash.turbine.test
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.NavigationDirection
import com.esde.companion.domain.state.AppStateReducer
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EsdeLogFileRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    /**
     * Defaults [bootTimeMillis] to well before any real file's mtime, so tests
     * unrelated to boot-staleness detection get the pre-existing "always replay"
     * behavior without needing to know about it. Real usage wires the actual
     * SystemClock-based default on [EsdeLogFileRepository] itself; tests here go
     * through this helper instead so they never touch that Android stub.
     */
    private fun repositoryFor(logFile: File, bootTimeMillis: () -> Long = { 0L }): EsdeLogFileRepository =
        EsdeLogFileRepository(logFilePath = logFile.absolutePath, bootTimeMillis = bootTimeMillis)

    @Test
    fun `startup replays from the last anchor event so screensaver-end resolves to the real prior state`() = runTest {
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

        repository.observeEvents().test {
            val events = mutableListOf<EsdeEvent>()
            repeat(4) { events += awaitItem() }
            cancelAndIgnoreRemainingEvents()

            val finalState = events.fold(AppState.Idle as AppState) { state, event ->
                AppStateReducer.reduce(state, event)
            }
            assertEquals(
                AppState.BrowsingSystem("psx", "Sony PlayStation", "/storage/E2AB-E84A/ROMs/psx"),
                finalState,
            )
        }
    }

    @Test
    fun `startup skips replay when the file predates the current boot`() = runTest {
        val logFile = tempFolder.newFile("es_log.txt")
        logFile.writeText(
            "Jul 28 15:16:07 Debug:  Scripting::fireEvent(): system-select \"psx\" \"Sony PlayStation\" \"/storage/E2AB-E84A/ROMs/psx\" \"\"\n",
        )

        // Simulates a reboot: the file's real mtime is "now" (test-run time), but boot
        // is reported as happening well after that - i.e. the file hasn't been touched
        // since boot, so its anchor must be a leftover from before the reboot.
        val repository = repositoryFor(logFile, bootTimeMillis = { System.currentTimeMillis() + 60_000L })

        repository.observeEvents().test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startup replays normally when the file was written after boot`() = runTest {
        val logFile = tempFolder.newFile("es_log.txt")
        logFile.writeText(
            "Jul 28 15:16:07 Debug:  Scripting::fireEvent(): system-select \"psx\" \"Sony PlayStation\" \"/storage/E2AB-E84A/ROMs/psx\" \"\"\n",
        )

        // Boot reported as happening well before the file's real (test-run time) mtime -
        // i.e. the file was written after boot, so its anchor is trustworthy.
        val repository = repositoryFor(logFile, bootTimeMillis = { System.currentTimeMillis() - 60_000L })

        repository.observeEvents().test {
            assertEquals(
                EsdeEvent.SystemSelect("psx", "Sony PlayStation", "/storage/E2AB-E84A/ROMs/psx"),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startup emits nothing when the file has no anchor event at all`() = runTest {
        val logFile = tempFolder.newFile("es_log.txt")
        logFile.writeText(
            "Jul 28 15:16:08 Debug:  Scripting::fireEvent(): screensaver-start \"manual\" \"\" \"\" \"\"\n",
        )

        val repository = repositoryFor(logFile)

        repository.observeEvents().test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the replayed navigation event carries the direction of its preceding controller press`() = runTest {
        val logFile = tempFolder.newFile("es_log.txt")
        logFile.writeText(
            """
            Aug 02 13:31:34 Debug:  Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right, value=1
            Aug 02 13:31:34 Debug:  Scripting::fireEvent(): game-select "/storage/E2AB-E84A/ROMs/mastersystem/Dragon Crystal (Europe, Brazil) (En).zip" "Dragon Crystal" "mastersystem" "Sega Master System"
            Aug 02 13:31:34 Debug:  Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right, value=0
            """.trimIndent(),
        )

        val repository = repositoryFor(logFile)

        repository.observeEvents().test {
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
    fun `a non-directional button press clears a prior direction, reproducing the real log excerpt's b-triggered system-select`() = runTest {
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

        repository.observeEvents().test {
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
    fun `startup finds an anchor beyond the initial tail window`() = runTest {
        val logFile = tempFolder.newFile("es_log.txt")
        val padding = "Debug: some unrelated line\n".repeat(6_000) // ~168KB, past the 64KB initial window
        val anchorLine = "Jul 28 07:01:30 Debug:  Scripting::fireEvent(): " +
                "system-select \"dreamcast\" \"Sega Dreamcast\" \"/roms/dreamcast\" \"\""
        logFile.writeText(padding + anchorLine + "\n")

        val repository = repositoryFor(logFile)

        repository.observeEvents().test {
            assertEquals(
                EsdeEvent.SystemSelect("dreamcast", "Sega Dreamcast", "/roms/dreamcast"),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a rom path with a multi-byte UTF-8 character decodes correctly instead of as ISO-8859-1 mojibake`() = runTest {
        val logFile = tempFolder.newFile("es_log.txt")
        logFile.writeText(
            "Aug 03 10:00:00 Debug:  Scripting::fireEvent(): game-select " +
                "\"/storage/E2AB-E84A/ROMs/steam/NieR_Automata™.steam\" \"NieR_Automata™\" \"steam\" \"Steam\"\n",
        )

        val repository = repositoryFor(logFile)

        repository.observeEvents().test {
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
}