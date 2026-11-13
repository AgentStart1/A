/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

fun getOrCreateNotificationChannel(context: Context, channel: String): NotificationManagerCompat {
    val notificationManager = NotificationManagerCompat.from(context)
    val notificationChannel = notificationManager.getNotificationChannel(channel)
    if (notificationChannel == null) {
        notificationManager.deleteNotificationChannel(channel)
    }
    val channelBuilder =
        NotificationChannelCompat.Builder(channel, NotificationManagerCompat.IMPORTANCE_LOW)
            .setSound(null, null)
            .setVibrationEnabled(true)
    if (channel == "Regular") {
        channelBuilder.setName("Regular")
        channelBuilder.setDescription("Regular")
    } else {
        channelBuilder.setName("Upload")
        channelBuilder.setDescription("Upload")
    }

    notificationManager.createNotificationChannel(channelBuilder.build())
    return notificationManager
}
