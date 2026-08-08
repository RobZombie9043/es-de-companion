package com.esde.companion.domain.parser

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses ES-DE's gamelist.xml content to find the <desc> text for one specific game.
 *
 * ES-DE stores each game's <path> as "./<relative path>" relative to that system's ROM
 * folder - e.g. "./Cosmic Smash (Japan).chd", or "./subfolder/Name.zip" for games in
 * subfolders - not the absolute romPath this app tracks elsewhere (see
 * GameMediaPathResolver). Matching is therefore done by stripping the leading "./" and
 * checking whether the caller's absolute [romPath] ends with that relative path, rather
 * than comparing full paths or bare filenames - the latter would incorrectly match two
 * different games in different subfolders that happen to share a filename.
 *
 * Deliberately takes raw XML [content] rather than a File, so this stays a pure,
 * Android-free function unit-testable against fixture XML with no filesystem required -
 * reading (and caching) the file is a data-layer concern, see
 * FileGameDescriptionRepository.
 *
 * Returns null for content that fails to parse (unexpected/corrupt gamelist.xml), a game
 * with no matching <path>, or a matching game with no (or blank) <desc> - all routine,
 * expected outcomes here, not errors.
 */
object GameListDescriptionParser {
    fun findDescription(
        content: String,
        romPath: String,
    ): String? {
        val gameListXml = content.extractGameListElement() ?: return null

        val document =
            try {
                DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(InputSource(StringReader(gameListXml)))
            } catch (
                @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
            ) {
                // Deliberately broad: this parses external, scraper-generated XML we don't
                // control. Malformed content (e.g. an unescaped "&" in a <desc>) can surface
                // as checked exceptions (SAXException) or unchecked ones (DOMException, seen
                // in practice on-device) depending on the platform XML implementation - any
                // parse failure here should degrade to "no description," never crash the app.
                return null
            }

        val gameNodes = document.getElementsByTagName("game")
        for (index in 0 until gameNodes.length) {
            val gameElement = gameNodes.item(index) as? Element ?: continue
            val path = gameElement.firstTextOf("path") ?: continue
            if (!romPath.matchesGamelistPath(path)) continue
            return gameElement.firstTextOf("desc")?.takeIf { it.isNotBlank() }
        }
        return null
    }

    /**
     * ES-DE sometimes writes a sibling <alternativeEmulator> element before <gameList> at
     * the document root - e.g. when a system has a per-game emulator override configured.
     * That makes the file invalid XML (two root elements), which a strict parser refuses
     * to parse at all. Since <gameList> is the only element this parser cares about,
     * slicing out just that element's text - and ignoring whatever ES-DE prepends or
     * appends outside it - sidesteps the problem entirely rather than needing to parse
     * (or tolerate parse failures on) content we'd discard anyway.
     */
    private fun String.extractGameListElement(): String? {
        val start = indexOf("<gameList")
        if (start == -1) return null
        val endTag = "</gameList>"
        val end = indexOf(endTag, start)
        if (end == -1) return null
        return substring(start, end + endTag.length)
    }

    private fun String.matchesGamelistPath(gamelistPath: String): Boolean {
        val relative = gamelistPath.removePrefix("./")
        return this == relative || this.endsWith("/$relative")
    }

    private fun Element.firstTextOf(tagName: String): String? {
        val nodes = getElementsByTagName(tagName)
        if (nodes.length == 0) return null
        return nodes.item(0).textContent?.trim()
    }
}
