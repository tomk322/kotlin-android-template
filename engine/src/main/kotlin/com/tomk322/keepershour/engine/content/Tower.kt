package com.tomk322.keepershour.engine.content

import com.tomk322.keepershour.engine.Cost
import com.tomk322.keepershour.engine.SceneBuilder

/**
 * The tower, bottom to top. Climbing costs a minute a floor, which is the single most important
 * number in the game: it is what makes knowing where a thing is worth more than the thing itself.
 */
object Tower {
    const val CELLAR = "cellar"
    const val STORE = "store"
    const val KITCHEN = "kitchen"
    const val BEDROOM = "bedroom"
    const val SERVICE = "service_room"
    const val LAMP = "lamp_room"
    const val GALLERY = "gallery"
}

internal fun SceneBuilder.climbTo(sceneId: String, label: String) {
    choice(label) {
        time(Cost.STEP)
        goto(sceneId)
    }
}

/** A no-cost step back to the room the beat happened in. */
internal fun SceneBuilder.back(sceneId: String, label: String = "Turn back to the room") {
    choice(label) {
        time(Cost.FREE)
        goto(sceneId)
    }
}
