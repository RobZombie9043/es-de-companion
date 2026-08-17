package com.esde.companion.domain.parser

/**
 * Computes the media base path (system-relative) that a game's media files live under in
 * ES-DE's downloaded_media layout - e.g. for rom path
 * ".../ROMs/dreamcast/Cosmic Smash (Japan).chd", system "dreamcast", romIsDirectory false,
 * this returns "Cosmic Smash (Japan)"; a caller then appends "/<mediaType>/<result>.<ext>"
 * onto the media root to get a candidate file path.
 *
 * [romIsDirectory] distinguishes ES-DE's "directories interpreted as files" case: when the
 * rom path itself is a directory (e.g. "Final Fantasy VII (USA).m3u" containing a file of
 * the same name), ES-DE treats that folder as the launchable unit and does NOT strip its
 * trailing ".m3u" when naming media - it isn't a stripped file extension there, it's the
 * directory's literal name. For an ordinary rom *file* (including a genuine standalone
 * .m3u playlist that is a file, not a directory), the extension is stripped as usual.
 *
 * Which case applies can't be determined from the path string alone - it requires checking
 * the real filesystem, which is deliberately kept out of this function (and out of domain
 * generally). Callers resolve [romIsDirectory] via File I/O in the data layer and pass the
 * answer in here, keeping this a pure function unit-testable against fixture paths with no
 * filesystem required.
 */
object GameMediaPathResolver {
    /**
     * [systemPath] is ES-DE's own reported ROM directory for this system - the `system-select`
     * fireEvent()'s third argument, carried on [com.esde.companion.domain.model.AppState] (see
     * AppStateReducer) - and is preferred whenever [romPath] actually sits under it, since it's
     * authoritative regardless of what that folder happens to be named on disk. ES-DE lets a
     * system's ROM directory be configured to anything, so the folder name is not guaranteed to
     * match [systemShortName] at all (e.g. a "DS" folder for shortname "nds") - a real setup
     * reported by a user, where no path search on [systemShortName] alone could ever succeed.
     *
     * Falls back to a marker search for "/[systemShortName]/" as a literal path segment
     * (case-insensitively - Android's filesystem is case-sensitive, so a folder named "PS2" for
     * shortname "ps2" is otherwise an exact-match miss, also a reported real setup) when
     * [systemPath] is null (e.g. a cold-start replay that never observed a system-select) or
     * romPath doesn't sit under it.
     */
    fun resolveBaseRelativePath(
        systemShortName: String,
        systemPath: String?,
        romPath: String,
        romIsDirectory: Boolean,
    ): String? {
        val relativePath =
            relativePathUnderSystemPath(systemPath, romPath)
                ?: relativePathFromShortNameMarker(systemShortName, romPath)
                ?: return null
        return baseRelativePathFrom(relativePath, romIsDirectory)
    }

    private fun relativePathUnderSystemPath(
        systemPath: String?,
        romPath: String,
    ): String? {
        if (systemPath.isNullOrEmpty()) return null
        val prefix = systemPath.removeSuffix("/") + "/"
        return romPath.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)
    }

    private fun relativePathFromShortNameMarker(
        systemShortName: String,
        romPath: String,
    ): String? {
        val systemMarker = "/$systemShortName/"
        val markerIndex = romPath.lastIndexOf(systemMarker, ignoreCase = true)
        if (markerIndex == -1) return null
        return romPath.substring(markerIndex + systemMarker.length)
    }

    /**
     * Applies the directory-vs-file extension rule described above to a ROM-directory-relative
     * path already resolved by one of the strategies in [resolveBaseRelativePath].
     */
    private fun baseRelativePathFrom(
        relativePath: String,
        romIsDirectory: Boolean,
    ): String? =
        when {
            relativePath.isEmpty() -> null
            romIsDirectory -> relativePath
            else -> relativePath.withoutLastSegmentExtension()
        }

    private fun String.withoutLastSegmentExtension(): String {
        val segments = split('/')
        val lastSegment = segments.last()
        val extensionIndex = lastSegment.lastIndexOf('.')
        val nameWithoutExtension =
            if (extensionIndex > 0) lastSegment.substring(0, extensionIndex) else lastSegment
        return (segments.dropLast(1) + nameWithoutExtension).joinToString("/")
    }
}
