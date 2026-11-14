package com.wendigo.tekrar.ui.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ai.AiModelOption
import com.wendigo.tekrar.ai.AiProviderPreset
import com.wendigo.tekrar.ui.components.LabeledSwitch
import com.wendigo.tekrar.ui.components.Panel
import com.wendigo.tekrar.ui.components.ScreenHeader
import com.wendigo.tekrar.ui.components.SectionTitle
import com.wendigo.tekrar.ui.components.Stepper

private val AiActionText = 16.sp

/**
 * AI ayarları ekranı. Mevcut [com.wendigo.tekrar.ui.settings.SettingsScreen]'den
 * ayrıdır; anahtar yalnız maskeli gösterilir ve kayıtlıyken son karakterleri
 * dahil hiçbir parçası ekrana yazılmaz.
 */
@Composable
fun AiSettingsScreen(
    state: AiSettingsUiState,
    onBack: () -> Unit,
    onProviderChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onBaseUrlCommit: () -> Unit,
    onModelChange: (String) -> Unit,
    onKeyDraftChange: (String) -> Unit,
    onSaveKey: () -> Unit,
    onDeleteKey: () -> Unit,
    onTestConnection: () -> Unit,
    onDailyLimitChange: (Int) -> Unit,
    onAutoSpeakChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onClearMessage: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    var showClearHistory by remember { mutableStateOf(false) }
    var showDeleteKey by remember { mutableStateOf(false) }
    var hadUrlFocus by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        onClearMessage()
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
                title = stringResource(R.string.ai_settings_title),
                subtitle = stringResource(R.string.ai_settings_subtitle),
                onBack = onBack,
            )

            SectionTitle(text = stringResource(R.string.ai_settings_provider))
            Panel {
                AiProviderOptionRow(
                    selected = !state.isCustomProvider,
                    title = stringResource(R.string.ai_settings_provider_open_code_go),
                    note = stringResource(R.string.ai_settings_provider_open_code_go_note),
                    onClick = { onProviderChange(AiProviderPreset.OPEN_CODE_GO_ID) },
                )
                AiProviderOptionRow(
                    selected = state.isCustomProvider,
                    title = stringResource(R.string.ai_settings_provider_custom),
                    note = stringResource(R.string.ai_settings_provider_custom_note),
                    onClick = { onProviderChange(AiProviderPreset.CUSTOM_ID) },
                )
                if (state.isCustomProvider) {
                    OutlinedTextField(
                        value = state.baseUrlDraft,
                        onValueChange = onBaseUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focus ->
                                if (focus.isFocused) {
                                    hadUrlFocus = true
                                } else if (hadUrlFocus) {
                                    hadUrlFocus = false
                                    onBaseUrlCommit()
                                }
                            },
                        label = { Text(stringResource(R.string.ai_settings_base_url)) },
                        placeholder = {
                            Text(stringResource(R.string.ai_settings_base_url_hint))
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onBaseUrlCommit() },
                        ),
                    )
                    Text(
                        text = stringResource(R.string.ai_settings_base_url_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionTitle(text = stringResource(R.string.ai_settings_model))
            Panel {
                AiModelOption.options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onModelChange(option.id) }
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        RadioButton(
                            selected = state.model == option.id,
                            onClick = { onModelChange(option.id) },
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = option.noteTr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.ai_settings_model_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionTitle(text = stringResource(R.string.ai_settings_key))
            Panel {
                Text(
                    text = stringResource(R.string.ai_settings_key_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.keyDraft,
                    onValueChange = onKeyDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.ai_settings_key)) },
                    placeholder = {
                        Text(stringResource(R.string.ai_settings_key_hint))
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSaveKey() }),
                )
                if (state.hasKey) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Text(
                            text = stringResource(R.string.ai_settings_key_saved),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onSaveKey,
                        enabled = state.keyDraft.isNotBlank() && !state.savingKey,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.ai_settings_key_save),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = AiActionText,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                    OutlinedButton(
                        onClick = { showDeleteKey = true },
                        enabled = state.hasKey && !state.savingKey,
                        modifier = Modifier.height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.ai_settings_key_delete),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AiActionText),
                        )
                    }
                }
                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = state.hasKey && state.keyDraft.isBlank() && !state.testing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (state.testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ai_settings_testing),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AiActionText),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.ai_settings_test),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AiActionText),
                        )
                    }
                }
            }

            SectionTitle(text = stringResource(R.string.ai_settings_chat))
            Panel {
                Text(
                    text = stringResource(R.string.ai_settings_limit),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.ai_settings_limit_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Stepper(
                    value = state.dailyMessageLimit,
                    onDecrease = { onDailyLimitChange(state.dailyMessageLimit - 5) },
                    onIncrease = { onDailyLimitChange(state.dailyMessageLimit + 5) },
                    decreaseEnabled = state.dailyMessageLimit > 5,
                    increaseEnabled = state.dailyMessageLimit < 500,
                    valueSuffix = stringResource(R.string.ai_settings_limit_suffix),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                )
                LabeledSwitch(
                    title = stringResource(R.string.ai_settings_auto_speak),
                    description = stringResource(R.string.ai_settings_auto_speak_desc),
                    checked = state.autoSpeak,
                    onCheckedChange = onAutoSpeakChange,
                )
            }

            SectionTitle(text = stringResource(R.string.ai_settings_data))
            Panel {
                Text(
                    text = stringResource(R.string.ai_settings_history_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { showClearHistory = true },
                    enabled = !state.busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.ai_settings_history_clear),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = AiActionText,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }

            Text(
                text = stringResource(R.string.ai_settings_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp),
        )
    }

    if (showDeleteKey) {
        AlertDialog(
            onDismissRequest = { showDeleteKey = false },
            title = {
                Text(text = stringResource(R.string.ai_settings_key_delete_title))
            },
            text = {
                Text(text = stringResource(R.string.ai_settings_key_delete_text))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteKey = false
                        onDeleteKey()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.ai_settings_key_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteKey = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showClearHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistory = false },
            title = {
                Text(text = stringResource(R.string.ai_settings_history_clear_title))
            },
            text = {
                Text(text = stringResource(R.string.ai_settings_history_clear_text))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistory = false
                        onClearHistory()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.ai_settings_history_clear_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistory = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun AiProviderOptionRow(
    selected: Boolean,
    title: String,
    note: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
