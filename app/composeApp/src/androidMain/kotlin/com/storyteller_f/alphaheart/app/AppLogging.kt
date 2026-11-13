/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import android.app.Application
import android.util.Log
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale

private const val LOG_DIR_NAME = "logs"
private const val LOG_FILE_NAME = "appium-app.log"
private const val DEFAULT_TAG = "Napier"

fun setupAppLogger(application: Application) {
    val logFile = ensureAppLogFile(application)
    Napier.takeLogarithm()
    Napier.base(AppAntilog(logFile))
    installCrashHandler()
}

private fun installCrashHandler() {
    val original = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        Napier.e(tag = "FATAL", throwable = throwable) {
            "Uncaught exception on thread ${thread.name}"
        }
        original?.uncaughtException(thread, throwable)
    }
}

private fun ensureAppLogFile(application: Application): File =
    File(
        application.filesDir,
        "$LOG_DIR_NAME/$LOG_FILE_NAME",
    ).apply {
        parentFile?.mkdirs()
        if (!exists()) {
            createNewFile()
        }
    }

private class AppAntilog(private val logFile: File) : Antilog() {
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
        val actualTag = tag?.takeIf { it.isNotBlank() } ?: DEFAULT_TAG
        val logcatMessage =
            when {
                message == null -> throwable?.stackTraceToString().orEmpty()
                throwable != null -> "$message\n${throwable.stackTraceToString()}"
                else -> message
            }
        if (logcatMessage.isNotBlank()) {
            Log.println(priority.toAndroidPriority(), actualTag, logcatMessage)
        }
        val renderedMessage =
            buildString {
                append(formatter.format(System.currentTimeMillis()))
                append(' ')
                append(priority.name)
                append(' ')
                append('[')
                append(actualTag)
                append(']')
                message?.takeIf { it.isNotBlank() }?.let {
                    append(' ')
                    append(it)
                }
                throwable?.let {
                    if (isNotEmpty()) {
                        append('\n')
                    }
                    append(it.stackTraceToString())
                }
            }
        if (renderedMessage.isBlank()) {
            return
        }
        writeLog(renderedMessage)
    }

    private fun writeLog(message: String) {
        try {
            OutputStreamWriter(FileOutputStream(logFile, true), StandardCharsets.UTF_8).use { writer ->
                writer.append(message)
                writer.append('\n')
            }
        } catch (e: Exception) {
            Log.e("AppAntilog", "Write log failed", e)
        }
    }
}

private fun LogLevel.toAndroidPriority(): Int =
    when (this) {
        LogLevel.VERBOSE -> Log.VERBOSE
        LogLevel.DEBUG -> Log.DEBUG
        LogLevel.INFO -> Log.INFO
        LogLevel.WARNING -> Log.WARN
        LogLevel.ERROR -> Log.ERROR
        LogLevel.ASSERT -> Log.ASSERT
    }
