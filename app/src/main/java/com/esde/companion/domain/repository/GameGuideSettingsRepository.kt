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
}
