package com.esde.companion.data.media

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.svg.SvgDecoder
import coil3.svg.isSvg
import java.io.File
import okio.FileSystem
import okio.Path.Companion.toOkioPath

private const val MIME_TYPE_SVG = "image/svg+xml"

/** Below this, an SVG's own coordinate system (viewBox, or width/height when there's no
 * viewBox) is considered "small" and gets grown - see [normalizeSmallViewBoxSvg]. */
internal const val SMALL_VIEW_BOX_THRESHOLD = 1000.0

/** Scale factor is chosen so a normalized SVG's longer edge lands here. */
internal const val NORMALIZED_LONG_EDGE = 3000.0

/** The opening `<svg ...>` tag - and the viewBox/width/height attributes on it - is always
 * within this many bytes of the start of any realistic SVG document, even one with a large
 * DOCTYPE/entity preamble. Bounds the peek in [decode] so deciding whether normalization is
 * needed never requires reading a whole (possibly huge) document. */
private const val OPEN_TAG_PEEK_BYTES = 16_384L

/**
 * Wraps [SvgDecoder] to work around a real rendering-quality issue in the androidsvg
 * library it delegates to: curve-flattening tolerance is computed in the SVG's own
 * coordinate space, not relative to the final raster size. An SVG authored with a tiny
 * coordinate system - a bundled system logo exported at e.g. 168x62, or a small icon a
 * user picks via the Custom Image widget - ends up visibly faceted once rasterized up to
 * real widget size, even though the decoded bitmap itself lands at the correct pixel
 * dimensions.
 *
 * Bundled system logo assets can be fixed once, by hand, at the source (and have been -
 * see git history). User-supplied images can't be: anything picked through the Custom
 * Image widget's file picker is read live from the user's own path on shared storage (see
 * SafPathResolver's kdoc) on every render, and rewriting the user's own file in place would
 * be destructive. Normalizing here, in the decode pipeline, fixes both cases - and any
 * future one, including a bundled logo getting re-exported back down to a small viewBox -
 * without needing another one-off asset pass.
 *
 * Registered ahead of the plain [SvgDecoder.Factory] in
 * [com.esde.companion.CompanionApplication] so every SVG this app decodes goes through it.
 * Only ever rewrites XML text before handing off to a real [SvgDecoder] for the actual
 * rasterization - this class has no rendering logic of its own.
 *
 * The rewritten document is handed to the inner [SvgDecoder] via a real temp file, not an
 * in-memory buffer - androidsvg reads an [ImageSource] more than once internally (once to
 * size the canvas, again to render), and Okio's [okio.Buffer] is a one-shot read: a second
 * read after the first drains it finds nothing, which androidsvg reports as "SVG document is
 * empty". This never showed up while testing against small bundled logos (their content
 * fits inside Okio's single ~8KB segment, which some internal copy path apparently
 * tolerates re-reading) but reproduces reliably on a large real-world SVG. A temp file
 * sidesteps the whole question - it can be read from the start as many times as needed,
 * matching how every other SVG this app decodes (a real bundled asset or a real
 * user-picked file) already works.
 */
class NormalizingSvgDecoder(
    private val result: SourceFetchResult,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult? {
        val prefix = run {
            val peeked = result.source.source().peek()
            peeked.request(OPEN_TAG_PEEK_BYTES)
            peeked.buffer.readUtf8(minOf(OPEN_TAG_PEEK_BYTES, peeked.buffer.size))
        }

        if (!prefixNeedsNormalization(prefix)) {
            return SvgDecoder(source = result.source, options = options).decode()
        }

        val originalXml = result.source.source().use { it.readUtf8() }
        val normalizedXml = normalizeSmallViewBoxSvg(originalXml) ?: originalXml

        val tempFile = File.createTempFile("normalized_svg_", ".svg", options.context.cacheDir)
        try {
            tempFile.writeText(normalizedXml, Charsets.UTF_8)
            val tempSource = ImageSource(file = tempFile.toOkioPath(), fileSystem = FileSystem.SYSTEM)
            return SvgDecoder(source = tempSource, options = options).decode()
        } finally {
            tempFile.delete()
        }
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            val isSvg = result.mimeType == MIME_TYPE_SVG || DecodeUtils.isSvg(result.source.source())
            if (!isSvg) return null
            return NormalizingSvgDecoder(result, options)
        }
    }
}

private val OPEN_TAG_REGEX = Regex("""<svg\b[^>]*>""")
private val VIEW_BOX_ATTR_REGEX = Regex("""\bviewBox\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
private val WIDTH_ATTR_REGEX = Regex("""\bwidth\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
private val HEIGHT_ATTR_REGEX = Regex("""\bheight\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)

private data class ViewBox(val minX: Double, val minY: Double, val width: Double, val height: Double)

/** Cheap check against just the leading bytes of a document (see [OPEN_TAG_PEEK_BYTES]) -
 * whether the full [normalizeSmallViewBoxSvg] pass would find anything to do, without
 * requiring the whole document to be read first. */
private fun prefixNeedsNormalization(prefix: String): Boolean {
    val openTag = OPEN_TAG_REGEX.find(prefix)?.value ?: return false
    return dimensionsOf(openTag)?.let { (w, h) -> scaleFor(w, h) != null } ?: false
}

/**
 * Returns [xml] with its coordinate system grown by a uniform scale (wrapping all content
 * in a `<g transform="scale(...)">` and enlarging its viewBox, or its width/height when
 * there's no viewBox) if that coordinate system's longer edge is below
 * [SMALL_VIEW_BOX_THRESHOLD]. Returns null - not the unchanged input - when nothing needed
 * to change (already large, or the document couldn't be parsed), so callers can tell
 * "normalized" apart from "passed through" without a redundant string comparison.
 *
 * A uniform scale on both the viewBox and every coordinate inside it is a no-op for how
 * the SVG actually renders - it only changes the units the document's geometry is
 * expressed in, which is exactly the lever androidsvg's curve flattening is sensitive to.
 */
internal fun normalizeSmallViewBoxSvg(xml: String): String? {
    val openTagMatch = OPEN_TAG_REGEX.find(xml) ?: return null
    val openTag = openTagMatch.value
    val closeIndex = xml.lastIndexOf("</svg>")
    if (closeIndex == -1 || closeIndex < openTagMatch.range.last) return null

    val viewBox = VIEW_BOX_ATTR_REGEX.find(openTag)?.groupValues?.get(1)?.let(::parseViewBox)
    val newOpenTag: String
    val scale: Double

    if (viewBox != null) {
        scale = scaleFor(viewBox.width, viewBox.height) ?: return null
        val newViewBox =
            "${viewBox.minX * scale} ${viewBox.minY * scale} ${viewBox.width * scale} ${viewBox.height * scale}"
        newOpenTag = setAttr(openTag, "viewBox", newViewBox)
    } else {
        val width = WIDTH_ATTR_REGEX.find(openTag)?.groupValues?.get(1)?.let(::parseUnitless) ?: return null
        val height = HEIGHT_ATTR_REGEX.find(openTag)?.groupValues?.get(1)?.let(::parseUnitless) ?: return null
        scale = scaleFor(width, height) ?: return null
        newOpenTag = setAttr(setAttr(openTag, "width", width * scale), "height", height * scale)
    }

    val body = xml.substring(openTagMatch.range.last + 1, closeIndex)
    return xml.substring(0, openTagMatch.range.first) +
        newOpenTag +
        "\n<g transform=\"scale($scale)\">" +
        body +
        "</g>\n</svg>\n"
}

/** [width]/[height] of an `<svg>` opening tag, from its viewBox if present, else its raw
 * width/height attributes. Null if neither can be parsed. */
private fun dimensionsOf(openTag: String): Pair<Double, Double>? {
    val viewBox = VIEW_BOX_ATTR_REGEX.find(openTag)?.groupValues?.get(1)?.let(::parseViewBox)
    if (viewBox != null) return viewBox.width to viewBox.height

    val width = WIDTH_ATTR_REGEX.find(openTag)?.groupValues?.get(1)?.let(::parseUnitless) ?: return null
    val height = HEIGHT_ATTR_REGEX.find(openTag)?.groupValues?.get(1)?.let(::parseUnitless) ?: return null
    return width to height
}

private fun scaleFor(width: Double, height: Double): Double? {
    val longEdge = maxOf(width, height)
    if (longEdge <= 0.0 || longEdge >= SMALL_VIEW_BOX_THRESHOLD) return null
    return NORMALIZED_LONG_EDGE / longEdge
}

private fun parseViewBox(value: String): ViewBox? {
    val parts = value.trim().split(Regex("""[\s,]+""")).mapNotNull { it.toDoubleOrNull() }
    if (parts.size != 4) return null
    return ViewBox(minX = parts[0], minY = parts[1], width = parts[2], height = parts[3])
}

private fun parseUnitless(value: String): Double? = value.removeSuffix("px").toDoubleOrNull()

private fun setAttr(tag: String, name: String, value: Double): String = setAttr(tag, name, value.toString())

private fun setAttr(tag: String, name: String, value: String): String {
    val attrRegex = Regex("""(\b$name\s*=\s*")([^"]*)(")""", RegexOption.IGNORE_CASE)
    return if (attrRegex.containsMatchIn(tag)) {
        attrRegex.replace(tag) { "${it.groupValues[1]}$value${it.groupValues[3]}" }
    } else {
        tag.replaceFirst("<svg", "<svg $name=\"$value\"")
    }
}
