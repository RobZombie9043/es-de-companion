package com.esde.companion.domain.model

import com.esde.companion.domain.parser.GamelistXml.matchesGamelistPath

/**
 * The package to launch, if any, when ES-DE starts playing the given game - the pure decision
 * logic behind Game Launch Override, deliberately kept Android-free and unit-testable the same
 * way the core state-reduction pipeline is (see CLAUDE.md). A per-game entry in [gameOverrides]
 * always wins over [systemDefaults] when present, *including* an entry whose own `packageName`
 * is null - that represents an explicit "never launch anything for this game," which must be
 * able to suppress a configured system default rather than silently falling back to it. Only the
 * true absence of a matching entry falls back to [systemDefaults]. Returns null (launch nothing)
 * when neither is configured.
 *
 * [romPath] is matched against each override's [GameLaunchOverride.relativeRomPath] by suffix
 * (see [matchesGamelistPath]) rather than exact equality, since overrides are authored from
 * gamelist.xml's own ES-DE-relative paths while [romPath] here is the absolute path ES-DE
 * reports via [AppState.PlayingGame].
 */
fun resolveGameLaunchPackage(
    systemShortName: String,
    romPath: String,
    systemDefaults: Map<String, String>,
    gameOverrides: List<GameLaunchOverride>,
): String? {
    val gameOverride =
        gameOverrides.firstOrNull {
            it.systemShortName == systemShortName && romPath.matchesGamelistPath(it.relativeRomPath)
        }
    if (gameOverride != null) return gameOverride.packageName

    return systemDefaults[systemShortName]
}
