package com.esde.companion.data.media

import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.repository.SystemMediaRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Picks a random file of [MediaType] from `<mediaFolderPath>/<systemShortName>/<type>/`,
 * walked recursively since games can live in subfolders (see GameMediaPathResolver - the
 * same subfolder structure is replicated under each media type folder). No attempt is
 * made to tie the chosen file back to a specific game; for this use case (a representative
 * system-level image) any file of that type belonging to the system is equally valid.
 */
class FileSystemMediaRepository(
    private val mediaFolderPath: String,
) : SystemMediaRepository {

    override suspend fun randomMedia(systemShortName: String, mediaType: MediaType): String? =
        withContext(Dispatchers.IO) {
            val typeDir = File(mediaFolderPath, "$systemShortName/${mediaType.folderName}")
            if (!typeDir.isDirectory) return@withContext null

            typeDir.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in mediaType.validExtensions }
                .toList()
                .randomOrNull()
                ?.absolutePath
        }
}