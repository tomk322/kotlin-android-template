package com.tomk322.keepershour.engine

/**
 * A tiny builder for the story graph.
 *
 * Choice ids are derived from labels so the content stays readable, but they are stable enough for
 * tests to drive a scripted playthrough by id. Collisions inside a scene are a build-time error
 * rather than a silent overwrite - see [StoryValidator].
 */
@DslMarker
annotation class StoryDslMarker

// A builder's whole job is to expose one verb per thing it can set.
@Suppress("TooManyFunctions")
@StoryDslMarker
class ChoiceBuilder(private val label: String) {
    private var id: String = slug(label)
    private var target: String = ""
    private var minutes: Int = Cost.FREE
    private var note: String? = null
    private val effects = mutableListOf<Effect>()
    private var predicate: (GameState) -> Boolean = { true }

    fun id(value: String) { id = value }
    fun goto(sceneId: String) { target = sceneId }
    fun time(value: Int) { minutes = value }
    fun note(value: String) { note = value }

    /** The choice only appears when this holds. */
    fun onlyIf(predicate: (GameState) -> Boolean) { this.predicate = predicate }

    fun learn(vararg facts: Knowledge) { facts.forEach { effects += Effect.Learn(it) } }
    fun take(vararg items: Item) { items.forEach { effects += Effect.Take(it) } }
    fun drop(vararg items: Item) { items.forEach { effects += Effect.Drop(it) } }
    fun raise(vararg flags: Flag) { flags.forEach { effects += Effect.Raise(it) } }
    fun lower(vararg flags: Flag) { flags.forEach { effects += Effect.Lower(it) } }
    fun suspect(amount: Int = 1) { effects += Effect.Suspect(amount) }
    fun finish(ending: Ending) { effects += Effect.Finish(ending) }
    fun waitOutTheHour() { effects += Effect.WaitOutTheHour }
    fun restart() { effects += Effect.Restart }

    fun build(sceneId: String): Choice {
        require(target.isNotBlank()) { "Choice '$id' in scene '$sceneId' has no target" }
        require(id.isNotBlank()) {
            "Choice '$label' in scene '$sceneId' slugified to an empty id; give it an explicit id()"
        }
        return Choice(id, label, target, minutes, effects.toList(), note, predicate)
    }

    private companion object {
        private const val MAX_ID_LENGTH = 40
        private val NON_WORD = Regex("[^a-z0-9]+")

        fun slug(text: String): String = text.lowercase()
            .replace(NON_WORD, "_")
            .trim('_')
            .take(MAX_ID_LENGTH)
    }
}

@StoryDslMarker
class SceneBuilder(private val id: String) {
    private var title: String = id
    private var body: (GameState) -> String = { "" }
    private var ending = false
    private val choices = mutableListOf<Choice>()

    fun title(value: String) { title = value }
    fun body(value: String) { body = { value } }
    fun body(value: (GameState) -> String) { body = value }
    fun markEnding() { ending = true }

    fun choice(label: String, block: ChoiceBuilder.() -> Unit) {
        choices += ChoiceBuilder(label).apply(block).build(id)
    }

    fun build(): Scene = Scene(id, title, body, choices.toList(), ending)
}

@StoryDslMarker
class StoryBuilder {
    private val scenes = mutableListOf<Scene>()

    fun scene(id: String, block: SceneBuilder.() -> Unit) {
        scenes += SceneBuilder(id).apply(block).build()
    }

    fun include(built: List<Scene>) { scenes += built }

    fun build(start: String): Story {
        val duplicates = scenes.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate scene ids: $duplicates" }
        return Story(start, scenes.associateBy { it.id })
    }
}

fun buildScenes(block: StoryBuilder.() -> Unit): List<Scene> {
    val builder = StoryBuilder()
    builder.block()
    return builder.build("unused").scenes.values.toList()
}

fun story(start: String, block: StoryBuilder.() -> Unit): Story =
    StoryBuilder().apply(block).build(start)
