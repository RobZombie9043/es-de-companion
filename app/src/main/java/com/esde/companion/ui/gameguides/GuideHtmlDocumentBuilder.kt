package com.esde.companion.ui.gameguides

internal const val BASE_FONT_SIZE_PX = 16
internal const val DARK_BACKGROUND_HEX = "#121212"
internal const val LIGHT_BACKGROUND_HEX = "#ffffff"

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
    return "<html><head><meta charset=\"utf-8\"><style>$css</style></head><body>$bodyHtml</body></html>"
}
