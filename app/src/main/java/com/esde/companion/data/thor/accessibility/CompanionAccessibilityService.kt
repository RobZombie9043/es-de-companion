package com.esde.companion.data.thor.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The first `AccessibilityService` in this codebase (see CLAUDE.md) - shared by every Thor
 * Settings feature: an on-demand screen-lock action (Lid Wake Guard), a foreground
 * package-change feed (Auto FPS Mode), raw BACK-button press/release feeds (Task Killer), and a
 * single-slot volume-key handler (Volume Sync). Never requests window content
 * (`canRetrieveWindowContent="false"` in `res/xml/thor_accessibility_service_config.xml`).
 *
 * Ported from Asgard's `AsgardAccessibilityService`, which is shared by the same four features
 * there too.
 *
 * Unlike Asgard - which scopes window-state events to `Display.DEFAULT_DISPLAY` because Asgard's
 * own app never runs on the Thor's secondary display - this service deliberately does NOT filter
 * by display id. ES-DE Companion itself runs on the secondary display, and apps launched from its
 * own App Drawer also land there, so Auto FPS Mode needs to react to foreground-package changes
 * on any display; the ignore-list (systemui, launcher, this app's own package, OEM overlays)
 * carries the noise-filtering weight instead. See CLAUDE.md for the open item to validate this
 * on-device.
 *
 * VOLUME_UP/VOLUME_DOWN go through a single installable handler ([setVolumeKeyHandler]) rather
 * than a fire-and-forget listener list like the back-press feed below - whether the event gets
 * *consumed* is a real return value the system needs back, and only one feature (Volume Sync) can
 * ever sensibly own that decision at a time, so there's no ambiguity to resolve between multiple
 * listeners the way there could be with a list.
 */
class CompanionAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        // canRequestFilterKeyEvents in accessibility_service_config.xml is necessary but not
        // sufficient - this runtime flag also has to be set before onKeyEvent will fire.
        serviceInfo =
            serviceInfo?.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instanceRef = null
        disconnectListeners.forEach { it() }
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        notifyForegroundPackage(packageName)
    }

    override fun onInterrupt() = Unit

    /**
     * Never consumes anything for BACK - every press, short or long, reaches the foreground app
     * exactly as if this service didn't exist. This deliberately doesn't try to suppress
     * anything: on the Thor the physical BACK button also presents as a joystick/gamepad HID
     * device, and SDL-based apps like ES-DE/RetroArch read that raw controller state directly
     * from the kernel input device, entirely bypassing Android's KeyEvent/AccessibilityService
     * pipeline - consuming the KeyEvent here has no effect on that separate path. Task Killer
     * instead only acts once BACK has been fully released, so the whole press/release cycle
     * plays out over the original foreground app and there's nothing left "in flight" by the
     * time a different app gains focus. See `TaskKillerCoordinator`'s kdoc.
     *
     * This reports the raw press (for scheduling non-input side effects like a confirmation
     * vibration once a hold threshold elapses - safe to do while the button is still down, since
     * it doesn't touch input dispatch at all) and how long BACK was held on every release. Neither
     * carries a display: `TaskKillerCoordinator` resolves which screen/app to act on fresh, via
     * root shell, rather than from anything this service tracks. Whoever's listening decides what
     * counts as "long" and what to do about it - this service stays a dumb raw-signal source,
     * same as [addForegroundPackageListener] already is for foreground-package changes.
     *
     * `repeatCount == 0` filters out the auto-repeat ACTION_DOWN events the input system
     * generates for a held key - without it, every repeat tick would be reported as a fresh
     * "press".
     *
     * Two independent early-return branches (BACK, volume) rather than one accumulating
     * condition, hence the ReturnCount suppression - same reasoning as
     * RefreshRateDecision.decide.
     */
    @Suppress("ReturnCount")
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) notifyBackPressed()
                KeyEvent.ACTION_UP -> notifyBackReleased(event.eventTime - event.downTime)
            }
            return false
        }
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeKeyHandler?.let { return it(event) }
        }
        return false
    }

    private fun lockScreen(): Boolean = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)

    // One shared companion object backing four independent features' listener
    // registration/notification pairs (lock, foreground-package, back-press/release,
    // volume-key) - see the class kdoc for why this is one service, not four.
    @Suppress("TooManyFunctions")
    companion object {
        @Volatile
        private var instanceRef: WeakReference<CompanionAccessibilityService>? = null

        @Volatile
        private var lastForegroundPackage: String? = null

        private val mainHandler = Handler(Looper.getMainLooper())
        private val foregroundPackageListeners = CopyOnWriteArrayList<(String) -> Unit>()
        private val disconnectListeners = CopyOnWriteArrayList<() -> Unit>()
        private val backPressListeners = CopyOnWriteArrayList<() -> Unit>()
        private val backReleaseListeners = CopyOnWriteArrayList<(Long) -> Unit>()

        @Volatile
        private var volumeKeyHandler: ((KeyEvent) -> Boolean)? = null

        /** True while the service is currently bound (the user has it enabled). */
        fun isEnabled(): Boolean = instanceRef?.get() != null

        /** Returns false if the service isn't currently bound (not enabled by the user). */
        fun requestLock(): Boolean {
            val service = instanceRef?.get() ?: return false
            mainHandler.post { service.lockScreen() }
            return true
        }

        /** Called with the foreground app's package name on every window-state change. */
        fun addForegroundPackageListener(listener: (String) -> Unit) {
            foregroundPackageListeners.add(listener)
        }

        /** Called when the service unbinds (the user disabled it) - a good time to fail safe. */
        fun addDisconnectListener(listener: () -> Unit) {
            disconnectListeners.add(listener)
        }

        /** The foreground app from the most recent window-state event. */
        fun currentForegroundPackage(): String? = lastForegroundPackage

        /** Called on every genuine BACK press (auto-repeat ticks excluded). */
        fun addBackPressListener(listener: () -> Unit) {
            backPressListeners.add(listener)
        }

        /** Called with how long (ms) BACK was held, on every release - a raw feed, not filtered by duration. */
        fun addBackReleaseListener(listener: (heldMs: Long) -> Unit) {
            backReleaseListeners.add(listener)
        }

        /**
         * Installs the one handler allowed to intercept VOLUME_UP/VOLUME_DOWN - it decides, per
         * event, whether to consume it (return `true`, e.g. Volume Sync mode is on) or let the
         * system handle it natively (return `false`). Pass `null` to release it back to native
         * handling entirely. See the class doc for why this is a single slot, not a listener list.
         */
        fun setVolumeKeyHandler(handler: ((KeyEvent) -> Boolean)?) {
            volumeKeyHandler = handler
        }

        private fun notifyForegroundPackage(packageName: String) {
            lastForegroundPackage = packageName
            foregroundPackageListeners.forEach { it(packageName) }
        }

        private fun notifyBackPressed() {
            backPressListeners.forEach { it() }
        }

        private fun notifyBackReleased(heldMs: Long) {
            backReleaseListeners.forEach { it(heldMs) }
        }
    }
}
