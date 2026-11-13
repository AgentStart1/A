/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)

package com.storyteller_f.alphaheart.app

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import com.storyteller_f.alphaheart.app.common.AppNav
import com.storyteller_f.alphaheart.app.common.AppNavFactory
import com.storyteller_f.alphaheart.app.common.DownloadContext
import com.storyteller_f.alphaheart.app.common.Downloader
import com.storyteller_f.alphaheart.app.common.DownloaderImpl
import com.storyteller_f.alphaheart.app.common.ExternalUriHandler
import com.storyteller_f.alphaheart.app.common.HomeScreen
import com.storyteller_f.alphaheart.app.common.OnTopicCreated
import com.storyteller_f.alphaheart.app.common.RoomScreen
import com.storyteller_f.alphaheart.app.common.SimpleTaskRegister
import com.storyteller_f.alphaheart.app.common.TopicScreen
import com.storyteller_f.alphaheart.app.common.Uploader
import com.storyteller_f.alphaheart.app.common.appNavSerializersModule
import com.storyteller_f.alphaheart.app.common.captureDownloadSession
import com.storyteller_f.alphaheart.app.common.createFileEngine
import com.storyteller_f.alphaheart.app.common.newAppNav
import com.storyteller_f.alphaheart.app.common.processEvent
import com.storyteller_f.alphaheart.app.common.rootEntryProvider
import com.storyteller_f.alphaheart.app.common.toRoute
import com.storyteller_f.alphaheart.app.components.SignInPrompt
import com.storyteller_f.alphaheart.app.pages.community.LocalCommunityFontStore
import com.storyteller_f.alphaheart.app.pages.community.createCommunityFontStore
import com.storyteller_f.alphaheart.app.pages.file.FileExplorerPage
import com.storyteller_f.alphaheart.app.pages.file.FileViewPage
import com.storyteller_f.alphaheart.app.pages.room.RoomPage
import com.storyteller_f.alphaheart.app.pages.topic.TopicComposeData
import com.storyteller_f.alphaheart.app.pages.user.AccountSwitch
import com.storyteller_f.alphaheart.app.pages.user.AccountSwitcher
import com.storyteller_f.alphaheart.app.ui.MaterialSymbolsOutlined
import com.storyteller_f.alphaheart.app.ui.theme.AppTheme
import com.storyteller_f.alphaheart.app.utils.ObserveAppNotificationClicks
import com.storyteller_f.alphaheart.app.utils.ProvideAppPreferences
import com.storyteller_f.alphaheart.app.utils.appPlatform
import com.storyteller_f.alphaheart.app.utils.createAppPreferencesDataStore
import com.storyteller_f.alphaheart.app.utils.initializeAppNotifications
import com.storyteller_f.alphaheart.app.utils.rememberAppNotificationPermission
import com.storyteller_f.alphaheart.app.utils.showAppNotification
import com.storyteller_f.alphaheart.client.compose_core.common.LocalClient
import com.storyteller_f.alphaheart.client.compose_core.components.AudioViewFilled
import com.storyteller_f.alphaheart.client.compose_core.components.CustomGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.CustomGlobalTask
import com.storyteller_f.alphaheart.client.compose_core.components.FileViewData
import com.storyteller_f.alphaheart.client.compose_core.components.GlobalDialog
import com.storyteller_f.alphaheart.client.compose_core.components.GlobalDialogContext
import com.storyteller_f.alphaheart.client.compose_core.components.GlobalTaskContext
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlayerService
import com.storyteller_f.alphaheart.client.compose_core.components.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.components.MediaPlaySession
import com.storyteller_f.alphaheart.client.compose_core.components.RemoteMediaItem
import com.storyteller_f.alphaheart.client.compose_core.components.Sonner
import com.storyteller_f.alphaheart.client.compose_core.components.VideoViewFilled
import com.storyteller_f.alphaheart.client.compose_core.components.rememberIsInPipMode
import com.storyteller_f.alphaheart.client.compose_core.utils.BindBrowserNavigation
import com.storyteller_f.alphaheart.client.compose_core.utils.addPlatformImageDecoders
import com.storyteller_f.alphaheart.client.compose_core.utils.buildSessionHistoryFactory
import com.storyteller_f.alphaheart.client.compose_core.utils.createSigninCookies
import com.storyteller_f.alphaheart.client.compose_core.utils.restoreFromStorage
import com.storyteller_f.alphaheart.client.compose_core.utils.signinActivity
import com.storyteller_f.alphaheart.client.core.ClientSessionState
import com.storyteller_f.alphaheart.client.core.ConstPassHolder
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.client.core.RawUserPass
import com.storyteller_f.alphaheart.client.core.SimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.UserPass
import com.storyteller_f.alphaheart.client.core.UserSessionModel
import com.storyteller_f.alphaheart.client.core.buildWebSocketUrl
import com.storyteller_f.alphaheart.client.core.createSimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.decryptCreatedTopic
import com.storyteller_f.alphaheart.client.core.defaultClientConfigure
import com.storyteller_f.alphaheart.client.core.getClient
import com.storyteller_f.alphaheart.client.core.login
import com.storyteller_f.alphaheart.client.core.restoreSession
import com.storyteller_f.alphaheart.client.core.startBackgroundTask
import com.storyteller_f.alphaheart.client.room.getRoomModelStorage
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicProjection
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.md5
import com.storyteller_f.storage.ModelStorage
import dev.tclement.fonticons.LocalIconFont
import dev.tclement.fonticons.LocalIconSize
import dev.tclement.fonticons.LocalIconTintProvider
import dev.tclement.fonticons.LocalIconWeight
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal fun getAsyncImageLoader(context: PlatformContext): ImageLoader {
    val builder = ImageLoader.Builder(context)
    return builder.addPlatformImageDecoders()
        .crossfade(true)
        .logger(DebugLogger())
        .build()
}

/**
 * 可以通过lazy 的模式获取Downloader 和 Uploader.
 */
interface ClientFileProvider {
    suspend fun getDownloader(): Downloader?
    suspend fun getUploader(): Uploader?

    companion object {
        val EMPTY =
            object : ClientFileProvider {
                override suspend fun getDownloader(): Downloader? = null

                override suspend fun getUploader(): Uploader? = null
            }
    }
}

val LocalAppNavFactory =
    compositionLocalOf {
        AppNavFactory.EMPTY
    }

val LocalSessionManager =
    compositionLocalOf<SimpleUserSessionManager> {
        error("LocalSessionManager must be provided")
    }

val LocalUserInfo =
    compositionLocalOf<UserInfo?> {
        null
    }

val LocalAccountSwitcher =
    compositionLocalOf {
        AccountSwitcher()
    }

val LocalClientFileProvider =
    compositionLocalOf {
        ClientFileProvider.EMPTY
    }

@OptIn(DelicateCoroutinesApi::class)
val LocalUiViewModel =
    staticCompositionLocalOf<UIViewModel> {
        error("LocalUiViewModel must be provided")
    }
internal typealias AppGlobalDialogController =
    CustomGlobalDialogController<GlobalDialogContext<SimpleUserSessionManager>>

val LocalGlobalDialog =
    compositionLocalOf<AppGlobalDialogController> {
        error("LocalGlobalDialog must be provided")
    }

internal typealias AppGlobalTask = CustomGlobalTask<GlobalTaskContext<SimpleUserSessionManager>>

val LocalGlobalTask =
    compositionLocalOf<AppGlobalTask> {
        error("LocalGlobalTask must be provided")
    }

@Composable
fun App() {
    remember { initializeAppNotifications() }
    CommonEntry {
        val mediaPlayerService = LocalMediaPlayerService.current
        val playerSession by mediaPlayerService.state.collectAsState()
        AppInternal(playerSession)
    }
}

@Composable
fun AppInternal(mediaPlaySession: MediaPlaySession?) {
    val isPip = rememberIsInPipMode()
    val config =
        remember {
            SavedStateConfiguration {
                serializersModule = appNavSerializersModule
            }
        }
    val backStack = rememberNavBackStack(config, HomeScreen)
    BindBrowserNavigation(backStack, config.serializersModule, "app")
    val sessionManager = LocalSessionManager.current
    val signInPromptShown = remember(backStack.lastOrNull(), sessionManager) { mutableStateOf(false) }
    val appNav =
        remember(backStack, sessionManager, signInPromptShown) {
            object : AppNavFactory {
                val appNav =
                    newAppNav(
                        backStack,
                        isSignedIn = { sessionManager.currentIsAlreadySignUp },
                        onSignInRequired = { signInPromptShown.value = true },
                    )
                override fun newAppNav() = appNav
            }
        }
    if (!isPip || mediaPlaySession == null) {
        MainPage(appNav, backStack)
    } else {
        val remoteMediaItem = mediaPlaySession.remoteMediaItem
        if (remoteMediaItem.contentType.startsWith("video")) {
            VideoViewFilled(remoteMediaItem)
        } else {
            AudioViewFilled(remoteMediaItem)
        }
    }
    SignInPrompt(signInPromptShown.value, { signInPromptShown.value = false }) {
        appNav.newAppNav().gotoSignIn()
    }
}

@Composable
private fun MainPage(appNav: AppNavFactory, backStack: NavBackStack<NavKey>) {
    DisposableEffect(Unit) {
        ExternalUriHandler.listener = { _ ->
        }
        // Removes the listener when the composable is no longer active
        onDispose {
            ExternalUriHandler.listener = null
        }
    }
    CompositionLocalProvider(
        LocalAppNavFactory provides appNav,
    ) {
        ObserveMessage()
        Box(modifier = Modifier.fillMaxSize().signinActivity(LocalSessionManager.current)) {
            NavDisplay(
                backStack = backStack,
                onBack = { appNav.newAppNav().back() },
                entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                },
                popTransitionSpec = {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                },
                predictivePopTransitionSpec = {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                },
                entryProvider = rootEntryProvider(appNav.newAppNav()),
            )
        }
    }
}

@Composable
fun CommonEntry(content: @Composable () -> Unit) {
    AppTheme(dynamicColor = true) {
        setSingletonImageLoaderFactory {
            getAsyncImageLoader(it)
        }
        val uiViewModel = LocalUiViewModel.current
        val instance by uiViewModel.instance.collectAsState()
        val currentUserSessionManager = instance.sessionManager
        val communityFonts =
            remember(uiViewModel) {
                uiViewModel.communityFonts
            }
        val user by currentUserSessionManager.model.userHandler.data.collectAsState()

        val toasterState = rememberToasterState()
        Toaster(toasterState, alignment = Alignment.TopCenter)
        GlobalDialog(instance.controller)
        CompositionLocalProvider(
            LocalSessionManager provides currentUserSessionManager,
            LocalCommunityFontStore provides communityFonts,
            LocalClient provides currentUserSessionManager.client,
            LocalToaster provides Sonner(toasterState),
            LocalGlobalDialog provides instance.controller,
            LocalGlobalTask provides instance.task,
            LocalUserInfo provides user,
            LocalRefCellHandlerProvider provides DefaultRefCellHandlerProvider,
        ) {
            ProvideFontIcon {
                val dataStore = createAppPreferencesDataStore()
                ProvideAppPreferences(dataStore) {
                    Box(Modifier.fillMaxSize()) {
                        content()
                        AccountSwitch()
                    }
                }
            }
        }
    }
}

sealed interface IAccountInstance {
    val accountScope: CoroutineScope

    val database: ModelStorage
    val sessionManager: SimpleUserSessionManager
    val task: CustomGlobalTask<GlobalTaskContext<SimpleUserSessionManager>>
    val controller: CustomGlobalDialogController<GlobalDialogContext<SimpleUserSessionManager>>
    val address: String

    val passHolder: ConstPassHolder

    fun close() {
        accountScope.cancel()
        sessionManager.client.close()
    }

    val isAlreadySign get() = passHolder.currentUserPass != null

    class None(parentScope: CoroutineScope, httpUrl: String, wsServerUrl: String) : IAccountInstance {
        override val accountScope =
            CoroutineScope(
                parentScope.coroutineContext + Job(parentScope.coroutineContext[Job]),
            )
        private val scope = accountScope
        override val passHolder: ConstPassHolder = ConstPassHolder(null)
        val events = MutableSharedFlow<Any>()
        override val sessionManager: SimpleUserSessionManager =
            createSimpleUserSessionManager(
                buildWebSocketUrl(wsServerUrl),
                createSigninCookies(httpUrl, "user"),
                passHolder,
                { m, c ->
                    buildHttpClient(
                        httpUrl,
                        c,
                        m,
                    )
                },
            ) { frame, _, _ ->
                if (frame is RoomFrame.NewTopicInfo) {
                    events.emit(OnTopicCreated(frame.topicInfo))
                }
            }
        override val database = getRoomModelStorage("guest")
        override val task: CustomGlobalTask<GlobalTaskContext<SimpleUserSessionManager>> =
            CustomGlobalTask(
                scope,
                GlobalTaskContext(events, sessionManager),
            )
        override val controller: CustomGlobalDialogController<GlobalDialogContext<SimpleUserSessionManager>> =
            CustomGlobalDialogController(
                scope,
                GlobalDialogContext(
                    events,
                    sessionManager,
                ),
            )
        override val address: String = "guest"
    }

    class Regular(
        parentScope: CoroutineScope,
        override val address: String,
        httpUrl: String,
        wsServerUrl: String,
        cookieManager: CookiesStorage,
        override val passHolder: ConstPassHolder,
    ) : IAccountInstance {
        override val accountScope =
            CoroutineScope(
                parentScope.coroutineContext + Job(parentScope.coroutineContext[Job]),
            )
        private val scope = accountScope
        private val events = MutableSharedFlow<Any>()
        override val sessionManager: SimpleUserSessionManager =
            createSimpleUserSessionManager(buildWebSocketUrl(wsServerUrl), cookieManager, passHolder, { m, c ->
                buildHttpClient(httpUrl, c, m)
            }) { frame, _, _ ->
                if (frame is RoomFrame.NewTopicInfo) {
                    events.emit(OnTopicCreated(frame.topicInfo))
                }
            }
        override val database = getRoomModelStorage(address)
        override val task: CustomGlobalTask<GlobalTaskContext<SimpleUserSessionManager>> =
            CustomGlobalTask(
                scope,
                GlobalTaskContext(events, sessionManager),
            )
        override val controller: CustomGlobalDialogController<GlobalDialogContext<SimpleUserSessionManager>> =
            CustomGlobalDialogController(
                scope,
                GlobalDialogContext(
                    events,
                    sessionManager,
                ),
            )

        init {
            scope.launch {
                processEvent(database, events)
            }
            scope.launch {
                if (passHolder.currentUserPass != null && sessionManager.model.userHandler.data.value == null) {
                    sessionManager.restoreSession().onSuccess { sessionManager.model.updateUser(it) }
                }
            }
            scope.launch {
                val jobs = sessionManager.startBackgroundTask()
                try {
                    awaitCancellation()
                } finally {
                    jobs.forEach {
                        it.cancelAndJoin()
                    }
                }
            }
        }
    }

    class Child(
        parentScope: CoroutineScope,
        override val address: String,
        httpUrl: String,
        wsServerUrl: String,
        val main: Regular,
        override val passHolder: ConstPassHolder,
    ) : IAccountInstance {
        override val accountScope =
            CoroutineScope(
                parentScope.coroutineContext + Job(parentScope.coroutineContext[Job]),
            )
        private val scope = accountScope
        val events = MutableSharedFlow<Any>()
        override val sessionManager: SimpleUserSessionManager =
            createSimpleUserSessionManager(
                buildWebSocketUrl(wsServerUrl),
                AcceptAllCookiesStorage(),
                passHolder,
                { m, c ->
                    buildHttpClient(
                        httpUrl,
                        c,
                        m,
                    )
                },
            ) { frame, _, _ ->
                if (frame is RoomFrame.NewTopicInfo) {
                    events.emit(OnTopicCreated(frame.topicInfo))
                }
            }
        override val database = getRoomModelStorage(address)
        override val task: CustomGlobalTask<GlobalTaskContext<SimpleUserSessionManager>> =
            CustomGlobalTask(
                scope,
                GlobalTaskContext(events, sessionManager),
            )
        override val controller: CustomGlobalDialogController<GlobalDialogContext<SimpleUserSessionManager>> =
            CustomGlobalDialogController(
                scope,
                GlobalDialogContext(
                    events,
                    sessionManager,
                ),
            )

        init {
            scope.launch {
                processEvent(database, events)
            }
        }

        /** Authenticates before this account is published or starts protected background requests. */
        suspend fun authenticate() {
            sessionManager.login()
            val failure = sessionManager.model.userHandler.state.value as? LoadingState.Error
            if (failure != null) throw failure.e
            checkNotNull(
                sessionManager.model.userHandler.data.value,
            ) { "Child account authentication did not complete" }
            scope.launch {
                val jobs = sessionManager.startBackgroundTask()
                try {
                    awaitCancellation()
                } finally {
                    jobs.forEach {
                        it.cancelAndJoin()
                    }
                }
            }
        }
    }
}

class UIViewModel(val viewModelScope: CoroutineScope, val wsServerUrl: String, val httpUrl: String) {
    val instance: MutableStateFlow<IAccountInstance>
    val historyManager = buildSessionHistoryFactory("user")
    internal val resourceNamespace = md5(httpUrl.trimEnd('/'))
    internal val resourceRecords = getRoomModelStorage("guest").download.forServer(resourceNamespace)
    internal val resourceFiles = createFileEngine()
    internal val resourceTasks = SimpleTaskRegister(viewModelScope)
    val downloader: Downloader =
        DownloaderImpl(
            DownloadContext(resourceNamespace, viewModelScope, resourceRecords, ::captureDownloadSession),
            resourceTasks,
            resourceFiles,
        )
    internal val communityFonts by lazy { createCommunityFontStore(this) }

    init {
        val value = IAccountInstance.None(viewModelScope, httpUrl, wsServerUrl)
        instance = MutableStateFlow(value)
        viewModelScope.launch {
            instance.collectLatest { account ->
                account.sessionManager.model.sessionExpired.collect { expired ->
                    if (expired && account !is IAccountInstance.None) {
                        historyManager.exitSession(account.address)
                        logout()
                    }
                }
            }
        }
        viewModelScope.launch {
            val state = restoreFromStorage("user")
            if (state is ClientSessionState.Success) {
                val address =
                    when (val userPass = state.userPass) {
                        is RawUserPass -> userPass.rawUSerPass.address
                        else -> userPass.address().getOrNull() ?: return@launch
                    }
                val cookies =
                    if (state.cookies.isEmpty()) {
                        createSigninCookies(
                            httpUrl,
                            "user",
                        )
                    } else {
                        AcceptAllCookiesStorage()
                    }
                state.cookies.forEach { cookies.addCookie(io.ktor.http.Url(httpUrl), it) }
                val regular =
                    IAccountInstance.Regular(
                        viewModelScope,
                        address,
                        httpUrl,
                        wsServerUrl,
                        cookies,
                        ConstPassHolder(state.userPass),
                    )
                instance.value = regular
            }
        }
    }

    fun logout() {
        val old = instance.value
        if (old is IAccountInstance.Child) {
            switchToMain()
            return
        }
        old.close()
        instance.value = IAccountInstance.None(viewModelScope, httpUrl, wsServerUrl)
    }

    suspend fun switchUser(main: IAccountInstance.Regular, rawUserPass: RawUserPass) {
        val old = instance.value
        val currentMain = if (old is IAccountInstance.Child) old.main else old
        check(currentMain === main) { "Main account changed before child authentication" }
        val child =
            IAccountInstance.Child(
                viewModelScope,
                rawUserPass.rawUSerPass.address,
                httpUrl,
                wsServerUrl,
                main,
                ConstPassHolder(rawUserPass),
            )
        try {
            child.authenticate()
            check(instance.compareAndSet(old, child)) { "Active account changed during child authentication" }
        } finally {
            if (instance.value !== child) child.close()
        }
        if (old is IAccountInstance.Child) old.close()
    }

    fun login(address: String, data: String, signature: String, userPass: UserPass, userInfo: UserInfo? = null) {
        val cookieManager = instance.value.sessionManager.cookieManager
        instance.value =
            IAccountInstance.Regular(
                viewModelScope,
                address,
                httpUrl,
                wsServerUrl,
                cookieManager,
                ConstPassHolder(userPass),
            ).apply {
                sessionManager.model.updateSignature(data, signature)
                if (userInfo != null) {
                    sessionManager.model.updateUser(userInfo)
                }
            }
    }

    fun switchToMain() {
        val old = instance.value
        if (old !is IAccountInstance.Child) {
            return
        }
        old.close()
        instance.value = old.main
    }
}

@Composable
fun ProvideFontIcon(block: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalIconFont provides MaterialSymbolsOutlined.rememberIconFont(),
        LocalIconSize provides 20.dp,
        LocalIconTintProvider provides LocalContentColor,
        LocalIconWeight provides FontWeight.Normal,
        content = {
            block()
        },
    )
}

fun buildHttpClient(httpUrl: String, cookieManager: CookiesStorage, model: UserSessionModel): HttpClient =
    if (httpUrl.isEmpty()) {
        HttpClient { }
    } else {
        getClient {
            defaultClientConfigure(cookieManager, model, httpUrl)
        }
    }

@Composable
private fun ObserveMessage() {
    val appNavFactory = LocalAppNavFactory.current
    val sessionManager = LocalSessionManager.current
    val messageToasterState = rememberToasterState()
    Toaster(messageToasterState, alignment = Alignment.TopCenter)
    val hasPermission = rememberAppNotificationPermission()
    ObserveAppNotificationClicks { payload ->
        payload[TOPIC_NOTIFICATION_ID_KEY]
            ?.toString()
            ?.toLongOrNull()
            ?.let { topicId -> appNavFactory.newAppNav().gotoTopic(topicId) }
    }
    LaunchedEffect(sessionManager) {
        sessionManager.webSocketClient.frameFlow.collect { frame ->
            if (frame is RoomFrame.NewTopicInfo) {
                val plainFrame =
                    if (frame.topicInfo.content is TopicContent.Encrypted) {
                        val topicInfo =
                            decryptCreatedTopic(frame.topicInfo, sessionManager)
                        RoomFrame.NewTopicInfo(topicInfo)
                    } else {
                        frame
                    }
                val topicInfo = plainFrame.topicInfo
                val message = topicInfo.content
                if (message is TopicContent.Plain) {
                    Napier.i(tag = "WebSocket") {
                        "ws listener ${appPlatform.isActive} $hasPermission"
                    }
                    if (appPlatform.isActive) {
                        val roomId = appNavFactory.newAppNav().toRoute<RoomScreen>()?.roomId
                        val topicId = appNavFactory.newAppNav().toRoute<TopicScreen>()?.topicId
                        if (roomId != topicInfo.parentId &&
                            topicId != topicInfo.parentId
                        ) {
                            val nickname = topicInfo.authorInfo?.nickname
                            messageToasterState.show("${nickname ?: "<none>"}: ${message.plain}")
                        }
                    } else if (hasPermission) {
                        sendTopicNotification(message, topicInfo)
                    }
                }
            }
        }
    }
}

private const val TOPIC_NOTIFICATION_ID_KEY = "topic-id"

private fun sendTopicNotification(message: TopicContent.Plain, topicInfo: TopicProjection) {
    showAppNotification(
        title = "New topic",
        body = message.plain,
        payloadData = mapOf(TOPIC_NOTIFICATION_ID_KEY to topicInfo.id.toString()),
    )
}

@Composable
fun MediaPlayerPage(remoteMediaItem: RemoteMediaItem) {
    CommonEntry {
        FileViewPage(FileViewData.Player(remoteMediaItem))
    }
}

@Composable
fun BubblePage(roomId: Long) {
    CommonEntry {
        val appNav =
            remember {
                createAppNavFactoryForBubble()
            }
        CompositionLocalProvider(
            LocalAppNavFactory provides appNav,
        ) {
            RoomPage(roomId, false)
        }
    }
}

private fun createAppNavFactoryForBubble(): AppNavFactory =
    object : AppNavFactory {
        override fun newAppNav() =
            object : AppNav {
                override val backStack: NavBackStack<NavKey>
                    get() = throw UnsupportedOperationException("Bubble navigation does not expose a back stack")

                override fun gotoSignIn() = Unit
                override fun requestSignIn() = Unit

                override fun gotoCall(roomId: PrimaryKey) = Unit

                override fun gotoRoom(roomId: PrimaryKey, showDialog: Boolean) = Unit

                override fun gotoCommunity(communityId: PrimaryKey, showDialog: Boolean) = Unit

                override fun gotoTopic(topicId: PrimaryKey) = Unit

                override fun gotoHome() = Unit

                override fun gotoTopicCompose(data: TopicComposeData) = Unit

                override fun gotoMemberPage(objectId: PrimaryKey, objectType: ObjectType) = Unit

                override fun gotoAbout() = Unit

                override fun gotoUser(uid: PrimaryKey) = Unit

                override fun back() = Unit

                override fun gotoUserSetting() = Unit

                override fun gotoPreference() = Unit

                override fun gotoMedia(info: FileInfo) = Unit

                override fun gotoLocalImage(url: String) = Unit

                override fun gotoTitleCompose() = Unit

                override fun gotoCommunityTitleCompose(communityId: PrimaryKey) = Unit

                override fun gotoRoomTitleCompose(roomId: PrimaryKey) = Unit

                override fun gotoCommunityCompose() = Unit

                override fun gotoRoomCompose() = Unit

                override fun gotoSettingPage(objectId: PrimaryKey, objectType: ObjectType) = Unit

                override fun gotoFontSettingsPage(communityId: PrimaryKey) = Unit

                override fun gotoReactionListPage(topicId: PrimaryKey) = Unit

                override fun gotoFavoritePage() = Unit

                override fun gotoSubscriptionPage() = Unit
                override fun gotoUserReactionRecordsPage() = Unit

                override fun gotoUserCommentsPage() = Unit

                override fun gotoFileExplorer(objectTuple: ObjectTuple?) = Unit

                override fun gotoFileRefs(fileId: PrimaryKey) = Unit
            }
    }

@Composable
fun UploadPage() {
    CommonEntry {
        val userInfo = LocalUserInfo.current
        if (userInfo != null) {
            FileExplorerPage(mediaTarget = userInfo.id ob ObjectType.USER)
        }
    }
}
