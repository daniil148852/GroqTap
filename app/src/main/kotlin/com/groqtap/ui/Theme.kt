package com.groqtap.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────── Groq colour palette ───────────────
object GroqColors {
    val Orange      = Color(0xFFF55036)   // Groq signature orange
    val OrangeLight = Color(0xFFFF7A5C)
    val OrangeDim   = Color(0xFF3D1A12)   // dark tint for surfaces

    val Bg          = Color(0xFF080808)
    val BgElevated  = Color(0xFF111111)
    val Surface     = Color(0xFF1A1A1A)
    val SurfaceHigh = Color(0xFF232323)

    val TextPrimary   = Color(0xFFF2F2F2)
    val TextSecondary = Color(0xFF888888)
    val TextTertiary  = Color(0xFF505050)

    val Border     = Color(0xFF2A2A2A)
    val BorderFaint = Color(0xFF1E1E1E)

    val UserBubble = Color(0xFF1E1208)   // very dark orange tint
    val UserBubbleBorder = Color(0xFF3D2415)
    val AiBubble   = Color(0xFF141414)
    val AiBubbleBorder = Color(0xFF242424)
}

private val darkColorScheme = darkColorScheme(
    primary          = GroqColors.Orange,
    onPrimary        = Color.White,
    primaryContainer = GroqColors.OrangeDim,
    onPrimaryContainer = GroqColors.OrangeLight,
    secondary        = GroqColors.Surface,
    onSecondary      = GroqColors.TextPrimary,
    background       = GroqColors.Bg,
    onBackground     = GroqColors.TextPrimary,
    surface          = GroqColors.BgElevated,
    onSurface        = GroqColors.TextPrimary,
    surfaceVariant   = GroqColors.Surface,
    onSurfaceVariant = GroqColors.TextSecondary,
    outline          = GroqColors.Border,
    error            = Color(0xFFFF5252),
    onError          = Color.White,
)

val GroqTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 32.sp, lineHeight = 40.sp, color = GroqColors.TextPrimary),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
)

@Composable
fun GroqTapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme,
        typography  = GroqTypography,
        content     = content,
    )
}
