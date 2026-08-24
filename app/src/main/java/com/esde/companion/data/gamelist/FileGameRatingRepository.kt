package com.esde.companion.data.gamelist

import com.esde.companion.domain.model.GameRating
import com.esde.companion.domain.parser.GameListRatingParser
import com.esde.companion.domain.parser.LegacyGamelistPathResolver
import com.esde.companion.domain.repository.GameRatingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads and parses gamelist.xml to find a game's <rating> - same standard-then-legacy
 * location resolution, same per-file content cache (keyed by absolute path +
 * lastModified()), and same concurrency reasoning as FileGameDescriptionRepository; see
 * its kdoc for the full rationale. Kept as its own independent cache/repository rather
 * than sharing FileGameDescriptionRepository's, the same per-feature independence
 * GameDescription/GameMedia already follow elsewhere in this codebase.
 */
class FileGameRatingRepository(
    private val esdeRootPath: String,
) : GameRatingRepository {
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    override suspend fun resolveRating(
        systemShortName: String,
        romPath: String,
    ): GameRating =
        withContext(Dispatchers.IO) {
            val file = resolveGamelistFile(systemShortName, romPath) ?: return@withContext GameRating(value = null)

            val lastModified = file.lastModified()
            val cached = cache[file.path]
            val content =
                if (cached != null && cached.lastModified == lastModified) {
                    cached.content
                } else {
                    file.readText().also { cache[file.path] = CacheEntry(lastModified, it) }
                }

            val value = GameListRatingParser.findRating(content, romPath)
            GameRating(value = value, gamelistPath = file.path)
        }

    private fun resolveGamelistFile(
        systemShortName: String,
        romPath: String,
    ): File? {
        val standard = File(esdeRootPath, "gamelists/$systemShortName/gamelist.xml")
        if (standard.isFile) return standard

        val legacyPath = LegacyGamelistPathResolver.resolvePath(systemShortName, romPath) ?: return null
        return File(legacyPath).takeIf { it.isFile }
    }

    private data class CacheEntry(val lastModified: Long, val content: String)
}
