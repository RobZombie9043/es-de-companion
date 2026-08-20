package com.esde.companion.data.thor.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The first `AccessibilityService` in this codebase (see CLAUDE.md) - shared by Thor Settings'
 * two features: an on-demand screen-lock action (Lid Wake Guard) and a foreground
 * package-change feed (Auto FPS Mode). Never requests window content
 * (`canRetrieveWindowContent="false"` in `res/xml/thor_accessibility_service_config.xml`).
 *
 * Ported from Asgard's `AsgardAccessibilityService`, deliberately narrowed to only what these
 * two features need - Asgard's current version also handles BACK-key and volume-key events for
 * two other features (Task Killer, Volume Sync) this app doesn't have, so those are not ported;
 * `canRequestFilterKeyEvents` is correspondingly left out of this service's config.
 *
 * Unlike Asgard - which scopes events to `Display.DEFAULT_DISPLAY` because Asgard's own app
 * never runs on the Thor's secondary display - this service deliberately does NOT filter by
 * display id. ES-DE Companion itself runs on the secondary display, and apps launched from its
 * own App Drawer also land there, so Auto FPS Mode needs to react to foreground-package changes
 * on any display; the ignore-list (systemui, launcher, this app's own package, OEM overlays)
 * carries the noise-filtering weight instead. See CLAUDE.md for the open item to validate this
 * on-device.
 */
class CompanionAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
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

    private fun lockScreen(): Boolean = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)

    companion object {
        @Volatile
        private var instanceRef: WeakReference<CompanionAccessibilityService>? = null

        @Volatile
        private var lastForegroundPackage: String? = null

        private val mainHandler = Handler(Looper.getMainLooper())
        private val foregroundPackageListeners = CopyOnWriteArrayList<(String) -> Unit>()
        private val disconnectListeners = CopyOnWriteArrayList<() -> Unit>()

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

        private fun notifyForegroundPackage(packageName: String) {
            lastForegroundPackage = packageName
            foregroundPackageListeners.forEach { it(packageName) }
        }
    }
}
