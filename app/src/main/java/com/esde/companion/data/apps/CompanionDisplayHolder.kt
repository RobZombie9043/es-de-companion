package com.esde.companion.data.apps

/**
 * The display id Companion's own Activity is currently running on, recorded by
 * [com.esde.companion.ui.MainActivity] (a real, visual Activity Context - see
 * [SecondaryDisplayResolver.currentDisplayId]'s kdoc for why that matters) so that a
 * background coroutine holding only an application Context - currently just
 * [com.esde.companion.data.gamelist.GameLaunchOverrideCoordinator] - can still resolve "the
 * other screen" via [SecondaryDisplayResolver.secondaryDisplayId]'s `knownCurrentDisplayId`
 * parameter instead of crashing trying to derive it itself. One shared instance lives in
 * `AppContainer` for the app's whole lifetime, same shape as `ProcessActivityVisibilityRepository`.
 * Null only before MainActivity's first `onStart()`.
 */
class CompanionDisplayHolder {
    @Volatile
    var displayId: Int? = null
}
