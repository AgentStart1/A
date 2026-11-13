/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

fun getDesktopServerUrl(): String = System.getProperty("appium.server.url") ?: AppConfig.SERVER_URL

fun getDesktopWsServerUrl(): String = System.getProperty("appium.ws.url") ?: AppConfig.WS_SERVER_URL
