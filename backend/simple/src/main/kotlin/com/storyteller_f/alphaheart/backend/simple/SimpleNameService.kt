/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.simple

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.service.NameService
import com.storyteller_f.alphaheart.backend.core.service.NameServiceFactory
import com.storyteller_f.shared.type.DEFAULT_PRIMARY_KEY
import com.storyteller_f.shared.type.PrimaryKey

class SimpleNameService : NameService {
    private val nameMap = mutableMapOf<Int, Int>()

    // 存储合集累加的个数
    private val countList = mutableListOf<Int>()

    init {
        ClassLoader.getSystemResourceAsStream("charset")!!.bufferedReader().use { reader ->
            reader.readText().split("\n").filter { element ->
                !element.startsWith("//")
            }.map {
                val split = it.trim().split(Regex("[ \\t]"))
                // 个数以及范围
                split[1].dropLast(1).toInt() to split[2].split("-")
            }.fold(0) { acc, i ->
                val count = acc + i.first
                countList.add(count)
                val leftRange = i.second.first().toInt(16)
                nameMap[count] = leftRange
                count
            }
        }
    }

    override fun parse(id: Long): String = numberToCustomCharset(id)

    // 将数字转换为自定义字符集表示的字符串
    private fun numberToCustomCharset(num: PrimaryKey): String {
        val base = countList.last().toLong()

        var number = num

        assert(num > DEFAULT_PRIMARY_KEY)

        val result = StringBuilder()
        while (number > DEFAULT_PRIMARY_KEY) {
            val remainder = (number % base).toInt()
            val i =
                countList.indexOfFirst {
                    it > remainder
                }
            val leftRange = nameMap.getValue(countList[i])
            val toChar = (leftRange + remainder - countList.getOrElse(i - 1) { 0 }).toChar()
            result.append(toChar)
            number /= base
        }

        // 因为进制转换是从低位到高位进行的，所以结果需要反转
        return result.reverse().toString()
    }
}

class SimpleNameServiceFactory : NameServiceFactory {
    override fun match(env: MergedEnv): Boolean = true

    override fun build(env: MergedEnv): NameService = SimpleNameService()
}
