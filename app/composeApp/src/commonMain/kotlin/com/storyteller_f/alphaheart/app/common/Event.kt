/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.PosterSearch
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TitleDetail
import com.storyteller_f.shared.model.TopicCreated
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.toInitialDetail
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.storage.CommunityCollection
import com.storyteller_f.storage.FileCollection
import com.storyteller_f.storage.ModelStorage
import com.storyteller_f.storage.RoomCollection
import com.storyteller_f.storage.TitleCollection
import com.storyteller_f.storage.TopicCollection
import com.storyteller_f.storage.UserCollection
import com.storyteller_f.storage.update
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableSharedFlow

data class OnTopicPinChanged(
    val topicId: PrimaryKey,
    val parentId: PrimaryKey,
    val rootType: ObjectType,
    val isPin: Boolean,
)
data class OnTopicCreated(val topicInfo: TopicCreated)

data class OnMediaUploaded(val fileInfos: List<FileInfo>)

data class OnUserUpdated(val info: UserInfo)

data class OnTitleCreated(val title: TitleDetail)

data class OnCommunityCreated(val info: CommunityInfo)
data class OnCommunityJoined(val info: CommunityInfo)
data class OnCommunityExited(val info: CommunityInfo)

data class OnCommunityUpdated(val info: CommunityInfo)
data class OnRoomRead(val roomId: com.storyteller_f.shared.type.PrimaryKey)

data object OnRoomActivityRefreshRequested

data class OnRoomCreated(val info: RoomInfo)
data class OnRoomJoined(val info: RoomInfo)
data class OnRoomExited(val info: RoomInfo)

data class OnRoomUpdated(val info: RoomInfo)
data class OnAddReaction(val info: ReactionInfo, val topicInfo: TopicDetail)
data class OnRemoveReaction(val info: ReactionInfo, val topicInfo: TopicDetail)

data class OnAddFavorite(val objectTuple: ObjectTuple)
data class OnRemoveFavorite(val objectTuple: ObjectTuple)

data class OnAddSubscription(val objectTuple: ObjectTuple)
data class OnRemoveSubscription(val objectTuple: ObjectTuple)

suspend fun processEvent(database: ModelStorage, bus: MutableSharedFlow<Any>) {
    bus.collect { event ->
        when (event) {
            is OnAddReaction -> processOnAddReaction(database, event)

            is OnRemoveReaction -> processRemoveReaction(database, event)

            is OnCommunityCreated -> processCommunityCreated(event, database)

            is OnCommunityJoined -> database.community.saveToDefault(event.info)

            is OnCommunityExited ->
                database.community
                    .saveToDefault(event.info)

            is OnCommunityUpdated ->
                database.community
                    .saveToDefault(event.info)

            is OnTopicPinChanged -> processTopicPinChanged(event, database)

            is OnTopicCreated -> processTopicCreated(event, database)

            is OnRoomJoined -> database.room.saveToDefault(event.info)

            is OnRoomExited -> database.room.saveToDefault(event.info)

            is OnRoomUpdated -> processRoomUpdated(event, database)

            is OnRoomCreated -> processRoomCreated(event, database)

            is OnUserUpdated -> database.user.saveToDefault(event.info)

            is OnMediaUploaded -> processOnMediaUploaded(event, database)

            is OnAddFavorite -> processOnAddFavorite(event, database)

            is OnRemoveFavorite -> processOnRemoveFavorite(event, database)

            is OnAddSubscription -> processOnAddSubscription(event, database)

            is OnRemoveSubscription -> processOnRemoveSubscriptionEvent(event, database)
        }
    }
}

private suspend fun processCommunityCreated(event: OnCommunityCreated, database: ModelStorage) {
    database.community.saveToDefault(event.info)
    database.community.saveFirst(
        CommunityCollection.SearchCommunity(JoinStatusSearch.JOINED, "", null, PosterSearch.UNSPECIFIED),
        event.info,
    )
}

private suspend fun processOnMediaUploaded(event: OnMediaUploaded, database: ModelStorage) {
    event.fileInfos.forEach {
        database.fileInfo.saveToDefault(it)
        // 同时保存到对应的 FileList 集合中
        database.fileInfo.saveFirst(FileCollection.FileList(it.owner), it)
    }
}

private suspend fun processOnAddFavorite(event: OnAddFavorite, database: ModelStorage) {
    val objectId = event.objectTuple.objectId
    when (event.objectTuple.objectType) {
        ObjectType.TOPIC ->
            database.topic.update(TopicCollection.Topics, objectId) {
                it.copy(topicInfo = it.topicInfo.copy(favoriteId = -1))
            }

        ObjectType.ROOM ->
            database.room.update(RoomCollection.Rooms, objectId) {
                it.copy(favoriteId = -1)
            }

        ObjectType.COMMUNITY ->
            database.community.update(CommunityCollection.Communities, objectId) {
                it.copy(favoriteId = -1)
            }

        ObjectType.TITLE ->
            database.title.update(TitleCollection.Titles, objectId) {
                it.copy(titleInfo = it.titleInfo.copy(favoriteId = -1))
            }

        ObjectType.FILE ->
            database.fileInfo.update(FileCollection.Files, objectId) {
                it.copy(favoriteId = -1)
            }

        ObjectType.USER ->
            database.user.update(UserCollection.Users, objectId) {
                it.copy(favoriteId = -1)
            }

        else -> Unit
    }
}

private suspend fun processOnRemoveFavorite(event: OnRemoveFavorite, database: ModelStorage) {
    val objectId = event.objectTuple.objectId
    when (event.objectTuple.objectType) {
        ObjectType.TOPIC ->
            database.topic.update(TopicCollection.Topics, objectId) {
                it.copy(topicInfo = it.topicInfo.copy(favoriteId = null))
            }

        ObjectType.ROOM ->
            database.room.update(RoomCollection.Rooms, objectId) {
                it.copy(favoriteId = null)
            }

        ObjectType.COMMUNITY ->
            database.community.update(CommunityCollection.Communities, objectId) {
                it.copy(favoriteId = null)
            }

        ObjectType.TITLE ->
            database.title.update(TitleCollection.Titles, objectId) {
                it.copy(titleInfo = it.titleInfo.copy(favoriteId = null))
            }

        ObjectType.FILE ->
            database.fileInfo.update(FileCollection.Files, objectId) {
                it.copy(favoriteId = null)
            }

        ObjectType.USER ->
            database.user.update(UserCollection.Users, objectId) {
                it.copy(favoriteId = null)
            }

        else -> Unit
    }
}

private suspend fun processOnAddSubscription(event: OnAddSubscription, database: ModelStorage) {
    val objectId = event.objectTuple.objectId
    when (event.objectTuple.objectType) {
        ObjectType.TOPIC ->
            database.topic.update(TopicCollection.Topics, objectId) {
                it.copy(topicInfo = it.topicInfo.copy(subscriptionId = -1))
            }

        ObjectType.ROOM ->
            database.room.update(RoomCollection.Rooms, objectId) {
                it.copy(subscriptionId = -1)
            }

        ObjectType.COMMUNITY ->
            database.community.update(CommunityCollection.Communities, objectId) {
                it.copy(subscriptionId = -1)
            }

        ObjectType.TITLE ->
            database.title.update(TitleCollection.Titles, objectId) {
                it.copy(titleInfo = it.titleInfo.copy(subscriptionId = -1))
            }

        ObjectType.FILE ->
            database.fileInfo.update(FileCollection.Files, objectId) {
                it.copy(subscriptionId = -1)
            }

        ObjectType.USER ->
            database.user.update(UserCollection.Users, objectId) {
                it.copy(subscriptionId = -1)
            }

        else -> Unit
    }
}

private suspend fun processOnRemoveSubscriptionEvent(event: OnRemoveSubscription, database: ModelStorage) {
    val objectId = event.objectTuple.objectId
    when (event.objectTuple.objectType) {
        ObjectType.TOPIC ->
            database.topic.update(TopicCollection.Topics, objectId) {
                it.copy(topicInfo = it.topicInfo.copy(subscriptionId = null))
            }

        ObjectType.ROOM ->
            database.room.update(RoomCollection.Rooms, objectId) {
                it.copy(subscriptionId = null)
            }

        ObjectType.COMMUNITY ->
            database.community.update(CommunityCollection.Communities, objectId) {
                it.copy(subscriptionId = null)
            }

        ObjectType.TITLE ->
            database.title.update(TitleCollection.Titles, objectId) {
                it.copy(titleInfo = it.titleInfo.copy(subscriptionId = null))
            }

        ObjectType.FILE ->
            database.fileInfo.update(FileCollection.Files, objectId) {
                it.copy(subscriptionId = null)
            }

        ObjectType.USER ->
            database.user.update(UserCollection.Users, objectId) {
                it.copy(subscriptionId = null)
            }

        else -> Unit
    }
}

private suspend fun processTopicCreated(event: OnTopicCreated, database: ModelStorage) {
    val topicInfo =
        database.topic.getDocument(TopicCollection.Topics, event.topicInfo.id)
            ?: event.topicInfo.toInitialDetail()
    database.topic.saveToDefault(topicInfo)
    database.topic.saveFirst(TopicCollection.ChildTopicList(topicInfo.parentId), topicInfo)
}

private suspend fun processTopicPinChanged(event: OnTopicPinChanged, database: ModelStorage) {
    database.topic.getDocument(TopicCollection.Topics, event.topicId)?.let { cached ->
        database.topic.saveToDefault(cached.copy(topicInfo = cached.topicInfo.copy(isPin = event.isPin)))
    }
    listOf(TopicCollection.Recommend, TopicCollection.ChildTopicList(event.parentId)).forEach { collection ->
        database.topic.update(collection, event.topicId) { cached ->
            cached.copy(topicInfo = cached.topicInfo.copy(isPin = event.isPin))
        }
    }
}

private suspend fun processRoomUpdated(event: OnRoomUpdated, database: ModelStorage) {
    val info = event.info
    database.room.saveToDefault(info)
    val communityId = info.communityId
    if (communityId != null) {
        val collection = RoomCollection.CommunityRooms(communityId)
        if (database.room.getDocument(collection, info.id) != null) {
            database.room.updateDocument(collection, info)
        }
    }
}

private suspend fun processRoomCreated(event: OnRoomCreated, database: ModelStorage) {
    val info = event.info
    database.room.saveToDefault(info)
    val communityId = info.communityId
    if (communityId != null) {
        database.room.saveFirst(RoomCollection.CommunityRooms(communityId), info)
    }
}

private suspend fun processRemoveReaction(database: ModelStorage, event: OnRemoveReaction) {
    listOf(
        TopicCollection.Topics,
        TopicCollection.Recommend,
        TopicCollection.ChildTopicList(event.topicInfo.parentId),
    ).forEach { collectionName ->
        database.topic.update(collectionName, event.topicInfo.id) { old ->
            val newReactions =
                old.reactions.map { info ->
                    if (info.emoji == event.info.emoji) {
                        event.info
                    } else {
                        info
                    }
                }.filter {
                    it.count > 0
                }.toImmutableList()
            old.copy(reactions = newReactions, topicInfo = old.topicInfo.copy(reactionCount = old.reactionCount - 1))
        }
    }
}

private suspend fun processOnAddReaction(database: ModelStorage, event: OnAddReaction) {
    listOf(
        TopicCollection.Topics,
        TopicCollection.Recommend,
        TopicCollection.ChildTopicList(event.topicInfo.parentId),
    ).forEach { collectionName ->
        database.topic.update(collectionName, event.info.objectId) { old ->
            val oldReactions = old.reactions
            val existing =
                oldReactions.firstOrNull {
                    it.emoji == event.info.emoji
                }
            val newReactions =
                if (existing == null) {
                    val info = event.info
                    oldReactions.toPersistentList().adding(info)
                } else {
                    oldReactions.map { info ->
                        if (info.emoji == event.info.emoji) {
                            event.info
                        } else {
                            info
                        }
                    }.toImmutableList()
                }
            old.copy(reactions = newReactions, topicInfo = old.topicInfo.copy(reactionCount = old.reactionCount + 1))
        }
    }
}
