package com.esde.companion.data.retroachievements

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DataStore-backed [GameListCacheStore], one JSON-encoded value per console (same pattern
 * as [com.esde.companion.data.settings.FileWidgetLayoutRepository]'s per-[com.esde.companion.domain.model.StateGroup]
 * canvas keys). Not a secret - plain DataStore, unlike the RetroAchievements credentials.
 */
class DataStoreGameListCacheStore(
    private val context: Context,
) : GameListCacheStore {
    override suspend fun read(consoleId: Long): CachedGameList? {
        val json = context.gameListCacheDataStore.data.first()[cacheKey(consoleId)] ?: return null
        return try {
            Json.decodeFromString<CachedGameListDto>(json).toDomain()
        } catch (
            @Suppress("SwallowedException") e: SerializationException,
        ) {
            null
        }
    }

    override suspend fun write(
        consoleId: Long,
        cached: CachedGameList,
    ) {
        context.gameListCacheDataStore.edit {
            it[cacheKey(consoleId)] = Json.encodeToString(cached.toDto())
        }
    }

    private fun cacheKey(consoleId: Long) = stringPreferencesKey("games_$consoleId")
}

@Serializable
private data class CachedGameListDto(
    val fetchedAtMillis: Long,
    val games: List<CandidateGameDto>,
)

@Serializable
private data class CandidateGameDto(
    val gameId: Long,
    val title: String,
    val iconUrl: String?,
)

private fun CachedGameList.toDto(): CachedGameListDto {
    val gameDtos = games.map { CandidateGameDto(it.gameId, it.title, it.iconUrl) }
    return CachedGameListDto(fetchedAtMillis, gameDtos)
}

private fun CachedGameListDto.toDomain() =
    CachedGameList(fetchedAtMillis, games.map { RetroAchievementsCandidateGame(it.gameId, it.title, it.iconUrl) })
