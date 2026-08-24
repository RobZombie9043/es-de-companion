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
    fun `a missing space is matched as SpaceInsensitiveTitle`() {
        val matchingCandidate = candidate(1, "Motoracer Advance")
        val candidates = listOf(matchingCandidate)

        val result = GameTitleMatcher.match("Moto Racer Advance", candidates)

        assertEquals(TitleMatchResult.Matched(matchingCandidate, TitleMatchMethod.SpaceInsensitiveTitle), result)
    }

    @Test
    fun `an extra space is matched as SpaceInsensitiveTitle`() {
        val matchingCandidate = candidate(1, "Moto Racer Advance")
        val candidates = listOf(matchingCandidate)

        val result = GameTitleMatcher.match("Motoracer Advance", candidates)

        assertEquals(TitleMatchResult.Matched(matchingCandidate, TitleMatchMethod.SpaceInsensitiveTitle), result)
    }

    @Test
    fun `SpaceInsensitiveTitle does not fire when NormalizedTitle already matches`() {
        val matchingCandidate = candidate(1, "Chrono Trigger (USA)")
        val candidates = listOf(matchingCandidate)

        val result = GameTitleMatcher.match("Chrono Trigger", candidates)

        assertEquals(TitleMatchResult.Matched(matchingCandidate, TitleMatchMethod.NormalizedTitle), result)
    }

    @Test
    fun `a genuine spelling difference beyond just spacing is still NoMatch`() {
        // "Motorace" vs "Moto Racer" differ by more than whitespace alone (a missing "r"),
        // so this must stay NoMatch even with the SpaceInsensitiveTitle tier in place -
        // this is the case rankBySimilarity (not match()) is meant to catch instead.
        val candidates = listOf(candidate(1, "Moto Racer Advance"), candidate(2, "Chrono Trigger"))

        val result = GameTitleMatcher.match("Motorace Advance", candidates)

        assertEquals(TitleMatchResult.NoMatch, result)
    }

    @Test
    fun `a title with one inserted word in the middle is matched as PartialTitle`() {
        val matchingCandidate = candidate(1, "The Bugs Bunny Birthday Blowout")
        val candidates = listOf(matchingCandidate)

        val result = GameTitleMatcher.match("The Bugs Bunny Blowout", candidates)

        assertEquals(TitleMatchResult.Matched(matchingCandidate, TitleMatchMethod.PartialTitle), result)
    }

    @Test
    fun `the inserted word can be on either side of the comparison`() {
        val matchingCandidate = candidate(1, "Bugs Bunny Blowout")
        val candidates = listOf(matchingCandidate)

        val result = GameTitleMatcher.match("The Bugs Bunny Birthday Blowout", candidates)

        assertEquals(TitleMatchResult.Matched(matchingCandidate, TitleMatchMethod.PartialTitle), result)
    }

    @Test
    fun `more than two inserted words is NoMatch rather than a partial match`() {
        val candidates = listOf(candidate(1, "Bugs Bunny Has A Very Silly Birthday Blowout"))

        val result = GameTitleMatcher.match("Bugs Bunny Blowout", candidates)

        assertEquals(TitleMatchResult.NoMatch, result)
    }

    @Test
    fun `a short one or two word title never qualifies for a partial match`() {
        val candidates = listOf(candidate(1, "Pac-Man Championship Edition"))

        val result = GameTitleMatcher.match("Pac-Man", candidates)

        assertEquals(TitleMatchResult.NoMatch, result)
    }

    @Test
    fun `an exact title takes precedence over a partial match when both exist`() {
        val exactCandidate = candidate(1, "Bugs Bunny Blowout")
        val candidates = listOf(candidate(2, "Bugs Bunny Birthday Blowout"), exactCandidate)

        val result = GameTitleMatcher.match("Bugs Bunny Blowout", candidates)

        assertEquals(TitleMatchResult.Matched(exactCandidate, TitleMatchMethod.ExactTitle), result)
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
    fun `a genuinely different title is not auto-matched even when it is the closest candidate by spelling`() {
        val candidates = listOf(candidate(1, "Moto Racer Advance"), candidate(2, "Chrono Trigger"))

        val result = GameTitleMatcher.match("Motorace Advance", candidates)

        assertEquals(TitleMatchResult.NoMatch, result)
    }

    @Test
    fun `rankBySimilarity puts a title with only a missing space and a single extra letter first`() {
        val closeMatch = candidate(1, "Moto Racer Advance")
        val candidates = listOf(candidate(2, "Chrono Trigger"), closeMatch, candidate(3, "Xenogears"))

        val result = GameTitleMatcher.rankBySimilarity("Motorace Advance", candidates)

        assertEquals(closeMatch, result.first())
    }

    @Test
    fun `rankBySimilarity returns every candidate, just reordered`() {
        val candidates = listOf(candidate(1, "Chrono Cross"), candidate(2, "Chrono Trigger"))

        val result = GameTitleMatcher.rankBySimilarity("Xenogears", candidates)

        assertEquals(candidates.toSet(), result.toSet())
    }

    @Test
    fun `an exact title takes precedence over a normalized match when both exist`() {
        val candidates = listOf(candidate(1, "Chrono Trigger (USA)"), candidate(2, "Chrono Trigger"))

        val result = GameTitleMatcher.match("Chrono Trigger", candidates)

        assertEquals(TitleMatchResult.Matched(candidate(2, "Chrono Trigger"), TitleMatchMethod.ExactTitle), result)
    }
}
