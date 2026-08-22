package com.esde.companion.domain.model

/** One row of a single leaderboard's entries - backs the per-leaderboard accordion drill-down. */
data class LeaderboardEntry(
    val rank: Long,
    val user: String,
    val formattedScore: String,
    val submittedAtMillis: Long?,
)
