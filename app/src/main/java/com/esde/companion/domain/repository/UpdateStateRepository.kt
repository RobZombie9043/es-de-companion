package com.esde.companion.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Tracks which app version's changelog was last shown to the user, so the "what's new"
 * prompt fires once per version bump rather than on every app start. Kept separate from
 * [UpdateRepository] (network fetch) and from [OnboardingRepository] (unrelated setup
 * concerns) - a small, focused repository per concern, same as
 * [LastKnownContextRepository]/[AppDrawerSettingsRepository]/[DockSettingsRepository].
 */
interface UpdateStateRepository {
    fun observeLastSeenChangelogVersionCode(): Flow<Int?>

    suspend fun setLastSeenChangelogVersionCode(versionCode: Int)
}
