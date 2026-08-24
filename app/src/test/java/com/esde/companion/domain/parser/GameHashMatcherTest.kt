package com.esde.companion.domain.parser

import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameHashMatcherTest {
    private fun candidate(
        gameId: Long,
        title: String,
        hashes: List<String> = emptyList(),
    ) = RetroAchievementsCandidateGame(gameId, title, iconUrl = null, hashes = hashes)

    @Test
    fun `matches a candidate whose hashes contain the exact hash`() {
        val matching = candidate(1, "Chrono Trigger", hashes = listOf("deadbeefdeadbeefdeadbeefdeadbeef"))
        val candidates = listOf(candidate(2, "Chrono Cross"), matching)

        val result = GameHashMatcher.match("deadbeefdeadbeefdeadbeefdeadbeef", candidates)

        assertEquals(matching, result)
    }

    @Test
    fun `matches case-insensitively when the input hash is uppercase`() {
        val matching = candidate(1, "Chrono Trigger", hashes = listOf("deadbeefdeadbeefdeadbeefdeadbeef"))

        val result = GameHashMatcher.match("DEADBEEFDEADBEEFDEADBEEFDEADBEEF", listOf(matching))

        assertEquals(matching, result)
    }

    @Test
    fun `matches case-insensitively when a candidate's stored hash is uppercase`() {
        val matching = candidate(1, "Chrono Trigger", hashes = listOf("DEADBEEFDEADBEEFDEADBEEFDEADBEEF"))

        val result = GameHashMatcher.match("deadbeefdeadbeefdeadbeefdeadbeef", listOf(matching))

        assertEquals(matching, result)
    }

    @Test
    fun `tolerates surrounding whitespace on the input hash`() {
        val matching = candidate(1, "Chrono Trigger", hashes = listOf("deadbeefdeadbeefdeadbeefdeadbeef"))

        val result = GameHashMatcher.match("  deadbeefdeadbeefdeadbeefdeadbeef  ", listOf(matching))

        assertEquals(matching, result)
    }

    @Test
    fun `matches on a non-first entry in a candidate's hashes`() {
        val hashes = listOf("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
        val matching = candidate(1, "Chrono Trigger", hashes = hashes)

        val result = GameHashMatcher.match("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", listOf(matching))

        assertEquals(matching, result)
    }

    @Test
    fun `no candidate contains the hash is null`() {
        val candidates = listOf(candidate(1, "Chrono Trigger", hashes = listOf("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")))

        val result = GameHashMatcher.match("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz", candidates)

        assertNull(result)
    }

    @Test
    fun `all candidates have empty hashes is null`() {
        val candidates = listOf(candidate(1, "Chrono Trigger"), candidate(2, "Chrono Cross"))

        val result = GameHashMatcher.match("deadbeefdeadbeefdeadbeefdeadbeef", candidates)

        assertNull(result)
    }

    @Test
    fun `a blank input hash is null`() {
        val candidates = listOf(candidate(1, "Chrono Trigger", hashes = listOf("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")))

        val result = GameHashMatcher.match("   ", candidates)

        assertNull(result)
    }

    @Test
    fun `an empty candidate list is null`() {
        val result = GameHashMatcher.match("deadbeefdeadbeefdeadbeefdeadbeef", emptyList())

        assertNull(result)
    }
}
