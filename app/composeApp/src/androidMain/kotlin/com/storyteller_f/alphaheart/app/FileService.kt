/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.storyteller_f.alphaheart.app.android_library.R.drawable.baseline_upload_file_24
import com.storyteller_f.alphaheart.app.common.Downloader
import com.storyteller_f.alphaheart.app.common.SimpleTaskRegister
import com.storyteller_f.alphaheart.app.common.Uploader
import com.storyteller_f.alphaheart.app.common.UploaderImpl
import com.storyteller_f.shared.getAppContextRefValue
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class FileService : LifecycleService() {
    val channel = "File Service"
    val taskRegister by lazy { SimpleTaskRegister(lifecycleScope) }

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch {
            combine(taskRegister.runningTaskCount, uiViewModel.resourceTasks.runningTaskCount) { uploads, downloads ->
                uploads + downloads
            }.debounce(1000).collectLatest {
                updateNotification(it)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val count = taskRegister.runningTaskCount.value + uiViewModel.resourceTasks.runningTaskCount.value
        updateNotification(count.coerceAtLeast(1))
        return START_NOT_STICKY
    }

    private fun updateNotification(count: Int) {
        Napier.i(tag = "File Service") {
            "task count $count"
        }
        getOrCreateNotificationChannel(this, channel)
        val id = 2
        if (count > 0) {
            val notification =
                NotificationCompat.Builder(this, channel)
                    .setSmallIcon(baseline_upload_file_24)
                    .setContentTitle("Transferring files")
                    .setOngoing(true)
            startForeground(id, notification.build())
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        val uiViewModel = uiViewModel
        return FileBinder(uiViewModel.downloader, UploaderImpl(uiViewModel, taskRegister))
    }
}

class FileBinder(val downloader: Downloader, val uploader: Uploader) :
    Binder(),
    Downloader by downloader,
    Uploader by uploader

interface ClientFileServiceContainer {
    var binder: FileBinder?
    var isConnecting: Boolean
}

class FileConnection(val context: WeakReference<ClientFileServiceContainer>, val clipData: ImmutableList<ClipFile>) :
    ServiceConnection {
    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
        val binder = checkNotNull(p1) { "File service connected without a binder" } as FileBinder
        binder.upload(clipData)
        val container = context.get() ?: return
        container.binder = binder
        container.isConnecting = false
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        context.get()?.binder = null
    }
}

fun <T> T.bindFileService(
    clipFiles: ImmutableList<ClipFile>,
)
    where T : ComponentActivity, T : ClientFileServiceContainer {
    isConnecting = true
    val serviceIntent = Intent(this, FileService::class.java)
    val connection = FileConnection(WeakReference(this), clipFiles)
    bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                unbindService(connection)
            }
        },
    )
}

class CustomClientFileProvider<T>(val service: T) :
    ClientFileProvider where T : ComponentActivity, T : ClientFileServiceContainer {
    override suspend fun getDownloader(): Downloader? {
        val binder = service.binder ?: awaitBinder()
        return binder?.downloader
    }

    private suspend fun awaitBinder(): FileBinder? {
        if (!service.isConnecting) {
            service.bindFileService(persistentListOf())
        }
        while (service.isConnecting) {
            delay(100)
        }
        val binder = service.binder
        if (binder == null) {
            Napier.w(tag = "file") {
                "FileService binder is null after connect"
            }
        }
        return binder
    }

    override suspend fun getUploader(): Uploader? {
        val binder = service.binder ?: awaitBinder()
        return binder?.uploader
    }
}

/** Observe application tasks without transferring their ownership to a bound Activity or Service. */
internal fun observeResourceTransfers(viewModel: UIViewModel) {
    viewModel.viewModelScope.launch(Dispatchers.Main) {
        viewModel.resourceTasks.runningTaskCount
            .map { it > 0 }.distinctUntilChanged().collect { active ->
                if (active) {
                    val context = checkNotNull(getAppContextRefValue()).applicationContext
                    ContextCompat.startForegroundService(context, Intent(context, FileService::class.java))
                }
            }
    }
}
