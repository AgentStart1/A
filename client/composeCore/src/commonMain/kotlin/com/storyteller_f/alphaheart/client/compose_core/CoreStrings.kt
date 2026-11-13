/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

object CoreStrings {
    @Composable
    fun selectFile(): String = stringResource(Res.string.select_file)

    @Composable
    fun ok(): String = stringResource(Res.string.ok)

    @Composable
    fun start(): String = stringResource(Res.string.start)

    @Composable
    fun cancel(): String = stringResource(Res.string.cancel)

    @Composable
    fun aid(aid: String): String = stringResource(Res.string.aid_label, aid)

    @Composable
    fun ad(address: String): String = stringResource(Res.string.ad_label, address)

    @Composable
    fun privateKey(): String = stringResource(Res.string.private_key)

    @Composable
    fun permission_denied(): String = stringResource(Res.string.permission_denied)
}
