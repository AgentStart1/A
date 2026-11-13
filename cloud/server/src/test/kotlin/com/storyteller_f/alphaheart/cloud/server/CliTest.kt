/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch.AidListFetch
import com.storyteller_f.alphaheart.backend.core.types.User
import com.storyteller_f.alphaheart.client.core.AuthKey
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.getAllPrivateRooms
import com.storyteller_f.alphaheart.client.core.getAllPublicRooms
import com.storyteller_f.alphaheart.client.core.getAllTitles
import com.storyteller_f.alphaheart.client.core.getAllTopics
import com.storyteller_f.alphaheart.client.core.getUserInfo
import com.storyteller_f.alphaheart.client.core.overview
import com.storyteller_f.alphaheart.cloud.cli.AddPreset
import com.storyteller_f.alphaheart.cloud.worker.WorkerBackend
import com.storyteller_f.alphaheart.cloud.worker.doAcgTask
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.PassType
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.WorkerTask
import com.storyteller_f.shared.obj.PresetCommunity
import com.storyteller_f.shared.obj.PresetFile
import com.storyteller_f.shared.obj.PresetPanelAccount
import com.storyteller_f.shared.obj.PresetRoom
import com.storyteller_f.shared.obj.PresetTitle
import com.storyteller_f.shared.obj.PresetTopic
import com.storyteller_f.shared.obj.PresetUser
import com.storyteller_f.shared.obj.PresetValue
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.now
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private data class P256KeyMaterial(
    val privatePem: String,
    val derPrivate: String,
    val derPublic: String,
    val address: String,
)

private suspend fun generateP256KeyMaterial(): P256KeyMaterial {
    val algo = getAlgo(AlgoType.P256)
    val (privatePem, _) = algo.generatePemKeyPair().getOrThrow()
    val derPrivate = algo.getDerPrivateKey(privatePem).getOrThrow()
    val derPublic = algo.getDerPublicKeyFromPrivateKey(privatePem).getOrThrow()
    val address = algo.calcAddress(derPublic).getOrThrow()
    return P256KeyMaterial(privatePem, derPrivate, derPublic, address)
}

private fun preparePresetDir(testName: String): File {
    val dir = File("build/test/cli/$testName").canonicalFile
    check(dir.exists() || dir.mkdirs()) {
        "failed to create preset dir: ${dir.canonicalPath}"
    }
    return dir
}

private suspend fun ensureSystemUser(testMate: TestMate) {
    testMate.withCliBackend { backend ->
        backend.database.init()
        val existing = backend.database.user.getRawUsers(AidListFetch(listOf("System"))).getOrThrow()
        if (existing.isNotEmpty()) {
            return@withCliBackend
        }
        val algo = getAlgo(AlgoType.P256)
        val (_, sysPubPem) = algo.generatePemKeyPair().getOrThrow()
        val sysPubDer = algo.getDerPublicKeyFromPem(sysPubPem).getOrThrow()
        val sysAddress = algo.calcAddress(sysPubDer).getOrThrow()
        val systemUser =
            User(
                aid = "System",
                encryptionPublicKey = null,
                publicKey = sysPubDer,
                address = sysAddress,
                icon = null,
                nickname = "System",
                id = SnowflakeFactory.nextId(),
                createdTime = now(),
                acgAmount = 0L,
                passType = PassType.RAW,
                algoType = AlgoType.P256,
                notificationId = SnowflakeFactory.nextId(),
            )
        backend.database.user.createUser(systemUser).getOrThrow()
    }
}

class CliTest {
    @Test
    fun `cli clean and init reset core data`() =
        test {
            attachSession {
                val community = createCommunityForTest("cli-clean-community", "cli-clean-community")
                createTopic(ObjectType.COMMUNITY, community.id, "seed-topic").getOrThrow()
            }

            withCliBackend { backend ->
                backend.database.clean()
                backend.database.init()
            }

            attachPanelSession {
                val panelOverview = overview().getOrThrow()
                assertEquals(0, panelOverview.userCount)
                assertEquals(0, panelOverview.communityCount)
                assertEquals(0, panelOverview.topicCount)
                assertEquals(0, panelOverview.fileCount)
            }

            attachSession()
            attachPanelSession()
        }

    @Test
    fun `cli apply user preset and sign in`() =
        test {
            ensureSystemUser(this)

            val presetDir = preparePresetDir("user")
            val keysDir = File(presetDir, "keys").apply { mkdirs() }
            val userAid = "cli_preset_user"

            val userKeys = generateP256KeyMaterial()

            val userKeyPath = File(keysDir, "user.pem").apply { writeText(userKeys.privatePem) }

            withCliBackend { backend ->
                backend.database.init()

                backend.applyPreset(
                    PresetValue(
                        type = "user",
                        userData =
                        listOf(
                            PresetUser(
                                name = "Preset User",
                                aid = userAid,
                                privateKey = userKeyPath.relativeTo(presetDir).path,
                            ),
                        ),
                    ),
                    presetDir,
                )
            }

            val userAuthKey =
                AuthKey.P256(
                    userKeys.privatePem,
                    userKeys.derPrivate,
                    userKeys.derPublic,
                )

            val userTuple =
                getAppSignInSession(
                    authKey = userAuthKey,
                    onReceive = { _, _, _ -> },
                ) {
                    getUserInfo(it.uid).getOrThrow()
                }
            assertEquals(userAid, userTuple.custom.aid)
        }

    @Suppress("LongMethod")
    @Test
    fun `admin sees CLI community and room presets`() =
        test {
            ensureSystemUser(this)

            val presetDir = preparePresetDir("community-room")
            val keysDir = File(presetDir, "keys").apply { mkdirs() }
            val userAid = "cli_preset_user"
            val communityAid = "cli_preset_community"
            val publicRoomAid = "cli_public_room"
            val privateRoomAid = "cli_private_room"

            val userKeys = generateP256KeyMaterial()
            val userKeyPath = File(keysDir, "user.pem").apply { writeText(userKeys.privatePem) }

            withCliBackend { backend ->
                backend.database.init()

                backend.applyPreset(
                    PresetValue(
                        type = "user",
                        userData =
                        listOf(
                            PresetUser(
                                name = "Preset User",
                                aid = userAid,
                                privateKey = userKeyPath.relativeTo(presetDir).path,
                            ),
                        ),
                    ),
                    presetDir,
                )

                backend.applyPreset(
                    PresetValue(
                        type = "community",
                        communityData =
                        listOf(
                            PresetCommunity(
                                name = "Preset Community",
                                id = communityAid,
                                admin = userAid,
                                users = emptyList(),
                            ),
                        ),
                    ),
                    presetDir,
                )

                backend.applyPreset(
                    PresetValue(
                        type = "room",
                        roomData =
                        listOf(
                            PresetRoom(
                                name = "Preset Public Room",
                                community = communityAid,
                                users = listOf(userAid),
                                id = publicRoomAid,
                                admin = userAid,
                            ),
                            PresetRoom(
                                name = "Preset Private Room",
                                users = listOf(userAid),
                                id = privateRoomAid,
                                admin = userAid,
                            ),
                        ),
                    ),
                    presetDir,
                )
            }

            attachPanelSession {
                val panelOverview = overview().getOrThrow()
                assertEquals(1, panelOverview.communityCount)

                val publicRooms = getAllPublicRooms(PaginationQuery()).getOrThrow().data
                val privateRooms = getAllPrivateRooms(PaginationQuery()).getOrThrow().data

                assertTrue(publicRooms.any { room -> room.aid == publicRoomAid })
                assertTrue(privateRooms.any { room -> room.aid == privateRoomAid })
            }
        }

    @Suppress("LongMethod")
    @Test
    fun `CLI topic preset persists content and author`() =
        test {
            withCliContentPreset("topic") { backend, presetDir ->
                backend.applyPreset(
                    PresetValue(
                        type = "topic",
                        topicData =
                        listOf(
                            PresetTopic(content = "preset topic content", author = "cli_preset_user"),
                        ),
                    ),
                    presetDir,
                )
            }
            attachPanelSession {
                val topic = getAllTopics(PaginationQuery()).getOrThrow().data.single()
                assertEquals("preset topic content", assertIs<TopicContent.Plain>(topic.content).plain)
                assertEquals("cli_preset_user", topic.authorInfo?.aid)
                assertEquals(1, overview().getOrThrow().topicCount)
            }
        }

    @Test
    fun `CLI title preset keeps receiver scope description`() =
        test {
            withCliContentPreset("title") { backend, presetDir ->
                backend.applyPreset(
                    PresetValue(
                        type = "community",
                        communityData =
                        listOf(
                            PresetCommunity(
                                name = "Preset Community",
                                id = "cli_preset_community",
                                admin = "cli_preset_user",
                                users = emptyList(),
                            ),
                        ),
                    ),
                    presetDir,
                )
                backend.applyPreset(
                    PresetValue(
                        type = "title",
                        titleData =
                        listOf(
                            PresetTitle(
                                creator = "cli_preset_user",
                                uid = "cli_preset_user",
                                name = "Preset Title",
                                scope = "cli_preset_community",
                                scopeType = ObjectType.COMMUNITY,
                                type = TitleType.REGULAR,
                                description = "preset title description",
                            ),
                        ),
                    ),
                    presetDir,
                )
            }
            attachPanelSession {
                val title = getAllTitles(PaginationQuery()).getOrThrow().data.single()
                assertEquals("Preset Title", title.name)
                assertEquals(TitleType.REGULAR, title.type)
                assertEquals("cli_preset_user", title.creatorInfo.aid)
                assertEquals("cli_preset_user", title.receiverInfo.aid)
                assertEquals(ObjectType.COMMUNITY, title.scopeType)
                assertEquals("cli_preset_community", title.communityInfo?.aid)
                assertEquals(
                    "preset title description",
                    assertIs<TopicContent.Plain>(title.descriptionTopicInfo.content).plain,
                )
                assertEquals(1, overview().getOrThrow().topicCount)
            }
        }

    @Test
    fun `cli apply file preset reflected in overview`() =
        test {
            ensureSystemUser(this)

            val presetDir = preparePresetDir("file")
            val keysDir = File(presetDir, "keys").apply { mkdirs() }
            val userAid = "cli_preset_user"

            val userKeys = generateP256KeyMaterial()
            val userKeyPath = File(keysDir, "user.pem").apply { writeText(userKeys.privatePem) }
            val fileForPreset = File(presetDir, "sample.txt").apply { writeText("cli preset file") }

            withCliBackend { backend ->
                backend.database.init()

                backend.applyPreset(
                    PresetValue(
                        type = "user",
                        userData =
                        listOf(
                            PresetUser(
                                name = "Preset User",
                                aid = userAid,
                                privateKey = userKeyPath.relativeTo(presetDir).path,
                            ),
                        ),
                    ),
                    presetDir,
                )

                backend.applyPreset(
                    PresetValue(
                        type = "file",
                        fileData =
                        listOf(
                            PresetFile(
                                owner = userAid,
                                paths = listOf(fileForPreset.relativeTo(presetDir).path),
                            ),
                        ),
                    ),
                    presetDir,
                )
            }

            attachPanelSession {
                val panelOverview = overview().getOrThrow()
                assertTrue(panelOverview.fileCount >= 1)
            }
        }

    @Test
    fun `panel preset is queryable by address`() =
        test {
            ensureSystemUser(this)

            val presetDir = preparePresetDir("panel-account")
            val keysDir = File(presetDir, "keys").apply { mkdirs() }
            val panelKeys = generateP256KeyMaterial()
            val panelKeyPath = File(keysDir, "panel.pem").apply { writeText(panelKeys.privatePem) }

            withCliBackend { backend ->
                backend.database.init()

                backend.applyPreset(
                    PresetValue(
                        type = "panelAccount",
                        panelAccountData =
                        listOf(
                            PresetPanelAccount(
                                name = "Preset Panel",
                                privateKey = panelKeyPath.relativeTo(presetDir).path,
                            ),
                        ),
                    ),
                    presetDir,
                )

                val panelAuth = backend.database.panelAccount.getUserAuthDataByAddress(panelKeys.address).getOrThrow()
                assertNotNull(panelAuth)
            }
        }

    @Test
    fun `cli preset data works with worker acg task`() =
        test {
            ensureSystemUser(this)

            val presetDir = preparePresetDir("worker-combination")
            val keysDir = File(presetDir, "keys").apply { mkdirs() }
            val userAid = "cli_worker_user"

            val userKeys = generateP256KeyMaterial()
            val userKeyPath = File(keysDir, "user.pem").apply { writeText(userKeys.privatePem) }

            val presetUser =
                PresetValue(
                    type = "user",
                    userData =
                    listOf(
                        PresetUser(
                            name = "Worker User",
                            aid = userAid,
                            privateKey = userKeyPath.relativeTo(presetDir).path,
                        ),
                    ),
                )

            val presetTopic =
                PresetValue(
                    type = "topic",
                    topicData =
                    listOf(
                        PresetTopic(content = "topic-1", author = userAid),
                        PresetTopic(content = "topic-2", author = userAid),
                        PresetTopic(content = "topic-3", author = userAid),
                    ),
                )

            withCliBackend { backend ->
                backend.database.init()

                backend.applyPreset(presetUser, presetDir)
                backend.applyPreset(presetTopic, presetDir)

                val uid =
                    backend.database.user
                        .getRawUsers(AidListFetch(listOf(userAid)))
                        .getOrThrow()
                        .first()
                        .user.id
                backend.doAcgTask()
                val userAcg =
                    backend.database.user.getUserAcgByIds(
                        com.storyteller_f.alphaheart.backend.core.ObjectListFetch.IdListFetch(listOf(uid)),
                    ).getOrThrow()
                val totalAcg = userAcg.find { pair -> pair.first == uid }?.second ?: 0L
                assertTrue(totalAcg >= 3L)
            }
        }

    @Test
    fun `cli preset initializes worker task configurations`() {
        test {
            val presetDir = preparePresetDir("task-config")
            val configs =
                listOf(
                    WorkerTask(
                        type = TaskRecordType.TOPIC_ACG,
                        isEnabled = true,
                        fetchSize = 5,
                        waitDurationMillis = 1_000,
                    ),
                    WorkerTask(
                        type = TaskRecordType.TOPIC_MODERATION,
                        isEnabled = false,
                        fetchSize = 2,
                        waitDurationMillis = 20_000,
                    ),
                )

            withCliBackend { backend ->
                backend.database.init()
                backend.applyPreset(
                    PresetValue(type = "taskConfig", workerTaskData = configs),
                    presetDir,
                )

                assertEquals(configs, backend.database.workerTask.getWorkerTasks().getOrThrow())
            }
        }
    }
}

private suspend fun TestMate.withCliContentPreset(testName: String, block: suspend (WorkerBackend, File) -> Unit) {
    ensureSystemUser(this)
    val presetDir = preparePresetDir(testName)
    val keysDir = File(presetDir, "keys").apply { mkdirs() }
    val userAid = "cli_preset_user"

    val userKeys = generateP256KeyMaterial()
    val userKeyPath = File(keysDir, "user.pem").apply { writeText(userKeys.privatePem) }

    withCliBackend { backend ->
        backend.database.init()

        backend.applyPreset(
            PresetValue(
                type = "user",
                userData =
                listOf(
                    PresetUser(
                        name = "Preset User",
                        aid = userAid,
                        privateKey = userKeyPath.relativeTo(presetDir).path,
                    ),
                ),
            ),
            presetDir,
        )

        block(backend, presetDir)
    }
}

private suspend fun Backend.applyPreset(presetValue: PresetValue, parentDir: File) {
    AddPreset().run {
        applyPresetByValue(presetValue, parentDir)
    }
}
