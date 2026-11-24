/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.logs

private val panelStatusLogPattern =
    Regex("^Update (user|community|room|topic|title|file) status to (NORMAL|READ_ONLY|DELETED)$")

internal fun parsePanelStatusLog(action: String): Pair<String, String>? =
    panelStatusLogPattern.matchEntire(action)?.let { it.groupValues[1].uppercase() to it.groupValues[2] }
