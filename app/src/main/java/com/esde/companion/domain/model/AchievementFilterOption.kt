package com.esde.companion.domain.model

/**
 * Independent, togglable filters over a game's achievement list - same filters
 * RetroAchievements' own website offers. Unlike a single-select "which one" choice, any
 * combination of these can be active at once; see [filteredByAchievementFilters] for how they
 * combine.
 */
enum class AchievementFilterOption {
    MissableOnly,
    LockedOnly,
}

private fun AchievementFilterOption.matches(achievement: AchievementItem): Boolean =
    when (this) {
        AchievementFilterOption.MissableOnly -> achievement.type == AchievementType.Missable
        AchievementFilterOption.LockedOnly -> !achievement.unlocked
    }

/**
 * Applies every active [filters] option as a successive narrowing pass - i.e. AND across
 * options, not OR: enabling both [AchievementFilterOption.MissableOnly] and
 * [AchievementFilterOption.LockedOnly] shows only achievements that are both missable and
 * still locked, not either one. These are two independent facets (what an achievement *is* vs.
 * the user's relationship to it), unlike e.g. a multi-select "which type of game" filter where
 * several selected values should broaden the results (OR) rather than narrow them - the same
 * widget shape can combine differently depending on what it's actually filtering.
 *
 * An empty set is the "show everything" case - the old dedicated `All` option is gone because
 * it's now just the identity of this fold.
 */
fun List<AchievementItem>.filteredByAchievementFilters(filters: Set<AchievementFilterOption>): List<AchievementItem> =
    filters.fold(this) { remaining, option -> remaining.filter { option.matches(it) } }
