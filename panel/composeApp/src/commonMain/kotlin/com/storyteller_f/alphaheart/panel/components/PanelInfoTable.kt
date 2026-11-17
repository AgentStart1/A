/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.storyteller_f.alphaheart.client.compose_core.components.InfoTable
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.review_field_acg
import com.storyteller_f.alphaheart.panel.review_field_address
import com.storyteller_f.alphaheart.panel.review_field_aid
import com.storyteller_f.alphaheart.panel.review_field_author
import com.storyteller_f.alphaheart.panel.review_field_child_account_count
import com.storyteller_f.alphaheart.panel.review_field_comment_count
import com.storyteller_f.alphaheart.panel.review_field_community_id
import com.storyteller_f.alphaheart.panel.review_field_content_type
import com.storyteller_f.alphaheart.panel.review_field_created_time
import com.storyteller_f.alphaheart.panel.review_field_creator
import com.storyteller_f.alphaheart.panel.review_field_description_topic_id
import com.storyteller_f.alphaheart.panel.review_field_dimension
import com.storyteller_f.alphaheart.panel.review_field_favorite_count
import com.storyteller_f.alphaheart.panel.review_field_font_settings
import com.storyteller_f.alphaheart.panel.review_field_full_name
import com.storyteller_f.alphaheart.panel.review_field_has_comment
import com.storyteller_f.alphaheart.panel.review_field_has_poster
import com.storyteller_f.alphaheart.panel.review_field_has_unread
import com.storyteller_f.alphaheart.panel.review_field_icon
import com.storyteller_f.alphaheart.panel.review_field_id
import com.storyteller_f.alphaheart.panel.review_field_is_encrypted
import com.storyteller_f.alphaheart.panel.review_field_is_joined
import com.storyteller_f.alphaheart.panel.review_field_is_pin
import com.storyteller_f.alphaheart.panel.review_field_is_private
import com.storyteller_f.alphaheart.panel.review_field_joined_time
import com.storyteller_f.alphaheart.panel.review_field_last_modified
import com.storyteller_f.alphaheart.panel.review_field_last_modified_time
import com.storyteller_f.alphaheart.panel.review_field_latest_topic
import com.storyteller_f.alphaheart.panel.review_field_level
import com.storyteller_f.alphaheart.panel.review_field_member_count
import com.storyteller_f.alphaheart.panel.review_field_member_policy
import com.storyteller_f.alphaheart.panel.review_field_name
import com.storyteller_f.alphaheart.panel.review_field_nickname
import com.storyteller_f.alphaheart.panel.review_field_owner
import com.storyteller_f.alphaheart.panel.review_field_owner_type
import com.storyteller_f.alphaheart.panel.review_field_parent_id
import com.storyteller_f.alphaheart.panel.review_field_parent_type
import com.storyteller_f.alphaheart.panel.review_field_poster
import com.storyteller_f.alphaheart.panel.review_field_reaction_count
import com.storyteller_f.alphaheart.panel.review_field_reaction_record_count
import com.storyteller_f.alphaheart.panel.review_field_read_only
import com.storyteller_f.alphaheart.panel.review_field_receiver
import com.storyteller_f.alphaheart.panel.review_field_root_id
import com.storyteller_f.alphaheart.panel.review_field_root_type
import com.storyteller_f.alphaheart.panel.review_field_scope_id
import com.storyteller_f.alphaheart.panel.review_field_scope_type
import com.storyteller_f.alphaheart.panel.review_field_size
import com.storyteller_f.alphaheart.panel.review_field_status
import com.storyteller_f.alphaheart.panel.review_field_subscription_count
import com.storyteller_f.alphaheart.panel.review_field_type
import com.storyteller_f.alphaheart.panel.review_field_url
import com.storyteller_f.alphaheart.panel.review_member_invited
import com.storyteller_f.alphaheart.panel.review_member_joined
import com.storyteller_f.alphaheart.panel.review_member_requested
import com.storyteller_f.alphaheart.panel.review_no
import com.storyteller_f.alphaheart.panel.review_not_set
import com.storyteller_f.alphaheart.panel.review_object_community
import com.storyteller_f.alphaheart.panel.review_object_file
import com.storyteller_f.alphaheart.panel.review_object_panel_account
import com.storyteller_f.alphaheart.panel.review_object_room
import com.storyteller_f.alphaheart.panel.review_object_title
import com.storyteller_f.alphaheart.panel.review_object_topic
import com.storyteller_f.alphaheart.panel.review_object_user
import com.storyteller_f.alphaheart.panel.review_policy_approval
import com.storyteller_f.alphaheart.panel.review_policy_invitation
import com.storyteller_f.alphaheart.panel.review_policy_public
import com.storyteller_f.alphaheart.panel.review_status_deleted
import com.storyteller_f.alphaheart.panel.review_status_normal
import com.storyteller_f.alphaheart.panel.review_status_read_only
import com.storyteller_f.alphaheart.panel.review_title_join
import com.storyteller_f.alphaheart.panel.review_title_regular
import com.storyteller_f.alphaheart.panel.review_unknown
import com.storyteller_f.alphaheart.panel.review_upload_aborted
import com.storyteller_f.alphaheart.panel.review_upload_failed
import com.storyteller_f.alphaheart.panel.review_upload_pending
import com.storyteller_f.alphaheart.panel.review_upload_success
import com.storyteller_f.alphaheart.panel.review_yes
import org.jetbrains.compose.resources.stringResource

private val panelFieldLabels =
    mapOf(
        "acg" to Res.string.review_field_acg,
        "address" to Res.string.review_field_address,
        "aid" to Res.string.review_field_aid,
        "author" to Res.string.review_field_author,
        "childAccountCount" to Res.string.review_field_child_account_count,
        "commentCount" to Res.string.review_field_comment_count,
        "communityId" to Res.string.review_field_community_id,
        "contentType" to Res.string.review_field_content_type,
        "createdTime" to Res.string.review_field_created_time,
        "creator" to Res.string.review_field_creator,
        "descriptionTopicId" to Res.string.review_field_description_topic_id,
        "dimension" to Res.string.review_field_dimension,
        "favoriteCount" to Res.string.review_field_favorite_count,
        "fontSettings" to Res.string.review_field_font_settings,
        "fullName" to Res.string.review_field_full_name,
        "hasComment" to Res.string.review_field_has_comment,
        "hasPoster" to Res.string.review_field_has_poster,
        "hasUnread" to Res.string.review_field_has_unread,
        "icon" to Res.string.review_field_icon,
        "id" to Res.string.review_field_id,
        "isEncrypted" to Res.string.review_field_is_encrypted,
        "isJoined" to Res.string.review_field_is_joined,
        "isPin" to Res.string.review_field_is_pin,
        "isPrivate" to Res.string.review_field_is_private,
        "joinedTime" to Res.string.review_field_joined_time,
        "lastModified" to Res.string.review_field_last_modified,
        "lastModifiedTime" to Res.string.review_field_last_modified_time,
        "latestTopic" to Res.string.review_field_latest_topic,
        "level" to Res.string.review_field_level,
        "memberCount" to Res.string.review_field_member_count,
        "memberPolicy" to Res.string.review_field_member_policy,
        "name" to Res.string.review_field_name,
        "nickname" to Res.string.review_field_nickname,
        "owner" to Res.string.review_field_owner,
        "ownerType" to Res.string.review_field_owner_type,
        "parentId" to Res.string.review_field_parent_id,
        "parentType" to Res.string.review_field_parent_type,
        "poster" to Res.string.review_field_poster,
        "reactionCount" to Res.string.review_field_reaction_count,
        "reactionRecordCount" to Res.string.review_field_reaction_record_count,
        "readOnly" to Res.string.review_field_read_only,
        "receiver" to Res.string.review_field_receiver,
        "rootId" to Res.string.review_field_root_id,
        "rootType" to Res.string.review_field_root_type,
        "scopeId" to Res.string.review_field_scope_id,
        "scopeType" to Res.string.review_field_scope_type,
        "size" to Res.string.review_field_size,
        "status" to Res.string.review_field_status,
        "subscriptionCount" to Res.string.review_field_subscription_count,
        "type" to Res.string.review_field_type,
        "url" to Res.string.review_field_url,
    )

private val panelValueLabels =
    mapOf(
        "true" to Res.string.review_yes,
        "false" to Res.string.review_no,
        "null" to Res.string.review_not_set,
        "Unknown" to Res.string.review_unknown,
        "USER" to Res.string.review_object_user,
        "COMMUNITY" to Res.string.review_object_community,
        "ROOM" to Res.string.review_object_room,
        "TOPIC" to Res.string.review_object_topic,
        "FILE" to Res.string.review_object_file,
        "TITLE" to Res.string.review_object_title,
        "PANEL_ACCOUNT" to Res.string.review_object_panel_account,
        "NORMAL" to Res.string.review_status_normal,
        "READ_ONLY" to Res.string.review_status_read_only,
        "DELETED" to Res.string.review_status_deleted,
        "JOINED" to Res.string.review_member_joined,
        "INVITED" to Res.string.review_member_invited,
        "REQUESTED" to Res.string.review_member_requested,
        "OPEN" to Res.string.review_policy_public,
        "APPROVAL" to Res.string.review_policy_approval,
        "INVITE_ONLY" to Res.string.review_policy_invitation,
        "REGULAR" to Res.string.review_title_regular,
        "JOIN" to Res.string.review_title_join,
        "PENDING" to Res.string.review_upload_pending,
        "SUCCESS" to Res.string.review_upload_success,
        "FAILED" to Res.string.review_upload_failed,
        "ABORTED" to Res.string.review_upload_aborted,
    )

/** Localizes presentation labels while preserving stored identifiers and full-value inspection. */
@Composable
internal fun PanelInfoTable(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    InfoTable(
        items.map { (key, value) ->
            val label = panelFieldLabels[key]?.let { stringResource(it) } ?: key
            val display = if (key in panelValueFields) panelValueText(value) else value
            label to display
        },
        modifier,
    )
}

private val panelValueFields =
    setOf(
        "status", "type", "ownerType", "scopeType", "rootType", "parentType", "memberPolicy",
        "readOnly", "hasComment", "hasPoster", "hasUnread", "isEncrypted", "isJoined", "isPin", "isPrivate",
        "icon", "poster", "latestTopic", "joinedTime", "lastModifiedTime", "dimension", "fontSettings",
    )

@Composable
internal fun panelValueText(value: String): String = panelValueLabels[value]?.let { stringResource(it) } ?: value

/** Only an absent value is replaced; literal identifiers and diagnostic text remain intact. */
@Composable
internal fun panelOptionalValueText(value: String?): String = value ?: stringResource(Res.string.review_not_set)
