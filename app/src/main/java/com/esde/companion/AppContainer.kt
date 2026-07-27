package com.esde.companion

import android.content.Context
import com.esde.companion.data.log.ReactiveEsdeLogRepository
import com.esde.companion.data.settings.FileOnboardingRepository
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.CompleteOnboardingUseCase
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveOnboardingCompleteUseCase
import com.esde.companion.domain.usecase.ValidateEsdeLogFolderUseCase
import com.esde.companion.domain.usecase.ValidateEsdeMediaFolderUseCase

/**
 * Minimal hand-rolled composition root. At this project's current scale (single module,
 * one developer) this is a deliberate, simpler alternative to a DI framework - see
 * CLAUDE.md. If/when the dependency graph grows enough to justify it, this can be
 * replaced with Hilt without changing anything below the ViewModel layer, since
 * everything here is already expressed as interfaces / constructor dependencies.
 *
 * Now takes an application [Context] - required to build the DataStore-backed
 * [OnboardingRepository] and to resolve the user's chosen ES-DE folder reactively,
 * rather than the single hardcoded path this used to hold directly.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val onboardingRepository: OnboardingRepository = FileOnboardingRepository(appContext)

    // Reacts to the user changing the ES-DE folder later via Settings, without requiring
    // the process to restart - see ReactiveEsdeLogRepository.
    private val logRepository: EsdeLogRepository =
        ReactiveEsdeLogRepository(logFolderPath = onboardingRepository.observeLogFolderPath())

    val observeAppStateUseCase = ObserveAppStateUseCase(logRepository)
    val observeConnectionStateUseCase = ObserveConnectionStateUseCase(logRepository, observeAppStateUseCase)

    val validateEsdeLogFolderUseCase = ValidateEsdeLogFolderUseCase(onboardingRepository)
    val validateEsdeMediaFolderUseCase = ValidateEsdeMediaFolderUseCase(onboardingRepository)
    val completeOnboardingUseCase = CompleteOnboardingUseCase(onboardingRepository)
    val observeOnboardingCompleteUseCase = ObserveOnboardingCompleteUseCase(onboardingRepository)
}