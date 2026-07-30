package com.esde.companion.ui.widgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeConnectionState
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
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import com.esde.companion.domain.usecase.ResolveRandomSystemMediaUseCase
import com.esde.companion.ui.main.systemLogoAssetName
import com.esde.companion.ui.widgets.FALLBACK_BACKGROUND_ASSET
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
) : ViewModel() {

    private val gridDimensions = MutableStateFlow<GridDimensions?>(null)

    fun setGridDimensions(grid: GridDimensions) {
        gridDimensions.value = grid
    }

    private val groupedAppState: Flow<Pair<StateGroup, AppState>?> = observeConnectionState()
        .map { connection ->
            val appState = (connection as? EsdeConnectionState.Connected)?.appState ?: return@map null
            appState.stateGroup()?.let { group -> group to appState }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val canvasState: StateFlow<WidgetCanvasState> =
        combine(groupedAppState, gridDimensions.filterNotNull()) { grouped, grid -> grouped to grid }
            .flatMapLatest { (grouped, grid) ->
                if (grouped == null) {
                    flowOf(WidgetCanvasState.None)
                } else {
                    val (stateGroup, appState) = grouped
                    observeWidgetCanvas(stateGroup, grid).map { widgets ->
                        WidgetCanvasState.Showing(widgets, resolveContent(widgets, appState))
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
    private suspend fun resolveContent(widgets: List<PlacedWidget>, appState: AppState): Map<String, WidgetContent> {
        val gameRef = appState.currentGameReference()
        val gameMedia = gameRef?.let { resolveGameMedia(it.systemShortName, it.romPath) }
        val systemShortName = (appState as? AppState.BrowsingSystem)?.systemShortName

        val neededSystemMediaTypes = widgets
            .mapNotNull { (it.widgetType as? WidgetType.SystemMedia)?.mediaType }
            .distinct()
        val systemMediaByType: Map<MediaType, String?> = systemShortName?.let { shortName ->
            neededSystemMediaTypes.associateWith { mediaType -> resolveRandomSystemMedia(shortName, mediaType) }
        } ?: emptyMap()

        val systemLogoAssetPath = systemShortName
            ?.let { "file:///android_asset/system_logos/${systemLogoAssetName(it)}.svg" }

        return widgets.associate { widget ->
            widget.id to WidgetContentResolver.resolve(
                widgetType = widget.widgetType,
                systemLogoAssetPath = { systemLogoAssetPath },
                systemMediaLookup = { mediaType -> systemMediaByType[mediaType] },
                gameMediaLookup = { mediaType -> gameMedia?.path(mediaType) },
                fallbackBackgroundAssetPath = FALLBACK_BACKGROUND_ASSET,
            )
        }
    }
}