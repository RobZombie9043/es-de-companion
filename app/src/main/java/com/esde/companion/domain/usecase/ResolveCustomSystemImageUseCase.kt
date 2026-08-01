package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.CustomSystemImageRepository

class ResolveCustomSystemImageUseCase(
    private val repository: CustomSystemImageRepository,
) {
    suspend operator fun invoke(systemShortName: String): String? = repository.findImage(systemShortName)
}