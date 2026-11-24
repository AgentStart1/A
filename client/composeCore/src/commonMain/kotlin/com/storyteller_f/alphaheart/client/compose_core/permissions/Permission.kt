/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.permissions

sealed interface Permission {
    data object Audio : Permission
    data object Notification : Permission
    data object Camera : Permission
}

expect fun requestPermission(permission: Permission)
