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
import com.wendigo.tekrar.ai.AiSessionProfile
import com.wendigo.tekrar.audio.Speaker
import com.wendigo.tekrar.core.CustomSession
import com.wendigo.tekrar.ui.ai.AiChatMode
import com.wendigo.tekrar.ui.ai.AiChatRequest
import com.wendigo.tekrar.ui.ai.AiChatScreen
import com.wendigo.tekrar.ui.ai.AiChatViewModel
import com.wendigo.tekrar.ui.ai.AiSettingsScreen
import com.wendigo.tekrar.ui.ai.AiSettingsViewModel
import com.wendigo.tekrar.ui.add.AddWordScreen
import com.wendigo.tekrar.ui.components.BottomBar
import com.wendigo.tekrar.ui.contracts.CustomSessionRequest
import com.wendigo.tekrar.ui.custom.CustomSessionScreen
import com.wendigo.tekrar.ui.decks.DecksScreen
import com.wendigo.tekrar.ui.contracts.LibraryFilter
import com.wendigo.tekrar.ui.contracts.LibrarySort
import com.wendigo.tekrar.ui.contracts.StudyFormat
import com.wendigo.tekrar.ui.contracts.StudyMode
import com.wendigo.tekrar.ui.leech.LeechesScreen
import com.wendigo.tekrar.ui.learn.LearnHomeScreen
import com.wendigo.tekrar.ui.learn.LearnHomeViewModel
import com.wendigo.tekrar.ui.learn.LessonScreen
import com.wendigo.tekrar.ui.learn.LessonViewModel
import com.wendigo.tekrar.ui.library.LibraryScreen
import com.wendigo.tekrar.ui.mine.MineScreen
import com.wendigo.tekrar.ui.placement.PlacementScreen
import com.wendigo.tekrar.ui.profile.ProfileScreen
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
            startDestination = ROUTE_LEARN,
            modifier = Modifier.weight(1f),
        ) {
            composable(ROUTE_LEARN) {
                val vm: LearnHomeViewModel = viewModel(
                    factory = viewModelFactory {
                        LearnHomeViewModel(
                            container.courseRepository,
                            container.courseProgress,
                            container.repository,
                        )
                    },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                LifecycleResumeEffect(Unit) {
                    vm.refresh()
                    onPauseOrDispose { }
                }
                LearnHomeScreen(
                    state = state,
                    onLesson = { unitId, lessonId ->
                        navController.navigate(
                            "$ROUTE_LESSON/${Uri.encode(unitId)}/${Uri.encode(lessonId)}"
                        )
                    },
                    onRoleplay = { unitId, _ ->
                        navController.navigate("$ROUTE_AI_CHAT?unitId=${Uri.encode(unitId)}")
                    },
                    onPractice = {
                        navController.navigate(
                            "$ROUTE_STUDY/${StudyMode.MIXED.name}/${StudyFormat.FLASHCARD.name}"
                        )
                    },
                    onRetry = vm::retry,
                    onAiCoach = { navController.navigate(ROUTE_AI_CHAT) },
                )
            }

            composable("$ROUTE_LESSON/{unitId}/{lessonId}") { entry ->
                val unitId = entry.arguments?.getString("unitId").orEmpty()
                val lessonId = entry.arguments?.getString("lessonId").orEmpty()
                val vm: LessonViewModel = viewModel(
                    factory = viewModelFactory {
                        LessonViewModel(
                            container.courseRepository,
                            container.courseProgress,
                            Speaker(container.appContext),
                        )
                    },
                )
                LaunchedEffect(unitId, lessonId) {
                    vm.start(Uri.decode(unitId), Uri.decode(lessonId))
                }
                val state by vm.state.collectAsStateWithLifecycle()
                LessonScreen(
                    state = state,
                    onClose = { navController.popBackStack() },
                    onSpeak = vm::speak,
                    onContinueTeach = vm::continueFromTeach,
                    onSubmitChoice = vm::submitChoice,
                    onSubmitMatch = vm::submitMatch,
                    onSubmitTokens = vm::submitTokens,
                    onSubmitText = vm::submitText,
                    onSubmitSpeech = vm::submitSpeech,
                    onNext = vm::next,
                    onRetryLoad = vm::retryStart,
                    onResultBack = { navController.popBackStack() },
                    onResultPractice = {
                        navController.navigate(
                            "$ROUTE_STUDY/${StudyMode.MIXED.name}/${StudyFormat.FLASHCARD.name}"
                        )
                    },
                    onRetrySave = vm::finish,
                )
            }

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
                    onDecks = { navController.navigate(ROUTE_DECKS) },
                    onPlacement = { navController.navigate(ROUTE_PLACEMENT) },
                    onCustomSession = { navController.navigate(ROUTE_CUSTOM_SESSION) },
                    onMine = { navController.navigate(ROUTE_MINE) },
                )
            }

            composable(ROUTE_MINE) {
                val vm: MineViewModel = viewModel(
                    factory = viewModelFactory {
                        MineViewModel(
                            container.repository,
                            container.dictionary,
                            container.appContext,
                        )
                    },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                MineScreen(
                    state = state,
                    onInput = vm::setInput,
                    onAnalyze = vm::analyze,
                    onAdd = vm::add,
                    onAddAll = vm::addAll,
                    onImportDictionary = vm::importDictionary,
                    onClearMessage = vm::clearMessage,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(ROUTE_DECKS) {
                val vm: DecksViewModel = viewModel(
                    factory = viewModelFactory {
                        DecksViewModel(container.repository, container.appContext)
                    },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                val message by vm.message.collectAsStateWithLifecycle()
                DecksScreen(
                    state = state,
                    message = message,
                    onClearMessage = vm::clearMessage,
                    onInstall = vm::install,
                    onStudy = { deck ->
                        navController.navigate(
                            "$ROUTE_STUDY_DECK/${Uri.encode(deck)}/${StudyFormat.FLASHCARD.name}"
                        )
                    },
                    onStudyLibrary = {
                        navController.navigate(
                            "$ROUTE_STUDY/${StudyMode.MIXED.name}/${StudyFormat.FLASHCARD.name}"
                        )
                    },
                    onPlacement = { navController.navigate(ROUTE_PLACEMENT) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(ROUTE_PLACEMENT) {
                val vm: PlacementViewModel = viewModel(
                    factory = viewModelFactory {
                        PlacementViewModel(container.repository)
                    },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                PlacementScreen(
                    state = state,
                    onAnswer = vm::answer,
                    onNext = vm::next,
                    onApply = vm::apply,
                    onRetry = vm::load,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(ROUTE_CUSTOM_SESSION) {
                val vm: CustomSessionViewModel = viewModel(
                    factory = viewModelFactory {
                        CustomSessionViewModel(container.repository, container.appContext)
                    },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                CustomSessionScreen(
                    state = state,
                    onMode = vm::setMode,
                    onDeck = vm::setDeck,
                    onTag = vm::setTag,
                    onLimit = vm::setLimit,
                    onFormat = vm::setFormat,
                    onCram = vm::setCram,
                    onStart = { request -> navController.navigate(customStudyRoute(request)) },
                    onBack = { navController.popBackStack() },
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
                    onMine = { navController.navigate(ROUTE_MINE) },
                )
            }

            composable(ROUTE_PROFILE) {
                ProfileScreen(
                    onStats = { navController.navigate(ROUTE_STATS) },
                    onSettings = { navController.navigate(ROUTE_SETTINGS) },
                    onAiSettings = { navController.navigate(ROUTE_AI_SETTINGS) },
                )
            }

            composable(
                route = "$ROUTE_AI_CHAT?unitId={unitId}",
                arguments = listOf(
                    navArgument("unitId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val unitId = entry.arguments?.getString("unitId")
                val vm: AiChatViewModel = viewModel(
                    factory = viewModelFactory {
                        AiChatViewModel(
                            container.aiRepository,
                            container.aiSettings,
                            container.appContext,
                        )
                    },
                )
                LifecycleResumeEffect(Unit) {
                    vm.refreshKeyState()
                    onPauseOrDispose { }
                }
                LaunchedEffect(unitId) {
                    vm.start(buildAiChatRequest(container, unitId))
                }
                val state by vm.state.collectAsStateWithLifecycle()
                val appSettings by container.settings.settings.collectAsStateWithLifecycle(
                    initialValue = com.wendigo.tekrar.repo.AppSettings(),
                )
                AiChatScreen(
                    state = state,
                    onDraftChange = vm::onDraftChange,
                    onSend = vm::send,
                    onRetry = vm::retryLast,
                    onEndSession = vm::endSession,
                    onNewSession = { vm.newSession() },
                    onClearError = vm::clearError,
                    onOpenSettings = { navController.navigate(ROUTE_AI_SETTINGS) },
                    onClose = {
                        vm.close()
                        navController.popBackStack()
                    },
                    speechRate = appSettings.speechRate,
                )
            }

            composable(ROUTE_AI_SETTINGS) {
                val vm: AiSettingsViewModel = viewModel(
                    factory = viewModelFactory {
                        AiSettingsViewModel(
                            container.aiRepository,
                            container.aiSettings,
                            container.appContext,
                        )
                    },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                AiSettingsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onProviderChange = vm::onProviderChange,
                    onBaseUrlChange = vm::onBaseUrlChange,
                    onBaseUrlCommit = vm::onBaseUrlCommit,
                    onModelChange = vm::onModelChange,
                    onKeyDraftChange = vm::onKeyDraftChange,
                    onSaveKey = vm::saveKey,
                    onDeleteKey = vm::deleteKey,
                    onTestConnection = vm::testConnection,
                    onDailyLimitChange = vm::onDailyLimitChange,
                    onAutoSpeakChange = vm::onAutoSpeakChange,
                    onClearHistory = vm::clearHistory,
                    onClearMessage = vm::clearMessage,
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
                    onBack = { navController.popBackStack() },
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
                    onWeeklySummary = vm::setWeeklySummary,
                    onTestNotification = vm::testNotification,
                    onExportJson = vm::exportJson,
                    onImportJson = vm::importJson,
                    onExportCsv = vm::exportCsv,
                    onImportCsv = vm::importCsv,
                    onResetAll = vm::resetAllProgress,
                    onBack = { navController.popBackStack() },
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

            composable("$ROUTE_STUDY_DECK/{deck}/{format}") { entry ->
                val deck = Uri.decode(entry.arguments?.getString("deck").orEmpty())
                val format = runCatching {
                    StudyFormat.valueOf(
                        entry.arguments?.getString("format") ?: StudyFormat.FLASHCARD.name
                    )
                }.getOrDefault(StudyFormat.FLASHCARD)
                val vm: StudyViewModel = viewModel(
                    factory = viewModelFactory {
                        StudyViewModel(
                            repository = container.repository,
                            mode = StudyMode.MIXED,
                            format = format,
                            deck = deck,
                        )
                    },
                )
                val state by vm.state.collectAsStateWithLifecycle()
                val settings by container.settings.settings.collectAsStateWithLifecycle(
                    initialValue = com.wendigo.tekrar.repo.AppSettings(),
                )
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
                    onExit = { navController.popBackStack() },
                    onLibrary = { navController.navigateTab(ROUTE_LIBRARY) },
                )
            }

            composable(
                route = "$ROUTE_CUSTOM_STUDY?mode={mode}&format={format}&deck={deck}" +
                    "&tag={tag}&limit={limit}&cram={cram}",
                arguments = listOf(
                    navArgument("mode") {
                        type = NavType.StringType
                        defaultValue = StudyMode.MIXED.name
                    },
                    navArgument("format") {
                        type = NavType.StringType
                        defaultValue = StudyFormat.FLASHCARD.name
                    },
                    navArgument("deck") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("tag") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("limit") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                    navArgument("cram") {
                        type = NavType.BoolType
                        defaultValue = false
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
                val deck = CustomSession.deckFromArg(
                    entry.arguments?.getString("deck")?.let { Uri.decode(it) }
                )
                val tag = CustomSession.tagFromArg(
                    entry.arguments?.getString("tag")?.let { Uri.decode(it) }
                )
                val limit = entry.arguments?.getInt("limit") ?: 0
                val cram = entry.arguments?.getBoolean("cram") ?: false
                val vm: StudyViewModel = viewModel(
                    factory = viewModelFactory {
                        StudyViewModel(
                            repository = container.repository,
                            mode = mode,
                            format = format,
                            deck = deck,
                            maxCards = limit.takeIf { it > 0 },
                            tag = tag,
                            cram = cram,
                            custom = true,
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

        val tabs = listOf(ROUTE_LEARN, ROUTE_TODAY, ROUTE_LIBRARY, ROUTE_PROFILE)
        val selectedIndex = tabs.indexOf(route)
        if (selectedIndex >= 0) {
            BottomBar(
                selectedIndex = selectedIndex,
                labels = listOf(
                    stringResource(R.string.nav_learn),
                    stringResource(R.string.nav_today),
                    stringResource(R.string.nav_library),
                    stringResource(R.string.nav_profile),
                ),
                onSelect = { index -> navController.navigateTab(tabs[index]) },
            )
        }
    }
}

private suspend fun buildAiChatRequest(
    container: AppContainer,
    unitId: String?,
): AiChatRequest {
    val id = unitId?.trim().orEmpty()
    if (id.isEmpty()) {
        return AiChatRequest(
            mode = AiChatMode.CHAT,
            profile = AiSessionProfile(level = "A1"),
        )
    }
    return try {
        val index = container.courseRepository.loadIndex()
        val unitRef = index.levels.flatMap { it.units }.firstOrNull { it.id == id }
            ?: return AiChatRequest(
                mode = AiChatMode.CHAT,
                profile = AiSessionProfile(level = "A1"),
            )
        val unit = container.courseRepository.loadUnit(unitRef)
        val roleplayRef = unit.roleplay
        val roleplay = roleplayRef?.let {
            container.courseRepository.loadRoleplay(unitRef, it)
        }
        if (roleplay == null) {
            AiChatRequest(
                mode = AiChatMode.CHAT,
                title = unit.title,
                unitId = unit.id,
                profile = AiSessionProfile(level = unit.cefr),
            )
        } else {
            AiChatRequest(
                mode = AiChatMode.ROLEPLAY,
                title = roleplay.title,
                unitId = roleplay.unit,
                scenarioId = roleplay.id,
                openingEn = roleplay.openingEn,
                profile = AiSessionProfile(
                    level = roleplay.cefr,
                    scenarioTitle = roleplay.title,
                    settingTr = roleplay.settingTr,
                    roleEn = roleplay.roleEn,
                    goalTr = roleplay.goalTr,
                    targetPhrases = roleplay.targetPhrases,
                ),
            )
        }
    } catch (error: Exception) {
        AiChatRequest(
            mode = AiChatMode.CHAT,
            profile = AiSessionProfile(level = "A1"),
        )
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

private fun customStudyRoute(request: CustomSessionRequest): String {
    val base = "$ROUTE_CUSTOM_STUDY?mode=${request.mode.name}&format=${request.format.name}"
    return base +
        "&deck=${Uri.encode(CustomSession.deckArg(request.deck))}" +
        "&tag=${Uri.encode(CustomSession.tagArg(request.tag))}" +
        "&limit=${request.limit ?: 0}" +
        "&cram=${request.cram}"
}

private const val ROUTE_LEARN = "learn"
private const val ROUTE_LESSON = "learn/lesson"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_AI_CHAT = "ai/chat"
private const val ROUTE_AI_SETTINGS = "ai/settings"
private const val ROUTE_TODAY = "today"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_STATS = "stats"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_STUDY = "study"
private const val ROUTE_STUDY_DECK = "study-deck"
private const val ROUTE_CUSTOM_SESSION = "custom-session"
private const val ROUTE_CUSTOM_STUDY = "custom-study"
private const val ROUTE_MINE = "mine"
private const val ROUTE_DECKS = "decks"
private const val ROUTE_PLACEMENT = "placement"
private const val ROUTE_LEECHES = "leeches"
private const val ROUTE_WORD_NEW = "word/new"
private const val ROUTE_WORD_EDIT = "word/edit"
private const val ROUTE_WORD_PREFIX = "detail"
