/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.service

import kotlinx.io.files.Path
import kotlinx.io.files.SystemTemporaryDirectory

actual fun buildGPT(): GPT = NoOpGPT()

actual fun getGPTModelDirectory(): Path = Path(SystemTemporaryDirectory, "gpt-models")
