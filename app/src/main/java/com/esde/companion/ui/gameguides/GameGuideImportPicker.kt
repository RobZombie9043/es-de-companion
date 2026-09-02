package com.esde.companion.ui.gameguides

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.esde.companion.data.gameguides.GuideImportFileIo
import com.esde.companion.domain.model.GameGuideFormat
import kotlinx.coroutines.launch

private val IMPORT_MIME_TYPES = arrayOf("text/plain", "text/html", "application/pdf", "image/*")

/** Kept as a typealias purely to keep [rememberGuideImportLauncher]'s own signature within
 * this project's line-length limits. */
internal typealias GuideImportedCallback = (bytes: ByteArray, fileName: String, format: GameGuideFormat) -> Unit

/**
 * Backs the "+" dropdown's Import item, at both call sites (the overlay's own guide list and
 * Settings > Game Guides > Add Guide) - same [ActivityResultContracts.OpenDocument] pattern
 * `SetupSettingsContent`'s Backup & Restore uses, with the [Uri][android.net.Uri] handled
 * entirely here rather than passed into a ViewModel. Silently does nothing for a file whose
 * extension [GameGuideFormat.forFileExtension] doesn't recognize - the picker's own MIME
 * filter already keeps this to an edge case (e.g. a filename with no extension).
 */
@Composable
fun rememberGuideImportLauncher(onImported: GuideImportedCallback): () -> Unit {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val imported = GuideImportFileIo.readBytesAndFileName(context, uri).getOrNull() ?: return@launch
                val extension = imported.fileName.substringAfterLast('.', "")
                val format = GameGuideFormat.forFileExtension(extension) ?: return@launch
                onImported(imported.bytes, imported.fileName, format)
            }
        }
    return { launcher.launch(IMPORT_MIME_TYPES) }
}
