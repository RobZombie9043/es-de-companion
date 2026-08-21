package com.esde.companion.data.thor

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager

/** Ported from Asgard's `taskkiller.Vibration.vibrate`. [amplitude] is 1-255; a single short
 * pulse is enough to confirm a hold registered. */
fun vibrate(
    context: Context,
    durationMs: Long,
    amplitude: Int,
) {
    val vibratorManager =
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager ?: return
    vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
}
