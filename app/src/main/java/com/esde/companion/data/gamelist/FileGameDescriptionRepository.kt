package com.esde.companion.data.gamelist

import com.esde.companion.domain.model.GameDescription
import com.esde.companion.domain.parser.GameListDescriptionParser
import com.esde.companion.domain.repository.GameDescriptionRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads and parses "<ES-DE root>/gamelists/<systemShortName>/gamelist.xml" to find a
 * game's <desc>. [esdeRootPath] is the ES-DE root folder (the same one es_log.txt lives
 * under - see ReactiveEsdeLogRepository), not the gamelists folder directly, so callers
 * deal with the one folder the user actually picked during onboarding.
 *
 * Caches each system's raw file content in memory, keyed by the gamelist file's
 * lastModified() timestamp - gamelist.xml files can run to several MB for large
 * collections, and re-parsing the whole document on every game switch (which happens far
 * more often than the file itself changes, i.e. only when ES-DE's scraper runs) would be
 * wasteful. A stale entry is corrected automatically the next time the timestamp is
 * checked, no manual invalidation needed.
 */
class FileGameDescriptionRepository(
    private val esdeRootPath: String,
) : GameDescriptionRepository {

    private val cache = mutableMapOf<String, CacheEntry>()

    override suspend fun resolveDescription(systemShortName: String, romPath: String): GameDescription =
        withContext(Dispatchers.IO) {
            val file = File(esdeRootPath, "gamelists/$systemShortName/gamelist.xml")
            if (!file.isFile) return@withContext GameDescription(text = null)

            val lastModified = file.lastModified()
            val cached = cache[systemShortName]
            val content = if (cached != null && cached.lastModified == lastModified) {
                cached.content
            } else {
                file.readText().also { cache[systemShortName] = CacheEntry(lastModified, it) }
            }

            GameDescription(text = GameListDescriptionParser.findDescription(content, romPath))
        }

    private data class CacheEntry(val lastModified: Long, val content: String)
}