package com.esde.companion.data.media

import com.esde.companion.domain.repository.CustomSystemImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileCustomSystemImageRepository(
    private val folderPath: String,
) : CustomSystemImageRepository {
    override suspend fun findImage(systemShortName: String): String? =
        withContext(Dispatchers.IO) { findSystemAssetFile(folderPath, systemShortName) }
}
