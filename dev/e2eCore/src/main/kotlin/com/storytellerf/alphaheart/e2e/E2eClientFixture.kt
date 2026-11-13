/*
 * This is a private project. All rights reserved.
 */

package com.storytellerf.alphaheart.e2e

import com.storyteller_f.alphaheart.client.core.AuthKey
import com.storyteller_f.alphaheart.client.core.PanelSessionManager
import com.storyteller_f.alphaheart.client.core.SimplePassHolder
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.buildWebSocketUrl
import com.storyteller_f.alphaheart.client.core.createSimplePanelSessionManager
import com.storyteller_f.alphaheart.client.core.createSimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.defaultClientConfigure
import com.storyteller_f.alphaheart.client.core.defaultClientConfigureForPanel
import com.storyteller_f.alphaheart.client.core.getClient
import com.storyteller_f.alphaheart.client.core.panelSignUp
import com.storyteller_f.alphaheart.client.core.userSignUp
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.model.AlgoType
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage

/** A synthetic registered identity; CLI tests still authenticate through terminal input. */
class E2eUser(val key: String, val session: UserSessionManager) : AutoCloseable {
    val id get() = requireNotNull(session.model.uid)
    override fun close() = session.client.close()
}

class E2ePanel(val key: String, val session: PanelSessionManager) : AutoCloseable {
    override fun close() = session.client.close()
}

suspend fun createE2eUser(ports: E2ePorts): E2eUser {
    val key = createE2eAuthKey()
    val pass = SimplePassHolder()
    val manager =
        createSimpleUserSessionManager(
            buildWebSocketUrl("ws://127.0.0.1:${ports.ws}"),
            AcceptAllCookiesStorage(),
            pass,
            { model, cookies ->
                getClient { defaultClientConfigure(cookies, model, "http://127.0.0.1:${ports.server}") }
            },
        ) { _, _, _ -> }
    var retained = false
    return try {
        manager.userSignUp(key, pass)
        val user = E2eUser(key.pemPrivateKey, manager)
        retained = true
        user
    } finally {
        if (!retained) manager.client.close()
    }
}

suspend fun createE2ePanel(ports: E2ePorts): E2ePanel {
    val key = createE2eAuthKey()
    val pass = SimplePassHolder()
    val manager =
        createSimplePanelSessionManager(pass, AcceptAllCookiesStorage()) { model, cookies ->
            getClient { defaultClientConfigureForPanel(cookies, model, "http://127.0.0.1:${ports.server}") }
        }
    var retained = false
    return try {
        manager.panelSignUp(key, pass)
        val panel = E2ePanel(key.pemPrivateKey, manager)
        retained = true
        panel
    } finally {
        if (!retained) manager.client.close()
    }
}

private suspend fun createE2eAuthKey(): AuthKey.P256 {
    loadCryptoLibIfNeed()
    val algo = getAlgo(AlgoType.P256)
    val key = algo.generatePemKeyPair().getOrThrow().first
    return AuthKey.P256(
        key,
        algo.getDerPrivateKey(key).getOrThrow(),
        algo.getDerPublicKeyFromPrivateKey(key).getOrThrow(),
    )
}
