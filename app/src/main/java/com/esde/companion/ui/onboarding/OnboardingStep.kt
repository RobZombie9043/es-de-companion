package com.esde.companion.ui.onboarding

/**
 * Forward order: Permission -> EsdeFolder -> MediaFolder -> [LegacyScripts] ->
 * [EventScriptSettings] -> LiveLogCheck. LegacyScripts/EventScriptSettings are shown only
 * when there's actually something to fix - see OnboardingViewModel's transition logic.
 */
sealed class OnboardingStep {
    data object Permission : OnboardingStep()
    data object EsdeFolder : OnboardingStep()
    data object MediaFolder : OnboardingStep()
    data object LegacyScripts : OnboardingStep()
    data object EventScriptSettings : OnboardingStep()
    data object LiveLogCheck : OnboardingStep()
}
