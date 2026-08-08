package com.esde.companion.domain.repository

/**
 * Source of a user-supplied logo override for a whole system - a literal
 * `<systemShortName>.<ext>` file in the folder configured under Settings > Setup, used
 * in place of the bundled system_logos SVG asset when present.
 */
interface CustomSystemLogoRepository {
    suspend fun findLogo(systemShortName: String): String?
}
