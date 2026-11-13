/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

internal suspend fun withEnglishAndroidAppLocale(
    packageName: String,
    runAdbCommand: (List<String>) -> String,
    block: suspend () -> Unit,
) {
    val api = runAdbCommand(listOf("shell", "getprop", "ro.build.version.sdk")).trim().toInt()
    if (api < 33) {
        val locale = runAdbCommand(listOf("shell", "getprop", "persist.sys.locale")).trim()
        check(locale.startsWith("en")) { "Android below 13 requires an English device locale for E2E" }
        runAdbCommand(listOf("shell", "pm", "clear", packageName))
        block()
        return
    }
    val output = runAdbCommand(listOf("shell", "cmd", "locale", "get-app-locales", packageName))
    val previous =
        requireNotNull(Regex("\\[(.*)]").find(output)?.groupValues?.get(1)) {
            "Unable to read the test application's locale"
        }
    try {
        // Clearing app data also clears its locale override; capture it before resetting.
        runAdbCommand(listOf("shell", "pm", "clear", packageName))
        runAdbCommand(listOf("shell", "cmd", "locale", "set-app-locales", packageName, "--locales", "en-US"))
        block()
    } finally {
        val command = listOf("shell", "cmd", "locale", "set-app-locales", packageName)
        runAdbCommand(if (previous.isEmpty()) command else command + listOf("--locales", previous))
    }
}
