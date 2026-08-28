package com.tomk322.keepershour.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proves each of the seven endings is actually reachable by playing to it.
 *
 * This is the suite that stands in for a device: if these pass, the game is completable, the
 * critical path fits inside the hour, and no condition has quietly removed an option from it.
 */
class EndingsReachableTest {

    private companion object {
        /** Every discovery beat exits through the same "back to the room" choice. */
        const val BACK = "turn_back_to_the_room"
    }

    /** Lamp room to lit lamp, knowing where the key and the clean oil are. Costs 26 minutes. */
    private fun Playthrough.relight() = takeAll(
        "down_to_the_service_room",
        "down_to_the_bedroom",
        "down_to_the_kitchen",
        "take_the_cellar_key_from_the_nail", BACK,
        "down_to_the_store_room",
        "unlock_the_cellar_hatch_and_go_down",
        "take_the_sealed_can", BACK,
        "up_to_the_store_room",
        "up_to_the_kitchen",
        "up_to_the_bedroom",
        "up_to_the_service_room",
        "up_to_the_lamp_room",
        "drain_the_fouled_reservoir", BACK,
        "refill_the_reservoir_from_the_sealed_can", BACK,
        "light_the_lamp", BACK,
    )

    /** Climbs from the kitchen back to the glass to let the hour run out. */
    private fun Playthrough.watchFromTheLamp() = takeAll(
        "up_to_the_bedroom",
        "up_to_the_service_room",
        "up_to_the_lamp_room",
        "stand_at_the_glass_and_watch_for_her",
    )

    private fun informed(vararg facts: Knowledge): Playthrough =
        Playthrough().take("get_up").seed(facts.toSet())

    @Test
    fun `the rocks - do nothing and the hour runs out`() {
        val play = Playthrough().takeAll("get_up", "stand_at_the_glass_and_watch_for_her")
        assertEquals(Ending.THE_ROCKS, play.ending)
        assertTrue(play.state.endingsSeen.contains(Ending.THE_ROCKS))
    }

    @Test
    fun `drowned - the gallery does not forgive an unclipped door`() {
        val play = Playthrough().takeAll("get_up", "step_out_onto_the_gallery", "sink")
        assertEquals(Ending.DROWNED, play.ending)
    }

    @Test
    fun `locked in - too much rummaging while he is still awake`() {
        val play = Playthrough().take("get_up")
        play.takeAll(
            "down_to_the_service_room",
            "down_to_the_bedroom",
            "down_to_the_kitchen",
            "speak_to_him",
            "ask_him_about_ossian",
            "take_that_in",
            "leave_him_to_it",
            "up_to_the_bedroom",
            "up_to_the_service_room",
            "force_the_sea_chest",
            "take_the_money_and_the_letter",
        )
        assertEquals(Ending.LOCKED_IN, play.ending)
    }

    @Test
    fun `a lesser light - a rocket and nothing else`() {
        val play = informed(Knowledge.ROCKETS)
        play.takeAll(
            "down_to_the_service_room",
            "down_to_the_bedroom",
            "down_to_the_kitchen",
            "down_to_the_store_room",
            "take_a_distress_rocket", BACK,
            "up_to_the_kitchen",
            "up_to_the_bedroom",
            "up_to_the_service_room",
            "up_to_the_lamp_room",
            "clip_on_the_safety_line", BACK,
            "step_out_onto_the_gallery_clipped_on",
            "fire_a_distress_rocket", BACK,
            "back_inside",
            "stand_at_the_glass_and_watch_for_her",
        )
        assertEquals(Ending.LESSER_LIGHT, play.ending)
    }

    @Test
    fun `clean burn - the light saves the ship and nothing else changes`() {
        val play = informed(Knowledge.OIL_FOULED, Knowledge.CELLAR_KEY, Knowledge.SPARE_OIL)
        play.relight()
        val spentRelighting = play.state.minutesElapsed
        play.take("stand_at_the_glass_and_watch_for_her")

        assertEquals(Ending.CLEAN_BURN, play.ending)
        assertTrue(
            "Relighting took $spentRelighting minutes, leaving no room for anything else",
            spentRelighting < GameState.LOOP_MINUTES / 2,
        )
    }

    @Test
    fun `the reckoning - light the lamp and hang him with the logbook`() {
        val play = informed(
            Knowledge.OIL_FOULED,
            Knowledge.CELLAR_KEY,
            Knowledge.SPARE_OIL,
            Knowledge.LOOSE_FLAGSTONE,
            Knowledge.OSSIAN_LOGBOOK,
        )
        play.relight()
        play.takeAll(
            "down_to_the_service_room",
            "down_to_the_bedroom",
            "lift_the_flagstone_and_take_the_logbook", BACK,
            "down_to_the_kitchen",
            "have_it_out_with_him",
            "show_him_ossian_s_logbook", BACK,
        )
        play.watchFromTheLamp()
        assertEquals(Ending.THE_RECKONING, play.ending)
    }

    /** The full-knowledge route, stopping at the confrontation so its cost can be measured. */
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
    fun `the keepers hour - the whole truth, in the right order, inside the hour`() {
        val play = informed(*Knowledge.entries.toTypedArray())
        play.gatherAndConfront()
        val spent = play.state.minutesElapsed
        play.take("follow_him_up")

        assertEquals(Ending.THE_KEEPERS_HOUR, play.ending)
        assertTrue("The true ending must break the loop", play.ending!!.breaksLoop)
        assertTrue(
            "A fully informed run took $spent minutes; it should be a comfortable sprint",
            spent < GameState.LOOP_MINUTES / 2,
        )
    }

    @Test
    fun `the true ending does not require lighting the lamp yourself`() {
        val play = informed(*Knowledge.entries.toTypedArray())
        play.gatherAndConfront()
        play.take("follow_him_up")
        assertEquals(Ending.THE_KEEPERS_HOUR, play.ending)
        assertTrue("Kell lights it himself", !play.state.isSet(Flag.LAMP_LIT))
    }

    @Test
    fun `the letter alone is not enough to move him`() {
        val play = informed(Knowledge.AILSA_ABOARD)
        play.takeAll(
            "down_to_the_service_room",
            "take_ailsa_s_letter_from_the_desk", BACK,
            "down_to_the_bedroom",
            "down_to_the_kitchen",
            "have_it_out_with_him",
            "give_him_the_letter",
        )
        assertEquals("beat_letter_alone", play.at())
        assertTrue("He must not break on the letter alone", !play.state.isSet(Flag.EZRA_BROKEN))
    }

    @Test
    fun `the epilogue only follows the true ending`() {
        val play = informed(*Knowledge.entries.toTypedArray())
        play.gatherAndConfront()
        play.take("follow_him_up")
        play.take("end")
        assertEquals("epilogue", play.at())
    }

    @Test
    fun `every ending is covered by a route in this suite`() {
        val covered = setOf(
            Ending.THE_ROCKS, Ending.DROWNED, Ending.LOCKED_IN, Ending.LESSER_LIGHT,
            Ending.CLEAN_BURN, Ending.THE_RECKONING, Ending.THE_KEEPERS_HOUR,
        )
        assertEquals(Ending.entries.toSet(), covered)
    }
}
