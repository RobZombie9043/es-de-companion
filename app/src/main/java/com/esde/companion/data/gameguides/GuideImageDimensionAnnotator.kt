package com.esde.companion.data.gameguides

import android.graphics.BitmapFactory
import java.io.File

private val IMG_TAG_REGEX = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
private val SRC_ATTR_REGEX = Regex("""\bsrc\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
private val HAS_WIDTH_ATTR_REGEX = Regex("""\bwidth\s*=""", RegexOption.IGNORE_CASE)
private val IMG_OPEN_TAG_REGEX = Regex("<img", RegexOption.IGNORE_CASE)

/**
 * Injects real `width`/`height` attributes into every `<img>` in [bodyHtml] whose dimensions can
 * be resolved locally, so the browser reserves the correct layout space before the image has
 * actually decoded - see `GameGuideHtmlViewer`'s kdoc for why this replaces waiting for images
 * to finish loading before trusting `scrollHeight`/`scrollIntoView`. A tag that already has a
 * `width` attribute is left untouched (idempotent, and forward-compatible if dimensions are ever
 * also baked in at save time). A `src` that doesn't resolve to a real local file, or fails to
 * decode, just leaves that one tag unmodified - graceful, per-image degradation, not a whole-page
 * failure; the caller already tolerates some images not having known dimensions by falling back
 * to its own layout-stability wait.
 *
 * [readImageDimensions] defaults to [readBitmapBounds] (real file IO) but is overridable so this
 * function itself stays plain-Kotlin unit-testable without touching Android framework classes -
 * see `GuideImageDimensionAnnotatorTest`.
 */
fun annotateImageDimensions(
    bodyHtml: String,
    mediaDirectoryPath: String,
    readImageDimensions: (File) -> Pair<Int, Int>? = ::readBitmapBounds,
): String =
    IMG_TAG_REGEX.replace(bodyHtml) { match ->
        val tag = match.value
        if (HAS_WIDTH_ATTR_REGEX.containsMatchIn(tag)) {
            tag
        } else {
            val src = SRC_ATTR_REGEX.find(tag)?.groupValues?.get(1)
            val dimensions = src?.let { readImageDimensions(File(mediaDirectoryPath, it)) }
            if (dimensions == null) {
                tag
            } else {
                val (width, height) = dimensions
                tag.replaceFirst(IMG_OPEN_TAG_REGEX, "<img width=\"$width\" height=\"$height\"")
            }
        }
    }

/**
 * Header-only decode ([BitmapFactory.Options.inJustDecodeBounds]) - no pixel data is actually
 * read, just the image container's declared dimensions, cheap enough to do for every image on a
 * page load. Null on any failure (missing file, corrupt/unsupported format, ...).
 *
 * Not unit tested - real Android `BitmapFactory` calls aren't exercisable under this project's
 * plain JVM unit tests (no Robolectric shadowing configured; the same gap `NativeImageDownloader`'s
 * own `BitmapFactory` usage already has). [annotateImageDimensions]'s `readImageDimensions`
 * parameter is what keeps the surrounding logic testable despite that.
 */
fun readBitmapBounds(file: File): Pair<Int, Int>? {
    if (!file.isFile) return null
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, options)
    val width = options.outWidth
    val height = options.outHeight
    return if (width > 0 && height > 0) width to height else null
}
