package com.tomk322.keepershour.engine.content

import com.tomk322.keepershour.engine.Cost
import com.tomk322.keepershour.engine.Scene
import com.tomk322.keepershour.engine.StoryBuilder
import com.tomk322.keepershour.engine.buildScenes

/** Where every loop starts: flat on her back on the lamp-room floor, at eleven o'clock. */
internal object Opening {

    private const val SETTLING_IN = 4

    fun scenes(): List<Scene> = buildScenes { awaken() }

    private fun StoryBuilder.awaken() = scene("awaken") {
        title("Eleven O'Clock")
        body { state ->
            when {
                state.loop == 1 -> FIRST
                state.loop == 2 -> SECOND
                state.loop < SETTLING_IN -> THIRD
                else -> settled(state.loop, state.knowledge.size)
            }
        }
        choice("Get up") {
            time(Cost.FREE)
            goto(Tower.LAMP)
        }
    }

    private val FIRST = """
        You come to on the lamp-room floor with the taste of copper in your mouth and no memory
        of lying down.

        The great lens stands dark above you.

        The clock on the wall says eleven, and the clock on the wall said eleven when you came
        up the stairs, and you have been up here longer than nothing.

        Two floors down, Ezra Kell is singing.

        The Ardnamurchan comes past Corr reef at midnight with two hundred souls aboard her, and
        Corr Sgeir light is out, and you have sixty minutes.
    """.trimIndent()

    private val SECOND = """
        Copper in your mouth. Floor against your back. Dark lens.

        Eleven o'clock.

        You watched that ship go onto the reef. You watched it from the gallery with the rain
        coming sideways, and you heard the sound a steel hull makes on rock, which is not a
        sound you had ever wanted to know.

        And now it is eleven o'clock, and Kell is singing the same three bars two floors down,
        and the oil in the lamp is exactly as bad as it was.

        Sixty minutes. Again.
    """.trimIndent()

    private val THIRD = """
        Eleven o'clock.

        You do not bother being surprised any more. You lie there for a second and let the list
        assemble itself.

        Down there, Kell has not yet done the thing he is going to do. Out there, she is making
        eleven knots in the dark toward a rock nobody has told her about.

        The hour is the same every time. What you bring into it is not.
    """.trimIndent()

    private fun settled(loop: Int, facts: Int) = """
        Eleven o'clock. The ${ordinal(loop)} time.

        You are up before your eyes are properly open, because the floor is the floor and the
        lens is dark and the singing is the singing, and none of that is worth a second of the
        sixty you have.

        You know ${if (facts == 0) "nothing yet" else "$facts things now"} that the rest of this
        rock does not. That is the only cargo that survives the crossing.
    """.trimIndent()

    private fun ordinal(n: Int): String = when (n) {
        FOURTH_LOOP -> "fourth"
        FIFTH_LOOP -> "fifth"
        SIXTH_LOOP -> "sixth"
        SEVENTH_LOOP -> "seventh"
        EIGHTH_LOOP -> "eighth"
        NINTH_LOOP -> "ninth"
        TENTH_LOOP -> "tenth"
        else -> "${n}th"
    }

    private const val FOURTH_LOOP = 4
    private const val FIFTH_LOOP = 5
    private const val SIXTH_LOOP = 6
    private const val SEVENTH_LOOP = 7
    private const val EIGHTH_LOOP = 8
    private const val NINTH_LOOP = 9
    private const val TENTH_LOOP = 10
}
