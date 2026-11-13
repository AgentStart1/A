/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.api.SearchQuery
import com.storyteller_f.alphaheart.client.core.getAllFiles
import com.storyteller_f.alphaheart.client.core.searchFiles
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminFileTest {
    @Test
    fun `admin search files by name`() =
        test {
            val outer = attachPanelSession()
            attachSession {
                upload(it.uid ob ObjectType.USER, getUploadDataFromText("file1", "test1.txt")).getOrThrow()
                upload(it.uid ob ObjectType.USER, getUploadDataFromText("file2", "document.txt")).getOrThrow()
                upload(it.uid ob ObjectType.USER, getUploadDataFromText("file3", "test2.txt")).getOrThrow()
            }

            loginPanelSession(outer) {
                // 按名称搜索
                val testFiles =
                    searchFiles(
                        SearchQuery(word = "test"),
                    ).getOrThrow().data
                assertEquals(2, testFiles.size)
                kotlin.test.assertTrue(testFiles.all { it.name.contains("test") })

                // 搜索特定文件
                val docFiles =
                    searchFiles(
                        SearchQuery(word = "document"),
                    ).getOrThrow().data
                assertEquals(1, docFiles.size)
                assertEquals("document.txt", docFiles.first().name)
            }
        }

    @Test
    fun `admin search files across multiple users`() =
        test {
            val outer = attachPanelSession()
            attachSession {
                upload(it.uid ob ObjectType.USER, getUploadDataFromText("user1 file", "user1.txt")).getOrThrow()
            }
            attachSession {
                upload(it.uid ob ObjectType.USER, getUploadDataFromText("user2 file", "user2.txt")).getOrThrow()
            }

            loginPanelSession(outer) {
                // 管理员应该能搜索所有用户的文件
                val allUserFiles =
                    searchFiles(
                        SearchQuery(word = "user"),
                    ).getOrThrow().data
                assertEquals(2, allUserFiles.size)

                val fileNames = allUserFiles.map { it.name }.toSet()
                kotlin.test.assertTrue(fileNames.contains("user1.txt"))
                kotlin.test.assertTrue(fileNames.contains("user2.txt"))
            }
        }

    @Test
    fun `empty admin search returns every file`() =
        test {
            val outer = attachPanelSession()
            attachSession {
                upload(it.uid ob ObjectType.USER, getUploadDataFromText("content1", "file1.txt")).getOrThrow()
                upload(it.uid ob ObjectType.USER, getUploadDataFromText("content2", "file2.txt")).getOrThrow()
            }

            loginPanelSession(outer) {
                // 空关键词应返回所有文件
                val allFiles = getAllFiles(PaginationQuery()).getOrThrow().data
                assertEquals(2, allFiles.size)
            }
        }
}
