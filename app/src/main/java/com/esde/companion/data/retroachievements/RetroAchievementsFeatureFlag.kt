package com.esde.companion.data.retroachievements

import com.esde.companion.BuildConfig

/**
 * RetroAchievements is complete but waiting on an ES-DE feature that hasn't shipped yet -
 * gates every RetroAchievements Settings/FAB entry point (same shape as
 * [com.esde.companion.data.thor.isAynThorDevice]) so it stays debug-build-only until then.
 */
fun retroAchievementsEnabled(): Boolean = BuildConfig.DEBUG
