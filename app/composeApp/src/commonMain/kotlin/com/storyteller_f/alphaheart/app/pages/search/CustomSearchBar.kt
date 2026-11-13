/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.storyteller_f.alphaheart.app.IAccountInstance
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.close
import com.storyteller_f.alphaheart.app.common.FileSearchViewModel
import com.storyteller_f.alphaheart.app.common.createFileSearchViewModel
import com.storyteller_f.alphaheart.app.common.createMemberSearchInCommunityViewModel
import com.storyteller_f.alphaheart.app.common.createMemberSearchViewModel
import com.storyteller_f.alphaheart.app.common.createRoomSearchInCommunityViewModel
import com.storyteller_f.alphaheart.app.common.createRoomSearchViewModel
import com.storyteller_f.alphaheart.app.common.createSearchCommunitiesViewModel
import com.storyteller_f.alphaheart.app.common.createSearchMemberInRoomViewModel
import com.storyteller_f.alphaheart.app.common.createTargetUserJoinedCommunitiesViewModel
import com.storyteller_f.alphaheart.app.common.createTopicSearchInCommunityViewModel
import com.storyteller_f.alphaheart.app.common.createTopicSearchInRoomViewModel
import com.storyteller_f.alphaheart.app.common.createTopicSearchInTopicViewModel
import com.storyteller_f.alphaheart.app.common.createTopicSearchInUserViewModel
import com.storyteller_f.alphaheart.app.common.createTopicSearchViewModel
import com.storyteller_f.alphaheart.app.common.createUserTitlesViewModel
import com.storyteller_f.alphaheart.app.common.getUserOverviewViewModel
import com.storyteller_f.alphaheart.app.create
import com.storyteller_f.alphaheart.app.input_search_community
import com.storyteller_f.alphaheart.app.input_search_files
import com.storyteller_f.alphaheart.app.input_search_members
import com.storyteller_f.alphaheart.app.input_search_room
import com.storyteller_f.alphaheart.app.input_search_topics
import com.storyteller_f.alphaheart.app.input_search_topics_and_users
import com.storyteller_f.alphaheart.app.input_search_user_created_titles
import com.storyteller_f.alphaheart.app.input_search_user_received_titles
import com.storyteller_f.alphaheart.app.pages.community.CommunityList
import com.storyteller_f.alphaheart.app.pages.file.FileCell
import com.storyteller_f.alphaheart.app.pages.room.RoomList
import com.storyteller_f.alphaheart.app.pages.title.ComposeMenu
import com.storyteller_f.alphaheart.app.pages.title.TitleList
import com.storyteller_f.alphaheart.app.pages.topic.TopicList
import com.storyteller_f.alphaheart.app.pages.user.MemberList
import com.storyteller_f.alphaheart.app.pages.user.SelfUserIconWithDialog
import com.storyteller_f.alphaheart.app.utils.appPlatform
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.topPrepend
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

sealed interface SearchScope {
    data object World : SearchScope
    data object MyCommunity : SearchScope
    data object MyRoom : SearchScope
    data class CommunityTopic(val communityId: PrimaryKey) : SearchScope
    data class CommunityRoom(val communityId: PrimaryKey) : SearchScope
    data class RoomTopic(val roomId: PrimaryKey) : SearchScope
    data class TopicTopic(val topicId: PrimaryKey) : SearchScope
    data class CommunityMember(val communityId: PrimaryKey) : SearchScope
    data class RoomMember(val roomId: PrimaryKey) : SearchScope
    data object Users : SearchScope
    data class UserTopic(val userId: PrimaryKey) : SearchScope
    data class UserCommunities(val userId: PrimaryKey) : SearchScope
    data class UserReceivedTitle(val userId: PrimaryKey) : SearchScope
    data class UserCreatedTitle(val userId: PrimaryKey) : SearchScope
    data class UploadedFiles(val objectId: PrimaryKey, val objectType: ObjectType) : SearchScope
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSearchBar(
    scope: SearchScope,
    showSelfIcon: Boolean = true,
    title: String? = null,
    leadingIcon: @Composable () -> Unit,
) {
    val appNavFactory = LocalAppNavFactory.current
    var query by rememberSaveable {
        mutableStateOf("")
    }
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }
    var active by rememberSaveable {
        mutableStateOf(false)
    }
    var showSheet by remember {
        mutableStateOf(false)
    }
    CustomSearchBarInternal(
        scope = scope,
        title = title,
        query = query,
        updateQuery = { updatedQuery ->
            query = updatedQuery
        },
        searchQuery = searchQuery,
        updateSearch = { updatedSearch ->
            searchQuery = updatedSearch
        },
        active = active,
        onActiveChange = { updatedActive ->
            active = updatedActive
        },
        showSelfIcon = showSelfIcon,
        leadingIcon = leadingIcon,
        onClickCreate = {
            showSheet = true
        },
    )
    val sheetState = rememberModalBottomSheetState()
    ComposeMenu(showSheet, sheetState, {
        showSheet = false
    }) { objectType ->
        showSheet = false
        when (objectType) {
            ObjectType.COMMUNITY -> appNavFactory.newAppNav().gotoCommunityCompose()
            ObjectType.ROOM -> appNavFactory.newAppNav().gotoRoomCompose()
            ObjectType.TOPIC -> Unit
            ObjectType.USER -> Unit
            ObjectType.TITLE -> appNavFactory.newAppNav().gotoTitleCompose()
            ObjectType.FILE -> Unit
            ObjectType.PANEL_ACCOUNT -> Unit
        }
    }
}

@Composable
internal fun detailSearchBar(scope: SearchScope, leadingIcon: @Composable () -> Unit) {
    CustomSearchBar(scope = scope, leadingIcon = leadingIcon)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CustomSearchBarInternal(
    scope: SearchScope,
    title: String?,
    query: String,
    updateQuery: (String) -> Unit,
    searchQuery: String,
    updateSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    showSelfIcon: Boolean,
    leadingIcon: @Composable (() -> Unit),
    onClickCreate: () -> Unit,
) {
    val results =
        remember {
            movableContentOf<SearchScope, String> { searchScope, text ->
                SearchContent(searchScope, text)
            }
        }
    val canCreate = showSelfIcon && LocalUserInfo.current?.status == UserStatus.NORMAL
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = active,
    ) { onActiveChange(false) }
    BoxWithConstraints(
        Modifier.fillMaxWidth().then(if (active) Modifier.imePadding() else Modifier).onPreviewKeyEvent { event ->
            if (active && event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                onActiveChange(false)
                true
            } else {
                false
            }
        },
    ) {
        val inputField: @Composable () -> Unit = {
            SearchBarDefaults.InputField(
                modifier = Modifier.searchQuerySemantics(query, updateQuery, updateSearch, onActiveChange),
                query = query,
                onQueryChange = updateQuery,
                onSearch = updateSearch,
                expanded = active,
                onExpandedChange = onActiveChange,
                leadingIcon = { MergedLeadingIcon(leadingIcon, active, onActiveChange) },
                trailingIcon = {
                    if (showSelfIcon) SelfIcon(onClickCreate)
                },
                placeholder = { SearchPlaceholder(scope) },
            )
        }
        if (maxWidth >= 600.dp) {
            DesktopSearchLayout(title, active, inputField, {
                if (canCreate && title != null) {
                    IconButton(onClick = onClickCreate) {
                        Icon(Icons.Default.Add, stringResource(Res.string.create))
                    }
                }
                if (active) {
                    IconButton(onClick = { onActiveChange(false) }) {
                        Icon(Icons.Default.Close, stringResource(Res.string.close))
                    }
                }
            }) { results(scope, searchQuery) }
        } else {
            SearchBar(
                inputField = inputField,
                expanded = active,
                onExpandedChange = onActiveChange,
                modifier = Modifier.align(Alignment.Center),
            ) { results(scope, searchQuery) }
        }
    }
}

private fun Modifier.searchQuerySemantics(
    query: String,
    updateQuery: (String) -> Unit,
    updateSearch: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
) = appiumSemantics(
    description = "search query",
    input = true,
    inputValue = query,
    onInputValueChange = updateQuery,
    onClick = { onActiveChange(true) },
    onInputSubmit = { updateSearch(query) },
)

@Composable
private fun SelfIcon(onClickCreate: () -> Unit) {
    val myInfo = LocalUserInfo.current
    val userOverviewViewModel = getUserOverviewViewModel()
    SelfUserIconWithDialog(myInfo, overviewHandler = userOverviewViewModel.handler, onClickCreate = {
        if (myInfo?.status == UserStatus.NORMAL) onClickCreate()
    })
}

@Composable
private fun MergedLeadingIcon(leadingIcon: @Composable () -> Unit, active: Boolean, update: (Boolean) -> Unit) {
    val appNavFactory = LocalAppNavFactory.current
    if (appPlatform.hasNativeBack) {
        leadingIcon()
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val navigateBack = {
                if (active) {
                    update(false)
                } else {
                    appNavFactory.newAppNav().back()
                }
            }
            IconButton(
                onClick = navigateBack,
                modifier =
                Modifier.appiumSemantics(
                    description = "back",
                    onClick = navigateBack,
                ),
            ) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, "back")
            }
            leadingIcon()
        }
    }
}

@Composable
private fun SearchPlaceholder(scope: SearchScope) {
    Text(
        stringResource(
            when (scope) {
                SearchScope.World -> Res.string.input_search_topics_and_users
                SearchScope.MyCommunity -> Res.string.input_search_community
                SearchScope.MyRoom -> Res.string.input_search_room
                is SearchScope.CommunityTopic -> Res.string.input_search_topics
                is SearchScope.CommunityRoom -> Res.string.input_search_room
                is SearchScope.RoomTopic -> Res.string.input_search_topics
                is SearchScope.TopicTopic -> Res.string.input_search_topics
                is SearchScope.CommunityMember -> Res.string.input_search_members
                is SearchScope.RoomMember -> Res.string.input_search_members
                SearchScope.Users -> Res.string.input_search_members
                is SearchScope.UserTopic -> Res.string.input_search_topics
                is SearchScope.UserCommunities -> Res.string.input_search_community
                is SearchScope.UserReceivedTitle -> Res.string.input_search_user_received_titles
                is SearchScope.UserCreatedTitle -> Res.string.input_search_user_created_titles
                is SearchScope.UploadedFiles -> Res.string.input_search_files
            },
        ),
    )
}

@Composable
private fun SearchContent(scope: SearchScope, searchQuery: String) {
    val current = searchQuery.trim()
    when (scope) {
        SearchScope.World -> WorldSearchContent(current)
        SearchScope.MyCommunity -> MyCommunitySearchContent(current)
        SearchScope.MyRoom -> MyRoomSearchContent(current)
        is SearchScope.CommunityTopic -> CommunityTopicSearchContent(current, scope)
        is SearchScope.CommunityRoom -> CommunityRoomSearchContent(current, scope)
        is SearchScope.RoomTopic -> RoomTopicSearchContent(current, scope)
        is SearchScope.TopicTopic -> TopicTopicSearchContent(current, scope)
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
private fun TopicTopicSearchContent(current: String, scope: SearchScope.TopicTopic) {
    if (current.isNotBlank()) {
        val viewModel = createTopicSearchInTopicViewModel(scope, current)
        TopicList(viewModel)
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
                horizontalArrangement = Arrangement.Center,
            ) {
                SingleChoiceSegmentedButtonRow {
                    options.forEachIndexed { i, e ->
                        SegmentedButton(
                            currentOption == e,
                            {
                                selectedOption = i
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
                        ) {
                            Text(e.name.lowercase().replace("_", " "))
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
                horizontalArrangement = Arrangement.Center,
            ) {
                SingleChoiceSegmentedButtonRow {
                    options.forEachIndexed { i, e ->
                        SegmentedButton(
                            currentOption == e,
                            {
                                selectedOption = i
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
                        ) {
                            Text(e.name.lowercase().replace("_", " "))
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
        val tabs = listOf("Topics", "Users")
        val coroutineScope = rememberCoroutineScope()
        PrimaryTabRow(pagerState.currentPage) {
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
        HorizontalPager(pagerState) {
            if (it == 0) {
                val viewModel = createTopicSearchViewModel(current)
                TopicList(viewModel)
            } else {
                val viewModel = createMemberSearchViewModel(current)
                MemberList(viewModel)
            }
        }
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
            topPrepend(pagingItems.loadState)
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
            bottomAppending(pagingItems.loadState)
        }
    }
}
