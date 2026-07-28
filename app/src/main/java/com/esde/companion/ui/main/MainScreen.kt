package com.esde.companion.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.domain.model.EsdeConnectionState
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
    onOpenSettings: () -> Unit,
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val coverImageStatus by viewModel.coverImageStatus.collectAsStateWithLifecycle()
    val overlayEnabled by viewModel.overlayEnabled.collectAsStateWithLifecycle()
    val mainScreenImageState by viewModel.mainScreenImageState.collectAsStateWithLifecycle()
    MainScreenContent(
        connectionState = connectionState,
        coverImageStatus = coverImageStatus,
        overlayEnabled = overlayEnabled,
        mainScreenImageState = mainScreenImageState,
        appDrawerViewModel = appDrawerViewModel,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    connectionState: EsdeConnectionState,
    coverImageStatus: CoverImageStatus?,
    overlayEnabled: Boolean,
    mainScreenImageState: MainScreenImageState,
    appDrawerViewModel: AppDrawerViewModel,
    onOpenSettings: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val drawerHeightPx = with(density) { maxHeight.toPx() }
        val coroutineScope = rememberCoroutineScope()

        // 0f = fully closed, 1f = fully open. Tracked as a fraction rather than raw
        // pixels so it stays meaningful across recomposition even if maxHeight changes
        // (e.g. rotation), unlike a remembered pixel offset which would go stale.
        val openFraction = remember { Animatable(0f) }

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
                // reliable way to close. Revisit if a real device with a tall app list
                // makes the grid conflict a problem.
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
                },
        ) {
            MainScreenImages(state = mainScreenImageState, modifier = Modifier.fillMaxSize())

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {},
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        actions = {
                            IconButton(onClick = onOpenSettings) {
                                Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                AnimatedVisibility(
                    visible = overlayEnabled,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        StateOverlay(
                            connectionState = connectionState,
                            coverImageStatus = coverImageStatus,
                            modifier = Modifier.align(Alignment.TopStart),
                        )
                    }
                }
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
                    onAppLaunched = { settle(0f, positionThreshold = 0f) },
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