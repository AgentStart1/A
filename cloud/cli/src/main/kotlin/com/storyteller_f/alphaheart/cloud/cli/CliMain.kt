/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.cli

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CombinedDatabase
import com.storyteller_f.alphaheart.backend.core.CustomConfig
import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.buildCommunitySearchService
import com.storyteller_f.alphaheart.backend.core.buildFileSearchService
import com.storyteller_f.alphaheart.backend.core.buildMemberSearchService
import com.storyteller_f.alphaheart.backend.core.buildNameService
import com.storyteller_f.alphaheart.backend.core.buildRoomSearchService
import com.storyteller_f.alphaheart.backend.core.buildTopicSearchService
import com.storyteller_f.alphaheart.backend.core.buildUserSearchService
import com.storyteller_f.alphaheart.backend.core.databaseConnection
import com.storyteller_f.alphaheart.backend.core.loadAvif
import com.storyteller_f.alphaheart.backend.core.mediaService
import com.storyteller_f.alphaheart.backend.core.readEnv
import com.storyteller_f.alphaheart.backend.core.service.CommunitySearchService
import com.storyteller_f.alphaheart.backend.core.service.FileSearchService
import com.storyteller_f.alphaheart.backend.core.service.MemberSearchService
import com.storyteller_f.alphaheart.backend.core.service.NameService
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageService
import com.storyteller_f.alphaheart.backend.core.service.RoomSearchService
import com.storyteller_f.alphaheart.backend.core.service.TopicSearchService
import com.storyteller_f.alphaheart.backend.core.service.UserSearchService
import com.storyteller_f.alphaheart.backend.exposed.buildExposedDatabase
import com.storyteller_f.shared.setupKmpLogger
import io.github.aakira.napier.Napier
import kotlinx.cli.ArgParser
import kotlinx.cli.ExperimentalCli

fun requireBackend(): Backend = buildBackendFromEnv(readEnv())

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>) {
    setupKmpLogger()
    Napier.i(tag = "cli") {
        "args: ${args.contentToString()}"
    }
    loadAvif()
    SnowflakeFactory.setMachine(0)
    val argParser = ArgParser("ACli")
    argParser.subcommands(AddPreset(), CleanCommand(), PrintCommand(), InitTableCommand(), GeneratePresetKeysCommand())
    argParser.parse(args)
}

class CliBackend(
    override val customConfig: CustomConfig,
    override val topicSearchService: TopicSearchService,
    override val roomSearchService: RoomSearchService,
    override val communitySearchService: CommunitySearchService,
    override val userSearchService: UserSearchService,
    override val memberSearchService: MemberSearchService,
    override val fileSearchService: FileSearchService,
    override val objectStorageService: ObjectStorageService,
    override val nameService: NameService,
    override val database: CombinedDatabase,
) : Backend

fun buildBackendFromEnv(env: MergedEnv): Backend {
    Napier.i("load env: ${env.getAll("COMPOSE_PROJECT_NAME")}")

    val databaseConnection = databaseConnection(env)

    val buildType = env["BUILD_TYPE"] ?: "prod"
    val flavor = env["FLAVOR"] ?: error("FLAVOR is empty")

    val customConfig = CustomConfig(buildType, flavor, null)

    val topicSearchService = buildTopicSearchService(env)
    val userSearchService = buildUserSearchService(env)
    val roomSearchService = buildRoomSearchService(env)
    val communitySearchService = buildCommunitySearchService(env)
    val memberSearchService = buildMemberSearchService(env)
    val fileSearchService = buildFileSearchService(env)
    val mediaService = mediaService(env)

    val exposedDatabase = buildExposedDatabase(databaseConnection)
    return CliBackend(
        customConfig,
        topicSearchService,
        roomSearchService,
        communitySearchService,
        userSearchService,
        memberSearchService,
        fileSearchService,
        mediaService,
        buildNameService(env),
        exposedDatabase,
    )
}
