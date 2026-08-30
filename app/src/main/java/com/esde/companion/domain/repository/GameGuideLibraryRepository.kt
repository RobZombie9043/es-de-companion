package com.esde.companion.domain.repository

import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.GameReference
import kotlinx.coroutines.flow.Flow

/**
 * Downloaded guide storage - see the app-private filesystem + DataStore-backed
 * implementation for the real details. Content is stored as one string per page ([saveGuide]'s
 * [content]) so a multi-page HTML guide's pages can be loaded/rendered independently rather
 * than as one concatenated blob.
 */
interface GameGuideLibraryRepository {
    suspend fun saveGuide(
        guide: DownloadedGameGuide,
        content: List<String>,
    ): Result<Unit>

    fun observeGuidesFor(gameReference: GameReference): Flow<List<DownloadedGameGuide>>

    /** Every downloaded guide across every game/system - Settings > Game Guides >
     * "Browse Downloaded Guides", the one place this app shows guide storage as a whole
     * rather than scoped to a single current game. */
    fun observeAllGuides(): Flow<List<DownloadedGameGuide>>

    suspend fun loadContent(guideId: String): List<String>?

    suspend fun deleteGuide(guideId: String)

    suspend fun deleteAllGuides()
}
