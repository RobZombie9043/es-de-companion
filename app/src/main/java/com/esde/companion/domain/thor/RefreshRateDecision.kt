package com.esde.companion.domain.thor

/** What Auto FPS mode's foreground-app reactor should do in response to a foreground-package change. */
enum class RefreshRateDecisionResult { IGNORE, ENTER_HIGH_REFRESH, SCHEDULE_REVERT_CHECK, NO_OP }

/**
 * The reactor's own state at the moment a foreground-package change arrives - bundled into one
 * [RefreshRateDecision.decide] parameter (rather than two separate ones) to keep that function's
 * parameter count within this project's `LongParameterList` limit, same reasoning as
 * [com.esde.companion.data.storage.SelfHealConfig] (see CLAUDE.md's ktlint/detekt gotcha).
 */
data class RefreshRateReactorState(
    val highRefreshActive: Boolean,
    val revertPending: Boolean,
)

/**
 * Pure "what should happen" decision behind Auto FPS mode's foreground-app reactor - ported
 * near-verbatim from Asgard's `AutoFpsForegroundReactor.decide()`, kept behaviorally identical.
 * Pulled out so it's testable without a `Context`/`Handler`/`AccessibilityService` - see
 * CLAUDE.md's domain-purity rule.
 */
object RefreshRateDecision {
    @Suppress("ReturnCount")
    fun decide(
        packageName: String,
        ignoredPackages: Set<String>,
        featureEnabled: Boolean,
        triggerPackages: Set<String>,
        state: RefreshRateReactorState,
    ): RefreshRateDecisionResult {
        if (packageName in ignoredPackages) return RefreshRateDecisionResult.IGNORE
        if (!featureEnabled) return RefreshRateDecisionResult.IGNORE
        if (packageName in triggerPackages) return RefreshRateDecisionResult.ENTER_HIGH_REFRESH
        return if (state.highRefreshActive && !state.revertPending) {
            RefreshRateDecisionResult.SCHEDULE_REVERT_CHECK
        } else {
            RefreshRateDecisionResult.NO_OP
        }
    }
}
