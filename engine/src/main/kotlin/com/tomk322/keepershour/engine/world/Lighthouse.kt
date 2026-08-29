// A floor plan is tile coordinates. Naming each one would bury the layout it describes; the
// geometry is verified by WorldIntegrityTest instead.
@file:Suppress("MagicNumber")

package com.tomk322.keepershour.engine.world

import com.tomk322.keepershour.engine.content.Tower

/**
 * Corr Sgeir as walkable space: seven round rooms stacked up a tower, plus the gallery ring
 * outside the lamp room.
 *
 * Every interactable's [Interactable.choiceIds] point at choices that already exist in that
 * room's scene, so nothing here changes what the game does - only how you get at it. The world
 * tests assert that mapping stays exhaustive in both directions.
 */
object Lighthouse {

    /** Interior radius: verified as fully walk-connected with every room's furniture in place. */
    private const val R = 5.8f

    val rooms: Map<String, Room> by lazy {
        listOf(lampRoom(), gallery(), serviceRoom(), bedroom(), kitchen(), store(), cellar())
            .associateBy { it.id }
    }

    operator fun get(id: String): Room? = rooms[id]

    fun isRoom(sceneId: String): Boolean = sceneId in rooms

    private fun tiles(vararg pairs: Pair<Int, Int>): Set<Tile> =
        pairs.map { Tile(it.first, it.second) }.toSet()

    private fun lampRoom() = Room(
        id = Tower.LAMP,
        name = "The Lamp Room",
        plan = Floorplan.Disc(R),
        props = listOf(
            Prop(PropKind.CRATE, tiles(5 to 11)),
        ),
        interactables = listOf(
            Interactable(
                id = "lens",
                label = "The great lens",
                kind = PropKind.LENS,
                footprint = tiles(7 to 7, 8 to 7, 7 to 8, 8 to 8),
                choiceIds = listOf(
                    "examine_the_oil_reservoir",
                    "drain_the_fouled_reservoir",
                    "refill_the_reservoir_from_the_sealed_can",
                    "light_the_lamp",
                ),
            ),
            Interactable(
                id = "glass",
                label = "The seaward glass",
                kind = PropKind.WINDOW,
                footprint = tiles(7 to 3, 8 to 3, 9 to 3),
                choiceIds = listOf("stand_at_the_glass_and_watch_for_her"),
            ),
            Interactable(
                id = "line_hook",
                label = "Harness and line",
                kind = PropKind.LINE_HOOK,
                footprint = tiles(11 to 10),
                choiceIds = listOf("clip_on_the_safety_line"),
            ),
            Interactable(
                id = "gallery_door",
                label = "Gallery door",
                kind = PropKind.DOOR,
                footprint = tiles(13 to 7, 13 to 8),
                choiceIds = listOf(
                    "step_out_onto_the_gallery_clipped_on",
                    "step_out_onto_the_gallery",
                ),
            ),
            Interactable(
                id = "stairs_down",
                label = "Stairs down",
                kind = PropKind.STAIRS_DOWN,
                footprint = tiles(3 to 7, 3 to 8),
                choiceIds = listOf("down_to_the_service_room"),
            ),
        ),
    )

    private fun gallery() = Room(
        id = Tower.GALLERY,
        name = "The Gallery",
        plan = Floorplan.Ring(inner = 4.6f, outer = 6.9f),
        interactables = listOf(
            Interactable(
                id = "gallery_door",
                label = "Back inside",
                kind = PropKind.DOOR,
                footprint = tiles(14 to 8),
                choiceIds = listOf("back_inside"),
            ),
            Interactable(
                id = "foghorn",
                label = "The foghorn",
                kind = PropKind.FOGHORN,
                footprint = tiles(8 to 2, 9 to 2),
                choiceIds = listOf("examine_the_foghorn", "reconnect_the_foghorn_air_line"),
            ),
            Interactable(
                id = "rail",
                label = "The seaward rail",
                kind = PropKind.RAIL,
                footprint = tiles(8 to 14, 7 to 14),
                choiceIds = listOf("look_toward_bail_ard_headland", "fire_a_distress_rocket"),
            ),
        ),
    )

    private fun serviceRoom() = Room(
        id = Tower.SERVICE,
        name = "The Service Room",
        plan = Floorplan.Disc(R),
        props = listOf(
            Prop(PropKind.BUNK, tiles(12 to 5, 12 to 6)),
            Prop(PropKind.CRATE, tiles(4 to 11)),
        ),
        interactables = listOf(
            Interactable(
                id = "desk",
                label = "Writing desk",
                kind = PropKind.DESK,
                footprint = tiles(4 to 6, 4 to 7),
                choiceIds = listOf(
                    "search_the_writing_desk",
                    "take_ailsa_s_letter_from_the_desk",
                ),
            ),
            Interactable(
                id = "chest",
                label = "Sea chest",
                kind = PropKind.CHEST,
                footprint = tiles(11 to 11),
                choiceIds = listOf("force_the_sea_chest", "open_the_chest_and_take_the_money"),
            ),
            Interactable(
                id = "stairs_up",
                label = "Stairs up",
                kind = PropKind.STAIRS_UP,
                footprint = tiles(7 to 3, 8 to 3),
                choiceIds = listOf("up_to_the_lamp_room"),
            ),
            Interactable(
                id = "stairs_down",
                label = "Stairs down",
                kind = PropKind.STAIRS_DOWN,
                footprint = tiles(7 to 13, 8 to 13),
                choiceIds = listOf("down_to_the_bedroom"),
            ),
        ),
    )

    private fun bedroom() = Room(
        id = Tower.BEDROOM,
        name = "The Bedroom",
        plan = Floorplan.Disc(R),
        props = listOf(
            Prop(PropKind.BUNK, tiles(12 to 7, 12 to 8)),
        ),
        interactables = listOf(
            Interactable(
                id = "ossian_bunk",
                label = "Ossian's bunk",
                kind = PropKind.BUNK,
                footprint = tiles(4 to 7, 4 to 8),
                choiceIds = listOf("go_through_ossian_s_things"),
            ),
            Interactable(
                id = "flagstone",
                label = "Loose flagstone",
                kind = PropKind.FLAGSTONE,
                footprint = tiles(5 to 10),
                solid = false,
                choiceIds = listOf(
                    "work_the_loose_flagstone_up",
                    "lift_the_flagstone_and_take_the_logbook",
                ),
            ),
            Interactable(
                id = "stairs_up",
                label = "Stairs up",
                kind = PropKind.STAIRS_UP,
                footprint = tiles(7 to 3, 8 to 3),
                choiceIds = listOf("up_to_the_service_room"),
            ),
            Interactable(
                id = "stairs_down",
                label = "Stairs down",
                kind = PropKind.STAIRS_DOWN,
                footprint = tiles(7 to 13, 8 to 13),
                choiceIds = listOf("down_to_the_kitchen"),
            ),
        ),
    )

    private fun kitchen() = Room(
        id = Tower.KITCHEN,
        name = "The Kitchen",
        plan = Floorplan.Disc(R),
        props = listOf(
            Prop(PropKind.TABLE, tiles(7 to 8, 8 to 8, 9 to 8)),
            Prop(PropKind.RANGE, tiles(4 to 6, 4 to 7)),
        ),
        interactables = listOf(
            Interactable(
                id = "kell",
                label = "Ezra Kell",
                kind = PropKind.KELL,
                footprint = tiles(8 to 7),
                choiceIds = listOf(
                    "speak_to_him",
                    "have_it_out_with_him",
                    "fill_his_cup_again",
                    "shake_him_awake",
                ),
            ),
            Interactable(
                id = "pegs",
                label = "Oilskins on their pegs",
                kind = PropKind.PEGS,
                footprint = tiles(12 to 10),
                choiceIds = listOf(
                    "search_behind_the_door",
                    "take_the_cellar_key_from_the_nail",
                ),
            ),
            Interactable(
                id = "stairs_up",
                label = "Stairs up",
                kind = PropKind.STAIRS_UP,
                footprint = tiles(7 to 3, 8 to 3),
                choiceIds = listOf("up_to_the_bedroom"),
            ),
            Interactable(
                id = "stairs_down",
                label = "Stairs down",
                kind = PropKind.STAIRS_DOWN,
                footprint = tiles(7 to 13, 8 to 13),
                choiceIds = listOf("down_to_the_store_room"),
            ),
        ),
    )

    private fun store() = Room(
        id = Tower.STORE,
        name = "The Store Room",
        plan = Floorplan.Disc(R),
        props = listOf(
            Prop(PropKind.CRATE, tiles(11 to 5, 12 to 6)),
            Prop(PropKind.CRATE, tiles(11 to 11)),
        ),
        interactables = listOf(
            Interactable(
                id = "locker",
                label = "The long locker",
                kind = PropKind.LOCKER,
                footprint = tiles(4 to 6, 4 to 7, 4 to 8),
                choiceIds = listOf("search_the_lockers", "take_a_distress_rocket"),
            ),
            Interactable(
                id = "hatch",
                label = "Cellar hatch",
                kind = PropKind.HATCH,
                footprint = tiles(8 to 11),
                solid = false,
                choiceIds = listOf("unlock_the_cellar_hatch_and_go_down", "try_the_cellar_hatch"),
            ),
            Interactable(
                id = "stairs_up",
                label = "Stairs up",
                kind = PropKind.STAIRS_UP,
                footprint = tiles(7 to 3, 8 to 3),
                choiceIds = listOf("up_to_the_kitchen"),
            ),
        ),
    )

    private fun cellar() = Room(
        id = Tower.CELLAR,
        name = "The Cellar",
        plan = Floorplan.Disc(5.4f),
        props = listOf(
            Prop(PropKind.WATER_BUTT, tiles(11 to 10)),
        ),
        interactables = listOf(
            Interactable(
                id = "oil_stock",
                label = "The oil stock",
                kind = PropKind.CASK,
                footprint = tiles(11 to 7, 11 to 8),
                choiceIds = listOf("search_the_oil_stock", "take_the_sealed_can"),
            ),
            Interactable(
                id = "stairs_up",
                label = "Stairs up",
                kind = PropKind.STAIRS_UP,
                footprint = tiles(5 to 7, 5 to 8),
                choiceIds = listOf("up_to_the_store_room"),
            ),
        ),
    )
}
