package com.esde.companion.domain.thor

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskKillerDecisionTest {
    private val ignored = setOf("com.android.systemui", "com.android.launcher3", "android", "com.odin.gameassistant")
    private val ownPackage = "com.esde.companion"
    private val homePackage = "xyz.mjolnir.launcher"

    private fun hold(
        heldMs: Long = 2000L,
        thresholdMs: Long = 800L,
    ) = TaskKillerHoldEvent(heldMs = heldMs, thresholdMs = thresholdMs)

    private fun capabilities(
        featureEnabled: Boolean = true,
        privilegedAvailable: Boolean = true,
    ) = TaskKillerCapabilities(featureEnabled = featureEnabled, privilegedAvailable = privilegedAvailable)

    private fun foreground(
        foregroundPackage: String? = "org.libretro.retroarch",
        homePackage: String? = this.homePackage,
        foregroundPinned: Boolean = false,
    ) = TaskKillerForegroundContext(
        foregroundPackage = foregroundPackage,
        homePackage = homePackage,
        foregroundPinned = foregroundPinned,
    )

    private fun decideDefaults(
        hold: TaskKillerHoldEvent = hold(),
        capabilities: TaskKillerCapabilities = capabilities(),
        foreground: TaskKillerForegroundContext = foreground(),
        ownPackage: String = this.ownPackage,
        excludedPackages: Set<String> = ignored,
    ): TaskKillerDecisionResult =
        TaskKillerDecision.decide(
            hold = hold,
            capabilities = capabilities,
            foreground = foreground,
            ownPackage = ownPackage,
            excludedPackages = excludedPackages,
        )

    @Test
    fun `feature disabled short-circuits regardless of hold duration`() {
        val result = decideDefaults(hold = hold(heldMs = 5000L), capabilities = capabilities(featureEnabled = false))
        assertEquals(TaskKillerDecisionResult.DISABLED, result)
    }

    @Test
    fun `hold under threshold is a short press even when everything else qualifies`() {
        assertEquals(TaskKillerDecisionResult.SHORT_PRESS, decideDefaults(hold = hold(heldMs = 300L)))
    }

    @Test
    fun `null foreground package is left alone`() {
        val result = decideDefaults(foreground = foreground(foregroundPackage = null))
        assertEquals(TaskKillerDecisionResult.NO_FOREGROUND, result)
    }

    @Test
    fun `own package is protected`() {
        val result = decideDefaults(foreground = foreground(foregroundPackage = ownPackage))
        assertEquals(TaskKillerDecisionResult.PROTECTED, result)
    }

    @Test
    fun `each excluded package is protected`() {
        for (pkg in ignored) {
            val result = decideDefaults(foreground = foreground(foregroundPackage = pkg))
            assertEquals(TaskKillerDecisionResult.PROTECTED, result)
        }
    }

    @Test
    fun `the resolved home package is protected even when not in the excluded list`() {
        val result = decideDefaults(foreground = foreground(foregroundPackage = homePackage))
        assertEquals(TaskKillerDecisionResult.PROTECTED, result)
    }

    @Test
    fun `a screen-pinned foreground app is protected`() {
        val result = decideDefaults(foreground = foreground(foregroundPinned = true))
        assertEquals(TaskKillerDecisionResult.PROTECTED, result)
    }

    @Test
    fun `privileged shell unavailable is reported separately from protected`() {
        val result = decideDefaults(capabilities = capabilities(privilegedAvailable = false))
        assertEquals(TaskKillerDecisionResult.PRIVILEGED_UNAVAILABLE, result)
    }

    @Test
    fun `fully qualifying hold force-stops the foreground app`() {
        assertEquals(TaskKillerDecisionResult.FORCE_STOP, decideDefaults())
    }
}
