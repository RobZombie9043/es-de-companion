package com.esde.companion.domain.model

/**
 * Whether a downloaded guide's content is plain text (rendered in a reflowed, monospace
 * Text list) or HTML (rendered in a sandboxed offline WebView) - see [DownloadedGameGuide].
 */
enum class GameGuideFormat {
    PlainText,
    Html,
}
