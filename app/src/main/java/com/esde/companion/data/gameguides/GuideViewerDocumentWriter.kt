package com.esde.companion.data.gameguides

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Fixed name for the composed document [writeViewerDocument] writes - the viewer always loads
 * this same file (via a matching fixed virtual URL, see `GameGuideHtmlViewer`'s
 * `GUIDE_MEDIA_DOCUMENT_URL`), so callers never need to invent or track per-load filenames. */
const val VIEWER_DOCUMENT_FILE_NAME = "_view.html"

/**
 * Writes the HTML guide viewer's composed document (the saved page's body HTML wrapped in a
 * theme/font-scale-specific `<html>`/`<head>`/CSS shell, rebuilt fresh on every font/theme
 * change) to [VIEWER_DOCUMENT_FILE_NAME] inside the guide's own [mediaDirectoryPath],
 * overwriting whatever was there before.
 *
 * Not loaded via a `file://` URL, despite living in a real file now - confirmed on-device as
 * `net::ERR_ACCESS_DENIED`, since a WebView's renderer runs in its own sandboxed process that
 * can't read the host app's private files directly by `file://` path on this Android version.
 * `GameGuideHtmlViewer` instead serves this directory through `androidx.webkit`'s
 * `WebViewAssetLoader`/`InternalStoragePathHandler`, which resolves a virtual
 * `https://appassets.androidplatform.net/...` URL against this same directory via its own
 * `ContentProvider`-style read, without needing a real network origin.
 */
suspend fun writeViewerDocument(
    mediaDirectoryPath: String,
    document: String,
) {
    withContext(Dispatchers.IO) {
        File(mediaDirectoryPath, VIEWER_DOCUMENT_FILE_NAME).writeText(document)
    }
}
