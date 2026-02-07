package com.esde.companion.managers

import com.esde.companion.data.AppConstants
import java.io.File

/**
 * Centralized manager for ES-DE script generation and management
 * Provides a single source of truth for script creation, validation, and updates
 */
object ScriptManager {

    // Hardcoded paths - must match MainActivity.getLogsPath()
    val LOGS_PATH = AppConstants.Paths.DEFAULT_LOGS_PATH
    val DEFAULT_SCRIPTS_PATH = AppConstants.Paths.DEFAULT_SCRIPTS_PATH

    // Script directory names
    private val SCRIPT_DIRECTORIES = listOf(
        "game-select",
        "system-select",
        "game-start",
        "game-end",
        "screensaver-start",
        "screensaver-end",
        "screensaver-game-select"
    )

    // Old script filenames that should be deleted
    private val OLD_SCRIPT_NAMES = listOf(
        "companion_game_select.sh",
        "companion_system_select.sh"
    )

    /**
     * Result of script operations
     */
    data class ScriptOperationResult(
        val success: Boolean,
        val message: String,
        val failedToDelete: List<String> = emptyList()
    )

    /**
     * Prepare all script subdirectories
     */
    fun prepareScriptDirectories(scriptsDir: File) {
        SCRIPT_DIRECTORIES.forEach { dirName ->
            val dir = File(scriptsDir, dirName)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }

    /**
     * Delete old script files that are no longer needed
     * @return List of script names that failed to delete
     */
    fun deleteOldScriptFiles(scriptsDir: File): List<String> {
        val failedToDelete = mutableListOf<String>()

        OLD_SCRIPT_NAMES.forEach { scriptName ->
            // Check in game-select directory
            val gameSelectOld = File(File(scriptsDir, "game-select"), scriptName)
            if (gameSelectOld.exists()) {
                try {
                    if (!gameSelectOld.delete()) {
                        failedToDelete.add(scriptName)
                    }
                } catch (e: Exception) {
                    failedToDelete.add(scriptName)
                }
            }

            // Check in system-select directory
            val systemSelectOld = File(File(scriptsDir, "system-select"), scriptName)
            if (systemSelectOld.exists()) {
                try {
                    if (!systemSelectOld.delete()) {
                        failedToDelete.add(scriptName)
                    }
                } catch (e: Exception) {
                    failedToDelete.add(scriptName)
                }
            }
        }

        return failedToDelete
    }

    /**
     * Write all 7 script files with the latest template
     * Uses the improved POSIX-compatible syntax that handles embedded quotes
     */
    fun writeAllScriptFiles(scriptsDir: File) {
        // Build log file paths once (Kotlin evaluates these at runtime)
        val gameFilenameLog = AppConstants.Paths.GAME_FILENAME_LOG
        val gameNameLog = AppConstants.Paths.GAME_NAME_LOG
        val gameSystemLog = AppConstants.Paths.GAME_SYSTEM_LOG
        val systemNameLog = AppConstants.Paths.SYSTEM_NAME_LOG
        val gameStartFilenameLog = AppConstants.Paths.GAME_START_FILENAME_LOG
        val gameStartNameLog = AppConstants.Paths.GAME_START_NAME_LOG
        val gameStartSystemLog = AppConstants.Paths.GAME_START_SYSTEM_LOG
        val gameEndFilenameLog = AppConstants.Paths.GAME_END_FILENAME_LOG
        val gameEndNameLog = AppConstants.Paths.GAME_END_NAME_LOG
        val gameEndSystemLog = AppConstants.Paths.GAME_END_SYSTEM_LOG
        val screensaverStartLog = AppConstants.Paths.SCREENSAVER_START_LOG
        val screensaverEndLog = AppConstants.Paths.SCREENSAVER_END_LOG
        val screensaverGameFilenameLog = AppConstants.Paths.SCREENSAVER_GAME_FILENAME_LOG
        val screensaverGameNameLog = AppConstants.Paths.SCREENSAVER_GAME_NAME_LOG
        val screensaverGameSystemLog = AppConstants.Paths.SCREENSAVER_GAME_SYSTEM_LOG

        // 1. Game select script
        val gameSelectScript = File(File(scriptsDir, "game-select"), AppConstants.Scripts.GAME_SELECT_SCRIPT)
        gameSelectScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

# Always write filename (arg 1)
printf '%s' "${'$'}1" > "${'$'}LOG_DIR/$gameFilenameLog"

# Check if we have at least 4 arguments
if [ "${'$'}#" -ge 4 ]; then
    # Use shift to access arguments
    file="${'$'}1"
    shift
    
    # Collect all middle arguments into game name
    game_name="${'$'}1"
    shift
    
    # Keep adding until we have 2 args left (system short and system full)
    while [ "${'$'}#" -gt 2 ]; do
        game_name="${'$'}game_name ${'$'}1"
        shift
    done
    
    # Now ${'$'}1 is system short, ${'$'}2 is system full
    system_short="${'$'}1"
    
    printf '%s' "${'$'}game_name" > "${'$'}LOG_DIR/$gameNameLog"
    printf '%s' "${'$'}system_short" > "${'$'}LOG_DIR/$gameSystemLog"
else
    # Fallback for edge cases
    printf '%s' "${'$'}2" > "${'$'}LOG_DIR/$gameNameLog"
    printf '%s' "${'$'}3" > "${'$'}LOG_DIR/$gameSystemLog"
fi
""")
        gameSelectScript.setExecutable(true)

        // 2. System select script
        val systemSelectScript = File(File(scriptsDir, "system-select"), AppConstants.Scripts.SYSTEM_SELECT_SCRIPT)
        systemSelectScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

printf '%s' "${'$'}1" > "${'$'}LOG_DIR/$systemNameLog" &
""")
        systemSelectScript.setExecutable(true)

        // 3. Game start script
        val gameStartScript = File(File(scriptsDir, "game-start"), AppConstants.Scripts.GAME_START_SCRIPT)
        gameStartScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

# Always write filename (arg 1)
printf '%s' "${'$'}1" > "${'$'}LOG_DIR/$gameStartFilenameLog"

# Check if we have at least 4 arguments
if [ "${'$'}#" -ge 4 ]; then
    # Use shift to access arguments
    file="${'$'}1"
    shift
    
    # Collect all middle arguments into game name
    game_name="${'$'}1"
    shift
    
    # Keep adding until we have 2 args left (system short and system full)
    while [ "${'$'}#" -gt 2 ]; do
        game_name="${'$'}game_name ${'$'}1"
        shift
    done
    
    # Now ${'$'}1 is system short, ${'$'}2 is system full
    system_short="${'$'}1"
    
    printf '%s' "${'$'}game_name" > "${'$'}LOG_DIR/$gameStartNameLog"
    printf '%s' "${'$'}system_short" > "${'$'}LOG_DIR/$gameStartSystemLog"
else
    # Fallback for edge cases
    printf '%s' "${'$'}2" > "${'$'}LOG_DIR/$gameStartNameLog"
    printf '%s' "${'$'}3" > "${'$'}LOG_DIR/$gameStartSystemLog"
fi
""")
        gameStartScript.setExecutable(true)

        // 4. Game end script
        val gameEndScript = File(File(scriptsDir, "game-end"), AppConstants.Scripts.GAME_END_SCRIPT)
        gameEndScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

# Always write filename (arg 1)
printf '%s' "${'$'}1" > "${'$'}LOG_DIR/$gameEndFilenameLog"

# Check if we have at least 4 arguments
if [ "${'$'}#" -ge 4 ]; then
    # Use shift to access arguments
    file="${'$'}1"
    shift
    
    # Collect all middle arguments into game name
    game_name="${'$'}1"
    shift
    
    # Keep adding until we have 2 args left (system short and system full)
    while [ "${'$'}#" -gt 2 ]; do
        game_name="${'$'}game_name ${'$'}1"
        shift
    done
    
    # Now ${'$'}1 is system short, ${'$'}2 is system full
    system_short="${'$'}1"
    
    printf '%s' "${'$'}game_name" > "${'$'}LOG_DIR/$gameEndNameLog"
    printf '%s' "${'$'}system_short" > "${'$'}LOG_DIR/$gameEndSystemLog"
else
    # Fallback for edge cases
    printf '%s' "${'$'}2" > "${'$'}LOG_DIR/$gameEndNameLog"
    printf '%s' "${'$'}3" > "${'$'}LOG_DIR/$gameEndSystemLog"
fi
""")
        gameEndScript.setExecutable(true)

        // 5. Screensaver start script
        val screensaverStartScript = File(File(scriptsDir, "screensaver-start"), AppConstants.Scripts.SCREENSAVER_START_SCRIPT)
        screensaverStartScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

printf '%s' "${'$'}1" > "${'$'}LOG_DIR/$screensaverStartLog"
""")
        screensaverStartScript.setExecutable(true)

        // 6. Screensaver end script
        val screensaverEndScript = File(File(scriptsDir, "screensaver-end"), AppConstants.Scripts.SCREENSAVER_END_SCRIPT)
        screensaverEndScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

printf '%s' "${'$'}1" > "${'$'}LOG_DIR/$screensaverEndLog"
""")
        screensaverEndScript.setExecutable(true)

        // 7. Screensaver game select script
        val screensaverGameSelectScript = File(File(scriptsDir, "screensaver-game-select"), AppConstants.Scripts.SCREENSAVER_GAME_SELECT_SCRIPT)
        screensaverGameSelectScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

# Always write filename (arg 1)
printf '%s' "${'$'}1" > "${'$'}LOG_DIR/$screensaverGameFilenameLog"

# Check if we have at least 4 arguments
if [ "${'$'}#" -ge 4 ]; then
    # Use shift to access arguments
    file="${'$'}1"
    shift
    
    # Collect all middle arguments into game name
    game_name="${'$'}1"
    shift
    
    # Keep adding until we have 2 args left (system short and system full)
    while [ "${'$'}#" -gt 2 ]; do
        game_name="${'$'}game_name ${'$'}1"
        shift
    done
    
    # Now ${'$'}1 is system short, ${'$'}2 is system full
    system_short="${'$'}1"
    
    printf '%s' "${'$'}game_name" > "${'$'}LOG_DIR/$screensaverGameNameLog"
    printf '%s' "${'$'}system_short" > "${'$'}LOG_DIR/$screensaverGameSystemLog"
else
    # Fallback for edge cases
    printf '%s' "${'$'}2" > "${'$'}LOG_DIR/$screensaverGameNameLog"
    printf '%s' "${'$'}3" > "${'$'}LOG_DIR/$screensaverGameSystemLog"
fi
""")
        screensaverGameSelectScript.setExecutable(true)
    }

    /**
     * Check if all 7 scripts exist at the given path
     */
    fun findExistingScripts(scriptsDir: File): List<File> {
        val scriptFiles = listOf(
            File(scriptsDir, "game-select/${AppConstants.Scripts.GAME_SELECT_SCRIPT}"),
            File(scriptsDir, "system-select/${AppConstants.Scripts.SYSTEM_SELECT_SCRIPT}"),
            File(scriptsDir, "game-start/${AppConstants.Scripts.GAME_START_SCRIPT}"),
            File(scriptsDir, "game-end/${AppConstants.Scripts.GAME_END_SCRIPT}"),
            File(scriptsDir, "screensaver-start/${AppConstants.Scripts.SCREENSAVER_START_SCRIPT}"),
            File(scriptsDir, "screensaver-end/${AppConstants.Scripts.SCREENSAVER_END_SCRIPT}"),
            File(scriptsDir, "screensaver-game-select/${AppConstants.Scripts.SCREENSAVER_GAME_SELECT_SCRIPT}")
        )

        return scriptFiles.filter { it.exists() }
    }

    /**
     * Create all scripts in one operation
     * @return ScriptOperationResult with success status and message
     */
    fun createAllScripts(scriptsDir: File): ScriptOperationResult {
        return try {
            // Prepare directories
            prepareScriptDirectories(scriptsDir)

            // Delete old scripts
            val failedToDelete = deleteOldScriptFiles(scriptsDir)

            // Write all 7 script files
            writeAllScriptFiles(scriptsDir)

            // Generate success message
            val message = when {
                failedToDelete.isNotEmpty() ->
                    "All 7 scripts created successfully!\n\nWarning: Could not delete old scripts: ${failedToDelete.joinToString()}"
                else ->
                    "All 7 scripts created successfully!"
            }

            ScriptOperationResult(
                success = true,
                message = message,
                failedToDelete = failedToDelete
            )
        } catch (e: Exception) {
            ScriptOperationResult(
                success = false,
                message = "Error creating scripts: ${e.message}"
            )
        }
    }

    /**
     * Validation result for script checking
     */
    data class ScriptValidationResult(
        val allValid: Boolean,
        val validCount: Int,
        val outdatedCount: Int,
        val missingCount: Int,
        val invalidCount: Int,
        val outdatedScripts: List<String> = emptyList(),
        val missingScripts: List<String> = emptyList(),
        val invalidScripts: List<String> = emptyList()
    )

    /**
     * Check if all scripts exist and are up-to-date
     * This is a simple boolean check for quick validation
     *
     * @return true if all 7 scripts exist with correct format
     */
    fun areScriptsValid(scriptsDir: File): Boolean {
        val result = validateScripts(scriptsDir)
        return result.allValid
    }

    /**
     * Comprehensive script validation with detailed results
     *
     * Checks for:
     * - All 7 scripts exist
     * - Scripts use new format (#!/bin/sh, printf '%s')
     * - Scripts have argument reconstruction logic
     * - Scripts don't use old format (#!/bin/bash, echo -n)
     *
     * @return ScriptValidationResult with detailed information
     */
    fun validateScripts(scriptsDir: File): ScriptValidationResult {
        // Define required scripts with their expected content patterns
        val requiredScripts = mapOf(
            "game-select/${AppConstants.Scripts.GAME_SELECT_SCRIPT}" to ValidationPattern(
                required = listOf(
                    AppConstants.Scripts.EXPECTED_SHEBANG,
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    AppConstants.Paths.GAME_FILENAME_LOG,
                    AppConstants.Paths.GAME_NAME_LOG,
                    AppConstants.Paths.GAME_SYSTEM_LOG,
                    "if [ \"\$#\" -ge 4 ]"  // Argument reconstruction logic
                ),
                forbidden = listOf(
                    "echo -n",      // Old format
                    AppConstants.Scripts.OLD_SHEBANG   // Old shebang
                )
            ),
            "system-select/${AppConstants.Scripts.SYSTEM_SELECT_SCRIPT}" to ValidationPattern(
                required = listOf(
                    AppConstants.Scripts.EXPECTED_SHEBANG,
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    AppConstants.Paths.SYSTEM_NAME_LOG
                ),
                forbidden = listOf("echo -n", AppConstants.Scripts.OLD_SHEBANG)
            ),
            "game-start/${AppConstants.Scripts.GAME_START_SCRIPT}" to ValidationPattern(
                required = listOf(
                    AppConstants.Scripts.EXPECTED_SHEBANG,
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    AppConstants.Paths.GAME_START_FILENAME_LOG,
                    AppConstants.Paths.GAME_START_NAME_LOG,
                    AppConstants.Paths.GAME_START_SYSTEM_LOG,
                    "if [ \"\$#\" -ge 4 ]"
                ),
                forbidden = listOf("echo -n", AppConstants.Scripts.OLD_SHEBANG)
            ),
            "game-end/${AppConstants.Scripts.GAME_END_SCRIPT}" to ValidationPattern(
                required = listOf(
                    AppConstants.Scripts.EXPECTED_SHEBANG,
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    AppConstants.Paths.GAME_END_FILENAME_LOG,
                    AppConstants.Paths.GAME_END_NAME_LOG,
                    AppConstants.Paths.GAME_END_SYSTEM_LOG,
                    "if [ \"\$#\" -ge 4 ]"
                ),
                forbidden = listOf("echo -n", AppConstants.Scripts.OLD_SHEBANG)
            ),
            "screensaver-start/${AppConstants.Scripts.SCREENSAVER_START_SCRIPT}" to ValidationPattern(
                required = listOf(
                    AppConstants.Scripts.EXPECTED_SHEBANG,
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    AppConstants.Paths.SCREENSAVER_START_LOG
                ),
                forbidden = listOf("echo -n", AppConstants.Scripts.OLD_SHEBANG)
            ),
            "screensaver-end/${AppConstants.Scripts.SCREENSAVER_END_SCRIPT}" to ValidationPattern(
                required = listOf(
                    AppConstants.Scripts.EXPECTED_SHEBANG,
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    AppConstants.Paths.SCREENSAVER_END_LOG
                ),
                forbidden = listOf("echo -n", AppConstants.Scripts.OLD_SHEBANG)
            ),
            "screensaver-game-select/${AppConstants.Scripts.SCREENSAVER_GAME_SELECT_SCRIPT}" to ValidationPattern(
                required = listOf(
                    AppConstants.Scripts.EXPECTED_SHEBANG,
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    AppConstants.Paths.SCREENSAVER_GAME_FILENAME_LOG,
                    AppConstants.Paths.SCREENSAVER_GAME_NAME_LOG,
                    AppConstants.Paths.SCREENSAVER_GAME_SYSTEM_LOG,
                    "if [ \"\$#\" -ge 4 ]"
                ),
                forbidden = listOf("echo -n", AppConstants.Scripts.OLD_SHEBANG)
            )
        )

        var validCount = 0
        val outdatedScripts = mutableListOf<String>()
        val missingScripts = mutableListOf<String>()
        val invalidScripts = mutableListOf<String>()

        // Validate each script
        for ((scriptPath, pattern) in requiredScripts) {
            val scriptFile = File(scriptsDir, scriptPath)
            val scriptName = scriptPath.substringAfterLast("/")

            if (!scriptFile.exists()) {
                missingScripts.add(scriptName)
                continue
            }

            try {
                val content = scriptFile.readText()

                // Check for forbidden patterns (old format)
                val hasOldFormat = pattern.forbidden.any { content.contains(it) }
                if (hasOldFormat) {
                    outdatedScripts.add(scriptName)
                    continue
                }

                // Check for required patterns (new format)
                val hasNewFormat = pattern.required.all { content.contains(it) }
                if (!hasNewFormat) {
                    invalidScripts.add(scriptName)
                    continue
                }

                // Script is valid
                validCount++

            } catch (e: Exception) {
                android.util.Log.e("ScriptManager", "Error reading script: $scriptPath", e)
                invalidScripts.add(scriptName)
            }
        }

        return ScriptValidationResult(
            allValid = validCount == AppConstants.Scripts.TOTAL_SCRIPT_COUNT,
            validCount = validCount,
            outdatedCount = outdatedScripts.size,
            missingCount = missingScripts.size,
            invalidCount = invalidScripts.size,
            outdatedScripts = outdatedScripts,
            missingScripts = missingScripts,
            invalidScripts = invalidScripts
        )
    }

    /**
     * Internal data class for validation patterns
     */
    private data class ValidationPattern(
        val required: List<String>,
        val forbidden: List<String>
    )
}
