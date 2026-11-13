/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.llm

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.params.LLMParams
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.SamplerConfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat

internal val LITE_RT_LLM_PROVIDER = LLMProvider("litert", "LiteRT")

internal class LiteRtLlmClient private constructor(private val engine: Engine) : LLMClient() {
    private val executionMutex = Mutex()

    override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): Message.Assistant {
        require(tools.isEmpty()) { "LiteRT JVM client does not support tools" }
        val systemInstruction = prompt.systemInstruction()
        val userMessage = prompt.singleUserMessage()
        val responseFormat = prompt.liteRtResponseFormat()
        val samplerConfig = prompt.liteRtSamplerConfig()
        val conversationConfig =
            ConversationConfig(
                systemInstruction = systemInstruction.takeIf { it.isNotBlank() }?.let { Contents.of(it) },
                samplerConfig = samplerConfig,
                maxOutputToken = prompt.params.maxTokens,
                enableResponseFormat = responseFormat != null,
            )
        val response =
            executionMutex.withLock {
                withContext(Dispatchers.Default) {
                    engine.createConversation(conversationConfig).use { conversation ->
                        conversation.sendMessage(
                            text = userMessage,
                            responseFormat = responseFormat,
                        )
                    }
                }
            }
        val responseText =
            response.contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString(separator = "") { it.text }
        return Message.Assistant(
            content = responseText,
            metaInfo = ResponseMetaInfo.Empty,
            finishReason = LITE_RT_FINISH_REASON,
        )
    }

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        throw UnsupportedOperationException("LiteRT JVM client does not provide a moderation API")

    override fun llmProvider(): LLMProvider = LITE_RT_LLM_PROVIDER

    override fun close() {
        Napier.i(tag = "litert") {
            "Closing LiteRT LLM client"
        }
        engine.close()
    }

    companion object {
        fun create(modelPath: Path, cachePath: Path? = null): LiteRtLlmClient {
            val absoluteModelPath = modelPath.toAbsolutePath().normalize()
            val engine =
                createWithLiteRtCacheAndPreferredBackend(
                    modelPath = absoluteModelPath,
                    configuredCacheDirectory = cachePath,
                ) { cacheDirectory, backend ->
                    createInitializedLiteRtEngine(
                        modelPath = absoluteModelPath,
                        cacheDirectory = cacheDirectory,
                        backend = backend,
                    )
                }
            return LiteRtLlmClient(engine)
        }
    }
}

internal fun <T> createWithPreferredLiteRtBackend(create: (Backend) -> T): T =
    try {
        create(Backend.GPU())
    } catch (exception: Exception) {
        Napier.w(tag = "litert", throwable = exception) {
            "LiteRT GPU initialization failed; falling back to CPU"
        }
        create(Backend.CPU())
    }

internal fun <T> createWithLiteRtCacheAndPreferredBackend(
    modelPath: Path,
    configuredCacheDirectory: Path?,
    temporaryDirectory: Path = systemTemporaryDirectory(),
    create: (Path, Backend) -> T,
): T {
    val cacheDirectory =
        resolveLiteRtCacheDirectory(
            modelPath = modelPath,
            configuredCacheDirectory = configuredCacheDirectory,
            temporaryDirectory = temporaryDirectory,
        )
    prepareLiteRtCacheDirectory(cacheDirectory)
    return createWithPreferredLiteRtBackend { backend -> create(cacheDirectory, backend) }
}

internal fun resolveLiteRtCacheDirectory(
    modelPath: Path,
    configuredCacheDirectory: Path?,
    temporaryDirectory: Path,
): Path =
    configuredCacheDirectory?.toAbsolutePath()?.normalize()
        ?: temporaryDirectory
            .toAbsolutePath()
            .normalize()
            .resolve(DEFAULT_CACHE_APPLICATION_DIRECTORY)
            .resolve(DEFAULT_CACHE_PROVIDER_DIRECTORY)
            .resolve(modelCacheKey(modelPath))

internal fun prepareLiteRtCacheDirectory(cacheDirectory: Path) {
    val preparedDirectory =
        try {
            Files.createDirectories(cacheDirectory)
        } catch (exception: IOException) {
            throw IllegalStateException("Unable to prepare LiteRT cache directory: $cacheDirectory", exception)
        }
    check(Files.isDirectory(preparedDirectory) && Files.isWritable(preparedDirectory)) {
        "LiteRT cache directory is not writable: $preparedDirectory"
    }
}

private fun createInitializedLiteRtEngine(modelPath: Path, cacheDirectory: Path, backend: Backend): Engine {
    Napier.i(tag = "litert") {
        "Initializing LiteRT LLM client with ${backend.name} backend"
    }
    val engine =
        Engine(
            EngineConfig(
                modelPath = modelPath.toString(),
                backend = backend,
                maxNumTokens = MODEL_CONTEXT_SIZE,
                cacheDir = cacheDirectory.toString(),
            ),
        )
    engine.initialize()
    Napier.i(tag = "litert") {
        "Initialized LiteRT LLM client with ${backend.name} backend"
    }
    return engine
}

internal fun Prompt.systemInstruction(): String =
    messages
        .filterIsInstance<Message.System>()
        .joinToString(separator = "\n") { it.textContent() }

internal fun Prompt.liteRtSamplerConfig(): SamplerConfig =
    SamplerConfig(
        topK = DEFAULT_TOP_K,
        topP = DEFAULT_TOP_P,
        temperature = params.temperature ?: DEFAULT_TEMPERATURE,
        seed = DEFAULT_SEED,
    )

internal fun Prompt.liteRtResponseFormat(): ResponseFormat? =
    when (val schema = params.schema) {
        null -> null

        is LLMParams.Schema.JSON.Standard -> ResponseFormat.json(schema.schema.toString())

        else ->
            throw UnsupportedOperationException(
                "LiteRT JVM client supports only standard JSON response schemas",
            )
    }

internal fun Prompt.singleUserMessage(): String {
    val unsupportedMessages = messages.filterNot { it is Message.System || it is Message.User }
    require(unsupportedMessages.isEmpty()) {
        "LiteRT JVM client supports only system and user messages"
    }
    val userMessages = messages.filterIsInstance<Message.User>()
    require(userMessages.size == 1) {
        "LiteRT JVM client requires exactly one user message"
    }
    return userMessages.single().textContent()
}

internal const val LITE_RT_MODEL_CONTEXT_SIZE = 4_096L
internal const val LITE_RT_MODEL_MAX_OUTPUT_TOKENS = 1_024L
private const val MODEL_CONTEXT_SIZE = 4_096
private const val DEFAULT_CACHE_APPLICATION_DIRECTORY = "a-worker"
private const val DEFAULT_CACHE_PROVIDER_DIRECTORY = "litertlm"
private const val TEMPORARY_DIRECTORY_PROPERTY = "java.io.tmpdir"
private const val DEFAULT_TOP_K = 1
private const val DEFAULT_TOP_P = 1.0
private const val DEFAULT_TEMPERATURE = 0.0
private const val DEFAULT_SEED = 0
private const val LITE_RT_FINISH_REASON = "stop"

private fun systemTemporaryDirectory(): Path {
    val configuredDirectory =
        checkNotNull(System.getProperty(TEMPORARY_DIRECTORY_PROPERTY)) {
            "JVM temporary directory property is unavailable"
        }
    require(configuredDirectory.isNotBlank()) { "JVM temporary directory property is blank" }
    return Path.of(configuredDirectory)
}

private fun modelCacheKey(modelPath: Path): String {
    val normalizedModelPath = modelPath.toAbsolutePath().normalize().toString()
    val digest =
        MessageDigest
            .getInstance("SHA-256")
            .digest(normalizedModelPath.toByteArray(StandardCharsets.UTF_8))
    return HexFormat.of().formatHex(digest)
}
