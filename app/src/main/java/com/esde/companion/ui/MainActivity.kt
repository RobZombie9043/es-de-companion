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
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.ui.drawer.AppDrawerViewModel
import com.esde.companion.ui.drawer.AppDrawerViewModelFactory
import com.esde.companion.ui.main.MainScreen
import com.esde.companion.ui.main.MainViewModel
import com.esde.companion.ui.main.MainViewModelFactory
import com.esde.companion.ui.onboarding.OnboardingScreen
import com.esde.companion.ui.onboarding.OnboardingViewModel
import com.esde.companion.ui.onboarding.OnboardingViewModelFactory
import com.esde.companion.ui.settings.ManageAppsViewModel
import com.esde.companion.ui.settings.ManageAppsViewModelFactory
import com.esde.companion.ui.settings.SettingsScreen
import com.esde.companion.ui.settings.SettingsViewModel
import com.esde.companion.ui.settings.SettingsViewModelFactory
import com.esde.companion.ui.theme.EsdeCompanionTheme
import com.esde.companion.ui.widgets.WidgetOverlay
import com.esde.companion.ui.widgets.WidgetsViewModel
import com.esde.companion.ui.widgets.WidgetsViewModelFactory
import com.esde.companion.ui.widgets.edit.EditWidgetsOverlay
import com.esde.companion.ui.widgets.edit.EditWidgetsViewModel
import com.esde.companion.ui.widgets.edit.EditWidgetsViewModelFactory
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
            val themePreference by produceState(initialValue = ThemePreference.Auto) {
                appContainer.observeThemePreferenceUseCase().collect { value = it }
            }
            EsdeCompanionTheme(themePreference = themePreference) {
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
                            var showEditWidgets by rememberSaveable { mutableStateOf(false) }

                            val widgetsLocked by produceState(initialValue = false) {
                                appContainer.observeWidgetsLockedUseCase().collect { value = it }
                            }

                            // Collected here, above the settings/edit-widgets toggles, so
                            // this call site - and the WidgetCanvas/CrossfadeAsyncImage
                            // state inside WidgetOverlay - never leaves composition just
                            // because Settings or edit mode is showing. Otherwise
                            // WhileSubscribed(5_000) on the ViewModel's flow would stop and
                            // restart on a long-enough visit, causing a visible
                            // reload/flash on return.
                            val widgetsViewModel: WidgetsViewModel = viewModel(factory = WidgetsViewModelFactory(appContainer))

                            Box(modifier = Modifier.fillMaxSize()) {
                                WidgetOverlay(viewModel = widgetsViewModel, modifier = Modifier.fillMaxSize())

                                when {
                                    showEditWidgets -> {
                                        val editWidgetsViewModel: EditWidgetsViewModel =
                                            viewModel(factory = EditWidgetsViewModelFactory(appContainer))
                                        EditWidgetsOverlay(
                                            viewModel = editWidgetsViewModel,
                                            onDone = { showEditWidgets = false },
                                        )
                                    }

                                    showSettings -> {
                                        val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(appContainer))
                                        val manageAppsViewModel: ManageAppsViewModel = viewModel(factory = ManageAppsViewModelFactory(appContainer))
                                        SettingsScreen(
                                            viewModel = settingsViewModel,
                                            manageAppsViewModel = manageAppsViewModel,
                                            onDone = { showSettings = false },
                                            onEditWidgetsClick = {
                                                showSettings = false
                                                showEditWidgets = true
                                            },
                                        )
                                    }

                                    else -> {
                                        MainScreen(
                                            viewModel = viewModel,
                                            appDrawerViewModel = appDrawerViewModel,
                                            widgetsLocked = widgetsLocked,
                                            onOpenSettings = { showSettings = true },
                                            onOpenEditWidgets = { showEditWidgets = true },
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
}
