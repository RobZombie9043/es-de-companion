package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.repository.LastKnownContextRepository
import kotlinx.coroutines.flow.Flow

class ObserveLastSystemShortNameUseCase(
    private val repository: LastKnownContextRepository,
) {
    operator fun invoke(): Flow<String?> = repository.observeLastSystemShortName()
}

class SetLastSystemShortNameUseCase(
    private val repository: LastKnownContextRepository,
) {
    suspend operator fun invoke(systemShortName: String) = repository.setLastSystemShortName(systemShortName)
}

class ObserveLastGameReferenceUseCase(
    private val repository: LastKnownContextRepository,
) {
    operator fun invoke(): Flow<GameReference?> = repository.observeLastGameReference()
}

class SetLastGameReferenceUseCase(
    private val repository: LastKnownContextRepository,
) {
    suspend operator fun invoke(gameReference: GameReference) = repository.setLastGameReference(gameReference)
}
