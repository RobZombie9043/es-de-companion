package com.esde.companion.domain.parser

import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import org.junit.Assert.assertEquals
import org.junit.Test

class GameTitleMatcherTest {
    private fun candidate(
        gameId: Long,
        title: String,
    ) = RetroAchievementsCandidateGame(gameId, title, iconUrl = null)

    @Test
    fun `an exact title match is reported as ExactTitle`() {
        val candidates = listOf(candidate(1, "Chrono Trigger"), candidate(2, "Chrono Cross"))

        val result = GameTitleMatcher.match("Chrono Trigger", candidates)

        assertEquals(TitleMatchResult.Matched(candidate(1, "Chrono Trigger"), TitleMatchMethod.ExactTitle), result)
    }

    @Test
    fun `a region tag difference is only matched after normalization`() {
        val matchingCandidate = candidate(1, "Chrono Trigger (USA)")
        val candidates = listOf(matchingCandidate)

        val result = GameTitleMatcher.match("Chrono Trigger", candidates)

        assertEquals(TitleMatchResult.Matched(matchingCandidate, TitleMatchMethod.NormalizedTitle), result)
    }

    @Test
    fun `a multi-tag region and language difference is only matched after normalization`() {
        val candidates = listOf(candidate(1, "Chrono Trigger (Japan) (En,Ja)"))

        val result = GameTitleMatcher.match("Chrono Trigger", candidates)

        assertEquals(
            TitleMatchResult.Matched(candidate(1, "Chrono Trigger (Japan) (En,Ja)"), TitleMatchMethod.NormalizedTitle),
            result,
        )
    }

    @Test
    fun `subtitle punctuation differences are only matched after normalization`() {
        val matchingCandidate = candidate(1, "Final Fantasy VII - Advent Children")
        val candidates = listOf(matchingCandidate)

        val result = GameTitleMatcher.match("Final Fantasy VII: Advent Children", candidates)

        assertEquals(TitleMatchResult.Matched(matchingCandidate, TitleMatchMethod.NormalizedTitle), result)
    }

    @Test
    fun `a leading The versus RetroAchievements' trailing comma-The convention is only matched after normalization`() {
        val matchingCandidate = candidate(1, "Legend of Zelda, The: A Link to the Past")
        val candidates = listOf(matchingCandidate)

        val result = GameTitleMatcher.match("The Legend of Zelda: A Link to the Past", candidates)

        assertEquals(TitleMatchResult.Matched(matchingCandidate, TitleMatchMethod.NormalizedTitle), result)
    }

    @Test
    fun `a genuinely different title is NoMatch`() {
        val candidates = listOf(candidate(1, "Chrono Cross"), candidate(2, "Chrono Trigger"))

        val result = GameTitleMatcher.match("Xenogears", candidates)

        assertEquals(TitleMatchResult.NoMatch, result)
    }

    @Test
    fun `an empty candidate list is NoMatch`() {
        val result = GameTitleMatcher.match("Chrono Trigger", emptyList())

        assertEquals(TitleMatchResult.NoMatch, result)
    }

    @Test
    fun `an exact title takes precedence over a normalized match when both exist`() {
        val candidates = listOf(candidate(1, "Chrono Trigger (USA)"), candidate(2, "Chrono Trigger"))

        val result = GameTitleMatcher.match("Chrono Trigger", candidates)

        assertEquals(TitleMatchResult.Matched(candidate(2, "Chrono Trigger"), TitleMatchMethod.ExactTitle), result)
    }
}
