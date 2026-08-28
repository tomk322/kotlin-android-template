package com.tomk322.keepershour.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Structural checks over the assembled graph. */
class StoryIntegrityTest {

    private val story = KeepersHour.story

    @Test
    fun `story graph has no structural problems`() {
        val problems = StoryValidator.problems(story)
        assertTrue(
            "Story graph problems:\n" + problems.joinToString("\n"),
            problems.isEmpty(),
        )
    }

    @Test
    fun `every scene renders for a fresh and a fully informed state`() {
        val fresh = GameState(sceneId = story.start)
        val omniscient = fresh.copy(
            loop = 9,
            knowledge = Knowledge.entries.toSet(),
            items = Item.entries.toSet(),
            flags = Flag.entries.toSet(),
            minutesElapsed = 55,
        )
        for (scene in story.scenes.values) {
            for (state in listOf(fresh, omniscient)) {
                val text = scene.body(state.copy(sceneId = scene.id))
                assertTrue("Scene '${scene.id}' rendered blank", text.isNotBlank())
                // Guards against an unsubstituted template hole slipping into shipped prose.
                assertTrue("Scene '${scene.id}' leaked a null", !text.contains("null"))
            }
        }
    }

    @Test
    fun `every scene offers at least one choice to a fresh player`() {
        val engine = GameEngine(story)
        for (scene in story.scenes.values) {
            val view = engine.view(GameState(sceneId = scene.id))
            assertTrue(
                "Scene '${scene.id}' offers nothing to a player who knows nothing",
                view.choices.isNotEmpty(),
            )
        }
    }

    @Test
    fun `clock formats from eleven at night to midnight`() {
        val state = GameState(sceneId = "awaken")
        assertEquals("23:00", state.clock)
        assertEquals("23:37", state.copy(minutesElapsed = 37).clock)
        assertEquals("00:00", state.copy(minutesElapsed = 60).clock)
    }

    @Test
    fun `a loop keeps knowledge and endings but drops everything else`() {
        val before = GameState(
            sceneId = "ending_the_rocks",
            loop = 3,
            minutesElapsed = 60,
            suspicion = 2,
            knowledge = setOf(Knowledge.OIL_FOULED, Knowledge.CELLAR_KEY),
            items = setOf(Item.CELLAR_KEY),
            flags = setOf(Flag.LAMP_DRAINED),
            endingsSeen = setOf(Ending.THE_ROCKS),
            currentEnding = Ending.THE_ROCKS,
        )
        val after = before.nextLoop("awaken")

        assertEquals(4, after.loop)
        assertEquals(before.knowledge, after.knowledge)
        assertEquals(before.endingsSeen, after.endingsSeen)
        assertEquals(emptySet<Item>(), after.items)
        assertEquals(emptySet<Flag>(), after.flags)
        assertEquals(0, after.minutesElapsed)
        assertEquals(0, after.suspicion)
        assertEquals(null, after.currentEnding)
    }
}
