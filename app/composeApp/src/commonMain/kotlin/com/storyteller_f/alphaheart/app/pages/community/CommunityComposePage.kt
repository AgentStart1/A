/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.aid
import com.storyteller_f.alphaheart.app.common.OnCommunityCreated
import com.storyteller_f.alphaheart.app.create_community_action
import com.storyteller_f.alphaheart.app.creation_aid
import com.storyteller_f.alphaheart.app.creation_community_name
import com.storyteller_f.alphaheart.app.creation_member_policy
import com.storyteller_f.alphaheart.app.member_policy_invite_only
import com.storyteller_f.alphaheart.app.member_policy_invite_only_help
import com.storyteller_f.alphaheart.app.member_policy_open
import com.storyteller_f.alphaheart.app.member_policy_open_help
import com.storyteller_f.alphaheart.app.name
import com.storyteller_f.alphaheart.app.pages.CreateFormLayout
import com.storyteller_f.alphaheart.app.pages.title.CommonComposePage
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.MemberPolicy
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField as OutlinedTextField

@Composable
fun CommunityComposePage() {
    var name by remember {
        mutableStateOf("")
    }
    var aid by remember {
        mutableStateOf("")
    }
    var memberJoinPolicy by remember { mutableStateOf(MemberPolicy.OPEN) }

    val appNavFactory = LocalAppNavFactory.current
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    CommonComposePage({
        scope.launch {
            globalDialogController.useResult {
                request {
                    createCommunity(NewCommunity(name, aid, memberPolicy = memberJoinPolicy))
                }
            }.onSuccess {
                globalDialogController.emitEvent(OnCommunityCreated(it))
                appNavFactory.newAppNav().back()
            }
        }
    }, title = stringResource(Res.string.create_community_action)) { actions ->
        CommunityComposeInternal(name, aid, memberJoinPolicy, {
            name = it
        }, {
            memberJoinPolicy = it
        }, {
            aid = it
        }, footer = actions)
    }
}

data class MemberJoinPolicy(val title: String, val policy: MemberPolicy, val description: String)

@Preview
@Composable
fun CommunityComposeInternal(
    name: String = "",
    aid: String = "",
    memberJoinPolicy: MemberPolicy = MemberPolicy.OPEN,
    onNameChange: (String) -> Unit = {},
    onMemberJoinPolicyChange: (MemberPolicy) -> Unit = {},
    onAidChange: (String) -> Unit = {},
    footer: @Composable () -> Unit = {},
) {
    CreateFormLayout {
        OutlinedTextField(
            name,
            singleLine = true,
            modifier =
            Modifier.padding(bottom = 20.dp).appiumSemantics(
                description = "community name",
                input = true,
                inputValue = name,
                onInputValueChange = onNameChange,
            ),
            onValueChange = {
                onNameChange(it)
            },
            label = {
                Text(stringResource(Res.string.creation_community_name))
            },
        )
        OutlinedTextField(
            aid,
            singleLine = true,
            modifier =
            Modifier.padding(bottom = 20.dp).appiumSemantics(
                description = "community aid",
                input = true,
                inputValue = aid,
                onInputValueChange = onAidChange,
            ),
            onValueChange = {
                onAidChange(it)
            },
            label = {
                Text(stringResource(Res.string.creation_aid))
            },
        )
        val radioOptions = getMemberPolicies()
        CommunityMemberPolicyChoices(radioOptions, memberJoinPolicy, onMemberJoinPolicyChange)
        footer()
    }
}

@Composable
private fun getMemberPolicies(): List<MemberJoinPolicy> =
    listOf(
        MemberJoinPolicy(
            stringResource(Res.string.member_policy_open),
            MemberPolicy.OPEN,
            stringResource(Res.string.member_policy_open_help),
        ),
        MemberJoinPolicy(
            stringResource(Res.string.member_policy_invite_only),
            MemberPolicy.INVITE_ONLY,
            stringResource(Res.string.member_policy_invite_only_help),
        ),
    )

@Composable
private fun CommunityMemberPolicyChoices(
    radioOptions: List<MemberJoinPolicy>,
    memberJoinPolicy: MemberPolicy,
    onMemberJoinPolicyChange: (MemberPolicy) -> Unit,
) {
    com.storyteller_f.alphaheart.client.compose_core.components.ReadingSelect(
        stringResource(Res.string.creation_member_policy),
        memberJoinPolicy,
        radioOptions.map { it.policy to it.title },
        onMemberJoinPolicyChange,
        Modifier.fillMaxWidth().padding(bottom = 20.dp),
    )
}
