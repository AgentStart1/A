/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.storyteller_f.alphaheart.app.pages.AdaptiveItemGrid
import com.storyteller_f.alphaheart.app.pages.adaptiveColumnCount
import com.storyteller_f.alphaheart.app.pages.search.CustomSearchBar
import com.storyteller_f.alphaheart.app.pages.search.SearchScope
import com.storyteller_f.alphaheart.app.pages.search.SearchScopeIdentity
import com.storyteller_f.alphaheart.app.pages.search.searchScopeName
import com.storyteller_f.alphaheart.app.room_member_page_title
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.topPrepend
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

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
    Scaffold(topBar = {
        CustomSearchBar(scope, title = title) { SearchScopeIdentity(scope) }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MemberPageHeading(searchScopeName(scope).orEmpty(), objectId, objectType)
            MemberList(viewModel)
        }
    }
}

@Composable
private fun MemberPageHeading(name: String, objectId: PrimaryKey, objectType: ObjectType) {
    val appNav = LocalAppNavFactory.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
        IconButton({
            when (objectType) {
                ObjectType.COMMUNITY -> appNav.newAppNav().gotoCommunityTitleCompose(objectId)
                ObjectType.ROOM -> appNav.newAppNav().gotoRoomTitleCompose(objectId)
                else -> appNav.newAppNav().gotoTitleCompose()
            }
        }) {
            Icon(Icons.Default.Add, stringResource(Res.string.create_title_action))
        }
    }
}

/** Uses the same available width and breakpoint as the grid that actually lays out the members. */
@Composable
private fun MemberGrid(content: LazyGridScope.(Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = adaptiveColumnCount(maxWidth.value)
        AdaptiveItemGrid { content(columns) }
    }
}

@Composable
fun MemberList(memberViewModel: ContainerMemberViewModel, onClick: ((UserInfo) -> Unit)? = null) {
    val appNavFactory = LocalAppNavFactory.current
    StateView(memberViewModel, Modifier.fillMaxSize()) { items ->
        MemberGrid { columns ->
            topPrepend(items.loadState, items::retry)
            pagingItems(items, { it.id }) { index ->
                Column {
                    UserCell(items[index]?.userInfo, onClickCell = {
                        onClick?.invoke(it) ?: appNavFactory.newAppNav().gotoUser(it.id)
                    })
                    Spacer(modifier = Modifier.height(20.dp))
                    if (index + columns < items.itemCount) HorizontalDivider()
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
        MemberGrid { columns ->
            topPrepend(items.loadState, items::retry)
            pagingItems(items, { it.id }) { index ->
                Column {
                    UserCell(items[index], onClickCell = {
                        onClick?.invoke(it) ?: appNavFactory.newAppNav().gotoUser(it.id)
                    })
                    Spacer(modifier = Modifier.height(20.dp))
                    if (index + columns < items.itemCount) HorizontalDivider()
                }
            }
            bottomAppending(items.loadState, items::retry)
        }
    }
}
