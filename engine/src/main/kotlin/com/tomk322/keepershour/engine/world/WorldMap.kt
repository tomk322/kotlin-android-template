package com.tomk322.keepershour.engine.world

import kotlin.math.hypot
import kotlin.math.roundToInt

/** A cell on a room's grid. Every room shares a grid size so the camera never special-cases. */
data class Tile(val x: Int, val y: Int) {
    fun distanceTo(px: Float, py: Float): Float = hypot(px - x, py - y)
}

/** What a thing looks like. The renderer picks shapes from this; the engine does not care. */
enum class PropKind {
    LENS, WINDOW, LINE_HOOK, DOOR, STAIRS_UP, STAIRS_DOWN, DESK, CHEST, BUNK,
    FLAGSTONE, TABLE, PEGS, RANGE, LOCKER, HATCH, CRATE, CASK, WATER_BUTT, FOGHORN, RAIL, KELL,
}

/** Scenery. Blocks movement, does nothing else. */
data class Prop(val kind: PropKind, val footprint: Set<Tile>)

/**
 * Something the player can walk up to and act on.
 *
 * [choiceIds] name choices in the room's own scene. Interacting invents no behaviour of its own:
 * it hands those ids to the engine, so the world is a way of *reaching* the story rather than a
 * second copy of it.
 */
data class Interactable(
    val id: String,
    val label: String,
    val kind: PropKind,
    val footprint: Set<Tile>,
    val choiceIds: List<String>,
    /** Floor features like a loose flagstone are stood on, not walked around. */
    val solid: Boolean = true,
) {
    fun nearestDistance(px: Float, py: Float): Float = footprint.minOf { it.distanceTo(px, py) }
}

/** Interior shape. The tower is round; the gallery wraps around the outside of it. */
sealed interface Floorplan {
    data class Disc(val radius: Float) : Floorplan
    data class Ring(val inner: Float, val outer: Float) : Floorplan
}

data class Room(
    val id: String,
    val name: String,
    val plan: Floorplan,
    val props: List<Prop> = emptyList(),
    val interactables: List<Interactable> = emptyList(),
) {
    private val blocked: Set<Tile> = buildSet {
        props.forEach { addAll(it.footprint) }
        interactables.filter { it.solid }.forEach { addAll(it.footprint) }
    }

    fun isFloor(tile: Tile): Boolean {
        val distance = hypot(tile.x - CENTRE, tile.y - CENTRE)
        return when (plan) {
            is Floorplan.Disc -> distance <= plan.radius
            is Floorplan.Ring -> distance >= plan.inner && distance <= plan.outer
        }
    }

    fun isBlocked(tile: Tile): Boolean = tile in blocked

    /** True when a point is on open floor. */
    fun isWalkable(x: Float, y: Float): Boolean {
        val tile = Tile(x.roundToInt(), y.roundToInt())
        return isFloor(tile) && !isBlocked(tile)
    }

    /**
     * True when a body of [radius] fits here.
     *
     * Tiles are centred on integers, so a body straddling a boundary overlaps two of them; testing
     * every tile the bounding box touches is what stops the keeper's shoulders clipping into
     * stone. A point test alone would let her stand half inside a wall.
     */
    fun canOccupy(x: Float, y: Float, radius: Float): Boolean {
        for (tx in (x - radius).roundToInt()..(x + radius).roundToInt()) {
            for (ty in (y - radius).roundToInt()..(y + radius).roundToInt()) {
                val tile = Tile(tx, ty)
                if (!isFloor(tile) || isBlocked(tile)) return false
            }
        }
        return true
    }

    fun walkableTiles(): List<Tile> = (0 until GRID).flatMap { y ->
        (0 until GRID).map { x -> Tile(x, y) }
    }.filter { isFloor(it) && !isBlocked(it) }

    fun interactableNear(x: Float, y: Float): Interactable? = interactables
        .map { it to it.nearestDistance(x, y) }
        .filter { (_, distance) -> distance <= INTERACT_RANGE }
        .minByOrNull { (_, distance) -> distance }
        ?.first

    /**
     * Where to drop the player on arrival: beside whichever door or stair leads back to
     * [fromRoom], so walking through a doorway puts you on the other side of it.
     */
    fun arrivalPoint(fromRoom: String?, targetOf: (String) -> String?): Pair<Float, Float> {
        val wayBack = interactables.firstOrNull { thing ->
            thing.choiceIds.any { targetOf(it) == fromRoom }
        }
        val anchor = wayBack?.footprint?.firstOrNull() ?: return firstOpenTile()
        val stepX = (CENTRE - anchor.x).coerceIn(-1f, 1f)
        val stepY = (CENTRE - anchor.y).coerceIn(-1f, 1f)
        val candidate = (anchor.x + stepX) to (anchor.y + stepY)
        return if (canOccupy(candidate.first, candidate.second, BODY_RADIUS)) {
            candidate
        } else {
            firstOpenTile()
        }
    }

    private fun firstOpenTile(): Pair<Float, Float> =
        walkableTiles()
            .filter { canOccupy(it.x.toFloat(), it.y.toFloat(), BODY_RADIUS) }
            .minByOrNull { hypot(it.x - CENTRE, it.y - CENTRE) }
            ?.let { it.x.toFloat() to it.y.toFloat() }
            ?: (CENTRE to CENTRE)

    companion object {
        const val GRID = 17
        const val CENTRE = 8f

        /** How close the player must stand to act on something. */
        const val INTERACT_RANGE = 1.45f

        /** The keeper's shoulders, in tiles. */
        const val BODY_RADIUS = 0.35f
    }
}

private fun hypot(dx: Int, dy: Float): Float = hypot(dx.toFloat(), dy)
