// A renderer is nothing but tuned numbers; naming each offset would obscure the drawing.
@file:Suppress("MagicNumber")

package com.tomk322.keepershour.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.tomk322.keepershour.engine.world.PropKind
import com.tomk322.keepershour.ui.LampAmber
import com.tomk322.keepershour.ui.LampGlow
import kotlin.math.cos
import kotlin.math.sin

private val StoneDark = Color(0xFF1A202B)
private val Timber = Color(0xFF4A3B2C)
private val TimberDark = Color(0xFF33291E)
private val Iron = Color(0xFF3C4655)
private val IronBright = Color(0xFF5A6779)
private val Cloth = Color(0xFF3A3F52)
private val Brass = Color(0xFF8A6B3A)

/** Draws one piece of furniture, centred in the rect its tiles occupy. */
@Suppress("CyclomaticComplexMethod", "LongMethod")
fun DrawScope.drawProp(kind: PropKind, area: Rect, lit: Boolean) {
    val w = area.width
    val h = area.height
    when (kind) {
        PropKind.LENS -> drawLens(area, lit)
        PropKind.WINDOW -> {
            drawRect(StoneDark, area.topLeft, area.size)
            drawRect(
                if (lit) LampGlow.copy(alpha = 0.5f) else Color(0xFF141B26),
                Offset(area.left, area.top + h * 0.3f),
                Size(w, h * 0.35f),
            )
        }
        PropKind.LINE_HOOK -> {
            drawCircle(Iron, w * 0.16f, area.center)
            drawArc(
                color = Brass, startAngle = 20f, sweepAngle = 300f, useCenter = false,
                topLeft = area.topLeft + Offset(w * 0.2f, h * 0.2f),
                size = Size(w * 0.6f, h * 0.6f), style = Stroke(w * 0.09f),
            )
        }
        PropKind.DOOR -> {
            drawRect(Timber, area.topLeft, area.size)
            drawRect(TimberDark, area.topLeft, area.size, style = Stroke(w * 0.06f))
            drawCircle(Brass, w * 0.07f, area.center + Offset(w * 0.25f, 0f))
        }
        PropKind.STAIRS_UP, PropKind.STAIRS_DOWN -> drawStairs(area, kind == PropKind.STAIRS_UP)
        PropKind.DESK -> {
            drawRect(Timber, area.topLeft, area.size)
            drawLine(TimberDark, Offset(area.left, area.center.y), Offset(area.right, area.center.y), w * 0.05f)
            drawCircle(Brass, w * 0.05f, area.center + Offset(0f, h * 0.22f))
        }
        PropKind.CHEST -> {
            drawRect(TimberDark, area.topLeft, area.size)
            drawRect(Timber, area.topLeft + Offset(0f, h * 0.15f), Size(w, h * 0.2f))
            drawCircle(Brass, w * 0.09f, area.center + Offset(0f, h * 0.1f))
        }
        PropKind.BUNK -> {
            drawRect(Iron, area.topLeft, area.size)
            drawRect(Cloth, area.topLeft + Offset(w * 0.1f, h * 0.1f), Size(w * 0.8f, h * 0.5f))
        }
        PropKind.FLAGSTONE -> {
            drawRect(Color(0xFF232A36), area.topLeft, area.size)
            drawRect(Color(0xFF3B4658), area.topLeft, area.size, style = Stroke(w * 0.08f))
        }
        PropKind.TABLE -> {
            drawRect(Timber, area.topLeft + Offset(0f, h * 0.1f), Size(w, h * 0.8f))
            drawCircle(Color(0xFF6B7280), w * 0.09f, area.center + Offset(-w * 0.2f, 0f))
        }
        PropKind.PEGS -> {
            drawRect(TimberDark, area.topLeft, Size(w, h * 0.2f))
            for (i in 0..2) {
                val x = area.left + w * (0.2f + i * 0.3f)
                drawRect(Cloth, Offset(x - w * 0.09f, area.top + h * 0.2f), Size(w * 0.18f, h * 0.65f))
            }
        }
        PropKind.RANGE -> {
            drawRect(Iron, area.topLeft, area.size)
            for (i in 0..1) for (j in 0..1) {
                drawCircle(StoneDark, w * 0.11f, area.topLeft + Offset(w * (0.3f + i * 0.4f), h * (0.3f + j * 0.4f)))
            }
        }
        PropKind.LOCKER -> {
            drawRect(Iron, area.topLeft, area.size)
            drawLine(StoneDark, Offset(area.center.x, area.top), Offset(area.center.x, area.bottom), w * 0.05f)
            drawCircle(Brass, w * 0.06f, area.center + Offset(w * 0.12f, 0f))
        }
        PropKind.HATCH -> {
            drawRect(TimberDark, area.topLeft, area.size)
            drawRect(Timber, area.topLeft, area.size, style = Stroke(w * 0.1f))
            drawCircle(Brass, w * 0.13f, area.center, style = Stroke(w * 0.06f))
        }
        PropKind.CRATE -> {
            drawRect(Timber, area.topLeft, area.size)
            drawLine(TimberDark, area.topLeft, Offset(area.right, area.bottom), w * 0.05f)
            drawLine(TimberDark, Offset(area.right, area.top), Offset(area.left, area.bottom), w * 0.05f)
        }
        PropKind.CASK -> {
            drawCircle(Timber, minOf(w, h) * 0.42f, area.center)
            drawCircle(TimberDark, minOf(w, h) * 0.42f, area.center, style = Stroke(w * 0.05f))
            drawCircle(TimberDark, minOf(w, h) * 0.14f, area.center)
        }
        PropKind.WATER_BUTT -> {
            drawCircle(Iron, minOf(w, h) * 0.45f, area.center)
            drawCircle(Color(0xFF20303F), minOf(w, h) * 0.32f, area.center)
        }
        PropKind.FOGHORN -> drawFoghorn(area)
        PropKind.RAIL -> {
            for (i in 0..1) {
                val y = area.top + h * (0.3f + i * 0.35f)
                drawLine(IronBright, Offset(area.left, y), Offset(area.right, y), h * 0.09f)
            }
            drawLine(Iron, Offset(area.center.x, area.top), Offset(area.center.x, area.bottom), w * 0.06f)
        }
        PropKind.KELL -> drawKell(area)
    }
}

private fun DrawScope.drawLens(area: Rect, lit: Boolean) {
    val r = minOf(area.width, area.height) * 0.44f
    if (lit) {
        drawCircle(LampGlow.copy(alpha = 0.35f), r * 2.4f, area.center)
        drawCircle(LampAmber.copy(alpha = 0.55f), r * 1.5f, area.center)
    }
    drawCircle(Iron, r, area.center)
    drawCircle(if (lit) LampGlow else Color(0xFF3E4A5C), r * 0.72f, area.center)
    for (i in 0 until 8) {
        val a = i * (Math.PI.toFloat() / 4f)
        drawLine(
            color = if (lit) LampAmber else Iron,
            start = area.center,
            end = area.center + Offset(cos(a) * r, sin(a) * r),
            strokeWidth = r * 0.09f,
        )
    }
    drawCircle(if (lit) Color.White else Color(0xFF29323F), r * 0.24f, area.center)
}

private fun DrawScope.drawStairs(area: Rect, up: Boolean) {
    drawRect(StoneDark, area.topLeft, area.size)
    val steps = 4
    val near = if (up) 0.30f else 0.34f
    val far = if (up) 0.60f else 0.18f
    for (i in 0 until steps) {
        val t = i / (steps - 1f)
        val shade = near + (far - near) * t
        drawRect(
            color = Color(shade * 0.55f, shade * 0.62f, shade * 0.78f, 1f),
            topLeft = Offset(area.left, area.top + area.height * (i / steps.toFloat())),
            size = Size(area.width, area.height / steps),
        )
    }
    val arrow = Path().apply {
        val cx = area.center.x
        val top = if (up) area.top + area.height * 0.22f else area.bottom - area.height * 0.22f
        val bottom = if (up) area.bottom - area.height * 0.28f else area.top + area.height * 0.28f
        moveTo(cx, top)
        lineTo(cx - area.width * 0.16f, bottom)
        lineTo(cx + area.width * 0.16f, bottom)
        close()
    }
    drawPath(arrow, Brass.copy(alpha = 0.85f))
}

private fun DrawScope.drawFoghorn(area: Rect) {
    val path = Path().apply {
        moveTo(area.left + area.width * 0.15f, area.center.y - area.height * 0.18f)
        lineTo(area.right - area.width * 0.1f, area.top + area.height * 0.08f)
        lineTo(area.right - area.width * 0.1f, area.bottom - area.height * 0.08f)
        lineTo(area.left + area.width * 0.15f, area.center.y + area.height * 0.18f)
        close()
    }
    drawPath(path, Brass.copy(alpha = 0.8f))
    drawCircle(Iron, area.height * 0.2f, Offset(area.left + area.width * 0.15f, area.center.y))
}

private fun DrawScope.drawKell(area: Rect) {
    val cx = area.center.x
    val cy = area.center.y
    val s = minOf(area.width, area.height)
    drawCircle(Color(0xFF1B2130), s * 0.52f, Offset(cx, cy + s * 0.06f))
    drawCircle(Color(0xFF564A3C), s * 0.40f, Offset(cx, cy + s * 0.08f))
    drawCircle(Color(0xFFB08968), s * 0.24f, Offset(cx, cy - s * 0.16f))
}

/** Mairead, with the lantern she is never without. */
fun DrawScope.drawKeeper(centre: Offset, tile: Float, bob: Float, facing: Offset) {
    val s = tile
    drawOval(
        color = Color(0x66000000),
        topLeft = centre + Offset(-s * 0.30f, s * 0.20f),
        size = Size(s * 0.60f, s * 0.24f),
    )
    val body = centre + Offset(0f, bob)
    drawCircle(Color(0xFF243040), s * 0.36f, body + Offset(0f, s * 0.04f))
    drawCircle(Color(0xFF37506B), s * 0.30f, body + Offset(0f, s * 0.02f))
    drawCircle(Color(0xFFC9A227), s * 0.10f, body + Offset(facing.x * s * 0.34f, facing.y * s * 0.34f))
    drawCircle(Color(0xFFE8C07D), s * 0.19f, body + Offset(0f, -s * 0.22f))
}
