/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.api.NewTitle
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.addFavorite
import com.storyteller_f.alphaheart.client.core.addSubscription
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createTitle
import com.storyteller_f.alphaheart.client.core.getFavorites
import com.storyteller_f.alphaheart.client.core.getSubscriptions
import com.storyteller_f.alphaheart.client.core.removeFavorite
import com.storyteller_f.alphaheart.client.core.removeSubscription
import com.storyteller_f.alphaheart.client.core.userTitles
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TitleWorkStatus
import com.storyteller_f.shared.type.ObjectType
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TitleTest {
    @Test
    fun `test title`() =
        test {
            attachSession { session ->
                val c = createCommunity(NewCommunity("c1", "c1")).getOrThrow()
                val cId = c.id
                assertListSize(0, userTitles(session.uid, 10, TitleSearchType.RECEIVER))
                createTitle(
                    NewTitle("c KOL", TitleType.REGULAR, session.uid, cId, ObjectType.COMMUNITY, "hello"),
                ).getOrThrow()
                assertListTotalSize(1, userTitles(session.uid, 10, TitleSearchType.RECEIVER))
                assertListSize(1, userTitles(session.uid, 10, TitleSearchType.RECEIVER))
                assertListSize(1, userTitles(session.uid, 10, TitleSearchType.CREATOR))
                assertListSize(1, userTitles(session.uid, 10, TitleSearchType.CREATOR, scopeId = cId))
                assertListSize(
                    1,
                    userTitles(session.uid, 10, TitleSearchType.CREATOR, type = TitleType.REGULAR, scopeId = cId),
                )
            }
        }

    @Test
    fun `test title expired filter`() =
        test {
            attachSession { session ->
                val c = createCommunity(NewCommunity("c-expired", "c-expired")).getOrThrow()
                val cId = c.id
                createTitle(
                    NewTitle(
                        "active-title",
                        TitleType.REGULAR,
                        session.uid,
                        cId,
                        ObjectType.COMMUNITY,
                        "active",
                        LocalDateTime.parse("2099-01-01T00:00:00"),
                    ),
                ).getOrThrow()
                createTitle(
                    NewTitle(
                        "expired-title",
                        TitleType.REGULAR,
                        session.uid,
                        cId,
                        ObjectType.COMMUNITY,
                        "expired",
                        LocalDateTime.parse("2000-01-01T00:00:00"),
                    ),
                ).getOrThrow()

                val okTitles =
                    userTitles(session.uid, 10, TitleSearchType.RECEIVER, status = TitleWorkStatus.OK)
                        .getOrThrow().data
                assertTrue(
                    okTitles.any { title ->
                        title.name == "active-title" && title.titleStatus == TitleWorkStatus.OK
                    },
                )
                assertTrue(okTitles.none { title -> title.name == "expired-title" })

                val expiredTitles =
                    userTitles(session.uid, 10, TitleSearchType.RECEIVER, status = TitleWorkStatus.EXPIRED)
                        .getOrThrow().data
                assertTrue(
                    expiredTitles.any { title ->
                        title.name == "expired-title" && title.titleStatus == TitleWorkStatus.EXPIRED
                    },
                )
            }
        }

    @Test
    fun `test add title favorite`() =
        test {
            attachSession { session ->
                val c = createCommunity(NewCommunity("fav1", "fav1")).getOrThrow()
                val cId = c.id
                createTitle(
                    NewTitle("c KOL fav", TitleType.REGULAR, session.uid, cId, ObjectType.COMMUNITY, "hello fav"),
                ).getOrThrow()

                val titleId =
                    userTitles(
                        session.uid,
                        10,
                        TitleSearchType.RECEIVER,
                    ).getOrThrow().data.first { t -> t.name == "c KOL fav" }.id

                addFavorite(NewFavorite(ObjectType.TITLE, titleId)).getOrThrow()
                assertListTotalSize(1, getFavorites(PaginationQuery()))

                val titleInfo =
                    userTitles(
                        session.uid,
                        10,
                        TitleSearchType.RECEIVER,
                    ).getOrThrow().data.first { t -> t.id == titleId }
                assertNotNull(titleInfo.favoriteId)
            }
        }

    @Test
    fun `test remove title favorite`() =
        test {
            attachSession { session ->
                val c = createCommunity(NewCommunity("fav1", "fav1")).getOrThrow()
                val cId = c.id
                createTitle(
                    NewTitle("c KOL fav", TitleType.REGULAR, session.uid, cId, ObjectType.COMMUNITY, "hello fav"),
                ).getOrThrow()

                val titleId =
                    userTitles(
                        session.uid,
                        10,
                        TitleSearchType.RECEIVER,
                    ).getOrThrow().data.first { t -> t.name == "c KOL fav" }.id

                addFavorite(NewFavorite(ObjectType.TITLE, titleId)).getOrThrow()

                removeFavorite(titleId, ObjectType.TITLE).getOrThrow()
                assertListTotalSize(0, getFavorites(PaginationQuery()))
            }
        }

    @Test
    fun `test add title subscription`() =
        test {
            attachSession { session ->
                val c = createCommunity(NewCommunity("sub1", "sub1")).getOrThrow()
                val cId = c.id
                createTitle(
                    NewTitle("c KOL sub", TitleType.REGULAR, session.uid, cId, ObjectType.COMMUNITY, "hello sub"),
                ).getOrThrow()

                val titleId =
                    userTitles(
                        session.uid,
                        10,
                        TitleSearchType.RECEIVER,
                    ).getOrThrow().data.first { t -> t.name == "c KOL sub" }.id

                addSubscription(NewSubscription(titleId, ObjectType.TITLE)).getOrThrow()
                assertListTotalSize(1, getSubscriptions(PaginationQuery()))

                val titleInfo =
                    userTitles(
                        session.uid,
                        10,
                        TitleSearchType.RECEIVER,
                    ).getOrThrow().data.first { t -> t.id == titleId }
                assertNotNull(titleInfo.subscriptionId)
            }
        }

    @Test
    fun `test remove title subscription`() =
        test {
            attachSession { session ->
                val c = createCommunity(NewCommunity("sub1", "sub1")).getOrThrow()
                val cId = c.id
                createTitle(
                    NewTitle("c KOL sub", TitleType.REGULAR, session.uid, cId, ObjectType.COMMUNITY, "hello sub"),
                ).getOrThrow()

                val titleId =
                    userTitles(
                        session.uid,
                        10,
                        TitleSearchType.RECEIVER,
                    ).getOrThrow().data.first { t -> t.name == "c KOL sub" }.id

                addSubscription(NewSubscription(titleId, ObjectType.TITLE)).getOrThrow()

                removeSubscription(titleId, ObjectType.TITLE).getOrThrow()
                assertListTotalSize(0, getSubscriptions(PaginationQuery()))
            }
        }
}
