package com.wendigo.tekrar.ai

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AiSettings(
    val providerId: String = AiProviderPreset.OPEN_CODE_GO_ID,
    val baseUrl: String = AiProviderPreset.OPEN_CODE_GO.baseUrl,
    val model: String = AiModelOption.DEFAULT_MODEL_ID,
    val dailyMessageLimit: Int = DEFAULT_DAILY_LIMIT,
    val autoSpeak: Boolean = false,
) {
    companion object {
        const val DEFAULT_DAILY_LIMIT = 60
    }
}

/**
 * AI ayarları: sağlayıcı, model, günlük limit. API anahtarı burada tutulmaz;
 * [KeystoreAiKeyStore] kullanılır.
 */
class AiSettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AiSettings> = _state.asStateFlow()

    fun update(settings: AiSettings) {
        val sanitized = settings.copy(
            providerId = if (settings.providerId == AiProviderPreset.CUSTOM_ID) {
                AiProviderPreset.CUSTOM_ID
            } else {
                AiProviderPreset.OPEN_CODE_GO_ID
            },
            model = AiModelOption.byId(settings.model)?.id ?: AiModelOption.DEFAULT_MODEL_ID,
            dailyMessageLimit = settings.dailyMessageLimit.coerceIn(5, 500),
            baseUrl = settings.baseUrl.trim(),
        )
        prefs.edit()
            .putString(KEY_PROVIDER, sanitized.providerId)
            .putString(KEY_BASE_URL, sanitized.baseUrl)
            .putString(KEY_MODEL, sanitized.model)
            .putInt(KEY_DAILY_LIMIT, sanitized.dailyMessageLimit)
            .putBoolean(KEY_AUTO_SPEAK, sanitized.autoSpeak)
            .apply()
        _state.value = sanitized
    }

    fun setProvider(providerId: String) {
        val preset = AiProviderPreset.byId(providerId)
        update(
            _state.value.copy(
                providerId = preset.id,
                baseUrl = preset.baseUrl.ifBlank { _state.value.baseUrl },
            ),
        )
    }

    fun setModel(modelId: String) {
        update(_state.value.copy(model = modelId))
    }

    fun setBaseUrl(baseUrl: String) {
        update(_state.value.copy(baseUrl = baseUrl))
    }

    fun setDailyLimit(limit: Int) {
        update(_state.value.copy(dailyMessageLimit = limit))
    }

    fun setAutoSpeak(enabled: Boolean) {
        update(_state.value.copy(autoSpeak = enabled))
    }

    fun config(apiKey: String): AiConfig {
        val current = _state.value
        val baseUrl = current.baseUrl.ifBlank {
            AiProviderPreset.byId(current.providerId).baseUrl
        }
        return AiConfig(
            providerId = current.providerId,
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = current.model,
        )
    }

    private fun read(): AiSettings {
        val provider = prefs.getString(KEY_PROVIDER, null)
            ?.takeIf { it == AiProviderPreset.OPEN_CODE_GO_ID || it == AiProviderPreset.CUSTOM_ID }
            ?: AiProviderPreset.OPEN_CODE_GO_ID
        val preset = AiProviderPreset.byId(provider)
        val model = prefs.getString(KEY_MODEL, null)
            ?.takeIf { AiModelOption.byId(it) != null }
            ?: AiModelOption.DEFAULT_MODEL_ID
        return AiSettings(
            providerId = provider,
            baseUrl = prefs.getString(KEY_BASE_URL, null)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: preset.baseUrl,
            model = model,
            dailyMessageLimit = prefs.getInt(
                KEY_DAILY_LIMIT,
                AiSettings.DEFAULT_DAILY_LIMIT,
            ).coerceIn(5, 500),
            autoSpeak = prefs.getBoolean(KEY_AUTO_SPEAK, false),
        )
    }

    private companion object {
        const val PREFS_NAME = "ai_settings"
        const val KEY_PROVIDER = "provider"
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_DAILY_LIMIT = "daily_limit"
        const val KEY_AUTO_SPEAK = "auto_speak"
    }
}
