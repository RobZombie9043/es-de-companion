package com.esde.companion.ui.gameguides

import android.webkit.WebView
import com.esde.companion.data.gameguides.GameFaqsBrowserBridge
import com.esde.companion.domain.gameguides.GuideDownloadProgress
import com.esde.companion.domain.gameguides.GuideTitleCleaner
import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.GameReference
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
 * to keep that function's own parameter count down (see detekt's LongParameterList). */
internal data class GuideDownloadDeps(
    val browserBridge: GameFaqsBrowserBridge,
    val saveGameGuide: SaveGameGuideUseCase,
    val clock: Clock,
)

/**
 * Downloads whatever guide the browser's WebView is currently showing and saves it against
 * [target] - a no-op returning false if the current page doesn't actually look like a guide.
 * Pulled out of [GameGuidesViewModel.saveCurrentGuide] to keep that function focused on state
 * transitions rather than the mechanics of building a [DownloadedGameGuide]; [target] carries
 * whichever game the caller is saving against (ES-DE's current one for the FAB, or an
 * explicitly-picked one for Settings > Game Guides > Add Guide - see
 * [GameGuidesViewModel.openBrowserFor]), since [saveCurrentGuide] itself is shared by both
 * paths.
 */
internal suspend fun downloadAndSaveGuide(
    deps: GuideDownloadDeps,
    webView: WebView,
    sourceUrl: String,
    target: GuideSaveTarget,
    onProgress: (GuideDownloadProgress) -> Unit,
): Boolean {
    val page = deps.browserBridge.downloadFullGuide(webView, onProgress)
    if (!page.isGuidePage || page.pages.isEmpty()) return false
    val guide =
        DownloadedGameGuide(
            id = sourceUrl.hashCode().toUInt().toString(GUIDE_ID_RADIX),
            gameReference = target.gameReference,
            // The game name is redundant once a guide is filed under that game's own library -
            // see GuideTitleCleaner's kdoc for why a simple fixed-separator strip doesn't hold
            // across guide pages.
            title = GuideTitleCleaner.clean(page.title, target.gameName),
            sourceUrl = sourceUrl,
            format = page.format,
            pageCount = page.pages.size,
            // FileGameGuideLibraryRepository recomputes this from what's actually written to
            // disk - this placeholder is never persisted.
            sizeBytes = 0L,
            downloadedAtMillis = deps.clock.millis(),
            tocEntries = page.tocEntries,
        )
    deps.saveGameGuide(guide, page.pages)
    return true
}
