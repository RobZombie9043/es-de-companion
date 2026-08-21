package com.esde.companion.domain.thor

import kotlin.math.abs

/**
 * Pure "should we lock the screen right now" logic for Thor Settings > Lid Wake Guard -
 * ported from Asgard's `GuardPrefs.isClosed`/`LidWakeGuardReactor.onScreenOn`, split into
 * two small functions so each is independently unit-testable against fixture values with no
 * Android dependency (`SensorManager`, `AccessibilityService`) - see CLAUDE.md's domain-purity
 * rule.
 */
object LidWakeGuardDecision {
    /**
     * A raw sensor [reading] is treated as "closed" when it's numerically closer to the
     * calibrated closed value than the calibrated open one - avoids assuming a fixed
     * unit/scale or an exact-zero reading across sensor firmwares, matching Asgard's own
     * calibration approach exactly.
     */
    fun isReadingClosed(
        reading: Float,
        closedValue: Float,
        openValue: Float,
    ): Boolean = abs(reading - closedValue) <= abs(reading - openValue)

    /**
     * Whether a stray wake event right now should trigger a re-lock. [guardEnabled] and
     * [isCalibrated] gate the feature/calibration state; [sensorReadsClosed] is the caller's
     * already-computed [isReadingClosed] result (or `false` if the sensor read timed out -
     * an unknown reading is never treated as "closed").
     */
    fun shouldLock(
        guardEnabled: Boolean,
        isCalibrated: Boolean,
        sensorReadsClosed: Boolean,
    ): Boolean = guardEnabled && isCalibrated && sensorReadsClosed
}
