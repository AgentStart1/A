/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.runtime.Composable
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.review_log_add_child
import com.storyteller_f.alphaheart.panel.review_log_add_favorite
import com.storyteller_f.alphaheart.panel.review_log_add_subscription
import com.storyteller_f.alphaheart.panel.review_log_create
import com.storyteller_f.alphaheart.panel.review_log_delete
import com.storyteller_f.alphaheart.panel.review_log_exit
import com.storyteller_f.alphaheart.panel.review_log_join
import com.storyteller_f.alphaheart.panel.review_log_remove_child
import com.storyteller_f.alphaheart.panel.review_log_remove_favorite
import com.storyteller_f.alphaheart.panel.review_log_remove_subscription
import com.storyteller_f.alphaheart.panel.review_log_sign_in
import com.storyteller_f.alphaheart.panel.review_log_sign_up
import com.storyteller_f.alphaheart.panel.review_log_update
import com.storyteller_f.alphaheart.panel.review_log_update_status
import com.storyteller_f.shared.model.UserLogType
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun panelUserLogText(type: UserLogType): String =
    stringResource(
        when (type) {
            UserLogType.SIGN_IN -> Res.string.review_log_sign_in
            UserLogType.SIGN_UP -> Res.string.review_log_sign_up
            UserLogType.CREATE -> Res.string.review_log_create
            UserLogType.DELETE -> Res.string.review_log_delete
            UserLogType.UPDATE -> Res.string.review_log_update
            UserLogType.JOIN -> Res.string.review_log_join
            UserLogType.EXIT -> Res.string.review_log_exit
            UserLogType.ADD_ALTERNATIVE_ACCOUNT -> Res.string.review_log_add_child
            UserLogType.REMOVE_ALTERNATIVE_ACCOUNT -> Res.string.review_log_remove_child
            UserLogType.ADD_FAVORITE -> Res.string.review_log_add_favorite
            UserLogType.REMOVE_FAVORITE -> Res.string.review_log_remove_favorite
            UserLogType.ADD_SUBSCRIPTION -> Res.string.review_log_add_subscription
            UserLogType.REMOVE_SUBSCRIPTION -> Res.string.review_log_remove_subscription
        },
    )

private val panelStatusLogPattern =
    Regex("^Update (user|community|room|topic|title|file) status to (NORMAL|READ_ONLY|DELETED)$")

internal fun parsePanelStatusLog(action: String): Pair<String, String>? =
    panelStatusLogPattern.matchEntire(action)?.let { it.groupValues[1].uppercase() to it.groupValues[2] }

/** Translate only the server's known structured action; leave free-form diagnostic text intact. */
@Composable
internal fun panelLogActionText(action: String): String {
    val (objectType, status) = parsePanelStatusLog(action) ?: return action
    return stringResource(Res.string.review_log_update_status, panelValueText(objectType), panelValueText(status))
}
