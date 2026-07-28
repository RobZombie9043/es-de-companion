package com.esde.companion.data.log

import app.cash.turbine.test
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeEvent
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

        val repository = EsdeLogFileRepository(logFilePath = logFile.absolutePath)

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
    fun `startup emits nothing when the file has no anchor event at all`() = runTest {
        val logFile = tempFolder.newFile("es_log.txt")
        logFile.writeText(
            "Jul 28 15:16:08 Debug:  Scripting::fireEvent(): screensaver-start \"manual\" \"\" \"\" \"\"\n",
        )

        val repository = EsdeLogFileRepository(logFilePath = logFile.absolutePath)

        repository.observeEvents().test {
            expectNoEvents()
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

        val repository = EsdeLogFileRepository(logFilePath = logFile.absolutePath)

        repository.observeEvents().test {
            assertEquals(
                EsdeEvent.SystemSelect("dreamcast", "Sega Dreamcast", "/roms/dreamcast"),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}