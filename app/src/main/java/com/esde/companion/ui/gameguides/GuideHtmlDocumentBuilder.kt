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

/**
 * Wraps a saved in-line HTML guide's bare content markup (just the body of `#faqwrap`, no
 * `<html>`/`<head>`/stylesheet of its own - GameFAQs' own CSS never shipped with it) in a
 * minimal, theme-aware document so tables/headings/links are actually readable instead of
 * unstyled browser-default markup - GameFAQs' own site classes on this content expect a
 * stylesheet this app never downloaded, so this targets plain tag selectors instead of
 * trying to reproduce their exact class names.
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
    return "<html><head><meta charset=\"utf-8\"><style>$css</style></head>" +
        "<body>${TAP_TOGGLE_SCRIPT.trimIndent()}$bodyHtml$colorFixScript</body></html>"
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
