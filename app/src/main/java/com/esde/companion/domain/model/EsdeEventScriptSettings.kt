package com.esde.companion.domain.model

/**
 * The three ES-DE settings required for this app's log pipeline to work at all, read from
 * `settings/es_settings.xml` (see EsdeSettingsParser). ES-DE does not update this file
 * live when these are changed from its own menu, so there's no way to auto-reverify a fix
 * mid-onboarding - the user has to confirm it themselves after changing them in ES-DE.
 */
data class EsdeEventScriptSettings(
    val customEventScripts: Boolean,
    val customEventScriptsBrowsing: Boolean,
    val debugMode: Boolean,
) {
    val allEnabled: Boolean get() = customEventScripts && customEventScriptsBrowsing && debugMode
}
