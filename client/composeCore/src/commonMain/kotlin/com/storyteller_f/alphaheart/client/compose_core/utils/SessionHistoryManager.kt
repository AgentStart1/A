/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import com.storyteller_f.alphaheart.client.core.AuthKey
import com.storyteller_f.alphaheart.client.core.ClientSessionState
import com.storyteller_f.alphaheart.client.core.RawUserPass
import com.storyteller_f.alphaheart.client.core.RawUserPassInfo
import com.storyteller_f.alphaheart.client.core.UserPass
import com.storyteller_f.shared.model.AlgoType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface SessionHistoryManager {
    suspend fun getSavedSession(): SavedSession

    suspend fun addSession(userPassInfo: RawUserPassInfo): UserPass

    suspend fun buildSession(alias: String): UserPass?

    suspend fun removeSession(alias: String)

    suspend fun exitSession(alias: String)

    suspend fun logSession(alias: String)
}

/** Last selected account and the account whose cookie should be restored on startup. */
@Serializable
data class SessionHistory(val last: String? = null, val current: String? = null)

@Serializable
internal data class ProtectedSessionHistory(
    val accounts: List<ConvertedRawUserPassInfo> = emptyList(),
    val history: SessionHistory? = null,
)

internal class ProtectedSessionHistoryManager(
    private val storageKey: String,
    private val read: suspend (String) -> String? = { readProtectedValue(it) },
    private val write: suspend (String, String) -> Unit = { key, value -> writeProtectedValue(key, value) },
) : SessionHistoryManager {
    override suspend fun getSavedSession(): SavedSession =
        gate.withLock {
            val saved = loadHistory()
            SavedSession(saved.accounts.map { it.address }, saved.history)
        }

    override suspend fun addSession(userPassInfo: RawUserPassInfo): UserPass =
        gate.withLock {
            val saved = loadHistory()
            val address = userPassInfo.address
            saveHistory(
                saved.copy(
                    accounts = saved.accounts.filter { it.address != address } + userPassInfo.toConverted(),
                    history = SessionHistory(address, address),
                ),
            )
            RawUserPass(userPassInfo)
        }

    override suspend fun buildSession(alias: String): UserPass? =
        gate.withLock {
            loadHistory().accounts.firstOrNull { it.address == alias }?.let { RawUserPass(it.toRawUserPassInfo()) }
        }

    override suspend fun removeSession(alias: String) =
        gate.withLock {
            val saved = loadHistory()
            val history =
                saved.history?.let {
                    it.copy(
                        last = it.last.takeUnless { value -> value == alias },
                        current = it.current.takeUnless { value -> value == alias },
                    )
                }
            saveHistory(saved.copy(accounts = saved.accounts.filter { it.address != alias }, history = history))
        }

    override suspend fun exitSession(alias: String) =
        gate.withLock {
            val saved = loadHistory()
            if (saved.history?.current == alias) saveHistory(saved.copy(history = saved.history.copy(current = null)))
        }

    override suspend fun logSession(alias: String) =
        gate.withLock {
            val saved = loadHistory()
            require(saved.accounts.any { it.address == alias }) { "Saved account does not exist" }
            saveHistory(saved.copy(history = SessionHistory(alias, alias)))
        }

    private suspend fun loadHistory(): ProtectedSessionHistory =
        read(storageKey)?.let { Json.decodeFromString<ProtectedSessionHistory>(it) } ?: ProtectedSessionHistory()

    private suspend fun saveHistory(saved: ProtectedSessionHistory) {
        val serialized = Json.encodeToString(saved)
        write(storageKey, serialized)
        check(read(storageKey) == serialized) { "Protected account storage could not be verified" }
    }

    companion object {
        private val gate = Mutex()
    }
}

/** Account keys and history are stored with platform protection; old Settings history is ignored. */
fun buildSessionHistoryFactory(namespace: String = "user"): SessionHistoryManager =
    ProtectedSessionHistoryManager("account-history:$namespace")

internal expect suspend fun readProtectedValue(key: String): String?
internal expect suspend fun writeProtectedValue(key: String, value: String)

data class SavedSession(val alias: List<String>, val history: SessionHistory?)

expect fun readInjectedSessionFromPrivateStorageOrNull(): ConvertedRawUserPassInfo?

suspend fun restoreFromStorage(namespace: String = "user"): ClientSessionState? {
    val injected = readInjectedSessionFromPrivateStorageOrNull()
    if (injected != null) {
        Napier.d("Found injected session, restoring...")
        Napier.d("APP_DESKTOP_SESSION_RESTORE source=injected address=${injected.address.shortForLog()}")
        val userPass = RawUserPass(injected.toRawUserPassInfo())
        return ClientSessionState.Success(userPass, injected.cookies)
    }

    return try {
        restoreProtectedHistory(namespace)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        Napier.w("Protected account history is unavailable; explicit sign-in is required")
        null
    }
}

private suspend fun restoreProtectedHistory(namespace: String): ClientSessionState? {
    val sessionFactory = buildSessionHistoryFactory(namespace)
    val (alias, history) = sessionFactory.getSavedSession()
    val current = history?.current
    Napier.d(
        "APP_DESKTOP_SESSION_RESTORE storage current=${current.shortForLog()} aliases=${alias.map {
            it.shortForLog()
        }}",
    )
    if (current != null && alias.contains(current)) {
        val session = sessionFactory.buildSession(current)
        if (session != null) {
            Napier.d("APP_DESKTOP_SESSION_RESTORE source=storage address=${current.shortForLog()}")
            return ClientSessionState.Success(session)
        }
    }

    Napier.d("No valid session found in storage, trying to read injected session")
    return null
}

private fun String?.shortForLog(): String = this?.take(8) ?: "null"

@Serializable
data class ConvertedRawUserPassInfo(
    val algo: AlgoType,
    val address: String,
    val pemPrivateKey: String,
    val derPrivateKey: String,
    val derPublicKey: String,
    val pemEncryptionPrivateKey: String? = null,
    val derEncryptionPrivateKey: String? = null,
    val derEncryptionPublicKey: String? = null,
    val cookies: List<io.ktor.http.Cookie> = emptyList(),
) {
    fun toRawUserPassInfo(): RawUserPassInfo {
        val key =
            when (algo) {
                AlgoType.P256 ->
                    AuthKey.P256(
                        pemPrivateKey = pemPrivateKey,
                        derPrivateKey = derPrivateKey,
                        derPublicKey = derPublicKey,
                    )

                AlgoType.DILITHIUM ->
                    AuthKey.Dilithium(
                        pemPrivateKey = pemPrivateKey,
                        derPrivateKey = derPrivateKey,
                        derPublicKey = derPublicKey,
                        pemEncryptionPrivateKey =
                        checkNotNull(pemEncryptionPrivateKey) {
                            "Dilithium PEM encryption private key is missing"
                        },
                        derEncryptionPrivateKey =
                        checkNotNull(derEncryptionPrivateKey) {
                            "Dilithium DER encryption private key is missing"
                        },
                        derEncryptionPublicKey =
                        checkNotNull(derEncryptionPublicKey) {
                            "Dilithium DER encryption public key is missing"
                        },
                    )
            }
        return RawUserPassInfo(address, key)
    }
}

fun RawUserPassInfo.toConverted(): ConvertedRawUserPassInfo =
    when (val key = authKey) {
        is AuthKey.Dilithium ->
            ConvertedRawUserPassInfo(
                algo = key.algo,
                address = address,
                pemPrivateKey = key.pemPrivateKey,
                derPrivateKey = key.derPrivateKey,
                derPublicKey = key.derPublicKey,
                pemEncryptionPrivateKey = key.pemEncryptionPrivateKey,
                derEncryptionPrivateKey = key.derEncryptionPrivateKey,
                derEncryptionPublicKey = key.derEncryptionPublicKey,
            )

        is AuthKey.P256 ->
            ConvertedRawUserPassInfo(
                algo = key.algo,
                address = address,
                pemPrivateKey = key.pemPrivateKey,
                derPrivateKey = key.derPrivateKey,
                derPublicKey = key.derPublicKey,
            )
    }
