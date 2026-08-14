package com.esde.companion.domain.model

/**
 * How a [RetroAchievementsGameMatch.Found] result was obtained. Provenance, not a numeric
 * confidence score - [ExactTitle] isn't a guarantee of correctness and [NormalizedTitle]/
 * [PartialTitle] aren't inherently unreliable, they just describe what happened. The
 * correction picker (see `RetroAchievementsScreen`'s kebab menu) is reachable regardless of
 * which method produced a match, rather than the UI trying to read intent into it.
 *
 * [RomHash] is the one entry that's qualitatively different from the rest: it identifies
 * the exact ROM dump via a hash from gamelist.xml, rather than a name heuristic, which is
 * why `ResolveRetroAchievementsGameUseCase` tries it ahead of every title tier. Listed
 * first to reflect that priority - the ordinal isn't persisted anywhere, so the ordering is
 * free to reflect intent.
 */
enum class MatchMethod {
    RomHash,
    ExactTitle,
    NormalizedTitle,
    SpaceInsensitiveTitle,
    PartialTitle,
    ManualOverride,
}
