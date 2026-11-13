/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storyteller_f.alphaheart.app.IAccountInstance
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.activity_pinned_topics
import com.storyteller_f.alphaheart.app.activity_unread_rooms
import com.storyteller_f.alphaheart.app.common.createJoinedCommunitiesViewModel
import com.storyteller_f.alphaheart.app.common.createWorldViewModel
import com.storyteller_f.alphaheart.app.communities
import com.storyteller_f.alphaheart.app.home_context_description
import com.storyteller_f.alphaheart.app.pages.community.CommunityCell
import com.storyteller_f.alphaheart.app.pages.topic.TopicList
import com.storyteller_f.alphaheart.app.rooms
import com.storyteller_f.alphaheart.app.view_all
import com.storyteller_f.alphaheart.client.compose_core.components.SignInButton
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import org.jetbrains.compose.resources.stringResource

@Composable
fun WorldPage(selectSection: (Int) -> Unit = {}) {
    val viewModel = createWorldViewModel()
    HomeFeedLayout(context = { HomeContext(selectSection) }) {
        TopicList(viewModel)
    }
}

@Composable
private fun HomeContext(selectSection: (Int) -> Unit) {
    val instance by LocalUiViewModel.current.instance.collectAsState()
    val nav = LocalAppNavFactory.current
    if (instance is IAccountInstance.None) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(Res.string.home_context_description))
            SignInButton { nav.newAppNav().gotoSignIn() }
        }
        return
    }
    val host = RememberActivityPreviewHost()
    ObserveActivityEvents(host, context = true)
    val refreshBadge = RememberUnreadBadgeRefresh()
    val unread by key(host) { host.unread.collectAsStateWithLifecycle() }
    val pinned by key(host) { host.pinned.collectAsStateWithLifecycle() }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item("unread-rooms") {
            ActivityPreviewSection(
                stringResource(Res.string.activity_unread_rooms),
                unread,
                {
                    host.refreshUnread()
                    refreshBadge()
                },
                action = { TextButton({ selectSection(2) }) { Text(stringResource(Res.string.rooms)) } },
            ) { preview ->
                RoomActivityPreviewContent(preview) { nav.newAppNav().gotoRoom(preview.room.id, false) }
            }
        }
        item("pinned-topics") {
            ActivityPreviewSection(
                stringResource(Res.string.activity_pinned_topics),
                pinned,
                { host.refreshPinned() },
            ) { preview ->
                ActivityMessageContent(
                    preview.message,
                    Modifier.fillMaxWidth().clickable { nav.newAppNav().gotoTopic(preview.id) }
                        .appiumSemantics(
                            description = "pinned topic preview",
                            onClick = { nav.newAppNav().gotoTopic(preview.id) },
                        )
                        .padding(8.dp),
                )
            }
        }
        item("joined-communities") { HomeCommunityContext { selectSection(1) } }
    }
}

@Composable
private fun HomeCommunityContext(openCommunities: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(Res.string.communities),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = openCommunities) { Text(stringResource(Res.string.view_all)) }
            }
            HomeCommunityPreview(Modifier.height(300.dp))
        }
    }
}

@Composable
private fun HomeCommunityPreview(modifier: Modifier) {
    val viewModel = createJoinedCommunitiesViewModel()
    StateView(viewModel, modifier.fillMaxSize(), showRefresh = true) { items ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(minOf(items.itemCount, 3)) { index -> CommunityCell(items[index]) }
        }
    }
}
