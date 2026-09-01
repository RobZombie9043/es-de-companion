package com.esde.companion.ui.gameguides

import android.util.Log
import android.webkit.WebView
import com.esde.companion.data.gameguides.GameFaqsBrowserBridge
import com.esde.companion.data.gameguides.GuidePageContentProcessor
import com.esde.companion.domain.gameguides.GuideDownloadProgress
import com.esde.companion.domain.gameguides.GuideTitleCleaner
import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.GameGuideFormat
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.repository.GuidePageContent
import com.esde.companion.domain.usecase.ResolveGameGuideMediaDirectoryUseCase
import com.esde.companion.domain.usecase.SaveGameGuideUseCase
import java.time.Clock

internal const val GAMEFAQS_SEARCH_URL = "https://gamefaqs.gamespot.com/search?game="
private const val GUIDE_ID_RADIX = 16

/** Which game a download should be saved against - [gameName] is needed alongside
 * [gameReference] purely for [GuideTitleCleaner]'s own title-cleaning, not for identity. */
internal data class GuideSaveTarget(
    val gameReference: GameReference,
    val gameName: String,
)

/** The pieces [downloadAndSaveGuide] needs that don't change between calls - bundled together
 * to keep that function's own parameter count down (see detekt's LongParameterList).
 * [pageProcessor] embeds images/tags headings for one page at a time as it's about to be
 * saved - see [downloadAndSaveGuide]'s kdoc for why that happens here rather than inside
 * [GameFaqsBrowserBridge] itself. */
internal data class GuideDownloadDeps(
    val browserBridge: GameFaqsBrowserBridge,
    val saveGameGuide: SaveGameGuideUseCase,
    val resolveMediaDirectory: ResolveGameGuideMediaDirectoryUseCase,
    val clock: Clock,
    val pageProcessor: GuidePageContentProcessor = GuidePageContentProcessor(),
)

/**
 * Downloads whatever guide the browser's WebView is currently showing and saves it against
 * [target] - returns false if the current page doesn't actually look like a guide, or if
 * [GuideDownloadDeps.saveGameGuide] itself fails (confirmed necessary, not defensive: this used
 * to unconditionally return true regardless of that call's own `Result`, so a real save failure
 * - a disk write error, or an exception thrown while embedding a page's images - silently looked
 * like success to the caller, which then navigated back to the guide list as if the download had
 * completed. [GuideDownloadDeps.saveGameGuide]'s failure is also logged, since this is otherwise
 * a dead end for figuring out why a guide never appeared in the library).
 * Pulled out of [GameGuidesViewModel.saveCurrentGuide] to keep that function focused on state
 * transitions rather than the mechanics of building a [DownloadedGameGuide]; [target] carries
 * whichever game the caller is saving against (ES-DE's current one for the FAB, or an
 * explicitly-picked one for Settings > Game Guides > Add Guide - see
 * [GameGuidesViewModel.openBrowserFor]), since [saveCurrentGuide] itself is shared by both
 * paths.
 *
 * [GameFaqsBrowserBridge.downloadFullGuide] only walks chapters and returns their raw HTML -
 * embedding (saving each page's images as real files and heading-tagging, via
 * [GuideDownloadDeps.pageProcessor] - see its kdoc for why images are saved as files rather
 * than inlined as base64 text) happens here, one page at a time, immediately before
 * [GuideDownloadDeps.saveGameGuide] writes that page to disk. Confirmed necessary: embedding
 * every chapter up front into one in-memory list (the previous design) could reach hundreds of
 * MB for a long, image-heavy guide before a single byte was saved - the same OutOfMemoryError
 * shape already fixed on the viewer's own page-loading path, just unaddressed here until now.
 */
internal suspend fun downloadAndSaveGuide(
    deps: GuideDownloadDeps,
    webView: WebView,
    sourceUrl: String,
    target: GuideSaveTarget,
    onProgress: (GuideDownloadProgress) -> Unit,
): Boolean =
    withNetworkImagesDisabled(webView) {
        val page = deps.browserBridge.downloadFullGuide(webView, onProgress)
        if (!page.isGuidePage || page.pages.isEmpty()) return@withNetworkImagesDisabled false
        val totalPages = page.pages.size
        val needsEmbedding = page.format == GameGuideFormat.Html
        val guide =
            DownloadedGameGuide(
                id = sourceUrl.hashCode().toUInt().toString(GUIDE_ID_RADIX),
                gameReference = target.gameReference,
                // The game name is redundant once a guide is filed under that game's own
                // library - see GuideTitleCleaner's kdoc for why a simple fixed-separator
                // strip doesn't hold across guide pages.
                title = GuideTitleCleaner.clean(page.title, target.gameName),
                sourceUrl = sourceUrl,
                format = page.format,
                pageCount = totalPages,
                // FileGameGuideLibraryRepository recomputes both of these from what's actually
                // embedded/written to disk - these placeholders are never persisted.
                sizeBytes = 0L,
                downloadedAtMillis = deps.clock.millis(),
                tocEntries = emptyList(),
            )
        val mediaDirectoryPath = deps.resolveMediaDirectory(guide.id)
        deps.saveGameGuide(guide) { index ->
            onProgress(GuideDownloadProgress.EmbeddingImages(index + 1, totalPages))
            if (needsEmbedding) {
                val embedded =
                    deps.pageProcessor.process(webView, page.pages[index], pageIndex = index, mediaDirectoryPath)
                GuidePageContent(html = embedded.html, tocEntries = embedded.tocEntries)
            } else {
                GuidePageContent(html = page.pages[index])
            }
        }.onFailure { error ->
            Log.e("GameGuides", "Failed to save guide '${guide.title}' (${guide.sourceUrl})", error)
        }.isSuccess
    }

/**
 * Runs [block] (a chapter walk plus, now, its per-page embedding - see
 * [downloadAndSaveGuide]'s kdoc) with the WebView's own network image loading turned off,
 * restoring it afterward regardless of how [block] finishes. Each chapter navigation already
 * waits for [android.webkit.WebViewClient.onPageFinished], which doesn't fire until the
 * WebView has also fetched every on-page image for rendering; embedding then does its own
 * `container.innerHTML = html` DOM pass, which triggers the same real network fetch for every
 * `<img>` it contains even though that container is never attached to the visible document -
 * both are images this download flow immediately discards, since [NativeImageDownloader]
 * fetches them separately anyway. A real 18-chapter, image-heavy guide (confirmed while
 * building - since reverted - Zelda Dungeon support, 70-180 images per chapter there) was
 * visibly slower without this, since every chapter's navigation was blocked on the WebView's
 * own redundant image fetches, not just its HTML. GameFAQs guides are typically lighter, but
 * the same wasted-fetch problem applies to any multi-chapter guide this downloads, so the fix
 * is kept regardless. Must stay a single wrap spanning the whole walk-then-embed span, not one
 * wrap per phase - two independent, nested `true`-then-`finally`-`false` scopes would have the
 * inner one incorrectly re-enable network images the moment it finishes, before the outer
 * scope's own work (embedding) is done.
 */
private suspend fun <T> withNetworkImagesDisabled(
    webView: WebView,
    block: suspend () -> T,
): T {
    webView.settings.blockNetworkImage = true
    try {
        return block()
    } finally {
        webView.settings.blockNetworkImage = false
    }
}
