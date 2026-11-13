/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.simple

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.error.LockError
import com.storyteller_f.alphaheart.backend.core.service.LockService
import com.storyteller_f.alphaheart.backend.core.service.LockServiceFactory
import com.storyteller_f.result.CustomResult
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class NativeLockService : LockService {
    val list = mutableSetOf<String>()
    val lock = Mutex()
    override suspend fun <T> withLock(
        key: String,
        block: suspend () -> CustomResult<T, LockError>,
    ): CustomResult<T, LockError> {
        lock.withLock {
            if (list.contains(key)) {
                return CustomResult.Failure(LockError.from(IllegalStateException("key $key is locked")))
            } else {
                list.add(key)
            }
        }
        try {
            return block()
        } finally {
            // 保证清理锁操作不会被协程取消
            withContext(NonCancellable) {
                lock.withLock {
                    list.remove(key)
                }
            }
        }
    }
}

class NativeLockServiceFactory : LockServiceFactory {
    override fun match(env: MergedEnv): Boolean = true

    override fun build(env: MergedEnv): LockService = NativeLockService()
}
