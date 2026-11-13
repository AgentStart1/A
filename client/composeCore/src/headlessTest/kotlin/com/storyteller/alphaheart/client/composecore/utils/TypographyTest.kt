/*
 * This is a private project. All rights reserved.
 */

package com.storyteller.alphaheart.client.composecore.utils

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import com.storyteller_f.alphaheart.client.compose_core.utils.withDefaultFontFamily
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

internal class TypographyTest {
    @Test
    fun keepsTypographyWithoutAFont() {
        val typography = Typography()

        assertSame(typography, typography.withDefaultFontFamily(null))
    }

    @Test
    fun appliesFontToAllStyles() {
        val fontFamily = FontFamily.SansSerif
        val typography = Typography().withDefaultFontFamily(fontFamily)

        val styles =
            listOf(
                typography.displayLarge,
                typography.displayMedium,
                typography.displaySmall,
                typography.headlineLarge,
                typography.headlineMedium,
                typography.headlineSmall,
                typography.titleLarge,
                typography.titleMedium,
                typography.titleSmall,
                typography.bodyLarge,
                typography.bodyMedium,
                typography.bodySmall,
                typography.labelLarge,
                typography.labelMedium,
                typography.labelSmall,
            )

        styles.forEach { assertEquals(fontFamily, it.fontFamily) }
    }
}
