/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.error_details
import com.storyteller_f.alphaheart.client.compose_core.operation_failed
import com.storyteller_f.alphaheart.client.compose_core.operation_failed_help
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import org.jetbrains.compose.resources.stringResource

/** Short failures wrap their content; long diagnostics scroll without displacing the dismissal controls. */
@Composable
internal fun ReadingFailureSurface(throwable: Throwable, scrollState: ScrollState, dismiss: () -> Unit) {
    ReadingActionMenuSurface(
        onDismiss = dismiss,
        heading = {
            Text(
                stringResource(Res.string.operation_failed),
                fontSize = 19.sp,
                lineHeight = 26.6.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
        // sourceModal uses community-dialog, not the compact third-dialog padding override.
        compact = false,
    ) {
        Text(stringResource(Res.string.operation_failed_help), Modifier.padding(top = 20.dp, bottom = 12.dp))
        Text(stringResource(Res.string.error_details), style = MaterialTheme.typography.labelMedium)
        ExceptionView(
            throwable,
            Modifier.fillMaxWidth().heightIn(max = 160.dp).testTag("failure-details").verticalScroll(scrollState),
        )
        Box(Modifier.fillMaxWidth().padding(top = 22.dp), contentAlignment = Alignment.CenterEnd) {
            val label = CoreStrings.ok()
            ReadingButton(dismiss, Modifier.appiumSemantics(text = label, onClick = dismiss)) { Text(label) }
        }
    }
}
