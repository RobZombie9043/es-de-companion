package com.esde.companion.data.retroachievements

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.esde.companion.domain.model.ProgressStatus
import com.esde.companion.domain.model.UserGameProgress
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DataStore-backed [UserProgressCacheStore], one JSON-encoded value per username. Its own
 * DataStore file ([userProgressCacheDataStore]) rather than sharing [gameListCacheDataStore] -
 * a full completion-progress snapshot can run to well over a thousand entries, and Preferences
 * DataStore reads/rewrites its entire file per access, so sharing would tax every game-list
 * cache read with this dataset's own size. Not a secret - plain DataStore, unlike the
 * RetroAchievements credentials - and, like the game list cache and credentials, not covered
 * by Backup & Restore (see AppContainer's wiring comment).
 */
class DataStoreUserProgressCacheStore(
    private val context: Context,
) : UserProgressCacheStore {
    override suspend fun read(username: String): CachedUserProgress? {
        val json = context.userProgressCacheDataStore.data.first()[cacheKey(username)] ?: return null
        return try {
            Json.decodeFromString<CachedUserProgressDto>(json).toDomain()
        } catch (
            @Suppress("SwallowedException") e: SerializationException,
        ) {
            null
        }
    }

    override suspend fun write(
        username: String,
        cached: CachedUserProgress,
    ) {
        context.userProgressCacheDataStore.edit {
            it[cacheKey(username)] = Json.encodeToString(cached.toDto())
        }
    }

    private fun cacheKey(username: String) = stringPreferencesKey("progress_$username")
}

@Serializable
private data class CachedUserProgressDto(
    val fetchedAtMillis: Long,
    val entries: List<UserGameProgressDto>,
)

/** [status] defaults to [ProgressStatus.None]'s name so an unrecognized/future value decodes safely. */
@Serializable
private data class UserGameProgressDto(
    val gameId: Long,
    val numAwarded: Int = 0,
    val maxPossible: Int = 0,
    val status: String = ProgressStatus.None.name,
)

private fun CachedUserProgress.toDto(): CachedUserProgressDto {
    val entryDtos = progressByGameId.values.map { it.toDto() }
    return CachedUserProgressDto(fetchedAtMillis, entryDtos)
}

private fun UserGameProgress.toDto(): UserGameProgressDto {
    return UserGameProgressDto(gameId, numAwarded, maxPossible, status.name)
}

private fun CachedUserProgressDto.toDomain(): CachedUserProgress {
    val progressByGameId = entries.map { it.toDomain() }.associateBy { it.gameId }
    return CachedUserProgress(fetchedAtMillis, progressByGameId)
}

private fun UserGameProgressDto.toDomain(): UserGameProgress {
    val parsedStatus = ProgressStatus.entries.firstOrNull { it.name == status } ?: ProgressStatus.None
    return UserGameProgress(gameId, numAwarded, maxPossible, parsedStatus)
}
