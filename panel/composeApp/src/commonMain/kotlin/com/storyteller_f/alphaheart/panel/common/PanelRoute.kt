/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)

package com.storyteller_f.alphaheart.panel.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.components.SignInButton
import com.storyteller_f.alphaheart.client.compose_core.components.safeArea
import com.storyteller_f.alphaheart.client.compose_core.utils.BindBrowserNavigation
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.LocalPanelUiViewModel
import com.storyteller_f.alphaheart.panel.PanelListDetailPane
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.addPanelDetail
import com.storyteller_f.alphaheart.panel.input
import com.storyteller_f.alphaheart.panel.pages.AllCommunitiesPage
import com.storyteller_f.alphaheart.panel.pages.AllFilesPage
import com.storyteller_f.alphaheart.panel.pages.AllPrivateRoomsPage
import com.storyteller_f.alphaheart.panel.pages.AllPublicRoomsPage
import com.storyteller_f.alphaheart.panel.pages.AllTitlesPage
import com.storyteller_f.alphaheart.panel.pages.AllTopicsPage
import com.storyteller_f.alphaheart.panel.pages.AllUsersPage
import com.storyteller_f.alphaheart.panel.pages.CommunityDetailPage
import com.storyteller_f.alphaheart.panel.pages.FileDetailPage
import com.storyteller_f.alphaheart.panel.pages.OverviewPage
import com.storyteller_f.alphaheart.panel.pages.PanelFilePreviewPage
import com.storyteller_f.alphaheart.panel.pages.PanelInputPage
import com.storyteller_f.alphaheart.panel.pages.RoomDetailPage
import com.storyteller_f.alphaheart.panel.pages.TaskRecordsPage
import com.storyteller_f.alphaheart.panel.pages.TitleDetailPage
import com.storyteller_f.alphaheart.panel.pages.TopicDetailPage
import com.storyteller_f.alphaheart.panel.pages.UserDetailPage
import com.storyteller_f.alphaheart.panel.panelListDetailDestination
import com.storyteller_f.alphaheart.panel.select_an_item
import com.storyteller_f.alphaheart.panel.sign_in
import com.storyteller_f.shared.model.TaskRecordType
import com.storytellerf.alphaheart.panel.pages.WorkerTasksPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.compose.resources.stringResource

@Serializable
data object LoginSelectScreen : NavKey

@Serializable
data object LoginInputScreen : NavKey

@Serializable
data class PanelUserDetailScreen(val uid: Long) : NavKey

@Serializable
data class PanelCommunityDetailScreen(val id: Long) : NavKey

@Serializable
data class PanelRoomDetailScreen(val id: Long) : NavKey

@Serializable
data class PanelTopicDetailScreen(val id: Long) : NavKey

@Serializable
data class PanelFileDetailScreen(val id: Long) : NavKey

@Serializable
data class PanelFilePreviewScreen(val id: Long) : NavKey

@Serializable
data class PanelTitleDetailScreen(val id: Long) : NavKey

@Serializable
data object PanelLoginScreen : NavKey

@Serializable
data object PanelOverviewScreen : NavKey

@Serializable
data object PanelAllUsersScreen : NavKey

@Serializable
data object PanelAllCommunitiesScreen : NavKey

@Serializable
data object PanelAllPublicRoomsScreen : NavKey

@Serializable
data object PanelAllPrivateRoomsScreen : NavKey

@Serializable
data object PanelAllTopicsScreen : NavKey

@Serializable
data object PanelAllFilesScreen : NavKey

@Serializable
data object PanelAllTitlesScreen : NavKey

@Serializable
data object PanelTaskRecordsScreen : NavKey

/** Opens the worker task configuration editor. */
@Serializable
data object PanelWorkerTasksScreen : NavKey

@Serializable
internal class PanelTaskRecordDetailScreen(val type: TaskRecordType) : NavKey

/** Serializers used by panel navigation state persistence. */
val panelNavSerializersModule: SerializersModule =
    SerializersModule {
        polymorphic(NavKey::class) {
            subclass(PanelUserDetailScreen::class)
            subclass(PanelCommunityDetailScreen::class)
            subclass(PanelRoomDetailScreen::class)
            subclass(PanelTopicDetailScreen::class)
            subclass(PanelFileDetailScreen::class)
            subclass(PanelFilePreviewScreen::class)
            subclass(PanelTitleDetailScreen::class)
            subclass(PanelLoginScreen::class)
            subclass(PanelOverviewScreen::class)
            subclass(PanelAllUsersScreen::class)
            subclass(PanelAllCommunitiesScreen::class)
            subclass(PanelAllPublicRoomsScreen::class)
            subclass(PanelAllPrivateRoomsScreen::class)
            subclass(PanelAllTopicsScreen::class)
            subclass(PanelAllFilesScreen::class)
            subclass(PanelAllTitlesScreen::class)
            subclass(PanelTaskRecordsScreen::class)
            subclass(PanelWorkerTasksScreen::class)
            subclass(PanelTaskRecordDetailScreen::class)
        }
    }

interface PanelNav {
    val drawerState: DrawerState

    val backStack: NavBackStack<NavKey>
    fun gotoLogin()
    fun gotoOverview()
    fun gotoAllUsers()
    fun gotoUserDetail(uid: Long)
    fun gotoAllCommunities()
    fun gotoCommunityDetail(id: Long)
    fun gotoAllPublicRooms()
    fun gotoRoomDetail(id: Long)
    fun gotoAllPrivateRooms()
    fun gotoAllTopics()
    fun gotoTopicDetail(id: Long)
    fun gotoAllFiles()
    fun gotoFileDetail(id: Long)
    fun gotoAllTitles()
    fun gotoTitleDetail(id: Long)
    fun gotoTaskRecords()

    /** Opens the worker task configuration editor. */
    fun gotoWorkerTasks()

    /** Opens execution history for one task type. */
    fun gotoTaskRecordDetail(type: TaskRecordType)

    fun gotoFilePreview(id: Long, url: String, contentType: String, name: String)
    fun back()
    fun open()
}

interface PanelNavFactory {
    fun newPanelNav(): PanelNav

    companion object {
        val EMPTY =
            object : PanelNavFactory {
                override fun newPanelNav(): PanelNav {
                    error("no panel nav")
                }
            }
    }
}

// PanelNav intentionally exposes one method per destination.
@Suppress("TooManyFunctions")
private class DefaultPanelNav(
    override val backStack: NavBackStack<NavKey>,
    override val drawerState: DrawerState,
    private val scope: CoroutineScope,
) : PanelNav {
    override fun gotoLogin() {
        backStack.add(PanelLoginScreen)
    }

    override fun gotoOverview() {
        backStack.add(PanelOverviewScreen)
    }

    override fun gotoAllUsers() {
        backStack.add(PanelAllUsersScreen)
    }

    override fun gotoUserDetail(uid: Long) {
        backStack.addPanelDetail(PanelUserDetailScreen(uid))
    }

    override fun gotoAllCommunities() {
        backStack.add(PanelAllCommunitiesScreen)
    }

    override fun gotoCommunityDetail(id: Long) {
        backStack.addPanelDetail(PanelCommunityDetailScreen(id))
    }

    override fun gotoAllPublicRooms() {
        backStack.add(PanelAllPublicRoomsScreen)
    }

    override fun gotoRoomDetail(id: Long) {
        backStack.addPanelDetail(PanelRoomDetailScreen(id))
    }

    override fun gotoAllPrivateRooms() {
        backStack.add(PanelAllPrivateRoomsScreen)
    }

    override fun gotoAllTopics() {
        backStack.add(PanelAllTopicsScreen)
    }

    override fun gotoTopicDetail(id: Long) {
        backStack.addPanelDetail(PanelTopicDetailScreen(id))
    }

    override fun gotoAllFiles() {
        backStack.add(PanelAllFilesScreen)
    }

    override fun gotoFileDetail(id: Long) {
        backStack.addPanelDetail(PanelFileDetailScreen(id))
    }

    override fun gotoAllTitles() {
        backStack.add(PanelAllTitlesScreen)
    }

    override fun gotoTitleDetail(id: Long) {
        backStack.addPanelDetail(PanelTitleDetailScreen(id))
    }

    override fun gotoTaskRecords() {
        backStack.add(PanelTaskRecordsScreen)
    }

    override fun gotoWorkerTasks() {
        backStack.add(PanelWorkerTasksScreen)
    }

    override fun gotoTaskRecordDetail(type: TaskRecordType) {
        backStack.add(PanelTaskRecordDetailScreen(type))
    }

    override fun gotoFilePreview(id: Long, url: String, contentType: String, name: String) {
        backStack.add(PanelFilePreviewScreen(id))
    }

    override fun back() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    override fun open() {
        scope.launch {
            drawerState.open()
        }
    }
}

internal fun newPanelNav(backStack: NavBackStack<NavKey>, drawerState: DrawerState, scope: CoroutineScope): PanelNav =
    DefaultPanelNav(backStack, drawerState, scope)

private fun panelListDetailMetadata(key: NavKey): Map<String, Any> {
    val destination = key.panelListDetailDestination() ?: return emptyMap()
    return if (destination.pane == PanelListDetailPane.List) {
        ListDetailSceneStrategy.listPane(
            sceneKey = destination.scene,
            detailPlaceholder = { PanelDetailPlaceholder() },
        )
    } else {
        ListDetailSceneStrategy.detailPane(sceneKey = destination.scene)
    }
}

internal fun rootEntryProvider(nav: PanelNav): (NavKey) -> NavEntry<NavKey> {
    val provider =
        entryProvider {
            addStandaloneEntries(nav)
            addListDetailEntries()
        }
    return provider
}

private fun EntryProviderScope<NavKey>.addStandaloneEntries(nav: PanelNav) {
    entry<PanelLoginScreen> {
        PanelLoginPage {
            nav.back()
            nav.gotoOverview()
        }
    }
    entry<PanelOverviewScreen> {
        PanelHost {
            OverviewPage()
        }
    }
    entry<PanelTaskRecordsScreen> {
        TaskRecordsPage()
    }
    entry<PanelWorkerTasksScreen> {
        WorkerTasksPage()
    }
    entry<PanelTaskRecordDetailScreen> {
        TaskRecordsPage(it.type)
    }
    entry<PanelFilePreviewScreen> {
        PanelFilePreviewPage(it.id)
    }
}

private fun EntryProviderScope<NavKey>.addListDetailEntries() {
    entry<PanelAllUsersScreen>(metadata = { panelListDetailMetadata(it) }) {
        AllUsersPage()
    }
    entry<PanelUserDetailScreen>(metadata = { panelListDetailMetadata(it) }) {
        UserDetailPage(it.uid)
    }
    entry<PanelAllCommunitiesScreen>(metadata = { panelListDetailMetadata(it) }) {
        AllCommunitiesPage()
    }
    entry<PanelCommunityDetailScreen>(metadata = { panelListDetailMetadata(it) }) {
        CommunityDetailPage(it.id)
    }
    entry<PanelAllPublicRoomsScreen>(metadata = { panelListDetailMetadata(it) }) {
        AllPublicRoomsPage()
    }
    entry<PanelRoomDetailScreen>(metadata = { panelListDetailMetadata(it) }) {
        RoomDetailPage(it.id)
    }
    entry<PanelAllPrivateRoomsScreen>(metadata = { panelListDetailMetadata(it) }) {
        AllPrivateRoomsPage()
    }
    entry<PanelAllTopicsScreen>(metadata = { panelListDetailMetadata(it) }) {
        AllTopicsPage()
    }
    entry<PanelTopicDetailScreen>(metadata = { panelListDetailMetadata(it) }) {
        TopicDetailPage(it.id)
    }
    entry<PanelAllFilesScreen>(metadata = { panelListDetailMetadata(it) }) {
        AllFilesPage()
    }
    entry<PanelFileDetailScreen>(metadata = { panelListDetailMetadata(it) }) {
        FileDetailPage(it.id)
    }
    entry<PanelAllTitlesScreen>(metadata = { panelListDetailMetadata(it) }) {
        AllTitlesPage()
    }
    entry<PanelTitleDetailScreen>(metadata = { panelListDetailMetadata(it) }) {
        TitleDetailPage(it.id)
    }
}

@Composable
private fun PanelDetailPlaceholder() {
    CenterBox {
        Text(
            text = stringResource(Res.string.select_an_item),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun PanelHost(content: @Composable () -> Unit) {
    val panelNav = LocalPanelNav.current
    val instance by LocalPanelUiViewModel.current.instance.collectAsState()
    val isAlreadySign = instance.isAlreadySign
    Scaffold {
        if (isAlreadySign) {
            content()
        } else {
            CenterBox {
                SignInButton {
                    panelNav.gotoLogin()
                }
            }
        }
    }
}

@Composable
fun PanelLoginPage(back: () -> Unit) {
    val module =
        SerializersModule {
            polymorphic(NavKey::class) {
                subclass(LoginSelectScreen::class)
                subclass(LoginInputScreen::class)
            }
        }
    val config =
        remember {
            SavedStateConfiguration {
                serializersModule = module
            }
        }
    val backStack = rememberNavBackStack(config, LoginSelectScreen)
    BindBrowserNavigation(backStack, config.serializersModule, "panel/login")
    Scaffold { paddingValues ->
        val direction = LocalLayoutDirection.current
        Box(Modifier.safeArea(paddingValues, direction)) {
            NavDisplay(
                backStack,
                entryProvider =
                entryProvider {
                    entry<LoginSelectScreen> {
                        PanelSelectLoginPage { backStack.add(LoginInputScreen) }
                    }
                    entry<LoginInputScreen> {
                        PanelInputPage(back)
                    }
                },
            )
        }
    }
}

@Composable
private fun PanelSelectLoginPage(gotoInput: () -> Unit) {
    CenterBox {
        Column(
            verticalArrangement = Arrangement.spacedBy(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.sign_in), style = MaterialTheme.typography.headlineMedium)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedButton(
                    gotoInput,
                    modifier = Modifier.appiumSemantics(text = stringResource(Res.string.input), onClick = gotoInput),
                    shape = ButtonDefaults.outlinedShape,
                ) {
                    Text(stringResource(Res.string.input))
                }
            }
        }
    }
}
