package com.wendigo.tekrar.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wendigo.tekrar.AppContainer
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ui.add.AddWordScreen
import com.wendigo.tekrar.ui.components.BottomBar
import com.wendigo.tekrar.ui.contracts.LibraryFilter
import com.wendigo.tekrar.ui.contracts.LibrarySort
import com.wendigo.tekrar.ui.contracts.StudyFormat
import com.wendigo.tekrar.ui.contracts.StudyMode
import com.wendigo.tekrar.ui.leech.LeechesScreen
import com.wendigo.tekrar.ui.library.LibraryScreen
import com.wendigo.tekrar.ui.settings.SettingsScreen
import com.wendigo.tekrar.ui.stats.StatsScreen
import com.wendigo.tekrar.ui.study.StudyScreen
import com.wendigo.tekrar.ui.today.TodayScreen
import com.wendigo.tekrar.ui.word.WordDetailScreen
import com.wendigo.tekrar.widget.DueWidgetProvider

@Composable
fun TekrarNavHost(
    container: AppContainer,
    launchRequest: LaunchRequest? = null,
    onLaunchRequestConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NavHost(
            navController = navController,
            startDestination = ROUTE_TODAY,
            modifier = Modifier.weight(1f),
        ) {
            composable(ROUTE_TODAY) {
                val vm: DashboardViewModel = viewModel(
                    factory = viewModelFactory { DashboardViewModel(container.repository) },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                LifecycleResumeEffect(Unit) {
                    vm.refresh()
                    onPauseOrDispose { }
                }
                LaunchedEffect(state.reviewedToday > 0) {
                    if (state.reviewedToday > 0) vm.refresh()
                }
                TodayScreen(
                    state = state,
                    onStudy = { mode, format ->
                        navController.navigate("$ROUTE_STUDY/${mode.name}/${format.name}")
                    },
                    onAddWord = { navController.navigate(ROUTE_WORD_NEW) },
                    onLibrary = { navController.navigateTab(ROUTE_LIBRARY) },
                )
            }

            composable(ROUTE_LIBRARY) {
                val vm: LibraryViewModel = viewModel(
                    factory = viewModelFactory { LibraryViewModel(container.repository) },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                LibraryScreen(
                    state = state,
                    onQuery = vm::setQuery,
                    onFilter = vm::setFilter,
                    onSort = vm::setSort,
                    onWord = { id -> navController.navigate("$ROUTE_WORD_PREFIX/$id") },
                    onAdd = { navController.navigate(ROUTE_WORD_NEW) },
                )
            }

            composable(ROUTE_STATS) {
                val vm: StatsViewModel = viewModel(
                    factory = viewModelFactory { StatsViewModel(container.repository) },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                StatsScreen(
                    state = state,
                    onLeeches = { navController.navigate(ROUTE_LEECHES) },
                )
            }

            composable(ROUTE_LEECHES) {
                val vm: LeechesViewModel = viewModel(
                    factory = viewModelFactory { LeechesViewModel(container.repository) },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                LeechesScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onStudy = {
                        navController.navigate(
                            "$ROUTE_STUDY/${StudyMode.LEECH.name}/${StudyFormat.FLASHCARD.name}"
                        )
                    },
                    onEdit = { id -> navController.navigate("$ROUTE_WORD_EDIT/$id") },
                    onSuspend = vm::suspendWord,
                )
            }

            composable(ROUTE_SETTINGS) {
                val vm: SettingsViewModel = viewModel(
                    factory = viewModelFactory {
                        SettingsViewModel(
                            container.repository,
                            container.settings,
                            container.backupManager,
                            container.appContext,
                        )
                    },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                val message by vm.message.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = state,
                    message = message,
                    onClearMessage = vm::clearMessage,
                    onNewPerDay = vm::setNewPerDay,
                    onDailyGoal = vm::setDailyGoal,
                    onWeeklyGoal = vm::setWeeklyGoal,
                    onRetention = vm::setRetention,
                    onThemeMode = vm::setThemeMode,
                    onAutoSpeak = vm::setAutoSpeak,
                    onSpeechRate = vm::setSpeechRate,
                    onReminder = vm::setReminder,
                    onTestNotification = vm::testNotification,
                    onExportJson = vm::exportJson,
                    onImportJson = vm::importJson,
                    onExportCsv = vm::exportCsv,
                    onImportCsv = vm::importCsv,
                    onResetAll = vm::resetAllProgress,
                )
            }

            composable(
                route = "$ROUTE_STUDY/{mode}/{format}?limit={limit}",
                arguments = listOf(
                    navArgument("limit") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
            ) { entry ->
                val mode = runCatching {
                    StudyMode.valueOf(entry.arguments?.getString("mode") ?: StudyMode.MIXED.name)
                }.getOrDefault(StudyMode.MIXED)
                val format = runCatching {
                    StudyFormat.valueOf(
                        entry.arguments?.getString("format") ?: StudyFormat.FLASHCARD.name
                    )
                }.getOrDefault(StudyFormat.FLASHCARD)
                val limit = entry.arguments?.getInt("limit") ?: 0
                val vm: StudyViewModel = viewModel(
                    factory = viewModelFactory {
                        StudyViewModel(
                            container.repository,
                            mode,
                            format,
                            maxCards = limit.takeIf { it > 0 },
                        )
                    },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                val settings by container.settings.settings.collectAsStateWithLifecycle(
                    initialValue = com.wendigo.tekrar.repo.AppSettings(),
                )
                LaunchedEffect(state.finished, state.empty) {
                    if (state.finished || state.empty) {
                        DueWidgetProvider.refresh(container.appContext)
                    }
                }
                StudyScreen(
                    state = state,
                    autoSpeak = settings.autoSpeak,
                    speechRate = settings.speechRate,
                    onReveal = vm::reveal,
                    onRate = vm::rate,
                    onChoose = vm::choose,
                    onSubmitTyping = vm::submitTyping,
                    onContinue = vm::continueAfterAnswer,
                    onUndo = vm::undo,
                    onReload = vm::load,
                    onExit = {
                        DueWidgetProvider.refresh(container.appContext)
                        navController.popBackStack()
                    },
                    onLibrary = { navController.navigateTab(ROUTE_LIBRARY) },
                )
            }

            composable(
                route = "$ROUTE_WORD_NEW?term={term}",
                arguments = listOf(
                    navArgument("term") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val initialTerm = entry.arguments?.getString("term")
                val vm: WordEditorViewModel = viewModel(
                    factory = viewModelFactory {
                        WordEditorViewModel(container.repository, null, initialTerm)
                    },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                AddWordScreen(
                    state = state,
                    onSave = { term, meaning, example, exampleTr, notes, tags, pos, onSaved ->
                        vm.save(term, meaning, example, exampleTr, notes, tags, pos, onSaved)
                    },
                    onDelete = { onDeleted -> vm.delete(onDeleted) },
                    onClearDuplicateError = vm::clearDuplicateError,
                    onBack = { navController.popBackStack() },
                )
            }

            composable("$ROUTE_WORD_EDIT/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull()
                val vm: WordEditorViewModel = viewModel(
                    factory = viewModelFactory {
                        WordEditorViewModel(container.repository, id)
                    },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                AddWordScreen(
                    state = state,
                    onSave = { term, meaning, example, exampleTr, notes, tags, pos, onSaved ->
                        vm.save(term, meaning, example, exampleTr, notes, tags, pos, onSaved)
                    },
                    onDelete = { onDeleted -> vm.delete(onDeleted) },
                    onClearDuplicateError = vm::clearDuplicateError,
                    onBack = { navController.popBackStack() },
                )
            }

            composable("$ROUTE_WORD_PREFIX/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                val vm: WordDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        WordDetailViewModel(container.repository, id)
                    },
                )
                val word by vm.word.collectAsStateWithLifecycle()
                WordDetailScreen(
                    word = word,
                    onBack = { navController.popBackStack() },
                    onEdit = { wordId -> navController.navigate("$ROUTE_WORD_EDIT/$wordId") },
                    onToggleFavorite = vm::toggleFavorite,
                    onToggleSuspended = vm::toggleSuspended,
                    onResetProgress = vm::resetProgress,
                    onDelete = { current, onDeleted ->
                        vm.delete(current, onDeleted)
                    },
                )
            }
        }

        LaunchedEffect(launchRequest) {
            val request = launchRequest ?: return@LaunchedEffect
            val limit = request.studyLimit
            val term = request.initialTerm
            when {
                limit != null -> navController.navigate(
                    studyRoute(StudyMode.MIXED, StudyFormat.FLASHCARD, limit)
                )
                term != null -> navController.navigate(
                    "$ROUTE_WORD_NEW?term=${Uri.encode(term)}"
                )
            }
            onLaunchRequestConsumed()
        }

        val tabs = listOf(ROUTE_TODAY, ROUTE_LIBRARY, ROUTE_STATS, ROUTE_SETTINGS)
        val selectedIndex = tabs.indexOf(route)
        if (selectedIndex >= 0) {
            BottomBar(
                selectedIndex = selectedIndex,
                labels = listOf(
                    stringResource(R.string.nav_today),
                    stringResource(R.string.nav_library),
                    stringResource(R.string.nav_stats),
                    stringResource(R.string.nav_settings),
                ),
                onSelect = { index -> navController.navigateTab(tabs[index]) },
            )
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun studyRoute(mode: StudyMode, format: StudyFormat, limit: Int? = null): String {
    val base = "$ROUTE_STUDY/${mode.name}/${format.name}"
    return if (limit != null && limit > 0) "$base?limit=$limit" else base
}

private const val ROUTE_TODAY = "today"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_STATS = "stats"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_STUDY = "study"
private const val ROUTE_LEECHES = "leeches"
private const val ROUTE_WORD_NEW = "word/new"
private const val ROUTE_WORD_EDIT = "word/edit"
private const val ROUTE_WORD_PREFIX = "detail"
