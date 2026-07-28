package com.esde.companion.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

/**
 * Wraps the "All files access" (MANAGE_EXTERNAL_STORAGE) runtime grant check and the
 * intent used to request it. This app is not Play Store-distributed, so the broader grant
 * is used directly rather than Storage Access Framework - see CLAUDE.md / conversation
 * history for why.
 */
object AllFilesAccessPermission {

    fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

    /**
     * Prefers the app-specific grant screen; falls back to the general all-files-access
     * list screen on OEM builds where the app-specific intent doesn't resolve.
     */
    fun requestIntent(context: Context): Intent {
        val appSpecific = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        return if (appSpecific.resolveActivity(context.packageManager) != null) {
            appSpecific
        } else {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }
    }
}