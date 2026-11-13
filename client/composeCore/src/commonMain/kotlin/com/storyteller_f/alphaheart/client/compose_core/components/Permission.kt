/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

sealed interface Permission {
    data object Audio : Permission
    data object Notification : Permission
    data object Camera : Permission
}

@Composable
expect fun isPermissionGranted(permission: Permission): MutableState<Boolean>

expect fun requestPermission(permission: Permission)
