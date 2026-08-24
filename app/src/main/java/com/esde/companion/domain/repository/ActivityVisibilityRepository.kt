package com.esde.companion.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Whether MainActivity is actually visible on screen right now - not window focus.
 * A game launched via SecondaryDisplayResolver genuinely covers this Activity on its
 * display; onStart/onStop are the lifecycle-contract-guaranteed signal for that, unlike
 * onWindowFocusChanged (which has proven unreliable for this exact "something else is
 * now on top" case in the past). This is the seam the Video widget uses to
 * make sure video/audio never plays while something else is actually
 * on top, regardless of what AppState currently claims.
 */
interface ActivityVisibilityRepository {
    fun observeIsVisible(): Flow<Boolean>

    fun setVisible(visible: Boolean)
}
