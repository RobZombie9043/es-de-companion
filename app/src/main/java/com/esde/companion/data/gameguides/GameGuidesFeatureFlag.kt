package com.esde.companion.data.gameguides

import com.esde.companion.BuildConfig

/**
 * Game Guides depends on GameFAQs' page structure staying scrapeable, which isn't
 * guaranteed - there's no public API, same reasoning as
 * [com.esde.companion.data.retroachievements.retroAchievementsEnabled]. Gates every Game
 * Guides Settings/FAB entry point until validated on-device across a real range of guides.
 */
fun gameGuidesEnabled(): Boolean = BuildConfig.DEBUG
