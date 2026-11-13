/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.PanelAccountInfo
import com.storyteller_f.shared.model.PassType
import com.storyteller_f.shared.type.PrimaryKey

data class PanelAccount(
    val id: PrimaryKey,
    val name: String,
    val passType: PassType,
    val algoType: AlgoType,
    val publicKey: String,
    val address: String,
) {
    companion object
}

fun PanelAccount.toPanelAccountInfo() = PanelAccountInfo(id, name)

data class RawPanelAccount(val id: PrimaryKey, val name: String, val algoType: AlgoType)
