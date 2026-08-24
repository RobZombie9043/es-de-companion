package com.esde.companion.data.systemstatus

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Wraps the BLUETOOTH_CONNECT runtime grant check (API 31+ only - below that, Bluetooth
 * connection state is available without a dangerous permission). This is the first
 * runtime-dangerous-permission check in this codebase - every other permission
 * (AllFilesAccessPermission, InstallUnknownAppsPermission, ThorAccessibilityPermission) sends
 * the user to a system Settings screen rather than showing an in-app system prompt.
 */
object BluetoothConnectPermission {
    const val PERMISSION = Manifest.permission.BLUETOOTH_CONNECT

    fun hasBluetoothHardware(context: Context): Boolean {
        return context.getSystemService(BluetoothManager::class.java)?.adapter != null
    }

    fun isGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED
}
