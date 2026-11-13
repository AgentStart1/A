/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.service

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

interface CacheService<K, T> {
    suspend fun get(key: K, block: suspend () -> T): T
}

interface CacheServiceFactory {
    fun match(env: MergedEnv): Boolean
    fun <K, T : Any> build(env: MergedEnv, vClass: KClass<T>): CacheService<K, T>
}

class WrapCacheService<K, V : Any>(val cacheService: CacheService<String, String>, val vClass: KClass<V>) :
    CacheService<K, V> {
    @OptIn(InternalSerializationApi::class)
    override suspend fun get(key: K, block: suspend () -> V): V {
        val k = key?.toString().orEmpty()
        val v =
            cacheService.get(k) {
                Json.encodeToString(vClass.serializer(), block())
            }
        return Json.decodeFromString(vClass.serializer(), v)
    }
}
