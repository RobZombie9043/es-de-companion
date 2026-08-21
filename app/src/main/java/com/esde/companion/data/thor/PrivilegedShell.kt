package com.esde.companion.data.thor

import android.annotation.SuppressLint
import android.os.IBinder
import android.os.Parcel
import java.nio.charset.Charset

/**
 * Talks to AYN's privileged system service, registered as "PServerBinder" - ported near-verbatim
 * from Asgard's `PrivilegedShell` (see CLAUDE.md's Auto FPS Mode section for the full
 * provenance/rationale). The public `Settings.System` write API blocks `min_refresh_rate`/
 * `peak_refresh_rate` client-side regardless of permission (a `targetSdkVersion`-gated check
 * inside the Java API itself, not a server-side restriction) - running the write through
 * PServerBinder as root goes straight to `SettingsProvider`, sidestepping that block entirely.
 *
 * This is Thor-firmware-specific hidden-API reflection with no substitute (no `WRITE_SETTINGS`,
 * no Shizuku, no root fallback) - genuinely brittle, and must degrade to "feature unavailable"
 * rather than crash if a future Thor OTA removes or renames this service. [execute] never throws;
 * every failure comes back as a [Result.failure].
 */
@SuppressLint("DiscouragedPrivateApi", "PrivateApi")
object PrivilegedShell {
    private const val SERVICE_NAME = "PServerBinder"

    private val binder: IBinder? by lazy { getService(SERVICE_NAME) }

    val isAvailable: Boolean get() = binder != null

    // Reflection into a hidden binder transact can fail in ways no narrower catch type
    // reliably covers (ClassNotFoundException, NoSuchMethodException, RemoteException,
    // arbitrary RuntimeExceptions from the OEM's own service implementation) - see this
    // object's own kdoc on why this must degrade to Result.failure rather than crash.
    @Suppress("TooGenericExceptionCaught")
    fun execute(cmd: String): Result<String?> {
        val service = binder ?: return Result.failure(IllegalStateException("$SERVICE_NAME not available"))
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeStringArray(arrayOf(cmd, "1"))
            service.transact(0, data, reply, 0)
            val result = reply.createByteArray()?.toString(Charset.defaultCharset())?.trim()
            Result.success(if (result == "null") null else result)
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun getSystemSetting(key: String): Result<String?> = execute("settings get system $key")

    fun putSystemSetting(
        key: String,
        value: String,
    ): Result<String?> = execute("settings put system $key $value")

    private fun getService(name: String): IBinder? =
        runCatching {
            val serviceManager = Class.forName("android.os.ServiceManager")
            val getService = serviceManager.getDeclaredMethod("getService", String::class.java)
            getService.invoke(serviceManager, name) as? IBinder
        }.getOrNull()
}
