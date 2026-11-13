/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.mikepenz.aboutlibraries.Libs
import com.storyteller_f.alphaheart.app.Res
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalResourceApi::class)
class AboutLibrariesTest {
    @Test
    fun bundledDefinitionsContainDependenciesAndLicenses() =
        runTest {
            val definitions = Res.readBytes("files/aboutlibraries.json").decodeToString()
            val libraries = Libs.Builder().withJson(definitions).build()
            assertTrue(libraries.libraries.any { it.uniqueId.startsWith("org.jetbrains.kotlin:") })
            assertTrue(libraries.licenses.all { it.name.isNotBlank() })
            assertTrue(
                libraries.libraries.first { it.uniqueId == "org.jetbrains.kotlin:kotlin-stdlib" }.licenses.isNotEmpty(),
            )
            assertEquals(libraries.libraries.size, libraries.libraries.map { it.uniqueId }.distinct().size)
        }
}
