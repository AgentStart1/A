/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import com.mikepenz.aboutlibraries.entity.Scm
import com.storyteller_f.alphaheart.app.common.LibrariesState
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class AboutLibrariesLabelsTest {
    @Test
    fun expandedLibraryActionsAndLicenseConfirmationFollowTheSystemLanguage() {
        val originalLocale = Locale.getDefault()
        try {
            for (chinese in listOf(false, true)) {
                Locale.setDefault(if (chinese) Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH)
                runComposeUiTest {
                    val opened = mutableListOf<String>()
                    setContent {
                        MaterialTheme {
                            ReadingInteractions {
                                CompositionLocalProvider(LocalUriHandler provides recordingUriHandler(opened)) {
                                    Box(Modifier.width(390.dp)) {
                                        LibrariesContent(LibrariesState.Ready(libraryFixture())) {}
                                    }
                                }
                            }
                        }
                    }
                    onNodeWithText("Synthetic library").performClick()
                    onNodeWithText(if (chinese) "源代码仓库" else "Source repository").assertIsDisplayed()
                    onNodeWithText(if (chinese) "项目网站" else "Website").assertIsDisplayed()
                    onNodeWithText(if (chinese) "查看许可" else "View license").assertIsDisplayed().performClick()
                    onNodeWithText("Synthetic license body").assertIsDisplayed()
                    runOnIdle { assertEquals(emptyList(), opened) }
                    onNodeWithText(if (chinese) "确定" else "OK").assertIsDisplayed().performClick()
                    onNodeWithText("Synthetic license body").assertDoesNotExist()
                    onNodeWithText("Synthetic library").assertExists()
                }
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun urlOnlyLicenseAndProjectLinksKeepTheirOriginalExternalTargets() {
        val originalLocale = Locale.getDefault()
        try {
            for (chinese in listOf(false, true)) {
                Locale.setDefault(if (chinese) Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH)
                runComposeUiTest {
                    val opened = mutableListOf<String>()
                    setContent {
                        MaterialTheme {
                            ReadingInteractions {
                                CompositionLocalProvider(LocalUriHandler provides recordingUriHandler(opened)) {
                                    Box(Modifier.width(390.dp)) {
                                        LibrariesContent(LibrariesState.Ready(libraryFixture(urlOnly = true))) {}
                                    }
                                }
                            }
                        }
                    }
                    onNodeWithText("Synthetic library").performClick()
                    onNodeWithText(if (chinese) "源代码仓库" else "Source repository").performClick()
                    onNodeWithText(if (chinese) "项目网站" else "Website").performClick()
                    onNodeWithText(if (chinese) "查看许可" else "View license").performClick()
                    runOnIdle {
                        assertEquals(
                            listOf("https://example.com/source", "https://example.com", "https://example.com/license"),
                            opened,
                        )
                    }
                    onAllNodes(isDialog()).assertCountEquals(0)
                    onNodeWithText("Synthetic license body").assertDoesNotExist()
                    onNodeWithText("Synthetic library").assertExists()
                }
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}

private fun recordingUriHandler(opened: MutableList<String>): UriHandler =
    object : UriHandler {
        override fun openUri(uri: String) {
            opened.add(uri)
        }
    }

private fun libraryFixture(urlOnly: Boolean = false): Libs {
    val license =
        License(
            "MIT",
            if (urlOnly) "https://example.com/license" else null,
            licenseContent = if (urlOnly) null else "Synthetic license body",
            hash = "mit",
        )
    val library =
        Library(
            uniqueId = "synthetic:library",
            artifactVersion = "1.0",
            name = "Synthetic library",
            description = "Synthetic description",
            website = "https://example.com",
            developers = emptyList(),
            organization = null,
            scm = Scm(null, null, "https://example.com/source"),
            licenses = setOf(license),
        )
    return Libs(listOf(library), setOf(license))
}
