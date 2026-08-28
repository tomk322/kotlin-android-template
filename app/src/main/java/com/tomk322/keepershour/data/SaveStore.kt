package com.tomk322.keepershour.data

import android.content.Context
import androidx.core.content.edit
import com.tomk322.keepershour.engine.Ending
import com.tomk322.keepershour.engine.Flag
import com.tomk322.keepershour.engine.GameState
import com.tomk322.keepershour.engine.Item
import com.tomk322.keepershour.engine.Knowledge
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the run to shared preferences as JSON.
 *
 * Enum *names* are written rather than ordinals, and unknown names are dropped on load, so
 * reordering or removing a Knowledge entry in a later version degrades a save instead of
 * corrupting it.
 */
class SaveStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): GameState? {
        val raw = prefs.getString(KEY_STATE, null) ?: return null
        return runCatching { decode(JSONObject(raw)) }.getOrNull()
    }

    fun save(state: GameState) {
        prefs.edit { putString(KEY_STATE, encode(state).toString()) }
    }

    fun clear() {
        prefs.edit { remove(KEY_STATE) }
    }

    private fun encode(state: GameState) = JSONObject().apply {
        put("scene", state.sceneId)
        put("loop", state.loop)
        put("minutes", state.minutesElapsed)
        put("suspicion", state.suspicion)
        put("knowledge", names(state.knowledge.map { it.name }))
        put("items", names(state.items.map { it.name }))
        put("flags", names(state.flags.map { it.name }))
        put("endings", names(state.endingsSeen.map { it.name }))
        put("currentEnding", state.currentEnding?.name ?: JSONObject.NULL)
    }

    private fun decode(json: JSONObject) = GameState(
        sceneId = json.getString("scene"),
        loop = json.getInt("loop"),
        minutesElapsed = json.getInt("minutes"),
        suspicion = json.optInt("suspicion", 0),
        knowledge = read(json, "knowledge", Knowledge::fromId),
        items = read(json, "items", Item::fromId),
        flags = read(json, "flags", Flag::fromId),
        endingsSeen = read(json, "endings", Ending::fromId),
        currentEnding = json.optString("currentEnding")
            .takeIf { it.isNotBlank() && it != "null" }
            ?.let(Ending::fromId),
    )

    private fun names(values: List<String>) = JSONArray().apply {
        for (value in values) put(value)
    }

    private fun <T> read(json: JSONObject, key: String, parse: (String) -> T?): Set<T> {
        val array = json.optJSONArray(key) ?: return emptySet()
        return (0 until array.length()).mapNotNull { parse(array.getString(it)) }.toSet()
    }

    private companion object {
        const val PREFS = "keepers_hour"
        const val KEY_STATE = "state"
    }
}
