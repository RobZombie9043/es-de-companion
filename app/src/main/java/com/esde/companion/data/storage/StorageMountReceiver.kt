package com.esde.companion.data.storage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.esde.companion.CompanionApplication

/**
 * Manifest-registered receiver for storage mount/unmount events - the signal that lets
 * `EsdeLogFileRepository`'s and `FileEsdeInstallationRepository`'s directory watchers rearm
 * once an SD card that wasn't mounted yet at watch-construction time becomes available (or
 * tear down cleanly if removed). This app can auto-start via its `HOME` intent filter before
 * vold finishes mounting removable storage after a reboot - see `AppContainer`/CLAUDE.md.
 *
 * Instantiated by the OS via its manifest declaration and a required no-arg constructor -
 * there is no constructor-injection path a manifest-declared [BroadcastReceiver] can go
 * through. This is a narrow, platform-forced exception to this project's "constructor
 * injection throughout" rule (see CLAUDE.md), not an ad-hoc singleton: it does nothing but
 * forward one signal into [AppContainer.storageMountEvents][com.esde.companion.AppContainer].
 */
class StorageMountReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val application = context.applicationContext as? CompanionApplication ?: return
        application.appContainer.storageMountEvents.notifyChanged()
    }
}
