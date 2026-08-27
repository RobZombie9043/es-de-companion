package com.esde.companion.domain.repository

import com.esde.companion.domain.model.GamelistSystemSummary
import com.esde.companion.domain.parser.GamelistGameEntry

/**
 * Whole-library enumeration over ES-DE's gamelist.xml files - every system that has one, and
 * every game within a given system - for features that need to browse the library rather than
 * look up one specific already-known game (contrast [GameDescriptionRepository]/
 * [GameRomHashRepository]/[GameRatingRepository], which all resolve a single game by its
 * already-known romPath). Currently used only by Game Launch Override's Settings pickers.
 *
 * Only supports the standard `<ES-DE root>/gamelists/<system>/gamelist.xml` location, not ES-DE's
 * legacy ROMs-adjacent location - see the implementation's kdoc for why a cold enumeration can't
 * resolve that location the way a single-game lookup (which always has a concrete romPath to
 * anchor on) can.
 */
interface GamelistLibraryRepository {
    suspend fun listSystems(): List<GamelistSystemSummary>

    suspend fun listGames(systemShortName: String): List<GamelistGameEntry>
}
