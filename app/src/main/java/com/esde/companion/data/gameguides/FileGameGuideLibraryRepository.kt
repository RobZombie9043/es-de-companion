package com.esde.companion.data.gameguides

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.esde.companion.data.pdf.PdfManualRenderer
import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.GameGuideFormat
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.GuideTocEntry
import com.esde.companion.domain.model.identifies
import com.esde.companion.domain.repository.GameGuideLibraryRepository
import com.esde.companion.domain.repository.GuidePageContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val PDF_EXTENSION = "pdf"

private val GUIDES_INDEX_KEY = stringPreferencesKey("guides_index")

/**
 * App-private storage for downloaded guides - never a user-visible/SAF folder, matching the
 * precedent every other app-fetched/cached blob in this app follows (the RetroAchievements
 * caches, the update checker's downloaded APK). Guide text content lives under [context]'s
 * files dir, one file per page ([saveGuide]); an imported binary guide (PDF/Image) instead
 * gets a single `content.<extension>` file in its own directory ([saveImportedGuide]) - the
 * two schemes coexist per-guide-directory since [GameGuideFormat] determines which one a
 * given guide actually used. A single DataStore key holds the JSON-encoded index of
 * every [DownloadedGameGuide]'s metadata - the expected guide count per device is small
 * enough that filtering the whole list in memory ([observeGuidesFor]) is simpler than
 * keying DataStore per game the way `GameListCache` keys per console.
 */
@Suppress("TooManyFunctions")
class FileGameGuideLibraryRepository(
    private val context: Context,
) : GameGuideLibraryRepository {
    // withContext(Dispatchers.IO) - same "don't block the caller's dispatcher" reasoning as
    // loadContent/loadPage below, now unmissable since this reads each page's content via a
    // real suspend callback (pageContent) rather than a plain in-memory list.
    override suspend fun saveGuide(
        guide: DownloadedGameGuide,
        pageContent: suspend (pageIndex: Int) -> GuidePageContent,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = guideDirectory(guide.id)
                dir.mkdirs()
                var sizeBytes = 0L
                val tocEntries = mutableListOf<GuideTocEntry>()
                // One page in memory at a time - the whole point of this being a callback
                // rather than a List<String> handed in up front. Confirmed necessary: a real
                // 20+ chapter, image-heavy guide can put ~8MB of base64-embedded HTML per page,
                // which used to all sit in one list before a single byte reached disk.
                for (index in 0 until guide.pageCount) {
                    val page = pageContent(index)
                    val bytes = page.html.toByteArray(Charsets.UTF_8)
                    File(dir, "page_$index.txt").writeBytes(bytes)
                    // Reuses the same encoded bytes just written, rather than re-encoding the
                    // whole string a second time purely to size it.
                    sizeBytes += bytes.size
                    tocEntries += page.tocEntries
                }
                val stored = guide.copy(sizeBytes = sizeBytes, tocEntries = tocEntries)
                writeIndex(readIndex().filterNot { it.id == guide.id } + stored)
            }
        }

    // withContext(Dispatchers.IO) - same "don't block the caller's dispatcher" reasoning as
    // loadContent below - writing the file and (for a PDF) opening it to read the real page
    // count are both real disk/native-library work.
    override suspend fun saveImportedGuide(
        guide: DownloadedGameGuide,
        contentBytes: ByteArray,
        fileExtension: String,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = guideDirectory(guide.id)
                dir.mkdirs()
                val file = File(dir, "content.$fileExtension")
                file.writeBytes(contentBytes)
                val pageCount =
                    if (fileExtension.equals(PDF_EXTENSION, ignoreCase = true)) {
                        val renderer = PdfManualRenderer.open(file.absolutePath)
                        val count = renderer?.pageCount ?: guide.pageCount
                        renderer?.close()
                        count
                    } else {
                        guide.pageCount
                    }
                val stored = guide.copy(sizeBytes = contentBytes.size.toLong(), pageCount = pageCount)
                writeIndex(readIndex().filterNot { it.id == guide.id } + stored)
            }
        }

    override suspend fun binaryContentPath(guideId: String): String? =
        withContext(Dispatchers.IO) {
            guideDirectory(guideId).listFiles { file -> file.name.startsWith("content.") }
                ?.firstOrNull()
                ?.absolutePath
        }

    override fun observeGuidesFor(gameReference: GameReference): Flow<List<DownloadedGameGuide>> =
        context.gameGuideLibraryDataStore.data.map { prefs ->
            decodeIndex(prefs[GUIDES_INDEX_KEY]).filter { it.gameReference.identifies(gameReference) }
        }

    override fun observeAllGuides(): Flow<List<DownloadedGameGuide>> =
        context.gameGuideLibraryDataStore.data.map { prefs -> decodeIndex(prefs[GUIDES_INDEX_KEY]) }

    // withContext(Dispatchers.IO), not left on whatever dispatcher the caller happens to be
    // on: viewModelScope.launch defaults to Dispatchers.Main.immediate, so File.readText()
    // here was blocking the UI thread for however long the read took - confirmed as a real,
    // noticeable "did my tap even register" pause opening a large (~1MB) real guide.
    override suspend fun loadContent(guideId: String): List<String>? =
        withContext(Dispatchers.IO) {
            val guide = readIndex().find { it.id == guideId } ?: return@withContext null
            val dir = guideDirectory(guideId)
            val files = (0 until guide.pageCount).map { index -> File(dir, "page_$index.txt") }
            files.takeIf { it.all(File::exists) }?.map(File::readText)
        }

    override suspend fun loadPage(
        guideId: String,
        pageIndex: Int,
    ): String? =
        withContext(Dispatchers.IO) {
            val file = File(guideDirectory(guideId), "page_$pageIndex.txt")
            file.takeIf(File::exists)?.readText()
        }

    override suspend fun deleteGuide(guideId: String) {
        guideDirectory(guideId).deleteRecursively()
        writeIndex(readIndex().filterNot { it.id == guideId })
    }

    override suspend fun deleteAllGuides() {
        guidesRootDirectory().deleteRecursively()
        writeIndex(emptyList())
    }

    private fun guidesRootDirectory() = File(context.filesDir, "game_guides")

    private fun guideDirectory(guideId: String) = File(guidesRootDirectory(), guideId)

    private suspend fun readIndex(): List<DownloadedGameGuide> =
        decodeIndex(context.gameGuideLibraryDataStore.data.first()[GUIDES_INDEX_KEY])

    private suspend fun writeIndex(guides: List<DownloadedGameGuide>) {
        context.gameGuideLibraryDataStore.edit {
            it[GUIDES_INDEX_KEY] = Json.encodeToString(guides.map { guide -> guide.toDto() })
        }
    }
}

private fun decodeIndex(json: String?): List<DownloadedGameGuide> {
    if (json == null) return emptyList()
    return try {
        Json.decodeFromString<List<DownloadedGameGuideDto>>(json).map { it.toDomain() }
    } catch (
        @Suppress("SwallowedException") e: SerializationException,
    ) {
        emptyList()
    }
}

@Serializable
private data class DownloadedGameGuideDto(
    val id: String,
    val systemShortName: String,
    val romPath: String,
    val systemPath: String? = null,
    val title: String,
    val sourceUrl: String,
    val format: String,
    val pageCount: Int,
    val downloadedAtMillis: Long,
    // Both default-valued so a guide saved before either field existed still decodes fine -
    // sizeBytes reads as 0 (shown as "0 B" until re-downloaded) and tocEntries as empty
    // (same as a plain-text guide, which never has one) rather than failing to load at all.
    val sizeBytes: Long = 0L,
    val tocEntries: List<GuideTocEntryDto> = emptyList(),
)

@Serializable
private data class GuideTocEntryDto(
    val title: String,
    val anchorId: String,
    val pageIndex: Int = 0,
)

private fun DownloadedGameGuide.toDto() =
    DownloadedGameGuideDto(
        id = id,
        systemShortName = gameReference.systemShortName,
        romPath = gameReference.romPath,
        systemPath = gameReference.systemPath,
        title = title,
        sourceUrl = sourceUrl,
        format = format.name,
        pageCount = pageCount,
        downloadedAtMillis = downloadedAtMillis,
        sizeBytes = sizeBytes,
        tocEntries = tocEntries.map { GuideTocEntryDto(it.title, it.anchorId, it.pageIndex) },
    )

private fun DownloadedGameGuideDto.toDomain() =
    DownloadedGameGuide(
        id = id,
        gameReference = GameReference(systemShortName, romPath, systemPath),
        title = title,
        sourceUrl = sourceUrl,
        format = runCatching { GameGuideFormat.valueOf(format) }.getOrDefault(GameGuideFormat.PlainText),
        pageCount = pageCount,
        downloadedAtMillis = downloadedAtMillis,
        sizeBytes = sizeBytes,
        tocEntries = tocEntries.map { GuideTocEntry(it.title, it.anchorId, it.pageIndex) },
    )
