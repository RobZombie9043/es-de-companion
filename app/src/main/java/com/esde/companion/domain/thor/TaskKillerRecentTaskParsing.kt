package com.esde.companion.domain.thor

/**
 * Pure so it's unit-testable against captured dumpsys text without a Context - ported
 * near-verbatim from Asgard's `TaskKillerReactor.parseRecentTaskIds`, mirrors
 * [TaskKillerDecision.decide]'s pure/I-O split. A "Recent #<n>: Task{... #<taskId> type=...
 * A=<uid>:<packageName>}" line carries two different numbers: the leading "Recent #<n>" is just
 * this entry's position in the list, not a task id - the real task id is the "#<taskId>
 * type=..." that follows inside the braces. Matches every such line, not just the first, since a
 * package can have more than one task in Recents. A task only carries "A=<uid>:<packageName>}"
 * when the app declares an explicit `android:taskAffinity` - without one, dumpsys shows
 * "I=<packageName>/<activity>}" instead.
 */
internal fun parseRecentTaskIds(
    dumpsysOutput: String,
    packageName: String,
): List<Int> {
    // Android's regex engine (ICU-backed, unlike desktop java.util.regex/grep) requires a literal
    // "}" to be escaped, or Pattern.compile throws a PatternSyntaxException.
    val escaped = Regex.escape(packageName)
    val recentLine = Regex("""Recent #.*(A=\d*:$escaped\}|I=$escaped/)""")
    val taskId = Regex("""#(\d+) type=""")
    return dumpsysOutput
        .lineSequence()
        .filter { recentLine.containsMatchIn(it) }
        .mapNotNull { line ->
            taskId
                .findAll(line)
                .lastOrNull()
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        }.toList()
}
