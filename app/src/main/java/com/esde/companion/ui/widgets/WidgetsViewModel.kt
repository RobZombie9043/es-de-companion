package com.esde.companion.ui.widgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.domain.model.WidgetContentResolver
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.model.currentGameReference
import com.esde.companion.domain.model.stateGroup
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemImageUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveGameDescriptionUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
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
    private val resolveCustomSystemImage: ResolveCustomSystemImageUseCase,
    private val resolveCustomSystemLogo: ResolveCustomSystemLogoUseCase,
) : ViewModel() {

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
    private val contentIdentity: Flow<ContentIdentity?> = observeConnectionState()
        .map { connection ->
            val appState = (connection as? EsdeConnectionState.Connected)?.appState ?: return@map null
            val group = appState.stateGroup() ?: return@map null
            ContentIdentity(
                stateGroup = group,
                gameRef = appState.currentGameReference(),
                systemShortName = (appState as? AppState.BrowsingSystem)?.systemShortName,
            )
        }
        .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val canvasState: StateFlow<WidgetCanvasState> =
        combine(contentIdentity, gridDimensions.filterNotNull()) { identity, grid -> identity to grid }
            .flatMapLatest { (identity, grid) ->
                if (identity == null) {
                    flowOf(WidgetCanvasState.None)
                } else {
                    observeWidgetCanvas(identity.stateGroup, grid).map { widgets ->
                        WidgetCanvasState.Showing(widgets, resolveContent(widgets, identity))
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = WidgetCanvasState.None,
            )

    /**
     * Pre-resolves every lookup a widget on this canvas could need, once, before building
     * the per-widget content map - keeps WidgetContentResolver's lookup lambdas cheap and
     * synchronous (see its kdoc), rather than each widget independently triggering a
     * suspend media lookup.
     */
    private suspend fun resolveContent(widgets: List<PlacedWidget>, identity: ContentIdentity): Map<String, WidgetContent> {
        val gameMedia = identity.gameRef?.let { resolveGameMedia(it.systemShortName, it.romPath) }
        val gameDescription = identity.gameRef?.let { resolveGameDescription(it.systemShortName, it.romPath) }
        val systemShortName = identity.systemShortName

        val hasSystemImageWidget = widgets.any { it.widgetType is WidgetType.SystemImage }
        val neededSystemMediaTypes = (
                widgets.mapNotNull { (it.widgetType as? WidgetType.SystemMedia)?.mediaType } +
                        if (hasSystemImageWidget) listOf(MediaType.FanArt, MediaType.Screenshots) else emptyList()
                ).distinct()
        val systemMediaByType: Map<MediaType, String?> = systemShortName?.let { shortName ->
            neededSystemMediaTypes.associateWith { mediaType -> resolveRandomSystemMedia(shortName, mediaType) }
        } ?: emptyMap()

        val systemLogoAssetPath = systemShortName
            ?.let { "file:///android_asset/system_logos/${systemLogoAssetName(it)}.svg" }

        val needsCustomLogo = widgets.any { it.widgetType is WidgetType.SystemLogo }
        val needsCustomImage = widgets.any { it.widgetType is WidgetType.SystemImage }
        val customSystemLogoPath = if (needsCustomLogo) systemShortName?.let { resolveCustomSystemLogo(systemLogoAssetName(it)) } else null
        val customSystemImagePath = if (needsCustomImage) systemShortName?.let { resolveCustomSystemImage(systemLogoAssetName(it)) } else null

        return widgets.associate { widget ->
            widget.id to WidgetContentResolver.resolve(
                widgetType = widget.widgetType,
                systemLogoAssetPath = { systemLogoAssetPath },
                customSystemLogoLookup = { customSystemLogoPath },
                customSystemImageLookup = { customSystemImagePath },
                systemMediaLookup = { mediaType -> systemMediaByType[mediaType] },
                gameMediaLookup = { mediaType -> gameMedia?.path(mediaType) },
                gameDescriptionLookup = { gameDescription?.text },
                fallbackBackgroundAssetPath = FALLBACK_BACKGROUND_ASSET, // null in EditWidgetsViewModel, as today
            )
        }
    }

    private data class ContentIdentity(
        val stateGroup: StateGroup,
        val gameRef: GameReference?,
        val systemShortName: String?,
    )
}
