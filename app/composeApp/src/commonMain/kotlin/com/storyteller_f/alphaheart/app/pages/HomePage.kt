/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storyteller_f.alphaheart.app.IAccountInstance
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.app_name
import com.storyteller_f.alphaheart.app.common.getUnreadMessageCountViewModel
import com.storyteller_f.alphaheart.app.communities
import com.storyteller_f.alphaheart.app.components.SignInRequiredContent
import com.storyteller_f.alphaheart.app.home_start_destination_communities
import com.storyteller_f.alphaheart.app.home_start_destination_rooms
import com.storyteller_f.alphaheart.app.home_start_destination_world
import com.storyteller_f.alphaheart.app.navigation.HOME_START_DESTINATION_COMMUNITIES
import com.storyteller_f.alphaheart.app.navigation.HOME_START_DESTINATION_PREFERENCE_KEY
import com.storyteller_f.alphaheart.app.navigation.HOME_START_DESTINATION_ROOMS
import com.storyteller_f.alphaheart.app.navigation.HOME_START_DESTINATION_WORLD
import com.storyteller_f.alphaheart.app.navigation.homePageFromPreference
import com.storyteller_f.alphaheart.app.open_source_libraries
import com.storyteller_f.alphaheart.app.pages.community.MyCommunitiesPage
import com.storyteller_f.alphaheart.app.pages.room.MyRoomsPage
import com.storyteller_f.alphaheart.app.pages.search.CustomSearchBar
import com.storyteller_f.alphaheart.app.rooms
import com.storyteller_f.alphaheart.app.search.SearchScope
import com.storyteller_f.alphaheart.app.source_repository
import com.storyteller_f.alphaheart.app.utils.LocalAppPreferences
import com.storyteller_f.alphaheart.app.world
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.icons.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBasicAlertDialog as BasicAlertDialog

@Composable
fun HomePage() {
    val preferences = LocalAppPreferences.current
    val startupValues =
        remember(preferences) {
            preferences.observeString(HOME_START_DESTINATION_PREFERENCE_KEY, HOME_START_DESTINATION_WORLD)
        }
    val pagerState = rememberStartupHomePager(startupValues)
    if (pagerState == null) {
        CenterBox { CircularProgressIndicator() }
        return
    }
    val homeNavRoutes =
        listOf(
            NavRoute(
                HOME_START_DESTINATION_WORLD,
                navigationIcon(NavigationGlyph.EARTH),
                stringResource(Res.string.world),
            ),
            NavRoute(
                HOME_START_DESTINATION_COMMUNITIES,
                navigationIcon(NavigationGlyph.USERS),
                stringResource(Res.string.communities),
            ),
            NavRoute(
                HOME_START_DESTINATION_ROOMS,
                navigationIcon(NavigationGlyph.MESSAGES_SQUARE),
                stringResource(Res.string.rooms),
            ),
        )
    val unreadMessagesViewModel = getUnreadMessageCountViewModel()
    ObserveUnreadMessageCountEvents(unreadMessagesViewModel.handler)
    val unreadMessages by key(
        unreadMessagesViewModel,
    ) { unreadMessagesViewModel.handler.data.collectAsStateWithLifecycle() }
    val unreadMessageCount = unreadMessages?.unreadMessageCount ?: 0
    AdaptiveSectionScaffold(
        pagerState,
        homeNavRoutes,
        unreadMessageCount > 0,
        unreadMessageCount,
        Modifier.testTag("home"),
        { page, compact ->
            val searchScope =
                when (page) {
                    1 -> SearchScope.MyCommunity
                    2 -> SearchScope.MyRoom
                    else -> SearchScope.World
                }
            CustomSearchBar(searchScope, title = if (compact) null else homeNavRoutes[page].label) { ProjectIcon() }
        },
    ) { page ->
        HomeSection(page)
    }
}

/** Read the persisted startup choice before first mount; later choices never move an active reader. */
@Composable
internal fun rememberStartupHomePager(values: Flow<String>): PagerState? {
    val startupValue by values.collectAsState(initial = null)
    val initialPage = startupValue?.let(::homePageFromPreference) ?: return null
    return rememberPagerState(initialPage = initialPage) { 3 }
}

@Composable
internal fun homeStartDestinationLabel(value: String?): String =
    when (value) {
        HOME_START_DESTINATION_COMMUNITIES -> stringResource(Res.string.home_start_destination_communities)
        HOME_START_DESTINATION_ROOMS -> stringResource(Res.string.home_start_destination_rooms)
        else -> stringResource(Res.string.home_start_destination_world)
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProjectIcon() {
    var showDialog by remember {
        mutableStateOf(false)
    }
    Box(
        modifier =
        Modifier.size(29.dp).appiumSemantics(description = "about project", onClick = { showDialog = true })
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                showDialog = true
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("α", color = MaterialTheme.colorScheme.onPrimary)
    }
    if (showDialog) {
        BasicAlertDialog({
            showDialog = false
        }) {
            ProjectDialogInternal {
                showDialog = false
            }
        }
    }
}

@Composable
private fun HomeSection(page: Int) {
    when (page) {
        0 -> WorldPage()
        1 -> UserHost { MyCommunitiesPage() }
        else -> UserHost { MyRoomsPage() }
    }
}

@Composable
internal fun UserHost(content: @Composable () -> Unit) {
    val uiViewModel = LocalUiViewModel.current
    val session by uiViewModel.instance.collectAsState()
    val appNavFactory = LocalAppNavFactory.current
    SignInRequiredContent(
        isSignedIn = session !is IAccountInstance.None,
        onSignIn = { appNavFactory.newAppNav().gotoSignIn() },
        content = content,
    )
}

@Composable
private fun ProjectDialogInternal(dismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Surface(shape = RoundedCornerShape(8.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.titleLarge)
            val appNavFactory = LocalAppNavFactory.current
            Column {
                ButtonNav(
                    actionIcon(ActionGlyph.CODE),
                    stringResource(Res.string.source_repository),
                ) {
                    uriHandler.openUri("https://github.com/storytellerF/A")
                }
                ButtonNav(
                    actionIcon(ActionGlyph.CODE),
                    stringResource(Res.string.open_source_libraries),
                ) {
                    dismiss()
                    appNavFactory.newAppNav().gotoAbout()
                }
            }
        }
    }
}
