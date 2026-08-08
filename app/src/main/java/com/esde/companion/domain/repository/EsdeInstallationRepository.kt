package com.esde.companion.domain.repository

import com.esde.companion.domain.model.EsdeEventScriptSettings
import kotlinx.coroutines.flow.Flow

/**
 * Reads ES-DE's own on-disk configuration and legacy-script footprint, given its root
 * folder path - a distinct concern from [OnboardingRepository] (persisted app settings),
 * used during onboarding to auto-detect the media folder and verify ES-DE itself is
 * configured correctly for this app's log pipeline.
 */
interface EsdeInstallationRepository {
    /** The MediaDirectory value from settings/es_settings.xml, or null if the file can't
     * be found/read, or the setting isn't present in it. */
    suspend fun readMediaDirectory(esdeRootPath: String): String?

    /** The three settings this app's log pipeline depends on, or null if
     * settings/es_settings.xml itself can't be found/read - distinct from "found, but a
     * flag is false/absent". */
    suspend fun readEventScriptSettings(esdeRootPath: String): EsdeEventScriptSettings?

    /** Emits [readEventScriptSettings]'s result once immediately, then again every time
     * settings/es_settings.xml changes on disk - ES-DE rewrites this file once the user
     * navigates back out of its settings menu (not live while the menu is still open), so
     * this is what lets onboarding auto-detect a fix instead of requiring a manual
     * re-check. */
    fun observeEventScriptSettings(esdeRootPath: String): Flow<EsdeEventScriptSettings?>

    /** Absolute paths of whichever legacy esdecompanion-*.sh script files (written by the
     * old app, no longer used - see CLAUDE.md) are currently present. Empty if none. */
    suspend fun findLegacyScriptFiles(esdeRootPath: String): List<String>

    /** Deletes whichever legacy script files are currently present; silently no-ops on
     * any that are already absent. */
    suspend fun deleteLegacyScriptFiles(esdeRootPath: String)
}
