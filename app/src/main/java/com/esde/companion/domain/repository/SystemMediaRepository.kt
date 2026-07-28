package com.esde.companion.domain.repository

/**
 * Source of a representative image for a whole system, as opposed to [GameMediaRepository]
 * which resolves media for one specific game. Currently just "pick a random fanart image
 * from among this system's games," for the system-browsing backdrop. How that random pick
 * is made (scanning the media folder) is entirely a data-layer concern.
 */
interface SystemMediaRepository {
    suspend fun randomFanart(systemShortName: String): String?
}