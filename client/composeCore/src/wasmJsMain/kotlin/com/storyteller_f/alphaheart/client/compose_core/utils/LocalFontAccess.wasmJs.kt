/*
 * This is a private project. All rights reserved.
 */

@file:JsModule("local-font-access")
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.utils

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString

internal external fun startFontGate(onSnapshot: (JsString) -> Unit)

internal external fun confirmFont(requestId: Int): Boolean

internal external fun rejectFont(requestId: Int)

internal external fun stopFontGate()
