package com.esde.companion.data.settings

import com.esde.companion.domain.model.EsdeEventScriptSettings
import com.esde.companion.domain.parser.EsdeSettingsParser
import com.esde.companion.domain.repository.EsdeInstallationRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads ES-DE's own settings/es_settings.xml and checks for the legacy
 * esdecompanion-*.sh script files the old app used to write into ES-DE's scripts/ folder
 * (see CLAUDE.md - that mechanism is gone, these files are pure leftover clutter now).
 */
class FileEsdeInstallationRepository : EsdeInstallationRepository {

    override suspend fun readMediaDirectory(esdeRootPath: String): String? =
        withContext(Dispatchers.IO) {
            readSettingsXml(esdeRootPath)?.let { xml ->
                EsdeSettingsParser.findStringValue(xml, "MediaDirectory")
            }
        }

    override suspend fun readEventScriptSettings(esdeRootPath: String): EsdeEventScriptSettings? =
        withContext(Dispatchers.IO) {
            readSettingsXml(esdeRootPath)?.let { xml ->
                EsdeEventScriptSettings(
                    customEventScripts = EsdeSettingsParser.findBoolValue(xml, "CustomEventScripts") ?: false,
                    customEventScriptsBrowsing = EsdeSettingsParser.findBoolValue(xml, "CustomEventScriptsBrowsing") ?: false,
                    debugMode = EsdeSettingsParser.findBoolValue(xml, "DebugMode") ?: false,
                )
            }
        }

    override suspend fun findLegacyScriptFiles(esdeRootPath: String): List<String> =
        withContext(Dispatchers.IO) {
            LEGACY_SCRIPT_RELATIVE_PATHS
                .map { File(esdeRootPath, it) }
                .filter { it.isFile }
                .map { it.absolutePath }
        }

    override suspend fun deleteLegacyScriptFiles(esdeRootPath: String) {
        withContext(Dispatchers.IO) {
            LEGACY_SCRIPT_RELATIVE_PATHS.forEach { File(esdeRootPath, it).delete() }
        }
    }

    private fun readSettingsXml(esdeRootPath: String): String? {
        val file = File(esdeRootPath, "settings/es_settings.xml")
        return if (file.isFile) file.readText() else null
    }

    private companion object {
        val LEGACY_SCRIPT_RELATIVE_PATHS = listOf(
            "scripts/game-end/esdecompanion-game-end.sh",
            "scripts/game-start/esdecompanion-game-start.sh",
            "scripts/game-select/esdecompanion-game-select.sh",
            "scripts/screensaver-end/esdecompanion-screensaver-end.sh",
            "scripts/screensaver-game-select/esdecompanion-screensaver-game-select.sh",
            "scripts/screensaver-start/esdecompanion-screensaver-start.sh",
            "scripts/system-select/esdecompanion-system-select.sh",
        )
    }
}
