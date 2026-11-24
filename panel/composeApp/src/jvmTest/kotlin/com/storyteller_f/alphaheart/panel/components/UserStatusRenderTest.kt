/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.viewModelScope
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogContext
import com.storyteller_f.alphaheart.client.core.RequestError
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.IdUserOverviewViewModel
import com.storyteller_f.alphaheart.panel.common.OnUserStatusUpdated
import com.storyteller_f.alphaheart.panel.set_normal
import com.storyteller_f.alphaheart.panel.set_read_only
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserOverview
import com.storyteller_f.shared.type.UserStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class UserStatusRenderTest {
    @Test
    fun successfulMutationEventImmediatelyChangesRenderedStatusAndAvailableAction() =
        runComposeUiTest {
            val originalLocale = Locale.getDefault()
            Locale.setDefault(Locale.ENGLISH)
            Dispatchers.setMain(Dispatchers.Unconfined)
            val events = MutableSharedFlow<Any>()
            val owner =
                IdUserOverviewViewModel(1, events) {
                    CustomResult.Success(UserOverview(0, 0, 0, 0, 0, 0, userInfo = UserInfo.EMPTY.copy(id = 1)))
                }
            val controller = CustomGlobalDialogController(owner.viewModelScope, GlobalDialogContext(events, Unit))
            try {
                setContent {
                    MaterialTheme {
                        val overview by owner.handler.data.collectAsState()
                        overview?.let { data ->
                            val status = data.userInfo.status
                            val next = if (status == UserStatus.NORMAL) UserStatus.READ_ONLY else UserStatus.NORMAL
                            Column {
                                Text(panelValueText(status.name), Modifier.testTag("status"))
                                ReadingButton(
                                    onClick = {
                                        owner.viewModelScope.launch {
                                            controller.useResult<Unit, RequestError> { CustomResult.Success(Unit) }
                                                .onSuccess {
                                                    controller.context.emitEvent(OnUserStatusUpdated(1, next))
                                                }
                                        }
                                    },
                                    modifier = Modifier.testTag("status-action"),
                                ) {
                                    val action =
                                        if (next == UserStatus.NORMAL) {
                                            Res.string.set_normal
                                        } else {
                                            Res.string.set_read_only
                                        }
                                    Text(stringResource(action))
                                }
                            }
                        }
                    }
                }
                onNodeWithTag("status").assertTextEquals("Normal")
                onNodeWithTag("status-action").performClick()
                onNodeWithTag("status").assertTextEquals("Read only")
                onNodeWithTag("status-action").assertTextEquals("Set to Normal")
                onNodeWithTag("status-action").performClick()
                onNodeWithTag("status").assertTextEquals("Normal")
                onNodeWithTag("status-action").assertTextEquals("Set to ReadOnly")
            } finally {
                owner.viewModelScope.cancel()
                Dispatchers.resetMain()
                Locale.setDefault(originalLocale)
            }
        }
}
