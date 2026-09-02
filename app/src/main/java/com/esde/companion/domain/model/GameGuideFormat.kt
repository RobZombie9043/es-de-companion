package com.esde.companion.domain.model

/**
 * Whether a downloaded guide's content is plain text (rendered in a reflowed, monospace
 * Text list), HTML (rendered in a sandboxed offline WebView), a PDF (rendered page-by-page
 * via PdfManualRenderer, same as Game Manual), or a single Image (a plain zoomable image
 * viewer) - see [DownloadedGameGuide]. Pdf/Image are only ever produced by importing a file
 * (see GameGuideImportPicker) - GameFAQs downloads only ever produce PlainText/Html.
 */
enum class GameGuideFormat {
    PlainText,
    Html,
    Pdf,
    Image,
    ;

    companion object {
        /** Maps an imported file's extension (case-insensitive, no leading dot) to the
         * format it should be saved/viewed as - null for an unrecognized extension, which
         * the import flow treats as "can't import this file". */
        fun forFileExtension(extension: String): GameGuideFormat? =
            when (extension.lowercase()) {
                "txt" -> PlainText
                "htm", "html" -> Html
                "pdf" -> Pdf
                "png", "jpg", "jpeg", "webp", "gif", "bmp" -> Image
                else -> null
            }
    }
}
