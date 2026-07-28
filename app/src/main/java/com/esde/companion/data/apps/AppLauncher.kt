package com.esde.companion.data.apps

import android.app.ActivityOptions
import android.content.Context

object AppLauncher {
    fun launch(context: Context, packageName: String, displayId: Int? = null) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        val options = displayId?.let {
            ActivityOptions.makeBasic().apply { setLaunchDisplayId(it) }
        }
        context.startActivity(intent, options?.toBundle())
    }
}