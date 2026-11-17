/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.FileInfoListResponse
import com.storyteller_f.alphaheart.app.PlatformHeadlessTest
import com.storyteller_f.alphaheart.app.utils.ClientFile
import com.storyteller_f.alphaheart.client.core.ConstPassHolder
import com.storyteller_f.alphaheart.client.core.createSimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.defaultClientConfigure
import com.storyteller_f.alphaheart.client.room.RoomModelStorage
import com.storyteller_f.alphaheart.client.room.getRoomDatabase
import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UploadRecordStatus
import com.storyteller_f.shared.utils.md5
import com.storyteller_f.shared.utils.sha256
import com.storyteller_f.storage.UploadCollection
import com.storyteller_f.storage.UploadInfo
import com.storyteller_f.storage.UploadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.Buffer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class UploadOwnerIsolationTest : PlatformHeadlessTest() {
    @Test
    fun samePathUploadsToThreeOwnersWithoutReadingTheOldPersonalHistory() =
        runTest {
            val database = getRoomDatabase("upload-owner-${Uuid.random().toHexString()}")
            val storage = RoomModelStorage(database)
            val owners = listOf(1L ob ObjectType.USER, 1L ob ObjectType.COMMUNITY, 1L ob ObjectType.ROOM)
            val requests = mutableListOf<ObjectTuple>()
            val mutex = Mutex()
            val session = createSingleUploadSession(requests, mutex)
            session.model.updateUser(UserInfo.EMPTY.copy(id = 1))
            val transferJob = SupervisorJob(backgroundScope.coroutineContext[Job])
            val tasks = SimpleTaskRegister(CoroutineScope(transferJob + Dispatchers.Default))
            val uploader = UploaderImpl({ UploadRuntime(session, storage) }, tasks, retainSelectedFiles = true)
            val file = memoryFile()
            val hash = md5(file.path)
            try {
                // Legacy personal callers omit owner; their completed history must retain its original namespace.
                uploader.upload(persistentListOf(file))
                awaitStatus(storage, owners[0], hash, UploadStatus.SUCCESS)
                awaitStopped(tasks, owners[0], hash)
                uploader.upload(persistentListOf(file), owners[1])
                uploader.upload(persistentListOf(file), owners[2])
                awaitStatus(storage, owners[1], hash, UploadStatus.SUCCESS)
                awaitStatus(storage, owners[2], hash, UploadStatus.SUCCESS)
                awaitStopped(tasks, owners[1], hash)
                awaitStopped(tasks, owners[2], hash)
                assertEquals(owners.toSet(), mutex.withLock { requests.toSet() })
                assertEquals(3, mutex.withLock { requests.size })
                owners.forEach { owner ->
                    val info = assertNotNull(storage.upload.getDocument(UploadCollection.fromOwner(owner), hash))
                    assertEquals(owner, info.owner)
                    assertEquals(file.size, info.progress)
                    assertEquals("text/plain", info.contentType)
                }
                assertEquals("upload_1", UploadCollection(1).getName())
                assertNotEquals(UploadCollection(1).getName(), UploadCollection.fromOwner(owners[1]).getName())
                val oldFields =
                    commonJson.parseToJsonElement(
                        commonJson.encodeToString(UploadInfo.EMPTY.copy(objectId = 1)),
                    ).jsonObject.filterKeys { it != "objectType" }
                val legacy = commonJson.decodeFromString<UploadInfo>(JsonObject(oldFields).toString())
                assertEquals(ObjectType.USER, legacy.objectType)
                assertEquals(UploadCollection(1), UploadCollection.fromInfo(legacy))
            } finally {
                withContext(NonCancellable) { transferJob.cancelAndJoin() }
                session.client.close()
                database.close()
            }
        }

    @Test
    fun chunkPauseResumeAndForgetAreIsolatedByOwnerAndReuseTheOriginalServerRecord() =
        runTest {
            val database = getRoomDatabase("upload-chunk-owner-${Uuid.random().toHexString()}")
            val storage = RoomModelStorage(database)
            val community = 9L ob ObjectType.COMMUNITY
            val room = 9L ob ObjectType.ROOM
            val blocked = CompletableDeferred<Unit>()
            val serverMutex = Mutex()
            val initialized = mutableMapOf<Long, ObjectTuple>()
            val uploaded = mutableMapOf<Long, MutableSet<Int>>()
            var holdCommunity = true
            val session =
                createChunkUploadSession(community, initialized, uploaded, serverMutex, blocked) { holdCommunity }
            session.model.updateUser(UserInfo.EMPTY.copy(id = 1))
            val transferJob = SupervisorJob(backgroundScope.coroutineContext[Job])
            val tasks = SimpleTaskRegister(CoroutineScope(transferJob + Dispatchers.Default))
            val uploader =
                UploaderImpl(
                    captureRuntime = { UploadRuntime(session, storage) },
                    taskRegister = tasks,
                    retainSelectedFiles = true,
                    persistTransportProgress = false,
                    defaultChunkSize = 4,
                )
            val file = memoryFile()
            val hash = md5(file.path)
            try {
                uploader.upload(persistentListOf(file), community)
                withContext(Dispatchers.Default) { withTimeout(5000) { blocked.await() } }
                val original =
                    assertNotNull(
                        storage.upload.getDocument(UploadCollection.fromOwner(community), hash)?.recordId,
                    )
                uploader.upload(persistentListOf(file), room)
                awaitStatus(storage, room, hash, UploadStatus.SUCCESS)
                uploader.pause(hash, community)
                awaitStatus(storage, community, hash, UploadStatus.PAUSED)
                awaitStopped(tasks, community, hash)
                awaitStopped(tasks, room, hash)
                assertEquals(
                    UploadStatus.SUCCESS,
                    storage.upload.getDocument(UploadCollection.fromOwner(room), hash)?.status,
                )
                // Releasing another owner's browser selection must not discard this paused upload's retained source.
                uploader.forget(hash, room)
                serverMutex.withLock { holdCommunity = false }
                uploader.resume(hash, community)
                val completed = awaitStatus(storage, community, hash, UploadStatus.SUCCESS)
                awaitStopped(tasks, community, hash)
                assertEquals(original, completed.recordId)
                assertEquals(12L, completed.progress)
                assertEquals(listOf(community, room), serverMutex.withLock { initialized.values.toList() })
                assertEquals(setOf(0, 1, 2), serverMutex.withLock { uploaded.getValue(original).toSet() })
                assertNotEquals(uploadTaskKey(community, hash), uploadTaskKey(room, hash))
            } finally {
                withContext(NonCancellable) { transferJob.cancelAndJoin() }
                session.client.close()
                database.close()
            }
        }
}

private suspend fun awaitStopped(tasks: TaskRegister, owner: ObjectTuple, hash: String) {
    withContext(Dispatchers.Default) {
        withTimeout(5000) { tasks.observeTask(uploadTaskKey(owner, hash)).first { !it } }
    }
}

private suspend fun awaitStatus(
    storage: RoomModelStorage,
    owner: ObjectTuple,
    hash: String,
    status: UploadStatus,
): UploadInfo =
    withContext(Dispatchers.Default) {
        withTimeout(5000) {
            assertNotNull(
                storage.upload.observeDatumByHash(UploadCollection.fromOwner(owner), hash)
                    .first { it?.status == status },
            )
        }
    }

private fun uploadedFile(id: Long, owner: ObjectTuple) =
    FileInfo.EMPTY.copy(
        id = id,
        owner = owner.objectId,
        ownerType = owner.objectType,
        name = "scope.txt",
        size = 12,
    )

private fun memoryFile(): ClientFile =
    object : ClientFile {
        override val name = "scope.txt"
        override val contentType = ContentType.Text.Plain
        override val size = 12L
        override val path = "memory://owner-scope/scope.txt"
        override fun source() = Buffer().apply { write(ByteArray(12) { it.toByte() }) }
    }

private fun createSingleUploadSession(requests: MutableList<ObjectTuple>, mutex: Mutex) =
    createSimpleUserSessionManager(
        "wss://uploads.invalid/ws",
        passHolder = ConstPassHolder(null),
        createClient = { model, cookies ->
            HttpClient(
                MockEngine { request ->
                    assertEquals("1", request.headers["X-Account-Id"])
                    val multipart = request.body.toByteArray()
                    assertTrue(multipart.decodeToString().contains("scope.txt"))
                    assertTrue(multipart.asList().windowed(12).any { it == (0 until 12).map { it.toByte() } })
                    val target =
                        ObjectTuple(
                            assertNotNull(request.url.parameters["objectId"]).toLong(),
                            ObjectType.valueOf(assertNotNull(request.url.parameters["objectType"])),
                        )
                    val id =
                        mutex.withLock {
                            requests += target
                            requests.size.toLong() + 100
                        }
                    respond(
                        commonJson.encodeToString(FileInfoListResponse(persistentListOf(uploadedFile(id, target)))),
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ) { defaultClientConfigure(cookies, model, "https://uploads.invalid/") }
        },
    ) { _, _, _ -> }

private fun createChunkUploadSession(
    community: ObjectTuple,
    initialized: MutableMap<Long, ObjectTuple>,
    uploaded: MutableMap<Long, MutableSet<Int>>,
    serverMutex: Mutex,
    blocked: CompletableDeferred<Unit>,
    holdCommunity: () -> Boolean,
) = createSimpleUserSessionManager(
    "wss://uploads.invalid/ws",
    passHolder = ConstPassHolder(null),
    createClient = { model, cookies ->
        HttpClient(
            MockEngine { request ->
                assertEquals("1", request.headers["X-Account-Id"])
                val path = request.url.encodedPath
                val json =
                    when {
                        path.endsWith("/init") -> {
                            val body =
                                commonJson.decodeFromString<CustomApi.Files.Chunks.InitBody>(
                                    request.body.toByteArray().decodeToString(),
                                )
                            assertEquals("text/plain", body.contentType)
                            val id =
                                serverMutex.withLock {
                                    val next = initialized.size.toLong() + 100
                                    initialized[next] = body.objectId ob body.objectType
                                    uploaded[next] = mutableSetOf()
                                    next
                                }
                            commonJson.encodeToString(CustomApi.Files.Chunks.InitResponse(id, 4))
                        }

                        path.endsWith("/status") -> {
                            val id = path.split('/').dropLast(1).last().toLong()
                            val indices = serverMutex.withLock { uploaded.getValue(id).toList() }
                            pendingChunkStatus(indices, id)
                        }

                        path.endsWith("/upload") -> {
                            val parts = path.split('/')
                            val id = parts[parts.lastIndex - 2].toLong()
                            val index = parts[parts.lastIndex - 1].toInt()
                            val shouldHold =
                                serverMutex.withLock { initialized[id] == community && holdCommunity() }
                            if (shouldHold) {
                                blocked.complete(Unit)
                                CompletableDeferred<Unit>().await()
                            }
                            val bytes = request.body.toByteArray()
                            assertContentEquals(ByteArray(4) { (index * 4 + it).toByte() }, bytes)
                            assertEquals(sha256(Buffer().apply { write(bytes) }), request.url.parameters["hash"])
                            serverMutex.withLock { uploaded.getValue(id).add(index) }
                            "{}"
                        }

                        path.endsWith("/complete") -> {
                            val id = path.split('/').dropLast(1).last().toLong()
                            val owner = serverMutex.withLock { initialized.getValue(id) }
                            commonJson.encodeToString(uploadedFile(id, owner))
                        }

                        else -> error("Unexpected upload endpoint")
                    }
                respond(json, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ) { defaultClientConfigure(cookies, model, "https://uploads.invalid/") }
    },
) { _, _, _ -> }

private fun pendingChunkStatus(indices: List<Int>, id: Long): String =
    commonJson.encodeToString(CustomApi.Files.Chunks.StatusResponse(indices, 4, 12, id, UploadRecordStatus.PENDING))
