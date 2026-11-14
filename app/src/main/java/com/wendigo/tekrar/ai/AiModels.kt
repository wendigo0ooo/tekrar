package com.wendigo.tekrar.ai

enum class AiRole(val wire: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    ;

    companion object {
        fun fromWire(value: String?): AiRole? = when (value?.trim()?.lowercase()) {
            "system" -> SYSTEM
            "user" -> USER
            "assistant" -> ASSISTANT
            else -> null
        }
    }
}

data class AiChatMessage(
    val role: AiRole,
    val content: String,
)

data class AiProviderPreset(
    val id: String,
    val label: String,
    val baseUrl: String,
    val docsUrl: String? = null,
) {
    companion object {
        const val OPEN_CODE_GO_ID = "open_code_go"
        const val CUSTOM_ID = "custom"

        val OPEN_CODE_GO = AiProviderPreset(
            id = OPEN_CODE_GO_ID,
            label = "OpenCode Go",
            baseUrl = "https://opencode.ai/inference/go",
        )

        val CUSTOM = AiProviderPreset(
            id = CUSTOM_ID,
            label = "Özel (OpenAI uyumlu)",
            baseUrl = "",
        )

        val presets: List<AiProviderPreset> = listOf(OPEN_CODE_GO, CUSTOM)

        fun byId(id: String?): AiProviderPreset =
            presets.firstOrNull { it.id == id } ?: OPEN_CODE_GO
    }
}

data class AiModelOption(
    val id: String,
    val label: String,
    val noteTr: String,
) {
    companion object {
        const val DEFAULT_MODEL_ID = "gpt-6-luna"

        val GPT_6_LUNA = AiModelOption(
            id = DEFAULT_MODEL_ID,
            label = "GPT-6 Luna",
            noteTr = "Varsayılan · hızlı",
        )
        val GLM_5_3_FLASH = AiModelOption(
            id = "glm-5.3-flash",
            label = "GLM-5.3 Flash",
            noteTr = "Dengeli ve hızlı",
        )
        val DEEPSEEK_V4_1_FLASH = AiModelOption(
            id = "deepseek-v4.1-flash",
            label = "DeepSeek V4.1 Flash",
            noteTr = "Ekonomik",
        )
        val MINIMAX_M3_1_FLASH = AiModelOption(
            id = "minimax-m3.1-flash",
            label = "MiniMax M3.1 Flash",
            noteTr = "Uzun yanıtlarda dengeli",
        )

        val options: List<AiModelOption> = listOf(
            GPT_6_LUNA,
            GLM_5_3_FLASH,
            DEEPSEEK_V4_1_FLASH,
            MINIMAX_M3_1_FLASH,
        )

        fun byId(id: String?): AiModelOption? = options.firstOrNull { it.id == id }
    }
}

data class AiConfig(
    val providerId: String = AiProviderPreset.OPEN_CODE_GO_ID,
    val baseUrl: String = AiProviderPreset.OPEN_CODE_GO.baseUrl,
    val apiKey: String,
    val model: String = AiModelOption.DEFAULT_MODEL_ID,
    val temperature: Double = DEFAULT_TEMPERATURE,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
) {
    init {
        require(baseUrl.trim().startsWith("https://", ignoreCase = true)) {
            "Sağlayıcı adresi https:// ile başlamalı."
        }
        require(apiKey.isNotBlank()) { "AI anahtarı boş olamaz." }
        require(apiKey.none { it == '\n' || it == '\r' }) {
            "AI anahtarı geçersiz karakter içeriyor."
        }
        require(model.isNotBlank()) { "Model seçilmedi." }
        require(temperature.isFinite() && temperature in 0.0..2.0) {
            "Sıcaklık 0.0 ile 2.0 arasında olmalı."
        }
        require(maxTokens in 1..4096) { "En fazla token 1 ile 4096 arasında olmalı." }
    }

    fun chatCompletionsUrl(): String {
        val base = baseUrl.trim().trimEnd('/')
        return "$base/openai/v1/chat/completions"
    }

    override fun toString(): String =
        "AiConfig(providerId=$providerId, baseUrl=$baseUrl, apiKey=***, model=$model, " +
            "temperature=$temperature, maxTokens=$maxTokens)"

    companion object {
        const val DEFAULT_TEMPERATURE = 0.6
        const val DEFAULT_MAX_TOKENS = 160

        fun openCodeGo(
            apiKey: String,
            model: String = AiModelOption.DEFAULT_MODEL_ID,
            temperature: Double = DEFAULT_TEMPERATURE,
            maxTokens: Int = DEFAULT_MAX_TOKENS,
        ): AiConfig = AiConfig(
            providerId = AiProviderPreset.OPEN_CODE_GO_ID,
            baseUrl = AiProviderPreset.OPEN_CODE_GO.baseUrl,
            apiKey = apiKey,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens,
        )
    }
}

sealed class AiStreamEvent {
    data class Delta(val text: String) : AiStreamEvent()

    data class Done(
        val finishReason: String? = null,
        val usageJson: String? = null,
    ) : AiStreamEvent()

    data class Error(
        val messageTr: String,
        val retryable: Boolean,
        val httpCode: Int? = null,
    ) : AiStreamEvent()
}
