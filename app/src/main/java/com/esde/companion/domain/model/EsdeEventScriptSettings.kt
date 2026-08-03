package com.esde.companion.domain.model

/**
 * The three ES-DE settings required for this app's log pipeline to work at all, read from
 * `settings/es_settings.xml` (see EsdeSettingsParser). ES-DE only rewrites this file once
 * the user navigates back out of its own settings menu (not live while it's still open),
 * so onboarding's re-check waits for that file change rather than polling continuously for
 * no reason - see ObserveEsdeEventScriptSettingsUseCase / OnboardingViewModel.
 */
data class EsdeEventScriptSettings(
    val customEventScripts: Boolean,
    val customEventScriptsBrowsing: Boolean,
    val debugMode: Boolean,
) {
    val allEnabled: Boolean get() = customEventScripts && customEventScriptsBrowsing && debugMode
}
