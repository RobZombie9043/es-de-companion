package com.esde.companion.domain.model

data class MusicTrack(
    val filePath: String,
    val title: String,
)

/**
 * Title is the filename minus its extension, computed via pure string ops (no [java.io.File])
 * so this stays testable against fixture paths with no filesystem required - same reasoning
 * as [com.esde.companion.domain.parser.GameMediaPathResolver].
 */
fun musicTrackFromPath(filePath: String): MusicTrack {
    val fileName = filePath.substringAfterLast('/')
    val extensionIndex = fileName.lastIndexOf('.')
    val title = if (extensionIndex > 0) fileName.substring(0, extensionIndex) else fileName
    return MusicTrack(filePath = filePath, title = title)
}
