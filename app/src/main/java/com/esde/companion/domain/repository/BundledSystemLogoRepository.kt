package com.esde.companion.domain.repository

/**
 * Whether the app's bundled `assets/system_logos/` folder actually contains a given logo
 * file - not every systemShortName ES-DE can report has a matching asset shipped with the
 * app, so this must be a real existence check rather than always constructing the path
 * string and hoping - a constructed-but-missing path would make WidgetContentResolver
 * think a logo was found (Coil then just fails to load it silently), rather than falling
 * through to its name-text fallback.
 */
interface BundledSystemLogoRepository {
    /** [assetName] is the already-mapped asset file stem (see
     * `com.esde.companion.ui.main.systemLogoAssetName`), not the raw systemShortName -
     * naming-convention mapping stays a ui concern; this only checks existence. */
    suspend fun findLogoAssetPath(assetName: String): String?
}
