package com.esde.companion.ui.gameguides

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import com.esde.companion.data.gameguides.VIEWER_DOCUMENT_FILE_NAME
import java.io.File

// A virtual, non-resolving domain reserved by WebViewAssetLoader's own convention for exactly
// this "serve local app content" use case - never a real network origin. GUIDE_MEDIA_PATH_PREFIX
// is mapped (per guide, see GuideMediaLoader) to that guide's own on-disk directory, so this
// fixed URL always resolves to whichever guide's VIEWER_DOCUMENT_FILE_NAME was written there
// most recently.
private const val GUIDE_MEDIA_DOMAIN = "appassets.androidplatform.net"
private const val GUIDE_MEDIA_PATH_PREFIX = "/guide-media/"

internal const val GUIDE_MEDIA_DOCUMENT_URL =
    "https://$GUIDE_MEDIA_DOMAIN$GUIDE_MEDIA_PATH_PREFIX$VIEWER_DOCUMENT_FILE_NAME"

/**
 * Resolves every request under [GUIDE_MEDIA_PATH_PREFIX] (the composed document itself, plus
 * every embedded image it references by relative path) against whichever guide's directory
 * [updateDirectory] was most recently pointed at - rebuilt only when that directory actually
 * changes, not per-request, since [HtmlGuideContent]'s single [WebView] instance can be reused
 * across different guides opened one after another in the same viewing session. Backed by
 * [WebViewAssetLoader.InternalStoragePathHandler], which validates the resolved path stays
 * inside the registered directory (blocking `../` traversal) before reading it.
 */
internal class GuideMediaLoader(private val context: Context) {
    private var directory: String? = null
    private var loader: WebViewAssetLoader? = null

    fun updateDirectory(mediaDirectoryPath: String) {
        if (mediaDirectoryPath == directory) return
        directory = mediaDirectoryPath
        loader =
            WebViewAssetLoader.Builder()
                .setDomain(GUIDE_MEDIA_DOMAIN)
                .addPathHandler(
                    GUIDE_MEDIA_PATH_PREFIX,
                    WebViewAssetLoader.InternalStoragePathHandler(context, File(mediaDirectoryPath)),
                )
                .build()
    }

    fun intercept(request: WebResourceRequest): WebResourceResponse? = loader?.shouldInterceptRequest(request.url)
}
