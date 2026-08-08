package com.esde.companion.domain.model

/**
 * The three ES-DE settings required for this app's log pipeline to work at all, plus one
 * advisory flag, read from `settings/es_settings.xml` (see EsdeSettingsParser). ES-DE only
 * rewrites this file once the user navigates back out of its own settings menu (not live
 * while it's still open), so onboarding's re-check waits for that file change rather than
 * polling continuously for no reason - see ObserveEsdeEventScriptSettingsUseCase /
 * OnboardingViewModel.
 */
data class EsdeEventScriptSettings(
    val customEventScripts: Boolean,
    val customEventScriptsBrowsing: Boolean,
    val debugMode: Boolean,
    /** Not one of the three required settings above - true here doesn't block onboarding,
     * it just means slide-animation direction can't be detected (see OnboardingScreen's
     * EventScriptSettingsStep). Not exposed in ES-DE's own settings menu, so the warning
     * tells the user to edit es_settings.xml by hand. */
    val debugSkipInputLogging: Boolean,
) {
    val allEnabled: Boolean get() = customEventScripts && customEventScriptsBrowsing && debugMode

    /** Whether the EventScriptSettings onboarding step has anything to show the user -
     * either a required setting still missing, or the advisory [debugSkipInputLogging]
     * warning - as opposed to nothing left to report. */
    val needsAttention: Boolean get() = !allEnabled || debugSkipInputLogging
}
