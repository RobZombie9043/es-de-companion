package com.esde.companion.domain.model

/**
 * A user-configured launch-app override for one specific game, keyed by [systemShortName] plus
 * the ES-DE-relative [relativeRomPath] (see `GamelistXml.matchesGamelistPath`) rather than an
 * absolute path - the picker UI this is authored from only ever has gamelist.xml's own relative
 * `<path>` values to work with (see `GamelistBulkParser`), and matching against a live
 * [AppState.PlayingGame.romPath] at resolve time is done by suffix, not exact equality.
 *
 * [packageName] being present-but-null is a real, distinct state from no entry existing at all:
 * it means "explicitly launch nothing for this game," which must be able to suppress a
 * configured [GamelistSystemSummary]-scoped system default - see [resolveGameLaunchPackage].
 * "Inherit the system default" is instead represented by the *absence* of an entry for a game,
 * not a third enum-like state on this class.
 */
data class GameLaunchOverride(
    val systemShortName: String,
    val relativeRomPath: String,
    val packageName: String?,
)

/** One system's worth of gamelist.xml, for the systems list page - just enough to render a row
 * without parsing every system's full game list up front. */
data class GamelistSystemSummary(
    val shortName: String,
    val gameCount: Int,
)
