package com.esde.companion.ui.gameguides

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.esde.companion.data.gameguides.writeViewerDocument
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.math.abs

private const val SCROLL_POLL_INTERVAL_MILLIS = 500L
private const val SCROLL_REPORT_THRESHOLD = 0.01f

// Longer than awaitScrollableLayout's own worst case (SCROLL_LAYOUT_POLL_ATTEMPTS *
// SCROLL_LAYOUT_POLL_INTERVAL_MILLIS = 1500ms) plus a margin for the restore script itself.
private const val SCROLL_INITIAL_SETTLE_MILLIS = 1_800L

// Bounds how long a cross-page TOC jump waits for its target page to finish loading before
// giving up and scrolling anyway (a same-page jump never waits at all - see the
// scrollToAnchorId effect's kdoc). Generous relative to a real page load's own worst case
// (loadAndAwaitFinished + awaitScrollableLayout), same "degrade, don't hang forever" principle
// used throughout this viewer.
private const val ANCHOR_WAIT_POLL_ATTEMPTS = 40
private const val ANCHOR_WAIT_POLL_INTERVAL_MILLIS = 100L

private fun backgroundColorInt(isDarkTheme: Boolean): Int =
    android.graphics.Color.parseColor(if (isDarkTheme) DARK_BACKGROUND_HEX else LIGHT_BACKGROUND_HEX)

data class HtmlViewerConfig(
    val html: String,
    val mediaDirectoryPath: String,
    val fontScale: Float,
    val isDarkTheme: Boolean,
    val searchQuery: String,
    val findNextRequestId: Int,
    val scrollToAnchorId: String?,
    val initialScrollFraction: Float,
)

data class HtmlViewerCallbacks(
    val onFindResult: (active: Int, total: Int) -> Unit,
    val onScrollToAnchorHandled: () -> Unit,
    val onScrollFractionChanged: (Float) -> Unit,
    val onTap: () -> Unit,
    val onInternalAnchorTapped: (fragment: String) -> Unit,
)

/**
 * Renders a saved [com.esde.companion.domain.model.GameGuideFormat.Html] guide in a
 * sandboxed offline WebView. A link tap never navigates this WebView out to
 * gamefaqs.gamespot.com (see [buildGuideWebView]'s `shouldOverrideUrlLoading`), since this is
 * meant to read as an offline saved document, not turn into a live browser - but a tap on one
 * of the guide's own in-content chapter/table-of-contents links (every one seen in practice
 * carries a `#fragment`, e.g. `?page=3#Boss Fight`) is handled locally instead of silently
 * doing nothing: [HtmlViewerCallbacks.onInternalAnchorTapped] reports the fragment back up to
 * [GameGuideViewerScreen], which resolves it against the guide's own [GuideTocEntry] list (its
 * `anchorId` is that exact fragment text - see [GuideTocOutlineExtractor]'s kdoc) the same way
 * a table-of-contents dialog tap already does, switching page + [HtmlViewerConfig.scrollToAnchorId]
 * as needed. A fragment with no matching entry (an in-body cross-reference the `.ftoc` outline
 * never listed) falls back to a same-page scroll attempt, a safe no-op if it doesn't resolve to
 * a real element on the current page. Find-in-page ([HtmlViewerConfig.searchQuery]/
 * [findNextRequestId]), table-of-contents jumps ([HtmlViewerConfig.scrollToAnchorId], via
 * `scrollIntoView()`), restoring [HtmlViewerConfig.initialScrollFraction], and periodically
 * reading the live scroll position back for [HtmlViewerCallbacks.onScrollFractionChanged]
 * all go through [WebView.evaluateJavascript] - a host-driven one-shot script injection.
 *
 * JavaScript **is** enabled, unlike an earlier version of this viewer - `evaluateJavascript`
 * silently does nothing at all if [android.webkit.WebSettings.setJavaScriptEnabled] is off
 * (there is no JS engine running to execute it, host-injected or not - a wrong assumption
 * this file used to document), which had made every one of the features listed above a
 * total no-op: confirmed on-device as scroll position, scroll polling, and table-of-contents
 * jumps all silently failing no matter how their surrounding Kotlin logic was fixed, because
 * the actual script never ran. The residual risk of turning script execution on for scraped,
 * third-party HTML is mitigated by this composed document being served through [GuideMediaLoader]
 * under a fixed, non-resolving virtual origin (`https://appassets.androidplatform.net`, see
 * [GUIDE_MEDIA_DOMAIN]) rather than a real network one, so an embedded script (GameFAQs
 * sanitizes user-submitted guide content, so this is a defense-in-depth measure, not a response
 * to a known issue) has no real origin's cookies/session to reach and no real network endpoint
 * to call.
 */
@Composable
fun HtmlGuideContent(
    config: HtmlViewerConfig,
    callbacks: HtmlViewerCallbacks,
) {
    val context = LocalContext.current
    val mediaLoader = remember { GuideMediaLoader(context) }
    val webView = remember { buildGuideWebView(context, callbacks, config.isDarkTheme, mediaLoader) }

    // Keyed on fontScale/isDarkTheme too, not just html - a font-size or theme change has to
    // rebuild the whole document (the CSS is baked into it, see buildGuideHtmlDocument), so
    // this effect also fires for those, not only a genuine page navigation. It used to always
    // restore config.initialScrollFraction regardless of *why* it fired - correct the one time
    // this is a real navigation to a new/resumed page, but wrong for a same-page reload
    // triggered by adjusting font size or the system theme changing: that silently snapped
    // the reader back to wherever the guide was originally opened, discarding any scrolling
    // done since - confirmed on-device as "changing font size reloads the page from the top."
    // lastLoadedHtml tracks which page is actually currently loaded so a same-page reload can
    // capture the live scroll position first and restore that instead.
    val lastLoadedHtml = remember { mutableStateOf<String?>(null) }
    // Gates the WebView's own visibility (not Compose's, see the AndroidView call below) so a
    // genuine page navigation renders fully scrolled-into-position before it's ever shown,
    // rather than flashing the top of the page first and then visibly jumping once the
    // restore script runs. Left true across a same-page reload (font/theme change) - that
    // path already re-restores the live scroll fraction it just captured, so there's nothing
    // to hide.
    val contentVisible = remember { mutableStateOf(false) }
    LaunchedEffect(config.html, config.fontScale, config.isDarkTheme) {
        loadAndRevealPage(webView, config, mediaLoader, lastLoadedHtml, contentVisible)
    }

    LaunchedEffect(config.searchQuery) {
        if (config.searchQuery.isBlank()) webView.clearMatches() else webView.findAllAsync(config.searchQuery)
    }

    LaunchedEffect(config.findNextRequestId) {
        if (config.findNextRequestId > 0) webView.findNext(true)
    }

    // A TOC entry on a different page (see GameGuideViewerScreen.onEntrySelected) sets
    // currentPageIndex and scrollToAnchorId in the same synchronous update - the former only
    // reaches this composable later, once the page content it triggers has actually loaded and
    // propagated into config.html, restarting the OTHER LaunchedEffect above to load it into
    // the WebView. Firing this scroll immediately raced that navigation: it evaluated against
    // whatever page was STILL loaded (the old one), where the target id doesn't exist, silently
    // no-oping via the `?.` - confirmed on-device as a cross-page TOC jump always landing on the
    // new page's top instead of the entry's real location, while a same-page entry (no
    // navigation to race) worked fine. latestConfig - not config directly - since this
    // coroutine's own closure is frozen to whatever config was current when it last (re)started,
    // and it must see config.html actually change over its wait without itself restarting (this
    // effect's key, scrollToAnchorId, isn't what changes during that wait).
    val latestConfig = rememberUpdatedState(config)
    LaunchedEffect(config.scrollToAnchorId) {
        val id = config.scrollToAnchorId ?: return@LaunchedEffect
        scrollToAnchorWhenReady(id, webView, latestConfig, lastLoadedHtml, contentVisible)
        callbacks.onScrollToAnchorHandled()
    }

    // Keyed on config.html, NOT webView - webView is the same stable instance for the whole
    // viewer session, but each page navigation needs its OWN lastReported baseline (that
    // page's own initialScrollFraction: the real resumed fraction on the page the guide was
    // opened to, 0f on any other). A webView-keyed effect never restarts across navigation,
    // so it kept comparing a newly-loaded page's fraction against a stale baseline left over
    // from whichever page the guide happened to open on - confirmed on-device as "remembers
    // which page, but not the scroll position on it" once page-index persistence itself was
    // fixed separately.
    //
    // Waits out SCROLL_INITIAL_SETTLE_MILLIS before its very first check - this effect starts
    // concurrently with the OTHER effect above that loads the page and restores
    // initialScrollFraction (loadDataWithBaseURL -> awaitScrollableLayout, up to
    // SCROLL_LAYOUT_POLL_ATTEMPTS * SCROLL_LAYOUT_POLL_INTERVAL_MILLIS -> the actual restore
    // script). Checking immediately raced that restore: it read the still-loading (or
    // previous) page's near-zero scroll position and reported it right away, silently
    // overwriting the just-read resumed fraction back to ~0 on every single reopen - a worse
    // regression than the dead-zone-on-quick-close problem it was meant to fix.
    LaunchedEffect(config.html) {
        delay(SCROLL_INITIAL_SETTLE_MILLIS)
        var lastReported = config.initialScrollFraction
        while (true) {
            val fraction = evaluateNumber(webView, SCROLL_FRACTION_EXPRESSION)
            if (fraction != null && abs(fraction - lastReported) >= SCROLL_REPORT_THRESHOLD) {
                lastReported = fraction
                callbacks.onScrollFractionChanged(fraction)
            }
            delay(SCROLL_POLL_INTERVAL_MILLIS)
        }
    }

    AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize().alpha(if (contentVisible.value) 1f else 0f))
}

/**
 * The body of [HtmlGuideContent]'s page-loading effect - pulled out purely to keep that
 * composable under detekt's length/complexity thresholds, not for reuse elsewhere.
 */
private suspend fun loadAndRevealPage(
    webView: WebView,
    config: HtmlViewerConfig,
    mediaLoader: GuideMediaLoader,
    lastLoadedHtml: MutableState<String?>,
    contentVisible: MutableState<Boolean>,
) {
    // Set before the new document loads (not left for the CSS background to establish once
    // rendered) so a theme change while a guide is already open doesn't itself produce the
    // same white-flash-before-the-real-background gap this is fixing.
    webView.setBackgroundColor(backgroundColorInt(config.isDarkTheme))
    val isSamePage = config.html == lastLoadedHtml.value
    // Only the very first page this WebView instance ever shows needs the software-layer trick
    // below - see buildGuideWebView's kdoc for why that race exists at all. By the second
    // navigation the WebView is already a stable, already-composited part of the hierarchy;
    // there's nothing left to race against.
    val isFirstReveal = lastLoadedHtml.value == null
    if (!isSamePage) contentVisible.value = false
    val liveFraction = if (isSamePage) evaluateNumber(webView, SCROLL_FRACTION_EXPRESSION) else null
    val document = buildGuideHtmlDocument(config.html, config.isDarkTheme, config.fontScale)
    // Pointed at this guide's own directory (not a generic cache/temp location) before the
    // write, so its relative image references (see NativeImageDownloader's kdoc) resolve as
    // plain sibling files once served back out through mediaLoader.
    mediaLoader.updateDirectory(config.mediaDirectoryPath)
    writeViewerDocument(config.mediaDirectoryPath, document)
    loadAndAwaitFinished(webView, GUIDE_MEDIA_DOCUMENT_URL, mediaLoader)
    lastLoadedHtml.value = config.html
    awaitScrollableLayout(webView)
    val targetFraction = liveFraction ?: config.initialScrollFraction
    restoreScrollFraction(webView, targetFraction)
    webView.revealOnFirstFrameLayer(isFirstReveal) { contentVisible.value = true }
}

/**
 * The body of [HtmlGuideContent]'s scroll-to-anchor effect - pulled out purely to keep that
 * composable under detekt's length/complexity thresholds, not for reuse elsewhere.
 *
 * A same-page entry (see [GameGuideViewerScreen.onEntrySelected]) never needs this wait at all -
 * [webView] already has the right page loaded, so [latestConfig]'s html already matches
 * [lastLoadedHtml] and [contentVisible] is already true. A cross-page entry, by contrast, always
 * arrives here on a *freshly mounted* [HtmlGuideContent] instance (a page change flips
 * `GameGuidesUiState.Viewing.isLoadingContent` true then false, which unmounts/remounts
 * [GuideContentArea] - see [PromotePendingAnchorWhenLoaded]'s kdoc for why `scrollToAnchorId`
 * itself is only ever set on this fresh mount, once its target page's content is confirmed
 * loaded), where `lastLoadedHtml` genuinely starts null and only catches up once the OTHER
 * loading effect finishes writing/loading/laying out that page - this wait is what keeps this
 * effect from evaluating `getElementById` before that element exists in the DOM yet.
 */
private suspend fun scrollToAnchorWhenReady(
    id: String,
    webView: WebView,
    latestConfig: State<HtmlViewerConfig>,
    lastLoadedHtml: MutableState<String?>,
    contentVisible: MutableState<Boolean>,
) {
    var attempts = 0
    while ((latestConfig.value.html != lastLoadedHtml.value || !contentVisible.value) &&
        attempts < ANCHOR_WAIT_POLL_ATTEMPTS
    ) {
        delay(ANCHOR_WAIT_POLL_INTERVAL_MILLIS)
        attempts++
    }
    val encodedId = Json.encodeToString(id)
    webView.evaluateJavascript("document.getElementById($encodedId)?.scrollIntoView({block: 'start'});", null)
}

/**
 * Runs [reveal] (setting `contentVisible.value = true`) bracketed by a brief software layer
 * type for [isFirstReveal] only, restored to hardware right after - see [buildGuideWebView]'s
 * kdoc for why this WebView needs that for its very first frame, and only that one. Extracted
 * out of [HtmlGuideContent]'s own loading effect purely to keep that composable's cyclomatic
 * complexity down - these two conditionals contribute nothing to that function's own logic.
 */
private fun WebView.revealOnFirstFrameLayer(
    isFirstReveal: Boolean,
    reveal: () -> Unit,
) {
    if (isFirstReveal) setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
    reveal()
    if (isFirstReveal) post { setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null) }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun buildGuideWebView(
    context: Context,
    callbacks: HtmlViewerCallbacks,
    isDarkTheme: Boolean,
    mediaLoader: GuideMediaLoader,
): WebView =
    WebView(context).apply {
        // GUIDE_MEDIA_DOCUMENT_URL is a fixed URL reused across every guide/page/theme/font
        // change (see this file's kdoc) - its underlying file changes every time, but its URL
        // string never does, so the normal HTTP cache a real https:// origin would get must be
        // disabled, or a stale previous guide's page could be served back out of cache instead
        // of reaching mediaLoader (and its now-current directory) at all.
        settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        // A hardware-accelerated WebView renders onto its own compositor surface, separate
        // from the rest of this screen's plain Compose-drawn content (the header/footer) -
        // on some devices this lets the WebView's own first frame reach the screen before a
        // full window redraw has flushed everything else, so the surrounding Compose chrome
        // (already composed, just not yet painted) visibly lags behind it by up to roughly a
        // second on first open. This WebView is deliberately left on the platform default
        // (hardware-backed) layer type here, not forced to software - HtmlGuideContent's own
        // loading effect briefly forces software for just the one frame where content first
        // becomes visible (see its isFirstReveal handling), immediately restoring hardware
        // afterward. An earlier version forced software for this WebView's entire lifetime
        // instead: confirmed on-device as measurably slower page loads (software rasterization
        // competing for CPU time with the page's own parse/layout/JS work, even while the
        // WebView sits invisible during that load) AND as the actual cause of a related but
        // distinct problem, fast scrolling through a real guide's text AND images alike (not
        // just images - ruling out a per-image loading cause) visibly outrunning software
        // rendering's rasterization and showing blank tiles that painted in shortly after.
        // A WebView's own default background is opaque white, painted immediately on
        // creation/attach - before loadDataWithBaseURL has even started, let alone finished
        // rendering the CSS background buildGuideHtmlDocument bakes into the document itself.
        // In dark theme this showed as a white flash for the gap between the WebView
        // appearing and its real (dark) page content taking over. Setting the matching
        // background color here means the correct color is already there from the very first
        // frame, with nothing to flash past.
        setBackgroundColor(backgroundColorInt(isDarkTheme))
        settings.javaScriptEnabled = true
        webViewClient =
            object : WebViewClient() {
                // Every navigation attempt is blocked (this never turns into a live browser -
                // see this file's own kdoc), but a fragment carried on the tapped link (the
                // common case for this guide's own in-content chapter/TOC links) is reported
                // back to the host via onInternalAnchorTapped rather than just discarded, so
                // GameGuideViewerScreen can resolve it against the guide's real GuideTocEntry
                // list and jump there itself.
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    request.url.fragment?.let(callbacks.onInternalAnchorTapped)
                    return true
                }

                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? = mediaLoader.intercept(request)
            }
        setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            callbacks.onFindResult(activeMatchOrdinal, numberOfMatches)
        }
        // A WebView fully owns its own native touch dispatch (needed for its own
        // scrolling/zooming), so a Compose-level pointerInput sibling never sees a tap that
        // lands on it - see GuideContentArea's own tap detector, which handles the plain-text
        // branch instead. onSingleTapConfirmed (not onSingleTapUp) waits out the double-tap
        // timeout first, so this doesn't fire once per tap of a genuine double-tap-to-zoom
        // gesture.
        val tapDetector =
            GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        callbacks.onTap()
                        return false
                    }
                },
            )
        setOnTouchListener { _, event ->
            tapDetector.onTouchEvent(event)
            false
        }
    }

/**
 * Loads [url] (always [GUIDE_MEDIA_DOCUMENT_URL] in practice - see [HtmlGuideContent]) and
 * suspends until the WebView's own [WebViewClient.onPageFinished] fires, rather than racing it
 * with an immediate `evaluateJavascript` check right after [WebView.loadUrl] returns (which is
 * fire-and-forget - the load hasn't actually started rendering yet). Confirmed on-device via
 * temporary logging: the very first `document.documentElement.scrollHeight` poll routinely read
 * back a near-empty, not-yet-laid-out page (e.g. 8px) which still passed a bare "> 0" check, so
 * the scroll-position restore ran against a document that hadn't actually rendered its real
 * (thousands-of-pixels) content yet and always landed at the top - the same
 * [onPageFinished]-based pattern `GameFaqsBrowserBridge.navigateAndAwaitLoad` already uses for
 * exactly this reason.
 *
 * `loadUrl` against [mediaLoader]'s virtual origin, not [WebView.loadDataWithBaseURL] (this
 * composable's previous mechanism) or a real `file://` path (briefly tried in between - see
 * [writeViewerDocument]'s kdoc for why that failed outright with `net::ERR_ACCESS_DENIED`).
 * `loadDataWithBaseURL` base64-encodes the whole document string internally before handing it
 * to Chromium, which for a real image-heavy guide page (confirmed on-device) can be tens of MB
 * on top of the string that already is, throwing an OutOfMemoryError inside `AwContents.
 * loadDataWithBaseURL` -> `Base64.encodeToString`. Serving a real file through [mediaLoader] has
 * no such second encoding pass, and lets sibling image files under the same directory resolve
 * as plain relative paths instead of needing to be embedded as base64 text in the first place.
 * This temporary client must keep the same [shouldInterceptRequest] delegation as the permanent
 * one [buildGuideWebView] installs - it fully replaces that client for the duration of this one
 * load, and the document's own embedded images request through the same interceptor as the
 * document itself.
 */
private suspend fun loadAndAwaitFinished(
    webView: WebView,
    url: String,
    mediaLoader: GuideMediaLoader,
) {
    val originalClient = webView.webViewClient
    try {
        suspendCancellableCoroutine { continuation ->
            webView.webViewClient =
                object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean = true

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? = mediaLoader.intercept(request)

                    override fun onPageFinished(
                        view: WebView,
                        url: String?,
                    ) {
                        webView.webViewClient = originalClient
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            webView.loadUrl(url)
        }
    } finally {
        webView.webViewClient = originalClient
    }
}

/**
 * A secondary safety net after [loadAndAwaitFinished] - `onPageFinished` corresponds to the
 * page's load event, which in the vast majority of cases means layout has already run too,
 * but this gives a freshly-loaded, still-reflowing page a few more short polls to settle
 * before the scroll-position restore reads its final `scrollHeight`.
 */
private suspend fun awaitScrollableLayout(webView: WebView) {
    repeat(SCROLL_LAYOUT_POLL_ATTEMPTS) {
        val scrollHeight = evaluateNumber(webView, "document.documentElement.scrollHeight")
        if (scrollHeight != null && scrollHeight > 0f) return
        delay(SCROLL_LAYOUT_POLL_INTERVAL_MILLIS)
    }
}

private suspend fun evaluateNumber(
    webView: WebView,
    expression: String,
): Float? =
    suspendCancellableCoroutine { continuation ->
        webView.evaluateJavascript("($expression);") { result -> continuation.resume(result?.toFloatOrNull()) }
    }

/**
 * Awaits the restore script's own completion callback rather than firing it and moving on -
 * [HtmlGuideContent] only reveals the WebView once this returns, so it's never shown mid-scroll.
 */
private suspend fun restoreScrollFraction(
    webView: WebView,
    fraction: Float,
) {
    val script =
        "(function(){var d=document.documentElement;var s=d.scrollHeight-window.innerHeight;" +
            "window.scrollTo(0, Math.max(0, s) * $fraction);})();"
    suspendCancellableCoroutine { continuation ->
        webView.evaluateJavascript(script) {
            if (continuation.isActive) continuation.resume(Unit)
        }
    }
}

private const val SCROLL_FRACTION_EXPRESSION =
    "(function(){var d=document.documentElement;var s=d.scrollHeight-window.innerHeight;" +
        "return s>0?(window.pageYOffset/s):0;})()"

private const val SCROLL_LAYOUT_POLL_ATTEMPTS = 10
private const val SCROLL_LAYOUT_POLL_INTERVAL_MILLIS = 150L
