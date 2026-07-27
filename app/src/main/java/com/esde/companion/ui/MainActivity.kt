package com.esde.companion.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.esde.companion.CompanionApplication
import com.esde.companion.ui.main.MainScreen
import com.esde.companion.ui.main.MainViewModel
import com.esde.companion.ui.main.MainViewModelFactory
import com.esde.companion.ui.theme.EsdeCompanionTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as CompanionApplication).appContainer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EsdeCompanionTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
