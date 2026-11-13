/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.service

import com.storyteller_f.alphaheart.backend.core.MergedEnv

interface NameService {
    fun parse(id: Long): String
}

interface NameServiceFactory {
    fun match(env: MergedEnv): Boolean
    fun build(env: MergedEnv): NameService
}
