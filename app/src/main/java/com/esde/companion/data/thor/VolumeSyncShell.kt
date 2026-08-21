package com.esde.companion.data.thor

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import com.esde.companion.domain.model.VolumeSyncTarget
import com.esde.companion.domain.thor.SECONDARY_VOLUME_MAX
import com.esde.companion.domain.thor.SECONDARY_VOLUME_MIN

/**
 * I/O side of Thor Settings > Volume Sync, ported near-verbatim from Asgard's
 * `VolumeSyncController` - see [com.esde.companion.domain.thor.proportionalSecondaryVolumeTarget]
 * for the pure math this wraps. The top screen is plain Android - [AudioManager]'s
 * `STREAM_MUSIC` - but the bottom screen's level lives entirely in a vendor-defined
 * `Settings.System` key with no `AudioManager` equivalent (confirmed on Asgard's own hardware
 * via `dumpsys audio`: the device exposes exactly one real audio stream/device - the two-screen
 * split only exists at this one extra Settings key, the same vendor pattern
 * [RefreshRateController] already relies on for `min_refresh_rate`/`peak_refresh_rate`). Writing
 * it goes through [PrivilegedShell]/PServerBinder as root, same as `RefreshRateController` - no
 * `WRITE_SETTINGS` grant needed.
 */
object VolumeSyncShell {
    private const val SECONDARY_SETTING_KEY = "secondary_screen_volume_level"

    /** Cheap, unprivileged existence check - confirms this firmware actually exposes the vendor key. */
    fun hasSecondarySetting(context: Context): Boolean =
        runCatching {
            Settings.System.getString(context.contentResolver, SECONDARY_SETTING_KEY) != null
        }.getOrDefault(false)

    fun readMain(context: Context): Int = audioManager(context)?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0

    fun mainMax(context: Context): Int =
        audioManager(context)
            ?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            ?.coerceAtLeast(1)
            ?: SECONDARY_VOLUME_MAX

    /**
     * Steps the top screen by [delta] (+-1 per key event, same as native behavior) and returns
     * the resulting index, so the caller can derive the bottom screen's proportional target from
     * the exact value that was actually applied rather than assuming the step landed cleanly.
     */
    fun adjustMain(
        context: Context,
        delta: Int,
    ): Int {
        val audio = audioManager(context) ?: return readMain(context)
        val max = mainMax(context)
        val target = (audio.getStreamVolume(AudioManager.STREAM_MUSIC) + delta).coerceIn(0, max)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return target
    }

    /** Blocking - runs a root-shell round trip via PServerBinder. Call off the main thread. */
    fun writeSecondary(target: Int): Boolean {
        val clamped = target.coerceIn(SECONDARY_VOLUME_MIN, SECONDARY_VOLUME_MAX)
        return PrivilegedShell.putSystemSetting(SECONDARY_SETTING_KEY, clamped.toString()).isSuccess
    }

    /** Unprivileged - reading a Settings.System int needs no special access, only writing does. */
    fun readSecondary(context: Context): Int =
        runCatching { Settings.System.getInt(context.contentResolver, SECONDARY_SETTING_KEY) }
            .getOrDefault(0)
            .coerceIn(SECONDARY_VOLUME_MIN, SECONDARY_VOLUME_MAX)

    /**
     * Resolves which screen currently holds hardware-key focus for Follow Focus mode. Root-backed
     * via [resolveFocusedDisplayId], so - unlike the community project this was cross-checked
     * against (pth2000/ThorVolumeLink), which has to calibrate an odd/even parity against AYN's
     * `focus_change` counter across several accessibility events because it has no privileged
     * shell to just ask directly - this needs no calibration step at all. Blocking; call off the
     * main thread.
     */
    fun resolveFocusedTarget(): VolumeSyncTarget? =
        resolveFocusedDisplayId()?.let { if (it == 0) VolumeSyncTarget.Main else VolumeSyncTarget.Secondary }

    private fun audioManager(context: Context): AudioManager? = context.getSystemService(AudioManager::class.java)
}
