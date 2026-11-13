/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.built_in_bot

import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GoogleSearch
import com.google.genai.types.Tool
import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.AuthKey
import com.storyteller_f.alphaheart.client.core.SimplePassHolder
import com.storyteller_f.alphaheart.client.core.SimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.buildWebSocketUrl
import com.storyteller_f.alphaheart.client.core.createSimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.defaultClientConfigure
import com.storyteller_f.alphaheart.client.core.getClient
import com.storyteller_f.alphaheart.client.core.getTopicList
import com.storyteller_f.alphaheart.client.core.getUserCommunities
import com.storyteller_f.alphaheart.client.core.userSignIn
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.TopicPinSearch
import com.storyteller_f.shared.setupKmpLogger
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.now
import io.github.aakira.napier.Napier
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.io.encoding.Base64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

fun main() = runBot()

private fun runBot() {
    setupKmpLogger()
    loadCryptoLibIfNeed()
    val base64BotPem = requireEnvironment("BOT_PEM")
    val httpUrl = requireEnvironment("SERVER_URL")
    val wsUrl = requireEnvironment("WS_SERVER_URL")
    val pemPrivateKey = Base64.decode(base64BotPem).decodeToString()
    val passHolder = SimplePassHolder()
    val sessionManager =
        createSimpleUserSessionManager(
            buildWebSocketUrl(wsUrl),
            AcceptAllCookiesStorage(),
            passHolder,
            { model, cookieManager ->
                getClient {
                    defaultClientConfigure(cookieManager, model, httpUrl, LogLevel.INFO)
                }
            },
        ) { r, t, _ ->
        }
    val commentPrompt = readResource("comment.prompt")
    val newsPrompt = readResource("news.prompt")
    val client = Client()
    runBlocking {
        val job =
            launch {
                // 确保第一次登录成功
                while (isActive) {
                    try {
                        val algo = getAlgo(AlgoType.P256)
                        val derPriKey = algo.getDerPrivateKey(pemPrivateKey).getOrThrow()
                        val derPubKey = algo.getDerPublicKeyFromPrivateKey(pemPrivateKey).getOrThrow()
                        val authKey = AuthKey.P256(pemPrivateKey, derPriKey, derPubKey)
                        sessionManager.userSignIn(authKey, passHolder)
                        break
                    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                        throw cancellation
                    } catch (e: Exception) {
                        Napier.e(e) {
                            "login failed"
                        }
                    }
                }
                processJob(this, sessionManager, client, commentPrompt, newsPrompt)
            }
        // 注册 JVM 关闭钩子，捕获 SIGINT / SIGTERM
        Runtime.getRuntime().addShutdownHook(
            Thread {
                Napier.i { "Shutdown signal received; stopping bot" }
                job.cancel()
                Thread.sleep(1000)
            },
        )
        job.join()
        Napier.i("bot done")
    }
}

private fun requireEnvironment(name: String): String = System.getenv(name) ?: error("$name does not exist")

private suspend fun processJob(
    scope: CoroutineScope,
    sessionManager: SimpleUserSessionManager,
    client: Client,
    commentPrompt: String,
    newsPrompt: String,
) {
    val job1 =
        scope.launch {
            loop(this, 1.minutes) {
                processCommunityTask(sessionManager) { communityInfo ->
                    handleCommunityComment(sessionManager, client, communityInfo, commentPrompt)
                }
            }
        }
    val job2 =
        scope.launch {
            loop(this, 1.hours) {
                processCommunityTask(sessionManager) { communityInfo ->
                    handleCommunityNews(sessionManager, client, communityInfo, newsPrompt)
                }
            }
        }
    job1.join()
    job2.join()
}

private suspend fun loop(scope: CoroutineScope, duration: Duration, block: suspend () -> Unit) {
    while (scope.isActive) {
        try {
            block()
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            } else if (e.message?.contains("User location is not supported for the API use") == true) {
                break
            } else {
                Napier.e(e) {
                    "handleCommunityNews failed"
                }
            }
        }
        delay(duration)
    }
}

private fun readResource(name: String): String =
    ClassLoader.getSystemResourceAsStream(name)!!.bufferedReader().readText()

private suspend fun processCommunityTask(
    sessionManager: SimpleUserSessionManager,
    extracted: suspend (CommunityInfo) -> Unit,
) {
    var next: String? = null
    while (true) {
        val resp =
            sessionManager.getUserCommunities(
                CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(nextPageToken = next),
            ).getOrThrow()
        delay(1.seconds)
        resp.data.forEach { communityInfo ->
            extracted(communityInfo.communityInfo)
        }
        next = resp.pagination?.nextPageToken ?: break
    }
}

private suspend fun handleCommunityComment(
    sessionManager: SimpleUserSessionManager,
    client: Client,
    info: CommunityInfo,
    prompt: String,
) {
    Napier.i {
        "check community latest commented topic ${info.name}[${info.aid}]"
    }
    var pre = getLatestHasCommentedTopic(sessionManager, info).toString()
    while (true) {
        val resp =
            sessionManager.getTopicList(
                ObjectType.COMMUNITY,
                info.id,
                TopicPinSearch.UNSPECIFIED,
                PaginationQuery(null, pre, size = 10),
            ).getOrThrow()
        delay(1.seconds)
        resp.data.forEach { topicInfo ->
            val isAuthor = topicInfo.author == sessionManager.model.uid
            if (isAuthor || topicInfo.hasComment) {
                Napier.i {
                    "skip topic ${topicInfo.id} " +
                        "isAuthor: $isAuthor " +
                        "hasComment: ${topicInfo.hasComment} " +
                        "createdTime: ${topicInfo.createdTime}"
                }
            } else {
                Napier.i {
                    "handle ${topicInfo.id} createdTime: ${topicInfo.createdTime}"
                }
                handleTopic(topicInfo, client, sessionManager, prompt)
                delay(1.seconds)
            }
        }
        pre = resp.pagination?.prePageToken ?: break
    }
}

private suspend fun getLatestHasCommentedTopic(sessionManager: SimpleUserSessionManager, info: CommunityInfo): Long {
    var next: String? = null
    // 找出最新的评论过的topic
    var latestHasCommentedTopicId = 0L
    while (true) {
        val resp =
            sessionManager.getTopicList(
                ObjectType.COMMUNITY,
                info.id,
                TopicPinSearch.UNSPECIFIED,
                PaginationQuery(next, null, size = 10),
            ).getOrThrow()
        delay(1.seconds)
        for (topicInfo in resp.data) {
            if (topicInfo.hasComment) {
                latestHasCommentedTopicId = topicInfo.id
                break
            }
        }
        if (latestHasCommentedTopicId != 0L) break
        next = resp.pagination?.nextPageToken ?: break
    }
    Napier.i {
        "latest commented topic is $latestHasCommentedTopicId"
    }
    return latestHasCommentedTopicId
}

private suspend fun handleTopic(
    topicInfo: TopicDetail,
    client: Client,
    sessionManager: SimpleUserSessionManager,
    prompt: String,
) {
    val plain = (topicInfo.content as TopicContent.Plain).plain
    val text =
        if (plain.length < 10) {
            null
        } else {
            val response =
                client.models.generateContent(
                    "gemini-2.5-flash",
                    "$prompt\n${topicInfo.content}",
                    null,
                )
            response.text()?.take(1000)
        } ?: "👍"
    sessionManager.createTopic(ObjectType.TOPIC, topicInfo.id, text)
        .onSuccess {
            Napier.i {
                "createNewTopic success $text"
            }
        }.onFailure {
            Napier.e(it.cause) {
                "createNewTopic failed"
            }
        }
    delay(1.seconds)
}

@OptIn(ExperimentalTime::class)
private suspend fun handleCommunityNews(
    sessionManager: UserSessionManager,
    client: Client,
    communityInfo: CommunityInfo,
    prompt: String,
) {
    Napier.i {
        "check community bot created latest topic ${communityInfo.name}[${communityInfo.aid}]"
    }
    var next: String? = null
    // 找出最新的评论过的topic
    var latestTopic: TopicDetail? = null
    while (true) {
        val resp =
            sessionManager.getTopicList(
                ObjectType.COMMUNITY,
                communityInfo.id,
                TopicPinSearch.UNSPECIFIED,
                PaginationQuery(next, null, size = 10),
            ).getOrThrow()
        delay(1.seconds)
        for (topicInfo in resp.data) {
            if (topicInfo.author == sessionManager.model.uid) {
                latestTopic = topicInfo
                break
            }
        }
        if (latestTopic != null) break
        next = resp.pagination?.nextPageToken ?: break
    }
    Napier.i {
        "latest bot created topic ${latestTopic?.id ?: "<none>"}"
    }
    addTopic(latestTopic, client, prompt, communityInfo, sessionManager)
}

@OptIn(ExperimentalTime::class)
private suspend fun addTopic(
    latestTopic: TopicDetail?,
    client: Client,
    prompt: String,
    communityInfo: CommunityInfo,
    sessionManager: UserSessionManager,
) {
    val now = now()
    val previousDate = now.toInstant(TimeZone.UTC).minus(1.days).toLocalDateTime(TimeZone.UTC)
    val year = previousDate.year
    val month = previousDate.month // 1-12
    val previousDay = previousDate.day
    Napier.i {
        "previous day [$year $month $previousDay] and now is $now"
    }
    if (latestTopic != null) {
        Napier.i {
            "latest bot created topic is ${latestTopic.id} createdTime: ${latestTopic.createdTime}"
        }
        val start = latestTopic.createdTime
        val latestYear = start.year
        val latestMonth = start.month
        val latestDay = start.day
        if (latestDay != previousDay + 1 || latestYear != year || latestMonth != month) {
            createNewsTopic(client, prompt, communityInfo, sessionManager, previousDate)
        } else {
            Napier.i {
                "skip create topic"
            }
        }
    } else {
        Napier.i {
            "never created topic"
        }
        createNewsTopic(client, prompt, communityInfo, sessionManager, previousDate)
    }
}

private suspend fun createNewsTopic(
    client: Client,
    newPrompt: String,
    communityInfo: CommunityInfo,
    sessionManager: UserSessionManager,
    date: LocalDateTime,
) {
    val year = date.year
    val month = date.month // 1-12
    val day = date.day
    val response =
        client.models.generateContent(
            "gemini-2.5-flash",
            "$newPrompt\n日期:${year}年${month}月${day}日，领域:${communityInfo.name}",
            GenerateContentConfig.builder().tools(Tool.builder().googleSearch(GoogleSearch.builder()))
                .build(),
        )
    val content = response.text()?.take(1000) ?: "😴"
    sessionManager.createTopic(ObjectType.COMMUNITY, communityInfo.id, content).onSuccess {
        Napier.i {
            "create new topic success $date $content"
        }
    }.onFailure {
        Napier.e(it.cause) {
            "create new topic failed $date $content"
        }
    }
    delay(1.seconds)
}
