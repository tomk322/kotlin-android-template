@file:Suppress("MagicNumber")

package com.tomk322.keepershour.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.tomk322.keepershour.engine.Choice
import com.tomk322.keepershour.engine.Flag
import com.tomk322.keepershour.engine.GameEngine
import com.tomk322.keepershour.engine.GameState
import com.tomk322.keepershour.engine.content.Tower
import com.tomk322.keepershour.engine.world.Interactable
import com.tomk322.keepershour.engine.world.Lighthouse
import com.tomk322.keepershour.engine.world.Room
import com.tomk322.keepershour.ui.ChoiceButton
import com.tomk322.keepershour.ui.Footnote
import com.tomk322.keepershour.ui.FoamDim
import com.tomk322.keepershour.ui.Hud
import com.tomk322.keepershour.ui.Instrument
import com.tomk322.keepershour.ui.LampAmber
import com.tomk322.keepershour.ui.SeaSurface
import kotlin.math.hypot

private const val WALK_TILES_PER_SECOND = 3.4f
private const val NANOS_PER_SECOND = 1_000_000_000f

/** Where the keeper is standing, kept outside the engine because the story does not care. */
data class Keeper(val x: Float, val y: Float, val facing: Offset = Offset(0f, 1f))

/**
 * The playable room.
 *
 * Everything the player does here still goes through [GameEngine] with the same choice ids the
 * text version used - walking up to the sea chest and pressing ACT applies exactly the choice a
 * menu would have. The world is a way of reaching the story, not a second implementation of it.
 */
/** What the world screen can hand back to the host. */
data class WorldActions(
    val onKeeperMoved: (Keeper) -> Unit,
    val onChoice: (String) -> Unit,
    val onJournal: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldScreen(
    state: GameState,
    engine: GameEngine,
    keeper: Keeper,
    actions: WorldActions,
) {
    val room = Lighthouse[state.sceneId] ?: return
    var stick by remember { mutableStateOf(Offset.Zero) }
    var walkPhase by remember { mutableStateOf(0f) }
    var openObject by remember(state.sceneId) { mutableStateOf<Interactable?>(null) }

    val nearby = room.interactableNear(keeper.x, keeper.y)
    val reachable = nearby?.let { available(engine, state, it) }.orEmpty()
    val prompt = nearby?.label?.takeIf { reachable.isNotEmpty() }

    MovementLoop(room, keeper, { stick }, actions.onKeeperMoved) { walkPhase = it }

    Box(modifier = Modifier.fillMaxSize()) {
        RoomCanvas(
            RoomView(
                room = room,
                playerX = keeper.x,
                playerY = keeper.y,
                facing = keeper.facing,
                walkPhase = walkPhase,
                highlighted = nearby.takeIf { reachable.isNotEmpty() },
                lampLit = state.isSet(Flag.LAMP_LIT),
                raining = room.id == Tower.GALLERY,
            ),
        )

        WorldOverlay(state, room.name, prompt, actions.onJournal)

        Thumbstick(
            onMove = { stick = it },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 36.dp),
        )
        ActionButton(
            label = prompt,
            onPress = {
                val single = reachable.singleOrNull()
                // A lone action leading to another room is a door: just walk through it.
                if (single != null && Lighthouse.isRoom(single.target)) {
                    actions.onChoice(single.id)
                } else {
                    openObject = nearby
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 34.dp, bottom = 46.dp),
        )

        val focused = openObject
        if (focused != null) {
            ModalBottomSheet(
                onDismissRequest = { openObject = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = SeaSurface,
            ) {
                ObjectMenu(focused, available(engine, state, focused)) { choiceId ->
                    openObject = null
                    actions.onChoice(choiceId)
                }
            }
        }
    }
}

/** Clock, room name and the "you could act on this" prompt, drawn over the room. */
@Composable
private fun BoxScope.WorldOverlay(
    state: GameState,
    roomName: String,
    prompt: String?,
    onJournal: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopStart)) {
        Hud(state, onJournal)
        Text(
            text = roomName.uppercase(),
            style = Instrument,
            color = FoamDim,
            modifier = Modifier.padding(start = 20.dp, top = 6.dp),
        )
    }
    if (prompt != null) {
        Text(
            text = prompt,
            style = Instrument,
            color = LampAmber,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 190.dp),
        )
    }
    // Shown until the first discovery, then never again.
    if (state.loop == 1 && state.knowledge.isEmpty()) {
        Text(
            text = "drag to walk  ·  ACT when something is in reach",
            style = Footnote,
            color = FoamDim,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 148.dp),
        )
    }
}

@Composable
private fun ObjectMenu(thing: Interactable, actions: List<Choice>, onPick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(thing.label.uppercase(), style = Instrument, color = LampAmber)
        if (actions.isEmpty()) {
            Text("Nothing to do here just now.", style = Footnote, color = FoamDim)
        }
        for (action in actions) {
            ChoiceButton(
                label = action.label,
                minutes = action.minutes,
                note = action.note,
                onClick = { onPick(action.id) },
            )
        }
        Text(
            text = "Costs are in minutes of the hour you have left.",
            style = Footnote,
            color = FoamDim,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** The object's own actions, in the order the world declares them, filtered to what is possible. */
private fun available(engine: GameEngine, state: GameState, thing: Interactable): List<Choice> {
    val offered = engine.view(state).choices.associateBy { it.id }
    return thing.choiceIds.mapNotNull { offered[it] }
}

/**
 * Advances the keeper once per frame.
 *
 * Axes are resolved separately so that walking into a wall at an angle slides along it instead of
 * stopping dead, which matters a lot in rooms this cluttered.
 */
@Composable
private fun MovementLoop(
    room: Room,
    keeper: Keeper,
    stick: () -> Offset,
    onMoved: (Keeper) -> Unit,
    onPhase: (Float) -> Unit,
) {
    // The frame loop outlives every recomposition, so it must not close over the parameters it
    // was launched with: read them through State that recomposition keeps current, or the keeper
    // walks one step away from where she spawned and stays there for ever.
    val position by rememberUpdatedState(keeper)
    val moved by rememberUpdatedState(onMoved)
    val phased by rememberUpdatedState(onPhase)

    LaunchedEffect(room.id) {
        var previous = 0L
        var phase = 0f
        while (true) {
            withFrameNanos { now ->
                val delta = if (previous == 0L) 0f else (now - previous) / NANOS_PER_SECOND
                previous = now
                val direction = stick()
                if (direction != Offset.Zero && delta > 0f) {
                    val step = WALK_TILES_PER_SECOND * delta
                    val current = position
                    var x = current.x
                    var y = current.y
                    val tryX = x + direction.x * step
                    if (canStand(room, tryX, y)) x = tryX
                    val tryY = y + direction.y * step
                    if (canStand(room, x, tryY)) y = tryY
                    phase += step * 7f
                    phased(phase)
                    if (x != current.x || y != current.y) {
                        moved(Keeper(x, y, normalise(direction)))
                    }
                }
            }
        }
    }
}

private fun normalise(v: Offset): Offset {
    val length = hypot(v.x, v.y)
    return if (length == 0f) Offset(0f, 1f) else Offset(v.x / length, v.y / length)
}

private fun canStand(room: Room, x: Float, y: Float): Boolean =
    room.canOccupy(x, y, Room.BODY_RADIUS)
