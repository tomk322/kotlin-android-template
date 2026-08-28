package com.tomk322.keepershour.engine

/** A state mutation produced by taking a choice. */
sealed interface Effect {
    data class Learn(val fact: Knowledge) : Effect
    data class Take(val item: Item) : Effect
    data class Drop(val item: Item) : Effect
    data class Raise(val flag: Flag) : Effect
    data class Lower(val flag: Flag) : Effect
    data class Suspect(val amount: Int) : Effect

    /** Ends the loop immediately with a fixed outcome, bypassing the clock. */
    data class Finish(val ending: Ending) : Effect

    /** Burns the rest of the hour, letting the clock resolve the outcome. */
    data object WaitOutTheHour : Effect

    /** Wipes the loop and starts over, keeping knowledge. */
    data object Restart : Effect
}

/**
 * One action available in a scene.
 *
 * [available] is a plain predicate rather than a data structure: conditions here are ad hoc
 * ("knows this and is not carrying that and it is not yet half past"), and expressing them as
 * Kotlin keeps the story legible. Reachability is proven by the tests instead.
 */
data class Choice(
    val id: String,
    val label: String,
    val target: String,
    val minutes: Int = Cost.FREE,
    val effects: List<Effect> = emptyList(),
    /** Shown under the label; used to telegraph what knowledge unlocked a shortcut. */
    val note: String? = null,
    val available: (GameState) -> Boolean = { true },
)

data class Scene(
    val id: String,
    val title: String,
    val body: (GameState) -> String,
    val choices: List<Choice>,
    /** Ending scenes render differently and only offer "wake". */
    val isEnding: Boolean = false,
)

data class Story(
    val start: String,
    val scenes: Map<String, Scene>,
) {
    operator fun get(id: String): Scene =
        scenes[id] ?: error("No scene with id '$id'")
}
