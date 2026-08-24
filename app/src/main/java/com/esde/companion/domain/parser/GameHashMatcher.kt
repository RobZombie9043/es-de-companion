package com.esde.companion.domain.parser

import com.esde.companion.domain.model.RetroAchievementsCandidateGame

/**
 * Matches a ROM hash (from gamelist.xml, see `GameRomHashRepository`) against a console's
 * cached RetroAchievements game list. A sibling of [GameTitleMatcher], not a function on it
 * - that object's kdoc is entirely about title-string comparison policy, none of which
 * applies to comparing hashes. A hash match is the only identification signal in this app
 * that is exact rather than heuristic (it identifies the specific ROM dump, not a guess from
 * a name), which is why `ResolveRetroAchievementsGameUseCase` tries it ahead of every title
 * tier - including [TitleMatchMethod.ExactTitle], which is still just string equality.
 */
object GameHashMatcher {
    /**
     * Case-insensitive, trimmed comparison against each candidate's [RetroAchievementsCandidateGame.hashes] -
     * RA's own hashes are lowercase hex MD5, but ES-DE's future output format isn't
     * verified yet, and a case mismatch would otherwise cause a silent, total failure of
     * the whole feature. No further validation (length/hex-format checks) is applied. If
     * more than one candidate happens to share a hash (RA occasionally logs hacks/subsets
     * under the same value), the first match wins - the same convention
     * [GameTitleMatcher.match] already uses.
     */
    fun match(
        romHash: String,
        candidates: List<RetroAchievementsCandidateGame>,
    ): RetroAchievementsCandidateGame? {
        if (romHash.isBlank()) return null
        val normalized = romHash.trim().lowercase()
        return candidates.firstOrNull { candidate -> candidate.hashes.any { it.trim().lowercase() == normalized } }
    }
}
