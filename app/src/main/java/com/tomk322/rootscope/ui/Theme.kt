// A colour palette is nothing but literals; naming each ARGB value adds no meaning.
@file:Suppress("MagicNumber")

package com.tomk322.rootscope.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tomk322.rootscope.model.Tone

private val Terminal = Color(0xFF3DDC84)
private val Amber = Color(0xFFFFB020)
private val Crimson = Color(0xFFFF5370)
private val Cyan = Color(0xFF4FC3F7)

private val DarkScheme = darkColorScheme(
    primary = Terminal,
    onPrimary = Color(0xFF04150B),
    secondary = Cyan,
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFE3E8EF),
    surface = Color(0xFF131922),
    onSurface = Color(0xFFE3E8EF),
    surfaceVariant = Color(0xFF1C2430),
    onSurfaceVariant = Color(0xFF9AA7B8),
    error = Crimson,
    outline = Color(0xFF2A3542),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF10794A),
    secondary = Color(0xFF0A6EA1),
    background = Color(0xFFF5F7FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EDF3),
    onSurfaceVariant = Color(0xFF52606E),
    error = Color(0xFFB3261E),
    outline = Color(0xFFCBD5E1),
)

/** Values are dense and often hex/paths, so everything numeric renders monospaced. */
private val AppTypography = Typography().let { base ->
    base.copy(
        bodyMedium = base.bodyMedium.copy(fontSize = 14.sp),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.8.sp),
    )
}

val MonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp,
    lineHeight = 18.sp,
)

val MonoBoldStyle = MonoStyle.copy(fontWeight = FontWeight.Bold)

@Composable
fun RootscopeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content,
    )
}

/** Maps a semantic [Tone] onto the active scheme so both themes stay readable. */
@Composable
fun Tone.color(): Color = when (this) {
    Tone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    Tone.GOOD -> MaterialTheme.colorScheme.primary
    Tone.WARN -> if (isSystemInDarkTheme()) Amber else Color(0xFF8A5A00)
    Tone.BAD -> MaterialTheme.colorScheme.error
    Tone.ACCENT -> MaterialTheme.colorScheme.secondary
}
