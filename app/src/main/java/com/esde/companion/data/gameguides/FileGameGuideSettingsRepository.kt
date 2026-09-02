package com.esde.companion.data.gameguides

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.esde.companion.domain.model.GameGuideDisplayPreferences
import com.esde.companion.domain.model.GameGuideReadingProgress
import com.esde.companion.domain.repository.GameGuideSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
private const val DEFAULT_FONT_SCALE = 1.0f
private val MANUAL_FALLBACK_ON_NO_GUIDE_KEY = booleanPreferencesKey("manual_fallback_on_no_guide")

/**
 * DataStore-backed [GameGuideSettingsRepository]. Reading progress is keyed per guide id
 * (one float + one long preference pair per guide, prefixed `progress_<guideId>_`) rather
 * than one JSON blob, since progress writes happen frequently (debounced scroll updates)
 * and should stay cheap per-key edits - same per-identity DataStore-key shape
 * `FileLastKnownContextRepository` and the RetroAchievements disk caches use.
 */
class FileGameGuideSettingsRepository(
    private val context: Context,
) : GameGuideSettingsRepository {
    override suspend fun setDisplayPreferences(preferences: GameGuideDisplayPreferences) {
        context.gameGuideSettingsDataStore.edit { it[FONT_SCALE_KEY] = preferences.fontScale }
    }

    override fun observeDisplayPreferences(): Flow<GameGuideDisplayPreferences> =
        context.gameGuideSettingsDataStore.data.map { prefs ->
            GameGuideDisplayPreferences(fontScale = prefs[FONT_SCALE_KEY] ?: DEFAULT_FONT_SCALE)
        }

    override suspend fun setReadingProgress(progress: GameGuideReadingProgress) {
        context.gameGuideSettingsDataStore.edit {
            it[scrollFractionKey(progress.guideId)] = progress.scrollFraction
            it[lastOpenedAtKey(progress.guideId)] = progress.lastOpenedAtMillis
            it[pageIndexKey(progress.guideId)] = progress.pageIndex
        }
    }

    override fun observeReadingProgress(guideId: String): Flow<GameGuideReadingProgress?> =
        context.gameGuideSettingsDataStore.data.map { prefs ->
            val scrollFraction = prefs[scrollFractionKey(guideId)] ?: return@map null
            val lastOpenedAt = prefs[lastOpenedAtKey(guideId)] ?: return@map null
            // Default 0 for progress saved before pageIndex existed - a single-page guide
            // (or a plain-text one, which never has more than one page) at pageIndex 0 is
            // exactly the pre-existing behavior, so this isn't a lossy migration.
            val pageIndex = prefs[pageIndexKey(guideId)] ?: 0
            GameGuideReadingProgress(guideId, scrollFraction, lastOpenedAt, pageIndex)
        }

    override suspend fun setManualFallbackOnNoGuideEnabled(enabled: Boolean) {
        context.gameGuideSettingsDataStore.edit { it[MANUAL_FALLBACK_ON_NO_GUIDE_KEY] = enabled }
    }

    override fun observeManualFallbackOnNoGuideEnabled(): Flow<Boolean> =
        context.gameGuideSettingsDataStore.data.map { prefs -> prefs[MANUAL_FALLBACK_ON_NO_GUIDE_KEY] ?: false }

    private fun scrollFractionKey(guideId: String) = floatPreferencesKey("progress_${guideId}_scroll")

    private fun lastOpenedAtKey(guideId: String) = longPreferencesKey("progress_${guideId}_last_opened")

    private fun pageIndexKey(guideId: String) = intPreferencesKey("progress_${guideId}_page")
}
