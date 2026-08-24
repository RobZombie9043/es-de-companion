package com.esde.companion.domain.model

/**
 * Backs the achievements screen's "Supported Hashes" kebab-menu entry - see
 * `GetGameHashSupportUseCase`. [currentHash] is the current game's own ROM hash parsed from
 * gamelist.xml ([GameRomHash.value]), null when none could be resolved. [supportedHashes] is
 * the already-resolved RetroAchievements game's full [RetroAchievementsCandidateGame.hashes]
 * list, independent of whether [currentHash] matches any of them - the UI decides how to
 * render that comparison, this model just carries both sides of it.
 */
data class GameHashSupport(
    val currentHash: String?,
    val supportedHashes: List<String>,
)
