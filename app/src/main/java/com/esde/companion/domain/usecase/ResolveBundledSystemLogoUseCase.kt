package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.BundledSystemLogoRepository

class ResolveBundledSystemLogoUseCase(
    private val bundledSystemLogoRepository: BundledSystemLogoRepository,
) {
    suspend operator fun invoke(assetName: String): String? = bundledSystemLogoRepository.findLogoAssetPath(assetName)
}
