/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)

package com.storyteller_f.alphaheart.app.common

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.pages.HomePage
import com.storyteller_f.alphaheart.app.pages.PreferencePage
import com.storyteller_f.alphaheart.app.pages.UserHost
import com.storyteller_f.alphaheart.app.pages.community.CommunityComposePage
import com.storyteller_f.alphaheart.app.pages.community.CommunityPage
import com.storyteller_f.alphaheart.app.pages.community.CommunitySettingPage
import com.storyteller_f.alphaheart.app.pages.community.FontSettingsPage
import com.storyteller_f.alphaheart.app.pages.file.FileExplorerPage
import com.storyteller_f.alphaheart.app.pages.file.FileRefsPage
import com.storyteller_f.alphaheart.app.pages.file.FileViewPage
import com.storyteller_f.alphaheart.app.pages.room.RoomComposePage
import com.storyteller_f.alphaheart.app.pages.room.RoomPage
import com.storyteller_f.alphaheart.app.pages.room.RoomSettingPage
import com.storyteller_f.alphaheart.app.pages.title.CommunityTitleComposePage
import com.storyteller_f.alphaheart.app.pages.title.RoomTitleComposePage
import com.storyteller_f.alphaheart.app.pages.title.TitleComposePage
import com.storyteller_f.alphaheart.app.pages.topic.ReactionListPage
import com.storyteller_f.alphaheart.app.pages.topic.TopicComposeData
import com.storyteller_f.alphaheart.app.pages.topic.TopicComposePage
import com.storyteller_f.alphaheart.app.pages.topic.TopicPage
import com.storyteller_f.alphaheart.app.pages.user.MemberPage
import com.storyteller_f.alphaheart.app.pages.user.SignInAndSignUpPage
import com.storyteller_f.alphaheart.app.pages.user.UserCommentsPage
import com.storyteller_f.alphaheart.app.pages.user.UserFavoritePage
import com.storyteller_f.alphaheart.app.pages.user.UserPage
import com.storyteller_f.alphaheart.app.pages.user.UserReactionRecordsPage
import com.storyteller_f.alphaheart.app.pages.user.UserSettingPage
import com.storyteller_f.alphaheart.app.pages.user.UserSubscriptionPage
import com.storyteller_f.alphaheart.app.utils.getDeepLinkHost
import com.storyteller_f.alphaheart.app.utils.getDeepLinkScheme
import com.storyteller_f.alphaheart.client.compose_core.components.FileViewData
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Serializable
data object HomeScreen : NavKey

@Serializable
data class CommunityScreen(val communityId: PrimaryKey, val showDialog: Boolean? = null) : NavKey

@Serializable
data class RtcCallScreen(val roomId: PrimaryKey) : NavKey

@Serializable
data class RoomScreen(val roomId: PrimaryKey, val showDialog: Boolean? = null) : NavKey

@Serializable
data object SignSessionScreen : NavKey

@Serializable
data class TopicScreen(val topicId: PrimaryKey) : NavKey

@Serializable
data object AboutScreen : NavKey

@Serializable
data class UserScreen(val uid: PrimaryKey) : NavKey

@Serializable
data class TopicComposePublicRoomScreen(val roomId: PrimaryKey, val communityId: PrimaryKey) : NavKey

@Serializable
data class TopicComposePublicRoomTopicScreen(
    val roomId: PrimaryKey,
    val communityId: PrimaryKey,
    val topicId: PrimaryKey,
) : NavKey

@Serializable
data class TopicComposePrivateRoomScreen(val roomId: PrimaryKey) : NavKey

@Serializable
data class TopicComposePrivateRoomTopicScreen(val roomId: PrimaryKey, val topicId: PrimaryKey) : NavKey

@Serializable
data class TopicComposeUserScreen(val uid: PrimaryKey) : NavKey

@Serializable
data class TopicComposeUserTopicScreen(val uid: PrimaryKey, val topicId: PrimaryKey) : NavKey

@Serializable
data class TopicComposeCommunityScreen(val communityId: PrimaryKey) : NavKey

@Serializable
data class TopicComposeCommunityTopicScreen(val communityId: PrimaryKey, val topicId: PrimaryKey) : NavKey

@Serializable
data class RoomMemberScreen(val objectId: PrimaryKey) : NavKey

@Serializable
data class CommunityMemberScreen(val objectId: PrimaryKey) : NavKey

@Serializable
data object UserSettingScreen : NavKey

@Serializable
data object PreferenceScreen : NavKey

@Serializable
data class LocalImageScreen(val url: String) : NavKey

@Serializable
data class FileInfoScreen(val fileId: PrimaryKey) : NavKey

@Serializable
data object TitleComposeScreen : NavKey

@Serializable
data class CommunityTitleComposeScreen(val communityId: PrimaryKey) : NavKey

@Serializable
data class RoomTitleComposeScreen(val roomId: PrimaryKey) : NavKey

@Serializable
data object CommunityComposeScreen : NavKey

@Serializable
data object RoomComposeScreen : NavKey

@Serializable
data class CommunitySettingScreen(val communityId: PrimaryKey) : NavKey

@Serializable
data class FontSettingsScreen(val communityId: PrimaryKey) : NavKey

@Serializable
data class RoomSettingScreen(val roomId: PrimaryKey) : NavKey

@Serializable
data class ReactionListScreen(val topicId: PrimaryKey) : NavKey

@Serializable
data object FavoriteScreen : NavKey

@Serializable
data object SubscriptionScreen : NavKey

@Serializable
data object UserReactionRecordsScreen : NavKey

@Serializable
data object UserCommentsScreen : NavKey

@Serializable
data object FileExplorerScreen : NavKey

@Serializable
data class RoomFileExplorerScreen(val roomId: PrimaryKey) : NavKey

@Serializable
data class CommunityFileExplorerScreen(val communityId: PrimaryKey) : NavKey

@Serializable
data class FileRefsScreen(val fileId: PrimaryKey) : NavKey

inline fun <reified T : NavKey> AppNav.toRoute(): T? {
    val last = backStack.lastOrNull()
    if (last !is T) return null
    return last
}

val appNavSerializersModule =
    SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomeScreen::class)
            subclass(CommunityScreen::class)
            subclass(RoomScreen::class)
            subclass(RtcCallScreen::class)
            subclass(SignSessionScreen::class)
            subclass(TopicScreen::class)
            subclass(AboutScreen::class)
            subclass(UserScreen::class)
            subclass(TopicComposePublicRoomScreen::class)
            subclass(TopicComposePublicRoomTopicScreen::class)
            subclass(TopicComposePrivateRoomScreen::class)
            subclass(TopicComposePrivateRoomTopicScreen::class)
            subclass(TopicComposeUserScreen::class)
            subclass(TopicComposeUserTopicScreen::class)
            subclass(TopicComposeCommunityScreen::class)
            subclass(TopicComposeCommunityTopicScreen::class)
            subclass(RoomMemberScreen::class)
            subclass(CommunityMemberScreen::class)
            subclass(UserSettingScreen::class)
            subclass(PreferenceScreen::class)
            subclass(LocalImageScreen::class)
            subclass(FileInfoScreen::class)
            subclass(TitleComposeScreen::class)
            subclass(CommunityTitleComposeScreen::class)
            subclass(RoomTitleComposeScreen::class)
            subclass(CommunityComposeScreen::class)
            subclass(RoomComposeScreen::class)
            subclass(CommunitySettingScreen::class)
            subclass(FontSettingsScreen::class)
            subclass(RoomSettingScreen::class)
            subclass(ReactionListScreen::class)
            subclass(FavoriteScreen::class)
            subclass(SubscriptionScreen::class)
            subclass(UserReactionRecordsScreen::class)
            subclass(UserCommentsScreen::class)
            subclass(FileExplorerScreen::class)
            subclass(RoomFileExplorerScreen::class)
            subclass(CommunityFileExplorerScreen::class)
            subclass(FileRefsScreen::class)
        }
    }

@Composable
inline fun <reified T : NavKey> AppNavFactory.hasRouteFlow(crossinline block: (T) -> Boolean = { true }): Boolean {
    val navKey = newAppNav().backStack.last()
    return navKey is T && block(navKey)
}

interface AppNav {
    val backStack: NavBackStack<NavKey>

    fun gotoSignIn()

    fun requestSignIn()

    fun gotoCall(roomId: PrimaryKey)

    fun gotoRoom(roomId: PrimaryKey, showDialog: Boolean)

    fun gotoCommunity(communityId: PrimaryKey, showDialog: Boolean)

    fun gotoTopic(topicId: PrimaryKey)

    fun gotoHome()

    fun gotoTopicCompose(data: TopicComposeData)

    fun gotoMemberPage(objectId: PrimaryKey, objectType: ObjectType)

    fun gotoAbout()

    fun gotoUser(uid: PrimaryKey)

    fun back()

    fun gotoUserSetting()

    fun gotoPreference()

    fun gotoMedia(info: FileInfo)

    fun gotoLocalImage(url: String)

    fun gotoTitleCompose()

    fun gotoCommunityTitleCompose(communityId: PrimaryKey)

    fun gotoRoomTitleCompose(roomId: PrimaryKey)

    fun gotoCommunityCompose()

    fun gotoRoomCompose()

    fun gotoSettingPage(objectId: PrimaryKey, objectType: ObjectType)

    fun gotoFontSettingsPage(communityId: PrimaryKey)

    fun gotoReactionListPage(topicId: PrimaryKey)

    fun gotoFavoritePage()

    fun gotoSubscriptionPage()

    fun gotoUserReactionRecordsPage()

    fun gotoUserCommentsPage()

    fun gotoFileExplorer(objectTuple: ObjectTuple? = null)

    fun gotoFileRefs(fileId: PrimaryKey)
}

inline fun <reified T : NavKey> AppNav.hasRoute(): Boolean = backStack.last() is T

interface AppNavFactory {
    fun newAppNav(): AppNav

    companion object {
        val EMPTY =
            object : AppNavFactory {
                override fun newAppNav(): AppNav {
                    error("no app nav")
                }
            }
    }
}

// AppNav intentionally exposes one method per destination.
@Suppress("TooManyFunctions")
private class DefaultAppNav(
    override val backStack: NavBackStack<NavKey>,
    private val isSignedIn: () -> Boolean,
    private val onSignInRequired: () -> Unit,
) : AppNav {
    override fun gotoSignIn() {
        backStack.add(SignSessionScreen)
    }

    override fun requestSignIn() = onSignInRequired()

    override fun gotoCall(roomId: PrimaryKey) {
        backStack.add(RtcCallScreen(roomId))
    }

    override fun gotoRoom(roomId: PrimaryKey, showDialog: Boolean) {
        backStack.add(RoomScreen(roomId, showDialog))
    }

    override fun gotoCommunity(communityId: PrimaryKey, showDialog: Boolean) {
        backStack.add(CommunityScreen(communityId, showDialog))
    }

    override fun gotoTopic(topicId: PrimaryKey) {
        backStack.add(TopicScreen(topicId))
    }

    override fun gotoHome() {
        val i = backStack.indexOf(HomeScreen)
        if (i >= 0) {
            repeat(backStack.size - i - 1) {
                backStack.removeLastOrNull()
            }
        } else {
            backStack.add(HomeScreen)
        }
    }

    override fun gotoTopicCompose(data: TopicComposeData) {
        when (data) {
            is TopicComposeData.PublicRoom -> {
                if (data.parentTuple.objectType == ObjectType.TOPIC) {
                    backStack.add(
                        TopicComposePublicRoomTopicScreen(
                            data.roomId,
                            data.communityId,
                            data.parentTuple.objectId,
                        ),
                    )
                } else {
                    backStack.add(TopicComposePublicRoomScreen(data.roomId, data.communityId))
                }
            }

            is TopicComposeData.PrivateRoom -> {
                if (data.parentTuple.objectType == ObjectType.TOPIC) {
                    backStack.add(TopicComposePrivateRoomTopicScreen(data.roomId, data.parentTuple.objectId))
                } else {
                    backStack.add(TopicComposePrivateRoomScreen(data.roomId))
                }
            }

            is TopicComposeData.User -> {
                if (data.objectTuple.objectType == ObjectType.TOPIC) {
                    backStack.add(TopicComposeUserTopicScreen(data.uid, data.objectTuple.objectId))
                } else {
                    backStack.add(TopicComposeUserScreen(data.uid))
                }
            }

            is TopicComposeData.Community -> {
                if (data.objectTuple.objectType == ObjectType.TOPIC) {
                    backStack.add(TopicComposeCommunityTopicScreen(data.communityId, data.objectTuple.objectId))
                } else {
                    backStack.add(TopicComposeCommunityScreen(data.communityId))
                }
            }
        }
    }

    override fun gotoMemberPage(objectId: PrimaryKey, objectType: ObjectType) {
        if (objectType == ObjectType.COMMUNITY) {
            backStack.add(CommunityMemberScreen(objectId))
        } else {
            backStack.add(RoomMemberScreen(objectId))
        }
    }

    override fun gotoAbout() {
        backStack.add(AboutScreen)
    }

    override fun gotoUser(uid: PrimaryKey) {
        backStack.add(UserScreen(uid))
    }

    override fun back() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    override fun gotoUserSetting() {
        backStack.add(UserSettingScreen)
    }

    override fun gotoPreference() {
        backStack.add(PreferenceScreen)
    }

    override fun gotoMedia(info: FileInfo) {
        if (isSignedIn()) {
            backStack.add(FileInfoScreen(info.id))
        } else {
            requestSignIn()
        }
    }

    override fun gotoLocalImage(url: String) {
        backStack.add(LocalImageScreen(url))
    }

    override fun gotoTitleCompose() {
        backStack.add(TitleComposeScreen)
    }

    override fun gotoCommunityTitleCompose(communityId: PrimaryKey) {
        backStack.add(CommunityTitleComposeScreen(communityId))
    }

    override fun gotoRoomTitleCompose(roomId: PrimaryKey) {
        backStack.add(RoomTitleComposeScreen(roomId))
    }

    override fun gotoCommunityCompose() {
        backStack.add(CommunityComposeScreen)
    }

    override fun gotoRoomCompose() {
        backStack.add(RoomComposeScreen)
    }

    override fun gotoSettingPage(objectId: PrimaryKey, objectType: ObjectType) {
        if (objectType == ObjectType.COMMUNITY) {
            backStack.add(CommunitySettingScreen(objectId))
        } else {
            backStack.add(RoomSettingScreen(objectId))
        }
    }

    override fun gotoFontSettingsPage(communityId: PrimaryKey) {
        backStack.add(FontSettingsScreen(communityId))
    }

    override fun gotoReactionListPage(topicId: PrimaryKey) {
        backStack.add(ReactionListScreen(topicId))
    }

    override fun gotoFavoritePage() {
        backStack.add(FavoriteScreen)
    }

    override fun gotoSubscriptionPage() {
        backStack.add(SubscriptionScreen)
    }

    override fun gotoUserReactionRecordsPage() {
        backStack.add(UserReactionRecordsScreen)
    }

    override fun gotoUserCommentsPage() {
        backStack.add(UserCommentsScreen)
    }

    @Suppress("ElseCaseInsteadOfExhaustiveWhen")
    override fun gotoFileExplorer(objectTuple: ObjectTuple?) {
        if (objectTuple == null) {
            backStack.add(FileExplorerScreen)
        } else {
            when (objectTuple.objectType) {
                ObjectType.ROOM -> backStack.add(RoomFileExplorerScreen(objectTuple.objectId))
                ObjectType.COMMUNITY -> backStack.add(CommunityFileExplorerScreen(objectTuple.objectId))
                else -> error("Unsupported object type for file explorer: ${objectTuple.objectType}")
            }
        }
    }

    override fun gotoFileRefs(fileId: PrimaryKey) {
        backStack.add(FileRefsScreen(fileId))
    }
}

internal fun newAppNav(
    backStack: NavBackStack<NavKey>,
    isSignedIn: () -> Boolean,
    onSignInRequired: () -> Unit,
): AppNav = DefaultAppNav(backStack, isSignedIn, onSignInRequired)

@OptIn(ExperimentalResourceApi::class)
fun rootEntryProvider(nav: AppNav) =
    entryProvider {
        handleMainScreen()
        handleSettingsScreen()
        handleComposeScreen(nav)
        handleFileExplorerScreen()
        entry<AboutScreen> {
            com.storyteller_f.alphaheart.app.pages.AboutLibrariesPage()
        }
        entry<FileInfoScreen> {
            UserHost { FileViewPage(FileViewData.Regular(it.fileId)) }
        }

        entry<LocalImageScreen> {
            FileViewPage(FileViewData.LocalImage(it.url))
        }
        entry<FileRefsScreen> {
            FileRefsPage(it.fileId)
        }
    }

private fun EntryProviderScope<NavKey>.handleMainScreen() {
    entry<HomeScreen> {
        HomePage()
    }
    entry<SignSessionScreen> {
        SignInAndSignUpPage()
    }
    entry<CommunityScreen> { screen ->
        CommunityPage(screen.communityId, screen.showDialog == true)
    }
    entry<RtcCallScreen> { screen ->
        com.storyteller_f.alphaheart.app.rtc.RtcCallPage(screen.roomId)
    }
    entry<RoomScreen> { screen ->
        RoomPage(screen.roomId, screen.showDialog == true)
    }
    entry<TopicScreen> {
        TopicPage(it.topicId)
    }

    entry<RoomMemberScreen> {
        MemberPage(it.objectId, ObjectType.ROOM)
    }
    entry<CommunityMemberScreen> {
        MemberPage(it.objectId, ObjectType.COMMUNITY)
    }
    entry<UserScreen> {
        UserPage(it.uid)
    }
    entry<ReactionListScreen> {
        ReactionListPage(it.topicId)
    }
    entry<FavoriteScreen> {
        UserFavoritePage()
    }
    entry<SubscriptionScreen> {
        UserSubscriptionPage()
    }
    entry<UserReactionRecordsScreen> {
        UserReactionRecordsPage()
    }
    entry<UserCommentsScreen> {
        UserCommentsPage()
    }
}

private fun EntryProviderScope<NavKey>.handleFileExplorerScreen() {
    entry<FileExplorerScreen> {
        val id = LocalUserInfo.current?.id ?: return@entry
        FileExplorerPage(mediaTarget = id ob ObjectType.USER)
    }
    entry<RoomFileExplorerScreen> {
        FileExplorerPage(mediaTarget = it.roomId ob ObjectType.ROOM)
    }
    entry<CommunityFileExplorerScreen> {
        FileExplorerPage(mediaTarget = it.communityId ob ObjectType.COMMUNITY)
    }
}

private fun EntryProviderScope<NavKey>.handleSettingsScreen() {
    entry<UserSettingScreen> {
        UserSettingPage()
    }
    entry<PreferenceScreen> {
        PreferencePage()
    }
    entry<CommunitySettingScreen> {
        CommunitySettingPage(it.communityId)
    }
    entry<FontSettingsScreen> {
        FontSettingsPage(it.communityId)
    }
    entry<RoomSettingScreen> {
        RoomSettingPage(it.roomId)
    }
}

private fun EntryProviderScope<NavKey>.handleComposeScreen(nav: AppNav) {
    entry<TitleComposeScreen> {
        TitleComposePage()
    }
    entry<CommunityTitleComposeScreen> {
        CommunityTitleComposePage(it.communityId)
    }
    entry<RoomTitleComposeScreen> {
        RoomTitleComposePage(it.roomId)
    }
    entry<CommunityComposeScreen> {
        CommunityComposePage()
    }
    entry<RoomComposeScreen> {
        RoomComposePage()
    }
    val backPrePage: () -> Unit = {
        nav.back()
    }
    handleTopicComposeRoomScreen(backPrePage)
    handleTopicComposeUserScreen(backPrePage)
    handleTopicComposeCommunityScreen(backPrePage)
}

private fun EntryProviderScope<NavKey>.handleTopicComposeRoomScreen(backPrePage: () -> Unit) {
    entry<TopicComposePublicRoomScreen> {
        val composeData = TopicComposeData.PublicRoom(it.roomId, it.communityId, it.roomId ob ObjectType.ROOM)
        TopicComposePage(composeData, backPrePage)
    }
    entry<TopicComposePublicRoomTopicScreen> { screen ->
        val composeData =
            TopicComposeData.PublicRoom(
                screen.roomId,
                screen.communityId,
                screen.topicId ob ObjectType.TOPIC,
            )
        TopicComposePage(composeData, backPrePage)
    }
    entry<TopicComposePrivateRoomScreen> {
        val composeData = TopicComposeData.PrivateRoom(it.roomId, it.roomId ob ObjectType.ROOM)
        TopicComposePage(composeData, backPrePage)
    }
    entry<TopicComposePrivateRoomTopicScreen> {
        val composeData = TopicComposeData.PrivateRoom(it.roomId, it.topicId ob ObjectType.TOPIC)
        TopicComposePage(composeData, backPrePage)
    }
}

private fun EntryProviderScope<NavKey>.handleTopicComposeUserScreen(backPrePage: () -> Unit) {
    entry<TopicComposeUserScreen> {
        val composeData = TopicComposeData.User(it.uid, it.uid ob ObjectType.USER)
        TopicComposePage(composeData, backPrePage)
    }
    entry<TopicComposeUserTopicScreen> {
        val composeData = TopicComposeData.User(it.uid, it.topicId ob ObjectType.TOPIC)
        TopicComposePage(composeData, backPrePage)
    }
}

private fun EntryProviderScope<NavKey>.handleTopicComposeCommunityScreen(backPrePage: () -> Unit) {
    entry<TopicComposeCommunityScreen> {
        val composeData = TopicComposeData.Community(it.communityId, it.communityId ob ObjectType.COMMUNITY)
        TopicComposePage(composeData, backPrePage)
    }
    entry<TopicComposeCommunityTopicScreen> {
        val composeData = TopicComposeData.Community(it.communityId, it.topicId ob ObjectType.TOPIC)
        TopicComposePage(composeData, backPrePage)
    }
}

object ExternalUriHandler {
    private var cached: String? = null

    var listener: ((uri: String) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                cached?.let { value.invoke(it) }
                cached = null
            }
        }

    fun onNewUri(uri: String) {
        cached = uri
        listener?.let {
            it.invoke(uri)
            cached = null
        }
    }
}

fun getDeepLink(path: String): String = "${getDeepLinkScheme()}://${getDeepLinkHost()}$path"
