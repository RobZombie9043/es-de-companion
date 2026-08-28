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
 *
 * A single tap here only proves the shell command ran, not that focus actually landed and
 * stuck on [displayId] - use [reclaimDisplayFocus] (this file's only remaining caller of this
 * function) rather than calling this directly.
 */
private fun focusDisplay(
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
 * actually take focus first means the later [reclaimDisplayFocus] call is reacting to the real
 * steal rather than guessing it already happened. Note this only returns `true` on the first
 * poll that sees [displayId] focused and never looks again - [reclaimDisplayFocus] is what
 * additionally guards against [displayId] taking focus a *second* time later on, and like this
 * function it is time-boxed rather than persistent: it gives up for good once its own window
 * elapses, it never keeps watching for the life of the game session.
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

/**
 * Polls [displayId] for the entirety of [durationMillis] and returns `false` the moment it
 * stops being the focused display - unlike [awaitDisplayFocus] (which returns `true` as soon as
 * [displayId] is *first* seen focused and never looks again), this also catches a later
 * focus-steal that overwrites an earlier-looking-successful reclaim within the same short
 * window. Used by [reclaimDisplayFocus] to decide whether a tap actually stuck, not just that
 * the shell command that fired it succeeded.
 */
private fun isFocusHeld(
    displayId: Int,
    durationMillis: Long,
): Boolean {
    val deadline = System.currentTimeMillis() + durationMillis
    do {
        if (resolveFocusedDisplayId() != displayId) return false
        Thread.sleep(AWAIT_FOCUS_POLL_INTERVAL_MS)
    } while (System.currentTimeMillis() < deadline)
    return true
}

/**
 * Result of [reclaimDisplayFocus]: whether [displayId] was confirmed to hold focus by the end
 * of the attempt window, and how many tap attempts it took to either land or exhaust the
 * window - surfaced so callers can log the difference between "worked first try" and "took N
 * retries" instead of a single collapsed boolean.
 */
data class DisplayFocusReclaimResult(
    val settled: Boolean,
    val attempts: Int,
)

/**
 * Retries a reclaim tap against [displayId], verifying via [isFocusHeld] after each one that
 * focus didn't just briefly land there but actually stuck - covers both remaining
 * unreliability gaps a single tap left open:
 * - The tap itself not registering (transient root-shell hiccup, system busy) - retried
 *   immediately, no [isFocusHeld] wait wasted on a tap [focusDisplay] itself reports failed.
 * - The launched app re-stealing focus a *second* time after an earlier tap already landed
 *   (e.g. a splash screen followed by a real main activity) - the symmetric case to the race
 *   [awaitDisplayFocus] exists to fix for the *pre*-reclaim wait, left uncorrected for the
 *   reclaim step itself until now.
 *
 * Strictly time-boxed to [windowMillis] from the moment this is called, via a single
 * `deadline = now + windowMillis` computed up front (same deadline shape as
 * [awaitDisplayFocus], not an attempt counter) - once it passes, this stops unconditionally and
 * returns `settled = false`, regardless of how many attempts that took. This is a deliberate
 * guarantee: this function is only ever active for the initial launch period right after a game
 * starts, never a persistent watcher correcting focus for the rest of the game session.
 *
 * Same blocking/off-main-thread/never-throws contract as every other function in this file.
 */
fun reclaimDisplayFocus(
    context: Context,
    displayId: Int,
    windowMillis: Long = RECLAIM_WINDOW_MS,
    settleWindowMillis: Long = RECLAIM_SETTLE_WINDOW_MS,
): DisplayFocusReclaimResult {
    val deadline = System.currentTimeMillis() + windowMillis
    var attempts = 0
    do {
        attempts++
        val tapped = focusDisplay(context, displayId)
        if (tapped && isFocusHeld(displayId, settleWindowMillis)) {
            return DisplayFocusReclaimResult(settled = true, attempts = attempts)
        }
    } while (System.currentTimeMillis() < deadline)
    return DisplayFocusReclaimResult(settled = false, attempts = attempts)
}

private const val AWAIT_FOCUS_TIMEOUT_MS = 8000L
private const val AWAIT_FOCUS_POLL_INTERVAL_MS = 150L

// Total wall-clock time reclaimDisplayFocus is ever active for, starting right after the
// launch-focus wait - a time budget, not a retry count, so "how long could this possibly keep
// touching the screen after launch" always has a fixed, obvious answer.
private const val RECLAIM_WINDOW_MS = 5000L

// How long one reclaim tap must hold displayId focused, unbroken, before it's trusted.
private const val RECLAIM_SETTLE_WINDOW_MS = 600L

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
