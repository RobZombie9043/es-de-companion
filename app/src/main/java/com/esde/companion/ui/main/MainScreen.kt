package com.esde.companion.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.ui.CORNER_BUTTON_EDGE_PADDING
import com.esde.companion.ui.CORNER_BUTTON_SIZE
import com.esde.companion.ui.dock.AppDock
import com.esde.companion.ui.dock.AppDockViewModel
import com.esde.companion.ui.dock.dockBarHeight
import com.esde.companion.ui.drawer.AppDrawer
import com.esde.companion.ui.drawer.AppDrawerHandle
import com.esde.companion.ui.drawer.AppDrawerViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Position-based fallback threshold for the whole-screen (opening) gesture - used only
 * when the release wasn't a decisive fling (see FLING_VELOCITY_THRESHOLD). */
private const val DRAWER_OPEN_SNAP_THRESHOLD = 0.35f

/** Lower position-based fallback for the handle's own drag zone - the handle exists to
 * be the reliable "close" gesture, so it shouldn't require dragging as far down as the
 * whole-screen gesture does before it commits to closing. */
private const val DRAWER_HANDLE_CLOSE_SNAP_THRESHOLD = 0.65f

/** Release speed, in screen-heights-per-second, above which a drag is treated as a
 * decisive fling: it wins the open/close decision outright regardless of the position
 * thresholds above, so a quick flick closes the drawer even from near the top. */
private const val FLING_VELOCITY_THRESHOLD = 1.0f

private val DRAWER_SETTLE_SPRING = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMedium,
)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    appDrawerViewModel: AppDrawerViewModel,
    dockViewModel: AppDockViewModel,
    widgetsLocked: Boolean,
    onOpenSettings: () -> Unit,
    onOpenEditWidgets: () -> Unit,
    onToggleBlankScreen: () -> Unit,
    onDrawerOpenChanged: (Boolean) -> Unit,
) {
    MainScreenContent(
        appDrawerViewModel = appDrawerViewModel,
        dockViewModel = dockViewModel,
        widgetsLocked = widgetsLocked,
        onOpenSettings = onOpenSettings,
        onOpenEditWidgets = onOpenEditWidgets,
        onToggleBlankScreen = onToggleBlankScreen,
        onDrawerOpenChanged = onDrawerOpenChanged,
    )
}

// mainScreenImageState (and the MainScreenImages composable it feeds) is now collected
// and rendered in MainActivity, above the MainScreen/SettingsScreen toggle, so the
// backdrop and its CrossfadeAsyncImage never leave composition just because Settings
// is showing. See MainActivity's MAIN destination.
//
// The double-tap-to-blank-screen overlay itself is ALSO rendered in MainActivity, not
// here - see MainActivity's MAIN destination for why (it needs to sit above the widget
// layer, which is a sibling of this whole screen, not a descendant of it). This
// composable only owns the gesture that toggles it, via onToggleBlankScreen.
@Composable
private fun MainScreenContent(
    appDrawerViewModel: AppDrawerViewModel,
    dockViewModel: AppDockViewModel,
    widgetsLocked: Boolean,
    onOpenSettings: () -> Unit,
    onOpenEditWidgets: () -> Unit,
    onToggleBlankScreen: () -> Unit,
    onDrawerOpenChanged: (Boolean) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val drawerHeightPx = with(density) { maxHeight.toPx() }
        val coroutineScope = rememberCoroutineScope()
        val dockSize by dockViewModel.dockSize.collectAsStateWithLifecycle()

        // 0f = fully closed, 1f = fully open. Tracked as a fraction rather than raw
        // pixels so it stays meaningful across recomposition even if maxHeight changes
        // (e.g. rotation), unlike a remembered pixel offset which would go stale.
        val openFraction = remember { Animatable(0f) }

        // Reported up to MainActivity so the automatic Dim/Black cover (Settings > UI
        // Settings) can suppress itself while the drawer is open - it should only ever
        // affect the plain main screen, not the drawer, Settings, or Edit Widgets.
        val drawerOpen by remember { derivedStateOf { openFraction.value > 0f } }
        LaunchedEffect(drawerOpen) { onDrawerOpenChanged(drawerOpen) }

        // System/hardware back must never exit this app - it's meant to run continuously on
        // the second display. If the App Drawer is open, back closes it first; otherwise the
        // event is simply consumed and does nothing.
        BackHandler(enabled = true) {
            if (openFraction.value > 0f) {
                coroutineScope.launch {
                    openFraction.animateTo(
                        targetValue = 0f,
                        animationSpec = DRAWER_SETTLE_SPRING,
                    )
                }
            }
        }

        // velocityFraction: positive = moving toward open (upward), negative = moving
        // toward closed (downward) - same sign convention as openFraction itself, so a
        // decisive value can be compared directly against FLING_VELOCITY_THRESHOLD.
        fun settle(velocityFraction: Float, positionThreshold: Float) {
            val towardOpen = when {
                velocityFraction > FLING_VELOCITY_THRESHOLD -> true
                velocityFraction < -FLING_VELOCITY_THRESHOLD -> false
                else -> openFraction.value > positionThreshold
            }
            coroutineScope.launch {
                openFraction.animateTo(
                    targetValue = if (towardOpen) 1f else 0f,
                    animationSpec = DRAWER_SETTLE_SPRING,
                    initialVelocity = velocityFraction,
                )
            }
        }

        // Unconditional close - used after an app launch, where there's no ambiguity to
        // resolve from velocity/position (unlike a drag release). settle()'s threshold logic
        // isn't a reliable "force closed" and shouldn't be reused for this.
        fun closeDrawer() {
            coroutineScope.launch {
                openFraction.animateTo(targetValue = 0f, animationSpec = DRAWER_SETTLE_SPRING)
            }
        }

        fun onVerticalDrag(dragAmount: Float) {
            val deltaFraction = -dragAmount / drawerHeightPx
            val newValue = (openFraction.value + deltaFraction).coerceIn(0f, 1f)
            coroutineScope.launch { openFraction.snapTo(newValue) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Deliberately on the outermost Box, not scoped to any particular child,
                // so the drawer can be opened by a swipe starting anywhere on screen.
                // Known limitation: once open, this and the app grid's own scrolling both
                // listen for vertical drags over the grid area - not resolved via
                // Compose's nested-scroll protocol here. The handle below remains the
                // reliable way to close.
                .pointerInput(drawerHeightPx) {
                    var velocityTracker = VelocityTracker()
                    detectVerticalDragGestures(
                        onDragStart = { velocityTracker = VelocityTracker() },
                        onDragEnd = {
                            val velocityFraction = -velocityTracker.calculateVelocity().y / drawerHeightPx
                            settle(velocityFraction, DRAWER_OPEN_SNAP_THRESHOLD)
                        },
                        onDragCancel = { settle(0f, DRAWER_OPEN_SNAP_THRESHOLD) },
                    ) { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPointerInputChange(change)
                        onVerticalDrag(dragAmount)
                    }
                }
                // Long-press-to-edit-widgets and double-tap-to-blank, layered alongside
                // the drag gesture above rather than replacing it. These coexist safely
                // because of how Compose's gesture consumption works:
                // detectVerticalDragGestures only consumes once real movement exceeds
                // touch slop, so a stationary hold/tap never gets consumed and this
                // detector's gestures fire normally; a genuine swipe does get consumed
                // by the drag detector, which cancels this detector's recognition. So a
                // still finger reaches edit mode/blank-toggle and a moving finger opens
                // the drawer, without any manual disambiguation logic - but this is
                // exactly the kind of gesture composition worth confirming feels right
                // on a real device, not just reasoning about in code.
                // Double-tap-to-blank is always available now (Settings > UI Settings
                // no longer gates it - see MainActivity for the automatic Dim/Black
                // behavior that now lives there instead).
                .pointerInput(widgetsLocked) {
                    detectTapGestures(
                        onLongPress = {
                            if (!widgetsLocked && openFraction.value == 0f) {
                                onOpenEditWidgets()
                            }
                        },
                        onDoubleTap = {
                            if (openFraction.value == 0f) {
                                onToggleBlankScreen()
                            }
                        },
                    )
                },
        ) {
            // Sized/positioned from the same CornerButtonMetrics constants as
            // MainActivity's music FAB (opposite corner) so the two stay vertically
            // aligned by construction - see CornerButtonMetrics' kdoc. Previously lived in
            // a Material3 TopAppBar purely to get a corner button; that added a whole
            // Scaffold+TopAppBar (with an otherwise-unused empty content slot) just to
            // reach a vertical centering that never actually matched the FAB's.
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(CORNER_BUTTON_EDGE_PADDING)
                    .size(CORNER_BUTTON_SIZE),
            ) {
                Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
            }

            // Slides up from below the bottom edge as openFraction goes 0 -> 1. At
            // openFraction = 0 this sits entirely below the visible screen (offset =
            // full height), so it doesn't intercept touches meant for the content above
            // even though no explicit visibility/clipping gate is applied.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        val clampedFraction = openFraction.value.coerceIn(0f, 1f)
                        IntOffset(x = 0, y = ((1f - clampedFraction) * drawerHeightPx).roundToInt())
                    },
            ) {
                AppDrawer(
                    viewModel = appDrawerViewModel,
                    onAppLaunched = { closeDrawer() },
                )

                // Anchored to the top edge of this same offset Box (which is the App
                // Drawer sheet's own top edge) rather than animated independently: at
                // openFraction = 0 this Box's top sits at the bottom of the screen, so
                // the dock (offset further up by its own height) rests pinned at the
                // screen's bottom edge; at openFraction = 1 this Box's top sits at
                // y = 0, pushing the dock fully off-screen above the top. Riding the
                // same offset means no separate animation/fraction math is needed here.
                AppDock(
                    viewModel = dockViewModel,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = -dockBarHeight(dockSize)),
                )

                // A dedicated, always-reliable drag target for closing - doesn't compete
                // with the app grid's own scroll gesture the way the whole-screen
                // detector above can once the drawer is open.
                AppDrawerHandle(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(32.dp)
                        .pointerInput(drawerHeightPx) {
                            var velocityTracker = VelocityTracker()
                            detectVerticalDragGestures(
                                onDragStart = { velocityTracker = VelocityTracker() },
                                onDragEnd = {
                                    val velocityFraction = -velocityTracker.calculateVelocity().y / drawerHeightPx
                                    settle(velocityFraction, DRAWER_HANDLE_CLOSE_SNAP_THRESHOLD)
                                },
                                onDragCancel = { settle(0f, DRAWER_HANDLE_CLOSE_SNAP_THRESHOLD) },
                            ) { change, dragAmount ->
                                change.consume()
                                velocityTracker.addPointerInputChange(change)
                                onVerticalDrag(dragAmount)
                            }
                        },
                )
            }
        }
    }
}