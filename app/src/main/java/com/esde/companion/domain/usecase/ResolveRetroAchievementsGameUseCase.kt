package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.EsdeSystemToRaConsoleMapping
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.MatchMethod
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsGameMatch
import com.esde.companion.domain.parser.GameHashMatcher
import com.esde.companion.domain.parser.GameTitleMatcher
import com.esde.companion.domain.parser.TitleMatchMethod
import com.esde.companion.domain.parser.TitleMatchResult
import com.esde.companion.domain.repository.GameMatchOverrideRepository
import com.esde.companion.domain.repository.GameRomHashRepository
import com.esde.companion.domain.repository.RetroAchievementsRepository

/**
 * Identification only - no achievement fetch, see [GetGameAchievementSummaryUseCase] for
 * that, deliberately kept separate so a network failure fetching achievements never gets
 * confused with "wrong/no game identified".
 *
 * Priority order, each tier only tried if the one above didn't apply:
 * 1. A stored manual override always wins, unconditionally - the candidate list isn't even
 *    fetched in this case (see the early return below), since it would only be discarded.
 * 2. A ROM hash parsed from gamelist.xml (see [gameRomHashRepository] - ahead of ES-DE's own
 *    upcoming hash support, so [com.esde.companion.domain.model.GameRomHash.value] is always
 *    null for real users today) is tried next, since it identifies the exact ROM dump rather
 *    than guessing from a name - it outranks every title tier below, including
 *    [TitleMatchMethod.ExactTitle], which is still just string equality. A hash that's
 *    present but matches no candidate falls through to title matching rather than
 *    [RetroAchievementsGameMatch.NoMatch] - RA's own stored hashes are frequently a
 *    transformed form (header-stripped, etc.), so a legitimate game can genuinely have no
 *    matching entry. This keeps the whole feature strictly additive: for any game, it can
 *    only turn a weaker/no match into a better one, never regress a match that already
 *    works today.
 * 3. [gameName] (ES-DE's scraped display title), matched via [GameTitleMatcher].
 * 4. The ROM's own filename (extension stripped) - a scraped title can be translated/
 *    altered/abbreviated from the original release name in ways the filename itself often
 *    isn't, so the two are independent signals worth trying in sequence.
 *
 * [gameName] is passed alongside [gameReference] rather than folded into it, since
 * [GameReference] only carries the identity ES-DE's own media lookup needs (system + rom
 * path) and has no display-title field for [GameTitleMatcher] to compare against.
 *
 * [onResolved] is an optional diagnostic callback, not a direct dependency on
 * [com.esde.companion.data.debug.DebugFileLogger] - this class stays plain Kotlin (see
 * CLAUDE.md's layering rules) by having the composition root (AppContainer) pass the
 * logger's method in as a function reference, the same pattern
 * [com.esde.companion.domain.music.MusicPlaybackCoordinator]'s onTrackStarted/onPlaybackError
 * callbacks already use. Invoked once per [invoke] call with a single line describing the
 * outcome, which signal produced it (manual override, ROM hash, scraped game name, or ROM
 * filename), and what it matched - so a wrong/unexpected identification can be diagnosed
 * from the debug log alone (tag "Cheevo").
 */
class ResolveRetroAchievementsGameUseCase(
    private val gameMatchOverrideRepository: GameMatchOverrideRepository,
    private val retroAchievementsRepository: RetroAchievementsRepository,
    private val gameRomHashRepository: GameRomHashRepository,
    private val onResolved: (String) -> Unit = {},
) {
    suspend operator fun invoke(
        gameReference: GameReference,
        gameName: String,
    ): RetroAchievementsGameMatch {
        val console =
            EsdeSystemToRaConsoleMapping.consoleFor(gameReference.systemShortName)
                ?: return report(gameReference, "", RetroAchievementsGameMatch.UnsupportedSystem)

        val override = gameMatchOverrideRepository.getOverride(gameReference)
        if (override != null) {
            val result = RetroAchievementsGameMatch.Found(override.raGameId, MatchMethod.ManualOverride)
            return report(gameReference, "via=ManualOverride gameId=${override.raGameId}", result)
        }

        val candidates = retroAchievementsRepository.getCandidateGames(console)

        val romHash = gameRomHashRepository.resolveRomHash(gameReference.systemShortName, gameReference.romPath).value
        val hashMatch = romHash?.let { GameHashMatcher.match(it, candidates) }
        if (hashMatch != null) {
            val result = RetroAchievementsGameMatch.Found(hashMatch.gameId, MatchMethod.RomHash)
            val details = "via=RomHash hash=$romHash gameId=${hashMatch.gameId} title=\"${hashMatch.title}\""
            return report(gameReference, details, result)
        }

        val gameNameMatch = matchAgainst(gameName, candidates)
        val fileNameGuess = gameReference.romFileNameGuess()
        val matched = gameNameMatch ?: matchAgainst(fileNameGuess, candidates)

        if (matched != null) {
            val signal =
                if (gameNameMatch != null) "gameName query=\"$gameName\"" else "filename query=\"$fileNameGuess\""
            val method = matched.method.toMatchMethod()
            val candidate = matched.candidate
            val details = "via=$method signal=$signal gameId=${candidate.gameId} title=\"${candidate.title}\""
            return report(gameReference, details, RetroAchievementsGameMatch.Found(candidate.gameId, method))
        }

        val details =
            "no signal matched (hash=${romHash ?: "none"}, gameName=\"$gameName\", filename=\"$fileNameGuess\")"
        return report(gameReference, details, RetroAchievementsGameMatch.NoMatch)
    }

    private fun report(
        gameReference: GameReference,
        details: String,
        result: RetroAchievementsGameMatch,
    ): RetroAchievementsGameMatch {
        val outcome =
            when (result) {
                is RetroAchievementsGameMatch.Found -> "MATCHED"
                RetroAchievementsGameMatch.NoMatch -> "NO MATCH"
                RetroAchievementsGameMatch.UnsupportedSystem -> "UNSUPPORTED"
            }
        val suffix = if (details.isBlank()) "" else " $details"
        onResolved("$outcome ${gameReference.systemShortName} ${gameReference.romPath}$suffix")
        return result
    }

    private fun matchAgainst(
        name: String,
        candidates: List<RetroAchievementsCandidateGame>,
    ): TitleMatchResult.Matched? = GameTitleMatcher.match(name, candidates) as? TitleMatchResult.Matched

    /** The ROM's filename with its extension stripped - see this class's kdoc for why it's a secondary match signal. */
    private fun GameReference.romFileNameGuess(): String {
        val fileName = romPath.substringAfterLast('/')
        return fileName.substringBeforeLast('.', fileName)
    }

    private fun TitleMatchMethod.toMatchMethod(): MatchMethod =
        when (this) {
            TitleMatchMethod.ExactTitle -> MatchMethod.ExactTitle
            TitleMatchMethod.NormalizedTitle -> MatchMethod.NormalizedTitle
            TitleMatchMethod.SpaceInsensitiveTitle -> MatchMethod.SpaceInsensitiveTitle
            TitleMatchMethod.PartialTitle -> MatchMethod.PartialTitle
        }
}
