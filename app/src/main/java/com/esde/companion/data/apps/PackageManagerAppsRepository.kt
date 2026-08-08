package com.esde.companion.data.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.repository.InstalledAppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Queries launchable activities (ACTION_MAIN / CATEGORY_LAUNCHER) via PackageManager,
 * excluding this app's own package - the drawer shouldn't offer to launch itself.
 *
 * Re-queries whenever a package is installed, removed, or updated, so the drawer stays
 * current without the app needing a restart. Mirrors the channelFlow + CONFLATED signal
 * + re-check-on-signal pattern used by EsdeLogFileRepository for the same reason: any
 * number of coalesced broadcasts should still only trigger one fresh query.
 */
class PackageManagerAppsRepository(
    private val context: Context,
) : InstalledAppsRepository {
    override fun observeInstalledApps(): Flow<List<InstalledApp>> =
        channelFlow {
            send(queryLaunchableApps())

            val signal = Channel<Unit>(capacity = Channel.CONFLATED)
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        receivedContext: Context?,
                        intent: Intent?,
                    ) {
                        signal.trySend(Unit)
                    }
                }
            val filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                    addAction(Intent.ACTION_PACKAGE_CHANGED)
                    addDataScheme("package")
                }
            context.registerReceiver(receiver, filter)

            try {
                for (unit in signal) {
                    send(queryLaunchableApps())
                }
            } finally {
                context.unregisterReceiver(receiver)
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun queryLaunchableApps(): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val packageManager = context.packageManager

            packageManager.queryIntentActivities(launcherIntent, 0)
                .filter { it.activityInfo.packageName != context.packageName }
                .map { resolveInfo ->
                    InstalledApp(
                        packageName = resolveInfo.activityInfo.packageName,
                        label = resolveInfo.loadLabel(packageManager).toString(),
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
        }
}
