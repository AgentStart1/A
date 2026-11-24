/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.IAccountInstance
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.FileSearchViewModel
import com.storyteller_f.alphaheart.app.common.createFileSearchViewModel
import com.storyteller_f.alphaheart.app.common.createMemberSearchInCommunityViewModel
import com.storyteller_f.alphaheart.app.common.createMemberSearchViewModel
import com.storyteller_f.alphaheart.app.common.createRoomSearchInCommunityViewModel
import com.storyteller_f.alphaheart.app.common.createRoomSearchViewModel
import com.storyteller_f.alphaheart.app.common.createRoomViewModel
import com.storyteller_f.alphaheart.app.common.createSearchCommunitiesViewModel
import com.storyteller_f.alphaheart.app.common.createSearchMemberInRoomViewModel
import com.storyteller_f.alphaheart.app.common.createTargetUserJoinedCommunitiesViewModel
import com.storyteller_f.alphaheart.app.common.createTopicSearchInCommunityViewModel
import com.storyteller_f.alphaheart.app.common.createTopicSearchInRoomViewModel
import com.storyteller_f.alphaheart.app.common.createTopicSearchInUserViewModel
import com.storyteller_f.alphaheart.app.common.createUserTitlesViewModel
import com.storyteller_f.alphaheart.app.common.getUserOverviewViewModel
import com.storyteller_f.alphaheart.app.pages.community.CommunityList
import com.storyteller_f.alphaheart.app.pages.file.FileCell
import com.storyteller_f.alphaheart.app.pages.room.RoomList
import com.storyteller_f.alphaheart.app.pages.title.ComposeMenu
import com.storyteller_f.alphaheart.app.pages.title.TitleList
import com.storyteller_f.alphaheart.app.pages.topic.TopicList
import com.storyteller_f.alphaheart.app.pages.user.MemberList
import com.storyteller_f.alphaheart.app.pages.user.SelfUserIconWithDialog
import com.storyteller_f.alphaheart.app.search.SearchScope
import com.storyteller_f.alphaheart.app.search_all
import com.storyteller_f.alphaheart.app.search_joined
import com.storyteller_f.alphaheart.app.search_topics
import com.storyteller_f.alphaheart.app.search_users
import com.storyteller_f.alphaheart.app.topic.TopicComposeData
import com.storyteller_f.alphaheart.app.topic_search_unsupported
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.paging.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.paging.topPrepend
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UserStatus
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab as SegmentedButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab as Tab
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTabRow as PrimaryTabRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSearchBar(
    scope: SearchScope,
    showSelfIcon: Boolean = true,
    title: String? = null,
    subtitle: String? = null,
    leadingIcon: @Composable () -> Unit,
) {
    val appNavFactory = LocalAppNavFactory.current
    val me = LocalUserInfo.current
    val roomId = (scope as? SearchScope.RoomTopic)?.roomId
    val room = roomId?.let { createRoomViewModel(it).handler.data.collectAsState().value }
    var showSheet by remember { mutableStateOf(false) }
    ReadingSearchToolbar(
        scope,
        title,
        leadingIcon,
        selfIcon = { if (showSelfIcon) SelfIcon { showSheet = true } },
        subtitle = subtitle,
    )
    ComposeMenu(showSheet, {
        showSheet = false
    }) { objectType ->
        showSheet = false
        when (objectType) {
            ObjectType.COMMUNITY -> appNavFactory.newAppNav().gotoCommunityCompose()

            ObjectType.ROOM -> appNavFactory.newAppNav().gotoRoomCompose()

            ObjectType.TOPIC -> {
                val data =
                    when {
                        scope is SearchScope.CommunityTopic ->
                            TopicComposeData.Community(scope.communityId, scope.communityId ob ObjectType.COMMUNITY)

                        room != null && room.isPrivate -> TopicComposeData.PrivateRoom(room.id, room.tuple())

                        room != null -> room.communityId?.let { TopicComposeData.PublicRoom(room.id, it, room.tuple()) }

                        else -> me?.let { TopicComposeData.User(it.id, it.tuple()) }
                    }
                data?.let { appNavFactory.newAppNav().gotoTopicCompose(it) }
            }

            ObjectType.USER -> Unit

            ObjectType.TITLE -> appNavFactory.newAppNav().gotoTitleCompose()

            ObjectType.FILE -> Unit

            ObjectType.PANEL_ACCOUNT -> Unit
        }
    }
}

@Composable
internal fun detailSearchBar(scope: SearchScope, leadingIcon: @Composable () -> Unit) {
    CustomSearchBar(scope = scope, title = searchScopeName(scope), leadingIcon = leadingIcon)
}

@Composable
private fun SelfIcon(onClickCreate: () -> Unit) {
    val myInfo = LocalUserInfo.current
    val userOverviewViewModel = getUserOverviewViewModel()
    SelfUserIconWithDialog(myInfo, overviewHandler = userOverviewViewModel.handler, onClickCreate = {
        if (myInfo?.status == UserStatus.NORMAL) onClickCreate()
    })
}

@Composable
internal fun SearchContent(scope: SearchScope, searchQuery: String) {
    val current = searchQuery.trim()
    when (scope) {
        SearchScope.World -> WorldSearchContent(current)
        SearchScope.MyCommunity -> MyCommunitySearchContent(current)
        SearchScope.MyRoom -> MyRoomSearchContent(current)
        is SearchScope.CommunityTopic -> CommunityTopicSearchContent(current, scope)
        is SearchScope.CommunityRoom -> CommunityRoomSearchContent(current, scope)
        is SearchScope.RoomTopic -> RoomTopicSearchContent(current, scope)
        is SearchScope.TopicTopic -> UnsupportedTopicSearchContent()
        is SearchScope.CommunityMember -> CommunityMemberSearchContent(current, scope)
        is SearchScope.RoomMember -> RoomMemberSearchContent(current, scope)
        SearchScope.Users -> MemberSearchContent(current)
        is SearchScope.UserTopic -> UserTopicSearchContent(current, scope)
        is SearchScope.UserCommunities -> UserCommunitySearchContent(current, scope)
        is SearchScope.UserReceivedTitle -> UserReceivedTitleSearchContent(current, scope)
        is SearchScope.UserCreatedTitle -> UserCreatedTitleSearchContent(current, scope)
        is SearchScope.UploadedFiles -> UploadedFilesSearchContent(current, scope)
    }
}

@Composable
fun UserCreatedTitleSearchContent(current: String, scope: SearchScope.UserCreatedTitle) {
    if (current.isNotBlank()) {
        TitleList(createUserTitlesViewModel(scope.userId, TitleSearchType.CREATOR))
    }
}

@Composable
fun UserReceivedTitleSearchContent(current: String, scope: SearchScope.UserReceivedTitle) {
    if (current.isNotBlank()) {
        TitleList(createUserTitlesViewModel(scope.userId, TitleSearchType.RECEIVER))
    }
}

@Composable
fun UserCommunitySearchContent(current: String, scope: SearchScope.UserCommunities) {
    if (current.isNotBlank()) {
        val communitiesViewModel = createTargetUserJoinedCommunitiesViewModel(scope.userId, current)
        CommunityList(communitiesViewModel)
    }
}

@Composable
private fun MemberSearchContent(current: String) {
    if (current.isNotBlank()) {
        val viewModel = createMemberSearchViewModel(current)
        MemberList(viewModel)
    }
}

@Composable
private fun RoomMemberSearchContent(current: String, scope: SearchScope.RoomMember) {
    if (current.isNotBlank()) {
        val viewModel = createSearchMemberInRoomViewModel(scope, current)
        MemberList(viewModel)
    }
}

@Composable
private fun CommunityMemberSearchContent(current: String, scope: SearchScope.CommunityMember) {
    if (current.isNotBlank()) {
        val viewModel = createMemberSearchInCommunityViewModel(scope, current)
        MemberList(viewModel)
    }
}

@Composable
internal fun UnsupportedTopicSearchContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(Res.string.topic_search_unsupported))
    }
}

@Composable
private fun UserTopicSearchContent(current: String, scope: SearchScope.UserTopic) {
    if (current.isNotBlank()) {
        val viewModel = createTopicSearchInUserViewModel(scope, current)
        TopicList(viewModel)
    }
}

@Composable
private fun RoomTopicSearchContent(current: String, scope: SearchScope.RoomTopic) {
    if (current.isNotBlank()) {
        val viewModel = createTopicSearchInRoomViewModel(scope, current)
        TopicList(viewModel)
    }
}

@Composable
private fun CommunityRoomSearchContent(current: String, scope: SearchScope.CommunityRoom) {
    if (current.isNotBlank()) {
        val viewModel = createRoomSearchInCommunityViewModel(scope, current)
        RoomList(viewModel)
    }
}

@Composable
private fun CommunityTopicSearchContent(current: String, scope: SearchScope.CommunityTopic) {
    if (current.isNotBlank()) {
        val viewModel = createTopicSearchInCommunityViewModel(scope, current)
        TopicList(viewModel)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MyRoomSearchContent(current: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val options = listOf(JoinStatusSearch.JOINED, JoinStatusSearch.UNSPECIFIED)
        var selectedOption by rememberSaveable { mutableIntStateOf(0) }
        val currentOption = options[selectedOption]

        val instance by LocalUiViewModel.current.instance.collectAsState()
        val isAlreadySignUp = instance !is IAccountInstance.None
        val finalOption = if (isAlreadySignUp) currentOption else JoinStatusSearch.UNSPECIFIED
        if (isAlreadySignUp) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEachIndexed { i, e ->
                        SegmentedButton(
                            currentOption == e,
                            {
                                selectedOption = i
                            },
                        ) {
                            Text(
                                stringResource(
                                    if (e ==
                                        JoinStatusSearch.JOINED
                                    ) {
                                        Res.string.search_joined
                                    } else {
                                        Res.string.search_all
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
        if (current.isNotBlank()) {
            val viewModel = createRoomSearchViewModel(finalOption, current)
            RoomList(viewModel)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MyCommunitySearchContent(query: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val options = listOf(JoinStatusSearch.JOINED, JoinStatusSearch.UNSPECIFIED)
        var selectedOption by rememberSaveable { mutableIntStateOf(0) }
        val currentOption = options[selectedOption]
        val instance by LocalUiViewModel.current.instance.collectAsState()
        val isAlreadySignUp = instance !is IAccountInstance.None
        val finalOption = if (isAlreadySignUp) currentOption else JoinStatusSearch.UNSPECIFIED
        if (isAlreadySignUp) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEachIndexed { i, e ->
                        SegmentedButton(
                            currentOption == e,
                            {
                                selectedOption = i
                            },
                        ) {
                            Text(
                                stringResource(
                                    if (e ==
                                        JoinStatusSearch.JOINED
                                    ) {
                                        Res.string.search_joined
                                    } else {
                                        Res.string.search_all
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
        if (query.isNotBlank()) {
            val viewModel = createSearchCommunitiesViewModel(finalOption, query)
            CommunityList(viewModel)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun WorldSearchContent(current: String) {
    if (current.isNotBlank()) {
        val pagerState =
            rememberPagerState {
                2
            }
        val tabs = listOf(stringResource(Res.string.search_topics), stringResource(Res.string.search_users))
        val coroutineScope = rememberCoroutineScope()
        SearchResultsLayout(
            filters = {
                PrimaryTabRow {
                    tabs.forEachIndexed { i, e ->
                        Tab(selected = pagerState.currentPage == i, onClick = {
                            coroutineScope.launch {
                                pagerState.scrollToPage(i)
                            }
                        }) {
                            Text(text = e, modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            },
            results = {
                HorizontalPager(pagerState) {
                    if (it == 0) {
                        UnsupportedTopicSearchContent()
                    } else {
                        val viewModel = createMemberSearchViewModel(current)
                        MemberList(viewModel)
                    }
                }
            },
        )
    }
}

@Composable
private fun UploadedFilesSearchContent(current: String, scope: SearchScope.UploadedFiles) {
    if (current.isNotBlank()) {
        val viewModel = createFileSearchViewModel(scope.objectId, scope.objectType, current)
        FileList(viewModel)
    }
}

@Composable
private fun FileList(viewModel: FileSearchViewModel) {
    val appNavFactory = LocalAppNavFactory.current
    StateView(viewModel, modifier = Modifier.fillMaxSize()) { pagingItems ->
        LazyColumn(contentPadding = PaddingValues(10.dp)) {
            topPrepend(pagingItems.loadState, pagingItems::retry)
            pagingItems(pagingItems, key = { it.id }) {
                val item: FileInfo? = pagingItems[it]
                FileCell(item) { items ->
                    val first = items.firstOrNull()
                    if (first != null) {
                        appNavFactory.newAppNav().gotoMedia(first)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            }
            bottomAppending(pagingItems.loadState, pagingItems::retry)
        }
    }
}
