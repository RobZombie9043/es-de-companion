package com.esde.companion.domain.thor

import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshRateDecisionTest {
    private val ignored = setOf("com.android.systemui", "com.android.launcher3", "android")
    private val triggers = setOf("org.libretro.retroarch", "com.esde.companion")

    private fun state(
        highRefreshActive: Boolean,
        revertPending: Boolean,
    ) = RefreshRateReactorState(highRefreshActive = highRefreshActive, revertPending = revertPending)

    @Test
    fun `ignored package is ignored regardless of feature state`() {
        val result =
            RefreshRateDecision.decide(
                packageName = "com.android.systemui",
                ignoredPackages = ignored,
                featureEnabled = true,
                triggerPackages = triggers,
                state = state(highRefreshActive = true, revertPending = false),
            )
        assertEquals(RefreshRateDecisionResult.IGNORE, result)
    }

    @Test
    fun `feature disabled ignores even a trigger package`() {
        val result =
            RefreshRateDecision.decide(
                packageName = "org.libretro.retroarch",
                ignoredPackages = ignored,
                featureEnabled = false,
                triggerPackages = triggers,
                state = state(highRefreshActive = false, revertPending = false),
            )
        assertEquals(RefreshRateDecisionResult.IGNORE, result)
    }

    @Test
    fun `trigger package enters high refresh`() {
        val result =
            RefreshRateDecision.decide(
                packageName = "org.libretro.retroarch",
                ignoredPackages = ignored,
                featureEnabled = true,
                triggerPackages = triggers,
                state = state(highRefreshActive = false, revertPending = false),
            )
        assertEquals(RefreshRateDecisionResult.ENTER_HIGH_REFRESH, result)
    }

    @Test
    fun `trigger package while already high refresh still enters high refresh`() {
        val result =
            RefreshRateDecision.decide(
                packageName = "org.libretro.retroarch",
                ignoredPackages = ignored,
                featureEnabled = true,
                triggerPackages = triggers,
                state = state(highRefreshActive = true, revertPending = true),
            )
        assertEquals(RefreshRateDecisionResult.ENTER_HIGH_REFRESH, result)
    }

    @Test
    fun `non-trigger package while high refresh active schedules a revert check`() {
        val result =
            RefreshRateDecision.decide(
                packageName = "com.some.other.app",
                ignoredPackages = ignored,
                featureEnabled = true,
                triggerPackages = triggers,
                state = state(highRefreshActive = true, revertPending = false),
            )
        assertEquals(RefreshRateDecisionResult.SCHEDULE_REVERT_CHECK, result)
    }

    @Test
    fun `non-trigger package while a revert is already pending is a no-op`() {
        val result =
            RefreshRateDecision.decide(
                packageName = "com.some.other.app",
                ignoredPackages = ignored,
                featureEnabled = true,
                triggerPackages = triggers,
                state = state(highRefreshActive = true, revertPending = true),
            )
        assertEquals(RefreshRateDecisionResult.NO_OP, result)
    }

    @Test
    fun `non-trigger package while not in high refresh is a no-op`() {
        val result =
            RefreshRateDecision.decide(
                packageName = "com.some.other.app",
                ignoredPackages = ignored,
                featureEnabled = true,
                triggerPackages = triggers,
                state = state(highRefreshActive = false, revertPending = false),
            )
        assertEquals(RefreshRateDecisionResult.NO_OP, result)
    }
}
