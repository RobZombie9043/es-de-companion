package com.esde.companion.domain.parser

/**
 * Locates gamelist.xml's legacy location: directly under the ROMs folder, alongside the
 * actual game files, at "<romsRoot>/<systemShortName>/gamelist.xml" - where ES-DE puts it
 * when its own "LegacyGamelistFileLocation" setting is enabled, instead of the standard
 * "<ES-DE root>/gamelists/<systemShortName>/gamelist.xml" (see FileGameDescriptionRepository).
 *
 * There's no separately configured ROMs root to anchor on - onboarding only asks for the
 * ES-DE root, not a ROMs path - so this derives it from the currently-browsed game's own
 * absolute path instead, the same "/<systemShortName>/" marker-anchoring
 * GameMediaPathResolver already uses for the same reason.
 *
 * Returns null if [romPath] doesn't contain a "/<systemShortName>/" segment to anchor on.
 */
object LegacyGamelistPathResolver {

    fun resolvePath(systemShortName: String, romPath: String): String? {
        val systemMarker = "/$systemShortName/"
        val markerIndex = romPath.lastIndexOf(systemMarker)
        if (markerIndex == -1) return null

        return romPath.substring(0, markerIndex + systemMarker.length) + "gamelist.xml"
    }
}
