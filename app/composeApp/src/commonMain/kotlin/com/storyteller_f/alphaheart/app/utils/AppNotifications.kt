/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.localNotifier
import com.mmk.kmpnotifier.notification.PayloadData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

private val mutableNotificationPermission = MutableStateFlow(false)
private val notificationPermission = mutableNotificationPermission.asStateFlow()
private val notificationClicks = Channel<PayloadData>(Channel.BUFFERED)
private val notificationClickListener =
    object : KMPNotifier.Listener {
        override fun onNotificationClicked(data: PayloadData) {
            notificationClicks.trySend(data)
        }
    }
private var notificationClickListenerRegistered = false

internal fun initializeAppNotifications() {
    initializePlatformAppNotifications()
    if (KMPNotifier.isInitialized && !notificationClickListenerRegistered) {
        KMPNotifier.addListener(notificationClickListener)
        notificationClickListenerRegistered = true
    }
    refreshAppNotificationPermission()
}

internal expect fun initializePlatformAppNotifications()

internal expect fun requestPlatformNotificationPermission(onResult: (Boolean) -> Unit)

internal fun refreshAppNotificationPermission() {
    if (!KMPNotifier.isInitialized) {
        mutableNotificationPermission.value = false
        return
    }
    KMPNotifier.permissionUtil.hasNotificationPermission { isGranted ->
        mutableNotificationPermission.value = isGranted
    }
}

internal fun requestAppNotificationPermission() {
    if (!KMPNotifier.isInitialized) return
    requestPlatformNotificationPermission { isGranted ->
        mutableNotificationPermission.value = isGranted
    }
}

@Composable
internal fun rememberAppNotificationPermission(): Boolean {
    LaunchedEffect(Unit) {
        refreshAppNotificationPermission()
    }
    val isGranted by notificationPermission.collectAsState()
    return isGranted
}

@Composable
internal fun ObserveAppNotificationClicks(onClick: (PayloadData) -> Unit) {
    val currentOnClick by rememberUpdatedState(onClick)
    LaunchedEffect(Unit) {
        notificationClicks.receiveAsFlow().collect { payload ->
            currentOnClick(payload)
        }
    }
}

internal fun showAppNotification(title: String, body: String, payloadData: Map<String, String> = emptyMap()) {
    if (!KMPNotifier.isInitialized) return
    KMPNotifier.localNotifier.notify(
        title = title,
        body = body,
        payloadData = payloadData,
    )
}
