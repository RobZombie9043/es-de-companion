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
        // FLAG_ACTIVITY_NEW_TASK is required when this is called from a non-Activity context
        // (GameLaunchOverrideCoordinator calls this from application scope) - Android throws
        // otherwise. Harmless to add unconditionally: every other caller here is already an
        // Activity context (LocalContext.current in Compose), and starting a new task from an
        // Activity context is a supported, unremarkable case too.
        val intent =
            context.packageManager.getLaunchIntentForPackage(packageName)
                ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                ?: return
        val options =
            displayId?.let {
                ActivityOptions.makeBasic().apply { setLaunchDisplayId(it) }
            }
        context.startActivity(intent, options?.toBundle())
    }

    /** Opens Android's top-level system Settings app (not this app's own settings). */
    fun openSystemSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
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
