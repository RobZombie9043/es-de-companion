package com.esde.companion.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.ui.CORNER_BUTTON_EDGE_PADDING
import com.esde.companion.ui.CornerFab
import com.esde.companion.ui.dock.AppDock
import com.esde.companion.ui.dock.AppDockViewModel
import com.esde.companion.ui.dock.dockBarHeight
import com.esde.companion.ui.drawer.AppDrawer
import com.esde.companion.ui.drawer.AppDrawerHandle
import com.esde.companion.ui.drawer.AppDrawerViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val LONG_PRESS_MENU_SHAPE = RoundedCornerShape(16.dp)
private val LONG_PRESS_MENU_WIDTH = 280.dp
private val LONG_PRESS_MENU_ITEM_MIN_HEIGHT = 72.dp
private val LONG_PRESS_MENU_ITEM_PADDING = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
private val LONG_PRESS_MENU_ICON_SIZE = 32.dp

// Entrance scale+fade for the long-press menu - starts at 85% size/transparent and
// springs up to full size, same damping/stiffness family as DRAWER_SETTLE_SPRING.
private const val LONG_PRESS_MENU_REVEAL_START_SCALE = 0.85f
private val LONG_PRESS_MENU_REVEAL_SPRING = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium,
)

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
    showSettingsFab: Boolean,
    overlayOpacityPercent: Int,
    onOpenSettings: () -> Unit,
    onOpenEditWidgets: () -> Unit,
    onToggleBlankScreen: () -> Unit,
    onDrawerOpenChanged: (Boolean) -> Unit,
    topStartOverlay: @Composable BoxScope.() -> Unit = {},
) {
    MainScreenContent(
        viewModel = viewModel,
        appDrawerViewModel = appDrawerViewModel,
        dockViewModel = dockViewModel,
        showSettingsFab = showSettingsFab,
        overlayOpacityPercent = overlayOpacityPercent,
        onOpenSettings = onOpenSettings,
        onOpenEditWidgets = onOpenEditWidgets,
        onToggleBlankScreen = onToggleBlankScreen,
        onDrawerOpenChanged = onDrawerOpenChanged,
        topStartOverlay = topStartOverlay,
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
    viewModel: MainViewModel,
    appDrawerViewModel: AppDrawerViewModel,
    dockViewModel: AppDockViewModel,
    showSettingsFab: Boolean,
    overlayOpacityPercent: Int,
    onOpenSettings: () -> Unit,
    onOpenEditWidgets: () -> Unit,
    onToggleBlankScreen: () -> Unit,
    onDrawerOpenChanged: (Boolean) -> Unit,
    topStartOverlay: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val drawerHeightPx = with(density) { maxHeight.toPx() }
        val coroutineScope = rememberCoroutineScope()
        val dockSize by dockViewModel.dockSize.collectAsStateWithLifecycle()
        val hapticFeedback = LocalHapticFeedback.current

        // Master on/off state for the long-press menu's quick toggles below - every
        // other music/video setting still lives in Settings > Background Music /
        // Video Playback, untouched from here.
        val musicEnabled by viewModel.musicEnabled.collectAsStateWithLifecycle()
        val videoPlaybackEnabled by viewModel.videoPlaybackEnabled.collectAsStateWithLifecycle()

        // Long-press popup offering Settings/Edit Widgets - see the Popup below. Local
        // state (not lifted to MainActivity) since nothing outside this screen needs to
        // know about it, same as openFraction.
        var longPressMenuOpen by remember { mutableStateOf(false) }

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
        // the second display. Undoes the most local thing first: the long-press menu (if
        // open), then the App Drawer (if open); otherwise the event is simply consumed and
        // does nothing. The menu and drawer can't both be open at once - opening the menu
        // is itself gated on the drawer being closed, see the long-press handler below.
        BackHandler(enabled = true) {
            when {
                longPressMenuOpen -> longPressMenuOpen = false
                openFraction.value > 0f -> {
                    coroutineScope.launch {
                        openFraction.animateTo(
                            targetValue = 0f,
                            animationSpec = DRAWER_SETTLE_SPRING,
                        )
                    }
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

        // Unconditional open - used by the App Dock's App Drawer shortcut, same
        // reasoning as closeDrawer() above (a deliberate tap, not a drag release, so
        // there's no velocity/position ambiguity for settle() to resolve).
        fun openDrawer() {
            coroutineScope.launch {
                openFraction.animateTo(targetValue = 1f, animationSpec = DRAWER_SETTLE_SPRING)
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
                // Long-press-to-open-menu and double-tap-to-blank, layered alongside the
                // drag gesture above rather than replacing it. These coexist safely
                // because of how Compose's gesture consumption works:
                // detectVerticalDragGestures only consumes once real movement exceeds
                // touch slop, so a stationary hold/tap never gets consumed and this
                // detector's gestures fire normally; a genuine swipe does get consumed
                // by the drag detector, which cancels this detector's recognition. So a
                // still finger reaches the menu/blank-toggle and a moving finger opens
                // the drawer, without any manual disambiguation logic - but this is
                // exactly the kind of gesture composition worth confirming feels right
                // on a real device, not just reasoning about in code.
                // Double-tap-to-blank is always available now (Settings > UI Settings
                // no longer gates it - see MainActivity for the automatic Dim/Black
                // behavior that now lives there instead).
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if (openFraction.value == 0f) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                longPressMenuOpen = true
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
            // CornerFab, not a plain IconButton with a Settings icon - same composable as
            // MainActivity's music FAB (opposite corner) and EditWidgetsOverlay's options
            // button, so all three pick up the exact same theme-driven container/content
            // colors (black-in-dark/white-in-light, matching the App Dock/App Drawer/music
            // panel) for free rather than needing their own color logic to match. Sized/
            // positioned from the same CornerButtonMetrics constants too - see its kdoc -
            // so all three stay vertically aligned by construction. Previously lived in a
            // Material3 TopAppBar purely to get a corner button; that added a whole
            // Scaffold+TopAppBar (with an otherwise-unused empty content slot) just to
            // reach a vertical centering that never actually matched the FAB's. Hidden
            // entirely when showSettingsFab is off (Settings > Other Settings) - Settings
            // stays reachable regardless, via the long-press menu below.
            if (showSettingsFab) {
                CornerFab(
                    onClick = onOpenSettings,
                    opacityPercent = overlayOpacityPercent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(CORNER_BUTTON_EDGE_PADDING),
                ) {
                    Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                }
            }

            // Fixed-center popup opened by the long-press gesture above, offering the
            // two destinations otherwise reached via the (optional) Settings FAB and the
            // Settings > UI Settings > Edit Widgets entry. Built from a plain Popup +
            // Surface, not DropdownMenu, since DropdownMenu positions itself relative to
            // its anchor rather than centering on it - see EditWidgetsOverlay's "options"
            // menu for the anchor-relative version of this same idiom. Items are sized up
            // from DropdownMenuItem's defaults (larger min height/padding/text/icon) since
            // this menu is meant to be comfortably reachable at a glance, not a dense list.
            if (longPressMenuOpen) {
                Popup(
                    alignment = Alignment.Center,
                    onDismissRequest = { longPressMenuOpen = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    // Entrance-only scale+fade, matching the app's existing spring-based
                    // motion (see DRAWER_SETTLE_SPRING) rather than snapping straight to
                    // full size the way a plain Popup does by default. No exit animation -
                    // Popup tears its window down immediately once longPressMenuOpen flips
                    // false, same as EditWidgetsOverlay's dialogs.
                    val revealProgress = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        revealProgress.animateTo(targetValue = 1f, animationSpec = LONG_PRESS_MENU_REVEAL_SPRING)
                    }

                    Surface(
                        modifier = Modifier.graphicsLayer {
                            val scale = LONG_PRESS_MENU_REVEAL_START_SCALE +
                                (1f - LONG_PRESS_MENU_REVEAL_START_SCALE) * revealProgress.value
                            scaleX = scale
                            scaleY = scale
                            alpha = revealProgress.value
                        },
                        shape = LONG_PRESS_MENU_SHAPE,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 3.dp,
                        shadowElevation = 3.dp,
                    ) {
                        Column(modifier = Modifier.width(LONG_PRESS_MENU_WIDTH)) {
                            DropdownMenuItem(
                                modifier = Modifier.heightIn(min = LONG_PRESS_MENU_ITEM_MIN_HEIGHT),
                                contentPadding = LONG_PRESS_MENU_ITEM_PADDING,
                                text = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(LONG_PRESS_MENU_ICON_SIZE),
                                    )
                                },
                                onClick = {
                                    longPressMenuOpen = false
                                    onOpenSettings()
                                },
                            )
                            DropdownMenuItem(
                                modifier = Modifier.heightIn(min = LONG_PRESS_MENU_ITEM_MIN_HEIGHT),
                                contentPadding = LONG_PRESS_MENU_ITEM_PADDING,
                                text = { Text("Edit Widgets", style = MaterialTheme.typography.titleLarge) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(LONG_PRESS_MENU_ICON_SIZE),
                                    )
                                },
                                onClick = {
                                    longPressMenuOpen = false
                                    onOpenEditWidgets()
                                },
                            )
                            HorizontalDivider()
                            // Master on/off quick toggles - Settings > Background Music /
                            // Video Playback own every other knob for each, this is only
                            // ever the top-level enable/disable. Deliberately don't close
                            // the menu on tap (unlike Settings/Edit Widgets above, which
                            // navigate away) - toggling one shouldn't force reopening the
                            // menu to also toggle the other.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                QuickToggleButton(
                                    onIcon = Icons.Filled.MusicNote,
                                    offIcon = Icons.Filled.MusicOff,
                                    enabled = musicEnabled,
                                    onLabel = "Turn off background music",
                                    offLabel = "Turn on background music",
                                    onClick = viewModel::toggleMusicEnabled,
                                )
                                QuickToggleButton(
                                    onIcon = Icons.Filled.Videocam,
                                    offIcon = Icons.Filled.VideocamOff,
                                    enabled = videoPlaybackEnabled,
                                    onLabel = "Turn off video playback",
                                    offLabel = "Turn on video playback",
                                    onClick = viewModel::toggleVideoPlaybackEnabled,
                                )
                            }
                        }
                    }
                }
            }

            // Rendered as a genuine child of this same gesture-handling Box - not a
            // sibling composed elsewhere - for the same reason the Settings CornerFab
            // above works reliably: Compose's Main pointer-input pass runs leaf-to-root,
            // so a descendant's own clickable/pointerInput gets first crack at a touch
            // before this Box's detectVerticalDragGestures/detectTapGestures do. Two
            // sibling subtrees that both cover the same screen area don't get that for
            // free - whichever is composed later simply wins the whole hit test, which
            // is what previously made the music FAB (composed as an external sibling,
            // deliberately placed before the `when` block in MainActivity so it would
            // draw underneath the App Drawer) untappable: MainScreen, composed after it,
            // silently absorbed the touch via this Box's own gesture detectors. Placed
            // here, before the App Drawer's Box below, so the drawer sheet still slides
            // up over it exactly as before.
            topStartOverlay()

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
                    onOpenAppDrawer = { openDrawer() },
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

/**
 * One quick-toggle button in the long-press menu's bottom row. On/off is communicated two
 * ways: which icon is shown ([offIcon] is the standard Material "-Off" variant, with a
 * diagonal slash built into the glyph itself - stronger and more legible at this size than
 * color alone) and its tint (the theme's accent color when [enabled], a muted variant when
 * not). Both colors come from MaterialTheme (not hardcoded black/white), so contrast holds
 * in light and dark color schemes alike.
 */
@Composable
private fun QuickToggleButton(
    onIcon: ImageVector,
    offIcon: ImageVector,
    enabled: Boolean,
    onLabel: String,
    offLabel: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (enabled) onIcon else offIcon,
            contentDescription = if (enabled) onLabel else offLabel,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(LONG_PRESS_MENU_ICON_SIZE),
        )
    }
}