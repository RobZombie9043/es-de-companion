package com.esde.companion.ui.gameguides

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.esde.companion.domain.gameguides.GuideDownloadProgress

private const val GAMEFAQS_HOST = "gamefaqs.gamespot.com"

/**
 * Embedded GameFAQs browser (Game Guides FAB, when no guides are downloaded yet, or the
 * Library's "+" dropdown). Navigation stays inside GameFAQs' own domain - anything else opens
 * in the system browser instead of hijacking this WebView (see [shouldStayInApp]), keeping
 * this from becoming a general-purpose browser. The Save action only appears once [state]'s
 * [GameGuidesUiState.Browsing.currentPageIsGuide] is true, following what
 * [GameGuidesViewModel.onBrowserPageLoaded] detected.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GameGuidesBrowserScreen(
    state: GameGuidesUiState.Browsing,
    onPageLoaded: (WebView) -> Unit,
    onSave: (WebView, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val webView =
        remember {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient =
                    object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val uri = request.url
                            if (shouldStayInApp(uri)) return false
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            return true
                        }

                        override fun onPageFinished(
                            view: WebView,
                            url: String?,
                        ) {
                            onPageLoaded(view)
                        }
                    }
            }
        }

    // Keyed on the search URL (not fired once inside the remember{} above) - this composable
    // can survive across a game switch (closing and quickly reopening the overlay for a
    // different game), in which case the same WebView instance is reused with a new [state]
    // rather than being freshly created. Loading only inside remember{} left it showing
    // whatever the *previous* game's search page was until the composable was fully torn
    // down and rebuilt - confirmed on-device as "opens on search page for previous guide."
    LaunchedEffect(state.searchUrl) {
        webView.loadUrl(state.searchUrl)
    }

    // Disabled while saving - back navigation would change webView's live URL/DOM out from
    // under the in-flight download, which reads both.
    BackHandler(enabled = !state.isSaving) {
        if (webView.canGoBack()) webView.goBack() else onClose()
    }

    // A real header section pushing the WebView down below it, not a translucent overlay
    // drawn on top of it - Surface (opaque, theme-aware surfaceContainerHigh) both paints a
    // solid background and sets the correct light/dark content color for its icons, unlike
    // the previous alpha=0.85f overlay approach.
    Column(modifier = modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { if (webView.canGoBack()) webView.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    IconButton(onClick = { webView.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reload")
                    }
                }
                if (state.currentPageIsGuide && !state.isSaving) {
                    TextButton(
                        onClick = { onSave(webView, webView.url.orEmpty()) },
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download")
                    }
                }
                IconButton(
                    onClick = onClose,
                    enabled = !state.isSaving,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
        }

        AndroidView(factory = { webView }, modifier = Modifier.weight(1f).fillMaxWidth())
    }

    state.downloadProgress?.let { progress ->
        DownloadProgressDialog(progress)
    }
}

/**
 * Non-dismissable while a download is in flight. [progress] reports real per-chapter counts
 * for a multi-page in-line HTML guide (see [GameFaqsBrowserBridge.walkHtmlChapters]); the
 * image-embedding pass that can follow it has no discrete steps to count, so that phase shows
 * as indeterminate instead of a fake percentage.
 */
@Composable
private fun DownloadProgressDialog(progress: GuideDownloadProgress) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text("Downloading guide") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(downloadProgressLabel(progress))
            }
        },
    )
}

private fun downloadProgressLabel(progress: GuideDownloadProgress): String =
    when (progress) {
        is GuideDownloadProgress.LoadingPage ->
            if (progress.totalPages > 1) {
                "Downloading page ${progress.page} of ${progress.totalPages}"
            } else {
                "Downloading guide…"
            }
        is GuideDownloadProgress.EmbeddingImages ->
            if (progress.totalPages > 1) {
                "Processing images (page ${progress.page} of ${progress.totalPages})…"
            } else {
                "Processing images…"
            }
    }

private fun shouldStayInApp(uri: Uri): Boolean = uri.host?.endsWith(GAMEFAQS_HOST) == true
