package com.esde.companion.ui.gameguides

import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.GameGuideFormat
import com.esde.companion.domain.model.GameReference

private const val IMPORTED_GUIDE_ID_RADIX = 16

// A single page is the right placeholder for every imported format - PlainText/Html imports
// really are one page (see GameGuidesViewModel.importGuideFor), and FileGameGuideLibraryRepository
// corrects this to the real page count for a Pdf import once the file is actually on disk.
private const val IMPORTED_GUIDE_PLACEHOLDER_PAGE_COUNT = 1

/** Stable id for an imported guide - unlike a GameFAQs download (see GuideDownloadAndSave),
 * an import has no source URL to derive an id from, so this combines the target game, the
 * imported file's own name, and the import instant instead. */
internal fun importedGuideId(
    reference: GameReference,
    fileName: String,
    atMillis: Long,
): String {
    val key = "${reference.systemShortName}|${reference.romPath}|$fileName|$atMillis"
    return key.hashCode().toUInt().toString(IMPORTED_GUIDE_ID_RADIX)
}

/** Builds the [DownloadedGameGuide] metadata for a freshly-imported file, before its content
 * is actually persisted (see [GameGuidesViewModel.importGuideFor]) - [sizeBytes]/[pageCount]
 * are corrected by the repository once the file is written, same as a GameFAQs download's
 * placeholder sizeBytes. */
internal fun buildImportedGuide(
    reference: GameReference,
    fileName: String,
    format: GameGuideFormat,
    atMillis: Long,
): DownloadedGameGuide =
    DownloadedGameGuide(
        id = importedGuideId(reference, fileName, atMillis),
        gameReference = reference,
        title = fileName.substringBeforeLast('.').ifBlank { fileName },
        sourceUrl = "import://$fileName",
        format = format,
        pageCount = IMPORTED_GUIDE_PLACEHOLDER_PAGE_COUNT,
        sizeBytes = 0L,
        downloadedAtMillis = atMillis,
    )
