package com.tomk322.keepershour.engine.content

import com.tomk322.keepershour.engine.Cost
import com.tomk322.keepershour.engine.Ending
import com.tomk322.keepershour.engine.Flag
import com.tomk322.keepershour.engine.GameEngine
import com.tomk322.keepershour.engine.Scene
import com.tomk322.keepershour.engine.SceneBuilder
import com.tomk322.keepershour.engine.StoryBuilder
import com.tomk322.keepershour.engine.buildScenes

/**
 * The seven ways the hour can finish.
 *
 * Six of them put her back on the lamp-room floor at eleven. Only [Ending.THE_KEEPERS_HOUR] lets
 * the night actually happen.
 */
internal object Endings {

    fun scenes(): List<Scene> = buildScenes {
        theRocks()
        drowned()
        lockedIn()
        lesserLight()
        cleanBurn()
        theReckoning()
        theKeepersHour()
        epilogue()
    }

    private fun StoryBuilder.epilogue() = scene("epilogue") {
        title("Corr Sgeir")
        markEnding()
        body(
            """
            THE KEEPER'S HOUR

            Corr Sgeir light was automated in 1971 and the keepers' quarters were sealed.

            The Board's file on the brig Marion was reopened in March 1908 on evidence submitted
            by Assistant Keeper M. Vance, and closed again in 1911 with a finding of wilful
            interference by persons ashore. Four men from Bail' Ard were transported.

            Ezra Kell served nine years. His statement, eleven pages in a steady hand, is still
            in the file.

            Ailsa Kell married in Oban in 1913. She kept, all her life, an unopened envelope with
            a broken seal, which is a contradiction her family never asked her to explain.

            Nobody ever wrote down what happened on the rock between eleven and midnight on the
            twenty-first of November, 1907, because only two people knew, and one of them spent
            the rest of his life being careful about what he put in writing.

            The other one lit the lamp.
            """,
        )
        choice("Begin again") {
            time(Cost.FREE)
            restart()
            goto("awaken")
        }
    }

    private fun StoryBuilder.theRocks() = ending(Ending.THE_ROCKS) {
        body { state ->
            """
            Midnight, and the light is out.

            ${if (state.isSet(Flag.FOGHORN_FIXED)) {
                "The horn is going. It has been going for a while now, and it is a sound in the " +
                    "dark that means rock, and it is not enough, because a horn tells a ship " +
                    "there is danger and a light tells her where it is."
            } else {
                "There is no light and no horn and nothing at all out there but weather."
            }}

            You are at the glass when it happens. You see her lights first - high, steady,
            confident, a lit hotel walking across the black - and you watch them come on at
            eleven knots toward a place you know and she does not.

            The sound arrives a second and a half after the shock does.

            Afterwards there are lights on the water that are not ship's lights, moving in a slow
            line from Bail' Ard headland, coming out to the wreck to take things off it.

            You are still watching them when the taste of copper comes into your mouth.
            """
        }
    }

    private fun StoryBuilder.drowned() = ending(Ending.DROWNED) {
        body(
            """
            The sea off Corr Sgeir is four degrees in November and the cold takes your breath
            before the water does.

            You go down past the rock you have lived on for two years, and the last thing is not
            fear, it is a very simple and very stupid regret: the lamp is still out, and nobody
            up there is going to light it.

            Then copper. Then the floor. Then eleven o'clock.

            Whatever the rock is doing to you, it is not finished, and neither are you.
            """,
        )
    }

    private fun StoryBuilder.lockedIn() = ending(Ending.LOCKED_IN) {
        body(
            """
            He has been watching you go up and down his tower all evening, and he is drunk, not
            stupid, and there is a difference that you keep forgetting.

            The store room door is two inches of oak with an outside bolt, for keeping weather
            out of the paint.

            "It's for your own good," he says through it, and he does not sound like a man who
            believes that, he sounds like a man saying a sentence he prepared. "You'll be let out
            at one."

            At one. Not at midnight. At one, when it is over.

            You put your shoulder to it forty times. You are still doing it when the sound comes
            through the rock - a long grinding note, felt in the soles of your boots more than
            heard.

            Then copper, and the floor, and eleven o'clock.
            """,
        )
    }

    private fun StoryBuilder.lesserLight() = ending(Ending.LESSER_LIGHT) {
        body { state ->
            """
            The rocket goes up red and the whole sound is lit for a second and a half.

            On the bridge of the Ardnamurchan a second officer sees it, and does what a good
            officer does with a red rocket in the dark: he reads it as a vessel in distress, and
            he alters toward it.

            Toward it. Toward you. Toward the reef.

            He sees the white water at two cables and puts the helm hard over and she comes round
            - most of her comes round. Her quarter goes across the outer teeth of Corr with a
            noise like a mill, and she goes on past you into the dark listing and lit and
            sounding her whistle, and she does not sink, and she does not arrive whole.

            ${if (state.isSet(Flag.FOGHORN_FIXED)) {
                "The horn was going, and that helped, and you will never know by how much."
            } else {
                "You will spend the rest of the hour working out what the horn would have added."
            }}

            They will put the number in the newspaper. It will not be two hundred. It will not be
            nothing.

            Copper. Floor. Eleven o'clock.
            """
        }
    }

    private fun StoryBuilder.cleanBurn() = ending(Ending.CLEAN_BURN) {
        body(
            """
            At twenty to midnight, Corr Sgeir light comes back on.

            She passes at eight minutes past, well out, exactly where a ship should be when
            somebody has told her where the rock is. You watch her go by lit up like a street and
            you sit down on the lamp-room floor and shake for a while.

            Two hundred people go to Oban and never know.

            Down in the kitchen, Ezra Kell hears the lens start to turn and pours himself another
            one. In the morning he will write in the station log that the burner was troublesome
            and was attended to, and the Board will read that, and in four months or eight there
            will be another night like this one on another rock, with another set of lanterns on
            another headland.

            You saved the ship. You did not save anything else.

            And when the taste of copper comes back into your mouth, you understand that the rock
            agrees with you.
            """,
        )
    }

    private fun StoryBuilder.theReckoning() = ending(Ending.THE_RECKONING) {
        body(
            """
            The light is burning and she goes by well out, and this time there is a logbook on
            the kitchen table with four entries in a dead man's hand, and eleven sovereigns
            holding it flat.

            Kell does not run. There is nowhere on a rock to run to. He sits with it until the
            relief boat comes on Friday and then he goes ashore between two men without saying
            anything at all, and you give the Board the book and the money and your statement,
            and it is enough. The brig Marion is reopened. Nine names get a different word
            written beside them than the one they had.

            It is a good night's work. It is the best night's work you will ever do.

            So you cannot immediately explain why, standing on the jetty watching the boat take
            him off, you find yourself thinking about a letter you never looked for, in a room
            you had already searched.

            Copper. Floor. Eleven o'clock.

            The rock is not done with you.
            """,
        )
    }

    private fun StoryBuilder.theKeepersHour() = ending(Ending.THE_KEEPERS_HOUR) {
        body(
            """
            He goes up the tower two steps at a time with the letter in his fist.

            You have never seen him move. Two years, and you did not know he could. He is through
            the service room and into the lamp room ahead of you, and by the time you get there
            he has the drain cock open and forty pounds of fouled oil going down the waste pipe,
            and he is talking, fast, flat, to himself and to you and to nobody.

            "Third stone from the wall in the bedroom, that's where he put it, I knew where he put
            it, I let it lie -" The clean can comes up off the floor like it weighs nothing.
            "- eleven days that letter sat there, eleven days, and I could not open it because I
            knew what I was going to do tonight and I could not have her voice in the room -"

            The wick. The match. His hands are perfectly steady.

            "- and she was on it. She was on it. She was coming home to see the light."

            It takes at 11:51. The lens catches it and the white bar goes out over the reef, and
            nine minutes later the Ardnamurchan comes past well out and lit up like a street, and
            two hundred people, one of whom is asleep in a second-class berth and does not know
            she was ever in any danger at all, go on to Oban.

            Ezra Kell stands at the glass and watches his daughter's ship go by and does not say
            anything for a long time.

            Then: "I'll write it all down. Tonight. The Marion, and the money, and Ossian." He is
            not looking at you. "You'll take it in for me. They'll not let me."

            "I'll take it in."

            "Aye." A long breath. "Aye."

            And you stand there together in a room full of turning light, waiting for the taste of
            copper.

            It does not come.

            The clock on the wall says three minutes past midnight, and then it says four minutes
            past, and outside the storm is going over and there is a thin grey line away east
            where Thursday is starting, and the hour is over, and it stays over.
            """,
        )
    }

    /** Ending scenes all share the same shape: text, then wake or stop. */
    private fun StoryBuilder.ending(outcome: Ending, block: SceneBuilder.() -> Unit) {
        scene(GameEngine.endingSceneId(outcome)) {
            title(outcome.title)
            markEnding()
            block()
            if (outcome.breaksLoop) {
                choice("End") {
                    time(Cost.FREE)
                    goto("epilogue")
                }
            } else {
                choice("Wake") {
                    time(Cost.FREE)
                    restart()
                    goto("awaken")
                }
            }
        }
    }
}
