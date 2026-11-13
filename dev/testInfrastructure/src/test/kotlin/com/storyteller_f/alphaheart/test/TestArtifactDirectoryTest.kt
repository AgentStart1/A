/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestArtifactDirectoryTest {
    @Test
    fun keepReadableNamesAndPreserveCollectedCases() {
        val root = File("build/test/report-directories/readable")
        val directory = createTestArtifactDirectory(root, "ExampleTest", "register and browse 测试")
        assertEquals(root.resolve("ExampleTest/register and browse 测试"), directory)
        directory.resolve("stale.log").writeText("old report")
        val other = createTestArtifactDirectory(root, "ExampleTest", "other test")
        other.resolve("keep.log").writeText("other report")
        assertEquals(directory, createTestArtifactDirectory(root, "ExampleTest", "register and browse 测试"))
        assertEquals("old report", directory.resolve("stale.log").readText())
        assertEquals("other report", other.resolve("keep.log").readText())
    }

    @Test
    fun encodeUnsafeNamesWithoutCollisions() {
        val root = File("build/test/report-directories/encoded")
        val names = listOf("../test", "..\\test", "CON", "%43ON", "", ".", "..", "a:", "a%3A", "trailing.")
        val directories = names.map { createTestArtifactDirectory(root, it) }
        assertEquals(names.size, directories.distinct().size)
        directories.forEach { assertTrue(it.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())) }
        assertEquals("%43ON", directories[2].name)
        assertEquals("a%3A", directories[7].name)
    }
}
