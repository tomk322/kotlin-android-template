package com.tomk322.keepershour.engine

import java.util.Locale

/**
 * A fact Mairead has learned. Knowledge is the only thing that survives a loop: the rock, the
 * storm and the ship reset every time, but what she understands does not. Every shortcut in the
 * game is gated on one of these.
 */
enum class Knowledge(val title: String, val summary: String) {
    OIL_FOULED(
        "The oil is fouled",
        "The reservoir oil is cut with seawater. It will never hold a flame.",
    ),
    CELLAR_KEY(
        "The cellar key",
        "The cellar key hangs on a nail behind the kitchen door, under the oilskins.",
    ),
    SPARE_OIL(
        "Sealed oil in the cellar",
        "A sealed can of clean colza oil sits in the cellar, behind the water butt.",
    ),
    ROCKETS(
        "Distress rockets",
        "Three Coston distress rockets are in the store room locker.",
    ),
    FOGHORN_SABOTAGED(
        "The foghorn was cut",
        "The foghorn's air line has been disconnected by hand. Not weather. Not wear.",
    ),
    EZRA_DRUNK(
        "Kell is drinking",
        "Ezra Kell has been at the whisky since sundown, and means to keep at it.",
    ),
    OSSIAN_FELL(
        "Ossian's fall",
        "The last assistant keeper, Ossian Rue, fell from the gallery in September. " +
            "Kell wrote it up as weather.",
    ),
    LOOSE_FLAGSTONE(
        "The loose flagstone",
        "There is a lifting flagstone under Ossian's old bunk in the bedroom.",
    ),
    OSSIAN_LOGBOOK(
        "Ossian's second logbook",
        "Ossian kept a private log. He recorded three nights the light was dimmed on purpose, " +
            "and the last entry says he had told Kell he was going to the Board.",
    ),
    FALSE_LIGHTS(
        "Lights on the headland",
        "Lanterns move in a line on Bail' Ard headland, where no road runs. " +
            "Someone is laying a false light to draw ships onto the reef.",
    ),
    EZRA_PAID(
        "Kell was paid",
        "Eleven sovereigns and a folded letter in Kell's chest. The letter names a night, " +
            "a price, and no sender.",
    ),
    AILSA_ABOARD(
        "Ailsa sails tonight",
        "An unopened letter in Kell's room. His daughter Ailsa took a berth on the " +
            "Ardnamurchan to come home to him. He never broke the seal.",
    ),
    ;

    companion object {
        /** Stable ids for save files, so reordering the enum cannot corrupt a save. */
        fun fromId(id: String): Knowledge? = entries.firstOrNull { it.name == id }
    }
}

/** Objects Mairead is carrying. Cleared at the start of every loop. */
enum class Item(val label: String) {
    CELLAR_KEY("cellar key"),
    CLEAN_OIL("can of clean oil"),
    ROCKET("distress rocket"),
    LOGBOOK("Ossian's logbook"),
    LETTER("Ailsa's letter"),
    SOVEREIGNS("eleven sovereigns"),
    ;

    companion object {
        fun fromId(id: String): Item? = entries.firstOrNull { it.name == id }
    }
}

/** World state within a single loop. Cleared at the start of every loop. */
enum class Flag {
    LAMP_DRAINED,
    LAMP_REFILLED,
    LAMP_LIT,
    FOGHORN_FIXED,
    ROCKET_FIRED,
    EZRA_ASLEEP,
    EZRA_EXPOSED,
    EZRA_BROKEN,
    LINE_SECURED,
    SEARCHED_CHEST,
    ;

    companion object {
        fun fromId(id: String): Flag? = entries.firstOrNull { it.name == id }
    }
}

/**
 * How a loop ended. Only [THE_KEEPERS_HOUR] breaks the cycle; every other ending is recorded and
 * then Mairead wakes at eleven o'clock again, which is the whole point of the thing.
 */
enum class Ending(
    val title: String,
    val breaksLoop: Boolean = false,
) {
    THE_ROCKS("The Rocks"),
    DROWNED("Salt and Dark"),
    LOCKED_IN("The Store Room Door"),
    LESSER_LIGHT("A Lesser Light"),
    CLEAN_BURN("Clean Burn"),
    THE_RECKONING("The Reckoning"),
    THE_KEEPERS_HOUR("The Keeper's Hour", breaksLoop = true),
    ;

    companion object {
        fun fromId(id: String): Ending? = entries.firstOrNull { it.name == id }
    }
}

/**
 * The whole game state.
 *
 * Split deliberately in two: [knowledge], [loop] and [endingsSeen] persist across loops, while
 * [items], [flags], [minutesElapsed] and [suspicion] are wiped by [nextLoop]. Every design
 * question in this game reduces to which side of that line a thing sits on.
 */
data class GameState(
    val sceneId: String,
    val loop: Int = 1,
    val minutesElapsed: Int = 0,
    val suspicion: Int = 0,
    val knowledge: Set<Knowledge> = emptySet(),
    val items: Set<Item> = emptySet(),
    val flags: Set<Flag> = emptySet(),
    val endingsSeen: Set<Ending> = emptySet(),
    /** Set only while an ending is being shown; cleared when the next loop starts. */
    val currentEnding: Ending? = null,
) {
    val minutesLeft: Int get() = (LOOP_MINUTES - minutesElapsed).coerceAtLeast(0)

    /** Wall clock, counting from eleven at night. */
    val clock: String
        get() {
            val total = START_HOUR * MINUTES_PER_HOUR + minutesElapsed
            val hours = (total / MINUTES_PER_HOUR) % HOURS_PER_DAY
            val minutes = total % MINUTES_PER_HOUR
            return "%02d:%02d".format(Locale.ROOT, hours, minutes)
        }

    fun knows(fact: Knowledge): Boolean = fact in knowledge
    fun has(item: Item): Boolean = item in items
    fun isSet(flag: Flag): Boolean = flag in flags

    /** Wipes everything the rock forgets, keeping everything Mairead does not. */
    fun nextLoop(startScene: String): GameState = GameState(
        sceneId = startScene,
        loop = loop + 1,
        minutesElapsed = 0,
        suspicion = 0,
        knowledge = knowledge,
        items = emptySet(),
        flags = emptySet(),
        endingsSeen = endingsSeen,
        currentEnding = null,
    )

    companion object {
        const val LOOP_MINUTES = 60
        const val START_HOUR = 23
        const val MINUTES_PER_HOUR = 60
        const val HOURS_PER_DAY = 24

        /** Kell throws her in the store room once he is this suspicious. */
        const val SUSPICION_LIMIT = 3
    }
}

/** Named action costs, so the story reads in intent rather than in numbers. */
object Cost {
    const val FREE = 0
    const val GLANCE = 1
    const val STEP = 1
    const val QUICK = 2
    const val ROUTINE = 3
    const val SEARCH = 5
    const val TALK = 5
    const val WORK = 6
    const val THOROUGH = 8
}
