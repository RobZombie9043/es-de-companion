package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.SystemMediaRepository

/**
 * The entry point the UI layer should depend on for "give me a representative backdrop
 * image while browsing this system" - a random fanart image belonging to some game on
 * that system.
 */
class ResolveRandomSystemFanartUseCase(
    private val systemMediaRepository: SystemMediaRepository,
) {
    suspend operator fun invoke(systemShortName: String): String? =
        systemMediaRepository.randomFanart(systemShortName)
}