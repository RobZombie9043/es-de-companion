package com.esde.companion.domain.parser

import com.esde.companion.domain.model.RetroAchievementsCandidateGame

/**
 * Matches ES-DE's display game name against a console's cached RetroAchievements game
 * list. Pure string comparison over already-fetched [RetroAchievementsCandidateGame]s - no
 * network/DTO involvement, which is why this lives in `domain` rather than `data`, same
 * reasoning as [GameMediaPathResolver].
 *
 * Deliberately does not do general fuzzy/edit-distance scoring (see CLAUDE.md's
 * RetroAchievements section) - exact equality, a normalized-title fallback covering region
 * tags/subtitle punctuation/"The"-prefix variants, a space-insensitive fallback covering a
 * missing/extra space between otherwise-identical words (e.g. "Moto Racer" vs. "Motoracer"),
 * and (see [isPartialMatch]) a narrow, bounded word-subsequence check for titles that are
 * otherwise identical but for a small number of inserted words (e.g. "The Bugs Bunny Blowout"
 * vs. the real RA title "The Bugs Bunny Birthday Blowout"). A title that matches none of these
 * is [TitleMatchResult.NoMatch], to be resolved via a manual override rather than a guess.
 */
object GameTitleMatcher {
    fun match(
        gameName: String,
        candidates: List<RetroAchievementsCandidateGame>,
    ): TitleMatchResult {
        val exactMatch = candidates.firstOrNull { it.title == gameName }
        val gameWords = normalize(gameName).split(" ").filter { it.isNotBlank() }
        val normalizedMatch = candidates.firstOrNull { normalizeToWords(it.title) == gameWords }
        val gameWordsJoined = gameWords.joinToString("")
        val spaceInsensitiveMatch = candidates.firstOrNull { joinedWordsOf(it.title) == gameWordsJoined }
        val partialMatch = candidates.firstOrNull { isPartialMatch(gameWords, normalizeToWords(it.title)) }
        return when {
            exactMatch != null -> TitleMatchResult.Matched(exactMatch, TitleMatchMethod.ExactTitle)
            normalizedMatch != null -> TitleMatchResult.Matched(normalizedMatch, TitleMatchMethod.NormalizedTitle)
            spaceInsensitiveMatch != null ->
                TitleMatchResult.Matched(spaceInsensitiveMatch, TitleMatchMethod.SpaceInsensitiveTitle)
            partialMatch != null -> TitleMatchResult.Matched(partialMatch, TitleMatchMethod.PartialTitle)
            else -> TitleMatchResult.NoMatch
        }
    }

    /**
     * True if [gameWords] and [candidateWords] (in either order) are the same word sequence
     * but for up to [MAX_INSERTED_WORDS] extra words inserted into the longer one - e.g.
     * "bugs bunny blowout" is a subsequence of "bugs bunny birthday blowout" with one word
     * inserted. Requires at least [MIN_WORDS_FOR_PARTIAL_MATCH] words on the shorter side so
     * single/two-word titles (which would trivially subsequence-match almost anything) never
     * qualify, and skips equal-length inputs entirely since those are already fully covered
     * by the normalized-equality check above.
     */
    private fun isPartialMatch(
        gameWords: List<String>,
        candidateWords: List<String>,
    ): Boolean {
        val shorter = if (gameWords.size <= candidateWords.size) gameWords else candidateWords
        val longer = if (gameWords.size <= candidateWords.size) candidateWords else gameWords
        val insertedWordCount = longer.size - shorter.size
        return shorter.size >= MIN_WORDS_FOR_PARTIAL_MATCH &&
            insertedWordCount in 1..MAX_INSERTED_WORDS &&
            isSubsequence(shorter, longer)
    }

    private fun isSubsequence(
        shorter: List<String>,
        longer: List<String>,
    ): Boolean {
        var shorterIndex = 0
        for (word in longer) {
            if (shorterIndex < shorter.size && word == shorter[shorterIndex]) shorterIndex++
        }
        return shorterIndex == shorter.size
    }

    private fun normalizeToWords(title: String) = normalize(title).split(" ").filter { it.isNotBlank() }

    private fun joinedWordsOf(title: String) = normalizeToWords(title).joinToString("")

    /**
     * Ranks [candidates] by trigram similarity to [gameName], most likely first - used only
     * to order the manual correction picker's initial (before the user types anything)
     * suggestions, never for automatic identification (see [match]). A wrong #1 suggestion
     * here just means the user picks a different entry from the list; it can never produce
     * a silently wrong achievement list the way loosening [match] itself would. Trigram
     * (character 3-gram) overlap tolerates exactly the kind of near-miss [match] deliberately
     * doesn't chase - a missing/extra space or a single added letter, e.g. ES-DE's "Motorace
     * Advance" vs. RA's actual "Moto Racer Advance" - without needing general edit-distance
     * scoring for automatic decisions.
     */
    fun rankBySimilarity(
        gameName: String,
        candidates: List<RetroAchievementsCandidateGame>,
    ): List<RetroAchievementsCandidateGame> {
        val gameTrigrams = trigramsOf(gameName)
        return candidates.sortedByDescending { trigramSimilarity(gameTrigrams, trigramsOf(it.title)) }
    }

    /**
     * Dice coefficient over two trigram sets: 2x their overlap over the combined size of
     * both, 0 if either is empty.
     */
    private fun trigramSimilarity(
        first: Set<String>,
        second: Set<String>,
    ): Double {
        if (first.isEmpty() || second.isEmpty()) return 0.0
        val sharedCount = first.intersect(second).size
        return (2.0 * sharedCount) / (first.size + second.size)
    }

    /** Every 3-character run of the normalized, space-collapsed title, so trigrams can span a word boundary too. */
    private fun trigramsOf(title: String): Set<String> {
        val letters = normalize(title).replace(" ", "")
        if (letters.length < TRIGRAM_LENGTH) return setOf(letters)
        return (0..letters.length - TRIGRAM_LENGTH).map { letters.substring(it, it + TRIGRAM_LENGTH) }.toSet()
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
    private const val MAX_INSERTED_WORDS = 2
    private const val MIN_WORDS_FOR_PARTIAL_MATCH = 3
    private const val TRIGRAM_LENGTH = 3
}
