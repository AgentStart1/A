/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.SignInBody
import com.storyteller_f.alphaheart.api.SignInResponse
import com.storyteller_f.endpoint4k.ktor.client.invoke
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.fold
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.finalData
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.PanelAccountInfo
import com.storyteller_f.shared.model.PrimaryKeyIdentifiable
import com.storyteller_f.shared.model.TopicActivity
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicCreated
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.TopicInfo
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.checkTsIsValid
import com.storyteller_f.shared.utils.extractMarkdownMediaLink
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.ExperimentalTime

sealed interface ClientSessionState {
    data object None : ClientSessionState
    data class Success(val userPass: UserPass, val cookies: List<io.ktor.http.Cookie> = emptyList()) :
        ClientSessionState
}

interface PassHolder {
    val currentUserPass: UserPass?
}

interface SessionModel<U> {
    val sessionExpired: MutableStateFlow<Boolean>
    val uid: PrimaryKey?
    val dataAndSignature: Pair<String, String?>?
    val userHandler: FixedLoadingHandler<U?>
    fun updateSignature(data: String, signature: String?)
    fun generateData(): String
    fun clear()
    fun updateUser(u: U)
}

class SimplePassHolder : PassHolder {
    val state = MutableStateFlow<ClientSessionState>(ClientSessionState.None)

    override val currentUserPass: UserPass?
        get() = (state.value as? ClientSessionState.Success)?.userPass

    fun updateState(newState: ClientSessionState) {
        state.value = newState
    }
}

data class ConstPassHolder(override val currentUserPass: UserPass?) : PassHolder

class SimpleSessionModel<U : PrimaryKeyIdentifiable> : SessionModel<U> {
    override val sessionExpired = MutableStateFlow(false)

    // 用于header 和server 协商被签名的数据
    private var currentStamp = 0L
    override val userHandler = FixedLoadingHandler<U?>()
    override val uid: PrimaryKey?
        get() = userHandler.data.value?.id

    // currentData 是本地使用的，但是还是需要依据server 的为准
    override var dataAndSignature: Pair<String, String?>? = null

    @OptIn(ExperimentalTime::class)
    override fun generateData(): String {
        val (nowSeconds, isValid) = checkTsIsValid(currentStamp, 60 * 3)
        return if (isValid) {
            currentStamp
        } else {
            // 超时，需要替换新的
            currentStamp = nowSeconds
            nowSeconds
        }.toString()
    }

    override fun updateSignature(data: String, signature: String?) {
        dataAndSignature = data to signature
    }

    override fun updateUser(u: U) {
        sessionExpired.value = false
        userHandler.done(u)
    }

    override fun clear() {
        userHandler.done(null)
        dataAndSignature = null
    }
}

class UserSessionModel(val simpleSessionModel: SimpleSessionModel<UserInfo> = SimpleSessionModel()) :
    SessionModel<UserInfo> by simpleSessionModel

class PanelSessionModel(val simpleSessionModel: SimpleSessionModel<PanelAccountInfo> = SimpleSessionModel()) :
    SessionModel<PanelAccountInfo> by simpleSessionModel

interface SessionManager<U> {
    val client: HttpClient
    val model: SessionModel<U>

    val passHolder: PassHolder

    val currentIsAlreadySignUp get() = passHolder.currentUserPass != null
}

interface IUserSessionManager : SessionManager<UserInfo>

interface UserSessionManager : IUserSessionManager {
    val webSocketClient: WebSocketClientImpl
}

data class SimpleUserSessionManager(
    override val client: HttpClient,
    override val webSocketClient: WebSocketClientImpl,
    override val model: SessionModel<UserInfo>,
    val cookieManager: CookiesStorage,
    override val passHolder: PassHolder,
) : UserSessionManager

interface PanelSessionManager : SessionManager<PanelAccountInfo>

data class SimplePanelSessionManager(
    override val client: HttpClient,
    override val model: SessionModel<PanelAccountInfo>,
    val cookieManager: CookiesStorage,
    override val passHolder: PassHolder,
) : PanelSessionManager

fun createSimpleUserSessionManager(
    webSocketUrl: String,
    cookieManager: CookiesStorage = AcceptAllCookiesStorage(),
    passHolder: PassHolder,
    createClient: (UserSessionModel, CookiesStorage) -> HttpClient,
    onReceiveFrame: suspend (RoomFrame, UserSessionModel, DefaultClientWebSocketSession) -> Unit,
): SimpleUserSessionManager {
    val model = UserSessionModel()
    val client = createClient(model, cookieManager)
    val webSocketClient =
        WebSocketClientImpl(
            model,
            passHolder,
            { user, _ ->
                try {
                    client.webSocketSession(webSocketUrl) {
                        url.parameters.append("account", user.id.toString())
                    }
                } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    restoreSessionForSocket(client, user)
                    throw SocketHandshakeException()
                }
            },
        ) { frame, session ->
            onReceiveFrame(frame, model, session)
        }
    return SimpleUserSessionManager(client, webSocketClient, model, cookieManager, passHolder)
}

fun createSimplePanelSessionManager(
    passHolder: PassHolder,
    cookieManager: CookiesStorage = AcceptAllCookiesStorage(),
    createClient: (PanelSessionModel, CookiesStorage) -> HttpClient,
): SimplePanelSessionManager {
    val model = PanelSessionModel()
    val client = createClient(model, cookieManager)
    return SimplePanelSessionManager(client, model, cookieManager, passHolder)
}

suspend fun UserSessionManager.login() {
    val userPass = passHolder.currentUserPass ?: return
    val userHandler = model.userHandler
    userHandler.request({
        userHandler.done(it)
    }) {
        captureFailure(RequestError::from) {
            val data = getData().getOrThrow()
            val address = userPass.address().getOrThrow()
            val signature = userPass.signature(finalData(data)).getOrThrow()
            val userInfo =
                when (val response = signIn(SignInBody(address, signature)).getOrThrow()) {
                    is SignInResponse.Success -> response.userInfo
                    SignInResponse.RequiresTotp -> error("totp required")
                }
            model.updateSignature(data, signature)
            userInfo
        }
    }
}

suspend fun PanelSessionManager.login() {
    val userPass = passHolder.currentUserPass ?: return
    val userHandler = model.userHandler
    userHandler.request({
        userHandler.done(it)
    }) {
        captureFailure(RequestError::from) {
            val data = getData().getOrThrow()
            val address = userPass.address().getOrThrow()
            val signature = userPass.signature(finalData(data)).getOrThrow()
            val userInfo = signIn(SignInBody(address, signature)).getOrThrow()
            model.updateSignature(data, signature)
            userInfo
        }
    }
}

data class PreparedSignInParam(val signature: String, val address: String, val authKey: AuthKey, val data: String)

sealed interface UserAuthResult {
    data class Success(val signResult: SignResult<UserInfo>, val userPass: UserPass) : UserAuthResult
    data class RequiresTotp(val pending: PendingTotpSignIn) : UserAuthResult
}

data class PendingTotpSignIn(val authKey: AuthKey, val data: String, val signature: String, val address: String)

suspend fun prepareSignInFromPrivateKey(
    authKey: AuthKey,
    getData: suspend () -> CustomResult<String, RequestError>,
): CustomResult<PreparedSignInParam, RequestError> =
    getAlgo(authKey.algo).captureFailure(RequestError::from) {
        val data = getData().getOrThrow()
        val f = finalData(data)

        val signature = signature(authKey.derPrivateKey, f).getOrThrow()
        val address = calcAddress(authKey.derPublicKey).getOrThrow()
        PreparedSignInParam(signature, address, authKey, data)
    }.mapError(RequestError::fromError)

data class SignResult<U>(
    val userInfo: U,
    val data: String,
    val signature: String,
    val address: String,
    val authKey: AuthKey,
)

suspend fun UserSessionManager.userSignUp(authKey: AuthKey, passHolder: SimplePassHolder): UserInfo {
    val signResult = getUserSignUpPass(authKey)
    val userPass =
        RawUserPass(
            RawUserPassInfo(
                signResult.address,
                signResult.authKey,
            ),
        )
    model.updateUser(signResult.userInfo)
    model.updateSignature(signResult.data, signResult.signature)
    passHolder.updateState(ClientSessionState.Success(userPass))
    return signResult.userInfo
}

suspend fun UserSessionManager.userSignIn(authKey: AuthKey, passHolder: SimplePassHolder): UserInfo {
    val signResult = getUserSignInPass(authKey)
    val userPass =
        RawUserPass(
            RawUserPassInfo(
                signResult.address,
                signResult.authKey,
            ),
        )
    model.updateUser(signResult.userInfo)
    model.updateSignature(signResult.data, signResult.signature)
    passHolder.updateState(ClientSessionState.Success(userPass))
    return signResult.userInfo
}

suspend fun PanelSessionManager.panelSignUp(authKey: AuthKey, passHolder: SimplePassHolder): PanelAccountInfo {
    val signResult = getPanelUserSignUpPass(authKey)
    val userPass =
        RawUserPass(
            RawUserPassInfo(
                signResult.address,
                signResult.authKey,
            ),
        )
    model.updateUser(signResult.userInfo)
    model.updateSignature(signResult.data, signResult.signature)
    passHolder.updateState(ClientSessionState.Success(userPass))
    return signResult.userInfo
}

suspend fun PanelSessionManager.panelSignIn(authKey: AuthKey, passHolder: SimplePassHolder): PanelAccountInfo {
    val signResult = getPanelUserSignInPass(authKey)
    val userPass =
        RawUserPass(
            RawUserPassInfo(
                signResult.address,
                signResult.authKey,
            ),
        )
    model.updateUser(signResult.userInfo)
    model.updateSignature(signResult.data, signResult.signature)
    passHolder.updateState(ClientSessionState.Success(userPass))
    return signResult.userInfo
}

suspend fun processEncryptedTopic(topics: List<TopicDetail>, manager: UserSessionManager): List<TopicDetail> =
    topics.zip(decryptTopicBodies(topics.map { it.topicInfo }, manager)) { topic, body -> topic.copy(topicInfo = body) }

suspend fun decryptTopicActivities(topics: List<TopicActivity>, manager: UserSessionManager): List<TopicActivity> =
    topics.zip(decryptTopicBodies(topics.map { it.topicInfo }, manager)) { topic, body -> topic.copy(topicInfo = body) }

suspend fun decryptCreatedTopic(topic: TopicCreated, manager: UserSessionManager): TopicCreated =
    topic.copy(topicInfo = decryptTopicBodies(listOf(topic.topicInfo), manager).single())

@OptIn(ExperimentalStdlibApi::class)
private suspend fun decryptTopicBodies(topicInfos: List<TopicInfo>, manager: UserSessionManager): List<TopicInfo> {
    val model = manager.model
    val uid = model.uid
    val key = manager.passHolder.currentUserPass
    return topicInfos.map { topicInfo ->
        val content = topicInfo.content
        if (content !is TopicContent.Encrypted) {
            topicInfo
        } else if (uid == null || key == null) {
            topicInfo.copy(content = TopicContent.Nil)
        } else {
            val s = content.encryptedKey[uid]
            if (s != null) {
                val topicContent =
                    key.decrypt(
                        content.encrypted.hexToByteArray(),
                        s.hexToByteArray(),
                    ).fold(onSuccess = {
                        val mediaInfos =
                            extractMarkdownMediaLink(it).mapNotNull { mediaName ->
                                manager.getMediaByName(mediaName, topicInfo.rootId, topicInfo.rootType)
                                    .getOrNull()
                            }
                        TopicContent.Plain(it, mediaInfos)
                    }, onFailure = {
                        TopicContent.DecryptFailed(it.cause.message.orEmpty())
                    })
                topicInfo.copy(content = topicContent)
            } else {
                topicInfo.copy(content = TopicContent.DecryptFailed("auth failed"))
            }
        }
    }
}

private suspend fun restoreSessionForSocket(client: HttpClient, expected: UserInfo) {
    val user = with(client) { CustomApi.Accounts.session() }
    if (user.id != expected.id) throw SessionIdentityChangedException()
}

internal class SocketHandshakeException : Exception("WebSocket handshake failed; check connection settings")
