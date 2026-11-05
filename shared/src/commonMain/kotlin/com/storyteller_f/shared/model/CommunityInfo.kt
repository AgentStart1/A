/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.shared.model

import com.storyteller_f.shared.type.DEFAULT_PRIMARY_KEY
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.now
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class FontSettingsWithInfo(
    val settings: FontSettings,
    val contentFont: FileInfo? = null,
    val codeFont: FileInfo? = null,
)

@Serializable
data class CommunityInfo(
    override val id: PrimaryKey,
    val aid: String,
    val name: String,
    val owner: PrimaryKey,
    val createdTime: LocalDateTime,
    val memberCount: Long,
    val memberPolicy: MemberPolicy,
    val icon: FileInfo? = null,
    val poster: FileInfo? = null,
    val member: NestedMemberInfo? = null,
    val lastRead: PrimaryKey? = null,
    val latestTopic: PrimaryKey? = null,
    val hasPoster: Boolean = poster != null,
    val fontSettings: FontSettingsWithInfo? = null,
    val favoriteId: PrimaryKey? = null,
    val subscriptionId: PrimaryKey? = null,
    val status: ObjectStatus = ObjectStatus.NORMAL,
) : ModelObject {
    val readOnly get() = status == ObjectStatus.READ_ONLY
    val isJoined = member != null
    override val objectType: ObjectType
        get() = ObjectType.COMMUNITY

    companion object {
        val EMPTY = CommunityInfo(DEFAULT_PRIMARY_KEY, "", "", DEFAULT_PRIMARY_KEY, now(), 0, MemberPolicy.OPEN)
    }
}

enum class PosterSearch {
    HAS_POSTER,
    NO_POSTER,
    UNSPECIFIED,
}

enum class MemberPolicy {
    OPEN,
    INVITE_ONLY,
}
