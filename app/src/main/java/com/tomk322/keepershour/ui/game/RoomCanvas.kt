@file:Suppress("MagicNumber")

package com.tomk322.keepershour.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.tomk322.keepershour.engine.world.Interactable
import com.tomk322.keepershour.engine.world.Room
import com.tomk322.keepershour.engine.world.Tile
import com.tomk322.keepershour.ui.LampAmber
import kotlin.math.min
import kotlin.math.sin

/** How many tiles of the room fit across the shorter screen edge. */
private const val TILES_IN_VIEW = 11f

private val FloorBase = Color(0xFF262E3C)
private val FloorAlt = Color(0xFF222A37)
private val WallFace = Color(0xFF141A24)
private val WallEdge = Color(0xFF39445A)
private val Void = Color(0xFF090C12)

/**
 * Draws one room from the keeper's point of view: a lantern's worth of floor, and the dark
 * pressing in on all sides of it.
 *
 * The darkness is doing double duty. It is the right look for a lighthouse at midnight, and it
 * keeps the drawn-in-code furniture reading as suggestion rather than as bad pixel art.
 */
data class RoomView(
    val room: Room,
    val playerX: Float,
    val playerY: Float,
    val facing: Offset,
    val walkPhase: Float,
    val highlighted: Interactable?,
    val lampLit: Boolean,
    val raining: Boolean,
)

@Composable
fun RoomCanvas(view: RoomView, modifier: Modifier = Modifier) {
    val room = view.room
    val lampLit = view.lampLit
    Canvas(modifier = modifier.fillMaxSize()) {
        val tile = min(size.width, size.height) / TILES_IN_VIEW
        val origin = Offset(
            x = size.width / 2f - view.playerX * tile,
            y = size.height / 2f - view.playerY * tile,
        )
        fun rectFor(tiles: Set<Tile>): Rect {
            val minX = tiles.minOf { it.x }
            val minY = tiles.minOf { it.y }
            val maxX = tiles.maxOf { it.x }
            val maxY = tiles.maxOf { it.y }
            return Rect(
                offset = origin + Offset((minX - 0.5f) * tile, (minY - 0.5f) * tile),
                size = Size((maxX - minX + 1) * tile, (maxY - minY + 1) * tile),
            )
        }

        drawRect(Void, Offset.Zero, size)
        drawFloorAndWalls(room, origin, tile)

        for (prop in room.props) {
            drawProp(prop.kind, rectFor(prop.footprint).deflate(tile * 0.06f), lampLit)
        }
        for (thing in room.interactables) {
            val area = rectFor(thing.footprint).deflate(tile * 0.06f)
            drawProp(thing.kind, area, lampLit)
            if (thing.id == view.highlighted?.id) {
                drawRect(
                    color = LampAmber.copy(alpha = 0.85f),
                    topLeft = area.topLeft - Offset(tile * 0.1f, tile * 0.1f),
                    size = Size(area.width + tile * 0.2f, area.height + tile * 0.2f),
                    style = Stroke(tile * 0.06f),
                )
            }
        }

        val centre = origin + Offset(view.playerX * tile, view.playerY * tile)
        drawKeeper(centre, tile, bob = sin(view.walkPhase) * tile * 0.05f, facing = view.facing)
        drawLantern(centre, tile, lampLit)
        if (view.raining) drawRain(view.walkPhase)
    }
}

private fun DrawScope.drawFloorAndWalls(room: Room, origin: Offset, tile: Float) {
    for (y in 0 until Room.GRID) {
        for (x in 0 until Room.GRID) {
            val at = Tile(x, y)
            val topLeft = origin + Offset((x - 0.5f) * tile, (y - 0.5f) * tile)
            val cell = Size(tile, tile)
            if (room.isFloor(at)) {
                // A deterministic wobble stops the stone floor reading as graph paper.
                drawRect(if ((x * 7 + y * 13) % 5 < 2) FloorAlt else FloorBase, topLeft, cell)
                drawRect(Color(0x14000000), topLeft, Size(tile, tile * 0.06f))
            } else if (touchesFloor(room, at)) {
                drawRect(WallFace, topLeft, cell)
                drawRect(WallEdge.copy(alpha = 0.35f), topLeft, Size(tile, tile * 0.14f))
            }
        }
    }
}

private fun touchesFloor(room: Room, at: Tile): Boolean =
    (-1..1).any { dx ->
        (-1..1).any { dy -> room.isFloor(Tile(at.x + dx, at.y + dy)) }
    }

/** The lantern pool, and the dark everywhere it does not reach. */
private fun DrawScope.drawLantern(centre: Offset, tile: Float, lampLit: Boolean) {
    val reach = tile * if (lampLit) 6.5f else 4.2f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x00000000),
                Color(0x33060A10),
                Color(0xC4060A10),
                Color(0xF2060A10),
            ),
            center = centre,
            radius = reach,
        ),
        radius = reach,
        center = centre,
    )
    // Beyond the gradient's edge the room is simply not there.
    drawRect(Color(0xF2060A10), Offset.Zero, Size(size.width, (centre.y - reach).coerceAtLeast(0f)))
    drawRect(
        color = Color(0xF2060A10),
        topLeft = Offset(0f, (centre.y + reach).coerceAtMost(size.height)),
        size = Size(size.width, (size.height - centre.y - reach).coerceAtLeast(0f)),
    )
    drawRect(Color(0xF2060A10), Offset.Zero, Size((centre.x - reach).coerceAtLeast(0f), size.height))
    drawRect(
        color = Color(0xF2060A10),
        topLeft = Offset((centre.x + reach).coerceAtMost(size.width), 0f),
        size = Size((size.width - centre.x - reach).coerceAtLeast(0f), size.height),
    )
    drawCircle(LampAmber.copy(alpha = 0.055f), reach * 0.55f, centre)
}

private fun DrawScope.drawRain(phase: Float) {
    val drops = 70
    for (i in 0 until drops) {
        val seed = i * 977 % 1000 / 1000f
        val x = ((seed * 1.7f + phase * 0.012f) % 1f) * size.width
        val y = ((seed * 3.1f + phase * 0.09f) % 1f) * size.height
        drawLine(
            color = Color(0x33AFC6DB),
            start = Offset(x, y),
            end = Offset(x - size.width * 0.012f, y + size.height * 0.045f),
            strokeWidth = 1.6f,
        )
    }
}

private fun Rect.deflate(by: Float) = Rect(left + by, top + by, right - by, bottom - by)
