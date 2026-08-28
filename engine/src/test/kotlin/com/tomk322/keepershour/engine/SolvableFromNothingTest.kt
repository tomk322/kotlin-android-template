package com.tomk322.keepershour.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The end-to-end guarantee: a player who starts knowing nothing can reach the true ending.
 *
 * The other suites seed knowledge to test routes in isolation. This one earns every fact through
 * play, across real loops, inside the real clock - so it also proves the hour is long enough to
 * make progress in and short enough to force more than one attempt.
 */
class SolvableFromNothingTest {

    private companion object {
        const val BACK = "turn_back_to_the_room"

        /** A blind first hour should still be productive, or the pacing is wrong. */
        const val EXPECTED_FIRST_LOOP = 8
    }

    /** Loop one: strip the tower of everything except what is out in the weather. */
    private fun Playthrough.firstLoop() = takeAll(
        "examine_the_oil_reservoir", "take_that_in",
        "down_to_the_service_room",
        "search_the_writing_desk", "fold_it_and_put_it_in_your_pocket",
        "force_the_sea_chest", "take_the_money_and_the_letter",
        "down_to_the_bedroom",
        "go_through_ossian_s_things", "take_that_in",
        "work_the_loose_flagstone_up", "take_the_logbook",
        "down_to_the_kitchen",
        "search_behind_the_door", "take_it",
        "speak_to_him",
        "ask_him_about_the_light", "take_that_in",
        "leave_him_to_it",
        "down_to_the_store_room",
        "search_the_lockers", "take_that_in",
        "unlock_the_cellar_hatch_and_go_down",
        "search_the_oil_stock", "take_that_in",
    )

    /** Loop two: the two facts that can only be had outside, then the run that ends it. */
    private fun Playthrough.secondLoop() = takeAll(
        "clip_on_the_safety_line", BACK,
        "step_out_onto_the_gallery_clipped_on",
        "look_toward_bail_ard_headland", "take_that_in",
        "examine_the_foghorn", "take_that_in",
    )

    /** Everything gathered and laid in front of him; stops short of following him up the tower. */
    private fun Playthrough.gatherAndConfront() = takeAll(
        "down_to_the_service_room",
        "take_ailsa_s_letter_from_the_desk", BACK,
        "open_the_chest_and_take_the_money", BACK,
        "down_to_the_bedroom",
        "lift_the_flagstone_and_take_the_logbook", BACK,
        "down_to_the_kitchen",
        "have_it_out_with_him",
        "give_him_his_daughter_s_letter",
    )

    @Test
    fun `a player starting from nothing can learn everything and break the loop`() {
        val play = Playthrough().take("get_up")

        play.firstLoop()
        val afterFirst = play.state.knowledge.size
        assertTrue(
            "One hour should not be enough to learn all of it (learned $afterFirst)",
            afterFirst < Knowledge.entries.size,
        )
        assertTrue("A first loop should still teach plenty", afterFirst >= EXPECTED_FIRST_LOOP)

        // Burn what is left of loop one climbing home, and let midnight arrive.
        play.takeAll(
            "up_to_the_store_room",
            "up_to_the_kitchen",
            "up_to_the_bedroom",
            "up_to_the_service_room",
            "up_to_the_lamp_room",
        )
        assertEquals("The first hour must run out", Ending.THE_ROCKS, play.ending)

        play.take("wake")
        assertEquals(2, play.state.loop)
        assertEquals(
            "Knowledge is the only thing that crosses the loop",
            afterFirst,
            play.state.knowledge.size,
        )
        assertTrue("Items do not cross the loop", play.state.items.isEmpty())

        play.take("get_up")
        play.secondLoop()
        assertEquals(
            "Everything should be learnable in two loops",
            Knowledge.entries.toSet(),
            play.state.knowledge,
        )

        play.take("back_inside")
        play.gatherAndConfront()
        play.take("follow_him_up")

        assertEquals(Ending.THE_KEEPERS_HOUR, play.ending)
        assertTrue(play.state.currentEnding!!.breaksLoop)
    }

    @Test
    fun `knowledge makes the second run dramatically faster`() {
        val blind = Playthrough().take("get_up")
        blind.firstLoop()
        val blindCost = blind.state.minutesElapsed

        // Measured at the confrontation: following him up deliberately burns the rest of the hour.
        val informed = Playthrough().take("get_up").seed(Knowledge.entries.toSet())
        informed.gatherAndConfront()
        val informedCost = informed.state.minutesElapsed

        assertTrue(
            "Knowing where everything is should at least halve the run " +
                "(blind $blindCost min, informed $informedCost min)",
            informedCost * 2 < blindCost,
        )
    }

}
