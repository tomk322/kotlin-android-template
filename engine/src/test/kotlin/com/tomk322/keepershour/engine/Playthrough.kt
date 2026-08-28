package com.tomk322.keepershour.engine

/**
 * Drives the engine the way a player would, so tests read as transcripts.
 *
 * Every step asserts the choice was actually offered, which means a playthrough test fails the
 * moment a condition changes and silently removes an option from the critical path.
 */
class Playthrough(private val engine: GameEngine = KeepersHour.newEngine()) {

    var state: GameState = engine.initialState()
        private set

    private val trail = mutableListOf<String>()

    /** Takes the choice with this id, failing loudly if it is not on offer. */
    fun take(choiceId: String): Playthrough {
        val view = engine.view(state)
        val choice = view.choices.firstOrNull { it.id == choiceId }
            ?: error(
                "In scene '${state.sceneId}' at ${state.clock} there is no available choice " +
                    "'$choiceId'.\nOffered: ${view.choices.map { it.id }}\n" +
                    "Trail: ${trail.takeLast(TRAIL_TAIL)}",
            )
        trail += "${state.sceneId}/${choice.id}"
        state = engine.apply(state, choiceId)
        return this
    }

    fun takeAll(vararg choiceIds: String): Playthrough {
        choiceIds.forEach { take(it) }
        return this
    }

    /** Grants knowledge outright, standing in for the loops a player would spend earning it. */
    fun seed(facts: Set<Knowledge>): Playthrough {
        state = state.copy(knowledge = state.knowledge + facts, loop = state.loop + facts.size)
        return this
    }

    fun offered(): List<String> = engine.view(state).choices.map { it.id }

    fun at(): String = state.sceneId

    val ending: Ending? get() = state.currentEnding

    val history: List<String> get() = trail.toList()

    private companion object {
        const val TRAIL_TAIL = 12
    }
}
