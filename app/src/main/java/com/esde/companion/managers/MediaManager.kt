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
        private val IMAGE_EXTENSIONS = AppConstants.FileExtensions.IMAGE
        private val VIDEO_EXTENSIONS = AppConstants.FileExtensions.VIDEO
    }

    fun findImageInFolder(
        systemName: String,
        gameName: String,
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
            listOf("screenshots", "fanart")
        else
            listOf("fanart", "screenshots")

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
        val rawName = strippedFilename

        val subfolderPath = extractSubfolderPath(fullPath, systemName)

        // 1️⃣ Subfolder lookup
        if (subfolderPath != null) {
            val subDir = File(dir, subfolderPath)
            if (subDir.exists()) {
                tryFindFileWithExtensions(subDir, nameWithoutExt, rawName, extensions)
                    ?.let { return it }
            }
        }

        // 2️⃣ Root fallback
        return tryFindFileWithExtensions(dir, nameWithoutExt, rawName, extensions)
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
            Log.d("MediaFileLocator", "System name '$systemName' not found in path: $fullPath")
            return null
        }

        val subfolders = segments.drop(systemIndex + 1)
        val result = if (subfolders.isNotEmpty()) subfolders.joinToString("/") else null

        Log.d("MediaFileLocator", "Extracted subfolder: '$result' from path: $fullPath")
        return result
    }
}