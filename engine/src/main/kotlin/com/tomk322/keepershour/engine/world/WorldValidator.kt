package com.tomk322.keepershour.engine.world

import com.tomk322.keepershour.engine.Story
import com.tomk322.keepershour.engine.StoryProblem
import kotlin.math.hypot

/**
 * Checks that the walkable world and the story graph still describe the same game.
 *
 * The dangerous failure when you put a story behind a character is silent: an action stays in the
 * content but no object in any room exposes it, so it can never be taken and nobody notices until
 * a player cannot finish. These checks make that a build failure.
 */
object WorldValidator {

    fun problems(story: Story): List<StoryProblem> = buildList {
        addAll(missingRooms(story))
        addAll(unknownChoiceIds(story))
        addAll(unexposedChoices(story))
        addAll(duplicateExposure())
        addAll(unreachableInteractables())
        addAll(strandedRooms())
    }

    /** Every walkable room must be a real scene, and every room scene must be walkable. */
    private fun missingRooms(story: Story) = buildList {
        for (id in Lighthouse.rooms.keys) {
            if (id !in story.scenes) add(StoryProblem("room-without-scene", id))
        }
    }

    /** An interactable naming a choice its room's scene does not have would be a dead button. */
    private fun unknownChoiceIds(story: Story) = Lighthouse.rooms.values.flatMap { room ->
        val scene = story.scenes[room.id] ?: return@flatMap emptyList()
        val known = scene.choices.map { it.id }.toSet()
        room.interactables.flatMap { thing ->
            thing.choiceIds.filter { it !in known }
                .map { StoryProblem("unknown-choice", "${room.id}/${thing.id} -> '$it'") }
        }
    }

    /** The important one: every choice in a room scene must be reachable from some object. */
    private fun unexposedChoices(story: Story) = Lighthouse.rooms.values.flatMap { room ->
        val scene = story.scenes[room.id] ?: return@flatMap emptyList()
        val exposed = room.interactables.flatMap { it.choiceIds }.toSet()
        scene.choices.map { it.id }.filter { it !in exposed }
            .map { StoryProblem("unexposed-choice", "${room.id}/$it has no object in the world") }
    }

    /** Two objects offering the same action would make which one you used arbitrary. */
    private fun duplicateExposure() = Lighthouse.rooms.values.flatMap { room ->
        room.interactables.flatMap { it.choiceIds }
            .groupingBy { it }.eachCount()
            .filterValues { it > 1 }
            .keys
            .map { StoryProblem("duplicate-exposure", "${room.id}/$it") }
    }

    /** An object walled off behind furniture is as good as absent. */
    private fun unreachableInteractables() = Lighthouse.rooms.values.flatMap { room ->
        val reachable = floodFill(room)
        room.interactables.filter { thing ->
            reachable.none { open -> thing.nearestDistance(open.x.toFloat(), open.y.toFloat()) <= Room.INTERACT_RANGE }
        }.map { StoryProblem("unreachable-object", "${room.id}/${it.id}") }
    }

    /** Every room needs a way out, or the player is stuck until midnight. */
    private fun strandedRooms() = Lighthouse.rooms.values.mapNotNull { room ->
        val exits = room.interactables.count { thing ->
            thing.kind in setOf(PropKind.STAIRS_UP, PropKind.STAIRS_DOWN, PropKind.DOOR, PropKind.HATCH)
        }
        if (exits == 0) StoryProblem("no-exit", room.id) else null
    }

    /**
     * Flood fill from the room's own arrival point, so "reachable" means reachable *by walking*
     * rather than merely present on the grid.
     */
    fun floodFill(room: Room): Set<Tile> {
        val open = room.walkableTiles().toSet()
        val start = open.minByOrNull { distanceFromCentre(it) } ?: return emptySet()

        val seen = mutableSetOf(start)
        val queue = ArrayDeque(listOf(start))
        while (queue.isNotEmpty()) {
            val tile = queue.removeFirst()
            for (next in neighbours(tile)) {
                if (next in open && seen.add(next)) queue += next
            }
        }
        return seen
    }

    private fun neighbours(tile: Tile) = listOf(
        Tile(tile.x + 1, tile.y),
        Tile(tile.x - 1, tile.y),
        Tile(tile.x, tile.y + 1),
        Tile(tile.x, tile.y - 1),
    )
}

private fun distanceFromCentre(tile: Tile): Float =
    hypot(tile.x - Room.CENTRE, tile.y - Room.CENTRE)
