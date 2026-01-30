package com.esde.companion.managers

import java.io.File

/**
 * Centralized manager for ES-DE script generation and management
 * Provides a single source of truth for script creation, validation, and updates
 */
object ScriptManager {

    // Hardcoded paths - must match MainActivity.getLogsPath()
    const val LOGS_PATH = "/storage/emulated/0/ES-DE Companion/logs"
    const val DEFAULT_SCRIPTS_PATH = "/storage/emulated/0/ES-DE/scripts"

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
        // 1. Game select script
        val gameSelectScript = File(File(scriptsDir, "game-select"), "esdecompanion-game-select.sh")
        gameSelectScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

# Always write filename (arg 1)
printf '%s' "${'$'}1" > "${'$'}LOG_DIR/esde_game_filename.txt"

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
    
    printf '%s' "${'$'}game_name" > "${'$'}LOG_DIR/esde_game_name.txt"
    printf '%s' "${'$'}system_short" > "${'$'}LOG_DIR/esde_game_system.txt"
else
    # Fallback for edge cases
    printf '%s' "${'$'}2" > "${'$'}LOG_DIR/esde_game_name.txt"
    printf '%s' "${'$'}3" > "${'$'}LOG_DIR/esde_game_system.txt"
fi
""")
        gameSelectScript.setExecutable(true)

        // 2. System select script
        val systemSelectScript = File(File(scriptsDir, "system-select"), "esdecompanion-system-select.sh")
        systemSelectScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

printf '%s' "${'$'}1" > "${'$'}LOG_DIR/esde_system_name.txt" &
""")
        systemSelectScript.setExecutable(true)

        // 3. Game start script
        val gameStartScript = File(File(scriptsDir, "game-start"), "esdecompanion-game-start.sh")
        gameStartScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

# Always write filename (arg 1)
printf '%s' "${'$'}1" > "${'$'}LOG_DIR/esde_gamestart_filename.txt"

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
    
    printf '%s' "${'$'}game_name" > "${'$'}LOG_DIR/esde_gamestart_name.txt"
    printf '%s' "${'$'}system_short" > "${'$'}LOG_DIR/esde_gamestart_system.txt"
else
    # Fallback for edge cases
    printf '%s' "${'$'}2" > "${'$'}LOG_DIR/esde_gamestart_name.txt"
    printf '%s' "${'$'}3" > "${'$'}LOG_DIR/esde_gamestart_system.txt"
fi
""")
        gameStartScript.setExecutable(true)

        // 4. Game end script
        val gameEndScript = File(File(scriptsDir, "game-end"), "esdecompanion-game-end.sh")
        gameEndScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

# Always write filename (arg 1)
printf '%s' "${'$'}1" > "${'$'}LOG_DIR/esde_gameend_filename.txt"

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
    
    printf '%s' "${'$'}game_name" > "${'$'}LOG_DIR/esde_gameend_name.txt"
    printf '%s' "${'$'}system_short" > "${'$'}LOG_DIR/esde_gameend_system.txt"
else
    # Fallback for edge cases
    printf '%s' "${'$'}2" > "${'$'}LOG_DIR/esde_gameend_name.txt"
    printf '%s' "${'$'}3" > "${'$'}LOG_DIR/esde_gameend_system.txt"
fi
""")
        gameEndScript.setExecutable(true)

        // 5. Screensaver start script
        val screensaverStartScript = File(File(scriptsDir, "screensaver-start"), "esdecompanion-screensaver-start.sh")
        screensaverStartScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

printf '%s' "${'$'}1" > "${'$'}LOG_DIR/esde_screensaver_start.txt"
""")
        screensaverStartScript.setExecutable(true)

        // 6. Screensaver end script
        val screensaverEndScript = File(File(scriptsDir, "screensaver-end"), "esdecompanion-screensaver-end.sh")
        screensaverEndScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

printf '%s' "${'$'}1" > "${'$'}LOG_DIR/esde_screensaver_end.txt"
""")
        screensaverEndScript.setExecutable(true)

        // 7. Screensaver game select script
        val screensaverGameSelectScript = File(File(scriptsDir, "screensaver-game-select"), "esdecompanion-screensaver-game-select.sh")
        screensaverGameSelectScript.writeText("""#!/bin/sh

LOG_DIR="$LOGS_PATH"
mkdir -p "${'$'}LOG_DIR"

# Always write filename (arg 1)
printf '%s' "${'$'}1" > "${'$'}LOG_DIR/esde_screensavergameselect_filename.txt"

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
    
    printf '%s' "${'$'}game_name" > "${'$'}LOG_DIR/esde_screensavergameselect_name.txt"
    printf '%s' "${'$'}system_short" > "${'$'}LOG_DIR/esde_screensavergameselect_system.txt"
else
    # Fallback for edge cases
    printf '%s' "${'$'}2" > "${'$'}LOG_DIR/esde_screensavergameselect_name.txt"
    printf '%s' "${'$'}3" > "${'$'}LOG_DIR/esde_screensavergameselect_system.txt"
fi
""")
        screensaverGameSelectScript.setExecutable(true)
    }

    /**
     * Check if all 7 scripts exist at the given path
     */
    fun findExistingScripts(scriptsDir: File): List<File> {
        val scriptFiles = listOf(
            File(scriptsDir, "game-select/esdecompanion-game-select.sh"),
            File(scriptsDir, "system-select/esdecompanion-system-select.sh"),
            File(scriptsDir, "game-start/esdecompanion-game-start.sh"),
            File(scriptsDir, "game-end/esdecompanion-game-end.sh"),
            File(scriptsDir, "screensaver-start/esdecompanion-screensaver-start.sh"),
            File(scriptsDir, "screensaver-end/esdecompanion-screensaver-end.sh"),
            File(scriptsDir, "screensaver-game-select/esdecompanion-screensaver-game-select.sh")
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
            "game-select/esdecompanion-game-select.sh" to ValidationPattern(
                required = listOf(
                    "#!/bin/sh",
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    "esde_game_filename.txt",
                    "esde_game_name.txt",
                    "esde_game_system.txt",
                    "if [ \"\$#\" -ge 4 ]"  // Argument reconstruction logic
                ),
                forbidden = listOf(
                    "echo -n",      // Old format
                    "#!/bin/bash"   // Old shebang
                )
            ),
            "system-select/esdecompanion-system-select.sh" to ValidationPattern(
                required = listOf(
                    "#!/bin/sh",
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    "esde_system_name.txt"
                ),
                forbidden = listOf("echo -n", "#!/bin/bash")
            ),
            "game-start/esdecompanion-game-start.sh" to ValidationPattern(
                required = listOf(
                    "#!/bin/sh",
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    "esde_gamestart_filename.txt",
                    "esde_gamestart_name.txt",
                    "esde_gamestart_system.txt",
                    "if [ \"\$#\" -ge 4 ]"
                ),
                forbidden = listOf("echo -n", "#!/bin/bash")
            ),
            "game-end/esdecompanion-game-end.sh" to ValidationPattern(
                required = listOf(
                    "#!/bin/sh",
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    "esde_gameend_filename.txt",
                    "esde_gameend_name.txt",
                    "esde_gameend_system.txt",
                    "if [ \"\$#\" -ge 4 ]"
                ),
                forbidden = listOf("echo -n", "#!/bin/bash")
            ),
            "screensaver-start/esdecompanion-screensaver-start.sh" to ValidationPattern(
                required = listOf(
                    "#!/bin/sh",
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    "esde_screensaver_start.txt"
                ),
                forbidden = listOf("echo -n", "#!/bin/bash")
            ),
            "screensaver-end/esdecompanion-screensaver-end.sh" to ValidationPattern(
                required = listOf(
                    "#!/bin/sh",
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    "esde_screensaver_end.txt"
                ),
                forbidden = listOf("echo -n", "#!/bin/bash")
            ),
            "screensaver-game-select/esdecompanion-screensaver-game-select.sh" to ValidationPattern(
                required = listOf(
                    "#!/bin/sh",
                    "LOG_DIR=\"$LOGS_PATH\"",
                    "printf '%s'",
                    "esde_screensavergameselect_filename.txt",
                    "esde_screensavergameselect_name.txt",
                    "esde_screensavergameselect_system.txt",
                    "if [ \"\$#\" -ge 4 ]"
                ),
                forbidden = listOf("echo -n", "#!/bin/bash")
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
            allValid = validCount == 7,
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
