/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.service

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.error.LockError
import com.storyteller_f.result.CustomResult

interface LockService {
    /** Runs [block] while holding the lock identified by [key]. */
    suspend fun <T> withLock(key: String, block: suspend () -> CustomResult<T, LockError>): CustomResult<T, LockError>
}

interface LockServiceFactory {
    fun match(env: MergedEnv): Boolean
    fun build(env: MergedEnv): LockService
}
