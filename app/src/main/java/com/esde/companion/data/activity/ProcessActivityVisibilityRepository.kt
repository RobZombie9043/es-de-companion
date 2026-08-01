package com.esde.companion.data.activity

import com.esde.companion.domain.repository.ActivityVisibilityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory only - visibility is a live platform fact, not something to persist across
 * process restarts. [setVisible] is called directly from MainActivity's onStart/onStop;
 * this class has no Android Activity/Context dependency itself, keeping it trivially
 * fakeable in tests the same way every other repository here is. A single instance
 * lives in AppContainer for the app's whole lifetime, same as SharedEsdeLogRepository.
 */
class ProcessActivityVisibilityRepository : ActivityVisibilityRepository {

    // Defaults to true: before MainActivity's first onStart/onStop callback lands,
    // assume visible rather than incorrectly suppressing video on cold start.
    private val isVisible = MutableStateFlow(true)

    override fun observeIsVisible(): Flow<Boolean> = isVisible

    override fun setVisible(visible: Boolean) {
        isVisible.value = visible
    }
}