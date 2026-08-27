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
            val root = refreshRootAndClearCacheIfChanged() ?: return@withContext null
            val file = resolveGamelistFile(root, systemShortName, romPath) ?: return@withContext null
            readAndCache(file)
        }

    /**
     * Reads a system's gamelist.xml without an already-known ROM path, for whole-library
     * enumeration (see [com.esde.companion.domain.repository.GamelistLibraryRepository]).
     * Standard location only - see [listSystemShortNames]'s kdoc for why the legacy,
     * ROMs-adjacent location can't be resolved from a cold enumeration.
     */
    suspend fun readSystem(systemShortName: String): GamelistFile? =
        withContext(Dispatchers.IO) {
            val root = refreshRootAndClearCacheIfChanged() ?: return@withContext null
            val candidate = File(root, "gamelists/$systemShortName/gamelist.xml")
            val file = candidate.takeIf { it.isFile } ?: return@withContext null
            readAndCache(file)
        }

    /**
     * Every system short name with a gamelist.xml at the standard location - i.e. every
     * subdirectory of "<ES-DE root>/gamelists/" that directly contains one. Deliberately does
     * not also look for the legacy, ROMs-adjacent location: unlike [read]'s fallback (which
     * always has a concrete romPath from [com.esde.companion.domain.model.AppState] to anchor
     * [LegacyGamelistPathResolver] on), a cold "list every system" enumeration has no ROM path
     * for any system yet - and this app has no separately configured ROMs root to search
     * instead (see [LegacyGamelistPathResolver]'s own kdoc). A system whose gamelist.xml only
     * exists at the legacy location is simply not discoverable by this method.
     */
    suspend fun listSystemShortNames(): List<String> =
        withContext(Dispatchers.IO) {
            val root = esdeRootPath.first() ?: return@withContext emptyList()
            File(root, "gamelists")
                .listFiles { candidate -> candidate.isDirectory && File(candidate, "gamelist.xml").isFile }
                ?.map { it.name }
                ?.sorted()
                ?: emptyList()
        }

    private suspend fun refreshRootAndClearCacheIfChanged(): String? {
        val root = esdeRootPath.first() ?: return null
        if (root != lastRoot) {
            cache.clear()
            lastRoot = root
        }
        return root
    }

    private fun readAndCache(file: File): GamelistFile {
        val lastModified = file.lastModified()
        val cached = cache[file.path]
        val content =
            if (cached != null && cached.lastModified == lastModified) {
                cached.content
            } else {
                file.readText().also { cache[file.path] = CacheEntry(lastModified, it) }
            }
        return GamelistFile(path = file.path, content = content)
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
