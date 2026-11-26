/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.ContainerMemberViewModel
import com.storyteller_f.alphaheart.app.common.UserSearchViewModel
import com.storyteller_f.alphaheart.app.common.createMemberViewModel
import com.storyteller_f.alphaheart.app.community_member_page_title
import com.storyteller_f.alphaheart.app.create_title_action
import com.storyteller_f.alphaheart.app.pages.search.CustomSearchBar
import com.storyteller_f.alphaheart.app.pages.search.SearchScopeIdentity
import com.storyteller_f.alphaheart.app.pages.search.searchScopeName
import com.storyteller_f.alphaheart.app.room_member_page_title
import com.storyteller_f.alphaheart.app.search.SearchScope
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.paging.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.paging.topPrepend
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

@Composable
fun MemberPage(objectId: PrimaryKey, objectType: ObjectType) {
    val viewModel = createMemberViewModel(objectId, objectType)
    val scope =
        when (objectType) {
            ObjectType.COMMUNITY -> SearchScope.CommunityMember(objectId)
            else -> SearchScope.RoomMember(objectId)
        }
    val titleResource =
        when (objectType) {
            ObjectType.COMMUNITY -> Res.string.community_member_page_title
            else -> Res.string.room_member_page_title
        }
    val title = stringResource(titleResource)
    Scaffold(
        topBar = {
            CustomSearchBar(scope, title = title, subtitle = searchScopeName(scope)) {
                SearchScopeIdentity(scope, interactive = true)
            }
        },
        floatingActionButton = { CreateMemberTitleFab(objectId, objectType) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { MemberList(viewModel) }
    }
}

@Composable
private fun CreateMemberTitleFab(objectId: PrimaryKey, objectType: ObjectType) {
    val appNav = LocalAppNavFactory.current
    FloatingActionButton(onClick = {
        when (objectType) {
            ObjectType.COMMUNITY -> appNav.newAppNav().gotoCommunityTitleCompose(objectId)
            ObjectType.ROOM -> appNav.newAppNav().gotoRoomTitleCompose(objectId)
            else -> appNav.newAppNav().gotoTitleCompose()
        }
    }) {
        Icon(actionIcon(ActionGlyph.PLUS), stringResource(Res.string.create_title_action))
    }
}

@Composable
private fun MemberRows(content: LazyListScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 740.dp).fillMaxSize(), content = content)
    }
}

@Composable
fun MemberList(memberViewModel: ContainerMemberViewModel, onClick: ((UserInfo) -> Unit)? = null) {
    val appNavFactory = LocalAppNavFactory.current
    StateView(memberViewModel, Modifier.fillMaxSize()) { items ->
        MemberRows {
            topPrepend(items.loadState, items::retry)
            pagingItems(items, { it.id }) { index ->
                Column {
                    UserCell(items[index]?.userInfo, onClickCell = {
                        onClick?.invoke(it) ?: appNavFactory.newAppNav().gotoUser(it.id)
                    })
                    Spacer(modifier = Modifier.height(20.dp))
                    if (index < items.itemCount - 1) HorizontalDivider()
                }
            }
            bottomAppending(items.loadState, items::retry)
        }
    }
}

@Composable
fun MemberList(memberViewModel: UserSearchViewModel, onClick: ((UserInfo) -> Unit)? = null) {
    val appNavFactory = LocalAppNavFactory.current
    StateView(memberViewModel, Modifier.fillMaxSize()) { items ->
        MemberRows {
            topPrepend(items.loadState, items::retry)
            pagingItems(items, { it.id }) { index ->
                Column {
                    UserCell(items[index], onClickCell = {
                        onClick?.invoke(it) ?: appNavFactory.newAppNav().gotoUser(it.id)
                    })
                    Spacer(modifier = Modifier.height(20.dp))
                    if (index < items.itemCount - 1) HorizontalDivider()
                }
            }
            bottomAppending(items.loadState, items::retry)
        }
    }
}
