/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.LocalNotifications
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration

internal actual fun initializePlatformAppNotifications() {
    if (!KMPNotifier.isInitialized) {
        KMPNotifier.initialize(
            configuration =
            NotificationPlatformConfiguration.Desktop(
                showPushNotification = false,
                notificationIconPath = null,
            ),
            LocalNotifications,
        )
    }
}

internal actual fun requestPlatformNotificationPermission(onResult: (Boolean) -> Unit) {
    KMPNotifier.permissionUtil.askNotificationPermission(onResult)
}
