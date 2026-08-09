package com.esde.companion.domain.model

/**
 * Which [FabSlot] occupies each of the four corners (Settings > UI Settings > FAB
 * Control). Persisted as four independent DataStore values (see
 * FileOnboardingRepository), bundled here so the one cross-corner business rule -
 * [FabType.Music] can only occupy one of the two top corners - can be resolved as a single
 * pure operation rather than scattered across call sites.
 */
data class FabAssignments(
    val topStart: FabSlot,
    val topEnd: FabSlot,
    val bottomStart: FabSlot,
    val bottomEnd: FabSlot,
) {
    operator fun get(position: FabPosition): FabSlot =
        when (position) {
            FabPosition.TopStart -> topStart
            FabPosition.TopEnd -> topEnd
            FabPosition.BottomStart -> bottomStart
            FabPosition.BottomEnd -> bottomEnd
        }

    /**
     * Returns a copy with [position] assigned [slot]. If [slot]'s type is [FabType.Music]
     * and the opposite top corner already holds Music, that corner is first swapped to
     * whatever [position] currently holds (the whole [FabSlot], including e.g. a
     * [FabType.CustomApp] selection) - the displaced FAB moves to the newly-vacated corner
     * rather than being discarded. No other reassignment touches a second corner.
     */
    fun with(
        position: FabPosition,
        slot: FabSlot,
    ): FabAssignments {
        val opposite = position.topOpposite
        val base =
            if (slot.type == FabType.Music && opposite != null && this[opposite].type == FabType.Music) {
                updated(opposite, this[position])
            } else {
                this
            }
        return base.updated(position, slot)
    }

    private fun updated(
        position: FabPosition,
        slot: FabSlot,
    ): FabAssignments =
        when (position) {
            FabPosition.TopStart -> copy(topStart = slot)
            FabPosition.TopEnd -> copy(topEnd = slot)
            FabPosition.BottomStart -> copy(bottomStart = slot)
            FabPosition.BottomEnd -> copy(bottomEnd = slot)
        }

    companion object {
        val Default =
            FabAssignments(
                topStart = FabSlot(FabType.Music),
                topEnd = FabSlot(FabType.Settings),
                bottomStart = FabSlot(FabType.None),
                bottomEnd = FabSlot(FabType.GameManual),
            )
    }
}
