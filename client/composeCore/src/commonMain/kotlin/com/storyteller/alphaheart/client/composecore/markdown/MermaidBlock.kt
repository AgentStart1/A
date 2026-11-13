/*
 * This is a private project. All rights reserved.
 */

package com.storyteller.alphaheart.client.composecore.markdown

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.storyteller_f.alphaheart.client.compose_core.components.HighlightCodeBlock
import com.storyteller_f.alphaheart.client.compose_core.error.UiOperationError
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.fold
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.utils.readCodeFence
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel

@Composable
internal fun MermaidBlock(modal: MarkdownComponentModel) {
    val source =
        remember(modal.node, modal.content) {
            readCodeFence(modal.node, modal.content)
        }
    val renderScope = remember { CoroutineScope(SupervisorJob()) }
    DisposableEffect(renderScope) {
        onDispose(renderScope::cancel)
    }
    val renderState by produceState<MermaidRenderState>(MermaidRenderState.Loading, source) {
        val rendering =
            renderScope.async {
                captureFailure(UiOperationError::from) { renderMermaidSvg(source) }
            }
        val result =
            try {
                rendering.await()
            } finally {
                rendering.cancel()
            }
        val error = result.exceptionOrNull()
        if (error is CancellationException) {
            throw error
        }
        value =
            result.fold(
                onSuccess = MermaidRenderState::Success,
                onFailure = { renderError ->
                    Napier.e(renderError.cause) { "Failed to render Mermaid code fence" }
                    MermaidRenderState.Error
                },
            )
    }

    when (val state = renderState) {
        MermaidRenderState.Loading -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        MermaidRenderState.Error -> {
            HighlightCodeBlock(modal)
        }

        is MermaidRenderState.Success -> {
            val svgBytes = remember(state.svg) { state.svg.encodeToByteArray() }
            SubcomposeAsyncImage(
                model = svgBytes,
                contentDescription = "Mermaid diagram",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
                loading = {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                },
                error = {
                    HighlightCodeBlock(modal)
                },
            )
        }
    }
}

private sealed interface MermaidRenderState {
    data object Loading : MermaidRenderState
    data object Error : MermaidRenderState
    data class Success(val svg: String) : MermaidRenderState
}
