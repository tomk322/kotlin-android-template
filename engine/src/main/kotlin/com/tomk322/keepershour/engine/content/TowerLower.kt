package com.tomk322.keepershour.engine.content

import com.tomk322.keepershour.engine.Cost
import com.tomk322.keepershour.engine.Flag
import com.tomk322.keepershour.engine.Item
import com.tomk322.keepershour.engine.Knowledge
import com.tomk322.keepershour.engine.Scene
import com.tomk322.keepershour.engine.StoryBuilder
import com.tomk322.keepershour.engine.buildScenes

/** Kell's room, the bedroom, the store and the cellar: where the evidence and the oil live. */
internal object TowerLower {

    /** Rifling the keeper's chest is noticed - unless he is too far gone to notice anything. */
    private const val CHEST_SUSPICION = 2

    fun scenes(): List<Scene> = buildScenes {
        serviceRoom()
        beatDesk(); beatLetterTaken(); beatChest(); beatMoneyTaken()
        bedroom()
        bedroomBeats()
        store()
        storeBeats()
        cellar()
        cellarBeats()
    }

    private fun StoryBuilder.serviceRoom() = scene(Tower.SERVICE) {
        title("The Service Room")
        body { state ->
            """
            Kell's room, and it smells like it: pipe ash, wet wool, and underneath that the
            sweetish reek of a man who has been drinking for three days in a small space.

            A writing desk with the flap down. A sea chest under the window with a strap over it.
            ${if (state.isSet(Flag.SEARCHED_CHEST)) "The chest stands open where you left it." else ""}
            """
        }
        choice("Search the writing desk") {
            time(Cost.SEARCH)
            onlyIf { !it.knows(Knowledge.AILSA_ABOARD) }
            goto("beat_desk")
        }
        choice("Take Ailsa's letter from the desk") {
            time(Cost.GLANCE)
            note("You know exactly which pigeonhole.")
            onlyIf { it.knows(Knowledge.AILSA_ABOARD) && !it.has(Item.LETTER) }
            take(Item.LETTER)
            goto("beat_letter_taken")
        }
        choice("Force the sea chest") {
            time(Cost.THOROUGH)
            note("Loud, slow work, and he will see the strap cut.")
            onlyIf { !it.knows(Knowledge.EZRA_PAID) }
            goto("beat_chest")
        }
        choice("Open the chest and take the money") {
            time(Cost.QUICK)
            note("You know the strap buckle gives if you lift and twist.")
            onlyIf { it.knows(Knowledge.EZRA_PAID) && !it.has(Item.SOVEREIGNS) }
            take(Item.SOVEREIGNS)
            raise(Flag.SEARCHED_CHEST)
            goto("beat_money_taken")
        }
        climbTo(Tower.LAMP, "Up to the lamp room")
        climbTo(Tower.BEDROOM, "Down to the bedroom")
    }

    private fun StoryBuilder.beatDesk() {
        scene("beat_desk") {
            title("The Writing Desk")
            body(
                """
                Board forms. A tide table two years out of date. A bill for boots.

                And in the third pigeonhole, a letter that has never been opened. The hand on the
                front is young and careful. It came out on the last supply boat, eleven days ago,
                and Ezra Kell has walked past it every morning since.

                You break the seal, because tonight you will do worse things than this.

                *Father - I have taken a berth on the Ardnamurchan and I will be at Oban on
                Thursday and at Corr on Friday if the weather lets the boat come out. I am not
                angry any more. I would like to see the light again. - Ailsa*

                The Ardnamurchan passes Corr reef at midnight.
                """,
            )
            choice("Fold it and put it in your pocket") {
                time(Cost.FREE)
                learn(Knowledge.AILSA_ABOARD)
                take(Item.LETTER)
                goto(Tower.SERVICE)
            }
        }
    }

    private fun StoryBuilder.beatLetterTaken() {
        scene("beat_letter_taken") {
            title("The Letter")
            body("You have it. Paper, and a girl's careful handwriting, and a berth number.")
            back(Tower.SERVICE)
        }
    }

    private fun StoryBuilder.beatChest() {
        scene("beat_chest") {
            title("The Sea Chest")
            body(
                """
                The strap is old and the buckle is stiff and in the end you cut it.

                Under a folded jersey and a bible nobody has opened: a leather purse with eleven
                sovereigns in it, which is four months of a keeper's pay.

                Under the purse, a letter with no sender and no signature. It names a night -
                tonight - and a price, and it says *the usual arrangement, and see the horn is
                quiet.*
                """,
            )
            choice("Take the money and the letter") {
                time(Cost.FREE)
                learn(Knowledge.EZRA_PAID)
                take(Item.SOVEREIGNS)
                raise(Flag.SEARCHED_CHEST)
                suspect(CHEST_SUSPICION)
                goto(Tower.SERVICE)
            }
        }
    }

    private fun StoryBuilder.beatMoneyTaken() {
        scene("beat_money_taken") {
            title("Eleven Sovereigns")
            body("Heavy, for so little. You put them in your apron and they knock together.")
            back(Tower.SERVICE)
        }
    }

    private fun StoryBuilder.bedroom() = scene(Tower.BEDROOM) {
        title("The Bedroom")
        body(
            """
            Two bunks in a curved wall. Yours, slept in. Ossian Rue's, stripped to the ticking
            three months ago and never given to anybody else, because nobody else has come.

            His sea boots are still under it. Kell would not let them be sent on.
            """,
        )
        choice("Go through Ossian's things") {
            time(Cost.SEARCH)
            onlyIf { !it.knows(Knowledge.LOOSE_FLAGSTONE) }
            goto("beat_ossian_things")
        }
        choice("Work the loose flagstone up") {
            time(Cost.WORK)
            onlyIf { it.knows(Knowledge.LOOSE_FLAGSTONE) && !it.knows(Knowledge.OSSIAN_LOGBOOK) }
            goto("beat_flagstone")
        }
        choice("Lift the flagstone and take the logbook") {
            time(Cost.QUICK)
            note("Third stone from the wall. It comes up if you lever the seaward edge.")
            onlyIf { it.knows(Knowledge.OSSIAN_LOGBOOK) && !it.has(Item.LOGBOOK) }
            take(Item.LOGBOOK)
            goto("beat_logbook_taken")
        }
        climbTo(Tower.SERVICE, "Up to the service room")
        climbTo(Tower.KITCHEN, "Down to the kitchen")
    }

    private fun StoryBuilder.bedroomBeats() {
        scene("beat_ossian_things") {
            title("Ossian's Bunk")
            body(
                """
                Boots. A razor. A photograph of a terrace in Greenock with nobody in it.

                And when you kneel to reach the back of the locker, your knee rocks. The third
                flagstone from the wall sits proud of its neighbours by the thickness of a nail,
                and the mortar round it is not mortar, it is soap and ash.

                He hid something. Three months ago, Ossian Rue went off the gallery in weather
                exactly like this, and Kell wrote it up as an accident, and Ossian hid something
                under his own bunk before he went.
                """,
            )
            choice("Take that in") {
                time(Cost.FREE)
                learn(Knowledge.LOOSE_FLAGSTONE, Knowledge.OSSIAN_FELL)
                goto(Tower.BEDROOM)
            }
        }
        scene("beat_flagstone") {
            title("Under the Stone")
            body(
                """
                You get the razor under the seaward edge and lever, and your fingers, and then
                your fingers and the boot-hook, and it comes up.

                A biscuit tin. In the tin, a logbook that is not the station logbook.

                *14 March. Light dimmed 40 min. E.K. says burner. Burner sound.*
                *2 August. Light dimmed 35 min. Horn off union. E.K. says weather.*
                *19 August. Brig Marion lost, Bail' Ard. Nine hands. Light was dimmed.*
                *11 September. Told E.K. I am writing to the Board on Friday. He asked me to
                think on it. I have thought on it.*

                There is no entry for the twelfth of September. On the twelfth of September,
                Ossian Rue fell.
                """,
            )
            choice("Take the logbook") {
                time(Cost.FREE)
                learn(Knowledge.OSSIAN_LOGBOOK)
                take(Item.LOGBOOK)
                goto(Tower.BEDROOM)
            }
        }
        scene("beat_logbook_taken") {
            title("The Second Log")
            body("The tin, the book, the four entries that hang a man. In your apron with the rest.")
            back(Tower.BEDROOM)
        }
    }

    private fun StoryBuilder.store() = scene(Tower.STORE) {
        title("The Store Room")
        body(
            """
            Rope, paint, a spare lens blank in a crate of straw, and the smell of tarred twine.

            The cellar hatch is in the floor, and it is locked, because Kell keeps it locked and
            Kell keeps the key.
            """,
        )
        choice("Search the lockers") {
            time(Cost.SEARCH)
            onlyIf { !it.knows(Knowledge.ROCKETS) }
            goto("beat_lockers")
        }
        choice("Take a distress rocket") {
            time(Cost.QUICK)
            onlyIf { it.knows(Knowledge.ROCKETS) && !it.has(Item.ROCKET) }
            take(Item.ROCKET)
            goto("beat_rocket_taken")
        }
        choice("Unlock the cellar hatch and go down") {
            time(Cost.QUICK)
            onlyIf { it.has(Item.CELLAR_KEY) }
            goto(Tower.CELLAR)
        }
        choice("Try the cellar hatch") {
            time(Cost.GLANCE)
            onlyIf { !it.has(Item.CELLAR_KEY) }
            goto("beat_hatch_locked")
        }
        climbTo(Tower.KITCHEN, "Up to the kitchen")
    }

    private fun StoryBuilder.storeBeats() {
        scene("beat_lockers") {
            title("The Lockers")
            body(
                """
                In the long locker, in a tin box lined with felt: three Coston distress rockets,
                dry, and a striker.

                Not a light. But red, and high, and visible for miles even in this.
                """,
            )
            learnAndReturn(Knowledge.ROCKETS, Tower.STORE)
        }
        scene("beat_rocket_taken") {
            title("Rocket")
            body("A tube the length of your forearm, and a striker. You take one and leave two.")
            back(Tower.STORE)
        }
        scene("beat_hatch_locked") {
            title("Locked")
            body(
                """
                The hatch does not give. It is a good lock and Kell keeps the key on him, or
                somewhere he thinks of as on him.
                """,
            )
            back(Tower.STORE)
        }
    }

    private fun StoryBuilder.cellar() = scene(Tower.CELLAR) {
        title("The Cellar")
        body(
            """
            Below the waterline of the rock. The walls sweat. Somewhere close and unseen the sea
            is working in a fissure with a sound like a slow door.

            Casks, a water butt, and the station's oil stock.
            """,
        )
        choice("Search the oil stock") {
            time(Cost.SEARCH)
            onlyIf { !it.knows(Knowledge.SPARE_OIL) }
            goto("beat_oil_stock")
        }
        choice("Take the sealed can") {
            time(Cost.QUICK)
            note("Behind the water butt, where it was put to be forgotten.")
            onlyIf { it.knows(Knowledge.SPARE_OIL) && !it.has(Item.CLEAN_OIL) }
            take(Item.CLEAN_OIL)
            goto("beat_can_taken")
        }
        choice("Up to the store room") {
            time(Cost.STEP)
            goto(Tower.STORE)
        }
    }

    private fun StoryBuilder.cellarBeats() {
        scene("beat_oil_stock") {
            title("The Oil Stock")
            body(
                """
                Four casks at the front, and you get the bung out of the first and smell it and
                it is the same brine-cut filth that is in the lamp. The second, the same. The
                third.

                Behind the water butt, where a man would put a thing he wanted out of the way
                rather than out of existence, there is a sealed five-gallon can of colza with the
                Board's stamp still whole on the cap.

                He fouled the stock. He did not have the nerve to destroy it.
                """,
            )
            learnAndReturn(Knowledge.SPARE_OIL, Tower.CELLAR)
        }
        scene("beat_can_taken") {
            title("Five Gallons")
            body("Forty pounds of it, and five floors to carry it. Take the rail with your free hand.")
            back(Tower.CELLAR)
        }
    }
}
