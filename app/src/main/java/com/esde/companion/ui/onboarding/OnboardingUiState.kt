package com.esde.companion.ui.onboarding

import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.EsdeEventScriptSettings
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Permission,
    /** Steps actually visited so far, in order - popped by onBack(). Skipped steps
     * (MediaFolder when auto-detected and confirmed valid, LegacyScripts/
     * EventScriptSettings when nothing needed fixing) are never pushed, so back
     * navigation correctly jumps over them too. */
    val stepBackStack: List<OnboardingStep> = emptyList(),
    val permissionGranted: Boolean = false,
    val logFolderPath: String = "",
    val logFolderValidation: LogFolderValidation? = null,
    val isValidatingLogFolder: Boolean = false,
    val mediaFolderPath: String = "",
    val mediaFolderValidation: MediaFolderValidation? = null,
    val isValidatingMediaFolder: Boolean = false,
    /** Whether [mediaFolderPath] came from ES-DE's own settings/es_settings.xml rather
     * than the hardcoded default guess. */
    val mediaFolderAutoDetected: Boolean = false,
    /** True while the background read kicked off on EsdeFolder confirm (media directory,
     * event-script flags, legacy script files) is still in flight - gates EsdeFolder's Next
     * button, since the app remains on that step until this decides whether MediaFolder can
     * be auto-skipped (see OnboardingViewModel.fetchInstallationInfo). */
    val isCheckingInstallation: Boolean = false,
    val legacyScriptFiles: List<String> = emptyList(),
    val isDeletingLegacyScripts: Boolean = false,
    /** Null until the background read (kicked off on EsdeFolder confirm) completes - a
     * null value at that point means settings/es_settings.xml itself couldn't be read. */
    val eventScriptSettings: EsdeEventScriptSettings? = null,
    val connectionState: EsdeConnectionState? = null,
    /** True once a connection-state emission distinct from LiveLogCheck's entry baseline
     * has been observed - see OnboardingViewModel. Gates the Finish button. */
    val liveCheckPassed: Boolean = false,
    val isCompleting: Boolean = false,
)
