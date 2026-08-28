// Deep-sea palette: the numbers here are the design, not magic constants to be named away.
@file:Suppress("MagicNumber")

package com.tomk322.keepershour.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Lamp amber - the one warm colour on the rock. */
val LampAmber = Color(0xFFF0B860)
val LampGlow = Color(0xFFFFD79A)
val SeaDeep = Color(0xFF07090F)
val SeaSurface = Color(0xFF10141E)
val SeaRaised = Color(0xFF19202D)
val Foam = Color(0xFFD8DEE9)
val FoamDim = Color(0xFF8A94A6)
val Rust = Color(0xFFC5553D)
val Verdigris = Color(0xFF5E9E92)

private val Scheme = darkColorScheme(
    primary = LampAmber,
    onPrimary = SeaDeep,
    secondary = Verdigris,
    background = SeaDeep,
    onBackground = Foam,
    surface = SeaSurface,
    onSurface = Foam,
    surfaceVariant = SeaRaised,
    onSurfaceVariant = FoamDim,
    error = Rust,
    outline = Color(0xFF283245),
)

/** Serif for prose - it is 1907 - and monospace for anything the player reads as an instrument. */
val Prose = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 17.sp,
    lineHeight = 27.sp,
)

val ProseEmphasis = Prose.copy(fontWeight = FontWeight.Medium, color = LampGlow)

val ChoiceLabel = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 16.sp,
    lineHeight = 22.sp,
)

/** Instrument text without the tracking, for notes that need to read as sentences. */
val Footnote = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = 12.sp,
    lineHeight = 16.sp,
)

val Clock = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 16.sp,
    letterSpacing = 1.sp,
    fontWeight = FontWeight.Bold,
)

val Instrument = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp,
    letterSpacing = 1.sp,
)

val Display = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 2.sp,
)

@Composable
fun KeepersHourTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = Typography(),
        content = content,
    )
}
