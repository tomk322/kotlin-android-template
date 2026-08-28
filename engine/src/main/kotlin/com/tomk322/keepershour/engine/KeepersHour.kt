package com.tomk322.keepershour.engine

import com.tomk322.keepershour.engine.content.Endings
import com.tomk322.keepershour.engine.content.Kell
import com.tomk322.keepershour.engine.content.Opening
import com.tomk322.keepershour.engine.content.TowerLower
import com.tomk322.keepershour.engine.content.TowerTop

/** The assembled story graph. */
object KeepersHour {

    val story: Story by lazy {
        story(start = "awaken") {
            include(Opening.scenes())
            include(TowerTop.scenes())
            include(TowerLower.scenes())
            include(Kell.scenes())
            include(Endings.scenes())
        }
    }

    fun newEngine(): GameEngine = GameEngine(story)
}
