package com.tomk322.keepershour.engine

/** A structural problem in the story graph. */
data class StoryProblem(val kind: String, val detail: String) {
    override fun toString(): String = "[$kind] $detail"
}

/**
 * Static checks over the scene graph.
 *
 * These catch the failure mode that actually bites when writing branching content: a typo in a
 * target id that nothing notices until a player taps that one choice on a device you do not have.
 */
object StoryValidator {

    fun problems(story: Story): List<StoryProblem> = buildList {
        addAll(danglingTargets(story))
        addAll(duplicateChoiceIds(story))
        addAll(unreachableScenes(story))
        addAll(deadEnds(story))
        addAll(unobtainableKnowledge(story))
        addAll(unreachableEndings(story))
    }

    /** Every `goto` must name a scene that exists. */
    private fun danglingTargets(story: Story) = story.scenes.values.flatMap { scene ->
        scene.choices
            .filter { it.target !in story.scenes }
            .map { StoryProblem("dangling", "${scene.id}/${it.id} -> '${it.target}'") }
    }

    /** Two choices sharing an id in one scene would make the second unselectable. */
    private fun duplicateChoiceIds(story: Story) = story.scenes.values.flatMap { scene ->
        scene.choices.groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
            .map { StoryProblem("duplicate-choice", "${scene.id}/$it") }
    }

    /**
     * Walks the graph ignoring conditions. A scene unreachable even with every choice unlocked is
     * dead content; one reachable only under some condition is the tests' problem, not this one.
     *
     * Ending scenes are seeded as roots because the engine jumps to them directly when the hour
     * resolves, so no `goto` points at most of them. That they are each *actually* achievable is
     * proven by playing to them in EndingsReachableTest, which is the stronger check anyway.
     */
    private fun unreachableScenes(story: Story): List<StoryProblem> {
        val roots = listOf(story.start) + Ending.entries.map { GameEngine.endingSceneId(it) }
        val seen = roots.toMutableSet()
        val queue = ArrayDeque(roots)
        while (queue.isNotEmpty()) {
            val scene = story.scenes[queue.removeFirst()] ?: continue
            for (choice in scene.choices) {
                if (choice.target in story.scenes && seen.add(choice.target)) {
                    queue += choice.target
                }
            }
        }
        return (story.scenes.keys - seen).map { StoryProblem("unreachable", it) }
    }

    /** A non-ending scene with no way out would strand the player. */
    private fun deadEnds(story: Story) = story.scenes.values
        .filter { it.choices.isEmpty() }
        .map { StoryProblem("dead-end", it.id) }

    /** Every fact must be learnable somewhere, or a knowledge gate can never open. */
    private fun unobtainableKnowledge(story: Story): List<StoryProblem> {
        val taught = story.scenes.values
            .flatMap { it.choices }
            .flatMap { it.effects }
            .filterIsInstance<Effect.Learn>()
            .map { it.fact }
            .toSet()
        return (Knowledge.entries - taught).map { StoryProblem("unobtainable", it.name) }
    }

    /** Every ending needs a scene, and every ending scene needs to be flagged as one. */
    private fun unreachableEndings(story: Story) = Ending.entries.mapNotNull { ending ->
        val id = GameEngine.endingSceneId(ending)
        when {
            id !in story.scenes -> StoryProblem("missing-ending-scene", id)
            story.scenes.getValue(id).isEnding.not() ->
                StoryProblem("ending-not-marked", id)
            else -> null
        }
    }
}
