/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.cli

import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.A_FILE_DEFAULT_BUCKET
import io.github.aakira.napier.Napier
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalCli::class)
class CleanCommand : Subcommand("clean", "clean all data") {
    override fun execute() {
        runBlocking {
            val connected = requireBackend()
            connected.database.clean()
            Napier.i {
                "database tables delete done"
            }
            connected.objectStorageService.clean(A_FILE_DEFAULT_BUCKET).getOrThrow()
            cleanSearchIndexes(connected)
        }
        Napier.i {
            "clean done"
        }
    }
}

internal suspend fun cleanSearchIndexes(backend: Backend) {
    backend.topicSearchService.clean().getOrThrow()
    backend.userSearchService.clean().getOrThrow()
    backend.roomSearchService.clean().getOrThrow()
    backend.communitySearchService.clean().getOrThrow()
    backend.memberSearchService.clean().getOrThrow()
    backend.fileSearchService.clean().getOrThrow()
}

@OptIn(ExperimentalCli::class)
class InitTableCommand : Subcommand("init", "init table data") {
    override fun execute() {
        runBlocking {
            requireBackend().database.init()
        }
        Napier.i {
            "init done"
        }
    }
}

@OptIn(ExperimentalCli::class)
class PrintCommand : Subcommand("print", "print") {
    override fun execute() = Unit
}
