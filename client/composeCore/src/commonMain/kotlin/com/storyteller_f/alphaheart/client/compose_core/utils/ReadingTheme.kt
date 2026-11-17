/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val readingLightColors =
    lightColorScheme(
        primary = Color(0xFF32634D),
        surfaceTint = Color(0xFF32634D),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE4EEE3),
        onPrimaryContainer = Color(0xFF22382E),
        secondary = Color(0xFF63766A),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE4EEE3),
        onSecondaryContainer = Color(0xFF22382E),
        tertiary = Color(0xFF32634D),
        tertiaryContainer = Color(0xFFE4EEE3),
        onTertiaryContainer = Color(0xFF22382E),
        background = Color(0xFFF3F6F2),
        onBackground = Color(0xFF22382E),
        surface = Color(0xFFF3F6F2),
        surfaceDim = Color(0xFFE4EEE3),
        surfaceBright = Color.White,
        onSurface = Color(0xFF22382E),
        surfaceVariant = Color(0xFFE4EEE3),
        onSurfaceVariant = Color(0xFF63766A),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF3F6F2),
        surfaceContainer = Color.White,
        surfaceContainerHigh = Color.White,
        surfaceContainerHighest = Color(0xFFE4EEE3),
        outline = Color(0xFF63766A),
        outlineVariant = Color(0xFFDCE4DD),
        error = Color(0xFFAA3D2B),
        errorContainer = Color(0xFFF8EAE5),
        onErrorContainer = Color(0xFF753024),
    )

private val readingDarkColors =
    darkColorScheme(
        primary = Color(0xFFAED4B5),
        surfaceTint = Color(0xFFAED4B5),
        onPrimary = Color(0xFF183524),
        primaryContainer = Color(0xFF2D4633),
        onPrimaryContainer = Color(0xFFE2EDE5),
        secondary = Color(0xFFADBCAF),
        onSecondary = Color(0xFF183524),
        secondaryContainer = Color(0xFF2D4633),
        onSecondaryContainer = Color(0xFFE2EDE5),
        tertiary = Color(0xFFAED4B5),
        tertiaryContainer = Color(0xFF2D4633),
        onTertiaryContainer = Color(0xFFE2EDE5),
        background = Color(0xFF142019),
        onBackground = Color(0xFFE2EDE5),
        surface = Color(0xFF142019),
        surfaceDim = Color(0xFF142019),
        surfaceBright = Color(0xFF2D4633),
        onSurface = Color(0xFFE2EDE5),
        surfaceVariant = Color(0xFF2D4633),
        onSurfaceVariant = Color(0xFFADBCAF),
        surfaceContainerLowest = Color(0xFF1D2A23),
        surfaceContainerLow = Color(0xFF1D2A23),
        surfaceContainer = Color(0xFF1D2A23),
        surfaceContainerHigh = Color(0xFF1D2A23),
        surfaceContainerHighest = Color(0xFF2D4633),
        outline = Color(0xFFADBCAF),
        outlineVariant = Color(0xFF3A4B40),
    )

fun readingColorScheme(darkTheme: Boolean): ColorScheme = if (darkTheme) readingDarkColors else readingLightColors

val readingShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )

val readingTypography =
    Typography(
        bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 22.4.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.4.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 19.2.sp),
        labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
        titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
        headlineSmall =
        TextStyle(
            fontSize = 26.sp,
            lineHeight = 35.1.sp,
            letterSpacing = (-0.6).sp,
            fontWeight = FontWeight(650),
        ),
    )
