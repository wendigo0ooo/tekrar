package com.wendigo.tekrar.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wendigo.tekrar.BuildConfig
import com.wendigo.tekrar.R
import com.wendigo.tekrar.repo.AppSettings
import com.wendigo.tekrar.repo.SettingsStore
import com.wendigo.tekrar.ui.components.LabeledSwitch
import com.wendigo.tekrar.ui.components.Panel
import com.wendigo.tekrar.ui.components.ScreenHeader
import com.wendigo.tekrar.ui.components.SectionTitle
import com.wendigo.tekrar.ui.components.Segmented
import com.wendigo.tekrar.ui.components.Stepper
import com.wendigo.tekrar.ui.contracts.SettingsUiState
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    message: String?,
    onClearMessage: () -> Unit,
    onNewPerDay: (Int) -> Unit,
    onDailyGoal: (Int) -> Unit,
    onRetention: (Int) -> Unit,
    onThemeMode: (Int) -> Unit,
    onAutoSpeak: (Boolean) -> Unit,
    onSpeechRate: (Float) -> Unit,
    onReminder: (Boolean, Int, Int) -> Unit,
    onTestNotification: () -> Unit,
    onExportJson: (android.net.Uri) -> Unit,
    onImportJson: (android.net.Uri) -> Unit,
    onExportCsv: (android.net.Uri) -> Unit,
    onImportCsv: (android.net.Uri) -> Unit,
    onResetAll: () -> Unit,
) {
    val settings = state.settings
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showReset by remember { mutableStateOf(false) }
    var pendingJsonUri by remember { mutableStateOf<Uri?>(null) }

    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onReminder(true, settings.reminderHour, settings.reminderMinute)
        } else {
            scope.launch {
                snackbar.showSnackbar(
                    context.getString(R.string.settings_reminder_permission_denied)
                )
            }
        }
    }

    val jsonExport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) onExportJson(uri) }

    val jsonImport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingJsonUri = uri }

    val csvExport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> if (uri != null) onExportCsv(uri) }

    val csvImport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onImportCsv(uri) }

    LaunchedEffect(message) {
        val text = message
        if (text != null) {
            snackbar.showSnackbar(text)
            onClearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_subtitle),
            )

            SectionTitle(text = stringResource(R.string.settings_study))
            Panel {
                Text(
                    text = stringResource(R.string.settings_new_per_day),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_new_per_day_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Stepper(
                    value = settings.newPerDay,
                    onDecrease = { onNewPerDay(settings.newPerDay - 5) },
                    onIncrease = { onNewPerDay(settings.newPerDay + 5) },
                    decreaseEnabled = settings.newPerDay > SettingsStore.MIN_NEW_PER_DAY,
                    increaseEnabled = settings.newPerDay < SettingsStore.MAX_NEW_PER_DAY,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                )
                Text(
                    text = stringResource(R.string.settings_daily_goal),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_daily_goal_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Stepper(
                    value = settings.dailyGoal,
                    onDecrease = { onDailyGoal(settings.dailyGoal - 5) },
                    onIncrease = { onDailyGoal(settings.dailyGoal + 5) },
                    decreaseEnabled = settings.dailyGoal > SettingsStore.MIN_DAILY_GOAL,
                    increaseEnabled = settings.dailyGoal < SettingsStore.MAX_DAILY_GOAL,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                )
                Text(
                    text = stringResource(R.string.settings_retention),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_retention_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Stepper(
                    value = settings.desiredRetention,
                    onDecrease = { onRetention(settings.desiredRetention - 1) },
                    onIncrease = { onRetention(settings.desiredRetention + 1) },
                    decreaseEnabled = settings.desiredRetention > SettingsStore.MIN_RETENTION,
                    increaseEnabled = settings.desiredRetention < SettingsStore.MAX_RETENTION,
                    valueSuffix = "%",
                )
            }

            SectionTitle(text = stringResource(R.string.settings_appearance))
            Panel {
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Segmented(
                    options = listOf(
                        stringResource(R.string.theme_system),
                        stringResource(R.string.theme_light),
                        stringResource(R.string.theme_dark),
                    ),
                    selectedIndex = settings.themeMode.coerceIn(0, 2),
                    onSelect = onThemeMode,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                )
                LabeledSwitch(
                    title = stringResource(R.string.settings_auto_speak),
                    description = stringResource(R.string.settings_auto_speak_desc),
                    checked = settings.autoSpeak,
                    onCheckedChange = onAutoSpeak,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.settings_speech_rate),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_speech_rate_value,
                            String.format(Locale.US, "%.1f", settings.speechRate),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = settings.speechRate,
                    onValueChange = onSpeechRate,
                    valueRange = SettingsStore.MIN_SPEECH_RATE..SettingsStore.MAX_SPEECH_RATE,
                    steps = 7,
                )
            }

            SectionTitle(text = stringResource(R.string.settings_reminder))
            Panel {
                LabeledSwitch(
                    title = stringResource(R.string.settings_reminder_time),
                    description = stringResource(R.string.settings_reminder_desc),
                    checked = settings.reminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onReminder(enabled, settings.reminderHour, settings.reminderMinute)
                        }
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = String.format(
                            Locale.US,
                            "%02d:%02d",
                            settings.reminderHour,
                            settings.reminderMinute,
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    onReminder(settings.reminderEnabled, hour, minute)
                                },
                                settings.reminderHour,
                                settings.reminderMinute,
                                true,
                            ).show()
                        },
                    ) {
                        Text(text = stringResource(R.string.settings_reminder_change))
                    }
                }
                OutlinedButton(
                    onClick = onTestNotification,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(text = stringResource(R.string.settings_test_notification))
                }
            }

            SectionTitle(text = stringResource(R.string.settings_data))
            Panel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.settings_total_words),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${state.totalWords}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.settings_reviewed_today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${state.reviewedToday}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                )
                OutlinedButton(
                    onClick = { jsonExport.launch("tekrar-yedek.json") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(text = stringResource(R.string.settings_backup_json))
                }
                OutlinedButton(
                    onClick = { jsonImport.launch(arrayOf("application/json", "text/plain")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(text = stringResource(R.string.settings_restore_json))
                }
                OutlinedButton(
                    onClick = { csvExport.launch("tekrar-kelimeler.csv") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(text = stringResource(R.string.settings_export_csv))
                }
                OutlinedButton(
                    onClick = {
                        csvImport.launch(
                            arrayOf("text/csv", "text/comma-separated-values", "text/plain")
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(text = stringResource(R.string.settings_import_csv))
                }
                TextButton(
                    onClick = { showReset = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.settings_reset_all),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            SectionTitle(text = stringResource(R.string.settings_about))
            Panel {
                Text(
                    text = stringResource(R.string.settings_about_text, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.settings_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp),
        )
    }

    pendingJsonUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingJsonUri = null },
            title = { Text(text = stringResource(R.string.settings_restore_confirm_title)) },
            text = { Text(text = stringResource(R.string.settings_restore_confirm_text)) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingJsonUri = null
                        onImportJson(uri)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(text = stringResource(R.string.settings_restore_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingJsonUri = null }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(text = stringResource(R.string.settings_reset_all_title)) },
            text = { Text(text = stringResource(R.string.settings_reset_all_text)) },
            confirmButton = {
                Button(
                    onClick = {
                        showReset = false
                        onResetAll()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(text = stringResource(R.string.settings_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
