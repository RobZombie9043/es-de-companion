package com.esde.companion.data.media

import android.content.Context
import com.esde.companion.domain.repository.BundledSystemLogoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Checks the app's own bundled `assets/system_logos/` folder for a `<assetName>.svg` file
 * before handing back its path - see [BundledSystemLogoRepository]'s kdoc for why this
 * can't just construct the path unconditionally. The listing is read once and cached -
 * bundled app assets can't change at runtime.
 */
class AssetBundledSystemLogoRepository(private val context: Context) : BundledSystemLogoRepository {
    private val availableFileNames: Set<String> by lazy {
        (context.assets.list(SYSTEM_LOGOS_DIR) ?: emptyArray()).toSet()
    }

    override suspend fun findLogoAssetPath(assetName: String): String? =
        withContext(Dispatchers.IO) {
            val fileName = "$assetName.svg"
            if (fileName in availableFileNames) "file:///android_asset/$SYSTEM_LOGOS_DIR/$fileName" else null
        }

    private companion object {
        const val SYSTEM_LOGOS_DIR = "system_logos"
    }
}
