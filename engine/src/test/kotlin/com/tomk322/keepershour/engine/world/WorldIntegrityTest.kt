package com.tomk322.keepershour.engine.world

import com.tomk322.keepershour.engine.KeepersHour
import org.junit.Assert.assertTrue
import org.junit.Test

/** The walkable world and the story must not be able to drift apart. */
class WorldIntegrityTest {

    private val story = KeepersHour.story

    @Test
    fun `world and story agree`() {
        val problems = WorldValidator.problems(story)
        assertTrue("World problems:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `every room has somewhere to stand`() {
        for (room in Lighthouse.rooms.values) {
            val open = WorldValidator.floodFill(room)
            assertTrue("Room '${room.id}' has almost no floor (${open.size} tiles)", open.size > MIN_FLOOR)
        }
    }

    @Test
    fun `the whole floor of every room is connected`() {
        for (room in Lighthouse.rooms.values) {
            val walkable = room.walkableTiles().toSet()
            val reachable = WorldValidator.floodFill(room)
            assertTrue(
                "Room '${room.id}' has ${walkable.size - reachable.size} tiles cut off from the rest",
                walkable == reachable,
            )
        }
    }

    @Test
    fun `arrival points are always somewhere legal`() {
        val targetOf = { choiceId: String ->
            story.scenes.values.flatMap { it.choices }.firstOrNull { it.id == choiceId }?.target
        }
        for (room in Lighthouse.rooms.values) {
            for (from in Lighthouse.rooms.keys + null) {
                val (x, y) = room.arrivalPoint(from, targetOf)
                assertTrue(
                    "Arriving in '${room.id}' from '$from' leaves the keeper stuck at $x,$y",
                    room.canOccupy(x, y, Room.BODY_RADIUS),
                )
            }
        }
    }

    @Test
    fun `a body fits on the open floor of every room`() {
        for (room in Lighthouse.rooms.values) {
            val standable = room.walkableTiles().count {
                room.canOccupy(it.x.toFloat(), it.y.toFloat(), Room.BODY_RADIUS)
            }
            assertTrue(
                "Room '${room.id}' has only $standable tiles the keeper actually fits on",
                standable > MIN_FLOOR,
            )
        }
    }

    @Test
    fun `every object can be reached by a body, not just by a point`() {
        for (room in Lighthouse.rooms.values) {
            for (thing in room.interactables) {
                val approach = room.walkableTiles().any { tile ->
                    room.canOccupy(tile.x.toFloat(), tile.y.toFloat(), Room.BODY_RADIUS) &&
                        thing.nearestDistance(tile.x.toFloat(), tile.y.toFloat()) <= Room.INTERACT_RANGE
                }
                assertTrue("Nothing can stand within reach of ${room.id}/${thing.id}", approach)
            }
        }
    }

    private companion object {
        const val MIN_FLOOR = 30
    }
}
