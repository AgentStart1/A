/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.getQuotaViewModel
import com.storyteller_f.alphaheart.app.quota_chunk_size
import com.storyteller_f.alphaheart.app.quota_title
import com.storyteller_f.alphaheart.app.quota_total_space
import com.storyteller_f.alphaheart.app.quota_upload_record
import com.storyteller_f.alphaheart.app.quota_used_space
import com.storyteller_f.alphaheart.client.compose_core.components.CachedRefreshIndicator
import com.storyteller_f.alphaheart.client.compose_core.components.ExceptionCell
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingActionMenuSurface
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.shared.model.QuotaDetail
import com.storyteller_f.shared.obj.ObjectTuple
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingDialog as Dialog

@Composable
internal fun QuotaDialog(show: Boolean, dismiss: () -> Unit, mediaTarget: ObjectTuple) {
    if (!show) return
    val quotaVm = getQuotaViewModel(mediaTarget)
    val refreshCached = remember(quotaVm) { quotaVm.handler.data.value != null }
    LaunchedEffect(quotaVm) {
        if (refreshCached) quotaVm.handler.refresh()
    }
    Dialog(dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
            ReadingActionMenuSurface(
                onDismiss = dismiss,
                heading = { Text(stringResource(Res.string.quota_title), fontSize = 17.sp) },
            ) {
                Box(Modifier.padding(top = 22.dp)) {
                    QuotaDialogState(quotaVm.handler)
                }
            }
        }
    }
}

@Composable
internal fun QuotaDialogState(handler: LoadingHandler<QuotaDetail>) {
    val quota by handler.data.collectAsState()
    val state by handler.state.collectAsState()
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val loaded = quota
        val error = state as? LoadingState.Error
        when {
            loaded != null -> {
                QuotaDialogContent(loaded)
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.TopCenter) {
                    CachedRefreshIndicator(state, handler::refresh)
                }
            }

            error != null -> ExceptionCell(error.e, handler::refresh)

            else -> CircularProgressIndicator(Modifier.padding(vertical = 24.dp))
        }
    }
}

/** Presentation only; the caller retains the typed quota owner and request lifecycle. */
@Composable
internal fun QuotaDialogContent(quota: QuotaDetail) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.quota_total_space, HumanReadable.fileSize(quota.total)))
        Text(stringResource(Res.string.quota_used_space, HumanReadable.fileSize(quota.used)))
        QuotaProgress(quota.used, quota.total)
        quota.uploadRecord?.let { record ->
            HorizontalDivider()
            Text(stringResource(Res.string.quota_upload_record, record.name))
            QuotaProgress(record.progress, record.total)
            Text("${HumanReadable.fileSize(record.progress)} / ${HumanReadable.fileSize(record.total)}")
            Text(uploadRecordStatusLabel(record.status))
            Text(stringResource(Res.string.quota_chunk_size, HumanReadable.fileSize(record.chunkSize)))
        }
    }
}

@Composable
private fun QuotaProgress(used: Long, total: Long) {
    val fraction = if (total > 0) (used.toFloat() / total).coerceIn(0f, 1f) else 0f
    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
    Text("${(fraction * 100).toInt()}%")
}
