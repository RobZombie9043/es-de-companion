package com.esde.companion.ui.main

/**
 * ES-DE ships three built-in auto collections - all games, favorites, and last played -
 * exposed via system-select events with fixed systemShortName values of "all",
 * "favorites", and "recent" respectively. Those short names don't follow the same
 * convention as the system_logos asset files on disk for real systems, so they need an
 * explicit mapping rather than being used directly to build an asset path.
 *
 * Kept in the ui layer, not domain - same reasoning as AppStateDisplay: AppState only
 * knows the systemShortName ES-DE actually reported, and shouldn't need to know anything
 * about asset file naming. Any systemShortName not in this map (i.e. every real system)
 * is returned unchanged.
 */
private val AUTO_COLLECTION_ASSET_NAMES = mapOf(
    "all" to "auto-allgames",
    "favorites" to "auto-favorites",
    "recent" to "auto-lastplayed",
)

fun systemLogoAssetName(systemShortName: String): String =
    AUTO_COLLECTION_ASSET_NAMES[systemShortName] ?: systemShortName