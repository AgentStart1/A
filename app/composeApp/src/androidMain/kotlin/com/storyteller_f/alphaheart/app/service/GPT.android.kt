/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.service

import com.google.ai.edge.litertlm.Backend.GPU
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.storyteller_f.alphaheart.app.error.ModelGenerationError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.mapError
import com.storyteller_f.shared.getAppContextRefValue
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import java.io.File

actual fun buildGPT(): GPT = AndroidEdgeGPT()

actual fun getGPTModelDirectory(): Path {
    val context = getAppContextRefValue() ?: error("context is nil")
    return Path(context.filesDir.resolve("llm").absolutePath)
}

class AndroidEdgeGPT : GPT {
    override val supportList: List<String> = listOf("litertlm")

    override suspend fun generate(path: String, prompt: String): CustomResult<Flow<GPTOutput>, ModelGenerationError> {
        val application =
            getAppContextRefValue()
                ?: return CustomResult.Failure(
                    ModelGenerationError.from(UnsupportedOperationException()),
                ).mapError(ModelGenerationError::fromError)
        val file = File(path)
        if (!file.exists()) {
            return CustomResult.Failure(
                ModelGenerationError.from(Exception("modal not exists")),
            ).mapError(ModelGenerationError::fromError)
        }
        val engineConfig =
            EngineConfig(
                modelPath = path,
                backend = GPU(),
                cacheDir = application.cacheDir.path,
            )
        val engine = Engine(engineConfig)
        return captureFailure(ModelGenerationError::from) {
            withContext(Dispatchers.IO) {
                engine.initialize()
            }
            Napier.i(tag = "gpt") {
                "engine initialized"
            }
            val conversation = engine.createConversation()
            conversation.sendMessageAsync(prompt).map {
                Napier.d(tag = "gpt") {
                    "gpt output ${it.contents}"
                }
                GPTOutput(it.contents.contents.joinToString())
            }.onCompletion {
                engine.close()
                Napier.i(tag = "gpt") {
                    "engine closed"
                }
            }
        }.mapError(ModelGenerationError::fromError)
    }

    override fun models(scope: CoroutineScope): Flow<List<GPTModel>> =
        observeModels(
            scope,
            getGPTModelDirectory(),
            supportList,
        )
}
