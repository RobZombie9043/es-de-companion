package com.esde.companion.data.apps

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object AppLauncher {
    fun launch(
        context: Context,
        packageName: String,
        displayId: Int? = null,
    ) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        val options =
            displayId?.let {
                ActivityOptions.makeBasic().apply { setLaunchDisplayId(it) }
            }
        context.startActivity(intent, options?.toBundle())
    }

    /** Opens Android's own "App info" system screen for [packageName]. */
    fun openAppInfo(
        context: Context,
        packageName: String,
    ) {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        context.startActivity(intent)
    }
}
