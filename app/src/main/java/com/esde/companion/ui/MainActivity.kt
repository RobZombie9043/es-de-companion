package com.esde.companion.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.esde.companion.CompanionApplication
import com.esde.companion.ui.drawer.AppDrawerViewModel
import com.esde.companion.ui.drawer.AppDrawerViewModelFactory
import com.esde.companion.ui.main.MainScreen
import com.esde.companion.ui.main.MainScreenImages
import com.esde.companion.ui.main.MainViewModel
import com.esde.companion.ui.main.MainViewModelFactory
import com.esde.companion.ui.onboarding.OnboardingScreen
import com.esde.companion.ui.onboarding.OnboardingViewModel
import com.esde.companion.ui.onboarding.OnboardingViewModelFactory
import com.esde.companion.ui.settings.SettingsScreen
import com.esde.companion.ui.settings.SettingsViewModel
import com.esde.companion.ui.settings.SettingsViewModelFactory
import com.esde.companion.ui.theme.EsdeCompanionTheme
import kotlinx.coroutines.flow.first

private object Destinations {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as CompanionApplication).appContainer

        setContent {
            EsdeCompanionTheme {
                // Read once, not continuously observed - NavHost's start destination is
                // fixed at first composition. Onboarding finishing later is handled by
                // an explicit navController.navigate call, not by this value changing
                // underneath the NavHost.
                val onboardingComplete by produceState<Boolean?>(initialValue = null) {
                    value = appContainer.observeOnboardingCompleteUseCase().first()
                }

                onboardingComplete?.let { complete ->
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = if (complete) Destinations.MAIN else Destinations.ONBOARDING,
                    ) {
                        composable(Destinations.ONBOARDING) {
                            val viewModel: OnboardingViewModel = viewModel(
                                factory = OnboardingViewModelFactory(appContainer),
                            )
                            OnboardingScreen(
                                viewModel = viewModel,
                                onOnboardingComplete = {
                                    navController.navigate(Destinations.MAIN) {
                                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(Destinations.MAIN) {
                            val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(appContainer))
                            val appDrawerViewModel: AppDrawerViewModel = viewModel(factory = AppDrawerViewModelFactory(appContainer))
                            var showSettings by rememberSaveable { mutableStateOf(false) }

                            // Collected here, above the settings toggle, so this call site -
                            // and the CrossfadeAsyncImage state inside MainScreenImages -
                            // never leaves composition just because Settings is showing.
                            // Otherwise WhileSubscribed(5_000) on the ViewModel's flow would
                            // stop and restart on a long-enough settings visit, causing a
                            // visible reload/flash on return.
                            val mainScreenImageState by viewModel.mainScreenImageState.collectAsStateWithLifecycle()

                            Box(modifier = Modifier.fillMaxSize()) {
                                MainScreenImages(state = mainScreenImageState, modifier = Modifier.fillMaxSize())

                                if (showSettings) {
                                    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(appContainer))
                                    SettingsScreen(
                                        viewModel = settingsViewModel,
                                        onDone = { showSettings = false },
                                    )
                                } else {
                                    MainScreen(
                                        viewModel = viewModel,
                                        appDrawerViewModel = appDrawerViewModel,
                                        onOpenSettings = { showSettings = true },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}