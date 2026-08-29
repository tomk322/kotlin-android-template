package com.tomk322.keepershour

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tomk322.keepershour.data.SaveStore
import com.tomk322.keepershour.engine.GameEngine
import com.tomk322.keepershour.engine.GameState
import com.tomk322.keepershour.engine.KeepersHour
import com.tomk322.keepershour.engine.world.Lighthouse
import com.tomk322.keepershour.engine.world.Room
import com.tomk322.keepershour.ui.JournalPanel
import com.tomk322.keepershour.ui.KeepersHourTheme
import com.tomk322.keepershour.ui.SeaSurface
import com.tomk322.keepershour.ui.StoryCard
import com.tomk322.keepershour.ui.TitleScreen
import com.tomk322.keepershour.ui.game.Keeper
import com.tomk322.keepershour.ui.game.WorldActions
import com.tomk322.keepershour.ui.game.WorldScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = SaveStore(this)
        val engine = KeepersHour.newEngine()

        setContent {
            KeepersHourTheme {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = MaterialTheme.colorScheme.background,
                ) { padding ->
                    Game(
                        store = store,
                        engine = engine,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                }
            }
        }
    }
}

/** Every choice's destination, looked up once rather than rescanned on every move. */
private val choiceTargets: Map<String, String> by lazy {
    KeepersHour.story.scenes.values
        .flatMap { it.choices }
        .associate { it.id to it.target }
}

/**
 * Where the keeper should stand after a move, or null to leave her exactly where she is.
 *
 * She is relocated when she walks between rooms, and also whenever she somehow ends up somewhere
 * she could not stand - which is what a loop restart does, since it keeps her coordinates but
 * changes the room underneath them.
 */
private fun relocate(from: String, to: String, keeper: Keeper): Keeper? {
    val arrived = Lighthouse[to]
    val changedRooms = Lighthouse.isRoom(from) && to != from
    val stranded = arrived != null && !arrived.canOccupy(keeper.x, keeper.y, Room.BODY_RADIUS)

    return if (arrived == null || !(changedRooms || stranded)) {
        null
    } else {
        arrived.arrivalPoint(from) { choiceTargets[it] }
            .let { (x, y) -> Keeper(x, y) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Game(store: SaveStore, engine: GameEngine, modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf(store.load()) }
    var onTitle by remember { mutableStateOf(true) }
    var journalOpen by remember { mutableStateOf(false) }
    var keeper by remember { mutableStateOf(Keeper(Room.CENTRE, Room.CENTRE + 1f)) }

    fun choose(from: GameState, choiceId: String) {
        val next = engine.apply(from, choiceId)
        relocate(from.sceneId, next.sceneId, keeper)?.let { keeper = it }
        state = next
        store.save(next)
    }

    Box(modifier = modifier) {
        val current = state
        when {
            onTitle || current == null -> TitleScreen(
                hasSave = current != null,
                endingsFound = current?.endingsSeen?.size ?: 0,
                onContinue = { onTitle = false },
                onNewGame = {
                    store.clear()
                    val fresh = engine.initialState()
                    state = fresh
                    store.save(fresh)
                    keeper = Keeper(Room.CENTRE, Room.CENTRE + 1f)
                    onTitle = false
                },
            )

            Lighthouse.isRoom(current.sceneId) -> WorldScreen(
                state = current,
                engine = engine,
                keeper = keeper,
                actions = WorldActions(
                    onKeeperMoved = { keeper = it },
                    onChoice = { choose(current, it) },
                    onJournal = { journalOpen = true },
                ),
            )

            // Everything that is not a room - waking, discovery beats, endings - is a story card.
            else -> StoryCard(
                view = engine.view(current),
                onChoice = { choose(current, it) },
            )
        }

        if (journalOpen && current != null) {
            ModalBottomSheet(
                onDismissRequest = { journalOpen = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = SeaSurface,
            ) {
                JournalPanel(current)
            }
        }
    }
}
