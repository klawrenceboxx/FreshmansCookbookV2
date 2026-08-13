package com.kaleel.freshmanscookbook.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Herb = Color(0xFF226B46)
val HerbDark = Color(0xFF16482F)
val MintWash = Color(0xFFEAF4EE)
val Ink = Color(0xFF17221B)
val Muted = Color(0xFF66736A)
val Line = Color(0xFFE1E7E3)
val Paper = Color(0xFFFFFFFF)
val WarmPlaceholder = Color(0xFFF3F0E9)

private val colors = lightColorScheme(
    primary = Herb,
    onPrimary = Color.White,
    primaryContainer = MintWash,
    onPrimaryContainer = HerbDark,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF5F7F5),
    onSurfaceVariant = Muted,
    outline = Line,
    error = Color(0xFFB3261E)
)

private val typography = Typography(
    displaySmall = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp),
    headlineLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 39.sp),
    headlineMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 31.sp),
    titleLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 21.sp),
    labelLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = .4.sp)
)

@Composable
fun CookbookTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
