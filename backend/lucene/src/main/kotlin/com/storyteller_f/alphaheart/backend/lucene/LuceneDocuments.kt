/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.lucene

import com.storyteller_f.alphaheart.backend.core.service.CommunityDocument
import com.storyteller_f.alphaheart.backend.core.service.FileDocument
import com.storyteller_f.alphaheart.backend.core.service.MemberDocument
import com.storyteller_f.alphaheart.backend.core.service.RoomDocument
import com.storyteller_f.alphaheart.backend.core.service.TopicDocument
import com.storyteller_f.alphaheart.backend.core.service.UserDocument
import com.storyteller_f.services.lucene.api.RpcLuceneDocValues
import com.storyteller_f.services.lucene.api.RpcLuceneDocument
import com.storyteller_f.services.lucene.api.RpcLuceneField
import com.storyteller_f.services.lucene.api.RpcLuceneIndex
import com.storyteller_f.services.lucene.api.RpcLuceneStoredDocument
import com.storyteller_f.services.lucene.api.RpcLuceneValue
import com.storyteller_f.shared.type.ObjectType

internal const val DOCUMENT_ID_FIELD = "id"
internal const val FILE_NAME_EXACT_FIELD = "nameExact"

private fun buildFields(
    id: Long,
    text: Map<String, String> = emptyMap(),
    keywords: Map<String, String> = emptyMap(),
    numbers: Map<String, Long> = emptyMap(),
): List<RpcLuceneField> =
    listOf(
        RpcLuceneField(
            DOCUMENT_ID_FIELD,
            RpcLuceneValue.LongNumber(id),
            RpcLuceneIndex.EXACT,
            docValues = RpcLuceneDocValues.NUMERIC,
        ),
    ) +
        text.map { (name, value) -> RpcLuceneField(name, RpcLuceneValue.Text(value), RpcLuceneIndex.TEXT) } +
        keywords.map { (name, value) -> RpcLuceneField(name, RpcLuceneValue.Text(value), RpcLuceneIndex.EXACT) } +
        numbers.map { (name, value) -> RpcLuceneField(name, RpcLuceneValue.LongNumber(value), RpcLuceneIndex.EXACT) }

internal interface LuceneDocument {
    fun save(): RpcLuceneDocument
}

internal interface LuceneDocumentCompanion<T> {
    fun restore(document: RpcLuceneStoredDocument): T
}

private fun RpcLuceneStoredDocument.text(name: String) = (fields.getValue(name).single() as RpcLuceneValue.Text).value
private fun RpcLuceneStoredDocument.number(name: String) =
    (
        fields.getValue(
            name,
        ).single() as RpcLuceneValue.LongNumber
        ).value
private fun RpcLuceneStoredDocument.optionalText(name: String) =
    fields[name]?.single()?.let { (it as RpcLuceneValue.Text).value }
private fun RpcLuceneStoredDocument.optionalNumber(name: String) =
    fields[name]?.single()?.let { (it as RpcLuceneValue.LongNumber).value }

internal data class LuceneTopicDocument(val topicDocument: TopicDocument) : LuceneDocument {
    override fun save(): RpcLuceneDocument =
        with(topicDocument) {
            RpcLuceneDocument(
                buildFields(
                    id,
                    text = mapOf("content" to content),
                    keywords = mapOf("rootType" to rootType, "parentType" to parentType),
                    numbers = mapOf("rootId" to rootId, "parentId" to parentId, "author" to author),
                ),
            )
        }

    companion object : LuceneDocumentCompanion<TopicDocument> {
        override fun restore(document: RpcLuceneStoredDocument): TopicDocument =
            with(document) {
                TopicDocument(
                    number(DOCUMENT_ID_FIELD),
                    text("content"),
                    number("rootId"),
                    text("rootType"),
                    number("parentId"),
                    text("parentType"),
                    number("author"),
                )
            }
    }
}

internal data class LuceneUserDocument(val userDocument: UserDocument) : LuceneDocument {
    override fun save(): RpcLuceneDocument =
        with(userDocument) {
            RpcLuceneDocument(
                buildFields(id, text = mapOf("nickname" to nickname) + aid?.let { mapOf("aid" to it) }.orEmpty()),
            )
        }

    companion object : LuceneDocumentCompanion<UserDocument> {
        override fun restore(document: RpcLuceneStoredDocument): UserDocument =
            with(document) {
                UserDocument(number(DOCUMENT_ID_FIELD), text("nickname"), optionalText("aid"))
            }
    }
}

internal data class LuceneRoomDocument(val roomDocument: RoomDocument) : LuceneDocument {
    override fun save(): RpcLuceneDocument =
        with(roomDocument) {
            RpcLuceneDocument(
                buildFields(
                    id,
                    text = mapOf("name" to name, "aid" to aid),
                    numbers = communityId?.let { mapOf("communityId" to it) }.orEmpty(),
                ),
            )
        }

    companion object : LuceneDocumentCompanion<RoomDocument> {
        override fun restore(document: RpcLuceneStoredDocument): RoomDocument =
            with(document) {
                RoomDocument(number(DOCUMENT_ID_FIELD), text("name"), text("aid"), optionalNumber("communityId"))
            }
    }
}

internal data class LuceneCommunityDocument(val communityDocument: CommunityDocument) : LuceneDocument {
    override fun save(): RpcLuceneDocument =
        with(communityDocument) {
            RpcLuceneDocument(
                buildFields(id, text = mapOf("name" to name, "aid" to aid), numbers = mapOf("owner" to owner)),
            )
        }

    companion object : LuceneDocumentCompanion<CommunityDocument> {
        override fun restore(document: RpcLuceneStoredDocument): CommunityDocument =
            with(document) {
                CommunityDocument(number(DOCUMENT_ID_FIELD), text("name"), text("aid"), number("owner"))
            }
    }
}

internal data class LuceneMemberDocument(val memberDocument: MemberDocument) : LuceneDocument {
    override fun save(): RpcLuceneDocument =
        with(memberDocument) {
            RpcLuceneDocument(
                buildFields(
                    id,
                    text = mapOf("nickname" to nickname, "objectName" to objectName),
                    keywords = mapOf("objectType" to objectType.name),
                    numbers =
                    mapOf(
                        "uid" to uid,
                        "objectId" to objectId,
                    ) + communityId?.let { mapOf("communityId" to it) }.orEmpty(),
                ),
            )
        }

    companion object : LuceneDocumentCompanion<MemberDocument> {
        override fun restore(document: RpcLuceneStoredDocument): MemberDocument =
            with(document) {
                MemberDocument(
                    number(DOCUMENT_ID_FIELD),
                    number("uid"),
                    number("objectId"),
                    ObjectType.valueOf(text("objectType")),
                    text("nickname"),
                    text("objectName"),
                    optionalNumber("communityId"),
                )
            }
    }
}

internal data class LuceneFileDocument(val fileDocument: FileDocument) : LuceneDocument {
    override fun save(): RpcLuceneDocument =
        with(fileDocument) {
            RpcLuceneDocument(
                buildFields(id, text = mapOf("name" to name), numbers = mapOf("ownerId" to ownerId)) +
                    RpcLuceneField(
                        FILE_NAME_EXACT_FIELD,
                        RpcLuceneValue.Text(name.lowercase()),
                        RpcLuceneIndex.EXACT,
                        stored = false,
                    ),
            )
        }

    companion object : LuceneDocumentCompanion<FileDocument> {
        override fun restore(document: RpcLuceneStoredDocument): FileDocument =
            with(document) {
                FileDocument(number(DOCUMENT_ID_FIELD), text("name"), number("ownerId"))
            }
    }
}
