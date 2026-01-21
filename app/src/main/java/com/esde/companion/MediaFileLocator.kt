package com.esde.companion

import android.content.SharedPreferences
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
class MediaFileLocator(private val prefs: SharedPreferences) {
    
    companion object {
        private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp", "gif")
        private val VIDEO_EXTENSIONS = listOf("mp4", "mkv", "avi", "wmv", "mov", "webm")
    }
    
    /**
     * Find an image file in a specific folder by folder name.
     * This is the primary method used throughout MainActivity.
     * 
     * @param systemName The ES-DE system name (e.g., "nes", "snes")
     * @param gameName The game name without extension (e.g., "Super Mario World")
     * @param gameFilename The full game filename/path from ES-DE (may include subfolders)
     * @param folderName The media folder name (e.g., "marquees", "covers", "fanart")
     * @return The image file if found, null otherwise
     */
    fun findImageInFolder(
        systemName: String,
        gameName: String,
        gameFilename: String,
        folderName: String
    ): File? {
        val mediaPath = prefs.getString("media_path", "/storage/emulated/0/ES-DE/downloaded_media")
            ?: return null
        
        val dir = File(mediaPath, "$systemName/$folderName")
        if (!dir.exists()) {
            android.util.Log.d("MediaFileLocator", "Folder does not exist: ${dir.absolutePath}")
            return null
        }
        
        return findFileInDirectory(dir, gameFilename, IMAGE_EXTENSIONS)
    }
    
    /**
     * Find a media image file for a specific widget type.
     * 
     * @param type The type of image to find (MARQUEE, FANART, SCREENSHOT, etc.)
     * @param systemName The ES-DE system name (e.g., "nes", "snes")
     * @param gameFilename The full game filename/path from ES-DE (may include subfolders)
     * @return The image file if found, null otherwise
     */
    fun findMediaFile(
        type: OverlayWidget.ImageType,
        systemName: String,
        gameFilename: String
    ): File? {
        val folderName = when (type) {
            OverlayWidget.ImageType.MARQUEE -> "marquees"
            OverlayWidget.ImageType.BOX_2D -> "covers"
            OverlayWidget.ImageType.BOX_3D -> "3dboxes"
            OverlayWidget.ImageType.MIX_IMAGE -> "miximages"
            OverlayWidget.ImageType.BACK_COVER -> "backcovers"
            OverlayWidget.ImageType.PHYSICAL_MEDIA -> "physicalmedia"
            OverlayWidget.ImageType.SCREENSHOT -> "screenshots"
            OverlayWidget.ImageType.FANART -> "fanart"
            OverlayWidget.ImageType.TITLE_SCREEN -> "titlescreens"
            OverlayWidget.ImageType.GAME_DESCRIPTION -> return null // Text-based, no file
            OverlayWidget.ImageType.SYSTEM_LOGO -> return null // Handled separately
        }
        
        val gameName = sanitizeFilename(gameFilename).substringBeforeLast('.')
        return findImageInFolder(systemName, gameName, gameFilename, folderName)
    }
    
    /**
     * Find a game background image based on user preference (fanart or screenshot).
     * 
     * @param systemName The ES-DE system name
     * @param gameFilename The full game filename/path from ES-DE
     * @param preferScreenshot If true, search screenshots first; otherwise search fanart first
     * @return The background image file if found, null otherwise
     */
    fun findGameBackgroundImage(
        systemName: String,
        gameFilename: String,
        preferScreenshot: Boolean
    ): File? {
        val mediaPath = prefs.getString("media_path", "/storage/emulated/0/ES-DE/downloaded_media")
            ?: return null
        
        val mediaBase = File(mediaPath, systemName)
        if (!mediaBase.exists()) return null
        
        val dirs = if (preferScreenshot) {
            listOf("screenshots", "fanart")
        } else {
            listOf("fanart", "screenshots")
        }
        
        // Try each directory in order
        for (dirName in dirs) {
            val dir = File(mediaBase, dirName)
            val file = findFileInDirectory(dir, gameFilename, IMAGE_EXTENSIONS)
            if (file != null) {
                android.util.Log.d("MediaFileLocator", "Found background in $dirName: ${file.absolutePath}")
                return file
            }
        }
        
        android.util.Log.d("MediaFileLocator", "No background image found for: $systemName/$gameFilename")
        return null
    }
    
    /**
     * Find a video file for a game.
     * 
     * @param systemName The ES-DE system name
     * @param gameFilename The full game filename/path from ES-DE
     * @return The video file path if found, null otherwise
     */
    fun findVideoFile(systemName: String, gameFilename: String): String? {
        val mediaPath = prefs.getString("media_path", "/storage/emulated/0/ES-DE/downloaded_media")
            ?: return null
        
        val videoDir = File(mediaPath, "$systemName/videos")
        if (!videoDir.exists()) {
            android.util.Log.d("MediaFileLocator", "Video directory does not exist: ${videoDir.absolutePath}")
            return null
        }
        
        val videoFile = findFileInDirectory(videoDir, gameFilename, VIDEO_EXTENSIONS)
        return videoFile?.absolutePath
    }
    
    /**
     * Find a file in a directory with support for subfolders and multiple extensions.
     * 
     * Search order:
     * 1. Subfolder with stripped name (e.g., "media/subfolder/game.png")
     * 2. Subfolder with raw name (e.g., "media/subfolder/game.zip.png")
     * 3. Root level with stripped name (e.g., "media/game.png")
     * 4. Root level with raw name (e.g., "media/game.zip.png")
     * 
     * @param dir The base directory to search in
     * @param fullPath The full game path/filename from ES-DE
     * @param extensions List of file extensions to try (without dots)
     * @return The file if found, null otherwise
     */
    private fun findFileInDirectory(
        dir: File,
        fullPath: String,
        extensions: List<String>
    ): File? {
        if (!dir.exists() || !dir.isDirectory) return null
        
        // Sanitize the filename (remove backslashes, get just filename part)
        val strippedName = sanitizeFilename(fullPath)
        val nameWithoutExt = strippedName.substringBeforeLast('.')
        
        // Get the raw filename (may still have extension)
        val rawName = fullPath.substringAfterLast("/")
        
        // Extract subfolder path if present
        val subfolderPath = extractSubfolderPath(fullPath)
        
        android.util.Log.d("MediaFileLocator", "Searching in: ${dir.absolutePath}")
        android.util.Log.d("MediaFileLocator", "  strippedName: $nameWithoutExt")
        android.util.Log.d("MediaFileLocator", "  rawName: $rawName")
        android.util.Log.d("MediaFileLocator", "  subfolderPath: $subfolderPath")
        
        // Try subfolder first if it exists
        if (subfolderPath != null) {
            val subDir = File(dir, subfolderPath)
            if (subDir.exists() && subDir.isDirectory) {
                val file = tryFindFileWithExtensions(subDir, nameWithoutExt, rawName, extensions)
                if (file != null) {
                    android.util.Log.d("MediaFileLocator", "Found in subfolder: ${file.absolutePath}")
                    return file
                }
            }
        }
        
        // Try root level
        val file = tryFindFileWithExtensions(dir, nameWithoutExt, rawName, extensions)
        if (file != null) {
            android.util.Log.d("MediaFileLocator", "Found in root: ${file.absolutePath}")
            return file
        }
        
        android.util.Log.d("MediaFileLocator", "File not found")
        return null
    }
    
    /**
     * Try to find a file with multiple name variations and extensions.
     * 
     * @param dir The directory to search in
     * @param strippedName The filename without extension
     * @param rawName The raw filename (may have extension)
     * @param extensions List of extensions to try
     * @return The file if found, null otherwise
     */
    private fun tryFindFileWithExtensions(
        dir: File,
        strippedName: String,
        rawName: String,
        extensions: List<String>
    ): File? {
        // Try both stripped name and raw name
        for (name in listOf(strippedName, rawName)) {
            for (ext in extensions) {
                val file = File(dir, "$name.$ext")
                if (file.exists()) {
                    return file
                }
            }
        }
        return null
    }
    
    /**
     * Sanitize a full game path to just the filename for media lookup.
     * 
     * Handles:
     * - Subfolders: "subfolder/game.zip" -> "game.zip"
     * - Backslashes: "game\file.zip" -> "gamefile.zip"
     * - Multiple path separators
     * 
     * @param fullPath The full path from ES-DE
     * @return The sanitized filename
     */
    private fun sanitizeFilename(fullPath: String): String {
        // Remove backslashes (screensaver case)
        var cleaned = fullPath.replace("\\", "")
        
        // Get just the filename (after last forward slash)
        cleaned = cleaned.substringAfterLast("/")
        
        return cleaned
    }
    
    /**
     * Extract the subfolder path from a full game path.
     * 
     * Examples:
     * - "subfolder/game.zip" -> "subfolder"
     * - "deep/nested/game.zip" -> "deep/nested"
     * - "game.zip" -> null
     * 
     * @param fullPath The full path from ES-DE
     * @return The subfolder path, or null if no subfolder
     */
    private fun extractSubfolderPath(fullPath: String): String? {
        // Get everything before the filename
        val beforeFilename = fullPath.substringBeforeLast("/", "")
        
        // Get just the immediate subfolder (everything after the system name)
        val subfolder = beforeFilename.substringAfterLast("/", "")
        
        return if (subfolder.isNotEmpty()) subfolder else null
    }
}
