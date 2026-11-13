/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.SessionHistoryViewModel
import com.storyteller_f.alphaheart.app.common.getLoginHistoryViewModel
import com.storyteller_f.alphaheart.app.delete
import com.storyteller_f.alphaheart.app.go_to_sign_up
import com.storyteller_f.alphaheart.app.last_used
import com.storyteller_f.alphaheart.app.private_key
import com.storyteller_f.alphaheart.app.sign_in
import com.storyteller_f.alphaheart.app.sign_in_subtitle
import com.storyteller_f.alphaheart.client.compose_core.components.BaseSheet
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.PendingTotpSignIn
import com.storyteller_f.alphaheart.client.core.RawUserPass
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignInPage(signInAndSignUpNav: SignInAndSignUpNav) {
    AuthPageChrome(
        title = stringResource(Res.string.sign_in),
        subtitle = stringResource(Res.string.sign_in_subtitle),
        footer = {
            TextButton(
                onClick = { signInAndSignUpNav.gotoSignUp() },
                modifier =
                Modifier.appiumSemantics(
                    testTag = "goto_sign_up",
                    text = stringResource(Res.string.go_to_sign_up),
                ),
            ) {
                Text(stringResource(Res.string.go_to_sign_up))
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                onClick = { signInAndSignUpNav.gotoPrivateKey(false) },
                modifier =
                Modifier.fillMaxWidth().appiumSemantics(
                    testTag = "private_key",
                    text = stringResource(Res.string.private_key),
                ),
                shape = ButtonDefaults.filledTonalShape,
            ) {
                Icon(Icons.Default.VpnKey, contentDescription = null)
                Text(stringResource(Res.string.private_key), modifier = Modifier.padding(start = 8.dp))
            }
            SelectFromHistory()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SelectFromHistory() {
    val viewModel = getLoginHistoryViewModel()
    SelectFromHistoryInternal(viewModel)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SelectFromHistoryInternal(viewModel: SessionHistoryViewModel) {
    var showSheet by remember {
        mutableStateOf(false)
    }
    val sheetState = rememberModalBottomSheetState()
    OutlinedButton(
        onClick = { showSheet = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.History, contentDescription = null)
        Text(stringResource(Res.string.last_used), modifier = Modifier.padding(start = 8.dp))
    }
    val uIViewModel = LocalUiViewModel.current
    val scope = rememberCoroutineScope()
    val appNavFactory = LocalAppNavFactory.current
    val globalDialogController = LocalGlobalDialog.current
    var pendingTotp by remember { mutableStateOf<PendingTotpSignIn?>(null) }
    BaseSheet(showSheet, sheetState, {
        showSheet = false
    }) {
        val data by viewModel.handler.data.collectAsState()
        val last = data?.history?.last
        StateView(viewModel.handler, modifier = Modifier.height(260.dp)) {
            LazyColumn {
                items(it.alias) { alias ->
                    LoginHistoryCell(alias, last, {
                        scope.launch {
                            pendingTotp =
                                globalDialogController.performKeySignInAuth(appNavFactory, uIViewModel) {
                                    val pass =
                                        checkNotNull(
                                            uIViewModel.historyManager.buildSession(alias),
                                        ) as RawUserPass
                                    pass.rawUSerPass.authKey
                                }
                            showSheet = false
                        }
                    }, {
                        viewModel.deleteSession(alias)
                    })
                }
            }
        }
    }
    TotpSignInDialog(pendingTotp, { pendingTotp = null }) { pending, code ->
        scope.completePendingTotpSignIn(globalDialogController, appNavFactory, pending, code, uIViewModel) {
            pendingTotp = null
        }
    }
}

class PrivateParameterProvider : PreviewParameterProvider<String> {
    override val values: Sequence<String>
        get() =
            sequence {
                yield(
                    buildString {
                        repeat(50) {
                            append("a")
                        }
                    },
                )
            }
}

@Preview
@Composable
private fun LoginHistoryCell(
    @PreviewParameter(PrivateParameterProvider::class) address: String,
    last: String? = "hello",
    onSelect: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.clickable { onSelect() }.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                address,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            val shape = RoundedCornerShape(8.dp)
            if (address == last) {
                Text(
                    stringResource(Res.string.last_used),
                    modifier =
                    Modifier.clip(shape)
                        .background(MaterialTheme.colorScheme.primaryContainer, shape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, stringResource(Res.string.delete))
            }
        }
    }
}
