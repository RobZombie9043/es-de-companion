package com.esde.companion.domain.model

/**
 * Which app(s) Task Killer force-stops on a BACK hold, resolved by
 * `TaskKillerCoordinator`. [FocusApp] is the original/default behavior - whichever app
 * currently has hardware-key focus, on whichever display that is. [ThisScreen]/[OtherScreen]
 * resolve relative to `CompanionDisplayHolder.displayId`, same as [GameLaunchDisplayTarget] -
 * not reused here directly since this setting needs two more cases ([FocusApp]/[Both]) that
 * enum has no concept of. [Both] acts on both displays independently.
 */
enum class TaskKillerTarget {
    FocusApp,
    ThisScreen,
    OtherScreen,
    Both,
}
