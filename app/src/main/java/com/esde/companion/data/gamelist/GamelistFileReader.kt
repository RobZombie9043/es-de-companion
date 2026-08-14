package com.esde.companion.data.gamelist

import com.esde.companion.domain.parser.LegacyGamelistPathResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** A gamelist.xml file's raw text, plus the absolute path it was read from. */
data class GamelistFile(val path: String, val content: String)

/**
 * Locates and reads gamelist.xml's raw text, checking the standard ES-DE location first -
 * "<ES-DE root>/gamelists/<systemShortName>/gamelist.xml" - then falling back to the
 * legacy, ROMs-adjacent location ES-DE uses when its own "LegacyGamelistFileLocation"
 * setting is enabled (see LegacyGamelistPathResolver). [esdeRootPath] is the ES-DE root
 * folder (the same one es_log.txt lives under), not the gamelists folder directly, so
 * callers deal with the one folder the user actually picked during onboarding - read fresh
 * (via Flow.first()) on every [read] call so a root change mid-session is picked up without
 * this needing to be reconstructed.
 *
 * Caches each resolved gamelist file's raw content in memory, keyed by its absolute path
 * and lastModified() timestamp - gamelist.xml files can run to several MB for large
 * collections, and re-parsing the whole document on every game switch (which happens far
 * more often than the file itself changes, i.e. only when ES-DE's scraper runs) would be
 * wasteful. A stale entry is corrected automatically the next time the timestamp is
 * checked, no manual invalidation needed. One instance of this class is shared, long-lived,
 * across every consumer that needs gamelist.xml content (description lookup, ROM-hash
 * lookup - see AppContainer), so there's exactly one cached copy of each file's text, not
 * one per consumer.
 *
 * [cache] is a ConcurrentHashMap rather than a plain map since [read] runs on
 * Dispatchers.IO (a multi-threaded pool) and two calls in flight at once (e.g. fast
 * browsing outpacing IO, or the main widgets screen and an edit-mode preview both resolving
 * around the same time) can genuinely land on different threads - each entry is independent
 * (no cross-key invariant to protect), so a concurrent map is a complete fix with no
 * Mutex/single-writer needed.
 *
 * Tracks the last-seen root and clears [cache] on a root change, so switching ES-DE root
 * (Settings > Setup) doesn't retain the old root's (possibly multi-MB) cached text
 * indefinitely.
 */
class GamelistFileReader(
    private val esdeRootPath: Flow<String?>,
) {
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    @Volatile
    private var lastRoot: String? = null

    suspend fun read(
        systemShortName: String,
        romPath: String,
    ): GamelistFile? =
        withContext(Dispatchers.IO) {
            val root = esdeRootPath.first() ?: return@withContext null
            if (root != lastRoot) {
                cache.clear()
                lastRoot = root
            }

            val file = resolveGamelistFile(root, systemShortName, romPath) ?: return@withContext null
            val lastModified = file.lastModified()
            val cached = cache[file.path]
            val content =
                if (cached != null && cached.lastModified == lastModified) {
                    cached.content
                } else {
                    file.readText().also { cache[file.path] = CacheEntry(lastModified, it) }
                }

            GamelistFile(path = file.path, content = content)
        }

    private fun resolveGamelistFile(
        root: String,
        systemShortName: String,
        romPath: String,
    ): File? {
        val standard = File(root, "gamelists/$systemShortName/gamelist.xml")
        if (standard.isFile) return standard

        val legacyPath = LegacyGamelistPathResolver.resolvePath(systemShortName, romPath) ?: return null
        return File(legacyPath).takeIf { it.isFile }
    }

    private data class CacheEntry(val lastModified: Long, val content: String)
}
