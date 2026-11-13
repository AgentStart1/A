/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.llm

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.LLModelDefinitions
import ai.koog.prompt.executor.clients.anthropic.AnthropicClientSettings
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.bedrock.BedrockModels
import ai.koog.prompt.executor.clients.dashscope.DashscopeModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleClientSettings
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.mistralai.MistralAIModels
import ai.koog.prompt.executor.clients.modelsById
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import com.storyteller_f.shared.model.LlmConfig
import com.storyteller_f.shared.model.LlmProvider
import io.github.aakira.napier.Napier
import java.nio.file.Path

/**
 * Factory for creating LLM clients based on configuration.
 * Uses koog's LLMClient interface directly for simple text generation.
 *
 */
object KoogClientFactory {
    /**
     * Creates an LLM client for the given configuration.
     */
    fun createClient(config: LlmConfig): LLMClient = createClient(config, null)

    // Transport injection keeps factory execution tests on the production authentication path.
    internal fun createClient(config: LlmConfig, bedrockHttpEngine: HttpClientEngine?): LLMClient {
        val client =
            when (config.provider) {
                LlmProvider.OPENAI -> createOpenAiClient(config)
                LlmProvider.ANTHROPIC -> createAnthropicClient(config)
                LlmProvider.GOOGLE -> createGoogleClient(config)
                LlmProvider.BEDROCK -> createBedrockClient(config, bedrockHttpEngine)
                LlmProvider.DEEPSEEK -> createDeepSeekClient(config)
                LlmProvider.MISTRAL_AI -> createMistralClient(config)
                LlmProvider.DASHSCOPE -> createDashscopeClient(config)
                LlmProvider.OLLAMA -> createOllamaClient(config)
                LlmProvider.OPENAI_COMPATIBLE -> createOpenAICompatibleClient(config)
                LlmProvider.OPENROUTER -> createOpenRouterClient(config)
                LlmProvider.LITERT_LLM -> createLiteRtClient(config)
            }
        return client
    }

    /**
     * Requires SDK catalog IDs for direct providers; router and compatible IDs remain opaque.
     */
    fun resolveModel(config: LlmConfig): LLModel =
        when (config.provider) {
            LlmProvider.OPENAI -> resolveCatalogModel(OpenAIModels, config, OpenAIModels.Chat.GPT4o)

            LlmProvider.ANTHROPIC -> resolveCatalogModel(AnthropicModels, config, AnthropicModels.Sonnet_4)

            LlmProvider.GOOGLE -> resolveCatalogModel(GoogleModels, config, GoogleModels.Gemini2_5Flash)

            LlmProvider.OLLAMA -> resolveCatalogModel(OllamaModels, config, OllamaModels.Meta.LLAMA_3_2)

            LlmProvider.BEDROCK -> resolveCatalogModel(BedrockModels, config, BedrockModels.OpenAIGptOss120B)

            LlmProvider.DEEPSEEK -> resolveCatalogModel(DeepSeekModels, config, DeepSeekModels.DeepSeekV4Flash)

            LlmProvider.MISTRAL_AI -> resolveCatalogModel(MistralAIModels, config, MistralAIModels.Chat.MistralMedium31)

            LlmProvider.DASHSCOPE -> resolveCatalogModel(DashscopeModels, config, DashscopeModels.QWEN_PLUS)

            LlmProvider.OPENAI_COMPATIBLE ->
                LLModel(provider = LLMProvider.OpenAI, id = config.model ?: "gpt-3.5-turbo")

            LlmProvider.OPENROUTER ->
                LLModel(
                    provider = LLMProvider.OpenRouter,
                    id = requireOpenRouterModel(config),
                    // Koog checks these before sending our completion/schema requests; the router validates model support.
                    capabilities = listOf(LLMCapability.Completion, LLMCapability.Schema.JSON.Standard),
                )

            LlmProvider.LITERT_LLM -> resolveLiteRtModel(config)
        }

    private fun createOpenAiClient(config: LlmConfig): LLMClient {
        val apiKey = config.apiKey ?: error("API key required for OpenAI")
        val settings =
            OpenAIClientSettings(
                baseUrl = config.baseUrl ?: "https://api.openai.com/v1",
            )
        val httpClientFactory = KtorKoogHttpClient.Factory()

        Napier.i(tag = "koog") {
            "Creating OpenAI client with model: ${config.model ?: "gpt-4o"}"
        }

        return OpenAILLMClient(apiKey, settings, httpClientFactory)
    }

    private fun createAnthropicClient(config: LlmConfig): LLMClient {
        val apiKey = config.apiKey ?: error("API key required for Anthropic")
        val settings =
            AnthropicClientSettings(
                baseUrl = config.baseUrl ?: "https://api.anthropic.com",
            )
        val httpClientFactory = KtorKoogHttpClient.Factory()

        Napier.i(tag = "koog") {
            "Creating Anthropic client with model: ${config.model ?: AnthropicModels.Sonnet_4.id}"
        }

        return AnthropicLLMClient(apiKey, settings, httpClientFactory)
    }

    private fun createGoogleClient(config: LlmConfig): LLMClient {
        val apiKey = config.apiKey ?: error("API key required for Google")
        val settings =
            GoogleClientSettings(
                baseUrl = config.baseUrl ?: "https://generativelanguage.googleapis.com",
            )
        return GoogleLLMClient(
            apiKey = apiKey,
            settings = settings,
            httpClientFactory = KtorKoogHttpClient.Factory(),
        )
    }

    private fun createOllamaClient(config: LlmConfig): LLMClient {
        val baseUrl = config.baseUrl ?: "http://localhost:11434"
        val httpClientFactory = KtorKoogHttpClient.Factory()

        Napier.i(tag = "koog") {
            "Creating Ollama client at $baseUrl with model: ${config.model ?: OllamaModels.Meta.LLAMA_3_2.id}"
        }

        return ai.koog.prompt.executor.ollama.client.OllamaClient(
            httpClientFactory = httpClientFactory,
            baseUrl = baseUrl,
        )
    }

    private fun createOpenAICompatibleClient(config: LlmConfig): LLMClient {
        val baseUrl =
            config.baseUrl
                ?: error("Base URL required for OpenAI-compatible provider")
        val apiKey = config.apiKey ?: "no-key"

        Napier.i(tag = "koog") {
            "Creating OpenAI-compatible client at $baseUrl with model: ${config.model ?: "gpt-3.5-turbo"}"
        }

        // Bypass koog's parameter determination logic for non-standard OpenAI-compatible model names.
        return createOpenAICompatibleClient(
            apiKey = apiKey,
            baseUrl = baseUrl,
        )
    }

    private fun createOpenRouterClient(config: LlmConfig): LLMClient {
        val apiKey = config.apiKey ?: error("API key required for OpenRouter")
        val model = requireOpenRouterModel(config)

        Napier.i(tag = "koog") {
            "Creating OpenRouter client with model: $model"
        }

        return OpenRouterLLMClient(
            apiKey = apiKey,
            httpClientFactory = KtorKoogHttpClient.Factory(),
        )
    }

    private fun createLiteRtClient(config: LlmConfig): LLMClient {
        val modelPath = config.modelPath ?: error("Model path required for LiteRT")
        val cachePath =
            config.cachePath?.let { configuredPath ->
                require(configuredPath.isNotBlank()) { "Cache path must not be blank for LiteRT" }
                Path.of(configuredPath)
            }
        return LiteRtLlmClient.create(
            modelPath = Path.of(modelPath),
            cachePath = cachePath,
        )
    }

    private fun resolveLiteRtModel(config: LlmConfig): LLModel {
        val modelPath = config.modelPath ?: error("Model path required for LiteRT")
        return LLModel(
            provider = LITE_RT_LLM_PROVIDER,
            id = config.model ?: Path.of(modelPath).fileName.toString(),
            capabilities =
            listOf(
                ai.koog.prompt.llm.LLMCapability.Completion,
            ),
            contextLength = LITE_RT_MODEL_CONTEXT_SIZE,
            maxOutputTokens = LITE_RT_MODEL_MAX_OUTPUT_TOKENS,
        )
    }

    private fun requireOpenRouterModel(config: LlmConfig): String =
        requireNotNull(config.model?.takeIf { it.isNotBlank() }) { "OpenRouter model ID is required" }

    private fun resolveCatalogModel(
        definitions: LLModelDefinitions,
        config: LlmConfig,
        defaultModel: LLModel,
    ): LLModel {
        val modelName = config.model ?: return defaultModel
        return requireNotNull(definitions.modelsById()[modelName]) {
            "Unknown ${config.provider} model ID: $modelName. Use an exact Koog catalog ID."
        }
    }
}
