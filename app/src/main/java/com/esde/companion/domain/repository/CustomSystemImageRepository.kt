package com.esde.companion.domain.repository

/**
 * Source of a user-supplied "System Image" for a whole system - a literal
 * `<systemShortName>.<ext>` file dropped in the folder configured under Settings > Setup,
 * as opposed to [SystemMediaRepository]'s "pick a random photo from ES-DE's own media".
 */
interface CustomSystemImageRepository {
    suspend fun findImage(systemShortName: String): String?
}