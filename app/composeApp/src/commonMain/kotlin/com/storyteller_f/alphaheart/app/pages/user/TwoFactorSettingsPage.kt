/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.cancel_action
import com.storyteller_f.alphaheart.app.common.TwoFactorStage
import com.storyteller_f.alphaheart.app.common.beginTwoFactorSetup
import com.storyteller_f.alphaheart.app.common.turnOffTwoFactor
import com.storyteller_f.alphaheart.app.copy_auth_uri
import com.storyteller_f.alphaheart.app.download_recovery_codes
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.parent_topic_load_failed_retry
import com.storyteller_f.alphaheart.app.totp_code
import com.storyteller_f.alphaheart.app.two_factor_authentication
import com.storyteller_f.alphaheart.app.two_factor_disabled
import com.storyteller_f.alphaheart.app.two_factor_enabled
import com.storyteller_f.alphaheart.app.utils.saveRecoveryCodes
import com.storyteller_f.alphaheart.app.wizard_add
import com.storyteller_f.alphaheart.app.wizard_add_hint
import com.storyteller_f.alphaheart.app.wizard_continue
import com.storyteller_f.alphaheart.app.wizard_disable
import com.storyteller_f.alphaheart.app.wizard_done
import com.storyteller_f.alphaheart.app.wizard_off_hint
import com.storyteller_f.alphaheart.app.wizard_on_hint
import com.storyteller_f.alphaheart.app.wizard_previous
import com.storyteller_f.alphaheart.app.wizard_recovery
import com.storyteller_f.alphaheart.app.wizard_recovery_error
import com.storyteller_f.alphaheart.app.wizard_recovery_hint
import com.storyteller_f.alphaheart.app.wizard_recovery_saved
import com.storyteller_f.alphaheart.app.wizard_scan
import com.storyteller_f.alphaheart.app.wizard_start
import com.storyteller_f.alphaheart.app.wizard_verify
import com.storyteller_f.alphaheart.app.wizard_verify_error
import com.storyteller_f.alphaheart.app.wizard_verify_hint
import com.storyteller_f.alphaheart.app.wizard_verify_submit
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton
import com.storyteller_f.alphaheart.client.compose_core.notifications.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.compose_core.utils.copyText
import com.storyteller_f.alphaheart.client.core.enableTotp
import com.storyteller_f.alphaheart.client.core.generateRecoveryCodes
import com.storyteller_f.alphaheart.client.core.getTwoFactorSettings
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.TotpSetupInfo
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TwoFactorSettingsPage() {
    val dialog = LocalGlobalDialog.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf<TwoFactorStage>(TwoFactorStage.Loading) }
    var attempt by remember { mutableStateOf(0) }
    LaunchedEffect(attempt) {
        stage = TwoFactorStage.Loading
        dialog.request { getTwoFactorSettings() }
            .onSuccess { stage = if (it.enabled) TwoFactorStage.Enabled else TwoFactorStage.Disabled }
            .onFailure { stage = TwoFactorStage.LoadFailed }
    }
    Scaffold(topBar = { ReadingPageToolbar(stringResource(Res.string.two_factor_authentication)) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            TwoFactorWorkspace(stage) {
                when (val current = stage) {
                    TwoFactorStage.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))

                    TwoFactorStage.LoadFailed -> {
                        ReadingTextButton({ attempt++ }) {
                            Text(stringResource(Res.string.parent_topic_load_failed_retry))
                        }
                    }

                    TwoFactorStage.Disabled, TwoFactorStage.Enabled -> {
                        TwoFactorOverview(
                            enabled = current == TwoFactorStage.Enabled,
                            onSetup = { scope.launch { dialog.beginTwoFactorSetup()?.let { stage = it } } },
                            onDisable = { scope.launch { dialog.turnOffTwoFactor()?.let { stage = it } } },
                            onRecovery = { stage = TwoFactorStage.Recovery(null) },
                        )
                    }

                    is TwoFactorStage.Scan -> {
                        TwoFactorScan(
                            current.setup,
                            onCancel = { stage = TwoFactorStage.Disabled },
                            onContinue = { stage = TwoFactorStage.Verify(current.setup) },
                        )
                    }

                    is TwoFactorStage.Verify -> {
                        TwoFactorVerify(onBack = { stage = TwoFactorStage.Scan(current.setup) }) { code ->
                            dialog.request { enableTotp(code) }
                                .onSuccess { stage = TwoFactorStage.Recovery(current.setup.recoveryCodes) }.isSuccess
                        }
                    }

                    is TwoFactorStage.Recovery -> {
                        TwoFactorRecovery(
                            onDownload = {
                                val codes =
                                    current.codes ?: dialog.useResult { request { generateRecoveryCodes() } }
                                        .getOrNull()?.recoveryCodes?.also { stage = TwoFactorStage.Recovery(it) }
                                codes != null && saveRecoveryCodes(codes)
                            },
                            onDone = { stage = TwoFactorStage.Enabled },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TwoFactorWorkspace(stage: TwoFactorStage, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.widthIn(max = 424.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val step =
            when (stage) {
                is TwoFactorStage.Scan -> 0
                is TwoFactorStage.Verify -> 1
                is TwoFactorStage.Recovery -> 2
                else -> null
            }
        step?.let { TwoFactorProgress(it) }
        content()
    }
}

@Composable
private fun TwoFactorProgress(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Res.string.wizard_scan,
            Res.string.wizard_verify,
            Res.string.wizard_recovery,
        ).forEachIndexed { i, label ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                val colors = MaterialTheme.colorScheme
                LinearProgressIndicator(
                    progress = { if (i <= step) 1f else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (i == step) colors.primary else colors.primaryContainer,
                    drawStopIndicator = {},
                )
                Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TwoFactorOverview(enabled: Boolean, onSetup: () -> Unit, onDisable: () -> Unit, onRecovery: () -> Unit) {
    TwoFactorStepTitle(
        stringResource(if (enabled) Res.string.two_factor_enabled else Res.string.two_factor_disabled),
        stringResource(if (enabled) Res.string.wizard_on_hint else Res.string.wizard_off_hint),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        if (enabled) {
            ReadingTextButton(onDisable) { Text(stringResource(Res.string.wizard_disable)) }
            ReadingButton(onRecovery) { Text(stringResource(Res.string.download_recovery_codes)) }
        } else {
            ReadingButton(onSetup) { Text(stringResource(Res.string.wizard_start)) }
        }
    }
}

@Composable
private fun TwoFactorScan(setup: TotpSetupInfo, onCancel: () -> Unit, onContinue: () -> Unit) {
    val clipboard = LocalClipboard.current
    val toast = LocalToaster.current
    val scope = rememberCoroutineScope()
    TwoFactorStepTitle(stringResource(Res.string.wizard_add), stringResource(Res.string.wizard_add_hint))
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(rememberQrCodePainter(setup.otpauthUri), stringResource(Res.string.wizard_scan), Modifier.size(140.dp))
        ReadingTextButton({ scope.launch { clipboard.copyText(setup.otpauthUri, toast) } }) {
            Text(stringResource(Res.string.copy_auth_uri))
        }
    }
    TwoFactorStepActions(onCancel, stringResource(Res.string.cancel_action)) {
        ReadingButton(onContinue) { Text(stringResource(Res.string.wizard_continue)) }
    }
}

@Composable
internal fun TwoFactorVerify(onBack: () -> Unit, onVerify: suspend (String) -> Boolean) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val codeStyle =
        MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
            fontSize = 19.sp,
            lineHeight = 26.sp,
            letterSpacing = 6.sp,
        )
    TwoFactorStepTitle(stringResource(Res.string.wizard_verify), stringResource(Res.string.wizard_verify_hint))
    ReadingOutlinedTextField(
        code,
        {
            code = it.filter { digit -> digit in '0'..'9' }.take(6)
            failed = false
        },
        modifier = Modifier.fillMaxWidth().testTag("two-factor-code"),
        enabled = !busy,
        singleLine = true,
        isError = failed,
        label = { Text(stringResource(Res.string.totp_code)) },
        textStyle = codeStyle,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
    if (failed) {
        Text(
            stringResource(Res.string.wizard_verify_error),
            Modifier.testTag("two-factor-code-error"),
            color = MaterialTheme.colorScheme.error,
        )
    }
    TwoFactorStepActions(onBack, stringResource(Res.string.wizard_previous), canGoBack = !busy) {
        ReadingButton(
            onClick = {
                busy = true
                scope.launch {
                    try {
                        failed = !onVerify(code)
                    } finally {
                        busy = false
                    }
                }
            },
            modifier = Modifier.testTag("two-factor-verify"),
            enabled = !busy && code.length == 6,
        ) {
            TwoFactorButtonBody(busy, stringResource(Res.string.wizard_verify_submit))
        }
    }
}

@Composable
internal fun TwoFactorRecovery(onDownload: suspend () -> Boolean, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    TwoFactorStepTitle(stringResource(Res.string.wizard_recovery), stringResource(Res.string.wizard_recovery_hint))
    if (saved) {
        Text(stringResource(Res.string.wizard_recovery_saved))
    } else if (failed) {
        Text(
            stringResource(Res.string.wizard_recovery_error),
            Modifier.testTag("two-factor-download-error"),
            color = MaterialTheme.colorScheme.error,
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        if (saved) {
            ReadingButton(onDone, Modifier.testTag("two-factor-done")) { Text(stringResource(Res.string.wizard_done)) }
        } else {
            ReadingButton(
                onClick = {
                    busy = true
                    failed = false
                    scope.launch {
                        try {
                            saved = onDownload()
                            failed = !saved
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            failed = true
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.testTag("two-factor-download"),
                enabled = !busy,
            ) {
                TwoFactorButtonBody(busy, stringResource(Res.string.download_recovery_codes))
            }
        }
    }
}

@Composable
private fun TwoFactorStepTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TwoFactorStepActions(
    onBack: () -> Unit,
    backLabel: String,
    canGoBack: Boolean = true,
    submit: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ReadingTextButton(onBack, enabled = canGoBack) { Text(backLabel) }
        submit()
    }
}

@Composable
private fun TwoFactorButtonBody(busy: Boolean, label: String) {
    if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
    Text(label, Modifier.padding(start = if (busy) 8.dp else 0.dp))
}
