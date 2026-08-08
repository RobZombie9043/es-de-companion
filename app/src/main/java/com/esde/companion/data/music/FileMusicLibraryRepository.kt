package com.esde.companion.data.music

import com.esde.companion.domain.model.MusicPool
import com.esde.companion.domain.model.MusicTrack
import com.esde.companion.domain.model.musicTrackFromPath
import com.esde.companion.domain.repository.MusicLibraryRepository
import com.esde.companion.domain.repository.MusicPoolContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val MUSIC_EXTENSIONS = setOf("mp3", "ogg", "flac", "wav", "m4a", "aac", "opus")
private const val SYSTEMS_FOLDER_NAME = "systems"

/**
 * Scans a user-provided folder for playable tracks. [MusicPool.General] is the root plus
 * any non-"systems" subfolders; [MusicPool.PerSystem] pools live under
 * `<root>/systems/<shortName>/`. A per-system request that finds no tracks falls back to
 * the general pool - never another system's dedicated folder.
 */
class FileMusicLibraryRepository(
    private val musicFolderPath: String,
) : MusicLibraryRepository {
    override suspend fun loadPool(desired: MusicPool): MusicPoolContents =
        withContext(Dispatchers.IO) {
            when (desired) {
                is MusicPool.General -> MusicPoolContents(MusicPool.General, scanGeneralPool())
                is MusicPool.PerSystem -> {
                    val systemTracks = scanPerSystemPool(desired.systemShortName)
                    if (systemTracks.isNotEmpty()) {
                        MusicPoolContents(desired, systemTracks)
                    } else {
                        MusicPoolContents(MusicPool.General, scanGeneralPool())
                    }
                }
            }
        }

    private fun scanGeneralPool(): List<MusicTrack> {
        val root = File(musicFolderPath)
        if (!root.isDirectory) return emptyList()
        val systemsFolder = File(root, SYSTEMS_FOLDER_NAME)

        return root.walkTopDown()
            .onEnter { it != systemsFolder }
            .filter { it.isFile && it.extension.lowercase() in MUSIC_EXTENSIONS }
            .map { musicTrackFromPath(it.absolutePath) }
            .toList()
    }

    private fun scanPerSystemPool(systemShortName: String): List<MusicTrack> {
        val systemDir = File(musicFolderPath, "$SYSTEMS_FOLDER_NAME/$systemShortName")
        if (!systemDir.isDirectory) return emptyList()

        return systemDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in MUSIC_EXTENSIONS }
            .map { musicTrackFromPath(it.absolutePath) }
            .toList()
    }
}
