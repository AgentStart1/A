/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.coroutineScope
import com.storyteller_f.alphaheart.client.core.addDevice
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

class PushServiceImpl :
    PushService(),
    LifecycleOwner {
    val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = registry

    override fun onCreate() {
        super.onCreate()
        registry.currentState = Lifecycle.State.STARTED
    }

    override fun onMessage(message: PushMessage, instance: String) {
        Napier.i(tag = "push") {
            "receive message $message"
        }
        val channel = "Push"
        val notificationManager = getOrCreateNotificationChannel(this, channel)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val notification =
                NotificationCompat.Builder(this, channel)
                    .setSmallIcon(com.storyteller_f.alphaheart.app.android_library.R.drawable.ic_notify)
                    .setContentTitle("New message")
            notificationManager.notify(notifyId.getAndIncrement(), notification.build())
        }
    }

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        Napier.i(tag = "push") {
            "receive endpoint $endpoint"
        }
        application as AApplication
        val uiViewModel = uiViewModel
        val accountInstance = uiViewModel.instance.value
        lifecycle.coroutineScope.launch {
            accountInstance.sessionManager.addDevice(endpoint.url)
        }
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        Napier.i(tag = "push") {
            "receive failed $reason"
        }
    }

    override fun onUnregistered(instance: String) {
        Napier.i(tag = "push") {
            "receive unregistered"
        }
        val context = this
        UnifiedPush.getDistributors(context).firstOrNull()?.let { instance ->
            UnifiedPush.saveDistributor(context, instance)
            UnifiedPush.register(context, instance, "Alpha Heart")
            instance
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
