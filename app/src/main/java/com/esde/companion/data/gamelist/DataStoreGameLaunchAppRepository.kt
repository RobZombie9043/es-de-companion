package com.esde.companion.data.gamelist

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.esde.companion.domain.model.GameLaunchDisplayTarget
import com.esde.companion.domain.model.GameLaunchOverride
import com.esde.companion.domain.repository.GameLaunchAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val SYSTEM_DEFAULTS_KEY = stringPreferencesKey("system_defaults")
private val GAME_OVERRIDES_KEY = stringPreferencesKey("game_overrides")
private val LAUNCH_DISPLAY_TARGET_KEY = stringPreferencesKey("launch_display_target")

/**
 * DataStore-backed [GameLaunchAppRepository] - a JSON-encoded `Map<String, String>` for the
 * per-system defaults and a JSON-encoded list for the per-game overrides, same shape as
 * [DataStoreGameMatchOverrideRepository] (one key, decode-the-whole-thing, linear-scan lookup -
 * trivially fast at the sizes this ever reaches). Not a secret - plain DataStore.
 */
class DataStoreGameLaunchAppRepository(
    private val context: Context,
) : GameLaunchAppRepository {
    override fun observeSystemDefaults(): Flow<Map<String, String>> =
        context.gameLaunchOverrideDataStore.data.map { prefs ->
            prefs[SYSTEM_DEFAULTS_KEY]?.let(::decodeDefaults) ?: emptyMap()
        }

    override suspend fun setSystemDefault(
        systemShortName: String,
        packageName: String?,
    ) {
        context.gameLaunchOverrideDataStore.edit { prefs ->
            val current = prefs[SYSTEM_DEFAULTS_KEY]?.let(::decodeDefaults) ?: emptyMap()
            val updated =
                if (packageName == null) current - systemShortName else current + (systemShortName to packageName)
            prefs[SYSTEM_DEFAULTS_KEY] = encodeDefaults(updated)
        }
    }

    override fun observeGameOverrides(): Flow<List<GameLaunchOverride>> =
        context.gameLaunchOverrideDataStore.data.map { prefs ->
            prefs[GAME_OVERRIDES_KEY]?.let(::decodeOverrides) ?: emptyList()
        }

    override suspend fun setGameOverride(
        systemShortName: String,
        relativeRomPath: String,
        packageName: String?,
    ) {
        context.gameLaunchOverrideDataStore.edit { prefs ->
            val current = prefs[GAME_OVERRIDES_KEY]?.let(::decodeOverrides) ?: emptyList()
            val updated =
                current.filterNot { it.matches(systemShortName, relativeRomPath) } +
                    GameLaunchOverride(systemShortName, relativeRomPath, packageName)
            prefs[GAME_OVERRIDES_KEY] = encodeOverrides(updated)
        }
    }

    override suspend fun clearGameOverride(
        systemShortName: String,
        relativeRomPath: String,
    ) {
        context.gameLaunchOverrideDataStore.edit { prefs ->
            val current = prefs[GAME_OVERRIDES_KEY]?.let(::decodeOverrides) ?: emptyList()
            val updated = current.filterNot { it.matches(systemShortName, relativeRomPath) }
            prefs[GAME_OVERRIDES_KEY] = encodeOverrides(updated)
        }
    }

    override fun observeLaunchDisplayTarget(): Flow<GameLaunchDisplayTarget> =
        context.gameLaunchOverrideDataStore.data.map { prefs ->
            val name = prefs[LAUNCH_DISPLAY_TARGET_KEY]
            val match = GameLaunchDisplayTarget.entries.find { it.name == name }
            match ?: GameLaunchDisplayTarget.ThisScreen
        }

    override suspend fun setLaunchDisplayTarget(target: GameLaunchDisplayTarget) {
        context.gameLaunchOverrideDataStore.edit { prefs -> prefs[LAUNCH_DISPLAY_TARGET_KEY] = target.name }
    }

    private fun GameLaunchOverride.matches(
        systemShortName: String,
        relativeRomPath: String,
    ): Boolean = this.systemShortName == systemShortName && this.relativeRomPath == relativeRomPath
}

// File-scoped rather than class members - keeps DataStoreGameLaunchAppRepository's own method
// count under detekt's TooManyFunctions threshold now that it covers three settings (system
// defaults, game overrides, display target), not just the encode/decode pair the class alone
// would otherwise need per settings kind.
private fun decodeDefaults(json: String): Map<String, String> =
    try {
        Json.decodeFromString(json)
    } catch (
        @Suppress("SwallowedException") e: SerializationException,
    ) {
        emptyMap()
    }

private fun encodeDefaults(defaults: Map<String, String>): String = Json.encodeToString(defaults)

private fun decodeOverrides(json: String): List<GameLaunchOverride> =
    try {
        Json.decodeFromString<List<GameLaunchOverrideDto>>(json).map { it.toDomain() }
    } catch (
        @Suppress("SwallowedException") e: SerializationException,
    ) {
        emptyList()
    }

private fun encodeOverrides(overrides: List<GameLaunchOverride>): String {
    return Json.encodeToString(overrides.map { it.toDto() })
}

@Serializable
private data class GameLaunchOverrideDto(
    val systemShortName: String,
    val relativeRomPath: String,
    val packageName: String?,
)

private fun GameLaunchOverride.toDto() =
    GameLaunchOverrideDto(
        systemShortName = systemShortName,
        relativeRomPath = relativeRomPath,
        packageName = packageName,
    )

private fun GameLaunchOverrideDto.toDomain() =
    GameLaunchOverride(
        systemShortName = systemShortName,
        relativeRomPath = relativeRomPath,
        packageName = packageName,
    )
