/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.test.ArtifactCollectionError
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.onFailure
import io.appium.java_client.AppiumDriver
import java.io.File

object DesktopAppiumFailureDumper {
    internal data class Context(val outputDir: File, val appLabel: String, val logDir: File, val appLogFile: File)

    internal fun dumpOnFailure(context: Context, driver: AppiumDriver?, throwable: Throwable) {
        val outputDir = context.outputDir
        outputDir.mkdirs()

        File(outputDir, "failure.txt").writeText(
            buildString {
                appendLine(throwable::class.qualifiedName ?: throwable::class.java.name)
                appendLine(throwable.message.orEmpty())
                appendLine()
                appendLine(throwable.stackTraceToString())
            },
        )

        dumpPageSource(driver, outputDir)
        copyDesktopLogs(context.appLabel, context.logDir, context.appLogFile, outputDir)
    }

    private fun dumpPageSource(driver: AppiumDriver?, outputDir: File) {
        if (driver == null) return
        captureFailure(ArtifactCollectionError::CollectionFailed) {
            File(
                outputDir,
                "appium-page-source.xml",
            ).writeText(checkNotNull(driver.pageSource) { "Appium returned no page source" })
        }.onFailure {
            File(outputDir, "appium-page-source.error.txt").writeText(it.cause.stackTraceToString())
        }
    }

    private fun copyDesktopLogs(appLabel: String, logDir: File, appLogFile: File, outputDir: File) {
        if (appLogFile.isFile) {
            appLogFile.copyTo(File(outputDir, appLogFile.name), overwrite = true)
        } else {
            File(outputDir, "desktop-log.error.txt")
                .writeText("$appLabel log not found: ${appLogFile.canonicalPath}\n")
        }

        logDir.listFiles()
            .orEmpty()
            .filter { file ->
                file.isFile && (
                    file.name.startsWith("hs_err_pid") ||
                        file.name.startsWith("replay_pid") ||
                        file.extension == "mdmp"
                    )
            }
            .forEach { file ->
                file.copyTo(File(outputDir, file.name), overwrite = true)
            }
    }
}
