package com.esde.companion.ui.gameguides

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.math.abs

private const val BASE_FONT_SIZE_PX = 16
private const val DARK_BACKGROUND_HEX = "#121212"
private const val LIGHT_BACKGROUND_HEX = "#ffffff"
private const val SCROLL_POLL_INTERVAL_MILLIS = 500L
private const val SCROLL_REPORT_THRESHOLD = 0.01f

// Longer than awaitScrollableLayout's own worst case (SCROLL_LAYOUT_POLL_ATTEMPTS *
// SCROLL_LAYOUT_POLL_INTERVAL_MILLIS = 1500ms) plus a margin for the restore script itself.
private const val SCROLL_INITIAL_SETTLE_MILLIS = 1_800L

// A syntactically valid but deliberately non-resolving origin - .invalid is reserved by RFC
// 2606 to never be a real, registerable domain. Only used to make relative-link resolution
// well-defined for the WebView (every actual link tap is already blocked, see
// shouldOverrideUrlLoading above); never an origin a script could reach real cookies or a
// real server through.
private const val OFFLINE_GUIDE_BASE_URL = "https://offline-game-guide.invalid/"

private fun backgroundColorInt(isDarkTheme: Boolean): Int =
    android.graphics.Color.parseColor(if (isDarkTheme) DARK_BACKGROUND_HEX else LIGHT_BACKGROUND_HEX)

data class HtmlViewerConfig(
    val html: String,
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
)

/**
 * Renders a saved [com.esde.companion.domain.model.GameGuideFormat.Html] guide in a
 * sandboxed offline WebView. Every link tap is blocked rather than navigating this WebView
 * out to gamefaqs.gamespot.com, since this is meant to read as an offline saved document,
 * not turn into a live browser. Find-in-page ([HtmlViewerConfig.searchQuery]/
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
 * third-party HTML is mitigated by loading with a fake, non-resolving base URL (see
 * [OFFLINE_GUIDE_BASE_URL]) rather than the real `gamefaqs.gamespot.com` origin the content
 * came from, so an embedded script (GameFAQs sanitizes user-submitted guide content, so this
 * is a defense-in-depth measure, not a response to a known issue) has no real origin's
 * cookies/session to reach and no real network endpoint under that origin to call.
 */
@Composable
fun HtmlGuideContent(
    config: HtmlViewerConfig,
    callbacks: HtmlViewerCallbacks,
) {
    val context = LocalContext.current
    val webView = remember { buildGuideWebView(context, callbacks, config.isDarkTheme) }

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
        // Set before the new document loads (not left for the CSS background to establish
        // once rendered) so a theme change while a guide is already open doesn't itself
        // produce the same white-flash-before-the-real-background gap this is fixing.
        webView.setBackgroundColor(backgroundColorInt(config.isDarkTheme))
        val isSamePage = config.html == lastLoadedHtml.value
        if (!isSamePage) contentVisible.value = false
        val liveFraction = if (isSamePage) queryScrollFraction(webView) else null
        val document = buildGuideHtmlDocument(config.html, config.isDarkTheme, config.fontScale)
        // A non-null base URL so an in-line HTML guide's own relative links (that
        // shouldOverrideUrlLoading above blocks anyway) resolve without WebView warnings -
        // but a fake, non-resolving one, not the real gamefaqs.gamespot.com the content came
        // from, now that JS is enabled (see this composable's kdoc for why that origin choice
        // matters once script execution is on). Images are already embedded as data URIs at
        // download time (see GameFaqsBrowserBridge), so a real origin isn't needed for those.
        loadAndAwaitFinished(webView, document)
        lastLoadedHtml.value = config.html
        awaitScrollableLayout(webView)
        val targetFraction = liveFraction ?: config.initialScrollFraction
        restoreScrollFraction(webView, targetFraction)
        contentVisible.value = true
    }

    LaunchedEffect(config.searchQuery) {
        if (config.searchQuery.isBlank()) webView.clearMatches() else webView.findAllAsync(config.searchQuery)
    }

    LaunchedEffect(config.findNextRequestId) {
        if (config.findNextRequestId > 0) webView.findNext(true)
    }

    LaunchedEffect(config.scrollToAnchorId) {
        val id = config.scrollToAnchorId ?: return@LaunchedEffect
        val encodedId = Json.encodeToString(id)
        webView.evaluateJavascript("document.getElementById($encodedId)?.scrollIntoView({block: 'start'});", null)
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
            val fraction = queryScrollFraction(webView)
            if (fraction != null && abs(fraction - lastReported) >= SCROLL_REPORT_THRESHOLD) {
                lastReported = fraction
                callbacks.onScrollFractionChanged(fraction)
            }
            delay(SCROLL_POLL_INTERVAL_MILLIS)
        }
    }

    AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize().alpha(if (contentVisible.value) 1f else 0f))
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun buildGuideWebView(
    context: Context,
    callbacks: HtmlViewerCallbacks,
    isDarkTheme: Boolean,
): WebView =
    WebView(context).apply {
        // A hardware-accelerated WebView renders onto its own compositor surface, separate
        // from the rest of this screen's plain Compose-drawn content (the header/footer) -
        // on some devices this lets the WebView's own first frame reach the screen before a
        // full window redraw has flushed everything else, so the surrounding Compose chrome
        // (already composed, just not yet painted) visibly lags behind it by up to roughly a
        // second on first open. Forcing this specific WebView onto a software layer keeps its
        // drawing on the same path as its Compose siblings, trading a little rendering
        // headroom (irrelevant for a mostly-static text/image guide page, unlike a video or
        // animation-heavy page) for both appearing together.
        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
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
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean = true
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
 * Loads [html] and suspends until the WebView's own [WebViewClient.onPageFinished] fires,
 * rather than racing it with an immediate `evaluateJavascript` check right after
 * [WebView.loadDataWithBaseURL] returns (which is fire-and-forget - the load hasn't actually
 * started rendering yet). Confirmed on-device via temporary logging: the very first
 * `document.documentElement.scrollHeight` poll routinely read back a near-empty, not-yet-
 * laid-out page (e.g. 8px) which still passed a bare "> 0" check, so the scroll-position
 * restore ran against a document that hadn't actually rendered its real (thousands-of-pixels)
 * content yet and always landed at the top - the same [onPageFinished]-based pattern
 * `GameFaqsBrowserBridge.navigateAndAwaitLoad` already uses for exactly this reason.
 */
private suspend fun loadAndAwaitFinished(
    webView: WebView,
    html: String,
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

                    override fun onPageFinished(
                        view: WebView,
                        url: String?,
                    ) {
                        webView.webViewClient = originalClient
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            webView.loadDataWithBaseURL(OFFLINE_GUIDE_BASE_URL, html, "text/html", "utf-8", null)
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

private suspend fun queryScrollFraction(webView: WebView): Float? = evaluateNumber(webView, SCROLL_FRACTION_EXPRESSION)

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
    suspendCancellableCoroutine { continuation ->
        webView.evaluateJavascript(restoreScrollFractionScript(fraction)) {
            if (continuation.isActive) continuation.resume(Unit)
        }
    }
}

private fun restoreScrollFractionScript(fraction: Float): String =
    "(function(){var d=document.documentElement;var s=d.scrollHeight-window.innerHeight;" +
        "window.scrollTo(0, Math.max(0, s) * $fraction);})();"

private const val SCROLL_FRACTION_EXPRESSION =
    "(function(){var d=document.documentElement;var s=d.scrollHeight-window.innerHeight;" +
        "return s>0?(window.pageYOffset/s):0;})()"

private const val SCROLL_LAYOUT_POLL_ATTEMPTS = 10
private const val SCROLL_LAYOUT_POLL_INTERVAL_MILLIS = 150L

/**
 * Wraps a saved in-line HTML guide's bare content markup (just the body of `#faqwrap`, no
 * `<html>`/`<head>`/stylesheet of its own - GameFAQs' own CSS never shipped with it) in a
 * minimal, theme-aware document so tables/headings/links are actually readable instead of
 * unstyled browser-default markup - GameFAQs' own site classes on this content expect a
 * stylesheet this app never downloaded, so this targets plain tag selectors instead of
 * trying to reproduce their exact class names.
 */
private fun buildGuideHtmlDocument(
    bodyHtml: String,
    isDarkTheme: Boolean,
    fontScale: Float,
): String {
    val background = if (isDarkTheme) DARK_BACKGROUND_HEX else LIGHT_BACKGROUND_HEX
    val foreground = if (isDarkTheme) "#e0e0e0" else "#1a1a1a"
    val border = if (isDarkTheme) "#555555" else "#cccccc"
    val link = if (isDarkTheme) "#8ab4f8" else "#1a56db"
    val tableStripe = if (isDarkTheme) "#1e1e1e" else "#f5f5f5"
    val fontSizePx = (BASE_FONT_SIZE_PX * fontScale).toInt()
    val css =
        """
        body { font-family: sans-serif; font-size: ${fontSizePx}px; line-height: 1.5; padding: 16px;
               background: $background; color: $foreground; word-wrap: break-word; }
        h1, h2, h3, h4 { margin-top: 1.3em; margin-bottom: 0.4em; }
        p { margin: 0.6em 0; }
        table { border-collapse: collapse; width: 100%; margin: 1em 0; }
        th, td { border: 1px solid $border; padding: 6px 8px; text-align: left; vertical-align: top; }
        tr:nth-child(even) td { background: $tableStripe; }
        img { max-width: 100%; height: auto; }
        a { color: $link; }
        pre { white-space: pre-wrap; }
        """.trimIndent()
    return "<html><head><meta charset=\"utf-8\"><style>$css</style></head><body>$bodyHtml</body></html>"
}
