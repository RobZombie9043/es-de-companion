package com.esde.companion.domain.model

/**
 * RetroAchievements has no API-side game-classification field (confirmed against
 * `GetGameList`/`GetGameExtended`/`GetGameInfoAndUserProgress`) - its site convention is a
 * tilde-delimited tag embedded directly in the title string instead (e.g. `"~Hack~ Knuckles
 * the Echidna in Sonic the Hedgehog"`). [titleTag] is that literal tag; [Retail] has none - it's
 * never searched for, only emitted when nothing else matched. This is a documented best-effort
 * heuristic given the lack of a real field, the same "documented, not guessed" convention this
 * project already uses for [com.esde.companion.domain.update.VersionComparator].
 */
enum class RetroGameType(val titleTag: String?) {
    Retail(null),
    Hack("~Hack~"),
    Homebrew("~Homebrew~"),
    Prototype("~Prototype~"),
    Unlicensed("~Unlicensed~"),
    Demo("~Demo~"),
}

/**
 * A title can carry more than one tag (e.g. `"~Homebrew~ ~Demo~ Foo"`), so every matching tag
 * is returned, case-insensitively. [RetroGameType.Retail] is emitted only when nothing else
 * matched - it has no literal tag of its own to search for.
 */
fun String.retroGameTypes(): Set<RetroGameType> {
    val matched =
        RetroGameType.entries
            .filter { it.titleTag != null }
            .filter { type -> contains(type.titleTag!!, ignoreCase = true) }
            .toSet()
    return matched.ifEmpty { setOf(RetroGameType.Retail) }
}
