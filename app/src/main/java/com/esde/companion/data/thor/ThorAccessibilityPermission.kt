package com.esde.companion.data.thor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import com.esde.companion.data.thor.accessibility.CompanionAccessibilityService

/**
 * Wraps the accessibility-service grant check and request intent for
 * [CompanionAccessibilityService] - same shape as
 * [com.esde.companion.data.storage.AllFilesAccessPermission]/
 * [com.esde.companion.data.storage.InstallUnknownAppsPermission] (see CLAUDE.md: device/OS
 * capability checks deliberately stay plain `data/` objects, no domain wrapper).
 */
object ThorAccessibilityPermission {
    /** Reads the persisted enabled-services list directly, rather than relying on the service
     * having (re)bound yet - works for Settings UI even right after the user grants it. */
    fun isEnabledInSettings(context: Context): Boolean {
        val expected = "${context.packageName}/${CompanionAccessibilityService::class.java.name}"
        val enabled =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    /**
     * Jumps straight to this service's own toggle page instead of the generic list of every
     * installed accessibility service, via the unofficial but widely-used
     * `:settings:fragment_args_key`/`:settings:show_fragment_args` extras stock AOSP Settings
     * (and most OEM forks) honor. Falls back to the generic list if the Settings app on a given
     * device doesn't recognize the extras - never worse than before.
     */
    fun requestIntent(context: Context): Intent {
        val componentName = ComponentName(context, CompanionAccessibilityService::class.java)
        val fragmentArgsKey = componentName.flattenToString()
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            putExtra(":settings:fragment_args_key", fragmentArgsKey)
            putExtra(
                ":settings:show_fragment_args",
                Bundle().apply { putString(":settings:fragment_args_key", fragmentArgsKey) },
            )
        }
    }
}
