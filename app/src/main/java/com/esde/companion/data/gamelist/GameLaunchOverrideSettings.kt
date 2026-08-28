package com.esde.companion.data.gamelist

import com.esde.companion.domain.usecase.ObserveCloseAppOnGameEndUseCase
import com.esde.companion.domain.usecase.ObserveGameLaunchDisplayTargetUseCase
import com.esde.companion.domain.usecase.ObserveGameLaunchOverridesUseCase
import com.esde.companion.domain.usecase.ObserveGameLaunchSystemDefaultsUseCase

/**
 * Groups [GameLaunchOverrideCoordinator]'s four settings-observing use cases into one
 * parameter, rather than four separate ones, to keep its constructor within this project's
 * LongParameterList limit - same reasoning as `SelfHealConfig`/`BackupRepositories` (see
 * CLAUDE.md).
 */
class GameLaunchOverrideSettings(
    val observeSystemDefaults: ObserveGameLaunchSystemDefaultsUseCase,
    val observeOverrides: ObserveGameLaunchOverridesUseCase,
    val observeDisplayTarget: ObserveGameLaunchDisplayTargetUseCase,
    val observeCloseAppOnGameEnd: ObserveCloseAppOnGameEndUseCase,
)
