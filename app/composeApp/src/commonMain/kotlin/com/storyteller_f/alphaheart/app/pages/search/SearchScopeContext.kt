/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.createCommunityViewModel
import com.storyteller_f.alphaheart.app.common.createRoomViewModel
import com.storyteller_f.alphaheart.app.common.createUserViewModel
import com.storyteller_f.alphaheart.app.communities_title
import com.storyteller_f.alphaheart.app.rooms
import com.storyteller_f.alphaheart.app.scope_community
import com.storyteller_f.alphaheart.app.scope_files
import com.storyteller_f.alphaheart.app.scope_room
import com.storyteller_f.alphaheart.app.scope_topic_replies
import com.storyteller_f.alphaheart.app.scope_world
import com.storyteller_f.alphaheart.app.search.SearchScope
import com.storyteller_f.alphaheart.app.search_members_content
import com.storyteller_f.alphaheart.app.search_scope
import com.storyteller_f.alphaheart.app.search_scope_content
import com.storyteller_f.alphaheart.app.titles
import com.storyteller_f.alphaheart.app.topics
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SearchScopeContext(scope: SearchScope, identity: @Composable () -> Unit) {
    val label = searchScopeLabel(scope)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val name = searchScopeName(scope)
        if (name != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                identity()
                Text(name, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
internal fun searchScopeLabel(scope: SearchScope): String {
    val content =
        when (scope) {
            is SearchScope.CommunityTopic, is SearchScope.RoomTopic, is SearchScope.UserTopic -> Res.string.topics
            is SearchScope.CommunityMember, is SearchScope.RoomMember -> Res.string.search_members_content
            is SearchScope.CommunityRoom -> Res.string.rooms
            is SearchScope.UserCommunities -> Res.string.communities_title
            is SearchScope.UserReceivedTitle, is SearchScope.UserCreatedTitle -> Res.string.titles
            is SearchScope.UploadedFiles -> Res.string.scope_files
            else -> null
        }
    return if (content != null) {
        stringResource(Res.string.search_scope_content, stringResource(content))
    } else {
        stringResource(
            when (scope) {
                SearchScope.World -> Res.string.scope_world
                SearchScope.MyCommunity -> Res.string.scope_community
                SearchScope.MyRoom -> Res.string.scope_room
                is SearchScope.TopicTopic -> Res.string.scope_topic_replies
                else -> Res.string.search_scope
            },
        )
    }
}

@Composable
internal fun searchScopeName(scope: SearchScope): String? =
    when (scope) {
        is SearchScope.CommunityTopic -> communityScopeName(scope.communityId)

        is SearchScope.CommunityRoom -> communityScopeName(scope.communityId)

        is SearchScope.CommunityMember -> communityScopeName(scope.communityId)

        is SearchScope.RoomTopic -> roomScopeName(scope.roomId)

        is SearchScope.RoomMember -> roomScopeName(scope.roomId)

        is SearchScope.TopicTopic -> stringResource(Res.string.topics)

        is SearchScope.UploadedFiles ->
            when (scope.objectType) {
                ObjectType.COMMUNITY -> communityScopeName(scope.objectId)
                ObjectType.ROOM -> roomScopeName(scope.objectId)
                ObjectType.USER -> userScopeName(scope.objectId)
                else -> null
            }

        is SearchScope.UserTopic -> userScopeName(scope.userId)

        is SearchScope.UserCommunities -> userScopeName(scope.userId)

        is SearchScope.UserReceivedTitle -> userScopeName(scope.userId)

        is SearchScope.UserCreatedTitle -> userScopeName(scope.userId)

        else -> null
    }

@Composable
private fun communityScopeName(id: PrimaryKey): String? {
    val info by createCommunityViewModel(id).handler.data.collectAsStateWithLifecycle()
    return info?.name
}

@Composable
private fun roomScopeName(id: PrimaryKey): String? {
    val info by createRoomViewModel(id).handler.data.collectAsStateWithLifecycle()
    return info?.name
}

@Composable
private fun userScopeName(id: PrimaryKey): String? {
    val info by createUserViewModel(id).handler.data.collectAsStateWithLifecycle()
    return info?.nickname
}

@Composable
internal fun SearchScopeIdentity(scope: SearchScope, interactive: Boolean = false) {
    when (scope) {
        is SearchScope.CommunityTopic -> CommunityScopeIcon(scope.communityId, interactive)

        is SearchScope.CommunityRoom -> CommunityScopeIcon(scope.communityId, interactive)

        is SearchScope.CommunityMember -> CommunityScopeIcon(scope.communityId, interactive)

        is SearchScope.RoomTopic -> RoomScopeIcon(scope.roomId, interactive)

        is SearchScope.RoomMember -> RoomScopeIcon(scope.roomId, interactive)

        is SearchScope.UserTopic -> UserScopeIcon(scope.userId)

        is SearchScope.UserCommunities -> UserScopeIcon(scope.userId)

        is SearchScope.UserReceivedTitle -> UserScopeIcon(scope.userId)

        is SearchScope.UserCreatedTitle -> UserScopeIcon(scope.userId)

        is SearchScope.UploadedFiles ->
            when (scope.objectType) {
                ObjectType.COMMUNITY -> CommunityScopeIcon(scope.objectId)
                ObjectType.ROOM -> RoomScopeIcon(scope.objectId)
                ObjectType.USER -> UserScopeIcon(scope.objectId)
                else -> Unit
            }

        else -> Unit
    }
}

@Composable
private fun CommunityScopeIcon(id: PrimaryKey, interactive: Boolean = false) {
    val info by createCommunityViewModel(id).handler.data.collectAsStateWithLifecycle()
    val dialog = com.storyteller_f.alphaheart.client.compose_core.components.rememberCommonDialogController()
    com.storyteller_f.alphaheart.app.pages.community.CommunityIconWithDialog(
        info,
        dialog.shown.value,
        40.dp,
        interactive,
        dialog::update,
    )
}

@Composable
private fun RoomScopeIcon(id: PrimaryKey, interactive: Boolean = false) {
    val info by createRoomViewModel(id).handler.data.collectAsStateWithLifecycle()
    val dialog = com.storyteller_f.alphaheart.client.compose_core.components.rememberCommonDialogController()
    com.storyteller_f.alphaheart.app.pages.room.RoomIconWithDialog(
        info,
        dialog.shown.value,
        48.dp,
        interactive,
        dialog::update,
    )
}

@Composable
private fun UserScopeIcon(id: PrimaryKey) {
    val info by createUserViewModel(id).handler.data.collectAsStateWithLifecycle()
    com.storyteller_f.alphaheart.client.compose_core.components.UserIcon(false, info?.avatar?.url) {}
}
