package com.esde.companion.data.systemstatus

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import com.esde.companion.domain.model.BatteryStatus
import com.esde.companion.domain.model.SystemStatus
import com.esde.companion.domain.model.batteryTierFor
import com.esde.companion.domain.repository.SystemStatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge

/**
 * Backs the SystemStatus/ClockAndSystemStatus FABs. Combines three independent Android
 * signals - battery (sticky broadcast, no permission), Wifi connectivity (ConnectivityManager
 * network callback, ACCESS_NETWORK_STATE), and Bluetooth connection state (ACL broadcasts,
 * gated behind BLUETOOTH_CONNECT on API 31+) - into one [SystemStatus] stream. Each
 * sub-stream follows the same channelFlow/callbackFlow-wraps-a-callback-API idiom as
 * PackageManagerAppsRepository.
 *
 * [bluetoothPermissionRecheck] lets a caller (SystemStatusFabContent's ON_RESUME check) force
 * the Bluetooth half to re-evaluate after the user grants/revokes the permission via system
 * Settings - there's no OS broadcast for that.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidSystemStatusRepository(
    private val context: Context,
    private val bluetoothPermissionRecheck: Flow<Unit>,
) : SystemStatusRepository {
    override fun observeSystemStatus(): Flow<SystemStatus> =
        combine(batteryStatusFlow(), wifiConnectedFlow(), bluetoothConnectedFlow()) { battery, wifi, bluetooth ->
            SystemStatus(battery = battery, wifiConnected = wifi, bluetoothConnected = bluetooth)
        }

    private fun batteryStatusFlow(): Flow<BatteryStatus> =
        callbackFlow {
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        receivedContext: Context?,
                        intent: Intent?,
                    ) {
                        intent?.let { trySend(it.toBatteryStatus()) }
                    }
                }
            // Sticky intent - registerReceiver returns the current state immediately, so
            // there's no separate initial-poll step needed.
            val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            sticky?.let { trySend(it.toBatteryStatus()) }
            awaitClose { context.unregisterReceiver(receiver) }
        }.flowOn(Dispatchers.IO)

    private fun Intent.toBatteryStatus(): BatteryStatus {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * PERCENT_SCALE) / scale else 0
        val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryStatus(tier = batteryTierFor(percent), isCharging = isCharging)
    }

    private fun wifiConnectedFlow(): Flow<Boolean> =
        callbackFlow {
            val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
            val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(true)
                    }

                    override fun onLost(network: Network) {
                        trySend(false)
                    }
                }
            trySend(false)
            connectivityManager.registerNetworkCallback(request, callback)
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }.flowOn(Dispatchers.IO)

    // merge(flowOf(Unit), ...) so the flow evaluates immediately on subscription, not just
    // after the first recheck signal - bluetoothPermissionRecheck's replay=1 buffer starts
    // empty until SystemStatusFabContent's ON_RESUME path calls notifyChanged() at least once.
    private fun bluetoothConnectedFlow(): Flow<Boolean> =
        merge(flowOf(Unit), bluetoothPermissionRecheck).flatMapLatest { rawBluetoothConnectedFlow() }

    private fun rawBluetoothConnectedFlow(): Flow<Boolean> =
        callbackFlow {
            if (!BluetoothConnectPermission.hasBluetoothHardware(context) ||
                !BluetoothConnectPermission.isGranted(context)
            ) {
                trySend(false)
                awaitClose {}
                return@callbackFlow
            }
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        receivedContext: Context?,
                        intent: Intent?,
                    ) {
                        when (intent?.action) {
                            BluetoothDevice.ACTION_ACL_CONNECTED -> trySend(true)
                            BluetoothDevice.ACTION_ACL_DISCONNECTED -> trySend(false)
                        }
                    }
                }
            val filter =
                IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                    addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                }
            context.registerReceiver(receiver, filter)
            // No stable pre-API-33 public API to read an already-connected device's state
            // without a profile proxy (BluetoothDevice.isConnected() is API 33+ only, and
            // minSdk is 29) - start from "not connected" until the first ACL broadcast
            // arrives. Documented limitation, not a bug.
            trySend(false)
            awaitClose { context.unregisterReceiver(receiver) }
        }.flowOn(Dispatchers.IO)

    private companion object {
        const val PERCENT_SCALE = 100
    }
}
