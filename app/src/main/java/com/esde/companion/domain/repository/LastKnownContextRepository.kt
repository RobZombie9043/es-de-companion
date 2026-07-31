package com.esde.companion.domain.repository

import com.esde.companion.domain.model.GameReference
import kotlinx.coroutines.flow.Flow

/**
 * Tracks the most recently browsed system and game, independent of whatever ES-DE is
 * doing right now - used to give the widget edit-mode preview something real to show
 * even when the live AppState has nothing (Idle) or when editing a canvas that isn't
 * currently active (e.g. editing System while a game happens to be running).
 */
interface LastKnownContextRepository {
    fun observeLastSystemShortName(): Flow<String?>
    suspend fun setLastSystemShortName(systemShortName: String)
    fun observeLastGameReference(): Flow<GameReference?>
    suspend fun setLastGameReference(gameReference: GameReference)
}
