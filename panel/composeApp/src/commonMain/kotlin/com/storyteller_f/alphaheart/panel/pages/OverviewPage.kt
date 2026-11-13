/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.OverviewViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelOverviewViewModel
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.overview
import com.storyteller_f.alphaheart.panel.overview_community_count
import com.storyteller_f.alphaheart.panel.overview_file_count
import com.storyteller_f.alphaheart.panel.overview_private_room_count
import com.storyteller_f.alphaheart.panel.overview_public_room_count
import com.storyteller_f.alphaheart.panel.overview_title_count
import com.storyteller_f.alphaheart.panel.overview_topic_count
import com.storyteller_f.alphaheart.panel.overview_user_count
import com.storyteller_f.shared.model.PanelOverview
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource

@Composable
fun OverviewPage() {
    val viewModel = createPanelOverviewViewModel()
    OverviewPageInternal(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewPageInternal(viewModel: OverviewViewModel) {
    val panelNav = LocalPanelNav.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.overview),
                        modifier = Modifier.appiumSemantics(text = stringResource(Res.string.overview)),
                    )
                },
                navigationIcon = {
                    IconButton(
                        { panelNav.open() },
                        modifier =
                        Modifier.appiumSemantics(
                            description = stringResource(Res.string.menu),
                            onClick = panelNav::open,
                        ),
                    ) {
                        Icon(Icons.Default.Menu, stringResource(Res.string.menu))
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            StateView(viewModel.handler) {
                OverviewFlow(it)
            }
        }
    }
}

class OverviewFlowPreviewProvider : PreviewParameterProvider<PanelOverview> {
    override val values: Sequence<PanelOverview>
        get() =
            sequenceOf(
                PanelOverview(1, 2, 3, 4, 5, 6, 7000, 8000),
            )
}

@Preview
@Composable
private fun OverviewFlow(@PreviewParameter(OverviewFlowPreviewProvider::class) panelOverview: PanelOverview) {
    Column(modifier = Modifier.fillMaxSize()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UserCountOverviewCell(panelOverview)
            CommunityCountOverviewCell(panelOverview)
            TopicCountOverviewCell(panelOverview)
            TitleCountOverviewCell(panelOverview)
            PrivateRoomCountOverviewCell(panelOverview)
            CommunityRoomCountOverviewCell(panelOverview)
            FileCountOverviewCell(panelOverview)
        }
    }
}

@Composable
fun UserCountOverviewCell(panelOverview: PanelOverview) {
    val panelNav = LocalPanelNav.current
    Card(onClick = {
        panelNav.gotoAllUsers()
    }) {
        Box(modifier = Modifier.padding(16.dp)) {
            val raw = stringResource(Res.string.overview_user_count)
            val text =
                remember(panelOverview.userCount, raw) {
                    buildAnnotatedString {
                        val start = raw.indexOf($$"%1$d")
                        val s = panelOverview.userCount.toString()
                        val end = start + s.length
                        append(raw.replace($$"%1$d", s))
                        addStyle(SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold), start, end)
                    }
                }
            Text(text)
        }
    }
}

@Composable
fun CommunityCountOverviewCell(panelOverview: PanelOverview) {
    val panelNav = LocalPanelNav.current
    Card(onClick = { panelNav.gotoAllCommunities() }) {
        Box(modifier = Modifier.padding(16.dp)) {
            val raw = stringResource(Res.string.overview_community_count)
            val text =
                remember(panelOverview.communityCount, raw) {
                    buildAnnotatedString {
                        val start = raw.indexOf($$"%1$d")
                        val s = panelOverview.communityCount.toString()
                        val end = start + s.length
                        append(raw.replace($$"%1$d", s))
                        addStyle(SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold), start, end)
                    }
                }
            Text(text)
        }
    }
}

@Composable
fun TopicCountOverviewCell(panelOverview: PanelOverview) {
    val panelNav = LocalPanelNav.current
    Card(onClick = { panelNav.gotoAllTopics() }) {
        Box(modifier = Modifier.padding(16.dp)) {
            val raw = stringResource(Res.string.overview_topic_count)
            val text =
                remember(panelOverview.topicCount, raw) {
                    buildAnnotatedString {
                        val start = raw.indexOf($$"%1$d")
                        val s = panelOverview.topicCount.toString()
                        val end = start + s.length
                        append(raw.replace($$"%1$d", s))
                        addStyle(SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold), start, end)
                    }
                }
            Text(text)
        }
    }
}

@Composable
fun TitleCountOverviewCell(panelOverview: PanelOverview) {
    val panelNav = LocalPanelNav.current
    Card(onClick = { panelNav.gotoAllTitles() }) {
        Box(modifier = Modifier.padding(16.dp)) {
            val raw = stringResource(Res.string.overview_title_count)
            val text =
                remember(panelOverview.titleCount, raw) {
                    buildAnnotatedString {
                        val start = raw.indexOf($$"%1$d")
                        val s = panelOverview.titleCount.toString()
                        val end = start + s.length
                        append(raw.replace($$"%1$d", s))
                        addStyle(SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold), start, end)
                    }
                }
            Text(text)
        }
    }
}

@Composable
fun PrivateRoomCountOverviewCell(panelOverview: PanelOverview) {
    val panelNav = LocalPanelNav.current
    Card(onClick = { panelNav.gotoAllPrivateRooms() }) {
        Box(modifier = Modifier.padding(16.dp)) {
            val raw = stringResource(Res.string.overview_private_room_count)
            val text =
                remember(panelOverview.privateRoomCount, raw) {
                    buildAnnotatedString {
                        val start = raw.indexOf($$"%1$d")
                        val s = panelOverview.privateRoomCount.toString()
                        val end = start + s.length
                        append(raw.replace($$"%1$d", s))
                        addStyle(SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold), start, end)
                    }
                }
            Text(text)
        }
    }
}

@Composable
fun CommunityRoomCountOverviewCell(panelOverview: PanelOverview) {
    val panelNav = LocalPanelNav.current
    Card(onClick = { panelNav.gotoAllPublicRooms() }) {
        Box(modifier = Modifier.padding(16.dp)) {
            val raw = stringResource(Res.string.overview_public_room_count)
            val text =
                remember(panelOverview.communityRoomCount, raw) {
                    buildAnnotatedString {
                        val start = raw.indexOf($$"%1$d")
                        val s = panelOverview.communityRoomCount.toString()
                        val end = start + s.length
                        append(raw.replace($$"%1$d", s))
                        addStyle(SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold), start, end)
                    }
                }
            Text(text)
        }
    }
}

@Composable
fun FileCountOverviewCell(panelOverview: PanelOverview) {
    val panelNav = LocalPanelNav.current
    Card(onClick = { panelNav.gotoAllFiles() }) {
        Box(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                val raw = stringResource(Res.string.overview_file_count)
                Text(
                    remember(panelOverview.fileCount, raw) {
                        buildAnnotatedString {
                            val start = raw.indexOf($$"%1$d")
                            val s = panelOverview.fileCount.toString()
                            val end = start + s.length
                            append(raw.replace($$"%1$d", s))
                            addStyle(SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold), start, end)
                        }
                    },
                )

                Text(
                    remember(panelOverview.fileVolume) {
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
                            ) {
                                append(HumanReadable.fileSize(panelOverview.fileVolume))
                            }
                        }
                    },
                )
            }
        }
    }
}
