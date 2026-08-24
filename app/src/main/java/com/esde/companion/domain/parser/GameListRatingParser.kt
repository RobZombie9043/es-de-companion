package com.esde.companion.domain.parser

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses ES-DE's gamelist.xml content to find the <rating> value for one specific game.
 * Same matching/parsing shape as GameListDescriptionParser (see its kdoc for the <path>
 * matching rules and why raw XML content is taken directly rather than a File) - kept as
 * its own standalone object rather than sharing GameListDescriptionParser's internals, the
 * same per-feature independence GameDescription/GameMedia already follow elsewhere in this
 * codebase.
 *
 * ES-DE stores <rating> as a decimal string from 0.0 to 1.0 (e.g. "0.800000"). Returns
 * null for content that fails to parse, a game with no matching <path>, a matching game
 * with no (or blank) <rating>, or a <rating> value that doesn't parse as a number - all
 * routine, expected outcomes, not errors. A value outside 0f..1f (shouldn't happen with a
 * well-formed gamelist.xml, but not something this parser should trust blindly) is clamped
 * into range rather than rejected.
 */
object GameListRatingParser {
    fun findRating(
        content: String,
        romPath: String,
    ): Float? {
        val gameListXml = content.extractGameListElement() ?: return null

        val document =
            try {
                DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(InputSource(StringReader(gameListXml)))
            } catch (
                @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
            ) {
                // Deliberately broad: this parses external, scraper-generated XML we don't
                // control - see GameListDescriptionParser's matching catch for why any parse
                // failure here should degrade to "no rating," never crash the app.
                return null
            }

        val gameNodes = document.getElementsByTagName("game")
        for (index in 0 until gameNodes.length) {
            val gameElement = gameNodes.item(index) as? Element ?: continue
            val path = gameElement.firstTextOf("path") ?: continue
            if (!romPath.matchesGamelistPath(path)) continue
            return gameElement.firstTextOf("rating")?.toFloatOrNull()?.coerceIn(0f, 1f)
        }
        return null
    }

    /** See GameListDescriptionParser.extractGameListElement's kdoc - same <gameList>-only
     * slicing to tolerate a stray sibling root element ES-DE sometimes writes. */
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
