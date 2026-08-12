package com.esde.companion.domain.model

/**
 * How a [RetroAchievementsGameMatch.Found] result was obtained. Provenance, not a numeric
 * confidence score - [ExactTitle] isn't a guarantee of correctness and [NormalizedTitle]/
 * [PartialTitle] aren't inherently unreliable, they just describe what happened. The
 * correction picker (see `RetroAchievementsScreen`'s kebab menu) is reachable regardless of
 * which method produced a match, rather than the UI trying to read intent into it.
 */
enum class MatchMethod {
    ExactTitle,
    NormalizedTitle,
    PartialTitle,
    ManualOverride,
}
