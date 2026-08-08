package com.esde.companion.ui.manual

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.data.pdf.PdfManualRenderer
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.currentGameReference
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the GameManual screen behavior (Settings > UI Settings > Game Playing Behavior).
 * Resolves the current game's manual PDF path via ResolveGameMediaUseCase (MediaType.
 * Manuals), then owns paging through it with PdfManualRenderer.
 *
 * [pdfPath] being null - no manual found for this game - is what MainActivity checks to
 * decide whether the GameManual cover applies at all; it falls through to the plain main
 * screen in that case, same as ScreenBehavior.Nothing.
 */
class GameManualViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    private val resolveGameMedia: ResolveGameMediaUseCase,
) : ViewModel() {
    private val currentGameReference: Flow<GameReference?> =
        observeConnectionState()
            .map { connection -> (connection as? EsdeConnectionState.Connected)?.appState?.currentGameReference() }
            .distinctUntilChanged()

    val pdfPath: StateFlow<String?> =
        currentGameReference
            .map { gameRef -> gameRef?.let { resolveGameMedia(it.systemShortName, it.romPath).path(MediaType.Manuals) } }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = null,
            )

    private var renderer: PdfManualRenderer? = null
    private var openedForPath: String? = null
    private var targetWidthPx: Int = 0

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount

    private val _currentBitmap = MutableStateFlow<ImageBitmap?>(null)
    val currentBitmap: StateFlow<ImageBitmap?> = _currentBitmap

    private val _nextBitmap = MutableStateFlow<ImageBitmap?>(null)
    val nextBitmap: StateFlow<ImageBitmap?> = _nextBitmap

    init {
        viewModelScope.launch {
            pdfPath.collect { path -> onPathChanged(path) }
        }
    }

    /**
     * Re-opens the renderer whenever the resolved manual changes (new game, or the
     * manual disappearing) - closes the previous handle first so file descriptors never
     * pile up across games.
     */
    private suspend fun onPathChanged(path: String?) {
        if (path == openedForPath) return
        renderer?.close()
        renderer = null
        openedForPath = path
        _currentPage.value = 0
        _currentBitmap.value = null
        _nextBitmap.value = null
        _pageCount.value = 0

        val opened = path?.let { PdfManualRenderer.open(it) } ?: return
        renderer = opened
        _pageCount.value = opened.pageCount
        renderCurrentAndPrefetch()
    }

    /** Called once by GameManualScreen with the real measured width, before any render. */
    fun setTargetWidth(widthPx: Int) {
        if (widthPx <= 0 || widthPx == targetWidthPx) return
        targetWidthPx = widthPx
        viewModelScope.launch { renderCurrentAndPrefetch() }
    }

    fun nextPage() = goToPage(_currentPage.value + 1)

    fun previousPage() = goToPage(_currentPage.value - 1)

    private fun goToPage(index: Int) {
        val count = _pageCount.value
        if (count == 0 || index !in 0 until count || index == _currentPage.value) return
        _currentPage.value = index
        viewModelScope.launch { renderCurrentAndPrefetch() }
    }

    private suspend fun renderCurrentAndPrefetch() {
        val renderer = renderer ?: return
        if (targetWidthPx <= 0) return
        val page = _currentPage.value
        _currentBitmap.value = renderer.renderPage(page, targetWidthPx)
        _nextBitmap.value = if (page + 1 < renderer.pageCount) renderer.renderPage(page + 1, targetWidthPx) else null
    }

    override fun onCleared() {
        renderer?.close()
        renderer = null
    }
}
