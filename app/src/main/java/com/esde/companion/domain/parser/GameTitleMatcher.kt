package com.esde.companion.domain.parser

import com.esde.companion.domain.model.RetroAchievementsCandidateGame

/**
 * Matches ES-DE's display game name against a console's cached RetroAchievements game
 * list. Pure string comparison over already-fetched [RetroAchievementsCandidateGame]s - no
 * network/DTO involvement, which is why this lives in `domain` rather than `data`, same
 * reasoning as [GameMediaPathResolver].
 *
 * Deliberately does not do fuzzy/edit-distance scoring (see CLAUDE.md's RetroAchievements
 * section) - only exact equality and a normalized-title fallback covering region tags,
 * subtitle punctuation, and "The"-prefix variants. A title that matches neither is
 * [TitleMatchResult.NoMatch], to be resolved via a manual override rather than a guess.
 */
object GameTitleMatcher {
    fun match(
        gameName: String,
        candidates: List<RetroAchievementsCandidateGame>,
    ): TitleMatchResult {
        val exactMatch = candidates.firstOrNull { it.title == gameName }
        if (exactMatch != null) return TitleMatchResult.Matched(exactMatch, TitleMatchMethod.ExactTitle)

        val normalizedGameName = normalize(gameName)
        val normalizedMatch = candidates.firstOrNull { normalize(it.title) == normalizedGameName }
        return normalizedMatch
            ?.let { TitleMatchResult.Matched(it, TitleMatchMethod.NormalizedTitle) }
            ?: TitleMatchResult.NoMatch
    }

    /**
     * Strips region/language tags in parentheses or brackets (e.g. "(USA)", "(Japan) (En,Ja)"),
     * then splits on any non-alphanumeric character (so ":", "-", "," and whitespace are all
     * treated as word boundaries) and drops any resulting "the" word - handling both a
     * leading "The " (as in "The Legend of Zelda") and RetroAchievements' own convention of
     * moving it after a comma (as in "Legend of Zelda, The") the same way, since both reduce
     * to the same word list once "the" is removed.
     */
    private fun normalize(title: String): String {
        val withoutRegionTags = REGION_TAG_REGEX.replace(title, " ")
        val alphanumericWithSpaces = withoutRegionTags.map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
        val words = alphanumericWithSpaces.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        return words.filterNot { it.equals("the", ignoreCase = true) }.joinToString(" ") { it.lowercase() }
    }

    private val REGION_TAG_REGEX = Regex("""[\[(][^])]*[])]""")
    private val WHITESPACE_REGEX = Regex("""\s+""")
}
