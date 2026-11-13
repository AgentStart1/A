/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.ADDRESS_LENGTH
import com.storyteller_f.alphaheart.backend.core.types.PanelAccount
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.algoType
import com.storyteller_f.alphaheart.backend.exposed.passType
import org.jetbrains.exposed.v1.core.ResultRow

object PanelAccounts : BaseTable() {
    val name = varchar("name", 100)
    val publicKey = text("public_key")
    val publicKeyMd5 = varchar("public_key_md5", 32).uniqueIndex()
    val address = varchar("address", ADDRESS_LENGTH).uniqueIndex()
    val passType = passType("pass_type")
    val algoType = algoType("algo_type")
}

fun PanelAccount.Companion.wrapRow(resultRow: ResultRow): PanelAccount =
    PanelAccount(
        resultRow[PanelAccounts.id],
        resultRow[PanelAccounts.name],
        resultRow[PanelAccounts.passType],
        resultRow[PanelAccounts.algoType],
        resultRow[PanelAccounts.publicKey],
        resultRow[PanelAccounts.address],
    )
