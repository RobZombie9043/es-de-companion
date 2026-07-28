package com.esde.companion.data.apps

import android.content.Context

object AppLauncher {
    fun launch(context: Context, packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            context.startActivity(intent)
        }
    }
}