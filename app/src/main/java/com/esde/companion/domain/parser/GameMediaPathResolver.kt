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

    fun resolveBaseRelativePath(systemShortName: String, romPath: String, romIsDirectory: Boolean): String? {
        val systemMarker = "/$systemShortName/"
        val markerIndex = romPath.lastIndexOf(systemMarker)
        if (markerIndex == -1) return null

        val relativePath = romPath.substring(markerIndex + systemMarker.length)
        if (relativePath.isEmpty()) return null

        if (romIsDirectory) return relativePath

        val segments = relativePath.split('/')
        val lastSegment = segments.last()
        val extensionIndex = lastSegment.lastIndexOf('.')
        val nameWithoutExtension =
            if (extensionIndex > 0) lastSegment.substring(0, extensionIndex) else lastSegment
        return (segments.dropLast(1) + nameWithoutExtension).joinToString("/")
    }
}