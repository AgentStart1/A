/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.llm

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.bedrock.BedrockAPIMethod
import ai.koog.prompt.executor.clients.bedrock.BedrockClientSettings
import ai.koog.prompt.executor.clients.bedrock.BedrockLLMClient
import ai.koog.prompt.executor.clients.bedrock.StaticBearerTokenProvider
import ai.koog.prompt.executor.clients.dashscope.DashscopeClientSettings
import ai.koog.prompt.executor.clients.dashscope.DashscopeLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekClientSettings
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.mistralai.MistralAIClientSettings
import ai.koog.prompt.executor.clients.mistralai.MistralAILLMClient
import aws.sdk.kotlin.services.bedrockruntime.BedrockRuntimeClient
import aws.smithy.kotlin.runtime.auth.AuthSchemeId
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import com.storyteller_f.shared.model.LlmConfig
import kotlin.time.Duration.Companion.milliseconds

internal fun createDeepSeekClient(config: LlmConfig): LLMClient =
    DeepSeekLLMClient(
        apiKey = requireProviderKey(config),
        settings = DeepSeekClientSettings(baseUrl = config.baseUrl ?: DeepSeekClientSettings().baseUrl),
        httpClientFactory = KtorKoogHttpClient.Factory(),
    )

internal fun createMistralClient(config: LlmConfig): LLMClient =
    MistralAILLMClient(
        apiKey = requireProviderKey(config),
        settings = MistralAIClientSettings(baseUrl = config.baseUrl ?: MistralAIClientSettings().baseUrl),
        httpClientFactory = KtorKoogHttpClient.Factory(),
    )

internal fun createDashscopeClient(config: LlmConfig): LLMClient =
    DashscopeLLMClient(
        apiKey = requireProviderKey(config),
        settings = DashscopeClientSettings(baseUrl = config.baseUrl ?: DashscopeClientSettings().baseUrl),
        httpClientFactory = KtorKoogHttpClient.Factory(),
    )

internal fun createBedrockClient(config: LlmConfig, httpEngine: HttpClientEngine? = null): LLMClient {
    val settings = BedrockClientSettings()
    val region = config.region ?: settings.region
    require(region.isNotBlank()) { "Bedrock region must not be blank" }
    val apiKey = config.apiKey
    require(apiKey == null || apiKey.isNotBlank()) { "Bedrock API key must not be blank" }
    val endpoint = config.baseUrl?.let(Url::parse)
    val runtime =
        BedrockRuntimeClient {
            this.region = region
            endpointUrl = endpoint
            // Let the SDK bind its default credential chain to this region and own its lifecycle.
            if (apiKey != null) {
                bearerTokenProvider = StaticBearerTokenProvider(apiKey)
                authSchemePreference = listOf(AuthSchemeId.HttpBearer)
            }
            retryStrategy = StandardRetryStrategy { maxAttempts = 1 }
            val timeouts = settings.timeoutConfig
            callTimeout = timeouts.requestTimeoutMillis.milliseconds
            if (httpEngine != null) {
                httpClient = httpEngine
            } else {
                httpClient {
                    connectTimeout = timeouts.connectTimeoutMillis.milliseconds
                    socketReadTimeout = timeouts.socketTimeoutMillis.milliseconds
                    socketWriteTimeout = timeouts.socketTimeoutMillis.milliseconds
                }
            }
        }
    return BedrockLLMClient(runtime, apiMethod = BedrockAPIMethod.Converse)
}

private fun requireProviderKey(config: LlmConfig): String =
    requireNotNull(config.apiKey?.takeIf { it.isNotBlank() }) { "API key required for ${config.provider}" }
