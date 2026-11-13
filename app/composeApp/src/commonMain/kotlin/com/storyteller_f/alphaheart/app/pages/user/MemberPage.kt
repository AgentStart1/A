/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.common.ContainerMemberViewModel
import com.storyteller_f.alphaheart.app.common.UserSearchViewModel
import com.storyteller_f.alphaheart.app.common.createMemberViewModel
import com.storyteller_f.alphaheart.app.pages.AdaptiveItemGrid
import com.storyteller_f.alphaheart.app.pages.search.CustomSearchBar
import com.storyteller_f.alphaheart.app.pages.search.SearchScope
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.topPrepend
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey

@Composable
fun MemberPage(objectId: PrimaryKey, objectType: ObjectType) {
    val viewModel = createMemberViewModel(objectId, objectType)
    Scaffold {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CustomSearchBar(
                when (objectType) {
                    ObjectType.COMMUNITY -> SearchScope.CommunityMember(objectId)
                    else -> SearchScope.RoomMember(objectId)
                },
            ) {
                var expended by remember {
                    mutableStateOf(false)
                }
                val appNav = LocalAppNavFactory.current

                Row {
                    FilledIconButton({
                        when (objectType) {
                            ObjectType.COMMUNITY -> appNav.newAppNav().gotoCommunityTitleCompose(objectId)
                            ObjectType.ROOM -> appNav.newAppNav().gotoRoomTitleCompose(objectId)
                            else -> appNav.newAppNav().gotoTitleCompose()
                        }
                    }) {
                        Icon(Icons.Default.Add, "add member")
                    }
                    FilledIconButton({
                    }) {
                        Icon(Icons.Default.Menu, "menu")
                    }
                }
                DropdownMenu(expended, {
                    expended = false
                }) {
                    DropdownMenuItem(text = {
                        Text("Nothing")
                    }, onClick = {
                        expended = false
                    })
                }
            }
            MemberList(viewModel)
        }
    }
}

@Composable
fun MemberList(memberViewModel: ContainerMemberViewModel, onClick: ((UserInfo) -> Unit)? = null) {
    val appNavFactory = LocalAppNavFactory.current
    StateView(memberViewModel, Modifier.fillMaxSize()) { items ->
        AdaptiveItemGrid {
            topPrepend(items.loadState)
            pagingItems(items, { it.id }) { index ->
                Column {
                    UserCell(items[index]?.userInfo, onClickCell = {
                        onClick?.invoke(it) ?: appNavFactory.newAppNav().gotoUser(it.id)
                    })
                    Spacer(modifier = Modifier.height(20.dp))
                    if (index != items.itemCount - 1) HorizontalDivider()
                }
            }
            bottomAppending(items.loadState)
        }
    }
}

@Composable
fun MemberList(memberViewModel: UserSearchViewModel, onClick: ((UserInfo) -> Unit)? = null) {
    val appNavFactory = LocalAppNavFactory.current
    StateView(memberViewModel, Modifier.fillMaxSize()) { items ->
        AdaptiveItemGrid {
            topPrepend(items.loadState)
            pagingItems(items, { it.id }) { index ->
                Column {
                    UserCell(items[index], onClickCell = {
                        onClick?.invoke(it) ?: appNavFactory.newAppNav().gotoUser(it.id)
                    })
                    Spacer(modifier = Modifier.height(20.dp))
                    if (index != items.itemCount - 1) HorizontalDivider()
                }
            }
            bottomAppending(items.loadState)
        }
    }
}
