package com.esde.companion.data.retroachievements

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.esde.companion.domain.model.GameMatchOverride
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.repository.GameMatchOverrideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val OVERRIDES_KEY = stringPreferencesKey("overrides")

/**
 * DataStore-backed [GameMatchOverrideRepository] - one JSON-encoded list under a single
 * key, not a `Map<String, Long>` keyed by a delimited "$systemShortName|$romPath" string
 * (a ROM path could theoretically contain the delimiter). Lookup is a linear scan over the
 * decoded list, which is trivially fast at the sizes this ever reaches (a user's manually
 * corrected games, not their whole library). Not a secret - plain DataStore, unlike the
 * RetroAchievements credentials.
 */
class DataStoreGameMatchOverrideRepository(
    private val context: Context,
) : GameMatchOverrideRepository {
    private val overridesFlow: Flow<List<GameMatchOverride>> =
        context.gameMatchOverrideDataStore.data.map { prefs -> prefs[OVERRIDES_KEY]?.let(::decode) ?: emptyList() }

    override suspend fun setOverride(override: GameMatchOverride) {
        val reference = GameReference(override.systemShortName, override.romPath)
        context.gameMatchOverrideDataStore.edit { prefs ->
            val current = prefs[OVERRIDES_KEY]?.let(::decode) ?: emptyList()
            val updated = current.filterNot { it.matchesReference(reference) } + override
            prefs[OVERRIDES_KEY] = encode(updated)
        }
    }

    override suspend fun clearOverride(gameReference: GameReference) {
        context.gameMatchOverrideDataStore.edit { prefs ->
            val current = prefs[OVERRIDES_KEY]?.let(::decode) ?: emptyList()
            prefs[OVERRIDES_KEY] = encode(current.filterNot { it.matchesReference(gameReference) })
        }
    }

    override suspend fun getOverride(gameReference: GameReference): GameMatchOverride? {
        return overridesFlow.first().firstOrNull { it.matchesReference(gameReference) }
    }

    override fun observeAllOverrides(): Flow<List<GameMatchOverride>> = overridesFlow

    private fun GameMatchOverride.matchesReference(reference: GameReference): Boolean {
        return systemShortName == reference.systemShortName && romPath == reference.romPath
    }

    private fun decode(json: String): List<GameMatchOverride> =
        try {
            Json.decodeFromString<List<GameMatchOverrideDto>>(json).map { it.toDomain() }
        } catch (
            @Suppress("SwallowedException") e: SerializationException,
        ) {
            emptyList()
        }

    private fun encode(overrides: List<GameMatchOverride>): String = Json.encodeToString(overrides.map { it.toDto() })
}

@Serializable
private data class GameMatchOverrideDto(
    val systemShortName: String,
    val romPath: String,
    val raGameId: Long,
)

private fun GameMatchOverride.toDto(): GameMatchOverrideDto {
    return GameMatchOverrideDto(systemShortName = systemShortName, romPath = romPath, raGameId = raGameId)
}

private fun GameMatchOverrideDto.toDomain(): GameMatchOverride {
    return GameMatchOverride(systemShortName = systemShortName, romPath = romPath, raGameId = raGameId)
}
