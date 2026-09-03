package com.esde.companion.ui.gameguides

internal const val BASE_FONT_SIZE_PX = 16
internal const val DARK_BACKGROUND_HEX = "#000000"
internal const val LIGHT_BACKGROUND_HEX = "#ffffff"

// A fake, never-real navigation target: shouldOverrideUrlLoading already blocks every
// navigation unconditionally (see GameGuideHtmlViewer's kdoc), so this scheme never actually
// loads anything - it exists purely so the tap-toggle script below has some URL to navigate to
// that shouldOverrideUrlLoading can recognize and treat as "toggle chrome" rather than "an
// in-content anchor link".
internal const val TAP_TOGGLE_URL = "esdeguidetap://toggle"

// A `document.body` click listener, not a native Android GestureDetector - see
// GameGuideHtmlViewer's buildGuideWebView kdoc for why a from-scratch attempt at the latter,
// racing the WebView's own internal touch/gesture handling on the same raw MotionEvent stream,
// proved intermittently unreliable (confirmed on-device across two separate fix attempts).
// A JS `click` event, by contrast, is exactly the mechanism this viewer's existing in-content
// anchor-link jump feature already relies on (via shouldOverrideUrlLoading) and which has never
// itself been reported flaky - Chromium only synthesizes `click` once its own touch-to-gesture
// recognition has already ruled out a scroll/drag, so this reuses that same disambiguation
// instead of re-implementing it. Taps on an actual link are excluded (closest('a') check) so a
// link tap only ever triggers its own anchor-jump handling, not also a chrome toggle.
//
// Placed in the document BEFORE bodyHtml (see buildGuideHtmlDocument below), not after - this
// only needs document.body to exist, not any of the guide's own content inside it, so it can
// safely run first. A large/slow-loading guide's bodyHtml (megabytes of markup/images for a
// long chapter) can take a real, user-visible amount of time for the browser to parse; placing
// this script after that content meant the click listener didn't attach until parsing had
// already gotten all the way through it, so tapping to toggle chrome silently did nothing for
// as long as the page was still loading. Running first attaches the listener essentially
// immediately regardless of how slow the rest of the page is to parse/render.
private const val TAP_TOGGLE_SCRIPT =
    """
    <script>
    document.body.addEventListener('click', function(e) {
        if (e.target.closest('a')) return;
        window.location.href = '$TAP_TOGGLE_URL';
    });
    </script>
    """

// Only <img ...> tags, not the rest of the markup - a loose ">"-based regex is good enough here
// since this is already best-effort scraped-content processing (same spirit as
// GuidePageContentProcessor's MAX_EMBEDDED_IMAGES handling), not a real HTML parser.
private val IMG_TAG_REGEX = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
private val EXISTING_LOADING_ATTR_REGEX = Regex("""\s+loading\s*=\s*"[^"]*"""", RegexOption.IGNORE_CASE)
private val IMG_OPEN_TAG_REGEX = Regex("<img", RegexOption.IGNORE_CASE)

/**
 * Marks every `<img>` in [bodyHtml] `loading="lazy"` (normalizing away any conflicting attribute
 * a scraped source page already carried) so the very first paint only has to decode whatever is
 * on-screen, not the whole page - [BACKGROUND_IMAGE_LOADER_SCRIPT] is what keeps every other
 * image loading anyway, just without blocking that first paint. See this file's kdoc on
 * [buildGuideHtmlDocument] for the full reasoning.
 */
internal fun applyLazyImageLoading(bodyHtml: String): String =
    IMG_TAG_REGEX.replace(bodyHtml) { match ->
        val stripped = match.value.replace(EXISTING_LOADING_ATTR_REGEX, "")
        stripped.replaceFirst(IMG_OPEN_TAG_REGEX, "<img loading=\"lazy\"")
    }

// Placed AFTER bodyHtml (unlike TAP_TOGGLE_SCRIPT), same spot as CLASHING_INLINE_COLOR_FIX_SCRIPT
// below - it needs every <img> already parsed into the DOM to select them, not just document.body
// to exist.
//
// The promotion to eager is deliberately deferred to the window 'load' event, not run
// immediately - a loading="lazy" image is specifically excluded from delaying that event (the
// whole reason the attribute exists), but an ordinary eager <img> is NOT, so flipping every
// image back to eager before 'load' fires undoes the deferral entirely: WebViewClient's
// onPageFinished (what loadAndAwaitStarted used to await, back when it was called
// loadAndAwaitFinished) fires at essentially the same point as 'load', so this app would end up
// waiting for every image again regardless of the lazy attribute - confirmed on-device as page
// turns still being slow to reveal despite it. Waiting for 'load' first means the page has
// already finished loading (and this app has already revealed it) before this script starts
// pulling every remaining image in - true background loading, not just a relabeled eager load.
// No completion flag to report back here (an earlier version had one) - GameGuideHtmlViewer no
// longer waits on decode completion for anything; see annotateImageDimensions'/
// awaitStableScrollHeight's kdocs for why layout correctness no longer depends on it.
private const val BACKGROUND_IMAGE_LOADER_SCRIPT =
    """
    <script>
    (function() {
        var imgs = document.querySelectorAll('img[loading="lazy"]');
        window.addEventListener('load', function() {
            for (var i = 0; i < imgs.length; i++) {
                imgs[i].loading = 'eager';
            }
        });
    })();
    </script>
    """

/**
 * Wraps a saved in-line HTML guide's bare content markup (just the body of `#faqwrap`, no
 * `<html>`/`<head>`/stylesheet of its own - GameFAQs' own CSS never shipped with it) in a
 * minimal, theme-aware document so tables/headings/links are actually readable instead of
 * unstyled browser-default markup - GameFAQs' own site classes on this content expect a
 * stylesheet this app never downloaded, so this targets plain tag selectors instead of
 * trying to reproduce their exact class names.
 *
 * Every `<img>` is also marked `loading="lazy"` ([applyLazyImageLoading]) and
 * [BACKGROUND_IMAGE_LOADER_SCRIPT] is appended - confirmed on-device that an image-heavy guide
 * page was slow to open when every image loaded eagerly upfront. First paint now only waits on
 * whatever's on-screen; everything else keeps loading in the background regardless of scroll
 * position (these are local files, not network fetches, so that's typically fast). [bodyHtml] is
 * expected to already have real `width`/`height` attributes baked into its `<img>` tags where
 * they could be resolved (see `data/gameguides/GuideImageDimensionAnnotator.kt`, applied by the
 * caller before this function runs) - that, not waiting for images to decode, is what keeps a
 * position-critical action (restoring a saved scroll position, a TOC jump) accurate despite
 * images still loading in the background; see
 * [com.esde.companion.ui.gameguides.GameGuideHtmlViewer]'s `awaitStableScrollHeight`/
 * `awaitStableAnchorPosition`.
 */
internal fun buildGuideHtmlDocument(
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
    val colorFixScript = buildClashingInlineColorFixScript(isDarkTheme, foreground)
    val lazyBodyHtml = applyLazyImageLoading(bodyHtml)
    val body = "${TAP_TOGGLE_SCRIPT.trimIndent()}$lazyBodyHtml$BACKGROUND_IMAGE_LOADER_SCRIPT$colorFixScript"
    return "<html><head><meta charset=\"utf-8\"><style>$css</style></head><body>$body</body></html>"
}

private const val CLASH_PATTERN_BLACK = """^rgba?\(0,\s*0,\s*0(,\s*1)?\)$"""
private const val CLASH_PATTERN_WHITE = """^rgba?\(255,\s*255,\s*255(,\s*1)?\)$"""

// __CLASH_PATTERN__/__FOREGROUND__ substituted textually by buildClashingInlineColorFixScript() -
// a top-level template (rather than a string built inline in that function) sidesteps the
// ktlint/detekt multiline-string-indentation disagreement documented in this project's
// CLAUDE.md Known Gotchas.
private val CLASHING_INLINE_COLOR_FIX_SCRIPT_TEMPLATE =
    """
    <script>
    (function() {
        var clash = /__CLASH_PATTERN__/;
        var nodes = document.querySelectorAll('body *');
        for (var i = 0; i < nodes.length; i++) {
            var el = nodes[i];
            if (clash.test(getComputedStyle(el).color)) {
                el.style.setProperty('color', '__FOREGROUND__', 'important');
            }
        }
    })();
    </script>
    """.trimIndent()

/**
 * Guide authors' own inline `style="color:..."`/`<font color>` markup assumed GameFAQs' white
 * page background - most commonly literal black text for emphasis, copy-pasted straight into
 * their FAQ from elsewhere. Left alone, that renders as invisible black-on-black once our own
 * background goes dark (confirmed on a real guide page - see the bug this was written for).
 * Rather than stripping every author-chosen color indiscriminately (which would also erase
 * legitimate color-coding like red warning text), this only neutralizes colors that exactly
 * clash with our chosen extreme background: pure black in dark mode, pure white in light mode.
 * `getComputedStyle` normalizes every CSS color syntax (`black`, `#000`, `#000000`, `rgb(0,0,0)`,
 * a `<font color>` attribute, ...) down to one `rgb()`/`rgba()` form, so a regex against that
 * normalized value catches all of them without needing to parse the source markup's own syntax.
 */
private fun buildClashingInlineColorFixScript(
    isDarkTheme: Boolean,
    foreground: String,
): String {
    val clashPattern = if (isDarkTheme) CLASH_PATTERN_BLACK else CLASH_PATTERN_WHITE
    return CLASHING_INLINE_COLOR_FIX_SCRIPT_TEMPLATE
        .replace("__CLASH_PATTERN__", clashPattern)
        .replace("__FOREGROUND__", foreground)
}
