/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.maxmind.geoip2.DatabaseReader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import java.net.InetAddress
import kotlin.jvm.optionals.getOrNull

fun ApplicationCall.remoteIp(reader: DatabaseReader): List<Pair<String, String?>> {
    val remoteAddress = request.origin.remoteAddress
    val country = reader.tryCountry(InetAddress.getByName(remoteAddress)).getOrNull()
    return if (country == null) {
        request.header("X-Forwarded-For")?.split(", ").orEmpty().mapNotNull {
            val c = reader.tryCountry(InetAddress.getByName(it)).getOrNull()
            if (c != null) {
                it to c.country().isoCode()
            } else {
                null
            }
        }.ifEmpty {
            listOf("127.0.0.1" to null)
        }
    } else {
        listOf(remoteAddress to country.country().isoCode())
    }
}
