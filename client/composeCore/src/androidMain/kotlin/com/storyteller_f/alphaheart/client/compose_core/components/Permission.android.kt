/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.storyteller_f.alphaheart.client.compose_core.permissions.Permission
import com.storyteller_f.alphaheart.client.compose_core.permissions.requestQueue

@Composable
actual fun isPermissionGranted(permission: Permission): MutableState<Boolean> {
    val isGranted =
        remember {
            mutableStateOf(false)
        }
    val context = LocalContext.current
    LaunchedEffect(context, requestQueue) {
        val isGrantedNow =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        isGranted.value = isGrantedNow
    }
    return isGranted
}
