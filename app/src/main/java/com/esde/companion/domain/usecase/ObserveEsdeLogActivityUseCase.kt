package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.repository.EsdeLogRepository
import kotlinx.coroutines.flow.Flow

/**
 * The raw, undeduplicated event stream from [EsdeLogRepository] - used where proof that
 * *some* Scripting::fireEvent() line was freshly parsed matters more than what it actually
 * was (see OnboardingViewModel's LiveLogCheck step).
 *
 * [ObserveAppStateUseCase]/[ObserveConnectionStateUseCase] fold this into a derived
 * AppState/EsdeConnectionState backed by a [kotlinx.coroutines.flow.StateFlow], which only
 * emits to new collectors when its value actually *changes* - repeating the exact same
 * ES-DE navigation the user already did once (e.g. re-selecting the same system after
 * going back and re-entering the live-activity check) reduces to the same AppState value,
 * so that derived stream produces no new emission at all even though a real line was
 * appended to es_log.txt. This exposes the shared event stream directly instead, which has
 * no such conflation, so any repeated action still counts as proof of life.
 */
class ObserveEsdeLogActivityUseCase(
    private val logRepository: EsdeLogRepository,
) {
    operator fun invoke(): Flow<EsdeEvent> = logRepository.observeEvents()
}
