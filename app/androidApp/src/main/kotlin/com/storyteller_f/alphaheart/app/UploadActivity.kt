/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import com.storyteller_f.alphaheart.client.compose_core.commonForActivity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

class UploadActivity :
    ComponentActivity(),
    ClientFileServiceContainer {
    override var binder: FileBinder? = null
    override var isConnecting: Boolean = false
    val receiver = CustomClientFileProvider(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        commonForActivity()
        downloadFromIntent()
        setContent {
            CompositionLocalProvider(
                LocalClientFileProvider provides receiver,
                LocalUiViewModel provides uiViewModel,
            ) {
                UploadPage()
            }
        }
    }

    private fun getClipFiles(): ImmutableList<ClipFile> {
        val clipData = intent.clipData
        return if (clipData != null) {
            List(clipData.itemCount) {
                getClipFile(this, clipData.getItemAt(it).uri)
            }.filterNotNull().toImmutableList()
        } else {
            persistentListOf()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        downloadFromIntent()
    }

    private fun downloadFromIntent() {
        val clipData = getClipFiles()
        lifecycleScope.launch {
            receiver.getUploader()?.upload(clipData)
        }
    }
}
