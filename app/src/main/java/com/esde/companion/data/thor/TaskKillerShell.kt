package com.esde.companion.data.thor

import com.esde.companion.domain.thor.parseRecentTaskIds

/**
 * Root-shell operations Task Killer needs, all via [PrivilegedShell] - ported near-verbatim from
 * Asgard's `TaskKillerReactor`. Every call here blocks on a root-shell round trip; callers must
 * run these off the main thread.
 */
object TaskKillerShell {
    /**
     * Resolves [displayId]'s topResumedActivity via `dumpsys activity activities`. Used both by
     * [resolveFocusedForegroundPackage] (for [displayId] = [resolveFocusedDisplayId]'s result)
     * and directly by `TaskKillerCoordinator` for Task Killer's This Screen/Other Screen/Both
     * targets, which need a specific display's foreground app rather than whichever display
     * currently has hardware-key focus.
     */
    fun resolveForegroundPackageForDisplay(displayId: Int): String? {
        val activityCommand =
            "dumpsys activity activities | awk " +
                "'/^Display #/{f=(\$0 ~ /^Display #$displayId /)} f && /topResumedActivity=/{print; f=0}'"
        val activityLine = PrivilegedShell.execute(activityCommand).getOrNull()?.trim() ?: return null
        return Regex("""u0 ([^/]+)/""").find(activityLine)?.groupValues?.get(1)
    }

    /**
     * Two root-shell round trips, resolving fresh state rather than trusting anything tracked
     * from accessibility events: first [resolveFocusedDisplayId] - the actual display current
     * hardware key routing (BACK, HOME, etc.) targets, as opposed to a display that merely has a
     * "resumed" activity (both screens can simultaneously) - then that display's foreground
     * package via [resolveForegroundPackageForDisplay].
     */
    fun resolveFocusedForegroundPackage(): String? {
        val displayId = resolveFocusedDisplayId() ?: return null
        return resolveForegroundPackageForDisplay(displayId)
    }

    fun forceStop(packageName: String): Boolean = PrivilegedShell.execute("am force-stop $packageName").isSuccess

    /**
     * PrivilegedShell only ever returns a single line of output, so filtering has to happen
     * inside the shell command via grep; multiple matches (a package can have more than one
     * task) are squashed with `tr` into one line so they still survive the single-line boundary,
     * then split back apart and parsed in Kotlin - see [parseRecentTaskIds]'s kdoc for the parsing
     * side.
     */
    fun findRecentTaskIds(packageName: String): List<Int> {
        val command =
            "dumpsys activity recents | grep -E 'Recent #.*(A=[0-9]*:$packageName}|I=$packageName/)' | tr '\\n' ';'"
        val output = PrivilegedShell.execute(command).getOrNull() ?: return emptyList()
        return parseRecentTaskIds(output.replace(';', '\n'), packageName)
    }

    /** There's no "remove this one task" am/cmd subcommand on this firmware, only "am stack
     * remove <TASK_ID>". */
    fun removeRecentsTask(taskId: Int): Boolean = PrivilegedShell.execute("am stack remove $taskId").isSuccess
}
