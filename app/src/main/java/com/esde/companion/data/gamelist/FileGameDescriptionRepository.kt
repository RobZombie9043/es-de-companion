package com.esde.companion.data.gamelist

import com.esde.companion.domain.model.GameDescription
import com.esde.companion.domain.parser.GameListDescriptionParser
import com.esde.companion.domain.parser.LegacyGamelistPathResolver
import com.esde.companion.domain.repository.GameDescriptionRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads and parses gamelist.xml to find a game's <desc>, checking the standard ES-DE
 * location first - "<ES-DE root>/gamelists/<systemShortName>/gamelist.xml" - then falling
 * back to the legacy, ROMs-adjacent location ES-DE uses when its own
 * "LegacyGamelistFileLocation" setting is enabled (see LegacyGamelistPathResolver).
 * [esdeRootPath] is the ES-DE root folder (the same one es_log.txt lives under - see
 * ReactiveEsdeLogRepository), not the gamelists folder directly, so callers deal with the
 * one folder the user actually picked during onboarding.
 *
 * Caches each resolved gamelist file's raw content in memory, keyed by its absolute path
 * and lastModified() timestamp - gamelist.xml files can run to several MB for large
 * collections, and re-parsing the whole document on every game switch (which happens far
 * more often than the file itself changes, i.e. only when ES-DE's scraper runs) would be
 * wasteful. Keyed by path rather than systemShortName since which file applies (standard
 * vs legacy) is resolved fresh each call. A stale entry is corrected automatically the
 * next time the timestamp is checked, no manual invalidation needed.
 */
class FileGameDescriptionRepository(
    private val esdeRootPath: String,
) : GameDescriptionRepository {

    private val cache = mutableMapOf<String, CacheEntry>()

    override suspend fun resolveDescription(systemShortName: String, romPath: String): GameDescription =
        withContext(Dispatchers.IO) {
            val file = resolveGamelistFile(systemShortName, romPath) ?: return@withContext GameDescription(text = null)

            val lastModified = file.lastModified()
            val cached = cache[file.path]
            val content = if (cached != null && cached.lastModified == lastModified) {
                cached.content
            } else {
                file.readText().also { cache[file.path] = CacheEntry(lastModified, it) }
            }

            GameDescription(text = GameListDescriptionParser.findDescription(content, romPath))
        }

    private fun resolveGamelistFile(systemShortName: String, romPath: String): File? {
        val standard = File(esdeRootPath, "gamelists/$systemShortName/gamelist.xml")
        if (standard.isFile) return standard

        val legacyPath = LegacyGamelistPathResolver.resolvePath(systemShortName, romPath) ?: return null
        return File(legacyPath).takeIf { it.isFile }
    }

    private data class CacheEntry(val lastModified: Long, val content: String)
}