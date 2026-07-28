package com.esde.companion.ui.onboarding

sealed class OnboardingStep {
    data object Permission : OnboardingStep()
    data object LogFolder : OnboardingStep()
    data object MediaFolder : OnboardingStep()
}