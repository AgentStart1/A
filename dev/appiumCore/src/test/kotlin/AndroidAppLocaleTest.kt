/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AndroidAppLocaleTest {
    @Test
    fun restoreOriginalLocaleAfterReset() =
        runAppiumBlockingTest {
            val commands = mutableListOf<List<String>>()
            var locale = "zh-CN"
            withEnglishAndroidAppLocale("test.app", { args ->
                commands.add(args)
                when {
                    args.last() == "ro.build.version.sdk" -> "33"

                    "get-app-locales" in args -> "Locales for test.app are [$locale]"

                    "clear" in args -> {
                        locale = ""
                        "Success"
                    }

                    "set-app-locales" in args -> {
                        locale = args.last()
                        ""
                    }

                    else -> error("Unexpected command")
                }
            }) {
                assertEquals("en-US", locale)
                assertEquals("get-app-locales", commands[1][3])
                assertEquals(listOf("shell", "pm", "clear", "test.app"), commands[2])
            }
            assertEquals("zh-CN", locale)
        }

    @Test
    fun restoreEmptyOverrideAfterFailure() =
        runAppiumBlockingTest {
            val commands = mutableListOf<List<String>>()
            assertFailsWith<IllegalStateException> {
                withEnglishAndroidAppLocale("test.app", { args ->
                    commands.add(args)
                    when {
                        args.last() == "ro.build.version.sdk" -> "33"
                        "get-app-locales" in args -> "Locales for test.app are []"
                        else -> ""
                    }
                }) { error("Test failed") }
            }
            assertEquals(listOf("shell", "cmd", "locale", "set-app-locales", "test.app"), commands.last())
        }
}
