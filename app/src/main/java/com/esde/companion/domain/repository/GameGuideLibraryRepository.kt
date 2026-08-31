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

    /** Saves an imported [GameGuideFormat.Pdf]/[GameGuideFormat.Image] guide's raw file
     * bytes - the binary counterpart to [saveGuide], used by GameGuideImportPicker's flow.
     * [fileExtension] (no leading dot, e.g. "pdf") names the single file written under this
     * guide's own directory. For a "pdf" extension, the implementation corrects
     * [DownloadedGameGuide.pageCount] to the file's real page count before persisting it -
     * callers pass a placeholder pageCount that only matters for non-PDF formats. */
    suspend fun saveImportedGuide(
        guide: DownloadedGameGuide,
        contentBytes: ByteArray,
        fileExtension: String,
    ): Result<Unit>

    fun observeGuidesFor(gameReference: GameReference): Flow<List<DownloadedGameGuide>>

    /** Every downloaded guide across every game/system - Settings > Game Guides >
     * "Browse Downloaded Guides", the one place this app shows guide storage as a whole
     * rather than scoped to a single current game. */
    fun observeAllGuides(): Flow<List<DownloadedGameGuide>>

    suspend fun loadContent(guideId: String): List<String>?

    /** Loads just [pageIndex]'s saved content, without touching any other page - the Viewer
     * uses this instead of [loadContent] so an image-heavy, many-chapter HTML guide never
     * needs every chapter resident in memory at once (confirmed crashing with an
     * OutOfMemoryError otherwise - see `GameGuidesViewModel.loadedTextViewingStateFor`'s
     * kdoc). Null if [guideId] doesn't exist or has no page at [pageIndex]. */
    suspend fun loadPage(
        guideId: String,
        pageIndex: Int,
    ): String?

    /** The on-disk path to a [GameGuideFormat.Pdf]/[GameGuideFormat.Image] guide's saved
     * binary file - the binary counterpart to [loadContent]. Null if [guideId] doesn't exist
     * or was never saved via [saveImportedGuide]. */
    suspend fun binaryContentPath(guideId: String): String?

    suspend fun deleteGuide(guideId: String)

    suspend fun deleteAllGuides()
}
