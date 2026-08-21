package com.esde.companion.data.thor

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.KeyEvent
import android.view.ViewConfiguration
import com.esde.companion.data.thor.accessibility.CompanionAccessibilityService
import com.esde.companion.domain.model.VolumeSyncMode
import com.esde.companion.domain.model.VolumeSyncTarget
import com.esde.companion.domain.thor.SECONDARY_VOLUME_MAX
import com.esde.companion.domain.thor.SECONDARY_VOLUME_MIN
import com.esde.companion.domain.thor.proportionalSecondaryVolumeTarget
import com.esde.companion.domain.usecase.ObserveVolumeSyncEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVolumeSyncModeUseCase
import com.esde.companion.domain.usecase.SetVolumeSyncEnabledUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * App-scoped reactive piece for Thor Settings > Volume Sync - full port of Asgard's
 * `VolumeSyncReactor`, installing [CompanionAccessibilityService]'s single-slot volume-key
 * handler and driving both screens' volume together while it's held.
 *
 * Unlike [TaskKillerCoordinator]/[AutoFpsCoordinator], [handleVolumeKey] itself decides per
 * event whether to consume it - toggling the feature off just makes it start returning `false`
 * again (native system handling resumes), so there's no separate install/uninstall step tied to
 * the enabled flag; [start] installs the handler exactly once, unconditionally, matching how
 * [CompanionAccessibilityService]'s single volume-key slot has no per-feature remove API either.
 *
 * Consuming VOLUME_UP/VOLUME_DOWN here means Android's own default handling - which produces the
 * device's native "top screen to 0/max, then bottom screen" sequential behavior - never runs for
 * a consumed event. So unlike a normal accessibility service that just observes keys, this one
 * has to fully replace what the system would have done for the top screen too (see
 * [VolumeSyncShell.adjustMain]), not just add the bottom-screen half.
 *
 * Held-key auto-repeat is self-driven with a Handler loop using the system's own repeat timings,
 * rather than trusting the device to deliver repeated ACTION_DOWN events with incrementing
 * `repeatCount` through accessibility key filtering.
 *
 * [VolumeSyncMode.FollowFocus] needs one extra thing `Linked` doesn't: which screen to target is
 * unknown until [VolumeSyncShell.resolveFocusedTarget] returns, and that's a blocking root-shell
 * round trip - too slow to run on every repeat tick. So it's resolved once per hold (on the first
 * tick that finds [heldFollowFocusTarget] unset) and cached for every subsequent tick of the same
 * hold. The very first tick of a Follow-Focus hold is consequently a no-op step; every tick after
 * that is instant.
 *
 * Linked vs. Follow-Focus mode, held-repeat driving, and coalesced secondary-write draining
 * each need their own small methods rather than one dense function, hence the
 * TooManyFunctions suppression below.
 */
@Suppress("TooManyFunctions")
class VolumeSyncCoordinator(
    private val observeVolumeSyncEnabled: ObserveVolumeSyncEnabledUseCase,
    private val setVolumeSyncEnabled: SetVolumeSyncEnabledUseCase,
    private val observeVolumeSyncMode: ObserveVolumeSyncModeUseCase,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    // PrivilegedShell.execute() blocks on a root-shell round trip - never call it from the
    // accessibility service's main thread. A single HandlerThread (not a pool) is deliberate:
    // see requestSecondaryWrite/drainSecondaryWrite below for why serial execution is what makes
    // the "only the latest target survives" coalescing work without extra locking.
    private val workerThread = HandlerThread("VolumeSyncCoordinator").apply { start() }
    private val workerHandler = Handler(workerThread.looper)

    @Volatile
    private var featureEnabled = false

    @Volatile
    private var mode: VolumeSyncMode = VolumeSyncMode.Linked

    @Volatile
    private var volumeHeld = false

    @Volatile
    private var heldKeyCode = 0

    @Volatile
    private var heldMode: VolumeSyncMode = VolumeSyncMode.Linked

    @Volatile
    private var heldFollowFocusTarget: VolumeSyncTarget? = null

    @Volatile
    private var resolvingFollowFocusTarget = false

    // Written from both the accessibility service's main thread (requestSecondaryWrite) and
    // workerHandler's thread (drainSecondaryWrite). The check-then-act race between the two is
    // benign: drainSecondaryWrite always re-checks for <0 before acting, so a redundant post at
    // worst becomes a no-op, never a lost/duplicated write.
    @Volatile
    private var pendingSecondaryTarget = -1

    private var appContext: Context? = null

    private val volumeRepeat =
        object : Runnable {
            override fun run() {
                if (!volumeHeld) return
                performStep()
                mainHandler.postDelayed(this, systemKeyRepeatDelay())
            }
        }

    fun start(
        context: Context,
        applicationScope: CoroutineScope,
    ) {
        appContext = context.applicationContext
        CompanionAccessibilityService.setVolumeKeyHandler(::handleVolumeKey)
        CompanionAccessibilityService.addDisconnectListener {
            cancelHeldRepeat()
            if (featureEnabled) applicationScope.launch { setVolumeSyncEnabled(false) }
        }
        applicationScope.launch {
            observeVolumeSyncEnabled().collect { featureEnabled = it }
        }
        applicationScope.launch {
            observeVolumeSyncMode().collect { mode = it }
        }
    }

    @Suppress("ReturnCount")
    private fun handleVolumeKey(event: KeyEvent): Boolean {
        if (appContext == null || !featureEnabled || !RefreshRateController.canWrite()) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                // A device-generated repeat tick for the key already being held - ignore rather
                // than restart the hold, since our own Handler loop is what drives repeats here.
                if (volumeHeld && heldKeyCode == event.keyCode) return true
                cancelHeldRepeat()
                volumeHeld = true
                heldKeyCode = event.keyCode
                heldMode = mode
                performStep()
                mainHandler.postDelayed(volumeRepeat, systemKeyRepeatTimeout())
            }
            KeyEvent.ACTION_UP -> {
                if (heldKeyCode == event.keyCode) cancelHeldRepeat()
            }
        }
        return true
    }

    /**
     * Runs on the main thread - cheap by construction: AudioManager calls or a queued worker
     * post, never a blocking one.
     */
    private fun performStep() {
        val context = appContext ?: return
        val increase = heldKeyCode == KeyEvent.KEYCODE_VOLUME_UP
        when (heldMode) {
            VolumeSyncMode.Linked -> stepLinked(context, increase)
            VolumeSyncMode.FollowFocus -> stepFollowFocus(context, increase)
        }
    }

    private fun stepLinked(
        context: Context,
        increase: Boolean,
    ) {
        val mainTarget = VolumeSyncShell.adjustMain(context, if (increase) 1 else -1)
        val mainMax = VolumeSyncShell.mainMax(context)
        requestSecondaryWrite(proportionalSecondaryVolumeTarget(mainTarget, mainMax))
    }

    private fun stepFollowFocus(
        context: Context,
        increase: Boolean,
    ) {
        val target = heldFollowFocusTarget
        if (target == null) {
            if (!resolvingFollowFocusTarget) {
                resolvingFollowFocusTarget = true
                workerHandler.post(::resolveFollowFocusTarget)
            }
            return
        }
        when (target) {
            VolumeSyncTarget.Main -> VolumeSyncShell.adjustMain(context, if (increase) 1 else -1)
            VolumeSyncTarget.Secondary -> {
                val current = VolumeSyncShell.readSecondary(context)
                val next = (current + if (increase) 1 else -1).coerceIn(SECONDARY_VOLUME_MIN, SECONDARY_VOLUME_MAX)
                requestSecondaryWrite(next)
            }
        }
    }

    /** Runs on workerHandler's thread - the one blocking root-shell round trip per hold, not per tick. */
    private fun resolveFollowFocusTarget() {
        val resolved = VolumeSyncShell.resolveFocusedTarget()
        resolvingFollowFocusTarget = false
        // The hold may have already ended, or the user may have flipped the mode mid-hold, while
        // this was in flight - a late result for either case must not retarget/resume a step that
        // no longer applies.
        if (!volumeHeld || heldMode != VolumeSyncMode.FollowFocus) return
        // A failed query (nothing sensible to target) falls back to the top screen rather than
        // silently doing nothing for the rest of the hold.
        heldFollowFocusTarget = resolved ?: VolumeSyncTarget.Main
        mainHandler.post { if (volumeHeld) performStep() }
    }

    /**
     * Held-key repeat fires roughly every tens of milliseconds, far faster than a root-shell
     * round trip completes - posting one write per step would queue up an ever-growing backlog
     * and leave the bottom screen audibly lagging behind the top one by the time the key is
     * released. Only the most recent target survives: each drain pass re-reads
     * [pendingSecondaryTarget] just before writing, so a target superseded while a write was
     * still in flight is skipped rather than queued.
     */
    private fun requestSecondaryWrite(target: Int) {
        val alreadyDraining = pendingSecondaryTarget >= 0
        pendingSecondaryTarget = target
        if (alreadyDraining) return
        workerHandler.post(::drainSecondaryWrite)
    }

    private fun drainSecondaryWrite() {
        val target = pendingSecondaryTarget
        if (target < 0) return
        pendingSecondaryTarget = -1
        VolumeSyncShell.writeSecondary(target)
        if (pendingSecondaryTarget >= 0) drainSecondaryWrite()
    }

    private fun cancelHeldRepeat() {
        mainHandler.removeCallbacks(volumeRepeat)
        volumeHeld = false
        heldKeyCode = 0
        heldFollowFocusTarget = null
        resolvingFollowFocusTarget = false
    }

    private fun systemKeyRepeatTimeout(): Long =
        runCatching { ViewConfiguration.getKeyRepeatTimeout().toLong() }
            .getOrDefault(ViewConfiguration.getLongPressTimeout().toLong())
            .coerceAtLeast(1L)

    private fun systemKeyRepeatDelay(): Long =
        runCatching { ViewConfiguration.getKeyRepeatDelay().toLong() }
            .getOrDefault(DEFAULT_KEY_REPEAT_DELAY_MS)
            .coerceAtLeast(1L)

    private companion object {
        const val DEFAULT_KEY_REPEAT_DELAY_MS = 50L
    }
}
