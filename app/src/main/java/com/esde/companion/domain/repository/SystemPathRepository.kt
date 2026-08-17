package com.esde.companion.domain.repository

/**
 * Source of a system's ROM directory as ES-DE itself has it configured - the same
 * information `system-select`'s fireEvent() reports live, but readable up front from
 * ES-DE's own `custom_systems/es_systems.xml`, independent of any event having fired
 * this session. See GameMediaPathResolver's kdoc for why this matters: a system's ROM
 * folder is not guaranteed to be named after its shortname.
 *
 * A `null` result is a routine, expected outcome - the system may not be customized
 * (`custom_systems/es_systems.xml` only lists customized systems, or may not exist at
 * all), not an error - callers fall back to other strategies.
 */
interface SystemPathRepository {
    suspend fun resolveSystemPath(systemShortName: String): String?
}
