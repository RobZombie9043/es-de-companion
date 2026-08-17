package com.esde.companion.domain.parser

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses ES-DE's `custom_systems/es_systems.xml` content to find a single system's
 * configured ROM directory (its `<path>`) - the same, ES-DE-authoritative information
 * `system-select`'s third fireEvent() argument reports (see GameMediaPathResolver's
 * kdoc for why this matters), just readable up front from disk instead of waited-for as
 * a log event.
 *
 * This file only contains entries for systems whose ROM path has actually been
 * customized away from ES-DE's standard layout - confirmed against a real device - not
 * a full baseline copy of every system. A `null` result (no matching `<system>`, no
 * `<path>`, or a `%ROMPATH%`-relative path with no [romDirectory][findSystemPath] to
 * substitute) is therefore a routine, expected outcome - the caller falls back to other
 * strategies.
 *
 * DOM-based rather than regex, unlike [EsdeSettingsParser] - `<system>` blocks repeat
 * with several same-named child tags each (multiple `<command>` elements per system),
 * which a flat regex can't reliably scope to just one system's own `<path>`.
 *
 * Deliberately takes raw XML [content] and a resolved `romDirectory` rather than a File
 * or any Android dependency, so this stays a pure, unit-testable function - reading (and
 * caching) the file, and reading `ROMDirectory` out of es_settings.xml, are data-layer
 * concerns.
 */
object EsSystemsXmlParser {
    private const val ROM_PATH_PLACEHOLDER = "%ROMPATH%"

    fun findSystemPath(
        content: String,
        systemShortName: String,
        romDirectory: String?,
    ): String? {
        val rawPath = findRawPath(content, systemShortName) ?: return null
        return substitutePlaceholder(rawPath, romDirectory)
    }

    private fun substitutePlaceholder(
        rawPath: String,
        romDirectory: String?,
    ): String? =
        when {
            !rawPath.contains(ROM_PATH_PLACEHOLDER) -> rawPath
            romDirectory.isNullOrBlank() -> null
            else -> rawPath.replace(ROM_PATH_PLACEHOLDER, romDirectory.trimEnd('/'))
        }

    private fun findRawPath(
        content: String,
        systemShortName: String,
    ): String? =
        parseDocument(content)
            ?.let { document -> findMatchingSystemElement(document, systemShortName) }
            ?.firstTextOf("path")

    private fun parseDocument(content: String): Document? =
        try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(InputSource(StringReader(content)))
        } catch (
            @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception,
        ) {
            // Deliberately broad: this parses a user-editable config file we don't
            // control, the same rationale as GameListDescriptionParser's gamelist.xml
            // parsing - any parse failure here should degrade to "no known path,"
            // never crash the app.
            null
        }

    private fun findMatchingSystemElement(
        document: Document,
        systemShortName: String,
    ): Element? {
        val systemNodes = document.getElementsByTagName("system")
        for (index in 0 until systemNodes.length) {
            val systemElement = systemNodes.item(index) as? Element ?: continue
            if (systemElement.firstTextOf("name") == systemShortName) return systemElement
        }
        return null
    }

    private fun Element.firstTextOf(tagName: String): String? {
        val nodes = getElementsByTagName(tagName)
        if (nodes.length == 0) return null
        return nodes.item(0).textContent?.trim()?.takeIf { it.isNotEmpty() }
    }
}
