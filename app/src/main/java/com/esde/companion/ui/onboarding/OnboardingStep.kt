package com.esde.companion.ui.onboarding

/**
 * Forward order: Permission -> EsdeFolder -> [MediaFolder] -> [LegacyScripts] ->
 * [EventScriptSettings] -> LiveLogCheck. The three bracketed steps are shown only when
 * there's actually something to fix - see OnboardingViewModel's transition logic.
 * MediaFolder specifically is skipped when ES-DE's own settings/es_settings.xml already
 * names a media folder that exists on disk - a location confirmed by ES-DE itself needs no
 * re-confirmation from the user.
 */
sealed class OnboardingStep {
    data object Permission : OnboardingStep()

    data object EsdeFolder : OnboardingStep()

    data object MediaFolder : OnboardingStep()

    data object LegacyScripts : OnboardingStep()

    data object EventScriptSettings : OnboardingStep()

    data object LiveLogCheck : OnboardingStep()
}
