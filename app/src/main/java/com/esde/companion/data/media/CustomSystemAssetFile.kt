package com.esde.companion.data.media

import java.io.File

/**
 * Extensions accepted when matching a `<systemShortName>.<ext>` file in a user-supplied
 * custom folder (Custom System Images / Custom Logos folder, Settings > Setup). Broader
 * than MediaType's own IMAGE_EXTENSIONS - these are user-dropped single files rather than
 * ES-DE's downloaded_media convention, and gif/webp cover the animated-image ask (see
 * CompanionApplication's AnimatedImageDecoder registration for how those actually animate).
 * Priority order matters: if a system somehow has more than one match, the first wins,
 * so behavior is deterministic rather than filesystem-iteration-order-dependent.
 */
internal val CUSTOM_ASSET_EXTENSIONS = listOf("png", "jpg", "jpeg", "webp", "gif")

/**
 * Looks for `<folderPath>/<systemShortName>.<ext>` across [CUSTOM_ASSET_EXTENSIONS].
 * Not recursive - these are flat "one file per system" folders, unlike downloaded_media's
 * per-system subfolder layout (see FileSystemMediaRepository).
 */
internal fun findSystemAssetFile(folderPath: String, systemShortName: String): String? {
    val folder = File(folderPath)
    if (!folder.isDirectory) return null
    return CUSTOM_ASSET_EXTENSIONS
        .map { ext -> File(folder, "$systemShortName.$ext") }
        .firstOrNull { it.isFile }
        ?.absolutePath
}