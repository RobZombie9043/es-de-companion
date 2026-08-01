package com.esde.companion.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class MusicControlsViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MusicControlsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return MusicControlsViewModel(
            coordinator = appContainer.musicPlaybackCoordinator,
        ) as T
    }
}
