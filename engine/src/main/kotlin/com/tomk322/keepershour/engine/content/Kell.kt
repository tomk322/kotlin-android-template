package com.tomk322.keepershour.engine.content

import com.tomk322.keepershour.engine.Cost
import com.tomk322.keepershour.engine.Flag
import com.tomk322.keepershour.engine.GameState
import com.tomk322.keepershour.engine.Item
import com.tomk322.keepershour.engine.Knowledge
import com.tomk322.keepershour.engine.Scene
import com.tomk322.keepershour.engine.StoryBuilder
import com.tomk322.keepershour.engine.buildScenes

/**
 * The kitchen, and Ezra Kell in it.
 *
 * He is the lock the whole game turns on. Searching his room is safe only once he is asleep, but
 * a sleeping man cannot be confronted - so the player has to learn to put him under and then get
 * him up again, in that order, with the clock running.
 */
internal object Kell {

    private const val OSSIAN_SUSPICION = 1

    fun scenes(): List<Scene> = buildScenes {
        kitchen()
        kitchenBeats()
        talk()
        beatAskLight(); beatAskOssian(); beatAskShip()
        beatLetterAlone(); kellExposed(); kellBroken()
        confrontationChoices()
    }

    private fun StoryBuilder.kitchen() = scene(Tower.KITCHEN) {
        title("The Kitchen")
        body(::kitchenBody)

        choice("Search behind the door") {
            time(Cost.SEARCH)
            onlyIf { !it.knows(Knowledge.CELLAR_KEY) }
            goto("beat_behind_door")
        }
        choice("Take the cellar key from the nail") {
            time(Cost.GLANCE)
            note("Behind the door, under the oilskins.")
            onlyIf { it.knows(Knowledge.CELLAR_KEY) && !it.has(Item.CELLAR_KEY) }
            take(Item.CELLAR_KEY)
            goto("beat_key_taken")
        }
        choice("Speak to him") {
            time(Cost.FREE)
            onlyIf { !it.isSet(Flag.EZRA_ASLEEP) && !it.isSet(Flag.EZRA_BROKEN) }
            goto("kell_talk")
        }
        choice("Fill his cup again") {
            time(Cost.ROUTINE)
            note("He is most of the way there already. It would not take much.")
            onlyIf {
                it.knows(Knowledge.EZRA_DRUNK) && !it.isSet(Flag.EZRA_ASLEEP) &&
                    !it.isSet(Flag.EZRA_EXPOSED) && !it.isSet(Flag.EZRA_BROKEN)
            }
            raise(Flag.EZRA_ASLEEP)
            goto("beat_kell_under")
        }
        choice("Have it out with him") {
            time(Cost.FREE)
            onlyIf {
                !it.isSet(Flag.EZRA_ASLEEP) && !it.isSet(Flag.EZRA_BROKEN) &&
                    (it.has(Item.LOGBOOK) || it.has(Item.SOVEREIGNS) || it.has(Item.LETTER))
            }
            goto("kell_confront")
        }
        choice("Shake him awake") {
            time(Cost.QUICK)
            onlyIf { it.isSet(Flag.EZRA_ASLEEP) }
            lower(Flag.EZRA_ASLEEP)
            goto("beat_kell_woken")
        }
        climbTo(Tower.BEDROOM, "Up to the bedroom")
        climbTo(Tower.STORE, "Down to the store room")
    }

    private fun kitchenBody(state: GameState): String {
        val kell = when {
            state.isSet(Flag.EZRA_BROKEN) ->
                "Ezra Kell is sitting with his daughter's letter open in front of him and his " +
                    "hands flat on either side of it, not touching it."
            state.isSet(Flag.EZRA_EXPOSED) ->
                "Ezra Kell is very still, and watching you the way you watch weather."
            state.isSet(Flag.EZRA_ASLEEP) ->
                "Ezra Kell is face down on the table with his arm through the handle of the " +
                    "jug, breathing like a bellows with a hole in it."
            else ->
                "Ezra Kell is at the table with a jug and a tin cup, singing the same three " +
                    "bars of the same song over and over, the way a needle repeats in a worn " +
                    "groove."
        }
        return """
            The range is out. The room is warm anyway, in the way a room is warm when a big man
            has been sitting in it all evening.

            $kell
        """
    }

    private fun StoryBuilder.kitchenBeats() {
        scene("beat_behind_door") {
            title("Behind the Door")
            body(
                """
                Oilskins on three pegs, stiff with salt. Behind the third one, on a nail driven
                into the frame where the door swings back and hides it, the cellar key.

                He does not keep it on him at all. He keeps it where a man keeps a thing he has
                decided not to think about.
                """,
            )
            choice("Take it") {
                time(Cost.FREE)
                learn(Knowledge.CELLAR_KEY)
                take(Item.CELLAR_KEY)
                goto(Tower.KITCHEN)
            }
        }
        scene("beat_key_taken") {
            title("The Key")
            body("Cold iron, and a smear of green on it where it has sat against the nail.")
            back(Tower.KITCHEN)
        }
        scene("beat_kell_under") {
            title("Under")
            body(
                """
                You fill the cup and he takes it without looking at you, the way he takes
                everything.

                It takes eleven minutes and then it takes him all at once. He goes down onto his
                forearms, and then onto the table, and the singing stops in the middle of a bar.

                You can go through the whole tower now and he will not know. He also cannot
                answer a single question, which is a problem you have made for yourself.
                """,
            )
            back(Tower.KITCHEN)
        }
        scene("beat_kell_woken") {
            title("Up")
            body(
                """
                You get a hand in his collar and haul, and he comes up swearing and swinging and
                then not swinging, blinking at you out of a face like wet dough.

                "What. What is it. What."
                """,
            )
            back(Tower.KITCHEN)
        }
    }

    private fun StoryBuilder.talk() = scene("kell_talk") {
        title("Ezra Kell")
        body { state ->
            if (state.isSet(Flag.EZRA_EXPOSED)) {
                """
                He has not moved since you put it in front of him. The cup is where it was. He
                is looking at the table like the table has said something.
                """
            } else {
                """
                He looks up at you with the loose, careful focus of a man who has to aim his eyes.

                "The light's out," he says, agreeably, as if remarking on the weather. "She'll
                have gone out. They do that."
                """
            }
        }
        choice("Ask him about the light") {
            time(Cost.TALK)
            onlyIf { !it.knows(Knowledge.EZRA_DRUNK) }
            goto("beat_ask_light")
        }
        choice("Ask him about Ossian") {
            time(Cost.TALK)
            note("He will not like it.")
            onlyIf { !it.knows(Knowledge.OSSIAN_FELL) }
            goto("beat_ask_ossian")
        }
        choice("Ask him about the ship") {
            time(Cost.ROUTINE)
            goto("beat_ask_ship")
        }
        back(Tower.KITCHEN, "Leave him to it")
    }

    private fun StoryBuilder.beatAskLight() {
        scene("beat_ask_light") {
            title("\"They Do That\"")
            body(
                """
                "The oil's bad," you tell him.

                "Oil's oil."

                "It's cut with seawater, Mr Kell."

                And there it is - a thing crossing his face and being put away again, fast, the
                way you palm a coin. Not surprise. Recognition.

                "You'll want to be careful," he says, "saying that." He fills the cup. His hand
                is steady, which after that much is its own kind of information: he has been
                doing this a long time, and he is doing it deliberately tonight.
                """,
            )
            learnAndReturn(Knowledge.EZRA_DRUNK, "kell_talk")
        }
    }

    private fun StoryBuilder.beatAskOssian() {
        scene("beat_ask_ossian") {
            title("The Twelfth of September")
            body(
                """
                The singing stops.

                "Ossian Rue went off the gallery," he says, to the table. "In weather. I wrote it
                up. The Board took it. It's took and done."

                "I never said he didn't."

                "No." He turns the cup around once on the wood. "You never." And then, with the
                first entirely sober thing in his voice all night: "Don't you go up on that
                gallery tonight, girl."
                """,
            )
            choice("Take that in") {
                time(Cost.FREE)
                learn(Knowledge.OSSIAN_FELL)
                suspect(OSSIAN_SUSPICION)
                goto("kell_talk")
            }
        }
    }

    private fun StoryBuilder.beatAskShip() {
        scene("beat_ask_ship") {
            title("The Ardnamurchan")
            body(
                """
                "There's a steamer due past the reef at midnight."

                "There's a steamer due past the reef most nights." He says it into the cup. "It's
                nothing to do with me what floats past this rock."

                He is lying, and he is lying badly, and it costs you five minutes to learn
                nothing you did not already suspect.
                """,
            )
            back("kell_talk")
        }

    }

    private fun StoryBuilder.kellExposed() {
        scene("kell_exposed") {
            title("Shown")
            body { state ->
                val what = when {
                    state.has(Item.LOGBOOK) && state.has(Item.SOVEREIGNS) ->
                        "You put the logbook on the table, and the sovereigns on the logbook."
                    state.has(Item.LOGBOOK) ->
                        "You put Ossian's second logbook on the table in front of him."
                    else ->
                        "You put eleven sovereigns on the table, one at a time, so he has to " +
                            "hear each one."
                }
                """
                $what

                He does not touch it. He looks at it for a long moment, and then he looks up at
                you, and the drink has gone out of his face entirely.

                "You'll not prove it."

                "I don't have to prove it. I only have to hand it in."

                Something goes out of him. Not the fight - he has not got fight - but the story
                he has been telling himself, which is the thing that has been holding him up.
                He sits down heavily in a chair he is already sitting in.

                "It's too late anyway," he says. "She passes the reef at midnight."
                """
            }
            back(Tower.KITCHEN)
        }

    }

    private fun StoryBuilder.beatLetterAlone() {
        scene("beat_letter_alone") {
            title("Paper")
            body(
                """
                You put the letter in his hand.

                He looks at it for a while. Then he folds it in half, without reading past the
                first line, and puts it in his breast pocket.

                "That's a cruel thing," he says quietly, "to make up."

                And you understand, too late, that a man who has done what he has done tonight
                cannot afford to believe you. Not on a piece of paper. Not without the rest of
                it laid out where he cannot look away from it.
                """,
            )
            back(Tower.KITCHEN)
        }

    }

    private fun StoryBuilder.kellBroken() {
        scene("kell_broken") {
            title("The Keeper's Hour")
            body(
                """
                You put the logbook on the table. You put the sovereigns on the logbook.

                And then you put Ailsa's letter in his hand, and you close his fingers on it,
                because he will not take it otherwise.

                "It came out eleven days ago," you tell him. "You walked past it every morning."

                He looks at the hand and the paper in it. He turns it over. He sees the writing
                and you watch a man's whole face come apart in the time it takes to draw one
                breath.

                "Ailsa," he says.

                "She took a berth on the Ardnamurchan. To come home. To see the light again."

                For a moment he does not move at all, and the storm goes on outside, and
                somewhere out there in it a steamer is making eleven knots toward Corr reef in
                the dark, because the men on her bridge believe there is no light here because
                there is no light here.

                Then Ezra Kell stands up.

                He does not say anything. He goes past you and up the stairs, and he is not a
                drunk going up stairs, he is a keeper going up a tower, two at a time, and you
                have to run to keep with him.
                """,
            )
            choice("Follow him up") {
                time(Cost.FREE)
                raise(Flag.EZRA_BROKEN)
                waitOutTheHour()
                goto("ending_the_keepers_hour")
            }
        }
    }
}

/** Confrontation choices, hung off the kitchen so they read as things you do to his face. */
internal fun StoryBuilder.confrontationChoices() = scene("kell_confront") {
    title("Ezra Kell")
    body(
        """
        He is looking at you. The jug is between you on the table.

        Whatever you are going to do, the clock is doing it with you.
        """,
    )
    choice("Show him Ossian's logbook") {
        time(Cost.TALK)
        onlyIf {
            it.has(Item.LOGBOOK) && !it.isSet(Flag.EZRA_EXPOSED) && !it.isSet(Flag.EZRA_BROKEN)
        }
        raise(Flag.EZRA_EXPOSED)
        goto("kell_exposed")
    }
    choice("Put the sovereigns on the table") {
        time(Cost.TALK)
        onlyIf {
            it.has(Item.SOVEREIGNS) && !it.isSet(Flag.EZRA_EXPOSED) && !it.isSet(Flag.EZRA_BROKEN)
        }
        raise(Flag.EZRA_EXPOSED)
        goto("kell_exposed")
    }
    choice("Give him his daughter's letter") {
        time(Cost.TALK)
        note("Everything you have, all at once, in the right order.")
        onlyIf {
            it.has(Item.LETTER) && it.has(Item.LOGBOOK) && it.has(Item.SOVEREIGNS) &&
                !it.isSet(Flag.EZRA_BROKEN)
        }
        goto("kell_broken")
    }
    choice("Give him the letter") {
        time(Cost.TALK)
        note("You have the letter, but nothing to make him believe the rest of it.")
        onlyIf {
            it.has(Item.LETTER) && !(it.has(Item.LOGBOOK) && it.has(Item.SOVEREIGNS)) &&
                !it.isSet(Flag.EZRA_BROKEN)
        }
        goto("beat_letter_alone")
    }
    back(Tower.KITCHEN, "Say nothing")
}
