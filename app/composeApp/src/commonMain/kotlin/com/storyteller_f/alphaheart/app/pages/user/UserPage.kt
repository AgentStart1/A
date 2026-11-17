/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.IdUserViewModel
import com.storyteller_f.alphaheart.app.common.createTargetUserJoinedCommunitiesViewModel
import com.storyteller_f.alphaheart.app.common.createUserTitlesViewModel
import com.storyteller_f.alphaheart.app.common.createUserTopicsViewModel
import com.storyteller_f.alphaheart.app.common.createUserViewModel
import com.storyteller_f.alphaheart.app.communities_title
import com.storyteller_f.alphaheart.app.pages.AdaptiveSectionScaffold
import com.storyteller_f.alphaheart.app.pages.community.CommunityList
import com.storyteller_f.alphaheart.app.pages.search.SearchScope
import com.storyteller_f.alphaheart.app.pages.search.detailSearchBar
import com.storyteller_f.alphaheart.app.pages.title.TitleList
import com.storyteller_f.alphaheart.app.pages.topic.UserTopicList
import com.storyteller_f.alphaheart.app.titles
import com.storyteller_f.alphaheart.app.topics
import com.storyteller_f.alphaheart.client.compose_core.components.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.components.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.navigationIcon
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.compose.resources.stringResource

@Composable
fun UserPage(uid: PrimaryKey) {
    val userViewModel = createUserViewModel(uid)
    UserPageInternal(uid, userViewModel)
}

@Composable
private fun UserPageInternal(uid: PrimaryKey, userViewModel: IdUserViewModel) {
    val user by userViewModel.handler.data.collectAsState()
    val pagerState = rememberPagerState { 3 }
    val routes =
        listOf(
            NavRoute("/topics", navigationIcon(NavigationGlyph.TEXT), stringResource(Res.string.topics)),
            NavRoute(
                "/communities",
                navigationIcon(NavigationGlyph.USERS),
                stringResource(Res.string.communities_title),
            ),
            NavRoute("/titles", navigationIcon(NavigationGlyph.AWARD), stringResource(Res.string.titles)),
        )
    AdaptiveSectionScaffold(
        pagerState,
        routes,
        modifier = Modifier.testTag("user-page"),
        search = { page, _ ->
            val searchScope =
                when (page) {
                    0 -> SearchScope.UserTopic(uid)
                    1 -> SearchScope.UserCommunities(uid)
                    else -> SearchScope.UserReceivedTitle(uid)
                }
            detailSearchBar(searchScope) { userSearchLeadingIcon(uid, user) }
        },
    ) { page ->
        when (page) {
            0 -> UserTopicList(createUserTopicsViewModel(uid))
            1 -> CommunityList(createTargetUserJoinedCommunitiesViewModel(uid))
            else -> TitleList(createUserTitlesViewModel(uid, TitleSearchType.RECEIVER))
        }
    }
}

@Composable
private fun userSearchLeadingIcon(uid: PrimaryKey, user: UserInfo?) {
    if (uid != LocalUserInfo.current?.id) UserIconWithDialog(user)
}
