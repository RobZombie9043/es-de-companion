package com.esde.companion.data.media

import com.esde.companion.domain.repository.CustomSystemLogoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileCustomSystemLogoRepository(
    private val folderPath: String,
) : CustomSystemLogoRepository {
    override suspend fun findLogo(systemShortName: String): String? =
        withContext(Dispatchers.IO) { findSystemAssetFile(folderPath, systemShortName) }
}