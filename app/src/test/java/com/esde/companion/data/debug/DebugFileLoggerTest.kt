package com.esde.companion.data.debug

import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DebugFileLoggerTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeOnboardingRepository(
        val debugLoggingEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false),
        private val logFolderPath: String? = "/storage/emulated/0/ES-DE",
        private val mediaFolderPath: String? = "/storage/emulated/0/ES-DE/downloaded_media",
    ) : OnboardingRepository {
        override fun defaultLogFolderPath() = "/storage/emulated/0/ES-DE"

        override fun defaultMediaFolderPath() = "/storage/emulated/0/ES-DE/downloaded_media"

        override suspend fun validateLogFolder(path: String) = LogFolderValidation.FolderFound(true)

        override suspend fun validateMediaFolder(path: String) = MediaFolderValidation.FolderFound

        override suspend fun saveLogFolderPath(path: String) {}

        override suspend fun saveMediaFolderPath(path: String) {}

        override fun observeLogFolderPath(): Flow<String?> = flowOf(logFolderPath)

        override fun observeMediaFolderPath(): Flow<String?> = flowOf(mediaFolderPath)

        override suspend fun saveCustomSystemImagesFolderPath(path: String) {}

        override fun observeCustomSystemImagesFolderPath(): Flow<String?> = flowOf(null)

        override suspend fun clearCustomSystemImagesFolderPath() {}

        override suspend fun saveCustomLogosFolderPath(path: String) {}

        override fun observeCustomLogosFolderPath(): Flow<String?> = flowOf(null)

        override suspend fun clearCustomLogosFolderPath() {}

        override suspend fun markOnboardingComplete() {}

        override fun observeOnboardingComplete(): Flow<Boolean> = flowOf(false)

        override suspend fun setThemePreference(preference: ThemePreference) {}

        override fun observeThemePreference(): Flow<ThemePreference> = flowOf(ThemePreference.Auto)

        override suspend fun setGamePlayingBehavior(behavior: ScreenBehavior) {}

        override fun observeGamePlayingBehavior(): Flow<ScreenBehavior> = flowOf(ScreenBehavior.Nothing)

        override suspend fun setGamePlayingDimPercent(percent: Int) {}

        override fun observeGamePlayingDimPercent(): Flow<Int> = flowOf(50)

        override suspend fun setScreensaverBehavior(behavior: ScreenBehavior) {}

        override fun observeScreensaverBehavior(): Flow<ScreenBehavior> = flowOf(ScreenBehavior.Nothing)

        override suspend fun setScreensaverDimPercent(percent: Int) {}

        override fun observeScreensaverDimPercent(): Flow<Int> = flowOf(50)

        override suspend fun setVideoPlaybackEnabled(enabled: Boolean) {}

        override fun observeVideoPlaybackEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setVideoDelaySeconds(seconds: Int) {}

        override fun observeVideoDelaySeconds(): Flow<Int> = flowOf(0)

        override suspend fun setVideoAudioEnabled(enabled: Boolean) {}

        override fun observeVideoAudioEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicEnabled(enabled: Boolean) {}

        override fun observeMusicEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicPlayWhileBrowsingSystems(enabled: Boolean) {}

        override fun observeMusicPlayWhileBrowsingSystems(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicPlayWhileBrowsingGames(enabled: Boolean) {}

        override fun observeMusicPlayWhileBrowsingGames(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicPlayDuringScreensaver(enabled: Boolean) {}

        override fun observeMusicPlayDuringScreensaver(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicDuckingMode(mode: MusicDuckingMode) {}

        override fun observeMusicDuckingMode(): Flow<MusicDuckingMode> = flowOf(MusicDuckingMode.LowerVolume)

        override suspend fun setOverlayOpacityPercent(percent: Int) {}

        override fun observeOverlayOpacityPercent(): Flow<Int> = flowOf(80)

        override suspend fun saveCustomMusicFolderPath(path: String) {}

        override fun observeCustomMusicFolderPath(): Flow<String?> = flowOf(null)

        override suspend fun clearCustomMusicFolderPath() {}

        override suspend fun setCloseCompanionOnQuitEnabled(enabled: Boolean) {}

        override fun observeCloseCompanionOnQuitEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setFabAssignments(assignments: FabAssignments) {}

        override fun observeFabAssignments(): Flow<FabAssignments> = flowOf(FabAssignments.Default)

        override suspend fun setLaunchEsdeOnStartEnabled(enabled: Boolean) {}

        override fun observeLaunchEsdeOnStartEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setDebugLoggingEnabled(enabled: Boolean) {
            debugLoggingEnabled.value = enabled
        }

        override fun observeDebugLoggingEnabled(): Flow<Boolean> = debugLoggingEnabled
    }

    // Mirrors DebugFileLogger's private eventColumnWidth - kept in sync manually since
    // it's an internal formatting detail, not part of the class's public contract.
    private val eventColumnWidth = 7

    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-08-09T14:23:01Z"), ZoneOffset.UTC)

    // Unconfined everywhere (both the scope backing the init-block enabled-flag collector
    // and ioDispatcher) so every write/collection happens synchronously within the test
    // body itself - no advanceUntilIdle()/joins needed to observe file contents.
    // Mirrors DebugFileLogger's private default - kept in sync manually, same reasoning
    // as eventColumnWidth above.
    private val defaultMaxLogFileSizeBytes = 5L * 1024 * 1024

    private fun loggerFor(
        logFile: File,
        onboardingRepository: OnboardingRepository,
        maxLogFileSizeBytes: Long = defaultMaxLogFileSizeBytes,
    ) = DebugFileLogger(
        logFile = logFile,
        onboardingRepository = onboardingRepository,
        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        clock = fixedClock,
        ioDispatcher = UnconfinedTestDispatcher(),
        maxLogFileSizeBytes = maxLogFileSizeBytes,
    )

    private val sampleTransition: (DebugFileLogger) -> Unit = { logger ->
        logger.logStateTransition(
            event = EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"),
            newState = AppState.BrowsingSystem("gc", "Nintendo GameCube", "/roms/gc"),
        )
    }

    @Test
    fun `disabled - no file is ever created`() =
        runTest(UnconfinedTestDispatcher()) {
            val logFile = File(tempFolder.root, "logs/esde_companion_log.txt")
            val logger = loggerFor(logFile, FakeOnboardingRepository())

            sampleTransition(logger)

            assertFalse(logFile.exists())
        }

    @Test
    fun `enabled - first write creates parent dirs, writes the session header, then the state lines`() =
        runTest(UnconfinedTestDispatcher()) {
            val logFile = File(tempFolder.root, "logs/esde_companion_log.txt")
            val logger = loggerFor(logFile, FakeOnboardingRepository(debugLoggingEnabled = MutableStateFlow(true)))

            sampleTransition(logger)

            assertTrue(logFile.exists())
            val lines = logFile.readLines()
            assertEquals(8, lines.size)
            assertTrue(lines[0].startsWith("Aug 09 14:23:01 Info:"))
            assertTrue(lines[0].contains("companion-version:"))
            assertTrue(lines[1].contains("esde-folder: /storage/emulated/0/ES-DE"))
            assertTrue(lines[2].contains("media-folder: /storage/emulated/0/ES-DE/downloaded_media"))
            assertTrue(lines[3].contains("custom-images-folder: (not set)"))
            assertTrue(lines[4].contains("custom-logos-folder: (not set)"))
            assertTrue(lines[5].contains("custom-music-folder: (not set)"))
            assertTrue(lines[6].startsWith("Aug 09 14:23:01 Event:"))
            assertTrue(lines[6].contains("SystemSelect") && lines[6].contains("gc"))
            assertTrue(lines[7].startsWith("Aug 09 14:23:01 State:"))
            assertTrue(lines[7].contains("BrowsingSystem") && lines[7].contains("gc"))

            // "Neatly formatted": every line's details start in the same column,
            // regardless of how long that particular line's event tag is - "Info:"
            // and "Event:"/"State:" all land their details at the same offset.
            val detailsColumn = "Aug 09 14:23:01 ".length + eventColumnWidth
            assertTrue(lines[0].drop(detailsColumn).startsWith("companion-version:"))
            assertTrue(lines[6].drop(detailsColumn).startsWith("SystemSelect"))
            assertTrue(lines[7].drop(detailsColumn).startsWith("BrowsingSystem"))
        }

    @Test
    fun `logStateTransition omits Screensaver's previousState`() =
        runTest(UnconfinedTestDispatcher()) {
            val logFile = File(tempFolder.root, "logs/esde_companion_log.txt")
            val logger = loggerFor(logFile, FakeOnboardingRepository(debugLoggingEnabled = MutableStateFlow(true)))

            val previousState = AppState.BrowsingSystem("gc", "Nintendo GameCube", "/roms/gc")
            logger.logStateTransition(
                event = EsdeEvent.ScreensaverStart(mode = "timer"),
                newState = AppState.Screensaver(mode = "timer", currentGame = null, previousState = previousState),
            )

            val stateLine = logFile.readLines().single { it.startsWith("Aug 09 14:23:01 State:") }
            assertTrue(stateLine.contains("Screensaver(mode=timer, currentGame=null)"))
            assertFalse(stateLine.contains("BrowsingSystem"))
            assertFalse(stateLine.contains("previousState"))
        }

    @Test
    fun `logMediaResolution logs one line per requested type, with a wildcard path when not found`() =
        runTest(UnconfinedTestDispatcher()) {
            val logFile = File(tempFolder.root, "logs/esde_companion_log.txt")
            val repository =
                FakeOnboardingRepository(debugLoggingEnabled = MutableStateFlow(true), mediaFolderPath = "/media")
            val logger = loggerFor(logFile, repository)

            val filesByType =
                mapOf(
                    MediaType.Covers to "/media/gc/covers/game.png",
                    MediaType.Videos to "/media/gc/videos/game.mp4",
                )
            val result = GameMedia(baseRelativePath = "game", filesByType = filesByType)
            // Marquees isn't in filesByType (a genuine miss) and Videos IS present but not
            // requested - only Covers/Marquees should show up, each on its own line.
            logger.logMediaResolution(
                systemShortName = "gc",
                mediaTypes = setOf(MediaType.Covers, MediaType.Marquees),
                result = result,
            )

            val mediaLines = logFile.readLines().filter { it.contains("Media:") }
            assertEquals(2, mediaLines.size)

            val coverLine = mediaLines.single { it.contains("Covers") }
            assertTrue(coverLine.startsWith("Aug 09 14:23:01 Media:"))
            assertTrue(coverLine.contains("Covers FOUND /media/gc/covers/game.png"))

            val marqueeLine = mediaLines.single { it.contains("Marquees") }
            assertTrue(marqueeLine.contains("Marquees NOT FOUND /media/gc/marquees/game.*"))

            assertTrue(mediaLines.none { it.contains("Videos") })
        }

    @Test
    fun `logMediaResolution shows the real extension, not a wildcard, for a type with only one valid extension`() =
        runTest(UnconfinedTestDispatcher()) {
            val logFile = File(tempFolder.root, "logs/esde_companion_log.txt")
            val repository =
                FakeOnboardingRepository(debugLoggingEnabled = MutableStateFlow(true), mediaFolderPath = "/media")
            val logger = loggerFor(logFile, repository)

            // Manuals has exactly one valid extension (pdf, see MediaType.kt), so there's
            // no ambiguity about what would have matched - unlike Covers/Marquees/etc,
            // which each accept jpg/png/webp and so stay wildcarded.
            logger.logMediaResolution(
                systemShortName = "gc",
                mediaTypes = setOf(MediaType.Manuals),
                result = GameMedia(baseRelativePath = "game", filesByType = emptyMap()),
            )

            val manualLine = logFile.readLines().single { it.contains("Media:") }
            assertTrue(manualLine.contains("Manuals NOT FOUND /media/gc/manuals/game.pdf"))
        }

    @Test
    fun `logMediaResolution with no requested types logs nothing`() =
        runTest(UnconfinedTestDispatcher()) {
            val logFile = File(tempFolder.root, "logs/esde_companion_log.txt")
            val logger = loggerFor(logFile, FakeOnboardingRepository(debugLoggingEnabled = MutableStateFlow(true)))

            logger.logMediaResolution(
                systemShortName = "gc",
                mediaTypes = emptySet(),
                result = GameMedia(baseRelativePath = "game", filesByType = emptyMap()),
            )

            assertFalse(logFile.exists())
        }

    @Test
    fun `logPlaybackStarted logs the tag and path`() =
        runTest(UnconfinedTestDispatcher()) {
            val logFile = File(tempFolder.root, "logs/esde_companion_log.txt")
            val logger = loggerFor(logFile, FakeOnboardingRepository(debugLoggingEnabled = MutableStateFlow(true)))

            logger.logPlaybackStarted("Music", "/music/general/1.mp3")
            logger.logPlaybackStarted("Video", "/media/gc/videos/game.mp4")

            val musicLine = logFile.readLines().single { it.contains("Music:") }
            assertTrue(musicLine.contains("playing /music/general/1.mp3"))
            val videoLine = logFile.readLines().single { it.contains("Video:") }
            assertTrue(videoLine.contains("playing /media/gc/videos/game.mp4"))
        }

    @Test
    fun `logPlaybackError includes the path when one is known, and works with none`() =
        runTest(UnconfinedTestDispatcher()) {
            val logFile = File(tempFolder.root, "logs/esde_companion_log.txt")
            val logger = loggerFor(logFile, FakeOnboardingRepository(debugLoggingEnabled = MutableStateFlow(true)))

            logger.logPlaybackError("Music", "/music/general/1.mp3", "unreadable file")
            logger.logPlaybackError("Music", null, "no track loaded")
            logger.logPlaybackError("Video", "/media/gc/videos/game.mp4", "decoder init failed")

            val musicLines = logFile.readLines().filter { it.contains("Music:") }
            assertEquals(2, musicLines.size)
            assertTrue(musicLines[0].contains("error playing /music/general/1.mp3: unreadable file"))
            assertTrue(musicLines[1].contains("error: no track loaded"))

            val videoLine = logFile.readLines().single { it.contains("Video:") }
            assertTrue(videoLine.contains("error playing /media/gc/videos/game.mp4: decoder init failed"))
        }

    @Test
    fun `logInfo logs the given tag and message verbatim`() =
        runTest(UnconfinedTestDispatcher()) {
            val logFile = File(tempFolder.root, "logs/esde_companion_log.txt")
            val logger = loggerFor(logFile, FakeOnboardingRepository(debugLoggingEnabled = MutableStateFlow(true)))

            // Description-widget resolution outcome (LoggingGameDescriptionRepository).
            logger.logInfo("Desc", "FOUND gc /roms/gc/game.iso")
            logger.logInfo("Desc", "NOT FOUND gc /roms/gc/missing.iso")
            // Fallback-poll diagnostic (EsdeLogFileRepository).
            logger.logInfo("Poll", "fallback poll (not FileObserver) picked up an es_log.txt update")

            val descLines = logFile.readLines().filter { it.contains("Desc:") }
            assertEquals(2, descLines.size)
            assertTrue(descLines[0].contains("FOUND gc /roms/gc/game.iso"))
            assertTrue(descLines[1].contains("NOT FOUND gc /roms/gc/missing.iso"))

            val pollLine = logFile.readLines().single { it.contains("Poll:") }
            assertTrue(pollLine.contains("fallback poll"))
            assertTrue(pollLine.contains("FileObserver"))
        }

    @Test
    fun `the log file is truncated and re-headered once it exceeds the size cap`() =
        runTest(UnconfinedTestDispatcher()) {
            val logFile = File(tempFolder.root, "logs/esde_companion_log.txt")
            val repository = FakeOnboardingRepository(debugLoggingEnabled = MutableStateFlow(true))
            // A 1-byte cap is exceeded by the very first write, so every subsequent write
            // resets (truncate + header) exactly like the first - the file's content never
            // differs between writes, and never accumulates.
            val logger = loggerFor(logFile, repository, maxLogFileSizeBytes = 1L)

            sampleTransition(logger)
            val firstWrite = logFile.readText()

            sampleTransition(logger)
            val secondWrite = logFile.readText()

            assertEquals(firstWrite, secondWrite)
        }

    @Test
    fun `writes under the size cap accumulate normally instead of resetting`() =
        runTest(UnconfinedTestDispatcher()) {
            val logFile = File(tempFolder.root, "logs/esde_companion_log.txt")
            val repository = FakeOnboardingRepository(debugLoggingEnabled = MutableStateFlow(true))
            val logger = loggerFor(logFile, repository, maxLogFileSizeBytes = defaultMaxLogFileSizeBytes)

            sampleTransition(logger)
            sampleTransition(logger)

            assertEquals(1, Regex("companion-version:").findAll(logFile.readText()).count())
            assertEquals(2, Regex("SystemSelect").findAll(logFile.readText()).count())
        }

    @Test
    fun `toggling disabled mid-process does not re-truncate on the next enable`() =
        runTest(UnconfinedTestDispatcher()) {
            val logFile = File(tempFolder.root, "logs/esde_companion_log.txt")
            val repository = FakeOnboardingRepository(debugLoggingEnabled = MutableStateFlow(true))
            val logger = loggerFor(logFile, repository)

            sampleTransition(logger)
            val contentAfterFirstWrite = logFile.readText()

            repository.debugLoggingEnabled.value = false
            sampleTransition(logger)
            assertEquals(contentAfterFirstWrite, logFile.readText())

            repository.debugLoggingEnabled.value = true
            sampleTransition(logger)

            val finalContent = logFile.readText()
            assertTrue(finalContent.startsWith(contentAfterFirstWrite))
            assertEquals(1, Regex("companion-version:").findAll(finalContent).count())
        }
}
