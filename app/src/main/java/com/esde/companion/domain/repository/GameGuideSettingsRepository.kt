package com.esde.companion.domain.repository

import com.esde.companion.domain.model.GameGuideDisplayPreferences
import com.esde.companion.domain.model.GameGuideReadingProgress
import kotlinx.coroutines.flow.Flow

/** Reader display preferences and per-guide reading progress - see the app-private
 * DataStore-backed implementation for the real details. */
interface GameGuideSettingsRepository {
    suspend fun setDisplayPreferences(preferences: GameGuideDisplayPreferences)

    fun observeDisplayPreferences(): Flow<GameGuideDisplayPreferences>

    suspend fun setReadingProgress(progress: GameGuideReadingProgress)

    fun observeReadingProgress(guideId: String): Flow<GameGuideReadingProgress?>

    /** Settings > Game Guides: whether Game Playing Behavior = Guide should fall back to
     * showing the current game's manual when it has no downloaded guide, instead of showing
     * nothing. Defaults off, like every other automation toggle in this app. */
    suspend fun setManualFallbackOnNoGuideEnabled(enabled: Boolean)

    fun observeManualFallbackOnNoGuideEnabled(): Flow<Boolean>
}
