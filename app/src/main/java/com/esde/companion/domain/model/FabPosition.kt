package com.esde.companion.domain.model

/**
 * One of the four screen corners a FAB can be assigned to (see [FabAssignments]). Named
 * to match this codebase's existing `Alignment.TopStart`/`Alignment.TopEnd` convention
 * (see CornerButtonMetrics/MainScreen/MainActivity), not "Left"/"Right" - the Settings UI
 * presents these to the user as "Top Left"/"Top Right"/etc., but the domain model stays
 * consistent with the Compose alignment vocabulary the UI layer maps it onto.
 */
enum class FabPosition {
    TopStart,
    TopEnd,
    BottomStart,
    BottomEnd,
    ;

    /**
     * The other top corner - used to enforce that [FabType.Music] can only occupy one of
     * the two top corners (see [FabAssignments.with]). Null for the two bottom positions,
     * which have no such exclusivity rule.
     */
    val topOpposite: FabPosition?
        get() =
            when (this) {
                TopStart -> TopEnd
                TopEnd -> TopStart
                BottomStart, BottomEnd -> null
            }
}
