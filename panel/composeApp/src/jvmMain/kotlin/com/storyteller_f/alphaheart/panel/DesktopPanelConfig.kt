/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

fun getDesktopPanelServerUrl(): String = System.getProperty("appium.server.url") ?: PanelConfig.SERVER_URL
