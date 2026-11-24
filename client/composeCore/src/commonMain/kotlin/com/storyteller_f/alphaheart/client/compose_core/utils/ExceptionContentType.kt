/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import com.storyteller_f.alphaheart.client.core.ServerErrorException
import io.ktor.client.network.sockets.*

fun ServerErrorException.isHtmlContent(): Boolean = text.startsWith("<html") || text.startsWith("<!DOCTYPE html")
