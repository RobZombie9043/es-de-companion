package com.esde.companion.domain.parser

import com.esde.companion.domain.parser.GamelistXml.firstTextOf
import org.w3c.dom.Element
import org.w3c.dom.Node

/** One `<game>` entry from a gamelist.xml, as needed for whole-file enumeration - the ES-DE
 * relative path (see [GamelistXml.matchesGamelistPath]) and its display name. */
data class GamelistGameEntry(val relativeRomPath: String, val name: String)

/**
 * Parses every `<game>` entry out of a gamelist.xml's content, for features that need to browse
 * a system's whole game list (e.g. Game Launch Override) rather than look up one specific game
 * (see [GameListParser]). Shares [GameListParser]'s document-locate/parse quirk handling via
 * [GamelistXml] so that logic is only implemented once.
 *
 * Pure Kotlin, no Android dependency - unit-testable against fixture XML with no filesystem
 * required, same as [GameListParser]. Returns an empty list for content that fails to parse or
 * has no `<game>` entries, and skips any `<game>` node with no (or blank) `<path>` - both
 * routine, expected outcomes, not errors.
 */
object GamelistBulkParser {
    fun parseAllGames(content: String): List<GamelistGameEntry> {
        val document = GamelistXml.parseGameListDocument(content) ?: return emptyList()
        val gameNodes = document.getElementsByTagName("game")
        return (0 until gameNodes.length).mapNotNull { index -> parseEntry(gameNodes.item(index)) }
    }

    private fun parseEntry(node: Node?): GamelistGameEntry? {
        val element = node as? Element ?: return null
        return element.firstTextOf(PATH_TAG)?.takeIf { it.isNotBlank() }?.let { path ->
            val name = element.firstTextOf(NAME_TAG)?.takeIf { it.isNotBlank() } ?: path.removePrefix("./")
            GamelistGameEntry(relativeRomPath = path, name = name)
        }
    }

    private const val PATH_TAG = "path"
    private const val NAME_TAG = "name"
}
