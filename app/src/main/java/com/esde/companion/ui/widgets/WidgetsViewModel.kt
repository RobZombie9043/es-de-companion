package com.esde.companion.ui.widgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.NavigationDirection
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.domain.model.WidgetContentResolver
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.model.currentGameReference
import com.esde.companion.domain.model.navigationDirection
import com.esde.companion.domain.model.stateGroup
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.ResolveBundledSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemImageUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveGameDescriptionUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import com.esde.companion.domain.usecase.ResolveGameRatingUseCase
import com.esde.companion.domain.usecase.ResolveRandomSystemMediaUseCase
import com.esde.companion.ui.main.systemLogoAssetName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Resolves the live widget canvas: which StateGroup applies to the current AppState (if
 * any - Idle has none, see [stateGroup]), what's saved for that canvas, and what each
 * placed widget should currently display. [setGridDimensions] must be called once the
 * host composable has measured real screen space - nothing is shown before that, since
 * grid-relative positions are meaningless without it.
 */
class WidgetsViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    private val observeWidgetCanvas: ObserveWidgetCanvasUseCase,
    private val resolveGameMedia: ResolveGameMediaUseCase,
    private val resolveRandomSystemMedia: ResolveRandomSystemMediaUseCase,
    private val resolveGameDescription: ResolveGameDescriptionUseCase,
    private val resolveGameRating: ResolveGameRatingUseCase,
    private val resolveCustomSystemImage: ResolveCustomSystemImageUseCase,
    private val resolveCustomSystemLogo: ResolveCustomSystemLogoUseCase,
    private val resolveBundledSystemLogo: ResolveBundledSystemLogoUseCase,
) : ViewModel() {
    /** Caches the current system's random picks (per media type), reused as long as
     * [randomSystemMediaCacheSystem] still matches the system being resolved for - so a
     * flatMapLatest restart that doesn't correspond to an actual system change (e.g.
     * canvasState's WhileSubscribed(5_000) dropping and resubscribing the whole upstream
     * chain across a device sleep longer than that timeout) doesn't reroll
     * ResolveRandomSystemMediaUseCase's random pick for a system that's still the one
     * being displayed. The cache is cleared and rerolled the moment a *different* system
     * is resolved for - including returning to a system already visited earlier in the
     * session - so "system A -> system B -> system A" intentionally shows fresh art for
     * A the second time, while repeated resolution without ever leaving A does not. */
    private var randomSystemMediaCacheSystem: String? = null
    private val randomSystemMediaCache = mutableMapOf<MediaType, String?>()

    private suspend fun resolveRandomSystemMediaCached(
        systemShortName: String,
        mediaType: MediaType,
    ): String? {
        if (randomSystemMediaCacheSystem != systemShortName) {
            randomSystemMediaCache.clear()
            randomSystemMediaCacheSystem = systemShortName
        }
        return randomSystemMediaCache.getOrPut(mediaType) { resolveRandomSystemMedia(systemShortName, mediaType) }
    }

    private val gridDimensions = MutableStateFlow<GridDimensions?>(null)

    fun setGridDimensions(grid: GridDimensions) {
        gridDimensions.value = grid
    }

    // Distilled from raw AppState down to just the identity that actually matters for
    // widget content - same reasoning as MainViewModel's ImageSource. Without
    // distinctUntilChanged() here, any AppState field change irrelevant to widget content
    // (or the documented spurious game-select re-fire after game-start) still retriggers
    // flatMapLatest below, cancelling an in-flight resolution/decode and restarting the
    // whole chain - under a burst of same-target events that never converges quickly.
    private val contentIdentity: Flow<ContentIdentity?> =
        observeConnectionState()
            .map { connection ->
                val appState = (connection as? EsdeConnectionState.Connected)?.appState ?: return@map null
                val group = appState.stateGroup() ?: return@map null
                ContentIdentity(
                    stateGroup = group,
                    gameRef = appState.currentGameReference(),
                    isBrowsingGame = appState is AppState.BrowsingGame,
                    // System Logo/System Image widgets are offered on both canvases (see
                    // EditWidgetsOverlay's widgetCatalogFor), so this must resolve the
                    // system currently in play/browse-game context too, not just
                    // BrowsingSystem - otherwise those widgets would silently never
                    // render on the Playing canvas.
                    systemShortName =
                        when (appState) {
                            is AppState.BrowsingSystem -> appState.systemShortName
                            is AppState.BrowsingGame -> appState.systemShortName
                            is AppState.PlayingGame -> appState.systemShortName
                            is AppState.Screensaver -> appState.currentGame?.systemShortName
                            is AppState.Idle -> null
                        },
                    systemFullName =
                        when (appState) {
                            is AppState.BrowsingSystem -> appState.systemFullName
                            is AppState.BrowsingGame -> appState.systemFullName
                            is AppState.PlayingGame -> appState.systemFullName
                            is AppState.Screensaver -> appState.currentGame?.systemFullName
                            is AppState.Idle -> null
                        },
                    gameName =
                        when (appState) {
                            is AppState.BrowsingGame -> appState.gameName
                            is AppState.PlayingGame -> appState.gameName
                            is AppState.Screensaver -> appState.currentGame?.gameName
                            is AppState.Idle, is AppState.BrowsingSystem -> null
                        },
                    navigationDirection = appState.navigationDirection(),
                )
            }
            .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val canvasState: StateFlow<WidgetCanvasState> =
        combine(contentIdentity, gridDimensions.filterNotNull()) { identity, grid -> identity to grid }
            .flatMapLatest { (identity, grid) ->
                if (identity == null) {
                    flowOf(WidgetCanvasState.Disconnected)
                } else {
                    observeWidgetCanvas(identity.stateGroup, grid).map { widgets ->
                        WidgetCanvasState.Showing(
                            widgets,
                            resolveContent(widgets, identity),
                            identity.navigationDirection,
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = WidgetCanvasState.Unmeasured,
            )

    /**
     * Pre-resolves every lookup a widget on this canvas could need, once, before building
     * the per-widget content map - keeps WidgetContentResolver's lookup lambdas cheap and
     * synchronous (see its kdoc), rather than each widget independently triggering a
     * suspend media lookup.
     */
    private suspend fun resolveContent(
        widgets: List<PlacedWidget>,
        identity: ContentIdentity,
    ): Map<String, WidgetContent> {
        // Includes each widget's own fallbackMediaType, not just its primary mediaType -
        // otherwise a lone FanArt widget's Screenshots fallback would only ever resolve
        // by coincidence, when some other widget on the same canvas also happens to need
        // Screenshots. See WidgetContentResolver/resolveMediaWidgetContent's kdoc.
        val hasVideoWidget = widgets.any { it.widgetType is WidgetType.Video }
        val neededGameMediaTypes =
            widgets.mapNotNull { it.widgetType as? WidgetType.GameMedia }
                .flatMap { listOfNotNull(it.mediaType, it.fallbackMediaType) }
                .toSet() + if (hasVideoWidget) setOf(MediaType.Videos) else emptySet()
        val gameMedia =
            identity.gameRef?.let {
                resolveGameMedia(
                    systemShortName = it.systemShortName,
                    systemPath = it.systemPath,
                    romPath = it.romPath,
                    mediaTypes = neededGameMediaTypes,
                )
            }
        val gameDescription = identity.gameRef?.let { resolveGameDescription(it.systemShortName, it.romPath) }
        val gameRating = identity.gameRef?.let { resolveGameRating(it.systemShortName, it.romPath) }
        val systemShortName = identity.systemShortName

        val hasSystemImageWidget = widgets.any { it.widgetType is WidgetType.SystemImage }
        val neededSystemMediaTypes =
            (
                widgets.mapNotNull { it.widgetType as? WidgetType.SystemMedia }
                    .flatMap { listOfNotNull(it.mediaType, it.fallbackMediaType) } +
                    if (hasSystemImageWidget) listOf(MediaType.FanArt, MediaType.Screenshots) else emptyList()
            ).distinct()
        val systemMediaByType: Map<MediaType, String?> =
            systemShortName?.let { shortName ->
                neededSystemMediaTypes.associateWith { mediaType ->
                    resolveRandomSystemMediaCached(shortName, mediaType)
                }
            } ?: emptyMap()

        val systemLogoAssetPath = systemShortName?.let { resolveBundledSystemLogo(systemLogoAssetName(it)) }

        val needsCustomLogo = widgets.any { it.widgetType is WidgetType.SystemLogo }
        val needsCustomImage = widgets.any { it.widgetType is WidgetType.SystemImage }
        val customSystemLogoPath =
            if (needsCustomLogo) {
                systemShortName?.let {
                    resolveCustomSystemLogo(
                        systemLogoAssetName(it),
                    )
                }
            } else {
                null
            }
        val customSystemImagePath =
            if (needsCustomImage) {
                systemShortName?.let {
                    resolveCustomSystemImage(
                        systemLogoAssetName(it),
                    )
                }
            } else {
                null
            }

        return widgets.associate { widget ->
            widget.id to
                WidgetContentResolver.resolve(
                    widgetType = widget.widgetType,
                    systemLogoAssetPath = { systemLogoAssetPath },
                    customSystemLogoLookup = { customSystemLogoPath },
                    customSystemImageLookup = { customSystemImagePath },
                    systemMediaLookup = { mediaType -> systemMediaByType[mediaType] },
                    gameMediaLookup = { mediaType -> gameMedia?.path(mediaType) },
                    gameDescriptionLookup = { gameDescription?.text },
                    gameRatingLookup = { gameRating?.value },
                    // null in EditWidgetsViewModel, as today
                    fallbackBackgroundAssetPath = FALLBACK_BACKGROUND_ASSET,
                    systemNameLookup = { identity.systemFullName },
                    gameNameLookup = { identity.gameName },
                    videoLookup = { gameMedia?.path(MediaType.Videos).takeIf { identity.isBrowsingGame } },
                )
        }
    }

    private data class ContentIdentity(
        val stateGroup: StateGroup,
        val gameRef: GameReference?,
        val isBrowsingGame: Boolean,
        val systemShortName: String?,
        val systemFullName: String?,
        val gameName: String?,
        val navigationDirection: NavigationDirection?,
    )
}
