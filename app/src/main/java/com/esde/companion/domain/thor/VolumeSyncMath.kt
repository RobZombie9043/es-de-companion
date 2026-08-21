package com.esde.companion.domain.thor

import kotlin.math.roundToInt

/** The bottom screen's volume range on this device - a fixed vendor-defined 0-15 scale,
 * independent of whatever the top screen's `AudioManager` stream max happens to be. */
const val SECONDARY_VOLUME_MIN = 0
const val SECONDARY_VOLUME_MAX = 15

/**
 * Maps a main-stream index onto the bottom screen's fixed [SECONDARY_VOLUME_MIN]..
 * [SECONDARY_VOLUME_MAX] range by relative percentage (main's max isn't guaranteed to be 15,
 * even though it happens to be on the Thor today) - pure and unit-testable without a Context,
 * ported near-verbatim from Asgard's `VolumeSyncController.proportionalSecondaryTarget`.
 */
fun proportionalSecondaryVolumeTarget(
    mainCurrent: Int,
    mainMax: Int,
): Int {
    val safeMax = mainMax.coerceAtLeast(1)
    val target = ((mainCurrent.toDouble() * SECONDARY_VOLUME_MAX) / safeMax).roundToInt()
    return target.coerceIn(SECONDARY_VOLUME_MIN, SECONDARY_VOLUME_MAX)
}
