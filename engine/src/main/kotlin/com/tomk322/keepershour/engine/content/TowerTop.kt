package com.tomk322.keepershour.engine.content

import com.tomk322.keepershour.engine.Cost
import com.tomk322.keepershour.engine.Ending
import com.tomk322.keepershour.engine.Flag
import com.tomk322.keepershour.engine.GameState
import com.tomk322.keepershour.engine.Item
import com.tomk322.keepershour.engine.Knowledge
import com.tomk322.keepershour.engine.Scene
import com.tomk322.keepershour.engine.SceneBuilder
import com.tomk322.keepershour.engine.StoryBuilder
import com.tomk322.keepershour.engine.buildScenes

/** The lamp room and the gallery: the light itself, and the one place on the rock that can kill. */
internal object TowerTop {

    fun scenes(): List<Scene> = buildScenes {
        lampRoom()
        beatOil(); beatDrained(); beatRefilled()
        beatLit(); beatLine(); beatBlown()
        gallery()
        beatFalseLights(); beatFoghorn(); beatFoghornFixed(); beatRocket()
    }

    private fun StoryBuilder.lampRoom() = scene(Tower.LAMP) {
        title("The Lamp Room")
        body(::lampRoomBody)

        choice("Examine the oil reservoir") {
            time(Cost.SEARCH)
            onlyIf { !it.knows(Knowledge.OIL_FOULED) }
            goto("beat_oil")
        }
        choice("Drain the fouled reservoir") {
            time(Cost.WORK)
            note("The brine has to come out before anything else can go in.")
            onlyIf { it.knows(Knowledge.OIL_FOULED) && !it.isSet(Flag.LAMP_DRAINED) }
            raise(Flag.LAMP_DRAINED)
            goto("beat_drained")
        }
        choice("Refill the reservoir from the sealed can") {
            time(Cost.ROUTINE)
            onlyIf {
                it.isSet(Flag.LAMP_DRAINED) && it.has(Item.CLEAN_OIL) && !it.isSet(Flag.LAMP_REFILLED)
            }
            raise(Flag.LAMP_REFILLED)
            drop(Item.CLEAN_OIL)
            goto("beat_refilled")
        }
        choice("Light the lamp") {
            time(Cost.ROUTINE)
            onlyIf { it.isSet(Flag.LAMP_REFILLED) && !it.isSet(Flag.LAMP_LIT) }
            raise(Flag.LAMP_LIT)
            goto("beat_lit")
        }
        choice("Clip on the safety line") {
            time(Cost.QUICK)
            onlyIf { !it.isSet(Flag.LINE_SECURED) }
            raise(Flag.LINE_SECURED)
            goto("beat_line")
        }
        galleryDoor()
        climbTo(Tower.SERVICE, "Down to the service room")
        choice("Stand at the glass and watch for her") {
            time(Cost.FREE)
            note("Let the hour run out. Whatever you have done by now is what you have done.")
            waitOutTheHour()
            goto("ending_the_rocks")
        }
    }

    /** The same door, and the only difference between a discovery and a drowning. */
    private fun SceneBuilder.galleryDoor() {
        choice("Step out onto the gallery") {
            time(Cost.STEP)
            note("The wind out there is standing up. Nobody goes out unclipped.")
            onlyIf { !it.isSet(Flag.LINE_SECURED) }
            goto("beat_blown")
        }
        choice("Step out onto the gallery, clipped on") {
            time(Cost.STEP)
            onlyIf { it.isSet(Flag.LINE_SECURED) }
            goto(Tower.GALLERY)
        }
    }

    private fun lampRoomBody(state: GameState): String {
        val lamp = when {
            state.isSet(Flag.LAMP_LIT) ->
                "The lens is turning. Light goes out across the water in long white sweeps, " +
                    "and the storm looks smaller for it."
            state.isSet(Flag.LAMP_REFILLED) ->
                "The reservoir is full of clean oil and the wick is dry and waiting."
            state.isSet(Flag.LAMP_DRAINED) ->
                "The reservoir stands empty, stinking of brine."
            else ->
                "The wick sits cold in oil that will not take a flame."
        }
        return """
            Six feet of curved glass, and a lens like a cathedral window laid on its side.

            $lamp

            Rain comes at the panes in handfuls. When the dark thins you can see white water
            standing up over Corr reef, three cables out, exactly where a ship would be if
            nobody told her not to be.
        """
    }

    private fun StoryBuilder.beatOil() {
        scene("beat_oil") {
            title("The Reservoir")
            body(
                """
                You set a match to the wick first, because that is what anybody would do.

                It takes, holds for the length of a breath, and drowns. It does it again on the
                second try and on the third, and after the third you put a finger in the
                reservoir and taste it, and the taste is the sea.

                Somebody has cut the colza oil with seawater. Not spilled, not splashed - cut,
                carefully, in the proportion that lets a wick draw for an hour and then die and
                look for all the world like bad luck.

                This lamp was never going to burn tonight.
                """,
            )
            learnAndReturn(Knowledge.OIL_FOULED, Tower.LAMP)
        }
    }

    private fun StoryBuilder.beatDrained() {
        scene("beat_drained") {
            title("Emptied")
            body(
                """
                You work the drain cock and let it all go down the waste pipe, and the smell that
                comes up is low tide and lamp oil together.

                Empty. Useless until something clean goes in.
                """,
            )
            back(Tower.LAMP)
        }
    }

    private fun StoryBuilder.beatRefilled() {
        scene("beat_refilled") {
            title("Clean Oil")
            body(
                """
                The seal on the can gives with a sound like a held breath let go. You pour, and
                the oil runs the colour of weak tea, and the smell of it is right.

                You trim the wick with your thumbnail and set it standing.
                """,
            )
            back(Tower.LAMP)
        }
    }

    private fun StoryBuilder.beatLit() {
        scene("beat_lit") {
            title("Light")
            body { state ->
                """
                The match, the wick, the small blue crawl, and then it takes.

                The lens catches it and multiplies it and throws it out in one white bar that
                goes clean over the reef and out into the weather, and the whole room turns
                slowly with the shadows.

                ${if (state.minutesLeft < LATE_MINUTES) {
                    "It is very late. But it is lit, and she has not struck yet."
                } else {
                    "Corr Sgeir is burning again."
                }}
                """
            }
            back(Tower.LAMP)
        }
    }

    private fun StoryBuilder.beatLine() {
        scene("beat_line") {
            title("The Line")
            body(
                """
                You clip the harness line to the rail eye and pull twice, hard, the way Ossian
                taught you before Ossian stopped being someone you could ask things.
                """,
            )
            back(Tower.LAMP)
        }
    }

    private fun StoryBuilder.beatBlown() {
        scene("beat_blown") {
            title("Off the Rail")
            body(
                """
                The door goes out of your hand the moment the latch lifts.

                The wind does not push you so much as remove you. There is a half-second of rail
                under your ribs, and then there is no rail, and the black comes up very fast and
                very cold, and Corr Sgeir light stays out behind you all the way down.
                """,
            )
            markEnding()
            choice("...") {
                id("sink")
                time(Cost.FREE)
                finish(Ending.DROWNED)
                goto("ending_drowned")
            }
        }
    }

    private fun StoryBuilder.gallery() = scene(Tower.GALLERY) {
        title("The Gallery")
        body(
            """
            Outside, on the iron walkway that rings the lamp room, the storm is a solid thing
            leaning on you. The line at your waist goes tight and slack, tight and slack.

            Below, the sea works at the rock with the patience of something that has all night.
            """,
        )
        choice("Look toward Bail' Ard headland") {
            time(Cost.ROUTINE)
            onlyIf { !it.knows(Knowledge.FALSE_LIGHTS) }
            goto("beat_false_lights")
        }
        choice("Examine the foghorn") {
            time(Cost.SEARCH)
            onlyIf { !it.knows(Knowledge.FOGHORN_SABOTAGED) }
            goto("beat_foghorn")
        }
        choice("Reconnect the foghorn air line") {
            time(Cost.WORK)
            onlyIf { it.knows(Knowledge.FOGHORN_SABOTAGED) && !it.isSet(Flag.FOGHORN_FIXED) }
            raise(Flag.FOGHORN_FIXED)
            goto("beat_foghorn_fixed")
        }
        choice("Fire a distress rocket") {
            time(Cost.ROUTINE)
            onlyIf { it.has(Item.ROCKET) && !it.isSet(Flag.ROCKET_FIRED) }
            raise(Flag.ROCKET_FIRED)
            drop(Item.ROCKET)
            goto("beat_rocket")
        }
        choice("Back inside") {
            time(Cost.STEP)
            goto(Tower.LAMP)
        }
    }

    private fun StoryBuilder.beatFalseLights() {
        scene("beat_false_lights") {
            title("Lights Where No Road Runs")
            body(
                """
                You get your eyes over the rail and hold them there against the rain.

                On Bail' Ard headland, a mile off across the sound, there are lanterns. Three of
                them, moving in a slow line, spaced the way riding lights are spaced on a vessel
                at anchor in a safe roadstead.

                There is no road on Bail' Ard. There is no anchorage. There is a shingle beach
                that is very good for taking things off a broken ship, and men are standing on it
                in the rain pretending to be a harbour.
                """,
            )
            learnAndReturn(Knowledge.FALSE_LIGHTS, Tower.GALLERY)
        }
    }

    private fun StoryBuilder.beatFoghorn() {
        scene("beat_foghorn") {
            title("The Foghorn")
            body(
                """
                The horn is silent, and it should not be, not in this.

                You get the cover off. The air line has been taken off its union and laid aside,
                neatly, the way a man puts a thing down when he intends to put it back.

                Not weather. Not wear. Hands.
                """,
            )
            learnAndReturn(Knowledge.FOGHORN_SABOTAGED, Tower.GALLERY)
        }
    }

    private fun StoryBuilder.beatFoghornFixed() {
        scene("beat_foghorn_fixed") {
            title("Voice")
            body(
                """
                You get the union back on and hold it while the thread bites, and your knuckles
                go white and then bleed a little, and then the horn finds its voice.

                It is not a light. In weather like this a horn is a rumour of a rock rather than
                a warning of one. But it is something in the dark that says: here, here, here.
                """,
            )
            back(Tower.GALLERY)
        }
    }

    private fun StoryBuilder.beatRocket() {
        scene("beat_rocket") {
            title("Red")
            body(
                """
                You brace the tube on the rail, turn your face away, and pull.

                The rocket goes up through the rain and bursts red, and for a moment the whole
                sound is lit like a room - the reef, the headland, the false lights going
                suddenly still down there as men look up.

                Someone on the Ardnamurchan will see that. Whether they will read it as
                "stand off" or as "a ship in distress, go and help her" is another matter
                entirely.
                """,
            )
            back(Tower.GALLERY)
        }
    }

    private const val LATE_MINUTES = 10
}

/** A discovery beat: record the fact, then step back into the room. */
internal fun SceneBuilder.learnAndReturn(
    fact: Knowledge,
    room: String,
) {
    choice("Take that in") {
        time(Cost.FREE)
        learn(fact)
        goto(room)
    }
}
