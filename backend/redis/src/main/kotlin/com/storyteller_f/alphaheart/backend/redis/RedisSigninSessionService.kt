/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.redis

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.service.SigninSessionService
import com.storyteller_f.alphaheart.backend.core.service.SigninSessionServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.SigninSessionUnavailableException
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import java.util.Locale

/** Redis owns expiry and activity updates; reading a session never extends its lifetime. */
internal class RedisSigninSessionService(endpoint: String, private val namespace: String) : SigninSessionService {
    private val client = newClient(Endpoint.from(endpoint))

    override suspend fun read(id: String): String =
        access { client.eval(READ_SESSION, arrayOf(key(id)), emptyArray()) } as? String
            ?: throw NoSuchElementException("Session expired or revoked")

    override suspend fun write(id: String, value: String) {
        val payload = Json.parseToJsonElement(value).jsonObject
        val session = payload["session"]?.jsonObject ?: payload
        val success = session["type"]?.jsonPrimitive?.content == "success"
        val account = if (success) "signin:$namespace:account:${session.getValue("id").jsonPrimitive.content}" else ""
        access { client.eval(WRITE_SESSION, arrayOf(key(id)), arrayOf(value, account)) }
    }

    override suspend fun invalidate(id: String) {
        access { client.eval(REMOVE_SESSION, arrayOf(key(id)), emptyArray()) }
    }

    override suspend fun touchSession(id: String): Boolean =
        (access { client.eval(TOUCH_SESSION, arrayOf(key(id)), emptyArray()) } as? Number)?.toInt() == 1

    override suspend fun consumeChallenge(id: String, data: String, expectedType: String): Boolean =
        (
            access {
                client.eval(
                    CONSUME_CHALLENGE,
                    arrayOf(key(id)),
                    arrayOf(data, expectedType),
                )
            } as? Number
            )?.toInt() == 1

    override suspend fun invalidateAccount(id: Long) {
        access { client.eval(REMOVE_ACCOUNT, arrayOf("signin:$namespace:account:$id"), emptyArray()) }
    }

    private suspend fun <T> access(block: suspend () -> T): T =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            throw SigninSessionUnavailableException()
        }

    override fun close() = client.close()

    private fun key(id: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(id.encodeToByteArray())
        return "signin:$namespace:${hash.joinToString("") { "%02x".format(Locale.ROOT, it) }}"
    }

    companion object {
        // Redis TIME avoids differences between API and WS clocks. Lua makes touch/revoke atomic.
        private const val READ_SESSION = """
            local value = redis.call('GET', KEYS[1])
            if not value then return nil end
            local record = cjson.decode(value)
            local now = tonumber(redis.call('TIME')[1])
            if now >= record.absolute or now >= record.active + record.idle then
                redis.call('DEL', KEYS[1])
                return nil
            end
            return record.payload
        """
        private const val CONSUME_CHALLENGE = """
            local value = redis.call('GET', KEYS[1])
            if not value then return 0 end
            local record = cjson.decode(value)
            local now = tonumber(redis.call('TIME')[1])
            if now >= record.absolute or now >= record.active + record.idle then return 0 end
            local payload = cjson.decode(record.payload)
            local session = payload.session or payload
            if session.type ~= ARGV[2] or session.data ~= ARGV[1] then return 0 end
            redis.call('DEL', KEYS[1])
            return 1
        """
        private const val WRITE_SESSION = """
            local now = tonumber(redis.call('TIME')[1])
            local success = ARGV[2] ~= ''
            local idle = success and 2592000 or 300
            local lifetime = success and 7776000 or 300
            redis.call('SET', KEYS[1], cjson.encode({payload=ARGV[1], active=now,
                absolute=now+lifetime, idle=idle, account=ARGV[2]}), 'EX', idle)
            if success then
                redis.call('SADD', ARGV[2], KEYS[1])
                redis.call('EXPIRE', ARGV[2], lifetime)
            end
            return 1
        """
        private const val REMOVE_SESSION = """
            local value = redis.call('GET', KEYS[1])
            if value then
                local record = cjson.decode(value)
                if record.account ~= '' then redis.call('SREM', record.account, KEYS[1]) end
            end
            return redis.call('DEL', KEYS[1])
        """
        private const val REMOVE_ACCOUNT = """
            local sessions = redis.call('SMEMBERS', KEYS[1])
            for _, key in ipairs(sessions) do redis.call('DEL', key) end
            redis.call('DEL', KEYS[1])
            return #sessions
        """
        private const val TOUCH_SESSION = """
            local value = redis.call('GET', KEYS[1])
            if not value then return 0 end
            local record = cjson.decode(value)
            local now = tonumber(redis.call('TIME')[1])
            if now >= record.absolute or now >= record.active + record.idle then
                redis.call('DEL', KEYS[1])
                return 0
            end
            if now - record.active < 300 then return 1 end
            record.active = now
            redis.call('SET', KEYS[1], cjson.encode(record), 'EX',
                math.min(record.idle, record.absolute-now))
            return 1
        """
    }
}

class RedisSigninSessionServiceFactory : SigninSessionServiceFactory {
    override fun match(env: MergedEnv): Boolean {
        val serviceType = env["SIGNIN_SESSION_SERVICE"] ?: "redis"
        return serviceType == "redis"
    }

    override fun build(env: MergedEnv, namespace: String): SigninSessionService {
        val endpoint =
            requireNotNull(env["SIGNIN_REDIS_ENDPOINT"] ?: env["REDIS_ENDPOINT"]) {
                "SIGNIN_REDIS_ENDPOINT is required for sign-in sessions"
            }
        return RedisSigninSessionService(endpoint, namespace)
    }
}
