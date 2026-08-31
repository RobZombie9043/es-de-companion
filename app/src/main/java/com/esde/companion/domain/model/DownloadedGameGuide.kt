package com.esde.companion.domain.model

/**
 * Metadata for one guide saved for offline reading (the Game Guides FAB). [id] is a stable
 * identifier derived from [sourceUrl] (see `GameFaqsBrowserBridge`), used to key both the
 * saved content pages and [GameGuideReadingProgress]. An imported guide (see
 * `GameGuideImportPicker`/`GuideImportAndSave`) has no real URL to derive an id from, so its
 * [id] is instead derived from [gameReference] + the imported file's own name + the import
 * timestamp, and [sourceUrl] is a synthetic, never-dereferenced marker
 * (`"import://<fileName>"`) kept only for debugging/id-provenance. Keyed per-game via [gameReference],
 * the same per-ROM identity `ResolveGameMediaUseCase` already keys manuals by. [pageCount]
 * reflects how many page-strings were handed to `GameGuideLibraryRepository.saveGuide` -
 * always 1 for a single-page guide, more for a multi-page HTML guide whose pagination was
 * walked and stored page-by-page rather than concatenated into one blob. [tocEntries] is
 * only ever populated for [GameGuideFormat.Html] (tagged at download time, alongside image
 * embedding - see `GameFaqsBrowserBridge`); a [GameGuideFormat.PlainText] guide's table of
 * contents is instead computed on the fly when opened (see
 * `domain/gameguides/PlainTextGuideTocParser`), so it stays empty here. [sizeBytes] is the
 * combined byte size of [pageCount] pages' saved content, for display in the guide list.
 */
data class DownloadedGameGuide(
    val id: String,
    val gameReference: GameReference,
    val title: String,
    val sourceUrl: String,
    val format: GameGuideFormat,
    val pageCount: Int,
    val sizeBytes: Long,
    val downloadedAtMillis: Long,
    val tocEntries: List<GuideTocEntry> = emptyList(),
)
