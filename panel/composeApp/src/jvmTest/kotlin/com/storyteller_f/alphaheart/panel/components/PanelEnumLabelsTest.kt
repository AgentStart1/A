/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.log_supporting
import com.storyteller_f.alphaheart.panel.logs.parsePanelStatusLog
import com.storyteller_f.alphaheart.panel.review_user_log_supporting
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.UserLogType
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UploadRecordStatus
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
internal class PanelEnumLabelsTest {
    private val originalLocale = Locale.getDefault()

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun structuredPanelStatusActionsDoNotRewriteArbitraryDiagnosticText() {
        for (entity in listOf("user", "community", "room", "topic", "title", "file")) {
            for (status in listOf("NORMAL", "READ_ONLY")) {
                assertEquals(entity.uppercase() to status, parsePanelStatusLog("Update $entity status to $status"))
            }
        }
        assertNull(parsePanelStatusLog("Update user status to CUSTOM_STATUS"))
        assertNull(parsePanelStatusLog("User text: Update user status to NORMAL"))
        assertNull(parsePanelStatusLog("UPDATE user status to NORMAL"))
    }

    @Test
    fun englishLabelsDistinguishJoinActionFromTitleTypeAndFormatBothLogRows() {
        Locale.setDefault(Locale.ENGLISH)
        verifyLabels(chinese = false)
    }

    @Test
    fun chineseLabelsCoverEveryUserActionAndKeepTechnicalTaskCodes() {
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        verifyLabels(chinese = true)
    }

    private fun verifyLabels(chinese: Boolean) =
        runComposeUiTest {
            setContent { MaterialTheme { BusinessLabelFixture() } }
            val expected =
                if (chinese) {
                    listOf("登录", "注册", "创建", "删除", "更新", "加入", "退出", "添加子账号", "移除子账号", "收藏", "取消收藏", "订阅", "取消订阅")
                } else {
                    listOf(
                        "Sign in", "Sign up", "Create", "Delete", "Update", "Join", "Leave", "Add child account",
                        "Remove child account", "Add favorite", "Remove favorite", "Subscribe", "Unsubscribe",
                    )
                }
            assertEquals(UserLogType.entries.size, expected.size)
            UserLogType.entries.zip(expected).forEach { (type, label) ->
                onNodeWithTag("log-${type.name}").assertTextEquals(label)
            }
            onNodeWithTag("title-join").assertTextEquals(if (chinese) "加入型" else "Membership")
            onNodeWithTag("upload-failed").assertTextEquals(if (chinese) "失败" else "Failed")
            onNodeWithTag("task-code").assertTextEquals("TOPIC_ACG")
            onNodeWithTag("panel-account").assertTextEquals(if (chinese) "管理账号" else "Admin account")
            onNodeWithTag("status-action").assertTextEquals(
                if (chinese) "将社区状态设为只读" else "Set Community status to Read only",
            )
            onNodeWithTag("unknown-action").assertTextEquals("custom diagnostic action")
            onNodeWithTag("optional-missing").assertTextEquals(if (chinese) "未设置" else "Not set")
            onNodeWithTag("optional-literal").assertTextEquals("null")
            onNodeWithTag("optional-present").assertTextEquals("123")
            onNodeWithTag("optional-diagnostic").assertTextEquals("request returned null")
            onNodeWithTag("user-support").assertTextContains("123", substring = true)
            onNodeWithTag("user-support").assertTextContains("2026-11-16 12:00 UTC+08:00", substring = true)
            onNodeWithTag("panel-support").assertTextContains("456", substring = true)
            onNodeWithTag("panel-support").assertTextContains("2026-11-16 12:00 UTC+08:00", substring = true)
        }
}

@Composable
private fun BusinessLabelFixture() {
    Column {
        UserLogType.entries.forEach { Text(panelUserLogText(it), Modifier.testTag("log-${it.name}")) }
        Text(panelValueText(TitleType.JOIN.name), Modifier.testTag("title-join"))
        Text(panelValueText(UploadRecordStatus.FAILED.name), Modifier.testTag("upload-failed"))
        Text(panelValueText(TaskRecordType.TOPIC_ACG.name), Modifier.testTag("task-code"))
        Text(panelValueText(ObjectType.PANEL_ACCOUNT.name), Modifier.testTag("panel-account"))
        Text(panelLogActionText("Update community status to READ_ONLY"), Modifier.testTag("status-action"))
        Text(panelLogActionText("custom diagnostic action"), Modifier.testTag("unknown-action"))
        Text(panelOptionalValueText(null), Modifier.testTag("optional-missing"))
        Text(panelOptionalValueText("null"), Modifier.testTag("optional-literal"))
        Text(panelOptionalValueText("123"), Modifier.testTag("optional-present"))
        Text(panelOptionalValueText("request returned null"), Modifier.testTag("optional-diagnostic"))
        val objectLabel = panelValueText(ObjectType.USER.name)
        val time = "2026-11-16 12:00 UTC+08:00"
        Text(
            stringResource(Res.string.review_user_log_supporting, objectLabel, "123", time),
            Modifier.testTag("user-support"),
        )
        Text(
            stringResource(Res.string.log_supporting, objectLabel, "123", "456", time),
            Modifier.testTag("panel-support"),
        )
    }
}
