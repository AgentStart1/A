/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)

package com.storyteller_f.alphaheart.panel

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.common.LocalClient
import com.storyteller_f.alphaheart.client.compose_core.components.GlobalDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOverlayHost
import com.storyteller_f.alphaheart.client.compose_core.components.rememberResourceDownloadAction
import com.storyteller_f.alphaheart.client.compose_core.icons.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.media.LocalResourceDownloadAction
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomGlobalTask
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogContext
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalTaskContext
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.compose_core.utils.BindBrowserNavigation
import com.storyteller_f.alphaheart.client.compose_core.utils.CurrentWindowSizeClass
import com.storyteller_f.alphaheart.client.compose_core.utils.SessionHistoryManager
import com.storyteller_f.alphaheart.client.compose_core.utils.addPlatformImageDecoders
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.buildSessionHistoryFactory
import com.storyteller_f.alphaheart.client.compose_core.utils.createSigninCookies
import com.storyteller_f.alphaheart.client.compose_core.utils.restoreFromStorage
import com.storyteller_f.alphaheart.client.compose_core.utils.signinActivity
import com.storyteller_f.alphaheart.client.core.ClientSessionState
import com.storyteller_f.alphaheart.client.core.ConstPassHolder
import com.storyteller_f.alphaheart.client.core.PanelSessionManager
import com.storyteller_f.alphaheart.client.core.SignResult
import com.storyteller_f.alphaheart.client.core.SimplePanelSessionManager
import com.storyteller_f.alphaheart.client.core.UserPass
import com.storyteller_f.alphaheart.client.core.createSimplePanelSessionManager
import com.storyteller_f.alphaheart.client.core.defaultClientConfigureForPanel
import com.storyteller_f.alphaheart.client.core.getClient
import com.storyteller_f.alphaheart.client.core.restoreSession
import com.storyteller_f.alphaheart.client.core.signOut
import com.storyteller_f.alphaheart.client.room.getRoomModelStorage
import com.storyteller_f.alphaheart.panel.common.OnCommunityStatusUpdated
import com.storyteller_f.alphaheart.panel.common.OnFileStatusUpdated
import com.storyteller_f.alphaheart.panel.common.OnRoomStatusUpdated
import com.storyteller_f.alphaheart.panel.common.OnTitleStatusUpdated
import com.storyteller_f.alphaheart.panel.common.OnTopicStatusUpdated
import com.storyteller_f.alphaheart.panel.common.OnUserAdded
import com.storyteller_f.alphaheart.panel.common.OnUserStatusUpdated
import com.storyteller_f.alphaheart.panel.common.PanelLoginScreen
import com.storyteller_f.alphaheart.panel.common.PanelNav
import com.storyteller_f.alphaheart.panel.common.PanelNavFactory
import com.storyteller_f.alphaheart.panel.common.PanelOverviewScreen
import com.storyteller_f.alphaheart.panel.common.newPanelNav
import com.storyteller_f.alphaheart.panel.common.panelNavSerializersModule
import com.storyteller_f.alphaheart.panel.common.rootEntryProvider
import com.storyteller_f.alphaheart.panel.ui.theme.PanelTheme
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.PanelAccountInfo
import com.storyteller_f.storage.CommunityCollection
import com.storyteller_f.storage.FileCollection
import com.storyteller_f.storage.ModelStorage
import com.storyteller_f.storage.RoomCollection
import com.storyteller_f.storage.TitleCollection
import com.storyteller_f.storage.TopicCollection
import com.storyteller_f.storage.UserCollection
import com.storyteller_f.storage.update
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingAlertDialog as AlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button

internal typealias PanelGlobalDialogController =
    CustomGlobalDialogController<GlobalDialogContext<CustomPanelSessionManager>>

internal val LocalPanelGlobalDialog =
    compositionLocalOf<PanelGlobalDialogController> {
        error("LocalPanelGlobalDialog must be provided")
    }

internal val LocalPanelGlobalTask =
    compositionLocalOf<CustomGlobalTask<GlobalTaskContext<CustomPanelSessionManager>>> {
        error("LocalPanelGlobalTask must be provided")
    }

val LocalPanelUiViewModel =
    staticCompositionLocalOf<PanelUIViewModel> {
        error("LocalPanelUiViewModel must be provided")
    }

fun createPanelUIViewModel(scope: CoroutineScope) = PanelUIViewModel(scope, PanelConfig.SERVER_URL)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context).addPlatformImageDecoders().build()
    }
    val panelUiViewModel = LocalPanelUiViewModel.current
    val panelAccountInstance by panelUiViewModel.instance.collectAsState()
    val sessionManager = panelAccountInstance.sessionManager
    val client = sessionManager.client
    val config =
        remember {
            SavedStateConfiguration {
                serializersModule = panelNavSerializersModule
            }
        }
    val backStack = rememberNavBackStack(config, PanelOverviewScreen)
    BindBrowserNavigation(backStack, config.serializersModule, "panel")
    val loginStack = rememberNavBackStack(config, PanelLoginScreen)

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    LaunchedEffect(panelAccountInstance.isAlreadySign) {
        if (!panelAccountInstance.isAlreadySign) drawerState.close()
    }
    val controller = panelAccountInstance.controller
    val task = panelAccountInstance.task
    val nav =
        remember {
            object : PanelNavFactory {
                val panelNav = newPanelNav(backStack, drawerState, scope)
                override fun newPanelNav() = panelNav
            }
        }
    CompositionLocalProvider(
        LocalClient provides client,
        LocalResourceDownloadAction provides rememberResourceDownloadAction(controller, client),
        LocalPanelNav provides nav.newPanelNav(),
        LocalPanelGlobalDialog provides controller,
        LocalPanelGlobalTask provides task,
    ) {
        PanelTheme {
            ReadingOverlayHost {
                if (panelAccountInstance.isAlreadySign) {
                    PanelNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = { permanent ->
                            PanelDrawer(scope, drawerState, nav.newPanelNav(), permanent)
                        },
                    ) {
                        mainPanelPage(backStack, nav.newPanelNav())
                    }
                } else {
                    mainPanelPage(loginStack, nav.newPanelNav(), onBack = {})
                }
            }
            GlobalDialog(controller)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
internal fun PanelNavigationDrawer(
    drawerState: DrawerState,
    drawerContent: @Composable (permanent: Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val windowSizeClass = CurrentWindowSizeClass()
    val usePermanentDrawer = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val retainedContent = rememberPanelMainContent(content)
    if (usePermanentDrawer) {
        PermanentNavigationDrawer(
            drawerContent = { drawerContent(true) },
            content = retainedContent,
        )
    } else {
        val contentDispatcher = rememberNavigationEventDispatcherOwner(enabled = !drawerState.isOpen)
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { drawerContent(false) },
            content = {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides contentDispatcher) {
                    retainedContent()
                }
            },
        )
        PanelDrawerBackHandler(drawerState)
    }
}

@Composable
internal fun rememberPanelMainContent(content: @Composable () -> Unit): @Composable () -> Unit {
    val currentContent by rememberUpdatedState(content)
    return remember { movableContentOf { currentContent() } }
}

@Composable
private fun mainPanelPage(backStack: NavBackStack<NavKey>, nav: PanelNav, onBack: () -> Unit = nav::back) {
    val windowPosture = currentWindowAdaptiveInfoV2().windowPosture
    val account by LocalPanelUiViewModel.current.instance.collectAsState()
    BoxWithConstraints(
        modifier =
        Modifier.fillMaxSize().clipToBounds().signinActivity(
            account.sessionManager,
            panel = true,
        ),
    ) {
        val contentSize = DpSize(maxWidth, maxHeight)
        val directive =
            remember(contentSize, windowPosture) {
                calculatePanelPaneDirective(contentSize, windowPosture)
            }
        val listDetailSceneStrategy =
            rememberListDetailSceneStrategy<NavKey>(directive = directive)
        NavDisplay(
            backStack = backStack,
            onBack = onBack,
            entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            sceneStrategies = listOf(listDetailSceneStrategy),
            transitionSpec = {
                slideInVertically { it } togetherWith slideOutVertically { -it }
            },
            popTransitionSpec = {
                slideInVertically { -it } togetherWith slideOutVertically { it }
            },
            predictivePopTransitionSpec = {
                slideInVertically { -it } togetherWith slideOutVertically { it }
            },
            entryProvider = rootEntryProvider(),
        )
    }
}

internal fun calculatePanelPaneDirective(contentSize: DpSize, windowPosture: Posture): PaneScaffoldDirective =
    calculatePaneScaffoldDirective(
        WindowAdaptiveInfo(
            windowSizeClass =
            WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(
                widthDp = contentSize.width.value,
                heightDp = contentSize.height.value,
            ),
            windowPosture = windowPosture,
        ),
    ).copy(defaultPanePreferredWidth = 230.dp, horizontalPartitionSpacerSize = 0.dp)

@Composable
private fun PanelDrawer(scope: CoroutineScope, drawerState: DrawerState, nav: PanelNav, permanent: Boolean) {
    val scrollContent: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            PanelDrawerContent(scope, drawerState, nav)
        }
        PanelSignoutEntry(scope)
    }
    if (permanent) {
        PermanentDrawerSheet(
            modifier = Modifier.width(170.dp),
            drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            content = scrollContent,
        )
    } else {
        ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer, content = scrollContent)
    }
}

@Composable
private fun PanelDrawerContent(scope: CoroutineScope, drawerState: DrawerState, nav: PanelNav) {
    DrawerHeader()
    DrawerNavItem(
        navigationIcon(NavigationGlyph.HOUSE),
        stringResource(Res.string.overview),
        onNavigate(scope, drawerState) { nav.gotoOverview() },
    )
    DrawerSection(stringResource(Res.string.review_nav_people_spaces))
    DrawerNavItem(
        navigationIcon(NavigationGlyph.USERS),
        stringResource(Res.string.all_users),
        onNavigate(scope, drawerState) { nav.gotoAllUsers() },
    )
    DrawerNavItem(
        navigationIcon(NavigationGlyph.PANELS_TOP_LEFT),
        stringResource(Res.string.all_communities),
        onNavigate(scope, drawerState) { nav.gotoAllCommunities() },
    )
    DrawerNavItem(
        navigationIcon(NavigationGlyph.EARTH),
        stringResource(Res.string.all_public_rooms),
        onNavigate(scope, drawerState) { nav.gotoAllPublicRooms() },
    )
    DrawerNavItem(
        navigationIcon(NavigationGlyph.LOCK_KEYHOLE),
        stringResource(Res.string.all_private_rooms),
        onNavigate(scope, drawerState) { nav.gotoAllPrivateRooms() },
    )
    DrawerSection(stringResource(Res.string.review_nav_content_files))
    DrawerNavItem(
        navigationIcon(NavigationGlyph.TEXT),
        stringResource(Res.string.all_topics),
        onNavigate(scope, drawerState) { nav.gotoAllTopics() },
    )
    DrawerNavItem(
        navigationIcon(NavigationGlyph.FOLDER),
        stringResource(Res.string.all_files),
        onNavigate(scope, drawerState) { nav.gotoAllFiles() },
    )
    DrawerNavItem(
        navigationIcon(NavigationGlyph.AWARD),
        stringResource(Res.string.all_titles),
        onNavigate(scope, drawerState) { nav.gotoAllTitles() },
    )
    DrawerSection(stringResource(Res.string.review_nav_operations))
    DrawerNavItem(
        navigationIcon(NavigationGlyph.ACTIVITY),
        stringResource(Res.string.worker_records),
        onNavigate(scope, drawerState) { nav.gotoTaskRecords() },
    )
    DrawerNavItem(
        navigationIcon(NavigationGlyph.SETTINGS_2),
        stringResource(Res.string.worker_task_configurations),
        onNavigate(scope, drawerState) { nav.gotoWorkerTasks() },
    )
}

@Composable
private fun PanelSignoutEntry(scope: CoroutineScope) {
    val uiViewModel = LocalPanelUiViewModel.current
    val account by uiViewModel.instance.collectAsState()
    val signoutHost =
        remember(uiViewModel) {
            PanelSignoutHost(scope, panelUiCoordination, uiViewModel::requestSignout)
        }
    DisposableEffect(signoutHost) {
        onDispose { signoutHost.close() }
    }
    val confirming by signoutHost.confirming.collectAsState()
    val title = stringResource(Res.string.sign_out_prompt)
    val confirmLabel = CoreStrings.ok()
    if (confirming) {
        AlertDialog(
            onDismissRequest = signoutHost::cancel,
            title = { Text(title) },
            confirmButton = {
                Button(
                    signoutHost::confirm,
                    modifier = Modifier.appiumSemantics(text = confirmLabel, onClick = signoutHost::confirm),
                ) { Text(confirmLabel) }
            },
        )
    }
    if (account.isAlreadySign) {
        HorizontalDivider()
        DrawerNavItem(
            navigationIcon(NavigationGlyph.LOG_OUT),
            stringResource(Res.string.sign_out),
            signoutHost::request,
        )
    }
}

@Composable
private fun DrawerSection(label: String) {
    Text(
        label,
        Modifier.padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DrawerHeader() {
    Text(
        stringResource(Res.string.navigation_menu),
        modifier = Modifier.padding(16.dp),
        style = MaterialTheme.typography.titleLarge,
    )
    HorizontalDivider()
}

@Composable
private fun DrawerNavItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val active = label == selectedPanelDrawerLabel()
    Surface(
        onClick = onClick,
        modifier =
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
            .semantics { selected = active }.appiumSemantics(text = label, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReadingIcon(icon, null, Modifier.size(17.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun selectedPanelDrawerLabel(): String {
    val label =
        when (panelDrawerSection(LocalPanelNav.current.backStack)) {
            PanelDrawerSection.Overview -> Res.string.overview
            PanelDrawerSection.Users -> Res.string.all_users
            PanelDrawerSection.Communities -> Res.string.all_communities
            PanelDrawerSection.PublicRooms -> Res.string.all_public_rooms
            PanelDrawerSection.PrivateRooms -> Res.string.all_private_rooms
            PanelDrawerSection.Topics -> Res.string.all_topics
            PanelDrawerSection.Files -> Res.string.all_files
            PanelDrawerSection.Titles -> Res.string.all_titles
            PanelDrawerSection.TaskRecords -> Res.string.worker_records
            PanelDrawerSection.WorkerTasks -> Res.string.worker_task_configurations
            null -> return ""
        }
    return stringResource(label)
}

private fun onNavigate(scope: CoroutineScope, drawerState: DrawerState, navigate: () -> Unit): () -> Unit =
    {
        scope.launch { drawerState.close() }
        navigate()
    }

class CustomPanelSessionManager(val proxy: SimplePanelSessionManager, val historyFactory: SessionHistoryManager) :
    PanelSessionManager by proxy {
    companion object
}

sealed interface IPanelAccountInstance {
    val accountScope: CoroutineScope

    val database: ModelStorage
    val sessionManager: CustomPanelSessionManager
    val task: CustomGlobalTask<GlobalTaskContext<CustomPanelSessionManager>>
    val controller: CustomGlobalDialogController<GlobalDialogContext<CustomPanelSessionManager>>
    val address: String

    val passHolder: ConstPassHolder

    fun close() {
        accountScope.cancel()
    }

    val isAlreadySign get() = passHolder.currentUserPass != null

    class None(parentScope: CoroutineScope, httpUrl: String) : IPanelAccountInstance {
        override val accountScope =
            CoroutineScope(
                parentScope.coroutineContext + Job(parentScope.coroutineContext[Job]),
            )
        private val scope = accountScope
        val events = MutableSharedFlow<Any>()
        override val passHolder: ConstPassHolder = ConstPassHolder(null)

        override val sessionManager: CustomPanelSessionManager =
            createPanelSessionManager(
                httpUrl,
                passHolder,
                createSigninCookies(httpUrl, "panel"),
            )
        init {
            closePanelClientWithScope(accountScope, sessionManager.client)
        }

        override val database = getRoomModelStorage("guest")
        val context: GlobalTaskContext<CustomPanelSessionManager> =
            GlobalTaskContext(events, sessionManager)
        val dialogContext: GlobalDialogContext<CustomPanelSessionManager> =
            GlobalDialogContext(events, sessionManager)
        override val task = CustomGlobalTask(scope, context)
        override val controller: CustomGlobalDialogController<GlobalDialogContext<CustomPanelSessionManager>> =
            CustomGlobalDialogController(scope, dialogContext)
        override val address: String = "guest"
    }

    class Regular(
        parentScope: CoroutineScope,
        override val address: String,
        httpUrl: String,
        override val passHolder: ConstPassHolder,
        cookiesStorage: CookiesStorage,
    ) : IPanelAccountInstance {
        override val accountScope =
            CoroutineScope(
                parentScope.coroutineContext + Job(parentScope.coroutineContext[Job]),
            )
        private val scope = accountScope
        val events = MutableSharedFlow<Any>()
        override val sessionManager: CustomPanelSessionManager =
            createPanelSessionManager(httpUrl, passHolder, cookiesStorage)
        override val database = getRoomModelStorage(address)
        val context: GlobalTaskContext<CustomPanelSessionManager> =
            GlobalTaskContext(events, sessionManager)
        val dialogContext: GlobalDialogContext<CustomPanelSessionManager> =
            GlobalDialogContext(events, sessionManager)
        override val task = CustomGlobalTask(scope, context)
        override val controller: CustomGlobalDialogController<GlobalDialogContext<CustomPanelSessionManager>> =
            CustomGlobalDialogController(scope, dialogContext)

        init {
            closePanelClientWithScope(accountScope, sessionManager.client)
            scope.launch {
                sessionManager.restoreSession().onSuccess { sessionManager.model.updateUser(it) }
            }
            scope.launch {
                events.collectLatest {
                    processEvent(it, database)
                }
            }
        }

        private suspend fun processEvent(any: Any, storage: ModelStorage) {
            when (any) {
                is OnUserAdded -> {
                    storage.user.saveToDefault(any.info)
                }

                is OnUserStatusUpdated -> handleUserStatusUpdated(storage, any)

                is OnCommunityStatusUpdated -> handleCommunityReadOnlyUpdated(storage, any)

                is OnRoomStatusUpdated -> handleRoomReadOnlyUpdated(storage, any)

                is OnTopicStatusUpdated -> handleTopicReadOnlyUpdated(storage, any)

                is OnTitleStatusUpdated -> handleTitleReadOnlyUpdated(storage, any)

                is OnFileStatusUpdated -> handleFileReadOnlyUpdated(storage, any)
            }
        }

        private suspend fun handleUserStatusUpdated(storage: ModelStorage, event: OnUserStatusUpdated) {
            storage.user.update(UserCollection.Users, event.uid) {
                it.copy(status = event.status)
            }
            storage.user.update(UserCollection.AllUsers, event.uid) {
                it.copy(status = event.status)
            }
            val overview = storage.userOverview.observeDatum().firstOrNull()
            if (overview != null && overview.userInfo.id == event.uid) {
                val updatedUser = overview.userInfo.copy(status = event.status)
                storage.userOverview.save(overview.copy(userInfo = updatedUser))
            }
        }

        private suspend fun handleCommunityReadOnlyUpdated(storage: ModelStorage, event: OnCommunityStatusUpdated) {
            storage.community.update(CommunityCollection.AllCommunities, event.id) {
                it.copy(status = event.status)
            }
            storage.community.getDocument(CommunityCollection.AllCommunities, event.id)?.let {
                storage.community.saveToDefault(it.copy(status = event.status))
            }
        }

        private suspend fun handleRoomReadOnlyUpdated(storage: ModelStorage, event: OnRoomStatusUpdated) {
            storage.room.update(RoomCollection.AllRooms(false), event.id) {
                it.copy(status = event.status)
            }
            storage.room.update(RoomCollection.AllRooms(true), event.id) {
                it.copy(status = event.status)
            }
            val room =
                storage.room.getDocument(RoomCollection.AllRooms(false), event.id)
                    ?: storage.room.getDocument(RoomCollection.AllRooms(true), event.id)
            if (room != null) {
                storage.room.saveToDefault(room.copy(status = event.status))
            }
        }

        private suspend fun handleTopicReadOnlyUpdated(storage: ModelStorage, event: OnTopicStatusUpdated) {
            storage.topic.update(TopicCollection.AllTopics, event.id) {
                it.copy(topicInfo = it.topicInfo.copy(status = event.status))
            }
            storage.topic.getDocument(TopicCollection.AllTopics, event.id)?.let {
                storage.topic.saveToDefault(it.copy(topicInfo = it.topicInfo.copy(status = event.status)))
            }
        }

        private suspend fun handleTitleReadOnlyUpdated(storage: ModelStorage, event: OnTitleStatusUpdated) {
            storage.title.update(TitleCollection.AllTitles, event.id) {
                it.copy(titleInfo = it.titleInfo.copy(status = event.status))
            }
            storage.title.getDocument(TitleCollection.AllTitles, event.id)?.let {
                storage.title.saveToDefault(it.copy(titleInfo = it.titleInfo.copy(status = event.status)))
            }
        }

        private suspend fun handleFileReadOnlyUpdated(storage: ModelStorage, event: OnFileStatusUpdated) {
            storage.fileInfo.update(FileCollection.FileList(0), event.id) {
                it.copy(status = event.status)
            }
            storage.fileInfo.getDocument(FileCollection.FileList(0), event.id)?.let {
                storage.fileInfo.update(FileCollection.FileList(it.owner), event.id) { fileInfo ->
                    fileInfo.copy(status = event.status)
                }
                storage.fileInfo.saveToDefault(it.copy(status = event.status))
            }
        }
    }
}

fun createPanelSessionManager(
    httpUrl: String,
    passHolder: ConstPassHolder,
    cookiesStorage: CookiesStorage,
): CustomPanelSessionManager {
    val simpleManager =
        createSimplePanelSessionManager(
            passHolder,
            cookiesStorage,
        ) { m, c ->
            getClient {
                defaultClientConfigureForPanel(c, m, httpUrl)
            }
        }
    val historyManager = buildSessionHistoryFactory("panel")
    return CustomPanelSessionManager(simpleManager, historyManager)
}

private val panelUiCoordination = Dispatchers.Default.limitedParallelism(1)

class PanelUIViewModel(val viewModelScope: CoroutineScope, val httpUrl: String) {
    val instance: MutableStateFlow<IPanelAccountInstance>

    init {
        val value = IPanelAccountInstance.None(viewModelScope, httpUrl)
        instance = MutableStateFlow(value)
        viewModelScope.launch {
            instance.collectLatest { account ->
                account.sessionManager.model.sessionExpired.collect { expired ->
                    if (expired && account !is IPanelAccountInstance.None) {
                        account.sessionManager.historyFactory.exitSession(account.address)
                        logout()
                    }
                }
            }
        }
        viewModelScope.launch {
            val state = restoreFromStorage("panel")
            if (state is ClientSessionState.Success) {
                val address = state.userPass.address().getOrNull() ?: return@launch
                val cookies =
                    if (state.cookies.isEmpty()) {
                        createSigninCookies(
                            httpUrl,
                            "panel",
                        )
                    } else {
                        AcceptAllCookiesStorage()
                    }
                state.cookies.forEach { cookies.addCookie(io.ktor.http.Url(httpUrl), it) }
                val regular =
                    IPanelAccountInstance.Regular(
                        viewModelScope,
                        address,
                        httpUrl,
                        ConstPassHolder(state.userPass),
                        cookies,
                    )
                instance.value = regular
            }
        }
    }

    internal fun requestSignout() {
        val account = instance.value
        if (!account.isAlreadySign) return
        account.controller.launch {
            useResult {
                request { signOut() }.onSuccess {
                    account.sessionManager.historyFactory.exitSession(account.address)
                }
            }.onSuccess {
                if (instance.value === account) logout()
            }
        }
    }

    fun logout() {
        instance.value.close()
        instance.value = IPanelAccountInstance.None(viewModelScope, httpUrl)
    }

    fun login(it: SignResult<PanelAccountInfo>, pass: UserPass) {
        val old = instance.value.sessionManager.proxy.cookieManager
        instance.value =
            IPanelAccountInstance.Regular(
                viewModelScope,
                it.address,
                httpUrl,
                ConstPassHolder(pass),
                old,
            )
    }
}
