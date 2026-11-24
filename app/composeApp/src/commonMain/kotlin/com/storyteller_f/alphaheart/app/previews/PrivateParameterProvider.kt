/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

class PrivateParameterProvider : PreviewParameterProvider<String> {
    override val values: Sequence<String>
        get() =
            sequence {
                yield(
                    buildString {
                        repeat(50) {
                            append("a")
                        }
                    },
                )
            }
}
