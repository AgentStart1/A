/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.ADDRESS_LENGTH
import com.storyteller_f.alphaheart.backend.core.USER_NICKNAME
import com.storyteller_f.alphaheart.backend.core.types.ChildAccount
import com.storyteller_f.alphaheart.backend.core.types.RawUser
import com.storyteller_f.alphaheart.backend.core.types.User
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.algoType
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.passType
import com.storyteller_f.shared.type.UserStatus
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.selectAll

object Users : BaseTable() {
    // hex编码的der 格式公钥
    val publicKey = text("public_key")
    val publicKeyMd5 = varchar("public_key_md5", 32).uniqueIndex()

    // Public Key (Kyber-768) hex 编码的der 格式
    val encryptionPublicKey = text("encryption_public_key").nullable()

    val address = varchar("address", ADDRESS_LENGTH).uniqueIndex()
    val icon = customPrimaryKey("icon").nullable()
    val nickname = varchar("nickname", USER_NICKNAME).index()
    val acgAmount = long("acg_amount").default(0)
    val passType = passType("pass_type")
    val algoType = algoType("algo_type")
    val notificationId = customPrimaryKey("notification_id")
    val status = enumerationByName<UserStatus>("status", 20).default(UserStatus.NORMAL)
}

fun User.Companion.wrapRow(row: ResultRow): User =
    with(Users) {
        User(
            row[Aids.value],
            row[encryptionPublicKey],
            row[publicKey],
            row[address],
            row[icon],
            row[nickname],
            row[id],
            row[createdTime],
            row[acgAmount],
            row[passType],
            row[algoType],
            row[notificationId],
            row[status],
        )
    }

fun User.Companion.find(function: () -> Op<Boolean>): Query = Users.selectAll().where(function)

fun mapUserInfo(it: ResultRow): RawUser = RawUser(User.wrapRow(it))

object ChildAccounts : Table() {
    val uid = customPrimaryKey("uid")
    val hostId = customPrimaryKey("host_id")
    val encryptedPrivateKey = text("encrypted_private_key")
    val encryptedAesKey = text("encrypted_aes_key")
    val primaryKeyMd5 = varchar("private_key_md5", 32).uniqueIndex()
    val remark = text("remark").nullable()
    val encryptedEncryptionPrivateKey = text("encrypted_encryption_private_key").nullable()
    override val primaryKey = PrimaryKey(uid)

    init {
        index("child-accounts-main", false, hostId, uid)
    }
}

fun ChildAccount.Companion.wrapRow(row: ResultRow): ChildAccount =
    with(ChildAccounts) {
        ChildAccount(
            row[uid],
            row[encryptedPrivateKey],
            row[encryptedAesKey],
            row[hostId],
            row[remark],
            row[encryptedEncryptionPrivateKey],
        )
    }
