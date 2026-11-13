/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.redis

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.service.CacheService
import com.storyteller_f.alphaheart.backend.core.service.CacheServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.WrapCacheService
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.github.crackthecodeabhi.kreds.connection.newClient
import kotlin.reflect.KClass

class RedisCacheService(private val endpoint: String) : CacheService<String, String> {
    override suspend fun get(key: String, block: suspend () -> String): String =
        useRedis(endpoint) {
            val cache = get(key)
            if (cache == null) {
                val new = block()
                set(key, new)
                new
            } else {
                cache
            }
        }
}

private suspend fun <R> useRedis(endpoint: String, block: suspend KredsClient.() -> R): R =
    newClient(
        Endpoint.from(endpoint),
    ).use {
        it.block()
    }

class RedisCacheServiceFactory : CacheServiceFactory {
    override fun match(env: MergedEnv): Boolean = !env["CACHE_SERVICE"].isNullOrBlank()

    override fun <K, T : Any> build(env: MergedEnv, vClass: KClass<T>): CacheService<K, T> {
        val endpoint = env["REDIS_ENDPOINT"] ?: error("REDIS_ENDPOINT not set")
        val cacheService = RedisCacheService(endpoint)
        return WrapCacheService(cacheService, vClass)
    }
}
