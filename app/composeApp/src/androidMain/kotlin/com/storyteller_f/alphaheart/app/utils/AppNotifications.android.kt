/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.extensions.initialize
import com.mmk.kmpnotifier.local.LocalNotifications
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.mmk.kmpnotifier.permission.AndroidPermissionUtil
import com.mmk.kmpnotifier.permission.permissionUtil
import com.storyteller_f.alphaheart.app.android_library.R
import com.storyteller_f.alphaheart.client.compose_core.components.mainActivityRef

private var androidNotificationPermissionUtil: AndroidPermissionUtil? = null

internal actual fun initializePlatformAppNotifications() {
    val activity = mainActivityRef?.get() ?: return
    if (androidNotificationPermissionUtil == null) {
        androidNotificationPermissionUtil = activity.permissionUtil().value
    }
    if (!KMPNotifier.isInitialized) {
        KMPNotifier.initialize(
            context = activity,
            configuration =
            NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_notify,
                notificationChannelData =
                NotificationPlatformConfiguration.Android.NotificationChannelData(
                    id = "Regular",
                    name = "Regular",
                    description = "Regular",
                ),
            ),
            LocalNotifications,
        )
    }
}

internal actual fun requestPlatformNotificationPermission(onResult: (Boolean) -> Unit) {
    androidNotificationPermissionUtil?.askNotificationPermission(onResult) ?: onResult(false)
}
