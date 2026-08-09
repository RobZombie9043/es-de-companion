package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FabAssignmentsTest {
    private val defaults = FabAssignments.Default

    @Test
    fun `get returns the value stored for each position`() {
        val assignments =
            FabAssignments(
                topStart = FabSlot(FabType.Music),
                topEnd = FabSlot(FabType.Settings),
                bottomStart = FabSlot(FabType.None),
                bottomEnd = FabSlot(FabType.GameManual),
            )

        assertEquals(FabSlot(FabType.Music), assignments[FabPosition.TopStart])
        assertEquals(FabSlot(FabType.Settings), assignments[FabPosition.TopEnd])
        assertEquals(FabSlot(FabType.None), assignments[FabPosition.BottomStart])
        assertEquals(FabSlot(FabType.GameManual), assignments[FabPosition.BottomEnd])
    }

    @Test
    fun `reassigning a non-Music type only changes the target corner`() {
        val result = defaults.with(FabPosition.BottomStart, FabSlot(FabType.GameManual))

        assertEquals(
            defaults.copy(bottomStart = FabSlot(FabType.GameManual)),
            result,
        )
    }

    @Test
    fun `assigning Music to a top corner with no Music elsewhere just sets that corner`() {
        // Defaults already have Music at TopStart and Settings at TopEnd - reassigning
        // TopEnd to something else first isolates the "no conflict" case.
        val noMusicYet = defaults.copy(topStart = FabSlot(FabType.Settings))

        val result = noMusicYet.with(FabPosition.TopStart, FabSlot(FabType.Music))

        assertEquals(noMusicYet.copy(topStart = FabSlot(FabType.Music)), result)
    }

    @Test
    fun `assigning Music to TopEnd while TopStart already has Music swaps TopStart`() {
        // defaults: topStart = Music, topEnd = Settings.
        val result = defaults.with(FabPosition.TopEnd, FabSlot(FabType.Music))

        assertEquals(
            defaults.copy(topStart = FabSlot(FabType.Settings), topEnd = FabSlot(FabType.Music)),
            result,
        )
    }

    @Test
    fun `assigning Music to TopStart while TopEnd already has Music swaps TopEnd`() {
        val musicAtTopEnd = defaults.copy(topStart = FabSlot(FabType.GameManual), topEnd = FabSlot(FabType.Music))

        val result = musicAtTopEnd.with(FabPosition.TopStart, FabSlot(FabType.Music))

        assertEquals(
            musicAtTopEnd.copy(topStart = FabSlot(FabType.Music), topEnd = FabSlot(FabType.GameManual)),
            result,
        )
    }

    @Test
    fun `swapping a CustomApp slot out of the way preserves its selected package`() {
        val customAppAtTopEnd =
            defaults.copy(topStart = FabSlot(FabType.Music), topEnd = FabSlot(FabType.CustomApp, "com.example.app"))

        val result = customAppAtTopEnd.with(FabPosition.TopEnd, FabSlot(FabType.Music))

        assertEquals(
            customAppAtTopEnd.copy(
                topStart = FabSlot(FabType.CustomApp, "com.example.app"),
                topEnd = FabSlot(FabType.Music),
            ),
            result,
        )
    }

    @Test
    fun `reassigning Music to the corner it already occupies is a no-op`() {
        val result = defaults.with(FabPosition.TopStart, FabSlot(FabType.Music))

        assertEquals(defaults, result)
    }

    @Test
    fun `assigning Music to a bottom corner does not trigger the top swap`() {
        // Bottom positions have no topOpposite, so even if a bottom corner somehow held
        // Music, assigning Music elsewhere shouldn't touch it - the exclusivity rule only
        // exists between the two top corners.
        val result = defaults.with(FabPosition.BottomStart, FabSlot(FabType.Music))

        assertEquals(defaults.copy(bottomStart = FabSlot(FabType.Music)), result)
    }
}
