package com.esde.companion.domain.model

/**
 * Which display [com.esde.companion.data.gamelist.GameLaunchOverrideCoordinator] launches a
 * configured Game Launch Override app onto - a single global setting, not configurable per
 * system/game. [ThisScreen] launches on Companion's own screen (the historical, and still
 * default, behavior - see [AppLauncher][com.esde.companion.data.apps.AppLauncher]'s `displayId =
 * null`), temporarily replacing Companion's UI there. [OtherScreen] instead targets whichever
 * display Companion is *not* running on - the one ES-DE/the game itself is on - via
 * [SecondaryDisplayResolver][com.esde.companion.data.apps.SecondaryDisplayResolver].
 */
enum class GameLaunchDisplayTarget {
    ThisScreen,
    OtherScreen,
}
