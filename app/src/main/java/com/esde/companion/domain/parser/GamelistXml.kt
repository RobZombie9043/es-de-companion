package com.esde.companion.domain.parser

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Shared gamelist.xml quirk-handling used by both [GameListParser] (single-game field lookup)
 * and [GamelistBulkParser] (whole-file enumeration), so the XML peculiarities below only have
 * to be gotten right once. See [GameListParser]'s kdoc for the reasoning behind each of these.
 */
internal object GamelistXml {
    /** Slices out just the `<gameList>` element (see [extractGameListElement]) and parses it,
     * degrading to null on any parse failure rather than throwing - malformed/unexpected
     * scraper-generated XML is a routine, expected outcome here, not an error. */
    fun parseGameListDocument(content: String): Document? {
        val gameListXml = content.extractGameListElement() ?: return null
        return try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(InputSource(StringReader(gameListXml)))
        } catch (
            @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
        ) {
            null
        }
    }

    /**
     * ES-DE sometimes writes a sibling <alternativeEmulator> element before <gameList> at
     * the document root - e.g. when a system has a per-game emulator override configured.
     * That makes the file invalid XML (two root elements), which a strict parser refuses
     * to parse at all. Since <gameList> is the only element callers care about, slicing out
     * just that element's text - and ignoring whatever ES-DE prepends or appends outside it -
     * sidesteps the problem entirely rather than needing to parse (or tolerate parse failures
     * on) content that would be discarded anyway.
     */
    private fun String.extractGameListElement(): String? {
        val start = indexOf("<gameList")
        if (start == -1) return null
        val endTag = "</gameList>"
        val end = indexOf(endTag, start)
        if (end == -1) return null
        return substring(start, end + endTag.length)
    }

    /**
     * ES-DE stores each game's <path> as "./<relative path>" relative to that system's ROM
     * folder - e.g. "./Cosmic Smash (Japan).chd" - not the absolute romPath this app tracks
     * elsewhere (see GameMediaPathResolver). Matching is done by stripping the leading "./"
     * and checking whether the receiver (an absolute romPath) ends with that relative path,
     * rather than comparing full paths or bare filenames - the latter would incorrectly match
     * two different games in different subfolders that happen to share a filename.
     */
    fun String.matchesGamelistPath(gamelistPath: String): Boolean {
        val relative = gamelistPath.removePrefix("./")
        return this == relative || this.endsWith("/$relative")
    }

    fun Element.firstTextOf(tagName: String): String? {
        val nodes = getElementsByTagName(tagName)
        if (nodes.length == 0) return null
        return nodes.item(0).textContent?.trim()
    }
}
