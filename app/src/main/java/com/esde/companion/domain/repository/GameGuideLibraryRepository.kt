package com.esde.companion.domain.repository

import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.GuideTocEntry
import kotlinx.coroutines.flow.Flow

/** What one page's [GameGuideLibraryRepository.saveGuide] callback produces - its html plus
 * whatever table-of-contents entries were tagged on that specific page (empty for a page/
 * format that doesn't tag any, e.g. plain text). */
data class GuidePageContent(
    val html: String,
    val tocEntries: List<GuideTocEntry> = emptyList(),
)

/**
 * Downloaded guide storage - see the app-private filesystem + DataStore-backed
 * implementation for the real details. Content is stored as one string per page - [saveGuide]
 * calls [GuidePageContent] back once per page (`0 until guide.pageCount`) and writes each one
 * to disk immediately, so a multi-page HTML guide's pages are never all resident in memory at
 * once, whether being loaded/rendered (see [loadPage]) or saved.
 */
interface GameGuideLibraryRepository {
    suspend fun saveGuide(
        guide: DownloadedGameGuide,
        pageContent: suspend (pageIndex: Int) -> GuidePageContent,
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

    /** The directory [guideId]'s own on-disk assets live under (created if it doesn't exist
     * yet) - used for a [GameGuideFormat.Html] guide's embedded images (saved as real files
     * rather than inlined base64 text, see `NativeImageDownloader`'s kdoc) and the viewer's
     * own composed, theme/font-scale-specific document file (see `GameGuideHtmlViewer`'s
     * kdoc). Callable before the guide itself has ever been saved - a fresh download resolves
     * this first so it has somewhere to write images to as it embeds each page. */
    suspend fun mediaDirectoryPath(guideId: String): String

    suspend fun deleteGuide(guideId: String)

    suspend fun deleteAllGuides()
}
