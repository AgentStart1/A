/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.SignInBody
import com.storyteller_f.alphaheart.api.SignInResponse
import com.storyteller_f.alphaheart.client.core.disableTwoFactor
import com.storyteller_f.alphaheart.client.core.enableTotp
import com.storyteller_f.alphaheart.client.core.generateRecoveryCodes
import com.storyteller_f.alphaheart.client.core.getData
import com.storyteller_f.alphaheart.client.core.getTwoFactorSettings
import com.storyteller_f.alphaheart.client.core.prepareSignInFromPrivateKey
import com.storyteller_f.alphaheart.client.core.restoreSession
import com.storyteller_f.alphaheart.client.core.setupTotp
import com.storyteller_f.alphaheart.client.core.signIn
import com.storyteller_f.alphaheart.client.core.signInTotp
import com.storyteller_f.alphaheart.client.core.signOut
import com.storyteller_f.result.getOrThrow
import java.nio.ByteBuffer
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class TwoFactorLifecycleTest {
    @Test
    fun `totp enrollment challenge recovery rotation and disable`() =
        test {
            attachSession { account ->
                assertFalse(getTwoFactorSettings().getOrThrow().enabled)
                val setup = setupTotp().getOrThrow()
                assertFalse(getTwoFactorSettings().getOrThrow().enabled)
                assertTrue(enableTotp("invalid").isFailure)
                assertTrue(enableTotp(currentCode(setup.secret)).getOrThrow().enabled)
                assertTrue(getTwoFactorSettings().getOrThrow().enabled)
                val rotated = generateRecoveryCodes().getOrThrow().recoveryCodes
                assertEquals(10, rotated.size)
                assertEquals(10, rotated.distinct().size)
                assertTrue(rotated.none { it in setup.recoveryCodes })
                signOut().getOrThrow()
                assertTrue(restoreSession().isFailure)
                val prepared = prepareSignInFromPrivateKey(account.authKey) { getData() }.getOrThrow()
                assertIs<SignInResponse.RequiresTotp>(
                    signIn(SignInBody(prepared.address, prepared.signature)).getOrThrow(),
                )
                assertTrue(restoreSession().isFailure)
                assertTrue(signInTotp("invalid").isFailure)
                val signedIn = signInTotp(currentCode(setup.secret)).getOrThrow()
                assertEquals(account.uid, signedIn.id)
                assertEquals(account.uid, restoreSession().getOrThrow().id)
                assertFalse(disableTwoFactor().getOrThrow().enabled)
                assertFalse(getTwoFactorSettings().getOrThrow().enabled)
                signOut().getOrThrow()
                val withoutTotp = prepareSignInFromPrivateKey(account.authKey) { getData() }.getOrThrow()
                val response = signIn(SignInBody(withoutTotp.address, withoutTotp.signature)).getOrThrow()
                assertEquals(account.uid, assertIs<SignInResponse.Success>(response).userInfo.id)
            }
        }
}

private fun currentCode(secret: String): String {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    val bits = secret.map { alphabet.indexOf(it).toString(2).padStart(5, '0') }.joinToString("")
    val key = bits.chunked(8).map { it.toInt(2).toByte() }.toByteArray()
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(SecretKeySpec(key, "HmacSHA1"))
    val digest = mac.doFinal(ByteBuffer.allocate(8).putLong(Instant.now().epochSecond / 30).array())
    val offset = digest.last().toInt() and 15
    val number = ByteBuffer.wrap(digest, offset, 4).int and Int.MAX_VALUE
    return (number % 1_000_000).toString().padStart(6, '0')
}
