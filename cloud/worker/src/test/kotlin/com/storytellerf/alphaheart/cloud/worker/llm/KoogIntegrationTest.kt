/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.llm

import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.params.LLMParams
import com.storyteller_f.shared.model.LlmConfig
import com.storyteller_f.shared.model.LlmProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val OPENROUTER_MODEL_ID = "dots-studio/dots-3-note-preview:free"

internal class KoogIntegrationTest {
    @BeforeTest
    fun setUpLogPath() {
        val logPath = File("build/test/logs").canonicalPath
        System.setProperty("LOG_PATH", logPath)
    }

    @Test
    fun `test OpenAI client creation`() {
        val config =
            LlmConfig(
                provider = LlmProvider.OPENAI,
                apiKey = "test-key",
                model = "gpt-4o",
            )

        val client = KoogClientFactory.createClient(config)
        assertNotNull(client)
    }

    @Test
    fun `test Anthropic client creation`() {
        val config =
            LlmConfig(
                provider = LlmProvider.ANTHROPIC,
                apiKey = "test-key",
                model = "claude-sonnet-4-0",
            )

        val client = KoogClientFactory.createClient(config)
        assertNotNull(client)
    }

    @Test
    fun `test Google client creation`() {
        val config =
            LlmConfig(
                provider = LlmProvider.GOOGLE,
                apiKey = "test-key",
                model = "gemini-2.5-flash",
            )

        val client = KoogClientFactory.createClient(config)
        assertIs<GoogleLLMClient>(client)
        assertEquals(LLMProvider.Google, client.llmProvider())
        client.close()
    }

    @Test
    fun `test Ollama client creation`() {
        val config =
            LlmConfig(
                provider = LlmProvider.OLLAMA,
                baseUrl = "http://localhost:11434",
                model = "llama3.2:latest",
            )

        val client = KoogClientFactory.createClient(config)
        assertNotNull(client)
    }

    @Test
    fun `test OpenAI-compatible client creation`() {
        val config =
            LlmConfig(
                provider = LlmProvider.OPENAI_COMPATIBLE,
                apiKey = "test-key",
                baseUrl = "https://llm.example.com/v1",
                model = "gpt-3.5-turbo",
            )

        val client = KoogClientFactory.createClient(config)
        assertNotNull(client)
    }

    @Test
    fun `OpenRouter client ignores configured base URL`() {
        val config =
            LlmConfig(
                provider = LlmProvider.OPENROUTER,
                apiKey = "test-key",
                baseUrl = " ",
                model = OPENROUTER_MODEL_ID,
            )

        val client = KoogClientFactory.createClient(config)

        assertEquals(LLMProvider.OpenRouter, client.llmProvider())
        client.close()
    }

    @Test
    fun `OpenRouter model supports structured output`() {
        val config =
            LlmConfig(
                provider = LlmProvider.OPENROUTER,
                apiKey = "test-key",
                model = OPENROUTER_MODEL_ID,
            )

        val model = KoogClientFactory.resolveModel(config)

        assertEquals(OPENROUTER_MODEL_ID, model.id)
        assertEquals(LLMProvider.OpenRouter, model.provider)
        assertTrue(model.supports(LLMCapability.Schema.JSON.Standard))
    }

    @Test
    fun `known models retain SDK metadata`() {
        val cases =
            listOf(
                LlmProvider.OPENAI to OpenAIModels.Chat.GPT4o,
                LlmProvider.OPENAI to OpenAIModels.Chat.O3,
                LlmProvider.ANTHROPIC to AnthropicModels.Sonnet_4,
                LlmProvider.GOOGLE to GoogleModels.Gemini2_5Flash,
                LlmProvider.OLLAMA to OllamaModels.models.first(),
            )
        cases.forEach { (provider, expected) ->
            val actual = KoogClientFactory.resolveModel(LlmConfig(provider = provider, model = expected.id))
            assertEquals(expected, actual, "$provider model ${expected.id}")
        }
    }

    @Test
    fun `compatible endpoints treat model IDs as opaque`() {
        listOf("gpt-4o", "o3", "Custom/Model-V1").forEach { id ->
            val model = KoogClientFactory.resolveModel(LlmConfig(LlmProvider.OPENAI_COMPATIBLE, model = id))
            assertEquals(id, model.id)
            assertNull(model.capabilities)
            assertNull(model.contextLength)
            assertNull(model.maxOutputTokens)
        }
    }

    @Test
    fun `OpenRouter preserves IDs without guessed limits`() {
        listOf(OPENROUTER_MODEL_ID, "gpt-4o", "Custom/Model-V1").forEach { id ->
            val model = KoogClientFactory.resolveModel(LlmConfig(LlmProvider.OPENROUTER, model = id))
            assertEquals(id, model.id)
            assertEquals(LLMProvider.OpenRouter, model.provider)
            assertNull(model.contextLength)
            assertNull(model.maxOutputTokens)
        }
    }

    @Test
    fun `OpenRouter requires a nonblank ID`() {
        listOf(null, "", " ").forEach { id ->
            assertFailsWith<IllegalArgumentException> {
                KoogClientFactory.resolveModel(LlmConfig(LlmProvider.OPENROUTER, model = id))
            }
        }
    }

    @Test
    fun `catalog providers reject unknown model IDs`() {
        val providers =
            listOf(
                LlmProvider.OPENAI,
                LlmProvider.ANTHROPIC,
                LlmProvider.GOOGLE,
                LlmProvider.OLLAMA,
            )
        providers.forEach { provider ->
            listOf("Custom/Model-V1", "GPT-4o", " ", "claude-sonnet-4-20250514").forEach { id ->
                val failure =
                    assertFailsWith<IllegalArgumentException> {
                        KoogClientFactory.resolveModel(LlmConfig(provider = provider, model = id))
                    }
                assertTrue(failure.message.orEmpty().contains("model"))
            }
        }
    }

    @Test
    fun `catalog IDs cannot cross providers`() {
        assertFailsWith<IllegalArgumentException> {
            KoogClientFactory.resolveModel(LlmConfig(LlmProvider.ANTHROPIC, model = OpenAIModels.Chat.GPT4o.id))
        }
    }

    @Test
    fun `default models use SDK definitions`() {
        assertEquals(OpenAIModels.Chat.GPT4o, KoogClientFactory.resolveModel(LlmConfig(LlmProvider.OPENAI)))
        assertEquals(AnthropicModels.Sonnet_4, KoogClientFactory.resolveModel(LlmConfig(LlmProvider.ANTHROPIC)))
        assertEquals(GoogleModels.Gemini2_5Flash, KoogClientFactory.resolveModel(LlmConfig(LlmProvider.GOOGLE)))
        assertEquals(OllamaModels.Meta.LLAMA_3_2, KoogClientFactory.resolveModel(LlmConfig(LlmProvider.OLLAMA)))
        assertEquals(
            "gpt-3.5-turbo",
            KoogClientFactory.resolveModel(LlmConfig(LlmProvider.OPENAI_COMPATIBLE)).id,
        )
    }

    @Test
    fun `LiteRT model uses local provider and filename`() {
        val config =
            LlmConfig(
                provider = LlmProvider.LITERT_LLM,
                modelPath = "models/safety.litertlm",
            )

        val model = KoogClientFactory.resolveModel(config)

        assertEquals(LITE_RT_LLM_PROVIDER, model.provider)
        assertEquals("safety.litertlm", model.id)
        assertEquals(LITE_RT_MODEL_CONTEXT_SIZE, model.contextLength)
        assertEquals(LITE_RT_MODEL_MAX_OUTPUT_TOKENS, model.maxOutputTokens)
    }

    @Test
    fun `LiteRT config without cache path is compatible`() {
        val config =
            Json.decodeFromString<LlmConfig>(
                """{"provider":"LITERT_LLM","modelPath":"models/safety.litertlm"}""",
            )

        assertNull(config.cachePath)
    }

    @Test
    fun `OpenAI format contains JSON schema`() {
        val schema =
            LLMParams.Schema.JSON.Standard(
                name = "decision",
                schema = buildJsonObject { put("type", "object") },
            )

        val responseFormat = assertNotNull(schema.toOpenAIResponseFormat())

        val responseType = assertNotNull(responseFormat["type"])
        val jsonSchema = assertNotNull(responseFormat["json_schema"]).jsonObject
        val schemaName = assertNotNull(jsonSchema["name"])
        val strict = assertNotNull(jsonSchema["strict"])
        val schemaObject = assertNotNull(jsonSchema["schema"]).jsonObject
        val schemaType = assertNotNull(schemaObject["type"])
        assertEquals("json_schema", responseType.jsonPrimitive.content)
        assertEquals("decision", schemaName.jsonPrimitive.content)
        assertEquals("true", strict.jsonPrimitive.content)
        assertEquals("object", schemaType.jsonPrimitive.content)
    }

    @Test
    fun `OpenAI format absent without schema`() {
        assertNull(null.toOpenAIResponseFormat())
    }
}
