package com.wendigo.tekrar.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AiConfigTest {

    @Test
    fun buildsChatCompletionsUrlForDefaultProvider() {
        val config = AiConfig(apiKey = "oc_sk_test")

        assertEquals(
            "https://opencode.ai/inference/go/openai/v1/chat/completions",
            config.chatCompletionsUrl(),
        )
    }

    @Test
    fun trimsTrailingSlashesAndSpacesFromBaseUrl() {
        val config = AiConfig(
            apiKey = "oc_sk_test",
            baseUrl = "  https://example.com/api///  ",
        )

        assertEquals(
            "https://example.com/api/openai/v1/chat/completions",
            config.chatCompletionsUrl(),
        )
    }

    @Test
    fun rejectsNonHttpsBaseUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            AiConfig(apiKey = "oc_sk_test", baseUrl = "http://localhost:8080")
        }
    }

    @Test
    fun rejectsBlankApiKey() {
        assertThrows(IllegalArgumentException::class.java) {
            AiConfig(apiKey = "   ")
        }
    }

    @Test
    fun rejectsApiKeyWithLineBreaks() {
        assertThrows(IllegalArgumentException::class.java) {
            AiConfig(apiKey = "oc_sk_test\nInjected: 1")
        }
    }

    @Test
    fun rejectsBlankModel() {
        assertThrows(IllegalArgumentException::class.java) {
            AiConfig(apiKey = "oc_sk_test", model = "  ")
        }
    }

    @Test
    fun rejectsOutOfRangeSamplingValues() {
        assertThrows(IllegalArgumentException::class.java) {
            AiConfig(apiKey = "oc_sk_test", temperature = 3.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AiConfig(apiKey = "oc_sk_test", maxTokens = 0)
        }
    }

    @Test
    fun defaultsMatchContract() {
        val config = AiConfig(apiKey = "oc_sk_test")

        assertEquals(AiProviderPreset.OPEN_CODE_GO_ID, config.providerId)
        assertEquals("https://opencode.ai/inference/go", config.baseUrl)
        assertEquals("gpt-6-luna", config.model)
        assertEquals(0.6, config.temperature, 0.0001)
        assertEquals(160, config.maxTokens)
    }

    @Test
    fun factoryBuildsOpenCodeGoConfig() {
        val config = AiConfig.openCodeGo(apiKey = "oc_sk_test", model = "glm-5.3-flash")

        assertEquals(AiProviderPreset.OPEN_CODE_GO_ID, config.providerId)
        assertEquals("glm-5.3-flash", config.model)
    }

    @Test
    fun toStringMasksApiKey() {
        val config = AiConfig(apiKey = "oc_sk_secret_123")

        val text = config.toString()
        assertFalse(text.contains("oc_sk_secret_123"))
        assertTrue(text.contains("apiKey=***"))
    }

    @Test
    fun modelOptionsCoverContractModels() {
        assertEquals(
            setOf(
                "gpt-6-luna",
                "glm-5.3-flash",
                "deepseek-v4.1-flash",
                "minimax-m3.1-flash",
            ),
            AiModelOption.options.map { it.id }.toSet(),
        )
        assertEquals("gpt-6-luna", AiModelOption.DEFAULT_MODEL_ID)
        assertEquals("gpt-6-luna", AiModelOption.byId("gpt-6-luna")?.id)
        assertNull(AiModelOption.byId("yok-boyle-model"))
    }

    @Test
    fun providerPresetsExposeDefaultAndCustom() {
        assertEquals(
            "https://opencode.ai/inference/go",
            AiProviderPreset.byId(null).baseUrl,
        )
        assertEquals(
            AiProviderPreset.CUSTOM_ID,
            AiProviderPreset.byId(AiProviderPreset.CUSTOM_ID).id,
        )
        assertTrue(AiProviderPreset.presets.size >= 2)
    }

    @Test
    fun rolesUseWireNames() {
        assertEquals("system", AiRole.SYSTEM.wire)
        assertEquals("user", AiRole.USER.wire)
        assertEquals("assistant", AiRole.ASSISTANT.wire)
        assertEquals(AiRole.USER, AiRole.fromWire(" USER "))
        assertNull(AiRole.fromWire("tool"))
    }
}
