package com.esde.companion.domain.thor

/** What a completed BACK hold-and-release should result in. */
enum class TaskKillerDecisionResult {
    DISABLED,
    SHORT_PRESS,
    NO_FOREGROUND,
    PROTECTED,
    PRIVILEGED_UNAVAILABLE,
    FORCE_STOP,
}

/** How long BACK was actually held vs. the configured force-quit threshold. */
data class TaskKillerHoldEvent(
    val heldMs: Long,
    val thresholdMs: Long,
)

/** Feature-availability inputs, bundled the same way as [TaskKillerHoldEvent] to keep
 * [TaskKillerDecision.decide]'s parameter count within this project's `LongParameterList` limit
 * - see CLAUDE.md's ktlint/detekt gotcha. */
data class TaskKillerCapabilities(
    val featureEnabled: Boolean,
    val privilegedAvailable: Boolean,
)

/** What was actually in the foreground at BACK-press time, resolved fresh via root shell - see
 * `TaskKillerCoordinator`'s kdoc for why press time, not release time. */
data class TaskKillerForegroundContext(
    val foregroundPackage: String?,
    val homePackage: String?,
    val foregroundPinned: Boolean,
)

/**
 * Pure decision behind a completed BACK hold-and-release, ported near-verbatim from Asgard's
 * `TaskKillerReactor.decide()`, kept behaviorally identical. Pulled out so it's testable without
 * a `Context`/`Handler`/`AccessibilityService` - see CLAUDE.md's domain-purity rule.
 */
object TaskKillerDecision {
    @Suppress("ReturnCount")
    fun decide(
        hold: TaskKillerHoldEvent,
        capabilities: TaskKillerCapabilities,
        foreground: TaskKillerForegroundContext,
        ownPackage: String,
        excludedPackages: Set<String>,
    ): TaskKillerDecisionResult {
        if (!capabilities.featureEnabled) return TaskKillerDecisionResult.DISABLED
        if (hold.heldMs < hold.thresholdMs) return TaskKillerDecisionResult.SHORT_PRESS
        val foregroundPackage = foreground.foregroundPackage ?: return TaskKillerDecisionResult.NO_FOREGROUND
        val isProtected =
            foregroundPackage == ownPackage ||
                foregroundPackage in excludedPackages ||
                foregroundPackage == foreground.homePackage ||
                foreground.foregroundPinned
        if (isProtected) return TaskKillerDecisionResult.PROTECTED
        if (!capabilities.privilegedAvailable) return TaskKillerDecisionResult.PRIVILEGED_UNAVAILABLE
        return TaskKillerDecisionResult.FORCE_STOP
    }
}
