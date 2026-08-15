package com.esde.companion.domain.model

private val IMAGE_EXTENSIONS = listOf("jpg", "png", "webp")
private val VIDEO_EXTENSIONS = listOf("mp4", "mkv", "avi", "wmv", "mov", "webm", "m4v")
private val MANUAL_EXTENSIONS = listOf("pdf")

/**
 * The media subfolder types ES-DE's downloaded_media layout defines, each with the file
 * extensions valid for that type. [folderName] is the literal on-disk folder name under
 * `downloaded_media/<systemShortName>/`.
 */
enum class MediaType(val folderName: String, val validExtensions: List<String>) {
    ThreeDBoxes("3dboxes", IMAGE_EXTENSIONS),
    BackCovers("backcovers", IMAGE_EXTENSIONS),
    Covers("covers", IMAGE_EXTENSIONS),
    Custom("custom", IMAGE_EXTENSIONS),
    FanArt("fanart", IMAGE_EXTENSIONS),
    Manuals("manuals", MANUAL_EXTENSIONS),
    Marquees("marquees", IMAGE_EXTENSIONS),
    MixImages("miximages", IMAGE_EXTENSIONS),
    PhysicalMedia("physicalmedia", IMAGE_EXTENSIONS),
    Screenshots("screenshots", IMAGE_EXTENSIONS),
    TitleScreens("titlescreens", IMAGE_EXTENSIONS),
    Videos("videos", VIDEO_EXTENSIONS),
}
