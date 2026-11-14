package com.wendigo.tekrar.ui.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ai.AiModelOption
import com.wendigo.tekrar.ai.AiProviderPreset
import com.wendigo.tekrar.ai.AiRepository
import com.wendigo.tekrar.ai.AiSettings
import com.wendigo.tekrar.ai.AiSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AiSettingsUiState(
    val providerId: String = AiProviderPreset.OPEN_CODE_GO_ID,
    val baseUrl: String = AiProviderPreset.OPEN_CODE_GO.baseUrl,
    val baseUrlDraft: String = AiProviderPreset.OPEN_CODE_GO.baseUrl,
    val model: String = AiModelOption.DEFAULT_MODEL_ID,
    val hasKey: Boolean = false,
    val keyDraft: String = "",
    val dailyMessageLimit: Int = AiSettings.DEFAULT_DAILY_LIMIT,
    val autoSpeak: Boolean = false,
    val testing: Boolean = false,
    val savingKey: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
) {
    val isCustomProvider: Boolean get() = providerId == AiProviderPreset.CUSTOM_ID
}

/**
 * AI ayarları ekranı: sağlayıcı, model, anahtar (Keystore), bağlantı testi,
 * günlük limit ve geçmiş temizliği. Anahtar değeri state'te kalıcı tutulmaz;
 * kaydedilince alan temizlenir ve hiçbir yerde gösterilmez.
 */
class AiSettingsViewModel(
    private val repository: AiRepository,
    private val settings: AiSettingsStore,
    private val appContext: Context,
) : ViewModel() {

    private val initial = settings.state.value

    private val _state = MutableStateFlow(
        AiSettingsUiState(
            providerId = initial.providerId,
            baseUrl = initial.baseUrl,
            baseUrlDraft = initial.baseUrl,
            model = initial.model,
            dailyMessageLimit = initial.dailyMessageLimit,
            autoSpeak = initial.autoSpeak,
        ),
    )
    val state: StateFlow<AiSettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.state.collect { current ->
                _state.update { st ->
                    val providerChanged = st.providerId != current.providerId
                    st.copy(
                        providerId = current.providerId,
                        baseUrl = current.baseUrl,
                        baseUrlDraft = if (providerChanged) {
                            current.baseUrl
                        } else {
                            st.baseUrlDraft
                        },
                        model = current.model,
                        dailyMessageLimit = current.dailyMessageLimit,
                        autoSpeak = current.autoSpeak,
                    )
                }
            }
        }
        refreshKeyState()
    }

    fun onProviderChange(providerId: String) {
        settings.setProvider(providerId)
        _state.update {
            it.copy(
                providerId = settings.state.value.providerId,
                baseUrlDraft = settings.state.value.baseUrl,
            )
        }
    }

    fun onBaseUrlChange(text: String) {
        _state.update { it.copy(baseUrlDraft = text) }
    }

    /** Klavye "Bitti" veya odak kaybında çağrılır. */
    fun onBaseUrlCommit() {
        commitBaseUrl()
    }

    fun onModelChange(modelId: String) {
        settings.setModel(modelId)
    }

    fun onKeyDraftChange(text: String) {
        _state.update { it.copy(keyDraft = text) }
    }

    fun saveKey() {
        if (_state.value.savingKey) return
        val key = _state.value.keyDraft.trim()
        if (key.isEmpty()) {
            showMessage(appContext.getString(R.string.ai_settings_key_required), isError = true)
            return
        }
        _state.update { it.copy(savingKey = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { repository.saveApiKey(key) }
            }
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(hasKey = true, keyDraft = "", savingKey = false)
                    }
                    showMessage(
                        appContext.getString(R.string.ai_settings_key_save_ok),
                        isError = false,
                    )
                },
                onFailure = {
                    _state.update { it.copy(savingKey = false) }
                    showMessage(
                        appContext.getString(R.string.ai_settings_key_save_failed),
                        isError = true,
                    )
                },
            )
        }
    }

    fun deleteKey() {
        if (_state.value.savingKey) return
        _state.update { it.copy(savingKey = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { repository.clearApiKey() }
            }
            _state.update {
                it.copy(hasKey = !result.isSuccess, keyDraft = "", savingKey = false)
            }
            showMessage(
                appContext.getString(
                    if (result.isSuccess) {
                        R.string.ai_settings_key_deleted
                    } else {
                        R.string.ai_settings_key_save_failed
                    },
                ),
                isError = result.isFailure,
            )
        }
    }

    fun testConnection() {
        if (_state.value.testing) return
        if (_state.value.keyDraft.isNotBlank() || !_state.value.hasKey) {
            showMessage(appContext.getString(R.string.ai_settings_key_required), isError = true)
            return
        }
        if (!commitBaseUrl()) return
        _state.update { it.copy(testing = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.testConnection() }
            _state.update { it.copy(testing = false) }
            result.fold(
                onSuccess = {
                    showMessage(
                        appContext.getString(R.string.ai_settings_test_ok),
                        isError = false,
                    )
                },
                onFailure = { error ->
                    showMessage(
                        appContext.getString(
                            R.string.ai_settings_test_failed,
                            error.message ?: "",
                        ),
                        isError = true,
                    )
                },
            )
        }
    }

    fun onDailyLimitChange(limit: Int) {
        settings.setDailyLimit(limit)
    }

    fun onAutoSpeakChange(enabled: Boolean) {
        settings.setAutoSpeak(enabled)
    }

    fun clearHistory() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = runCatching { repository.deleteAllHistory() }
            _state.update { it.copy(busy = false) }
            showMessage(
                appContext.getString(
                    if (result.isSuccess) {
                        R.string.ai_settings_history_cleared
                    } else {
                        R.string.ai_settings_history_clear_failed
                    },
                ),
                isError = result.isFailure,
            )
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    /** Anahtar başka bir yerde değiştiyse (yeniden girişte) durumu tazeler. */
    fun refreshKeyState() {
        viewModelScope.launch {
            val hasKey = withContext(Dispatchers.IO) {
                runCatching { repository.hasKey() }.getOrDefault(false)
            }
            _state.update { it.copy(hasKey = hasKey) }
        }
    }

    private fun commitBaseUrl(): Boolean {
        val current = _state.value
        val draft = current.baseUrlDraft.trim()
        if (draft == current.baseUrl.trim()) return true
        if (draft.isEmpty() || !draft.startsWith("https://", ignoreCase = true)) {
            showMessage(
                appContext.getString(R.string.ai_settings_base_url_invalid),
                isError = true,
            )
            return false
        }
        settings.setBaseUrl(draft)
        _state.update {
            it.copy(
                baseUrl = settings.state.value.baseUrl,
                baseUrlDraft = settings.state.value.baseUrl,
            )
        }
        return true
    }

    private fun showMessage(text: String, isError: Boolean) {
        _state.update { it.copy(message = text, messageIsError = isError) }
    }
}
