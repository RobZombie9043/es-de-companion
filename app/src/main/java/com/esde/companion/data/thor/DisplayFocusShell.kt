package com.esde.companion.data.thor

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics

/**
 * Hands hardware-key input focus back to [displayId] via a root-shell round trip - there's no
 * public Android API to move focus to an arbitrary display.
 *
 * Two mechanisms were tried and rejected before this one, both confirmed on the physical Thor
 * test device:
 * - Synthetic key injection (`input -d <id> keyevent <code>`), on the theory that
 *   [resolveFocusedDisplayId]'s kdoc describes `mTopFocusedDisplayId` as tracking real
 *   hardware-key routing. Confirmed wrong: neither `KEYCODE_UNKNOWN` nor a real key
 *   (`KEYCODE_SCROLL_LOCK`) moved `mTopFocusedDisplayId` at all.
 * - `am start -n <pkg>/<activity> --display <id>`, re-targeting whatever activity was already
 *   the top-resumed activity on [displayId]. This *did* work, but only resumes-in-place without
 *   side effects for a `singleTop`/`singleTask`/`singleInstance` target - a `standard`-launch-mode
 *   activity already on top would get a redundant second instance stacked on it instead, so this
 *   needed an extra `PackageManager` launch-mode guard to be safe for an arbitrary app.
 *
 * What's actually used, confirmed on-device: injecting a touch `DOWN` immediately followed by
 * `CANCEL` at [displayId]'s own center - physical tap-on-screen is how this device's dual-screen
 * focus genuinely changes in normal use, which touch injection reproduces directly, without
 * touching any app's activity lifecycle at all (so the `am start -n` launch-mode risk has no
 * analog here). `CANCEL` rather than `UP` deliberately aborts gesture recognition before any
 * click-handling in the target app can fire, so this stays safe regardless of what's rendered at
 * the tap point - relevant since the target display isn't always predictable content (ES-DE, or
 * Companion's own screen with user-placed widgets in arbitrary positions). The center is computed
 * per-display rather than hardcoded because the two screens are confirmed different resolutions
 * on this device.
 *
 * Blocking (root-shell round trip); call off the main thread. Never throws; degrades to `false`
 * on any failure (unresolved display metrics, [PrivilegedShell] unavailable), same contract as
 * every other [PrivilegedShell] consumer.
 */
fun focusDisplay(
    context: Context,
    displayId: Int,
): Boolean {
    val (x, y) = displayCenter(context, displayId) ?: return false
    val command = "input -d $displayId motionevent DOWN $x $y; input -d $displayId motionevent CANCEL $x $y"
    return PrivilegedShell.execute(command).isSuccess
}

/**
 * Blocks (polling, not suspending - same off-main-thread contract as [focusDisplay] and every
 * other [PrivilegedShell] consumer) until [displayId] actually becomes the focused display, or
 * [timeoutMillis] elapses.
 *
 * Exists because [focusDisplay] alone raced the app it's meant to recover from: `startActivity`
 * returning doesn't mean the launched app has actually finished loading and grabbed focus yet -
 * confirmed on-device as a real bug, not just the theoretical risk this was originally flagged
 * as. Calling [focusDisplay] immediately after launch could win the race and land *before* the
 * launched app's own focus-steal, which then simply overwrites it once the app finishes loading,
 * with nothing left to correct it afterward. Waiting here for [displayId] (the launch target) to
 * actually take focus first means the later [focusDisplay] call is reacting to the real steal
 * rather than guessing it already happened.
 */
fun awaitDisplayFocus(
    displayId: Int,
    timeoutMillis: Long = AWAIT_FOCUS_TIMEOUT_MS,
): Boolean {
    val deadline = System.currentTimeMillis() + timeoutMillis
    while (System.currentTimeMillis() < deadline) {
        if (resolveFocusedDisplayId() == displayId) return true
        Thread.sleep(AWAIT_FOCUS_POLL_INTERVAL_MS)
    }
    return false
}

private const val AWAIT_FOCUS_TIMEOUT_MS = 8000L
private const val AWAIT_FOCUS_POLL_INTERVAL_MS = 150L

@Suppress("ReturnCount")
private fun displayCenter(
    context: Context,
    displayId: Int,
): Pair<Int, Int>? {
    val displayManager = context.getSystemService(DisplayManager::class.java) ?: return null
    val display = displayManager.getDisplay(displayId) ?: return null
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    display.getRealMetrics(metrics)
    return metrics.widthPixels / 2 to metrics.heightPixels / 2
}
