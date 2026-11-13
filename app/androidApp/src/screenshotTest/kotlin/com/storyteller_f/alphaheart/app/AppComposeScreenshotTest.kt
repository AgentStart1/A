/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.storyteller_f.alphaheart.app.common.MessagePreview
import com.storyteller_f.alphaheart.app.common.MessageSummary
import com.storyteller_f.alphaheart.app.common.PreviewState
import com.storyteller_f.alphaheart.app.common.RoomActivityPreview
import com.storyteller_f.alphaheart.app.components.SettingOptionResettableView
import com.storyteller_f.alphaheart.app.components.SettingOptionView
import com.storyteller_f.alphaheart.app.pages.ActivityPreviewSection
import com.storyteller_f.alphaheart.app.pages.AdaptiveEditorLayout
import com.storyteller_f.alphaheart.app.pages.AdaptiveFormLayout
import com.storyteller_f.alphaheart.app.pages.AdaptiveItemGrid
import com.storyteller_f.alphaheart.app.pages.AdaptiveSectionScaffold
import com.storyteller_f.alphaheart.app.pages.DetailReadingColumn
import com.storyteller_f.alphaheart.app.pages.HomeFeedLayout
import com.storyteller_f.alphaheart.app.pages.RoomActivityPreviewContent
import com.storyteller_f.alphaheart.app.pages.file.FixedProgress
import com.storyteller_f.alphaheart.app.pages.room.PrimaryRoomCell
import com.storyteller_f.alphaheart.app.pages.room.RoomRowContent
import com.storyteller_f.alphaheart.app.pages.room.UnboundedRoomCell
import com.storyteller_f.alphaheart.app.pages.search.DesktopSearchLayout
import com.storyteller_f.alphaheart.app.ui.theme.AppTheme
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.InfoTable
import com.storyteller_f.alphaheart.client.compose_core.components.MediaObjectBlock
import com.storyteller_f.alphaheart.client.compose_core.components.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.components.Pill
import com.storyteller_f.alphaheart.client.compose_core.components.PrivateKeyEditor
import com.storyteller_f.alphaheart.client.compose_core.components.SheetContainer
import com.storyteller_f.alphaheart.client.compose_core.components.SignInButton
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.type.ObjectStatus
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun ButtonNavScreenshot() {
    ScreenshotAppTheme {
        PaddedPreview {
            ButtonNav(Icons.Default.Settings, "Settings")
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun PillScreenshot() {
    ScreenshotAppTheme {
        PaddedPreview {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(text = "Selected", icon = Icons.Default.Lock, selected = true) {}
                Pill(text = "Emoji", emoji = "A", selected = false) {}
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SignInButtonScreenshot() {
    ScreenshotAppTheme {
        PaddedPreview {
            SignInButton()
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun MediaObjectBlockScreenshot() {
    ScreenshotAppTheme {
        PaddedPreview {
            MediaObjectBlock {
                Text("Media block")
                Text("Static content for snapshot testing", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun InfoTableScreenshot() {
    ScreenshotAppTheme {
        PaddedPreview {
            InfoTable(
                listOf(
                    "Name" to "Snapshot fixture",
                    "Status" to "Ready",
                    "Scope" to "client/composeCore",
                ),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SheetContainerScreenshot() {
    ScreenshotAppTheme {
        PaddedPreview {
            SheetContainer {
                ButtonNav(Icons.Default.Edit, "Edit profile")
                ButtonNav(Icons.Default.Lock, "Security")
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun PrivateKeyEditorScreenshot() {
    ScreenshotAppTheme {
        PaddedPreview {
            PrivateKeyEditor(
                privateKey = "private-key-preview",
                encryptionPrivateKey = null,
                enableRandom = false,
                algo = com.storyteller_f.shared.model.AlgoType.P256,
                onAlgoChange = {},
                onConfirmPrivateKey = {},
                onConfirmEncryptionPrivateKey = {},
                onCancel = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun PrimaryRoomCellScreenshot() {
    ScreenshotAppTheme {
        PaddedPreview {
            PrimaryRoomCell(sampleRoom().copy(latestTopic = 12, lastRead = 3))
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun UnboundedRoomCellScreenshot() {
    ScreenshotAppTheme {
        PaddedPreview {
            UnboundedRoomCell(sampleRoom())
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SettingOptionViewScreenshot() {
    ScreenshotAppTheme {
        PaddedPreview {
            SettingOptionView("Theme", onClick = {}) {
                Text("System")
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SettingOptionResettableViewScreenshot() {
    ScreenshotAppTheme {
        PaddedPreview {
            SettingOptionResettableView("Message font size", supportReset = true, onClick = {}) {
                Text("16sp")
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun FixedProgressScreenshot() {
    ScreenshotAppTheme {
        PaddedPreview {
            FixedProgress("89.99 %")
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
private fun ScreenshotAppTheme(content: @Composable () -> Unit) {
    AppTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

private fun sampleRoom() =
    RoomInfo.EMPTY.copy(
        id = 301,
        createdTime = fixedTime(),
        name = "Snapshot room",
        memberCount = 12,
    )

private fun fixedTime() = LocalDateTime(2026, 1, 2, 3, 4, 5)

@PreviewTest
@Preview(showBackground = true, widthDp = 1000, heightDp = 200)
@Composable
fun WideRoomRowScreenshot() {
    ScreenshotAppTheme {
        RoomRowContent(
            sampleRoom().copy(
                name = "A room with a long name that should wrap cleanly",
                latestTopic = 12,
                lastRead = 3,
            ),
            CommunityInfo.EMPTY.copy(name = "Design community"),
            true,
            Modifier.padding(20.dp),
            { Icon(Icons.Default.Groups, null, Modifier.size(60.dp)) },
            {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 1280, heightDp = 600)
@Preview(showBackground = true, widthDp = 720, heightDp = 600)
@Composable
fun HomeFeedLayoutScreenshot() {
    ScreenshotAppTheme {
        HomeFeedLayout(context = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Communities", style = MaterialTheme.typography.titleMedium)
                    Text("Design community")
                    Text("Developer community")
                }
            }
        }) {
            LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(6) { index ->
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("Topic ${index + 1}", style = MaterialTheme.typography.titleMedium)
                            Text("A single reading flow with bounded content width.")
                        }
                    }
                }
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 1000, heightDp = 320)
@Composable
fun DesktopSearchResultsScreenshot() {
    ScreenshotAppTheme {
        DesktopSearchLayout("Communities", true, {
            BasicTextField("Design", {}, Modifier.fillMaxWidth().padding(16.dp))
        }, {}) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(8) { index -> Text("Community ${index + 1}", Modifier.fillMaxWidth().padding(16.dp)) }
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 1000, heightDp = 600)
@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
fun HomeNavigationScreenshot() {
    ScreenshotAppTheme {
        val routes =
            listOf(
                "World",
                "Communities",
                "Rooms",
            ).map { NavRoute("/${it.lowercase()}", Icons.Default.Groups, it) }
        AdaptiveSectionScaffold(rememberPagerState { 3 }, routes, true, search = { page, compact ->
            Text(
                routes[page].label,
                Modifier.padding(if (compact) 12.dp else 24.dp),
                style = MaterialTheme.typography.titleLarge,
            )
        }) { page ->
            Column(Modifier.fillMaxSize().padding(16.dp)) { Text("${routes[page].label} content") }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 1000, heightDp = 400)
@Preview(showBackground = true, widthDp = 390, heightDp = 400)
@Composable
fun CommunityGridScreenshot() {
    ScreenshotAppTheme {
        AdaptiveItemGrid {
            items(12) { index ->
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Groups, null, Modifier.size(50.dp))
                        Text("Community ${index + 1}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 1000, heightDp = 240, fontScale = 1.5f)
@Composable
fun LargeTextRoomRowScreenshot() {
    ScreenshotAppTheme {
        RoomRowContent(
            sampleRoom().copy(
                name = "A private room with a long name and larger text",
                status = ObjectStatus.READ_ONLY,
            ),
            null,
            true,
            Modifier.padding(20.dp),
            { Icon(Icons.Default.Groups, null, Modifier.size(60.dp)) },
            {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 1280, heightDp = 400)
@Preview(showBackground = true, widthDp = 390, heightDp = 400)
@Composable
fun DetailReadingScreenshot() {
    ScreenshotAppTheme {
        DetailReadingColumn {
            Text("Room / topic", Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(10) { Text("Message ${it + 1}", Modifier.fillMaxWidth().padding(16.dp)) }
            }
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
                BasicTextField("Message draft", {}, Modifier.fillMaxWidth().padding(20.dp))
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 1000, heightDp = 200)
@Preview(showBackground = true, widthDp = 390, heightDp = 260, fontScale = 1.5f)
@Composable
fun ActivityRoomRowScreenshot() {
    ScreenshotAppTheme {
        BoxWithConstraints {
            val room = sampleRoom().copy(name = "Room with recent activity", latestTopic = 10)
            RoomRowContent(
                room,
                null,
                maxWidth >= 760.dp,
                Modifier.padding(20.dp),
                { Icon(Icons.Default.Groups, null, Modifier.size(60.dp)) },
                {},
                RoomActivityPreview(
                    room,
                    42,
                    MessagePreview(
                        MessageSummary.Text("A recent message that wraps within the reading column."),
                        "Sample author",
                        "2026-01-01 12:00",
                    ),
                ),
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 300, heightDp = 340)
@Composable
fun UnreadActivityContextScreenshot() {
    ScreenshotAppTheme {
        val room = sampleRoom().copy(name = "Private room")
        ActivityPreviewSection(
            "Unread rooms",
            PreviewState.Ready(
                persistentListOf(
                    RoomActivityPreview(
                        room,
                        12,
                        MessagePreview(MessageSummary.Unavailable, "Sample author", "2026-01-01 12:00"),
                    ),
                ),
                9,
            ),
            {},
        ) {
            RoomActivityPreviewContent(it)
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 300, heightDp = 240)
@Composable
fun ActivityErrorContextScreenshot() {
    ScreenshotAppTheme {
        ActivityPreviewSection<RoomActivityPreview>("Unread rooms", PreviewState.Failed, {}) {
            RoomActivityPreviewContent(it)
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 540, heightDp = 480)
@Preview(showBackground = true, widthDp = 1200, heightDp = 480)
@Composable
fun AdaptiveSettingsScreenshot() {
    ScreenshotAppTheme {
        AdaptiveFormLayout {
            SettingOptionView("Name", {}) { Text("Example community") }
            SettingOptionView("Alias", {}) { Text("example") }
            SettingOptionView("Content font", {}) { Text("Default") }
            SettingOptionView("Code font", {}) { Text("Monospace") }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 700, heightDp = 480)
@Preview(showBackground = true, widthDp = 1200, heightDp = 480)
@Composable
fun AdaptiveEditorScreenshot() {
    ScreenshotAppTheme {
        AdaptiveEditorLayout(preview = { Text("Rendered topic preview", Modifier.padding(20.dp)) }) {
            Text("Raw / Block", Modifier.padding(20.dp))
            BasicTextField("Draft topic content", {}, Modifier.fillMaxSize().padding(20.dp))
        }
    }
}
