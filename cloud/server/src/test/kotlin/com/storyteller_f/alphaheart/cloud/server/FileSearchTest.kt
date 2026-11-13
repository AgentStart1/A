/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.api.SearchQuery
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.joinRoom
import com.storyteller_f.alphaheart.client.core.searchFiles
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileSearchTest {
    @Test
    fun `file search matches full names and tokens`() =
        test {
            attachSession { session ->
                val name = "Annual-Report.pdf"
                upload(session.uid ob ObjectType.USER, getUploadDataFromText("report", name)).getOrThrow()
                for (word in listOf("annual-report.pdf", "ANNUAL-REPORT.PDF", "annual", "report", "annual report")) {
                    val files = searchFiles(SearchQuery(word = word), session.uid, ObjectType.USER).getOrThrow().data
                    assertEquals(listOf(name), files.map { it.name }, word)
                }
            }
        }

    @Test
    fun `test search files by name`() =
        test {
            attachSession { session ->
                // 上传多个文件
                val tuple = session.uid ob ObjectType.USER
                upload(tuple, getUploadDataFromText("file1", "test1.txt")).getOrThrow()
                upload(tuple, getUploadDataFromText("file2", "document.txt")).getOrThrow()
                upload(tuple, getUploadDataFromText("file3", "test2.txt")).getOrThrow()

                // 按名称搜索
                val testFiles =
                    searchFiles(
                        SearchQuery(word = "test"),
                        session.uid,
                        ObjectType.USER,
                    ).getOrThrow().data
                assertEquals(2, testFiles.size)
                assertTrue(testFiles.all { file -> file.name.contains("test") })

                // 搜索特定文件
                val docFiles =
                    searchFiles(
                        SearchQuery(word = "document"),
                        session.uid,
                        ObjectType.USER,
                    ).getOrThrow().data
                assertEquals(1, docFiles.size)
                assertEquals("document.txt", docFiles.first().name)
            }
        }

    @Test
    fun `test search files only returns user own files`() =
        test {
            attachSession { session ->
                upload(session.uid ob ObjectType.USER, getUploadDataFromText("user1 file", "user1.txt")).getOrThrow()
            }

            attachSession { session ->
                upload(session.uid ob ObjectType.USER, getUploadDataFromText("user2 file", "user2.txt")).getOrThrow()

                // 搜索自己的文件，不应包含其他用户的文件
                val myFiles =
                    searchFiles(
                        SearchQuery(word = "user"),
                        session.uid,
                        ObjectType.USER,
                    ).getOrThrow().data

                assertEquals(1, myFiles.size)
                assertEquals("user2.txt", myFiles.first().name)
                assertEquals(session.uid, myFiles.first().owner)
            }
        }

    @Test
    fun `test search room files with permission check`() =
        test {
            val sessionOuterTuple =
                attachSession { session ->
                    val communityId = createCommunity(NewCommunity("c1", "c1")).getOrThrow().id
                    val roomId = createRoom(NewRoom("r1", "room1", communityId = communityId)).getOrThrow().id

                    // 上传文件到房间
                    upload(roomId ob ObjectType.ROOM, getUploadDataFromText("room file1", "room1.txt")).getOrThrow()
                    upload(roomId ob ObjectType.ROOM, getUploadDataFromText("room file2", "room2.txt")).getOrThrow()

                    communityId to roomId
                }

            val communityId = sessionOuterTuple.custom.first
            val roomId = sessionOuterTuple.custom.second

            // 另一个用户加入社区和房间后应该能搜索房间文件
            attachSession { session ->
                joinCommunity(communityId).getOrThrow()
                joinRoom(roomId).getOrThrow()

                val roomFiles =
                    searchFiles(
                        SearchQuery(word = "room"),
                        roomId,
                        ObjectType.ROOM,
                    ).getOrThrow().data

                assertEquals(2, roomFiles.size)
                assertTrue(roomFiles.all { it.name.contains("room") })
            }
        }

    @Test
    fun `test search files without permission fails`() =
        test {
            val sessionOuterTuple =
                attachSession { session ->
                    val communityId = createCommunity(NewCommunity("c1", "c1")).getOrThrow().id
                    val roomId = createRoom(NewRoom("r1", "room1", communityId = communityId)).getOrThrow().id

                    upload(roomId ob ObjectType.ROOM, getUploadDataFromText("private file", "private.txt")).getOrThrow()

                    communityId to roomId
                }

            val roomId = sessionOuterTuple.custom.second

            // 未加入房间的用户不应能搜索房间文件
            attachSession { session ->
                val result =
                    searchFiles(
                        SearchQuery(word = "private"),
                        roomId,
                        ObjectType.ROOM,
                    )

                assertTrue(result.isFailure)
            }
        }

    @Test
    fun `test search files with empty keyword returns empty`() =
        test {
            attachSession { session ->
                upload(session.uid ob ObjectType.USER, getUploadDataFromText("content1", "file1.txt")).getOrThrow()
                upload(session.uid ob ObjectType.USER, getUploadDataFromText("content2", "file2.txt")).getOrThrow()
                upload(session.uid ob ObjectType.USER, getUploadDataFromText("content3", "file3.txt")).getOrThrow()

                // 空关键词应返回空列表
                val allFiles =
                    searchFiles(
                        SearchQuery(word = ""),
                        session.uid,
                        ObjectType.USER,
                    ).getOrThrow().data

                assertEquals(0, allFiles.size)
            }
        }

    @Test
    fun `test search files case insensitive`() =
        test {
            attachSession { session ->
                upload(session.uid ob ObjectType.USER, getUploadDataFromText("content", "TestFile.txt")).getOrThrow()

                // 搜索应该不区分大小写
                val upperCase =
                    searchFiles(
                        SearchQuery(word = "TEST"),
                        session.uid,
                        ObjectType.USER,
                    ).getOrThrow().data

                val lowerCase =
                    searchFiles(
                        SearchQuery(word = "test"),
                        session.uid,
                        ObjectType.USER,
                    ).getOrThrow().data

                assertEquals(1, upperCase.size)
                assertEquals(1, lowerCase.size)
                assertEquals(upperCase.first().id, lowerCase.first().id)
            }
        }
}
