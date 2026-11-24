/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.OnCommunityCreated
import com.storyteller_f.alphaheart.app.community.MemberJoinPolicy
import com.storyteller_f.alphaheart.app.create_community_action
import com.storyteller_f.alphaheart.app.creation_aid
import com.storyteller_f.alphaheart.app.creation_community_name
import com.storyteller_f.alphaheart.app.creation_member_policy
import com.storyteller_f.alphaheart.app.member_policy_invite_only
import com.storyteller_f.alphaheart.app.member_policy_invite_only_help
import com.storyteller_f.alphaheart.app.member_policy_open
import com.storyteller_f.alphaheart.app.member_policy_open_help
import com.storyteller_f.alphaheart.app.pages.CreateFormLayout
import com.storyteller_f.alphaheart.app.pages.title.CommonComposePage
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
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
            Modifier.fillMaxWidth().padding(bottom = 20.dp).appiumSemantics(
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
            Modifier.fillMaxWidth().padding(bottom = 20.dp).appiumSemantics(
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
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.creation_member_policy),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            radioOptions.forEach { choice ->
                MemberPolicyChoice(
                    choice,
                    choice.policy == memberJoinPolicy,
                    { onMemberJoinPolicyChange(choice.policy) },
                    Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun MemberPolicyChoice(
    choice: MemberJoinPolicy,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier.clip(shape)
            .background(if (selected) colors.primaryContainer else Color.Transparent)
            .border(1.dp, if (selected) colors.primary else colors.outlineVariant, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onSelect)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected, onClick = null, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(choice.title, style = MaterialTheme.typography.bodyMedium)
            Text(choice.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}
