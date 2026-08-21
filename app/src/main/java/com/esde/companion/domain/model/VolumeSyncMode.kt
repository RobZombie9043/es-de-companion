package com.esde.companion.domain.model

/**
 * How Thor Settings > Volume Sync reacts to the volume keys. [Linked] moves the top screen and
 * keeps the bottom screen at the same relative level (the default). [FollowFocus] instead
 * controls whichever screen currently holds hardware-key focus.
 */
enum class VolumeSyncMode { Linked, FollowFocus }

/** Which physical screen a volume-key press should adjust - only meaningful in
 * [VolumeSyncMode.FollowFocus]; [VolumeSyncMode.Linked] always adjusts both. */
enum class VolumeSyncTarget { Main, Secondary }
