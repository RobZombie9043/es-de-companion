package com.esde.companion.domain.parser

import com.esde.companion.domain.parser.GamelistXml.firstTextOf
import com.esde.companion.domain.parser.GamelistXml.matchesGamelistPath
import org.w3c.dom.Element

/**
 * Parses ES-DE's gamelist.xml content to find per-game fields for one specific game -
 * the <desc> text ([findDescription]), the <rating> value ([findRating]), and, in
 * preparation for ES-DE's upcoming ROM-hash support, a ROM-hash field ([findRomHash]). All
 * three share the same document-locate-and-match core ([findGameField]), so the XML-quirk
 * handling below only has to be gotten right once.
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
 * reading (and caching) the file is a data-layer concern, see GamelistFileReader.
 *
 * Returns null for content that fails to parse (unexpected/corrupt gamelist.xml), a game
 * with no matching <path>, or a matching game with no (or blank) requested field - all
 * routine, expected outcomes here, not errors.
 */
object GameListParser {
    fun findDescription(
        content: String,
        romPath: String,
    ): String? = findGameField(content, romPath, DESCRIPTION_TAG)

    fun findRomHash(
        content: String,
        romPath: String,
    ): String? = findGameField(content, romPath, ROM_HASH_TAG)

    /** ES-DE stores <rating> as a decimal string from 0.0 to 1.0 (e.g. "0.800000") - unlike
     * [findDescription]/[findRomHash]'s opaque strings, this converts to the numeric value
     * callers actually need, clamping into range rather than trusting a well-formed
     * gamelist.xml blindly. */
    fun findRating(
        content: String,
        romPath: String,
    ): Float? = findGameField(content, romPath, RATING_TAG)?.toFloatOrNull()?.coerceIn(0f, 1f)

    private fun findGameField(
        content: String,
        romPath: String,
        tagName: String,
    ): String? {
        val document = GamelistXml.parseGameListDocument(content) ?: return null

        val gameNodes = document.getElementsByTagName("game")
        return (0 until gameNodes.length).asSequence()
            .mapNotNull { gameNodes.item(it) as? Element }
            .firstOrNull { element ->
                val path = element.firstTextOf("path")
                path != null && romPath.matchesGamelistPath(path)
            }
            ?.firstTextOf(tagName)
            ?.takeIf { it.isNotBlank() }
    }

    private const val DESCRIPTION_TAG = "desc"
    private const val RATING_TAG = "rating"

    @Suppress("ForbiddenComment")
    // TODO: ES-DE has not shipped ROM-hash output in gamelist.xml yet, so this tag name is a
    //  placeholder. Update it (and GameListParserTest's fixtures) to the real element name once
    //  ES-DE ships the field. Nothing else in the hash pipeline hard-codes a tag name.
    private const val ROM_HASH_TAG = "hash"
}
