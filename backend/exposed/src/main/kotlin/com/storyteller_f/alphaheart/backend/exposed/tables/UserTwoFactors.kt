/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.UserTwoFactor
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.shared.model.TwoFactorType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.datetime.datetime

object UserTwoFactors : BaseTable() {
    val uid = customPrimaryKey("uid").uniqueIndex()
    val enabled = bool("enabled").default(false)
    val type = enumerationByName<TwoFactorType>("type", 20)
    val totpSecret = varchar("totp_secret", 64)
    val recoveryCodeHashes = text("recovery_code_hashes")
    val updatedAt = datetime("updated_at")
}

private val json = Json { ignoreUnknownKeys = true }
private val recoveryCodeHashesSerializer = ListSerializer(String.serializer())

fun UserTwoFactor.Companion.wrapRow(row: ResultRow): UserTwoFactor =
    with(UserTwoFactors) {
        UserTwoFactor(
            row[uid],
            row[enabled],
            row[type],
            row[totpSecret],
            json.decodeFromString(recoveryCodeHashesSerializer, row[recoveryCodeHashes]),
            row[updatedAt],
        )
    }

fun encodeRecoveryCodeHashes(hashes: List<String>): String = json.encodeToString(recoveryCodeHashesSerializer, hashes)
