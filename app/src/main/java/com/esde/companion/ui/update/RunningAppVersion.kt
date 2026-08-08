package com.esde.companion.ui.update

/**
 * The app's own version, as read from `BuildConfig` - bundled into one value so
 * [UpdateViewModel]'s constructor doesn't carry `name`/`code` as two separate params.
 */
data class RunningAppVersion(
    val name: String,
    val code: Int,
)
