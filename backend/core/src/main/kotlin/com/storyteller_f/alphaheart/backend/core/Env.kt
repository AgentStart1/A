/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

import io.github.aakira.napier.Napier
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.Properties

class MergedEnv(val list: List<Map<String, String>>) {
    operator fun get(key: String): String? =
        list.firstNotNullOfOrNull { map ->
            map[key]
        }

    fun getAll(key: String): String =
        list.joinToString {
            it[key] ?: "empty"
        }
}

fun readEnv(envMap: Map<String, String>? = null) =
    MergedEnv(
        buildList {
            addAll(
                listOfNotNull(
                    envMap, // 测试时通过config map手动传递
                    System.getenv(), // 正式部署
                    readResourceEnv("test.env"), // 测试
                ),
            )
        },
    )

fun readResourceEnv(resName: String): Map<String, String>? {
    val stream = ClassLoader.getSystemResourceAsStream(resName)
    Napier.i {
        if (stream == null) {
            "resource env file $resName not exists"
        } else {
            "read env from resource: $resName"
        }
    }
    return stream?.use {
        Properties().apply {
            load(it)
        }
    }?.map {
        it.key as String to it.value as String
    }?.associate { it }
}

fun readFileEnv(path: String): Map<String, String>? {
    val file = File(path)
    Napier.i {
        if (file.exists()) {
            "read env from file: ${file.canonicalPath}"
        } else {
            "env file ${file.canonicalPath} not exists"
        }
    }
    return if (file.exists()) {
        FileInputStream(path).use {
            Properties().apply {
                load(it)
            }
        }.map {
            it.key as String to it.value as String
        }.associate { it }
    } else {
        null
    }
}

fun setLogPath() {
    if (System.getProperty("LOG_PATH") == null) {
        val customLogPath = System.getenv("LOG_PATH")
        if (!customLogPath.isNullOrBlank()) {
            val s = File(customLogPath).canonicalPath
            Napier.i { "Set custom log path" }
            System.setProperty("LOG_PATH", s)
            return
        }
        val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
        val envOs = System.getenv("OSTYPE")?.lowercase(Locale.getDefault()).orEmpty()

        // 判断是否是 Windows 原生或 Cygwin / MINGW
        val isWindowsLike = osName.contains("win") || envOs.contains("cygwin") || envOs.contains("mingw")

        val logPath =
            if (isWindowsLike) {
                File(System.getProperty("java.io.tmpdir"), "log")
            } else {
                // 获取home 目录
                File(System.getProperty("user.home"), "log")
            }
        val s = logPath.canonicalPath
        Napier.i { "Set default log path" }
        System.setProperty("LOG_PATH", s)
    }
}

fun preprocessUserInputKeyword(words: String): String = words.lowercase()

/**
 * 分割并预处理用户输入的关键字.
 * @return 返回关键字列表，如果不包含空格则返回 null 表示使用原有逻辑
 */
fun splitKeywords(words: String): List<String>? {
    val trimmed = words.trim()
    if (!trimmed.contains(" ")) return null
    return trimmed.split("\\s+".toRegex())
        .filter { it.isNotBlank() }
        .map { preprocessUserInputKeyword(it) }
}
