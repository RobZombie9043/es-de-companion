package com.esde.companion.domain.model

/**
 * How a [RetroAchievementsGameMatch.Found] result was obtained. Provenance, not a numeric
 * confidence score - [ExactTitle] isn't a guarantee of correctness and [NormalizedTitle]
 * isn't inherently unreliable, they just describe what happened. The UI uses this to decide
 * how prominent a "wrong game?" affordance should be: subtle for [ExactTitle], more visible
 * for [NormalizedTitle], low-key for [ManualOverride].
 */
enum class MatchMethod {
    ExactTitle,
    NormalizedTitle,
    ManualOverride,
}
