/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.llm

import ai.koog.prompt.executor.clients.bedrock.BedrockLLMClient
import ai.koog.prompt.executor.clients.bedrock.BedrockModels
import ai.koog.prompt.executor.clients.dashscope.DashscopeLLMClient
import ai.koog.prompt.executor.clients.dashscope.DashscopeModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.mistralai.MistralAILLMClient
import ai.koog.prompt.executor.clients.mistralai.MistralAIModels
import com.storyteller_f.shared.model.LlmConfig
import com.storyteller_f.shared.model.LlmProvider
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

internal class OfficialProviderFactoryTest {
    @Test
    fun `new providers use catalog defaults and exact IDs`() {
        val cases =
            listOf(
                LlmProvider.BEDROCK to BedrockModels.OpenAIGptOss120B,
                LlmProvider.DEEPSEEK to DeepSeekModels.DeepSeekV4Flash,
                LlmProvider.MISTRAL_AI to MistralAIModels.Chat.MistralMedium31,
                LlmProvider.DASHSCOPE to DashscopeModels.QWEN_PLUS,
            )
        cases.forEach { (provider, expected) ->
            assertEquals(expected, KoogClientFactory.resolveModel(LlmConfig(provider)))
            assertEquals(expected, KoogClientFactory.resolveModel(LlmConfig(provider, model = expected.id)))
            assertFailsWith<IllegalArgumentException> {
                KoogClientFactory.resolveModel(LlmConfig(provider, model = "Custom/Model"))
            }
        }
    }

    @Test
    fun `new API providers create native clients`() {
        KoogClientFactory.createClient(LlmConfig(LlmProvider.DEEPSEEK, apiKey = "test-key")).use {
            assertIs<DeepSeekLLMClient>(it)
        }
        KoogClientFactory.createClient(LlmConfig(LlmProvider.MISTRAL_AI, apiKey = "test-key")).use {
            assertIs<MistralAILLMClient>(it)
        }
        KoogClientFactory.createClient(LlmConfig(LlmProvider.DASHSCOPE, apiKey = "test-key")).use {
            assertIs<DashscopeLLMClient>(it)
        }
    }

    @Test
    fun `Bedrock supports bearer and default credentials`() {
        listOf(null, "test-token").forEach { key ->
            KoogClientFactory.createClient(LlmConfig(LlmProvider.BEDROCK, apiKey = key, region = "us-west-2")).use {
                assertIs<BedrockLLMClient>(it)
            }
        }
    }

    @Test
    fun `API providers reject missing keys`() {
        listOf(LlmProvider.DEEPSEEK, LlmProvider.MISTRAL_AI, LlmProvider.DASHSCOPE).forEach { provider ->
            listOf(null, "", " ").forEach { key ->
                assertFailsWith<IllegalArgumentException> {
                    KoogClientFactory.createClient(LlmConfig(provider, apiKey = key))
                }
            }
        }
    }

    @Test
    fun `Bedrock rejects blank region and token`() {
        assertFailsWith<IllegalArgumentException> {
            KoogClientFactory.createClient(LlmConfig(LlmProvider.BEDROCK, region = " "))
        }
        assertFailsWith<IllegalArgumentException> {
            KoogClientFactory.createClient(LlmConfig(LlmProvider.BEDROCK, apiKey = " "))
        }
    }

    @Test
    fun `old configs remain compatible with optional region`() {
        val config = Json.decodeFromString<LlmConfig>("""{"provider":"OPENAI","model":"gpt-4o"}""")
        assertNull(config.region)
        val bedrock = LlmConfig(LlmProvider.BEDROCK, region = "us-west-2")
        assertEquals(bedrock, Json.decodeFromString<LlmConfig>(Json.encodeToString(bedrock)))
    }
}
