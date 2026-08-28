package com.tomk322.keepershour.engine

/** What a scene looks like to the UI, once conditions have been resolved against the state. */
data class SceneView(
    val scene: Scene,
    val text: String,
    val choices: List<Choice>,
    val state: GameState,
) {
    val isEnding: Boolean get() = scene.isEnding
}

/**
 * Applies choices to state and decides when the hour is up.
 *
 * The engine is deliberately pure: every method takes a state and returns a new one, which is what
 * lets the whole game be driven from unit tests without an emulator anywhere in sight.
 */
class GameEngine(private val story: Story) {

    fun initialState(): GameState = GameState(sceneId = story.start)

    fun view(state: GameState): SceneView {
        val scene = story[state.sceneId]
        return SceneView(
            scene = scene,
            text = scene.body(state).trimIndent().trim(),
            choices = scene.choices.filter { it.available(state) },
            state = state,
        )
    }

    /** Returns the choice with [choiceId] if it is currently available, else null. */
    fun choice(state: GameState, choiceId: String): Choice? =
        view(state).choices.firstOrNull { it.id == choiceId }

    /**
     * Applies a choice.
     *
     * Order matters: the clock is charged first, because an action Mairead cannot finish before
     * midnight is an action that does not happen at all.
     */
    fun apply(state: GameState, choiceId: String): GameState {
        val chosen = choice(state, choiceId)
            ?: error("Choice '$choiceId' is not available in scene '${state.sceneId}'")
        val spent = state.minutesElapsed + chosen.minutes

        return when {
            Effect.Restart in chosen.effects -> state.nextLoop(story.start)

            // Started something she could not finish. The hour ends where it ends.
            spent > GameState.LOOP_MINUTES ->
                resolve(state.copy(minutesElapsed = GameState.LOOP_MINUTES))

            else -> settle(
                chosen.effects.fold(state.copy(minutesElapsed = spent, sceneId = chosen.target)) {
                    running, effect ->
                    applyEffect(running, effect)
                },
            )
        }
    }

    /** Whatever the effects did, the hour and Kell's patience still get the last word. */
    private fun settle(state: GameState): GameState = when {
        state.currentEnding != null -> state
        state.minutesElapsed >= GameState.LOOP_MINUTES -> resolve(state)
        state.suspicion >= GameState.SUSPICION_LIMIT -> end(state, Ending.LOCKED_IN)
        else -> state
    }

    private fun applyEffect(state: GameState, effect: Effect): GameState = when (effect) {
        is Effect.Learn -> state.copy(knowledge = state.knowledge + effect.fact)
        is Effect.Take -> state.copy(items = state.items + effect.item)
        is Effect.Drop -> state.copy(items = state.items - effect.item)
        is Effect.Raise -> state.copy(flags = state.flags + effect.flag)
        is Effect.Lower -> state.copy(flags = state.flags - effect.flag)
        is Effect.Suspect -> state.copy(suspicion = state.suspicion + effect.amount)
        is Effect.Finish -> end(state, effect.ending)
        Effect.WaitOutTheHour -> resolve(state.copy(minutesElapsed = GameState.LOOP_MINUTES))
        Effect.Restart -> state.nextLoop(story.start)
    }

    /**
     * Works out how the hour ended from what Mairead actually managed to do.
     *
     * Ordered best to worst, so the strongest outcome she earned is the one she gets.
     */
    private fun resolve(state: GameState): GameState {
        val ending = when {
            state.isSet(Flag.EZRA_BROKEN) -> Ending.THE_KEEPERS_HOUR
            state.isSet(Flag.LAMP_LIT) && state.isSet(Flag.EZRA_EXPOSED) -> Ending.THE_RECKONING
            state.isSet(Flag.LAMP_LIT) -> Ending.CLEAN_BURN
            state.isSet(Flag.ROCKET_FIRED) -> Ending.LESSER_LIGHT
            else -> Ending.THE_ROCKS
        }
        return end(state, ending)
    }

    private fun end(state: GameState, ending: Ending): GameState = state.copy(
        sceneId = endingSceneId(ending),
        currentEnding = ending,
        endingsSeen = state.endingsSeen + ending,
    )

    companion object {
        fun endingSceneId(ending: Ending): String = "ending_${ending.name.lowercase()}"
    }
}
