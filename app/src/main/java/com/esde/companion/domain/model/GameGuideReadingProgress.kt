package com.esde.companion.domain.model

/**
 * Where the user left off in one downloaded guide - see `GameGuideSettingsRepository`.
 * [pageIndex] is which saved page the guide was on (always 0 for a single-page guide);
 * [scrollFraction] is 0f (top) to 1f (bottom) of *that page's* scrollable content. Both are
 * restored together the next time the guide is opened. Overall "percent read" for a
 * multi-page guide is `(pageIndex + scrollFraction) / DownloadedGameGuide.pageCount`.
 */
data class GameGuideReadingProgress(
    val guideId: String,
    val scrollFraction: Float,
    val lastOpenedAtMillis: Long,
    val pageIndex: Int = 0,
)
