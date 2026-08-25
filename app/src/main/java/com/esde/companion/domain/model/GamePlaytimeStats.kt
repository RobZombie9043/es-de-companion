package com.esde.companion.domain.model

/**
 * RA community-wide median times (in seconds) to reach three milestones for a game, from the
 * `GetGameProgression` endpoint - mirrors the "Playtime Stats" widget on a game's RA web page.
 * This is NOT the signed-in user's own playtime; it's a median across every player who's reached
 * that milestone. [beatSeconds]/[completedSeconds] are the softcore ("Casual") medians;
 * [beatHardcoreSeconds]/[masteredSeconds] are the hardcore medians - RA's "Mastered" award
 * requires hardcore completion, its softcore equivalent is "Completed" (see [progressStatusFor]'s
 * kdoc for the same Beaten/Completed/Mastered vocabulary). Any individual field is `null` when
 * too few (or zero) players have reached that milestone for RA to report a median - not a fetch
 * failure.
 */
data class GamePlaytimeStats(
    val beatSeconds: Int?,
    val beatHardcoreSeconds: Int?,
    val completedSeconds: Int?,
    val masteredSeconds: Int?,
)
