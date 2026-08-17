package com.esde.companion.data.systems

import com.esde.companion.domain.parser.EsSystemsXmlParser
import com.esde.companion.domain.parser.EsdeSettingsParser
import com.esde.companion.domain.repository.SystemPathRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads ES-DE's `custom_systems/es_systems.xml` to find a system's actual configured
 * ROM directory - see [SystemPathRepository]'s kdoc for why this matters, and
 * GameMediaPathResolver's kdoc for how it fits into the overall fallback chain. This is
 * the last-resort tier: only consulted when neither the live-observed `system-select`
 * path nor a marker search on the rom path itself could resolve a game's media.
 *
 * `%ROMPATH%` substitution needs `settings/es_settings.xml`'s `ROMDirectory` value -
 * read via the same [EsdeSettingsParser] pattern [com.esde.companion.data.settings.FileEsdeInstallationRepository]
 * uses for `MediaDirectory`, kept self-contained here rather than depending on that
 * repository, since the two are wired independently and this needs only one setting.
 *
 * Caches each file's raw content by path + lastModified(), same rationale as
 * [com.esde.companion.data.gamelist.FileGameDescriptionRepository] - a user-editable
 * config file that changes rarely, re-read only when it actually has.
 */
class FileSystemPathRepository(
    private val esdeRootPath: String,
) : SystemPathRepository {
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    override suspend fun resolveSystemPath(systemShortName: String): String? =
        withContext(Dispatchers.IO) {
            val systemsXml = readCached(File(esdeRootPath, SYSTEMS_XML_RELATIVE_PATH)) ?: return@withContext null
            val settingsXml = readCached(File(esdeRootPath, SETTINGS_XML_RELATIVE_PATH))
            val romDirectory = settingsXml?.let { EsdeSettingsParser.findStringValue(it, "ROMDirectory") }
            EsSystemsXmlParser.findSystemPath(systemsXml, systemShortName, romDirectory)
        }

    private fun readCached(file: File): String? {
        if (!file.isFile) return null
        val lastModified = file.lastModified()
        val cached = cache[file.path]
        return if (cached != null && cached.lastModified == lastModified) {
            cached.content
        } else {
            file.readText().also { cache[file.path] = CacheEntry(lastModified, it) }
        }
    }

    private data class CacheEntry(val lastModified: Long, val content: String)

    private companion object {
        const val SYSTEMS_XML_RELATIVE_PATH = "custom_systems/es_systems.xml"
        const val SETTINGS_XML_RELATIVE_PATH = "settings/es_settings.xml"
    }
}
