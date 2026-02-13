package com.esde.companion.managers

import android.util.Log
import com.esde.companion.data.AppConstants
import java.io.File

/**
 * Centralized media file location logic for ES-DE Companion.
 *
 * Handles finding media files (images and videos) with support for:
 * - Subfolders (e.g., "subfolder/game.zip" finds "media/screenshots/subfolder/game.png")
 * - Multiple file extensions
 * - Fallback search patterns (subfolder -> root level)
 * - Different media types (fanart, screenshots, marquees, videos)
 *
 * This class eliminates duplicate file-finding logic across MainActivity.
 */
class MediaManager(private val prefsManager: PreferencesManager) {

    companion object {
        private const val TAG = "MediaManager"
        private val IMAGE_EXTENSIONS = AppConstants.FileExtensions.IMAGE
        private val VIDEO_EXTENSIONS = AppConstants.FileExtensions.VIDEO
    }

    fun findImageInFolder(
        systemName: String,
        gameFilename: String,
        folderName: String
    ): File? {
        val mediaPath = prefsManager.mediaPath
        val dir = File(mediaPath, "$systemName/$folderName")

        if (!dir.exists()) return null

        return findFileInDirectory(
            dir = dir,
            fullPath = gameFilename,
            extensions = IMAGE_EXTENSIONS,
            systemName = systemName
        )
    }

    fun findGameBackgroundImage(
        systemName: String,
        gameFilename: String,
        preferScreenshot: Boolean
    ): File? {
        val mediaBase = File(prefsManager.mediaPath, systemName)
        if (!mediaBase.exists()) return null

        val dirs = if (preferScreenshot)
            listOf(AppConstants.Paths.MEDIA_SCREENSHOTS, AppConstants.Paths.MEDIA_FANART)
        else
            listOf(AppConstants.Paths.MEDIA_FANART, AppConstants.Paths.MEDIA_SCREENSHOTS)

        for (dirName in dirs) {
            val dir = File(mediaBase, dirName)
            val file = findFileInDirectory(
                dir = dir,
                fullPath = gameFilename,
                extensions = IMAGE_EXTENSIONS,
                systemName = systemName
            )
            if (file != null) return file
        }

        return null
    }

    fun findVideoFile(systemName: String, gameFilename: String): String? {
        val videoDir =
            File(prefsManager.mediaPath, "$systemName/${AppConstants.Paths.MEDIA_VIDEOS}")
        if (!videoDir.exists()) return null

        return findFileInDirectory(
            dir = videoDir,
            fullPath = gameFilename,
            extensions = VIDEO_EXTENSIONS,
            systemName = systemName
        )?.absolutePath
    }

    private fun findFileInDirectory(
        dir: File,
        fullPath: String,
        extensions: List<String>,
        systemName: String
    ): File? {
        if (!dir.exists() || !dir.isDirectory) return null

        val strippedFilename = sanitizeFilename(fullPath)
        val nameWithoutExt = strippedFilename.substringBeforeLast('.')

        val subfolderPath = extractSubfolderPath(fullPath, systemName)

        // Use subfolder if present, otherwise search directly in dir
        val searchDir = if (subfolderPath != null) File(dir, subfolderPath) else dir

        return tryFindFileWithExtensions(searchDir, nameWithoutExt, strippedFilename, extensions)
    }

    private fun tryFindFileWithExtensions(
        dir: File,
        strippedName: String,
        rawName: String,
        extensions: List<String>
    ): File? {
        for (name in listOf(strippedName, rawName)) {
            for (ext in extensions) {
                val file = File(dir, "$name.$ext")
                if (file.exists()) return file
            }
        }
        return null
    }

    private fun sanitizeFilename(fullPath: String): String {
        val normalized = fullPath.replace("\\", "/")
        return normalized.substringAfterLast("/")
    }

    /**
     * Returns path relative to the system folder.
     *
     * Works with any ROM folder name by using the system name as anchor point.
     * This handles cases where ES-DE uses "Games", "ROMs", "Roms", or any custom folder name.
     *
     * Examples:
     *  "sub/game.zip" → "sub"
     *  "/storage/.../Games/psx/sub/game.zip" → "sub"
     *  "/storage/.../ROMs/psx/sub/deep/game.zip" → "sub/deep"
     *  "/storage/.../MyRoms/psx/game.zip" → null
     *  "game.zip" → null
     *
     * @param fullPath The full path from ES-DE
     * @param systemName The ES-DE system name (e.g., "psx", "snes")
     * @return The subfolder path relative to the system folder, or null if no subfolder
     */
    private fun extractSubfolderPath(
        fullPath: String,
        systemName: String
    ): String? {

        val normalized = fullPath.replace("\\", "/")
        val beforeFilename = normalized.substringBeforeLast("/", "")
        if (beforeFilename.isEmpty()) return null

        // Relative path (e.g., "sub/game.zip")
        if (!beforeFilename.startsWith("/")) {
            return beforeFilename.ifEmpty { null }
        }

        // Absolute path - find system folder and extract subfolders after it
        val segments = beforeFilename.split("/").filter { it.isNotEmpty() }
        val systemIndex = segments.indexOf(systemName)

        if (systemIndex == -1) {
            Log.d(TAG, "System name '$systemName' not found in path: $fullPath")
            return null
        }

        val subfolders = segments.drop(systemIndex + 1)
        val result = if (subfolders.isNotEmpty()) subfolders.joinToString("/") else null

        Log.d(TAG, "Extracted subfolder: '$result' from path: $fullPath")
        return result
    }

    /**
     * Get a random image file from a system's media folder.
     *
     * @param systemName The ES-DE system name
     * @param folderName Media folder (e.g., "fanart", "screenshots")
     * @return Random image file, or null if folder is empty
     */
    fun getRandomImageFromSystemFolder(systemName: String, folderName: String): File? {
        val mediaPath = prefsManager.mediaPath
        val dir = File(mediaPath, "$systemName/$folderName")

        if (!dir.exists() || !dir.isDirectory) {
            Log.w("MediaManager", "Directory doesn't exist: ${dir.absolutePath}")
            return null
        }

        // Recursively find all image files in directory (including subfolders)
        val allImages = mutableListOf<File>()
        collectImageFiles(dir, allImages)

        if (allImages.isEmpty()) {
            Log.w("MediaManager", "No images found in: ${dir.absolutePath}")
            return null
        }

        val randomImage = allImages.random()
        Log.d("MediaManager", "Selected random image from ${allImages.size} total: ${randomImage.name}")
        return randomImage
    }

    /**
     * Recursively collect all image files from a directory.
     */
    private fun collectImageFiles(dir: File, accumulator: MutableList<File>) {
        if (!dir.exists() || !dir.isDirectory) return

        dir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> collectImageFiles(file, accumulator)
                file.isFile && file.extension.lowercase() in IMAGE_EXTENSIONS -> {
                    accumulator.add(file)
                }
            }
        }
    }

    /**
     * Parse a game's description from ES-DE gamelist.xml.
     *
     * Checks the standard ES-DE location first, then falls back to the legacy
     * ROMs-adjacent location used when ES-DE's LegacyGamelistFileLocation is enabled.
     *
     * Standard:  ~/ES-DE/gamelists/<system>/gamelist.xml
     * Legacy:    <romsRoot>/<system>/gamelist.xml  (derived from the game's absolute path)
     *
     * @param systemName    ES-DE system name (e.g. "psx", "snes")
     * @param gameFilename  Game path as received from ES-DE — may be absolute, relative, or bare
     * @return Description string, or null if not found or any error occurs
     */
    fun getGameDescription(systemName: String, gameFilename: String): String? {
        try {
            val gamelistFile = resolveGamelistFile(systemName, gameFilename) ?: return null
            val xmlContent = gamelistFile.readText()
            return parseDescriptionFromXml(xmlContent, systemName, gameFilename)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing gamelist.xml", e)
            return null
        }
    }

    /**
     * Resolve which gamelist.xml file to use.
     *
     * Standard ES-DE location is always tried first. The legacy ROMs-adjacent
     * location is only used as a fallback if the standard file doesn't exist.
     *
     * The legacy path is derived from the game's absolute filepath — everything
     * before the system name segment is treated as the ROMs root, regardless of
     * what the folder is named or where it lives.
     *
     * Examples:
     *   "/storage/emulated/0/ROMs/psx/game.zip"    → "/storage/emulated/0/ROMs/psx/gamelist.xml"
     *   "/storage/sdcard/Games/psx/sub/game.zip"   → "/storage/sdcard/Games/psx/gamelist.xml"
     *
     * @return The gamelist.xml File to use, or null if neither location exists
     */
    private fun resolveGamelistFile(systemName: String, gameFilename: String): File? {
        // Derive ES-DE root from the configured scripts path
        val scriptsDir = File(prefsManager.scriptsPath)
        val esdeRoot = scriptsDir.parentFile ?: return null

        // Standard path: ~/ES-DE/gamelists/<system>/gamelist.xml
        val standardFile = File(esdeRoot, "gamelists/$systemName/gamelist.xml")
        if (standardFile.exists()) {
            Log.d(TAG, "Using standard gamelist: ${standardFile.absolutePath}")
            return standardFile
        }

        // Legacy path: derive ROMs root from the game's absolute filepath
        val legacyFile = deriveLegacyGamelistFile(systemName, gameFilename)
        if (legacyFile?.exists() == true) {
            Log.d(TAG, "Using legacy gamelist: ${legacyFile.absolutePath}")
            return legacyFile
        }

        Log.d(TAG, "Gamelist not found for system: $systemName")
        Log.d(TAG, "  Checked standard: ${standardFile.absolutePath}")
        legacyFile?.let { Log.d(TAG, "  Checked legacy:   ${it.absolutePath}") }
        return null
    }

    /**
     * Derive the legacy gamelist.xml path from the game's absolute filepath.
     * Returns null if the filepath is relative (can't determine ROMs root).
     */
    private fun deriveLegacyGamelistFile(systemName: String, gameFilename: String): File? {
        val normalized = gameFilename.replace("\\", "/")
        if (!normalized.startsWith("/")) return null  // Relative path — can't derive root

        val segments = normalized.split("/").filter { it.isNotEmpty() }
        val sysIndex = segments.indexOf(systemName)
        if (sysIndex <= 0) return null  // System name not found, or nothing before it

        val romsRoot = "/" + segments.take(sysIndex).joinToString("/")
        return File(romsRoot, "$systemName/gamelist.xml")
    }

    /**
     * Parse the description for a specific game from gamelist XML content.
     *
     * Normalises the game filepath to the relative form ES-DE uses in <path> tags,
     * then tries multiple matching strategies to handle edge cases.
     */
    private fun parseDescriptionFromXml(
        xmlContent: String,
        systemName: String,
        gameFilename: String
    ): String? {
        // Normalise to the relative path ES-DE writes after the leading "./"
        // e.g. "/storage/.../psx/sub/game.zip" → "sub/game.zip"
        //      "sub/game.zip"                  → "sub/game.zip"
        //      "game.zip"                      → "game.zip"
        val normalizedPath = gameFilename.replace("\\", "/")
        val relativeGamePath: String = run {
            val segments = normalizedPath.split("/").filter { it.isNotEmpty() }
            val sysIndex = segments.indexOf(systemName)
            when {
                sysIndex != -1 && sysIndex + 1 < segments.size ->
                    segments.drop(sysIndex + 1).joinToString("/")
                normalizedPath.startsWith("/") ->
                    normalizedPath.substringAfterLast("/")
                else ->
                    normalizedPath
            }
        }

        Log.d(TAG, "Searching gamelist for: '$relativeGamePath'")

        // ES-DE only encodes & as &amp; in <path> tags
        val relativePathEncoded = relativeGamePath.replace("&", "&amp;")

        var pathMatch: MatchResult? = null

        // Strategy 1: &amp; encoded variant (most common for paths containing &)
        if (relativePathEncoded != relativeGamePath) {
            Log.d(TAG, "  Trying &amp; encoded: '$relativePathEncoded'")
            pathMatch = "<path>\\./\\Q$relativePathEncoded\\E</path>".toRegex().find(xmlContent)
        }

        // Strategy 2: Exact relative path match
        if (pathMatch == null) {
            Log.d(TAG, "  Trying exact: '$relativeGamePath'")
            pathMatch = "<path>\\./\\Q$relativeGamePath\\E</path>".toRegex().find(xmlContent)
        }

        // Strategy 3: Filename-only fallback (handles edge cases where the full
        // path couldn't be reconstructed but the filename is unique in the list)
        if (pathMatch == null) {
            val bareFilename = relativeGamePath.substringAfterLast("/")
            val bareEncoded = bareFilename.replace("&", "&amp;")
            Log.d(TAG, "  Trying filename-only fallback: '$bareFilename'")
            pathMatch = "<path>\\./(?:[^<]*/)?\\Q$bareEncoded\\E</path>".toRegex().find(xmlContent)
            if (pathMatch == null && bareEncoded != bareFilename) {
                pathMatch = "<path>\\./(?:[^<]*/)?\\Q$bareFilename\\E</path>".toRegex().find(xmlContent)
            }
        }

        if (pathMatch == null) {
            Log.d(TAG, "Game not found in gamelist: $relativeGamePath")
            return null
        }

        Log.d(TAG, "Game found in gamelist!")

        // Search for <desc> within this game entry (before the next <game> tag)
        val gameStartIndex = pathMatch.range.first
        val remainingXml = xmlContent.substring(gameStartIndex)
        val nextGameIndex = remainingXml.indexOf("<game>", startIndex = 1)
        val searchSpace = if (nextGameIndex > 0) remainingXml.take(nextGameIndex) else remainingXml

        val descMatch = "<desc>([\\s\\S]*?)</desc>".toRegex().find(searchSpace)
        return if (descMatch != null) {
            val description = decodeXmlEntities(descMatch.groupValues[1].trim())
            Log.d(TAG, "Found description: ${description.take(100)}...")
            description
        } else {
            Log.d(TAG, "No <desc> tag found for: $relativeGamePath")
            null
        }
    }

    /**
     * Decode XML entities to regular characters for display.
     */
    private fun decodeXmlEntities(text: String): String {
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
    }

}