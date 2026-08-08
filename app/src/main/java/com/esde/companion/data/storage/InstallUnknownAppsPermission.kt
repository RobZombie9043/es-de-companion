package com.esde.companion.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Wraps the "install unknown apps" (`REQUEST_INSTALL_PACKAGES`) runtime grant check and
 * the intent used to request it, for the update checker's Download & Install flow -
 * same shape as [AllFilesAccessPermission].
 *
 * Unlike [AllFilesAccessPermission.isGranted], [isGranted] here takes a [Context]
 * parameter deliberately: `PackageManager.canRequestPackageInstalls()` is an instance
 * method (there's no static equivalent to `Environment.isExternalStorageManager()`) -
 * this isn't an oversight to "fix" into a no-arg signature.
 */
object InstallUnknownAppsPermission {
    fun isGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun requestIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
}
