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
    fun secondaryDisplayId(context: Context): Int? {
        val displayManager = context.getSystemService(DisplayManager::class.java) ?: return null
        val currentDisplayId = currentDisplayId(context)

        return pickSecondary(displayManager.displays.map { it.displayId }, currentDisplayId)
    }

    internal fun pickSecondary(
        displayIds: List<Int>,
        currentId: Int,
    ): Int? = displayIds.firstOrNull { it != currentId }

    private fun currentDisplayId(context: Context): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.displayId ?: Display.DEFAULT_DISPLAY
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay.displayId
        }
}
