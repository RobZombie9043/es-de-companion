package com.esde.companion.data.debug

import com.esde.companion.BuildConfig
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Writes an opt-in debug log (state transitions, media resolution outcomes) to [logFile],
 * to help diagnose user-reported issues without a live repro. Mirrors es_log.txt's own
 * truncate-per-process-start behavior; line style is `<Mon dd HH:mm:ss> <Event>: <details>`,
 * with [Event] a capitalized tag (Info/Event/State/Media) identifying the kind of entry,
 * padded to a fixed column width (see [EVENT_COLUMN_WIDTH]) so every line's details line
 * up regardless of tag length.
 *
 * All writes are fire-and-forget on [applicationScope]/[ioDispatcher], guarded by a
 * [Mutex], so callers on the hot reducer/media-resolution paths are never blocked by disk
 * I/O. [logFile], [clock], and [ioDispatcher] are injected rather than derived/hardcoded
 * internally, so this class stays unit-testable against a temp file, a fixed time source,
 * and a deterministic test dispatcher.
 */
class DebugFileLogger(
    private val logFile: File,
    private val onboardingRepository: OnboardingRepository,
    private val applicationScope: CoroutineScope,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    @Volatile
    private var enabled = false

    @Volatile
    private var hasResetThisProcess = false

    private val writeMutex = Mutex()
    private val timestampFormatter = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss", Locale.US).withZone(clock.zone)

    init {
        onboardingRepository
            .observeDebugLoggingEnabled()
            .onEach { enabled = it }
            .launchIn(applicationScope)
    }

    fun logStateTransition(
        event: EsdeEvent,
        newState: AppState,
    ) = log { listOf("Event" to "$event", "State" to newState.describe()) }

    /**
     * [AppState.Screensaver.previousState] can itself be an arbitrarily nested prior
     * state - logging it verbatim would print the entire pre-screensaver state inline on
     * every screensaver transition, which is noise here (that state was already logged
     * in its own right when it first became current). Every other variant logs as its
     * plain data class [toString].
     */
    private fun AppState.describe(): String =
        when (this) {
            is AppState.Screensaver -> "Screensaver(mode=$mode, currentGame=$currentGame)"
            else -> "$this"
        }

    /**
     * Logs one line per requested type in [mediaTypes] - the set the caller actually
     * needed (e.g. the GameMedia widgets currently placed on a canvas, or the single type
     * a video/manual overlay needs) - as `<Type> FOUND <path>` or `<Type> NOT FOUND
     * <path with a wildcard extension>`, rather than dumping every media type on disk for
     * the game regardless of relevance. The NOT FOUND path shows where that file was
     * looked for (real extension unknown, since none matched) so it's directly checkable
     * by hand. A no-op (nothing logged) when [mediaTypes] is empty, since there's nothing
     * meaningful to report.
     */
    fun logMediaResolution(
        systemShortName: String,
        mediaTypes: Set<MediaType>,
        result: GameMedia,
    ) {
        if (mediaTypes.isEmpty()) return
        log {
            val repo = onboardingRepository
            val mediaFolder = repo.observeMediaFolderPath().first() ?: repo.defaultMediaFolderPath()
            MediaType.entries
                .filter { it in mediaTypes }
                .map { type ->
                    val foundPath = result.filesByType[type]
                    val location =
                        foundPath ?: notFoundLocation(mediaFolder, systemShortName, type, result.baseRelativePath)
                    val status = if (foundPath != null) "FOUND" else "NOT FOUND"
                    "Media" to "$type $status $location"
                }
        }
    }

    // A type with only one valid extension (e.g. Manuals -> pdf) has no real ambiguity
    // about what would have matched, so show it directly rather than a wildcard.
    private fun notFoundLocation(
        mediaFolder: String,
        systemShortName: String,
        type: MediaType,
        baseRelativePath: String?,
    ): String {
        val extension = type.validExtensions.singleOrNull() ?: "*"
        return baseRelativePath
            ?.let { "$mediaFolder/$systemShortName/${type.folderName}/$it.$extension" }
            ?: "(unresolvable path)"
    }

    /**
     * Writes one line per (event tag, details) entry [build] produces, all within a
     * single mutex hold - so a multi-line log call (e.g. logStateTransition's Event/State
     * pair, or one line per media type) is never interleaved with a concurrent caller's
     * lines. [build] runs suspended, inside the same lock, so it can do its own lookups
     * (e.g. the configured media folder) without a separate round trip.
     */
    private fun log(build: suspend () -> List<Pair<String, String>>) {
        if (!enabled) return
        applicationScope.launch(ioDispatcher) {
            if (!enabled || !AllFilesAccessPermission.isGranted()) return@launch
            writeMutex.withLock {
                if (!hasResetThisProcess) {
                    resetFileAndWriteHeader()
                    hasResetThisProcess = true
                }
                build().forEach { (event, details) -> appendLine(event, details) }
            }
        }
    }

    private suspend fun resetFileAndWriteHeader() {
        logFile.parentFile?.mkdirs()
        logFile.writeText("")
        val repo = onboardingRepository
        val esdeFolder = repo.observeLogFolderPath().first() ?: repo.defaultLogFolderPath()
        val mediaFolder = repo.observeMediaFolderPath().first() ?: repo.defaultMediaFolderPath()
        val customImagesFolder = repo.observeCustomSystemImagesFolderPath().first() ?: NOT_SET
        val customLogosFolder = repo.observeCustomLogosFolderPath().first() ?: NOT_SET
        val customMusicFolder = repo.observeCustomMusicFolderPath().first() ?: NOT_SET
        appendLine("Info", "companion-version: ${BuildConfig.VERSION_NAME}")
        appendLine("Info", "esde-folder: $esdeFolder")
        appendLine("Info", "media-folder: $mediaFolder")
        appendLine("Info", "custom-images-folder: $customImagesFolder")
        appendLine("Info", "custom-logos-folder: $customLogosFolder")
        appendLine("Info", "custom-music-folder: $customMusicFolder")
    }

    private fun appendLine(
        event: String,
        details: String,
    ) {
        val timestamp = timestampFormatter.format(clock.instant())
        val eventColumn = "$event:".padEnd(EVENT_COLUMN_WIDTH)
        logFile.appendText("$timestamp $eventColumn$details\n")
    }

    private companion object {
        const val NOT_SET = "(not set)"

        // Sized to the longest tag ("Event:"/"State:"/"Media:", 6 chars) plus one
        // separating space, so every logged line's details start in the same column
        // regardless of which tag (Info/Event/State/Media) it uses.
        const val EVENT_COLUMN_WIDTH = 7
    }
}
