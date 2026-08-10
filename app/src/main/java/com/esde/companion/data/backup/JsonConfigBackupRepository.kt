package com.esde.companion.data.backup

import com.esde.companion.domain.model.AppConfigBackup
import com.esde.companion.domain.repository.ConfigBackupRepository
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * `ignoreUnknownKeys` is a defensive belt-and-suspenders measure for genuinely unrecognized
 * keys - the actual guard against "newer backup, older app build" is the explicit `version`
 * check in [JsonConfigBackupRepository.deserialize], not reliance on lenient decoding to
 * silently drop fields this build doesn't understand yet.
 */
private val BackupJson = Json { ignoreUnknownKeys = true }

class JsonConfigBackupRepository : ConfigBackupRepository {
    override fun serialize(snapshot: AppConfigBackup): String = BackupJson.encodeToString(snapshot.toDto())

    override fun deserialize(contents: String): Result<AppConfigBackup> =
        runCatching { BackupJson.decodeFromString<ConfigBackupDto>(contents) }
            .mapCatching { dto ->
                require(dto.version <= AppConfigBackup.CURRENT_VERSION) {
                    "This backup was created by a newer version of ES-DE Companion and can't be restored here."
                }
                dto.toDomain()
            }
}
