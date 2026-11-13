/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.worker

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.backend.core.Backend
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
import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
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
import com.storyteller_f.alphaheart.backend.core.setLogPath
import com.storyteller_f.alphaheart.backend.exposed.buildExposedDatabase
import com.storyteller_f.alphaheart.cloud.error.WorkerError
import com.storyteller_f.alphaheart.cloud.ws.api.GlobalWsEventPublisher
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.model.LlmConfig
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.WorkerTask
import com.storyteller_f.shared.setupKmpLogger
import com.storyteller_f.shared.utils.now
import com.storytellerf.alphaheart.cloud.worker.moderation.KoogTopicSafetyReviewer
import com.storytellerf.alphaheart.cloud.worker.moderation.TopicSafetyReviewer
import com.storytellerf.alphaheart.cloud.worker.moderation.doTopicModerationTask
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MODERATION_LOG_TAG = "moderation"

fun main() {
    setLogPath()
    setupKmpLogger()
    loadCryptoLibIfNeed()
    SnowflakeFactory.setMachine(1)
    Napier.i {
        "start worker"
    }
    val env = readEnv()
    GlobalWsEventPublisher.configure(env["WS_RPC_URL"])
    val backend = buildBackendFromEnv(env)
    runBlocking {
        TopicSafetyReviewerProvider {
            createTopicSafetyReviewer(backend.database.getBackendConfig(LlmConfig.CONFIG_KEY))
        }.use { reviewerProvider ->
            Napier.i {
                "worker started"
            }
            val jobs = startWorkerTasks(backend, reviewerProvider)
            registerShutdownHook(jobs)
            jobs.joinAll()
        }
        Napier.i("worker done")
    }
}

internal class TopicSafetyReviewerProvider(
    private val factory: suspend () -> CustomResult<TopicSafetyReviewer?, WorkerError>,
) : AutoCloseable {
    private val initializationMutex = Mutex()
    private var reviewer: TopicSafetyReviewer? = null

    suspend fun get(): TopicSafetyReviewer? {
        val initializedReviewer =
            initializationMutex.withLock {
                reviewer ?: run {
                    val result = factory()
                    val failure = result.exceptionOrNull()
                    if (failure is CancellationException) throw failure
                    if (failure != null) {
                        Napier.e(tag = MODERATION_LOG_TAG, throwable = failure) {
                            "Unable to initialize topic safety reviewer; will retry"
                        }
                    }
                    reviewer = result.getOrNull()
                    reviewer
                }
            }
        return initializedReviewer
    }

    override fun close() {
        reviewer?.close()
    }
}

internal fun createTopicSafetyReviewer(
    configResult: CustomResult<String?, DatabaseError>,
): CustomResult<TopicSafetyReviewer?, WorkerError> =
    configResult
        .andThen(
            WorkerError::fromError,
        ) { value ->
            if (value == null) {
                CustomResult.success(
                    null,
                )
            } else {
                captureFailure(WorkerError::from) { commonJson.decodeFromString<LlmConfig>(value) }
            }
        }
        .andThenPresent(WorkerError::fromError) { config ->
            Napier.i(tag = MODERATION_LOG_TAG) {
                "using LLM provider: ${config.provider}"
            }
            captureFailure(WorkerError::from) { KoogTopicSafetyReviewer.create(config) }
        }
        .onSuccess { reviewer ->
            if (reviewer == null) {
                Napier.w(tag = MODERATION_LOG_TAG) {
                    "LLM configuration not found; topic moderation will retry after configuration is added"
                }
            }
        }.mapError(WorkerError::fromError)

private fun CoroutineScope.startWorkerTasks(
    backend: Backend,
    reviewerProvider: TopicSafetyReviewerProvider,
): List<Job> {
    val jobs =
        listOf(
            launchWorkerTask(backend, TaskRecordType.TOPIC_ACG, "acg") { config ->
                backend.doAcgTask(config.fetchSize)
            },
            launchWorkerTask(backend, TaskRecordType.INTRO, "intro") { config ->
                backend.doIntroTask(config.fetchSize)
            },
            launchWorkerTask(backend, TaskRecordType.SUBSCRIPTION, "subscription") { config ->
                backend.doSubscriptionTask(config.fetchSize)
            },
            launchWorkerTask(backend, TaskRecordType.TITLE, "title") { config ->
                backend.doTitleTask(config.fetchSize)
            },
            launchWorkerTask(backend, TaskRecordType.TOPIC_MODERATION, "topic moderation") { config ->
                reviewerProvider.get()?.let { reviewer ->
                    backend.doTopicModerationTask(reviewer, config.fetchSize)
                }
            },
        )
    return jobs
}

private fun CoroutineScope.launchWorkerTask(
    backend: Backend,
    type: TaskRecordType,
    name: String,
    task: suspend (WorkerTask) -> Unit,
): Job {
    val job =
        launch {
            while (isActive) {
                val configResult = backend.database.workerTask.getWorkerTask(type)
                executeConfiguredTaskIteration(name, configResult.mapError(WorkerError::fromError), task)
            }
        }
    return job
}

internal suspend fun executeConfiguredTaskIteration(
    name: String,
    configResult: CustomResult<WorkerTask?, WorkerError>,
    task: suspend (WorkerTask) -> Unit,
    wait: suspend (Long) -> Unit = { delay(it) },
) {
    val failure = configResult.exceptionOrNull()
    if (failure is CancellationException) throw failure
    if (failure != null) {
        Napier.e(tag = "task", throwable = failure) {
            "failed to load $name task configuration"
        }
        wait(WORKER_TASK_POLL_MILLIS)
        return
    }
    val config = configResult.getOrNull()
    if (config?.isEnabled != true) {
        Napier.d(tag = "task") {
            "$name task is not configured or is disabled"
        }
        wait(WORKER_TASK_POLL_MILLIS)
        return
    }
    Napier.i(tag = "task") {
        "execute $name task at ${now()}"
    }
    task(config)
    wait(config.waitDurationMillis)
}

private fun registerShutdownHook(jobs: List<Job>) {
    val shutdownHook =
        Thread(
            {
                Napier.i {
                    "worker received shutdown signal"
                }
                jobs.forEach(Job::cancel)
            },
            "worker-shutdown",
        )
    Runtime.getRuntime().addShutdownHook(shutdownHook)
}

class WorkerBackend(
    override val customConfig: CustomConfig,
    override val topicSearchService: TopicSearchService,
    override val roomSearchService: RoomSearchService,
    override val communitySearchService: CommunitySearchService,
    override val userSearchService: UserSearchService,
    override val memberSearchService: MemberSearchService,
    override val fileSearchService: FileSearchService,
    override val objectStorageService: ObjectStorageService,
    override val nameService: NameService,
    override val database: com.storyteller_f.alphaheart.backend.core.CombinedDatabase,
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

    return WorkerBackend(
        customConfig = customConfig,
        topicSearchService = topicSearchService,
        roomSearchService = roomSearchService,
        communitySearchService = communitySearchService,
        userSearchService = userSearchService,
        memberSearchService = memberSearchService,
        fileSearchService = fileSearchService,
        objectStorageService = mediaService,
        nameService = buildNameService(env),
        database = buildExposedDatabase(databaseConnection),
    )
}

internal const val WORKER_TASK_POLL_MILLIS = 10_000L
