/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.junit.Test
import org.openqa.selenium.WebDriver
import java.io.File
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class WasmCoverageTest {
    @Test
    fun cleanupRetainsTestAndCollectorFailures() {
        val events = mutableListOf<String>()
        val original = IllegalStateException("test failure")
        val snapshotFailure = IllegalStateException("snapshot failure")
        val closeFailure = IllegalStateException("driver failure")
        val browser =
            Proxy.newProxyInstance(
                WebDriver::class.java.classLoader,
                arrayOf(WebDriver::class.java),
            ) { _, method, _ ->
                check(method.name == "quit")
                events.add("quit")
                throw closeFailure
            } as WebDriver
        val actual =
            assertFailsWith<IllegalStateException> {
                useWasmBrowser(browser, {
                    events.add("snapshot")
                    throw snapshotFailure
                }) {
                    events.add("test")
                    throw original
                }
            }
        assertSame(original, actual)
        assertEquals(listOf("test", "snapshot", "quit"), events)
        assertEquals(listOf(snapshotFailure), original.suppressed.toList())
        assertEquals(listOf(closeFailure), snapshotFailure.suppressed.toList())
    }

    private fun fixture(name: String): Pair<WasmCoverage, File> {
        val directory = File("build/coverage-tests/$name").apply { mkdirs() }
        val manifest = directory.resolve("input.json")
        manifest.writeText("""{"buildId":"${"a".repeat(64)}","lines":[{"id":0}]}""")
        val output = directory.resolve("output")
        return WasmCoverage(manifest, output) to output
    }

    @Test
    fun navigationCannotHideAnUncollectedDocument() {
        val (coverage, output) = fixture("navigation")
        val identity = """ "buildId":"${"a".repeat(64)}" """
        coverage.record("begin", """{$identity,"documentKey":"first"}""")
        coverage.record("begin", """{$identity,"documentKey":"second"}""")
        coverage.record("snapshot", """{$identity,"documentKey":"second","instances":[{"covered":[0]}]}""")
        assertEquals("incomplete", output.resolve("status").readText())
        coverage.record("snapshot", """{$identity,"documentKey":"first","instances":[{"covered":[]}]}""")
        assertEquals("complete", output.resolve("status").readText())
        coverage.record("begin", """{$identity,"documentKey":"second"}""")
        assertEquals("complete", output.resolve("status").readText())
    }

    @Test
    fun rejectsForeignBuildAndUnknownLocations() {
        val (coverage, output) = fixture("identity")
        assertFailsWith<IllegalStateException> {
            coverage.record("begin", """{"buildId":"${"b".repeat(64)}","documentKey":"foreign"}""")
        }
        assertFailsWith<IllegalStateException> {
            coverage.record(
                "snapshot",
                """{"buildId":"${"a".repeat(64)}","documentKey":"current","instances":[{"covered":[1]}]}""",
            )
        }
        assertEquals("incomplete", output.resolve("status").readText())
    }
}
