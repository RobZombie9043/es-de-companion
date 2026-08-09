package com.esde.companion.domain.model

/**
 * What's assigned to a single corner (see [FabPosition]/[FabAssignments]). [type] alone is
 * enough for every [FabType] except [FabType.CustomApp], which additionally needs
 * [customAppPackageName] to know what to launch - null there means "Custom App selected,
 * but no app chosen yet", which renders as no FAB (see MainActivity's CustomAppFabContent)
 * until the user picks one in Settings > UI Settings > FAB Control.
 */
data class FabSlot(
    val type: FabType,
    val customAppPackageName: String? = null,
)
