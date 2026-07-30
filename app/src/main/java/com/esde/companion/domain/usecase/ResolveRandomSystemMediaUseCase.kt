package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.repository.SystemMediaRepository

/**
 * The entry point the UI layer should depend on for "give me a representative image of
 * [mediaType] while browsing this system" - e.g. fanart for the system-browsing backdrop,
 * or any other MediaType a widget asks for.
 */
class ResolveRandomSystemMediaUseCase(
    private val systemMediaRepository: SystemMediaRepository,
) {
    suspend operator fun invoke(systemShortName: String, mediaType: MediaType): String? =
        systemMediaRepository.randomMedia(systemShortName, mediaType)
}