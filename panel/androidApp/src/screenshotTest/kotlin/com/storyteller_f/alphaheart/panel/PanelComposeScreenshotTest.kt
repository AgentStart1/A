/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.android.tools.screenshot.PreviewTest
import com.storyteller_f.alphaheart.panel.common.PanelNav
import com.storyteller_f.alphaheart.panel.components.InfoTable
import com.storyteller_f.alphaheart.panel.components.TopicCell
import com.storyteller_f.alphaheart.panel.ui.theme.PanelTheme
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import kotlinx.datetime.LocalDateTime

@PreviewTest
@Preview(showBackground = true, widthDp = 480)
@Composable
fun PanelTopicCellScreenshot() {
    PanelScreenshotTheme {
        PaddedPreview {
            val topic = samplePanelTopic()
            TopicCell(
                topic.copy(topicInfo = topic.topicInfo.copy(commentCount = 4, reactionCount = 9, isPin = true)),
                PreviewPanelNav,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 480)
@Composable
fun PanelInfoTableScreenshot() {
    PanelScreenshotTheme {
        PaddedPreview {
            InfoTable(
                listOf(
                    "ID" to "501",
                    "Name" to "Panel snapshot fixture",
                    "Status" to "NORMAL",
                    "Owner" to "system",
                ),
            )
        }
    }
}

@Composable
private fun PaddedPreview(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
        content()
    }
}

@Composable
private fun PanelScreenshotTheme(content: @Composable () -> Unit) {
    PanelTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

private fun samplePanelTopic() =
    TopicDetail.EMPTY.copy(
        topicInfo =
        TopicDetail.EMPTY.topicInfo.copy(
            id = 501,
            content = TopicContent.Plain("Panel topic row rendered from screenshotTest."),
            createdTime = fixedTime(),
            commentCount = 2,
            reactionCount = 3,
            isEncrypted = true,
            lastModifiedTime = fixedTime(),
        ),
    )

private object PreviewPanelNav : PanelNav {
    override val drawerState: DrawerState
        get() = error("PreviewPanelNav.drawerState is not used in screenshot previews")
    override val backStack: NavBackStack<NavKey>
        get() = error("PreviewPanelNav.backStack is not used in screenshot previews")

    override fun gotoLogin() = Unit
    override fun gotoOverview() = Unit
    override fun gotoAllUsers() = Unit
    override fun gotoUserDetail(uid: Long) = Unit
    override fun gotoAllCommunities() = Unit
    override fun gotoCommunityDetail(id: Long) = Unit
    override fun gotoAllPublicRooms() = Unit
    override fun gotoRoomDetail(id: Long) = Unit
    override fun gotoAllPrivateRooms() = Unit
    override fun gotoAllTopics() = Unit
    override fun gotoTopicDetail(id: Long) = Unit
    override fun gotoAllFiles() = Unit
    override fun gotoFileDetail(id: Long) = Unit
    override fun gotoAllTitles() = Unit
    override fun gotoTitleDetail(id: Long) = Unit
    override fun gotoTaskRecords() = Unit
    override fun gotoWorkerTasks() = Unit
    override fun gotoTaskRecordDetail(type: TaskRecordType) = Unit
    override fun gotoFilePreview(id: Long, url: String, contentType: String, name: String) = Unit
    override fun back() = Unit
    override fun open() = Unit
}

private fun fixedTime() = LocalDateTime(2026, 1, 2, 3, 4, 5)
