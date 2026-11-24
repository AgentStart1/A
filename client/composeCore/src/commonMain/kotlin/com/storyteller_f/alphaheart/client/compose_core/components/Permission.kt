/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.storyteller_f.alphaheart.client.compose_core.permissions.Permission

@Composable
expect fun isPermissionGranted(permission: Permission): MutableState<Boolean>
