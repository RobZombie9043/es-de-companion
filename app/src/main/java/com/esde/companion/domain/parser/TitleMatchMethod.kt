package com.esde.companion.domain.parser

/** How [GameTitleMatcher] arrived at a [TitleMatchResult.Matched] result. */
enum class TitleMatchMethod {
    ExactTitle,
    NormalizedTitle,
    PartialTitle,
}
