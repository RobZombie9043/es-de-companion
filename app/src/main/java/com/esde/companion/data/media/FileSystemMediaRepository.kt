package com.esde.companion.data.media

import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.repository.SystemMediaRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Picks a random fanart file from `<mediaFolderPath>/<systemShortName>/fanart/`, walked
 * recursively since games can live in subfolders (see GameMediaPathResolver - the same
 * subfolder structure is replicated under each media type folder). No attempt is made to
 * tie the chosen file back to a specific game; for this use case (a representative
 * system-browsing backdrop) any fanart image belonging to the system is equally valid.
 */
class FileSystemMediaRepository(
    private val mediaFolderPath: String,
) : SystemMediaRepository {

    override suspend fun randomFanart(systemShortName: String): String? =
        withContext(Dispatchers.IO) {
            val fanartDir = File(mediaFolderPath, "$systemShortName/${MediaType.FanArt.folderName}")
            if (!fanartDir.isDirectory) return@withContext null

            fanartDir.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in MediaType.FanArt.validExtensions }
                .toList()
                .randomOrNull()
                ?.absolutePath
        }
}