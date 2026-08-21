package com.esde.companion.data.thor

/**
 * Resolves the WindowManager display id that currently holds hardware-key focus (0 = top/built-in,
 * non-zero = bottom/external - confirmed on-device) via a single root-shell round trip. Shared by
 * `TaskKillerShell.resolveFocusedForegroundPackage` (routing a held BACK button at the right
 * screen's foreground app) and `VolumeSyncShell.resolveFocusedTarget` (Volume Sync's Follow Focus
 * mode) - both need the exact same query, ported from Asgard's `TaskKillerReactor`/
 * `VolumeSyncController`, which duplicated it identically before this extraction.
 *
 * Blocking - runs a root-shell round trip via [PrivilegedShell]. Call off the main thread.
 */
fun resolveFocusedDisplayId(): Int? {
    val command =
        "dumpsys window windows | grep -m1 mTopFocusedDisplayId | sed -E 's/.*mTopFocusedDisplayId=([0-9]+).*/\\1/'"
    return PrivilegedShell.execute(command).getOrNull()?.trim()?.toIntOrNull()
}
