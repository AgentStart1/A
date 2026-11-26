/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.OverviewViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelOverviewViewModel
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.overview
import com.storyteller_f.alphaheart.panel.review_communities
import com.storyteller_f.alphaheart.panel.review_files
import com.storyteller_f.alphaheart.panel.review_private_rooms
import com.storyteller_f.alphaheart.panel.review_public_rooms
import com.storyteller_f.alphaheart.panel.review_titles
import com.storyteller_f.alphaheart.panel.review_topics
import com.storyteller_f.alphaheart.panel.review_users
import com.storyteller_f.shared.model.PanelOverview
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

@Composable
fun OverviewPage() {
    OverviewPageInternal(createPanelOverviewViewModel())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewPageInternal(viewModel: OverviewViewModel) {
    val nav = LocalPanelNav.current
    val title = stringResource(Res.string.overview)
    val menu = stringResource(Res.string.menu)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(title, Modifier.appiumSemantics(text = title)) },
            navigationIcon = {
                IconButton(nav::open, Modifier.appiumSemantics(description = menu, onClick = nav::open)) {
                    Icon(actionIcon(ActionGlyph.MENU), menu)
                }
            },
        )
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            StateView(viewModel.handler, Modifier.fillMaxSize()) { OverviewFlow(it) }
        }
    }
}

@Composable
private fun OverviewFlow(overview: PanelOverview) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        val compact = maxWidth < 600.dp
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (compact) 2 else 3),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp),
            modifier = Modifier.fillMaxSize().padding(vertical = 20.dp),
        ) {
            item { UserCountOverviewCell(overview) }
            item { CommunityCountOverviewCell(overview) }
            item { TopicCountOverviewCell(overview) }
            item { TitleCountOverviewCell(overview) }
            item { PrivateRoomCountOverviewCell(overview) }
            item { CommunityRoomCountOverviewCell(overview) }
            item { FileCountOverviewCell(overview) }
        }
    }
}

@Composable
private fun OverviewMetric(label: String, value: String, onClick: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 220.dp
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().appiumSemantics(text = "$value $label", onClick = onClick),
        ) {
            Box(Modifier.heightIn(min = if (compact) 125.dp else 140.dp).padding(if (compact) 16.dp else 20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        value,
                        fontSize = if (compact) 22.sp else 27.sp,
                        lineHeight = if (compact) 28.sp else 34.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Icon(actionIcon(ActionGlyph.EXTERNAL_LINK), null, Modifier.align(Alignment.BottomEnd))
            }
        }
    }
}

@Composable
fun UserCountOverviewCell(panelOverview: PanelOverview) {
    val nav = LocalPanelNav.current
    OverviewMetric(
        stringResource(Res.string.review_users),
        panelOverview.userCount.toString(),
        nav::gotoAllUsers,
    )
}

@Composable
fun CommunityCountOverviewCell(panelOverview: PanelOverview) {
    val nav = LocalPanelNav.current
    OverviewMetric(
        stringResource(Res.string.review_communities),
        panelOverview.communityCount.toString(),
        nav::gotoAllCommunities,
    )
}

@Composable
fun TopicCountOverviewCell(panelOverview: PanelOverview) {
    val nav = LocalPanelNav.current
    OverviewMetric(
        stringResource(Res.string.review_topics),
        panelOverview.topicCount.toString(),
        nav::gotoAllTopics,
    )
}

@Composable
fun TitleCountOverviewCell(panelOverview: PanelOverview) {
    val nav = LocalPanelNav.current
    OverviewMetric(
        stringResource(Res.string.review_titles),
        panelOverview.titleCount.toString(),
        nav::gotoAllTitles,
    )
}

@Composable
fun PrivateRoomCountOverviewCell(panelOverview: PanelOverview) {
    val nav = LocalPanelNav.current
    OverviewMetric(
        stringResource(Res.string.review_private_rooms),
        panelOverview.privateRoomCount.toString(),
        nav::gotoAllPrivateRooms,
    )
}

@Composable
fun CommunityRoomCountOverviewCell(panelOverview: PanelOverview) {
    val nav = LocalPanelNav.current
    OverviewMetric(
        stringResource(Res.string.review_public_rooms),
        panelOverview.communityRoomCount.toString(),
        nav::gotoAllPublicRooms,
    )
}

@Composable
fun FileCountOverviewCell(panelOverview: PanelOverview) {
    val nav = LocalPanelNav.current
    val volume = HumanReadable.fileSize(panelOverview.fileVolume).replace(' ', '\u00A0')
    OverviewMetric(
        stringResource(Res.string.review_files),
        "${panelOverview.fileCount} · $volume",
        nav::gotoAllFiles,
    )
}
