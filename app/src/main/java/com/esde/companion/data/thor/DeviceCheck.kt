package com.esde.companion.data.thor

import android.os.Build

private const val AYN_THOR_MODEL = "AYN Thor"

/** Ported from Asgard's `isAynThor()` - gates every Thor Settings feature/UI entry point. */
fun isAynThorDevice(): Boolean = Build.MODEL.equals(AYN_THOR_MODEL, ignoreCase = true)
