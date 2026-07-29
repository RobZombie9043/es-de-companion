package com.esde.companion.domain.model

/** Which display an app should be launched on. Mirrors the two-screen setup this app
 * targets - see SecondaryDisplayResolver. */
enum class LaunchLocation {
    ThisScreen,
    OtherScreen,
}