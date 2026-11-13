/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.storyteller_f.alphaheart.app.common.createCommunityViewModel
import com.storyteller_f.alphaheart.app.common.createRoomViewModel
import com.storyteller_f.alphaheart.app.common.createTopicViewModel
import com.storyteller_f.alphaheart.app.common.createUserViewModel
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.type.PrimaryKey

interface RefCellHandlerProvider {
    @Composable
    fun topicHandler(topicId: PrimaryKey): LoadingHandler<TopicDetail>

    @Composable
    fun topicHandler(topicAid: String): LoadingHandler<TopicDetail>

    @Composable
    fun roomHandler(roomId: PrimaryKey): LoadingHandler<RoomInfo>

    @Composable
    fun roomHandler(roomAid: String): LoadingHandler<RoomInfo>

    @Composable
    fun communityHandler(communityId: PrimaryKey): LoadingHandler<CommunityInfo>

    @Composable
    fun communityHandler(communityAid: String): LoadingHandler<CommunityInfo>

    @Composable
    fun userHandler(userId: PrimaryKey): LoadingHandler<UserInfo>

    @Composable
    fun userHandler(userAid: String): LoadingHandler<UserInfo>
}

object DefaultRefCellHandlerProvider : RefCellHandlerProvider {
    @Composable
    override fun topicHandler(topicId: PrimaryKey): LoadingHandler<TopicDetail> = createTopicViewModel(topicId).handler

    @Composable
    override fun topicHandler(topicAid: String): LoadingHandler<TopicDetail> = createTopicViewModel(topicAid).handler

    @Composable
    override fun roomHandler(roomId: PrimaryKey): LoadingHandler<RoomInfo> = createRoomViewModel(roomId).handler

    @Composable
    override fun roomHandler(roomAid: String): LoadingHandler<RoomInfo> = createRoomViewModel(roomAid).handler

    @Composable
    override fun communityHandler(communityId: PrimaryKey): LoadingHandler<CommunityInfo> =
        createCommunityViewModel(communityId).handler

    @Composable
    override fun communityHandler(communityAid: String): LoadingHandler<CommunityInfo> =
        createCommunityViewModel(communityAid).handler

    @Composable
    override fun userHandler(userId: PrimaryKey): LoadingHandler<UserInfo> = createUserViewModel(userId).handler

    @Composable
    override fun userHandler(userAid: String): LoadingHandler<UserInfo> = createUserViewModel(userAid).handler
}

val LocalRefCellHandlerProvider =
    compositionLocalOf<RefCellHandlerProvider> {
        error("LocalRefCellHandlerProvider must be provided")
    }
