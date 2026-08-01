package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.CustomSystemLogoRepository

class ResolveCustomSystemLogoUseCase(
    private val repository: CustomSystemLogoRepository,
) {
    suspend operator fun invoke(systemShortName: String): String? = repository.findLogo(systemShortName)
}