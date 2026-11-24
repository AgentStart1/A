/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import kotlin.js.ExperimentalWasmJsInterop

actual suspend fun saveTextToFile(
    suggestedName: String,
    extension: String,
    extensions: Set<String>,
    content: String,
): Boolean {
    require(extension in extensions)
    return requestTextDownload("$suggestedName.$extension", content)
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
    (name, text) => {
        const url = URL.createObjectURL(new Blob([text], {type: "text/plain;charset=utf-8"}));
        const link = document.createElement("a");
        link.href = url;
        link.download = name;
        try {
            document.body.appendChild(link);
            link.click();
            return true;
        } finally {
            link.remove();
            setTimeout(() => URL.revokeObjectURL(url), 1000);
        }
    }
    """,
)
private external fun requestTextDownload(name: String, text: String): Boolean
