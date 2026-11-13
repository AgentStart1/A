/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import com.storyteller_f.alphaheart.api.CustomApi.Accounts.ChildAccounts.AddChildAccountRequest
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.mapError
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.UserInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class Test {
    @Test
    fun `concurrent 401 responses share an auth refresh`() =
        runTest {
            val initialResponsesReleased = CompletableDeferred<Unit>()
            val lock = Mutex()
            var initial401Count = 0
            val retryCookies = mutableListOf<String?>()
            val userPass = CountingUserPass(initialResponsesReleased)
            val passHolder =
                SimplePassHolder().also {
                    it.updateState(ClientSessionState.Success(userPass))
                }
            val manager =
                UserSessionModel().also {
                    it.updateUser(UserInfo.EMPTY)
                }
            val client =
                buildSingleFlightTestClient(manager, passHolder) { request ->
                    handleSingleFlightRequest(request, lock, retryCookies) {
                        initial401Count += 1
                        if (initial401Count == 3) {
                            initialResponsesReleased.complete(Unit)
                        }
                    }
                }

            val responses =
                List(3) {
                    async {
                        client.get("https://example.test/resource/$it").bodyAsText()
                    }
                }.awaitAll()

            assertEquals(listOf("ok", "ok", "ok"), responses)
            assertEquals(1, userPass.signatureCount)
            assertEquals(3, retryCookies.size)
            assertEquals("user_session=success", retryCookies.last())
        }

    private fun buildSingleFlightTestClient(
        manager: UserSessionModel,
        passHolder: SimplePassHolder,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine) {
        install(SingleFlightCustomAuthPlugin) {
            configClientAuth(manager, passHolder) { u, l ->
                addRequestHeadersFromInfo(u, l)
            }
        }
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
        engine {
            addHandler(handler)
        }
    }

    private suspend fun MockRequestHandleScope.handleSingleFlightRequest(
        request: HttpRequestData,
        lock: Mutex,
        retryCookies: MutableList<String?>,
        onInitial401: () -> Unit,
    ): HttpResponseData {
        val auth = request.headers[HttpHeaders.Authorization]
        return if (auth == null) {
            lock.withLock { onInitial401() }
            respond(
                content = "unauthorized",
                status = HttpStatusCode.Unauthorized,
                headers =
                headersOf(
                    HttpHeaders.WWWAuthenticate to listOf("Custom auth-data"),
                    HttpHeaders.SetCookie to listOf("user_session=pending; Path=/"),
                ),
            )
        } else {
            lock.withLock {
                retryCookies += request.headers[HttpHeaders.Cookie]
            }
            respond(
                content = "ok",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.SetCookie to listOf("user_session=success; Path=/")),
            )
        }
    }
}

private class CountingUserPass(private val initialResponsesReleased: CompletableDeferred<Unit>) : UserPass {
    var signatureCount = 0
        private set

    override suspend fun signature(data: String): CustomResult<String, CredentialError> {
        signatureCount += 1
        initialResponsesReleased.await()
        return CustomResult.success("signature-$signatureCount")
    }

    override suspend fun verify(signature: String, data: String): CustomResult<Boolean, CredentialError> =
        CustomResult.success(
            true,
        )

    override suspend fun decrypt(
        encrypted: ByteArray,
        encryptedAesKey: ByteArray,
    ): CustomResult<String, CredentialError> =
        CustomResult.success(
            "",
        )

    override suspend fun address(): CustomResult<String, CredentialError> = CustomResult.success("address")

    override suspend fun decryptChildAccount(
        encryptedPrivateKey: String,
        encryptedAesKey: String,
        childAlgoType: AlgoType,
        encryptedEncryptionPrivateKey: String?,
    ): CustomResult<Pair<String, String?>, CredentialError> = CustomResult.success("" to null)

    override suspend fun encryptChildAccount(
        childAlgoType: AlgoType,
    ): CustomResult<AddChildAccountRequest, CredentialError> =
        CustomResult.Failure(
            CredentialError.from(UnsupportedOperationException()),
        ).mapError(CredentialError::fromError)
}
