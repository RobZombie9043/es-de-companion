package com.esde.companion.data.apps

import android.content.Context
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads a launchable app's icon. A real binder/disk call, so this always runs off the
 * main thread - callers load it per-item asynchronously (see AppDrawerItem) rather than
 * blocking composition. Returns null if the package has since been uninstalled out from
 * under a stale [com.esde.companion.domain.model.InstalledApp] list entry - an expected,
 * routine race with the broadcast-driven refresh, not an error.
 */
object AppIconLoader {
    suspend fun loadIcon(context: Context, packageName: String): Drawable? =
        withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
        }
}