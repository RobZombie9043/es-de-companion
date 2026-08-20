package com.esde.companion.data.thor

import android.util.Log

/**
 * Wraps the exact two Settings writes Auto FPS Mode needs ("min_refresh_rate"/
 * "peak_refresh_rate" -> 120.00001 on entering a trigger app, -> 60.0 on leaving) - ported from
 * Asgard's `RefreshRateController`, confirmed live via `dumpsys display`: `DisplayModeDirector`
 * observes these keys' *system* scope specifically (not secure), written through
 * [PrivilegedShell] rather than the public `Settings.System` API (see its kdoc for why).
 */
object RefreshRateController {
    private const val TAG = "RefreshRateController"
    private const val KEY_MIN_REFRESH_RATE = "min_refresh_rate"
    private const val KEY_PEAK_REFRESH_RATE = "peak_refresh_rate"
    private const val HIGH_REFRESH_RATE = "120.00001"
    private const val NORMAL_REFRESH_RATE = "60.0"

    fun canWrite(): Boolean = PrivilegedShell.isAvailable

    /** @return whether both writes succeeded - if PServerBinder breaks (e.g. a future Thor
     * OTA), this comes back false instead of failing silently. */
    fun setHighRefreshRate(): Boolean = apply(HIGH_REFRESH_RATE, writePeakFirst = true)

    fun setNormalRefreshRate(): Boolean = apply(NORMAL_REFRESH_RATE, writePeakFirst = false)

    /**
     * min_refresh_rate and peak_refresh_rate are two separate settings writes, so there's always
     * a moment between them where only one has taken effect. Writing whichever bound moves the
     * range *outward* first keeps min <= peak true at every instant: raising (60->120) widens
     * peak before min follows; lowering (120->60) narrows min before peak follows. An inverted
     * range (min > peak) mid-write is a plausible cause of a display going blank and needing a
     * hard power cycle to recover, observed during Asgard's own testing - see CLAUDE.md.
     */
    private fun apply(
        value: String,
        writePeakFirst: Boolean,
    ): Boolean {
        fun write(key: String) =
            PrivilegedShell
                .putSystemSetting(key, value)
                .onFailure { Log.w(TAG, "Failed to write $key=$value", it) }

        val (first, second) =
            if (writePeakFirst) {
                KEY_PEAK_REFRESH_RATE to KEY_MIN_REFRESH_RATE
            } else {
                KEY_MIN_REFRESH_RATE to KEY_PEAK_REFRESH_RATE
            }
        val firstResult = write(first)
        val secondResult = write(second)
        return firstResult.isSuccess && secondResult.isSuccess
    }
}
