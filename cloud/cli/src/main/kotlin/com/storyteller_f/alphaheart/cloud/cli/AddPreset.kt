/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.cli

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.InsertCommunityTuple
import com.storyteller_f.alphaheart.backend.core.InsertRoomTuple
import com.storyteller_f.alphaheart.backend.core.InsertTopicTuple
import com.storyteller_f.alphaheart.backend.core.MemberAuthData
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch.AidListFetch
import com.storyteller_f.alphaheart.backend.core.service.CommunityDocument
import com.storyteller_f.alphaheart.backend.core.service.MemberDocument
import com.storyteller_f.alphaheart.backend.core.service.RoomDocument
import com.storyteller_f.alphaheart.backend.core.service.TopicDocument
import com.storyteller_f.alphaheart.backend.core.service.UploadPack
import com.storyteller_f.alphaheart.backend.core.service.UserDocument
import com.storyteller_f.alphaheart.backend.core.types.Community
import com.storyteller_f.alphaheart.backend.core.types.FileRef
import com.storyteller_f.alphaheart.backend.core.types.Member
import com.storyteller_f.alphaheart.backend.core.types.PanelAccount
import com.storyteller_f.alphaheart.backend.core.types.Room
import com.storyteller_f.alphaheart.backend.core.types.Title
import com.storyteller_f.alphaheart.backend.core.types.Topic
import com.storyteller_f.alphaheart.backend.core.types.User
import com.storyteller_f.alphaheart.backend.core.types.UserSubscription
import com.storyteller_f.alphaheart.backend.core.types.buildMemberForNotificationRoom
import com.storyteller_f.alphaheart.backend.core.types.buildUserNotificationRoom
import com.storyteller_f.alphaheart.backend.core.types.toUserInfo
import com.storyteller_f.alphaheart.cloud.core.service.addUserLog
import com.storyteller_f.alphaheart.cloud.core.service.getFileInfoList
import com.storyteller_f.alphaheart.cloud.core.service.tryUploadFiles
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.Type2Algo
import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.encryptDataByAES
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.FontSettings
import com.storyteller_f.shared.model.LlmConfig
import com.storyteller_f.shared.model.MemberPolicy
import com.storyteller_f.shared.model.PassType
import com.storyteller_f.shared.model.TitleWorkStatus
import com.storyteller_f.shared.model.UserLogType
import com.storyteller_f.shared.obj.PresetCommunity
import com.storyteller_f.shared.obj.PresetRoom
import com.storyteller_f.shared.obj.PresetTitle
import com.storyteller_f.shared.obj.PresetTopic
import com.storyteller_f.shared.obj.PresetUser
import com.storyteller_f.shared.obj.PresetValue
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.extractMarkdownMediaLink
import com.storyteller_f.shared.utils.now
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File

data class EncryptedTopicTuple(
    val encryptedContent: ByteArray,
    val aesKey: ByteArray,
    val id: PrimaryKey,
    val presetTopic: PresetTopic,
)

data class UserPresetTuple(
    val presetUser: PresetUser,
    val pic: PrimaryKey?,
    val publicKey: String,
    val address: String,
    val id: PrimaryKey,
    val algoType: AlgoType,
)

@Suppress("LargeClass")
@OptIn(ExperimentalCli::class)
class AddPreset : Subcommand("add", "add entry") {
    private val jsonFilePath by argument(ArgType.String, "json", "json data file path")

    override fun execute() {
        if (!File(jsonFilePath).exists()) {
            Napier.i {
                "$jsonFilePath not exists."
            }
            error("$jsonFilePath not exists")
        }
        loadCryptoLibIfNeed()
        val connected = requireBackend()
        val jsonFile = File(jsonFilePath)

        val presetValue =
            Json {
                ignoreUnknownKeys = true
            }.decodeFromString<PresetValue>(jsonFile.readText())

        val parentDir = jsonFile.parentFile.canonicalFile
        runBlocking {
            connected.applyPresetByValue(presetValue, parentDir)
            Napier.i {
                "add done ${jsonFile.canonicalPath}"
            }
        }
    }

    suspend fun Backend.applyPresetByValue(presetValue: PresetValue, parentDir: File) {
        when (val type = presetValue.type) {
            "community" -> addCommunities(presetValue, parentDir)
            "user" -> addUsers(presetValue, parentDir)
            "topic" -> addTopics(presetValue, parentDir)
            "room" -> addRooms(presetValue, parentDir)
            "file" -> addFiles(presetValue, parentDir)
            "title" -> addTitles(presetValue)
            "panelAccount" -> addPanels(presetValue, parentDir)
            "taskConfig" -> addWorkerTasks(presetValue)
            "llmConfig" -> addLlmConfig(presetValue)
            else -> handleUnknownPresetType(type)
        }
    }

    private fun handleUnknownPresetType(type: String) {
        Napier.e { "unrecognized type $type" }
        error("unrecognized type $type")
    }

    private suspend fun Backend.addWorkerTasks(presetValue: PresetValue) {
        database.workerTask.upsertWorkerTasks(presetValue.workerTaskData.orEmpty()).getOrThrow()
    }

    private suspend fun Backend.addLlmConfig(presetValue: PresetValue) {
        val llmConfig =
            presetValue.llmConfigData
                ?: error("llmConfigData is required for llmConfig preset type")
        database.upsertBackendConfig(
            LlmConfig.CONFIG_KEY,
            commonJson.encodeToString(llmConfig),
        ).getOrThrow()
    }

    private suspend fun Backend.addPanels(presetValue: PresetValue, parentDir: File) {
        val accounts = presetValue.panelAccountData ?: return
        accounts.forEach { account ->
            val id = SnowflakeFactory.nextId()
            val algoType = account.algoType?.let { value -> AlgoType.valueOf(value) } ?: AlgoType.P256
            val (derPublicKey, ad) = getPubKeyAndAddress(parentDir, account.privateKey, algoType)
            database.panelAccount.addPanelAccount(
                PanelAccount(id, account.name, PassType.RAW, algoType, derPublicKey, ad),
            ).getOrThrow()
        }
    }

    private suspend fun Backend.addTitles(presetValue: PresetValue) {
        val titles = presetValue.titleData ?: return
        Napier.i {
            "titles count ${presetValue.titleData?.size ?: "<none>"}"
        }
        val userMap =
            database.user.getRawUsers(
                AidListFetch(
                    titles.flatMap {
                        buildList {
                            addAll(listOf(it.creator, it.uid))
                            if (it.scopeType == ObjectType.USER) {
                                add(it.scope)
                            }
                        }
                    }.distinct(),
                ),
            ).getOrThrow().associate {
                checkNotNull(it.user.aid) { "Imported user aid is missing" } to it.user
            }
        val communityMap =
            database.community.getRawCommunities(
                AidListFetch(
                    titles.filter {
                        it.scopeType == ObjectType.COMMUNITY
                    }.map {
                        it.scope
                    }.distinct(),
                ),
            ).getOrThrow().associate {
                it.community.aid to it.community
            }
        val roomMap =
            database.room.getRawRooms(
                AidListFetch(
                    titles.filter {
                        it.scopeType == ObjectType.ROOM
                    }.map {
                        it.scope
                    }.distinct(),
                ),
                null,
            ).getOrThrow().associate {
                it.room.aid to it.room
            }
        batchAddTitle(titles, userMap, communityMap, roomMap)
    }

    private suspend fun Backend.batchAddTitle(
        titles: List<PresetTitle>,
        userMap: Map<String, User>,
        communityMap: Map<String, Community>,
        roomMap: Map<String, Room>,
    ) {
        titles.forEach { presetTitle ->
            val titleId = SnowflakeFactory.nextId()
            val topicId = SnowflakeFactory.nextId()
            val creatorUid = userMap.getValue(presetTitle.creator).id
            val receiverUid = userMap.getValue(presetTitle.uid).id
            val scopeId =
                when (presetTitle.scopeType) {
                    ObjectType.USER -> {
                        userMap.getValue(presetTitle.scope).id
                    }

                    ObjectType.COMMUNITY -> {
                        communityMap.getValue(presetTitle.scope).id
                    }

                    ObjectType.ROOM -> roomMap.getValue(presetTitle.scope).id

                    ObjectType.TOPIC,
                    ObjectType.TITLE,
                    ObjectType.FILE,
                    ObjectType.PANEL_ACCOUNT,
                    -> error("Unsupported title scope type: ${presetTitle.scopeType}")
                }
            val title =
                Title(
                    titleId,
                    now(),
                    presetTitle.name,
                    creatorUid,
                    receiverUid,
                    presetTitle.type,
                    scopeId,
                    presetTitle.scopeType,
                    TitleWorkStatus.OK, topicId, null,
                )
            val topic =
                Topic(
                    topicId,
                    now(),
                    creatorUid,
                    titleId,
                    ObjectType.TITLE,
                    titleId,
                    ObjectType.TITLE,
                    presetTitle.description.encodeToByteArray(),
                    false,
                    1,
                )
            database.topic.createTitle(title, topic).getOrThrow()
            // 为创建者添加 user log
            addUserLog(creatorUid, UserLogType.CREATE, titleId ob ObjectType.TITLE).getOrThrow()
        }
    }

    private suspend fun Backend.addFiles(presetValue: PresetValue, parentDir: File) {
        val files = presetValue.fileData ?: return
        Napier.i {
            "files count ${presetValue.fileData?.size ?: "<none>"}"
        }
        val userMap =
            database.user.getRawUsers(
                AidListFetch(
                    files.map {
                        it.owner
                    }.distinct(),
                ),
            ).getOrThrow().associate {
                checkNotNull(it.user.aid) { "Imported user aid is missing" } to it.user
            }
        val fileList =
            HttpClient(OkHttp).use { client ->
                files.map {
                    it.paths.mapNotNull { p ->
                        downloadFileIfNeed(p, parentDir, client)
                    } to it.owner
                }
            }
        fileList.forEach { (it, owner) ->
            uploadFile(
                userMap.getValue(owner).id,
                ObjectType.USER,
                parentDir,
                it.map {
                    it.toRelativeString(parentDir)
                },
            )
        }
    }

    private suspend fun downloadFileIfNeed(p: String, parentDir: File?, client: HttpClient): File? =
        downloadPresetFileIfNeed(p, parentDir, client)

    private suspend fun Backend.addRooms(presetValue: PresetValue, parentDir: File) {
        val presetRooms = presetValue.roomData ?: return
        Napier.i {
            "rooms count ${presetValue.roomData?.size ?: "<none>"}"
        }
        val userMap = getRoomUserMap(presetRooms)
        val communityMap = getRoomCommunityMap(presetRooms)
        val fileRefs = mutableListOf<Triple<FileInfo, String, PrimaryKey>>()
        val data =
            presetRooms.map {
                val id = SnowflakeFactory.nextId()
                val icon = it.icon
                val s =
                    if (icon == null) {
                        null
                    } else {
                        uploadRoomIcon(id, parentDir, icon, fileRefs, it)
                    }
                InsertRoomTuple(it, s, id, now())
            }
        database.file.insertFileRefs(
            fileRefs.map { (fileInfo, admin, id) ->
                FileRef(
                    id = SnowflakeFactory.nextId(),
                    createdTime = now(),
                    objectId = id,
                    objectType = ObjectType.ROOM,
                    author = userMap.getValue(admin).id,
                    mediaName = fileInfo.name,
                    fileId = fileInfo.id,
                )
            },
        ).getOrThrow()
        val memberList = getRoomMembers(data, userMap)
        val roomList =
            data.map { roomTuple ->
                val presetRoom = roomTuple.room
                Room(
                    roomTuple.id,
                    roomTuple.createdTime,
                    presetRoom.id,
                    presetRoom.name,
                    userMap.getValue(presetRoom.admin).id,
                    roomTuple.icon,
                    communityMap[presetRoom.community]?.id,
                )
            }
        database.admin.batchAddRooms(roomList, memberList)
        roomSearchService.saveDocument(
            roomList.map {
                RoomDocument.fromRoom(it)
            },
        ).getOrThrow()
        // 为加入房间的成员添加 user log
        memberList.forEach { member ->
            addUserLog(member.uid, UserLogType.JOIN, member.objectId ob member.objectType).getOrThrow()
        }
        addMemberDocuments(memberList, userMap, roomMap = roomList.associateBy { it.id })
    }

    private suspend fun getRoomMembers(data: List<InsertRoomTuple>, userMap: Map<String?, User>): List<Member> {
        val memberList =
            data.flatMap { roomTuple ->
                (
                    roomTuple.room.users.map { s ->
                        userMap.getValue(s).id
                    } + userMap.getValue(roomTuple.room.admin).id
                    ).distinct().map { uid ->
                    Member(
                        SnowflakeFactory.nextId(),
                        uid,
                        roomTuple.id,
                        ObjectType.ROOM,
                        roomTuple.createdTime,
                        MemberStatus.JOINED,
                        roomTuple.createdTime,
                    )
                }
            }
        return memberList
    }

    private suspend fun Backend.getRoomCommunityMap(l: List<PresetRoom>): Map<String, Community> =
        database.community.getRawCommunities(
            AidListFetch(
                l.mapNotNull {
                    it.community
                }.distinct(),
            ),
        ).getOrThrow().associate {
            it.community.aid to it.community
        }

    private suspend fun Backend.getRoomUserMap(l: List<PresetRoom>): Map<String?, User> {
        val userMap =
            database.user.getRawUsers(
                AidListFetch(
                    l.flatMap {
                        it.users + it.admin
                    }.distinct(),
                ),
            ).getOrThrow().associate {
                it.user.aid to it.user
            }
        return userMap
    }

    private suspend fun Backend.addTopics(presetValue: PresetValue, parentDir: File) {
        Napier.i {
            "topics count ${presetValue.topicData?.size ?: "<none>"}"
        }
        val data = checkNotNull(presetValue.topicData) { "Preset topic data is missing" }
        val userMap =
            database.user.getRawUsers(
                AidListFetch(
                    data.map {
                        it.author
                    }.distinct(),
                ),
            ).getOrThrow().associate {
                checkNotNull(it.user.aid) { "Imported user aid is missing" } to it.user
            }
        data.groupBy {
            when {
                it.community != null -> ObjectType.COMMUNITY
                it.room != null -> ObjectType.ROOM
                else -> ObjectType.USER
            }
        }.forEach { (objectType, list) ->
            if (objectType == ObjectType.ROOM) {
                addTopicsIntoRoom(list, userMap, parentDir)
            } else {
                addTopics(list, userMap, parentDir, objectType)
            }
        }
    }

    private suspend fun Backend.addUsers(presetValue: PresetValue, parentDir: File) {
        val userList = presetValue.userData ?: return
        Napier.i {
            "users count ${presetValue.userData?.size ?: "<none>"}"
        }
        val (tuples, fileRefs) = getUserData(userList, parentDir)
        val users =
            tuples.map {
                getUserFromTuple(it, parentDir)
            }
        database.admin.batchAddUser(users)
        database.file.insertFileRefs(
            fileRefs.map { (uploadFile, id) ->
                FileRef(
                    id = SnowflakeFactory.nextId(),
                    createdTime = now(),
                    objectId = id,
                    objectType = ObjectType.USER,
                    author = id,
                    mediaName = uploadFile.name,
                    fileId = uploadFile.id,
                )
            },
        ).getOrThrow()
        // 为每个创建的用户添加 user log
        users.forEach { user ->
            addUserLog(user.id, UserLogType.SIGN_UP, user.id ob ObjectType.USER).getOrThrow()
        }
        val userMap =
            database.user.getRawUsers(AidListFetch(listOf("System"))).getOrThrow()
                .associate {
                    it.user.aid to it.user
                }
        val adminUid = userMap.getValue("System").id
        val realUser =
            users.filter {
                it.id > 1000
            }
        database.admin.batchAddRooms(
            realUser.map {
                buildUserNotificationRoom(it, adminUid)
            },
            realUser.flatMap {
                buildMemberForNotificationRoom(it, adminUid)
            },
        )
        userSearchService.saveDocument(
            users.map {
                UserDocument.fromUser(it)
            },
        )
    }

    private suspend fun Backend.addCommunities(presetValue: PresetValue, parentDir: File) {
        val communityData = checkNotNull(presetValue.communityData) { "Preset community data is missing" }
        Napier.i {
            "communities count ${presetValue.communityData?.size ?: "<none>"}"
        }
        val (data, fileRefs) = getCommunityTuples(communityData, parentDir)
        val userMap =
            database.user.getRawUsers(
                AidListFetch(
                    data.flatMap {
                        it.community.users.orEmpty() + (it.community.admin ?: "System")
                    }.distinct(),
                ),
            ).getOrThrow().associate {
                it.user.aid to it.user
            }
        val communities =
            data.map {
                Community(
                    it.id,
                    it.createdTime,
                    it.community.id,
                    it.community.name,
                    userMap.getValue(it.community.getSafeAdmin()).id,
                    MemberPolicy.OPEN,
                    it.icon,
                    fontSettings = it.fontSettings,
                )
            }
        val memberList = getCommunityMembers(data, userMap)
        database.admin.batchAddCommunities(communities, memberList)
        communitySearchService.saveDocument(
            communities.map {
                CommunityDocument.fromCommunity(it)
            },
        ).getOrThrow()

        // Construct FileRefs here
        val finalFileRefs =
            fileRefs.map { (fileInfo, communityId, adminAid) ->
                FileRef(
                    id = SnowflakeFactory.nextId(),
                    createdTime = now(),
                    objectId = communityId,
                    objectType = ObjectType.COMMUNITY,
                    author = userMap.getValue(adminAid).id,
                    mediaName = fileInfo.name,
                    fileId = fileInfo.id,
                )
            }
        database.file.insertFileRefs(finalFileRefs).getOrThrow()

        // 为社区创建者添加 user log
        communities.forEach { community ->
            addUserLog(community.owner, UserLogType.CREATE, community.id ob ObjectType.COMMUNITY).getOrThrow()
        }
        // 为加入的成员添加 user log
        memberList.forEach { member ->
            addUserLog(member.uid, UserLogType.JOIN, member.objectId ob member.objectType).getOrThrow()
        }
        addMemberDocuments(memberList, userMap, communityMap = communities.associateBy { it.id })
    }

    private suspend fun getCommunityMembers(
        data: List<InsertCommunityTuple>,
        userMap: Map<String?, User>,
    ): List<Member> {
        val memberList =
            data.flatMap { communityTuple ->
                (
                    communityTuple.community.users?.map { s ->
                        userMap.getValue(s).id
                    }.orEmpty() + userMap.getValue(communityTuple.community.getSafeAdmin()).id
                    ).map { uid ->
                    Member(
                        SnowflakeFactory.nextId(),
                        uid,
                        communityTuple.id,
                        ObjectType.COMMUNITY,
                        communityTuple.createdTime,
                        MemberStatus.JOINED,
                        communityTuple.createdTime,
                    )
                }
            }
        return memberList
    }

    private suspend fun Backend.getCommunityTuples(
        communityData: List<PresetCommunity>,
        parentDir: File,
    ): Pair<List<InsertCommunityTuple>, List<Triple<FileInfo, PrimaryKey, String>>> {
        val fileRefs = mutableListOf<Triple<FileInfo, PrimaryKey, String>>()
        val tuples =
            communityData.map {
                val id = SnowflakeFactory.nextId()
                val icon = it.icon
                val font = it.font
                val iconMedia =
                    if (icon == null) {
                        null
                    } else {
                        val uploadFile = uploadFile(id, ObjectType.COMMUNITY, parentDir, listOf(icon)).first()
                        fileRefs.add(Triple(uploadFile, id, it.getSafeAdmin()))
                        uploadFile.id
                    }
                val fontSettings =
                    if (font == null) {
                        null
                    } else {
                        val fontMediaId = getFileInfoList(listOf("100/$font")).getOrThrow()?.firstOrNull()?.id
                        fontMediaId?.let { fontId -> FontSettings(contentFontId = fontId) }
                    }
                InsertCommunityTuple(it, iconMedia, id, fontSettings, now())
            }
        return tuples to fileRefs
    }

    private suspend fun Backend.addMemberDocuments(
        memberList: List<Member>,
        userMap: Map<String?, User>,
        communityMap: Map<PrimaryKey, Community> = emptyMap(),
        roomMap: Map<PrimaryKey, Room> = emptyMap(),
    ) {
        memberSearchService.saveDocument(
            memberList.mapNotNull { member ->
                val user = userMap.values.find { it.id == member.uid }
                user?.let { userInfo ->
                    val objectName =
                        when (member.objectType) {
                            ObjectType.COMMUNITY -> communityMap[member.objectId]?.name
                            ObjectType.ROOM -> roomMap[member.objectId]?.name
                            else -> null
                        } ?: return@mapNotNull null
                    MemberDocument.fromUserInfo(
                        id = member.id,
                        userInfo = userInfo.toUserInfo(),
                        objectId = member.objectId,
                        objectType = member.objectType,
                        objectName = objectName,
                    )
                }
            },
        ).getOrThrow()
    }

    private suspend fun Backend.addTopics(
        list: List<PresetTopic>,
        userMap: Map<String, User>,
        parentDir: File,
        objectType: ObjectType,
    ) {
        val communityMap =
            database.community.getRawCommunities(
                AidListFetch(
                    list.mapNotNull {
                        it.community
                    },
                ),
            ).getOrThrow().associate {
                it.community.aid to it.community.id
            }
        val tuples = getTopicTuples(list, parentDir, objectType, userMap, communityMap)
        database.admin.batchAddTopics(tuples, userMap, objectType).getOrThrow()
        topicSearchService.saveDocument(getDocumentsFromTuples(tuples, objectType, userMap)).getOrThrow()
        tuples.forEach { topicTuple ->
            uploadTopicMedias(parentDir, userMap, topicTuple.id, topicTuple.topic)
        }
        // 为 topic 作者添加 user log
        tuples.forEach { tuple ->
            val authorId = userMap.getValue(tuple.topic.author).id
            addUserLog(authorId, UserLogType.CREATE, tuple.id ob ObjectType.TOPIC).getOrThrow()
        }
        batchAddSubscriptions(tuples, userMap)
    }

    private fun getDocumentsFromTuples(
        tuples: List<InsertTopicTuple>,
        objectType: ObjectType,
        userMap: Map<String, User>,
    ): List<TopicDocument> =
        tuples.mapIndexed { index, topicTuple ->
            val level = topicTuple.level
            TopicDocument(
                topicTuple.id,
                topicTuple.content.decodeToString(),
                topicTuple.rootId,
                objectType.name,
                when (level) {
                    0 -> topicTuple.rootId

                    else ->
                        tuples[
                            index -
                                checkNotNull(
                                    topicTuple.topic.parent,
                                ) { "Nested topic parent is missing" },
                        ].id
                },
                when (level) {
                    0 -> objectType
                    else -> ObjectType.TOPIC
                }.name,
                userMap.getValue(topicTuple.topic.author).id,
            )
        }

    private suspend fun getTopicTuples(
        list: List<PresetTopic>,
        parentDir: File,
        objectType: ObjectType,
        userMap: Map<String, User>,
        communityMap: Map<String, PrimaryKey>,
    ): List<InsertTopicTuple> =
        list.mapIndexed { index, addTopic ->
            val id = SnowflakeFactory.nextId()
            val level = addTopic.level
            val parent = addTopic.parent
            val content = getTopicContent(addTopic, parentDir).encodeToByteArray()
            val rootId =
                if (objectType == ObjectType.USER) {
                    userMap.getValue(addTopic.author).id
                } else {
                    communityMap.getValue(checkNotNull(addTopic.community) { "Topic community is missing" })
                }
            InsertTopicTuple(
                addTopic,
                index,
                if (parent == null || parent == 0 || level == null || level == 0) {
                    0
                } else {
                    level
                },
                id,
                content,
                false,
                rootId,
            )
        }

    private suspend fun Backend.getUserData(
        userList: List<PresetUser>,
        parentDir: File,
    ): Pair<List<UserPresetTuple>, MutableList<Pair<FileInfo, PrimaryKey>>> {
        val fileRefs = mutableListOf<Pair<FileInfo, PrimaryKey>>()
        val users =
            userList.map {
                val id = it.id ?: SnowflakeFactory.nextId()
                val algoType = it.algoType?.let { value -> AlgoType.valueOf(value) } ?: AlgoType.P256
                val (derPublicKey, ad) = getPubKeyAndAddress(parentDir, it.privateKey, algoType)
                val icon = it.icon
                val p =
                    if (icon == null) {
                        null
                    } else {
                        uploadUserIcon(id, parentDir, icon, fileRefs)
                    }
                UserPresetTuple(it, p, derPublicKey, ad, id, algoType)
            }
        return users to fileRefs
    }

    private suspend fun getPubKeyAndAddress(
        parentDir: File,
        privatePath: String,
        algoType: AlgoType,
    ): Pair<String, String> =
        getAlgo(algoType).run {
            val derPublicKey =
                getDerPublicKeyFromPrivateKey(
                    File(parentDir, privatePath).readText().replace("\r\n", "\n"),
                ).getOrThrow()
            val ad = calcAddress(derPublicKey).getOrThrow()
            Pair(derPublicKey, ad)
        }

    private suspend fun Backend.addTopicsIntoRoom(
        list: List<PresetTopic>,
        userMap: Map<String, User>,
        parentDir: File,
    ) {
        val roomMap =
            database.room.getRoomList(
                AidListFetch(
                    list.mapNotNull {
                        it.room
                    },
                ),
            ).getOrThrow().associateBy { it.aid }
        insertEncryptedTopicToRoom(
            parentDir,
            roomMap,
            list.filter {
                roomMap[it.room]?.communityId == null
            },
            userMap,
        )
        insertUnEncryptedTopicToRoom(
            parentDir,
            roomMap,
            userMap,
            list.filter {
                roomMap[it.room]?.communityId != null
            },
        )
    }

    private suspend fun Backend.insertUnEncryptedTopicToRoom(
        parentDir: File,
        roomMap: Map<String, Room>,
        userMap: Map<String, User>,
        topicList: List<PresetTopic>,
    ) {
        val tuples =
            topicList.mapIndexed { index, topic ->
                val id = SnowflakeFactory.nextId()
                val level = topic.level
                val parent = topic.parent
                val content = getTopicContent(topic, parentDir).encodeToByteArray()
                val roomName = checkNotNull(topic.room) { "Topic room is missing" }
                val rootId = roomMap.getValue(roomName).id
                val l =
                    if (parent == null || parent == 0 || level == null || level == 0) {
                        0
                    } else {
                        level
                    }
                InsertTopicTuple(topic, index, l, id, content, false, rootId)
            }
        database.admin.batchAddTopics(tuples, userMap, ObjectType.ROOM).getOrThrow()
        topicSearchService.saveDocument(
            tuples.mapIndexed { index, topicTuple ->
                val topic = topicTuple.topic
                val level = topic.level
                val roomName = checkNotNull(topic.room) { "Topic room is missing" }
                TopicDocument(
                    topicTuple.id,
                    topicTuple.content.decodeToString(),
                    roomMap.getValue(roomName).id,
                    ObjectType.ROOM.name,
                    when (level) {
                        null, 0 -> roomMap.getValue(roomName).id
                        else -> tuples[index - checkNotNull(topic.parent) { "Nested topic parent is missing" }].id
                    },
                    (if (level == 0) ObjectType.ROOM else ObjectType.TOPIC).name,
                    userMap.getValue(topic.author).id,
                )
            },
        ).getOrThrow()
        tuples.forEach { topicTuple ->
            uploadTopicMedias(parentDir, userMap, topicTuple.id, topicTuple.topic)
        }
        // 为 topic 作者添加 user log
        tuples.forEach { tuple ->
            val authorId = userMap.getValue(tuple.topic.author).id
            addUserLog(authorId, UserLogType.CREATE, tuple.id ob ObjectType.TOPIC).getOrThrow()
        }
        batchAddSubscriptions(tuples, userMap)
    }

    private suspend fun Backend.uploadTopicMedias(
        parentDir: File,
        userMap: Map<String, User>,
        topicId: PrimaryKey,
        presetTopic: PresetTopic,
    ) {
        val content = getTopicContent(presetTopic, parentDir)
        val mediaLink = extractMarkdownMediaLink(content)
        val author = userMap.getValue(presetTopic.author).id
        val fileInfos =
            uploadFile(
                author,
                ObjectType.USER,
                parentDir,
                mediaLink.map {
                    "medias/topics/$it"
                },
            )
        val fileRefs =
            fileInfos.map { info ->
                FileRef(
                    id = SnowflakeFactory.nextId(),
                    createdTime = now(),
                    objectId = topicId,
                    objectType = ObjectType.TOPIC,
                    author = author,
                    mediaName = info.name,
                    fileId = info.id,
                )
            }
        database.file.insertFileRefs(fileRefs).getOrThrow()
    }

    private suspend fun Backend.insertEncryptedTopicToRoom(
        parentDir: File,
        roomMap: Map<String, Room>,
        topicList: List<PresetTopic>,
        userMap: Map<String, User>,
    ) {
        val roomAids =
            topicList.mapNotNull {
                it.room
            }.distinct()
        val roomMembers = database.admin.getAllMembers(roomAids).getOrThrow().groupBy { it.roomAid }
        val encryptedContents = buildEncryptedTopicContents(topicList, parentDir)
        val encryptedKeys = buildEncryptedTopicKeys(encryptedContents, roomMembers)
        val tuples = buildEncryptedTopicTuples(encryptedContents, roomMap)
        database.admin.batchAddTopics(tuples, userMap, ObjectType.ROOM).getOrThrow()
        database.admin.batchAddEncryptTopicKeys(encryptedKeys).getOrThrow()
        uploadEncryptedTopicMedias(tuples, roomMap, parentDir)
        // 为 topic 作者添加 user log
        tuples.forEach { tuple ->
            val authorId = userMap.getValue(tuple.topic.author).id
            addUserLog(authorId, UserLogType.CREATE, tuple.id ob ObjectType.TOPIC).getOrThrow()
        }
        batchAddSubscriptions(tuples, userMap)
    }

    private suspend fun buildEncryptedTopicContents(
        topicList: List<PresetTopic>,
        parentDir: File,
    ): List<EncryptedTopicTuple> =
        topicList.map { presetTopic ->
            val (encryptedContent, aesBytes) =
                encryptDataByAES(
                    getTopicContent(presetTopic, parentDir),
                ).getOrThrow()
            EncryptedTopicTuple(encryptedContent, aesBytes, SnowflakeFactory.nextId(), presetTopic)
        }

    private suspend fun buildEncryptedTopicKeys(
        encryptedContents: List<EncryptedTopicTuple>,
        roomMembers: Map<String, List<MemberAuthData>>,
    ) = encryptedContents.flatMap { encryptedContent ->
        val roomAid = requireNotNull(encryptedContent.presetTopic.room) { "Encrypted topic must belong to a room" }
        roomMembers.getValue(roomAid).map { member ->
            Triple(
                encryptedContent.id,
                encryptTopicKeyForMember(member, encryptedContent.aesKey),
                member.userId,
            )
        }
    }

    private suspend fun encryptTopicKeyForMember(member: MemberAuthData, aesBytes: ByteArray): ByteArray {
        val derPublicKey =
            if (member.algoType == AlgoType.DILITHIUM) {
                requireNotNull(member.encryptionPublicKey) {
                    "Missing encryption public key for ${member.userId}"
                }
            } else {
                member.publicKey
            }
        return getAlgo(member.algoType).encryptionAlgo.kemEncrypt(derPublicKey, aesBytes).getOrThrow()
    }

    private fun buildEncryptedTopicTuples(encryptedContents: List<EncryptedTopicTuple>, roomMap: Map<String, Room>) =
        encryptedContents.mapIndexed { index, tuple ->
            val level = tuple.presetTopic.level
            val parent = tuple.presetTopic.parent
            val level1 =
                if (parent == null || parent == 0 || level == null || level == 0) {
                    0
                } else {
                    level
                }
            InsertTopicTuple(
                tuple.presetTopic,
                index,
                level1,
                tuple.id,
                tuple.encryptedContent,
                true,
                roomMap.getValue(checkNotNull(tuple.presetTopic.room) { "Topic room is missing" }).id,
            )
        }

    private suspend fun Backend.uploadEncryptedTopicMedias(
        tuples: List<InsertTopicTuple>,
        roomMap: Map<String, Room>,
        parentDir: File,
    ) {
        tuples.forEach { topicTuple ->
            val room = roomMap[topicTuple.topic.room]
            if (room != null) {
                val content = getTopicContent(topicTuple.topic, parentDir)
                uploadFile(
                    room.id,
                    ObjectType.ROOM,
                    parentDir,
                    extractMarkdownMediaLink(content).map {
                        "medias/topics/$it"
                    },
                )
            }
        }
    }

    private suspend fun Backend.batchAddSubscriptions(tuples: List<InsertTopicTuple>, userMap: Map<String, User>) {
        database.admin.batchAddSubscription(
            tuples.map {
                UserSubscription(it.id, userMap.getValue(it.topic.author).id, it.id, ObjectType.TOPIC, now())
            },
        ).getOrThrow()
        // 为订阅添加 user log
        tuples.forEach { tuple ->
            val authorId = userMap.getValue(tuple.topic.author).id
            addUserLog(authorId, UserLogType.ADD_SUBSCRIPTION, tuple.id ob ObjectType.TOPIC).getOrThrow()
        }
    }

    suspend fun Backend.uploadFile(id: PrimaryKey, type: ObjectType, parentDir: File, p: List<String>): List<FileInfo> {
        if (p.isEmpty()) return emptyList()
        return tryUploadFiles(
            id,
            type,
            p.map {
                val path = File(parentDir, it)
                val name = path.name
                UploadPack(path, name, path.length(), "$id/$name", sha256File(path))
            },
        ).getOrThrow()
    }

    private fun getTopicContent(presetTopic: PresetTopic, parentDir: File): String {
        val content =
            if (presetTopic.type == "file") {
                File(parentDir, presetTopic.content).readText().replace("\r\n", "\n")
            } else {
                presetTopic.content
            }
        return content
    }

    private suspend fun Backend.uploadUserIcon(
        id: PrimaryKey,
        parentDir: File,
        icon: String,
        fileRefs: MutableList<Pair<FileInfo, PrimaryKey>>,
    ): PrimaryKey {
        val uploadFile = uploadFile(id, ObjectType.USER, parentDir, listOf(icon)).first()
        fileRefs.add(uploadFile to id)
        return uploadFile.id
    }

    private suspend fun Backend.uploadRoomIcon(
        id: PrimaryKey,
        parentDir: File,
        icon: String,
        fileRefs: MutableList<Triple<FileInfo, String, PrimaryKey>>,
        room: PresetRoom,
    ): PrimaryKey {
        val fileInfo = uploadFile(id, ObjectType.ROOM, parentDir, listOf(icon)).first()
        fileRefs.add(Triple(fileInfo, room.admin, id))
        return fileInfo.id
    }

    private suspend fun Backend.getUserFromTuple(tuple: UserPresetTuple, parentDir: File): User {
        val encPubKey =
            if (tuple.algoType == AlgoType.DILITHIUM) {
                val algo = getAlgo(tuple.algoType).encryptionAlgo as Type2Algo
                val encryptionPrivateKey = tuple.presetUser.encryptionPrivateKey
                val pemPrivateKey =
                    if (encryptionPrivateKey != null) {
                        File(parentDir, encryptionPrivateKey).readText().replace("\r\n", "\n")
                    } else {
                        algo.generateEncryptionPemKeyPair().getOrThrow().first
                    }
                algo.getDerEncryptionPublicKeyFromPemPrivateKey(pemPrivateKey).getOrThrow()
            } else {
                null
            }
        val notificationId = SnowflakeFactory.nextId()
        return User(
            tuple.presetUser.aid,
            encPubKey,
            tuple.publicKey,
            tuple.address,
            tuple.pic,
            tuple.presetUser.name.takeIf { s -> s.isNotBlank() } ?: nameService.parse(tuple.id),
            tuple.id,
            now(),
            0,
            PassType.RAW,
            tuple.algoType,
            notificationId,
        )
    }
}
