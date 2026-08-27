package com.esde.companion.data.apps

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.WindowManager

/**
 * Picks the first connected display that isn't the one this app is currently running on.
 * Good enough for the two-screen setup this app targets - if more than two displays are
 * ever connected this just picks one of them, an acceptable simplification rather than
 * something worth a picker UI right now.
 */
object SecondaryDisplayResolver {
    /**
     * [knownCurrentDisplayId] lets a caller that already knows which display it's on (see
     * [CompanionDisplayHolder]) skip [currentDisplayId] entirely - required when [context] isn't
     * a visual Context (e.g. an application Context held by a coordinator running in application
     * scope, like [com.esde.companion.data.gamelist.GameLaunchOverrideCoordinator]), since
     * [currentDisplayId] throws in that case. Every other caller here uses a real Activity
     * Context (`LocalContext.current` in Compose) and can omit it.
     */
    fun secondaryDisplayId(
        context: Context,
        knownCurrentDisplayId: Int? = null,
    ): Int? {
        val displayManager = context.getSystemService(DisplayManager::class.java) ?: return null
        val currentDisplayId = knownCurrentDisplayId ?: currentDisplayId(context)

        return pickSecondary(displayManager.displays.map { it.displayId }, currentDisplayId)
    }

    internal fun pickSecondary(
        displayIds: List<Int>,
        currentId: Int,
    ): Int? = displayIds.firstOrNull { it != currentId }

    /**
     * The display [context] is currently associated with. Only safe to call with a genuinely
     * visual Context (an Activity, or one created via `Context#createWindowContext`/
     * `createDisplayContext`) - `Context#getDisplay()` throws `UnsupportedOperationException` on
     * API 30+ for any other kind (confirmed on-device: an application Context passed from
     * [com.esde.companion.data.gamelist.GameLaunchOverrideCoordinator], which runs in application
     * scope, crashed here). Public so [com.esde.companion.ui.MainActivity] can call it with its
     * own (visual) Context to populate [CompanionDisplayHolder] once, for callers that can't
     * safely call this themselves to pass into [secondaryDisplayId] instead.
     */
    fun currentDisplayId(context: Context): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.displayId ?: Display.DEFAULT_DISPLAY
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay.displayId
        }
}
