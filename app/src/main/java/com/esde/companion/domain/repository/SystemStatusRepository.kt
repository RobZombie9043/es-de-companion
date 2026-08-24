package com.esde.companion.domain.repository

import com.esde.companion.domain.model.SystemStatus
import kotlinx.coroutines.flow.Flow

/** Backs the SystemStatus/ClockAndSystemStatus FABs - see AndroidSystemStatusRepository. */
interface SystemStatusRepository {
    fun observeSystemStatus(): Flow<SystemStatus>
}
