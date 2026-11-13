/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.rules.TestName
import kotlin.time.Duration.Companion.minutes

open class AppiumTestBase<out T : TargetAppiumHelper, out P : PlatformAppiumHelper>(
    val targetHelper: T,
    val platformHelper: P,
) {
    @get:Rule
    val name = TestName()

    val testName: String get() = name.methodName
}

fun runAppiumBlockingTest(block: suspend () -> Unit) =
    runBlocking {
        withTimeout(10.minutes) {
            block()
        }
    }
