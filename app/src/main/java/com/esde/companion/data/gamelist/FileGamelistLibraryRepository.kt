package com.esde.companion.data.gamelist

import com.esde.companion.domain.model.GamelistSystemSummary
import com.esde.companion.domain.parser.GamelistBulkParser
import com.esde.companion.domain.parser.GamelistGameEntry
import com.esde.companion.domain.repository.GamelistLibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Enumerates every system/game gamelist.xml exposes, for Game Launch Override's browse UI - see
 * [GamelistFileReader.listSystemShortNames]/[GamelistFileReader.readSystem] for the standard-
 * location-only limitation. [GamelistBulkParser]'s DOM parse is CPU-bound - same
 * `withContext(Dispatchers.Default)` reasoning as [FileGameDescriptionRepository].
 */
class FileGamelistLibraryRepository(
    private val reader: GamelistFileReader,
) : GamelistLibraryRepository {
    override suspend fun listSystems(): List<GamelistSystemSummary> =
        reader.listSystemShortNames().map { shortName ->
            GamelistSystemSummary(shortName = shortName, gameCount = listGames(shortName).size)
        }

    override suspend fun listGames(systemShortName: String): List<GamelistGameEntry> {
        val file = reader.readSystem(systemShortName) ?: return emptyList()
        return withContext(Dispatchers.Default) { GamelistBulkParser.parseAllGames(file.content) }
    }
}
