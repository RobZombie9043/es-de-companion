package com.esde.companion.data.media

import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.parser.GameMediaPathResolver
import com.esde.companion.domain.repository.GameMediaRepository
import com.esde.companion.domain.repository.SystemPathRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileGameMediaRepository(
    private val mediaFolderPath: String,
    private val systemPathRepository: SystemPathRepository,
) : GameMediaRepository {
    override suspend fun resolveMedia(
        systemShortName: String,
        systemPath: String?,
        romPath: String,
        mediaTypes: Set<MediaType>,
    ): GameMedia =
        withContext(Dispatchers.IO) {
            // Real filesystem check for ES-DE's "directories interpreted as files" case -
            // see GameMediaPathResolver's kdoc for why this can't be determined from the
            // path string alone.
            val romIsDirectory = File(romPath).isDirectory
            val baseRelativePath =
                resolveBaseRelativePath(systemShortName, systemPath, romPath, romIsDirectory)
                    ?: return@withContext GameMedia(baseRelativePath = null, filesByType = emptyMap())

            val filesByType =
                mediaTypes
                    .mapNotNull { type ->
                        findExistingFile(type, systemShortName, baseRelativePath)?.let { type to it }
                    }
                    .toMap()

            GameMedia(baseRelativePath = baseRelativePath, filesByType = filesByType)
        }

    /**
     * Tries [systemPath] (already known on AppState, from a prior `system-select`) first
     * - this also covers GameMediaPathResolver's own marker-search fallback internally.
     * Only when that fully misses does it fall through to [systemPathRepository]'s
     * `custom_systems/es_systems.xml` lookup (data-layer file I/O, reserved for
     * genuinely custom-named ROM folders) and retry - see GameMediaPathResolver's kdoc
     * for the full three-tier rationale.
     */
    private suspend fun resolveBaseRelativePath(
        systemShortName: String,
        systemPath: String?,
        romPath: String,
        romIsDirectory: Boolean,
    ): String? {
        val fromKnownState =
            GameMediaPathResolver.resolveBaseRelativePath(
                systemShortName = systemShortName,
                systemPath = systemPath,
                romPath = romPath,
                romIsDirectory = romIsDirectory,
            )
        return fromKnownState ?: resolveViaSystemPathRepository(systemShortName, romPath, romIsDirectory)
    }

    private suspend fun resolveViaSystemPathRepository(
        systemShortName: String,
        romPath: String,
        romIsDirectory: Boolean,
    ): String? {
        val resolvedSystemPath = systemPathRepository.resolveSystemPath(systemShortName) ?: return null
        return GameMediaPathResolver.resolveBaseRelativePath(
            systemShortName = systemShortName,
            systemPath = resolvedSystemPath,
            romPath = romPath,
            romIsDirectory = romIsDirectory,
        )
    }

    private fun findExistingFile(
        type: MediaType,
        systemShortName: String,
        baseRelativePath: String,
    ): String? {
        for (extension in type.validExtensions) {
            val candidate = File(mediaFolderPath, "$systemShortName/${type.folderName}/$baseRelativePath.$extension")
            if (candidate.isFile) return candidate.absolutePath
        }
        return null
    }
}
