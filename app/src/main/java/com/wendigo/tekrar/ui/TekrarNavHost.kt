package com.wendigo.tekrar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wendigo.tekrar.AppContainer
import com.wendigo.tekrar.ui.add.AddWordScreen
import com.wendigo.tekrar.ui.contracts.StudyMode
import com.wendigo.tekrar.ui.dashboard.DashboardScreen
import com.wendigo.tekrar.ui.settings.SettingsScreen
import com.wendigo.tekrar.ui.study.StudyScreen

@Composable
fun TekrarNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_DASHBOARD) {
        composable(ROUTE_DASHBOARD) {
            val vm: DashboardViewModel = viewModel(
                factory = viewModelFactory { DashboardViewModel(container.repository) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            DashboardScreen(
                state = state,
                onStudy = { mode -> navController.navigate("$ROUTE_STUDY/${mode.name}") },
                onAddWord = { navController.navigate(ROUTE_ADD) },
                onSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }

        composable("$ROUTE_STUDY/{mode}") { entry ->
            val mode = runCatching {
                StudyMode.valueOf(entry.arguments?.getString("mode") ?: StudyMode.MIXED.name)
            }.getOrDefault(StudyMode.MIXED)
            val vm: StudyViewModel = viewModel(
                factory = viewModelFactory { StudyViewModel(container.repository, mode) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            StudyScreen(
                state = state,
                onReveal = vm::reveal,
                onRate = vm::rate,
                onRestart = vm::load,
                onExit = { navController.popBackStack() },
            )
        }

        composable(ROUTE_ADD) {
            val vm: AddWordViewModel = viewModel(
                factory = viewModelFactory { AddWordViewModel(container.repository) },
            )
            AddWordScreen(
                onSave = { term, meaning, example, notes ->
                    vm.save(term, meaning, example, notes) {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(ROUTE_SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = viewModelFactory {
                    SettingsViewModel(container.repository, container.settings)
                },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            SettingsScreen(
                state = state,
                onNewPerDayChange = vm::setNewPerDay,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_STUDY = "study"
private const val ROUTE_ADD = "add"
private const val ROUTE_SETTINGS = "settings"
