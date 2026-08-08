package com.esde.companion.data.media

import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.parser.GameMediaPathResolver
import com.esde.companion.domain.repository.GameMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileGameMediaRepository(
    private val mediaFolderPath: String,
) : GameMediaRepository {
    override suspend fun resolveMedia(
        systemShortName: String,
        romPath: String,
        mediaTypes: Set<MediaType>,
    ): GameMedia =
        withContext(Dispatchers.IO) {
            // Real filesystem check for ES-DE's "directories interpreted as files" case -
            // see GameMediaPathResolver's kdoc for why this can't be determined from the
            // path string alone.
            val romIsDirectory = File(romPath).isDirectory
            val baseRelativePath =
                GameMediaPathResolver.resolveBaseRelativePath(
                    systemShortName = systemShortName,
                    romPath = romPath,
                    romIsDirectory = romIsDirectory,
                ) ?: return@withContext GameMedia(baseRelativePath = null, filesByType = emptyMap())

            val filesByType =
                mediaTypes
                    .mapNotNull { type -> findExistingFile(type, systemShortName, baseRelativePath)?.let { type to it } }
                    .toMap()

            GameMedia(baseRelativePath = baseRelativePath, filesByType = filesByType)
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
