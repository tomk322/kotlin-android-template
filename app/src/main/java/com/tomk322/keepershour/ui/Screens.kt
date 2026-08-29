@file:Suppress("MagicNumber")

package com.tomk322.keepershour.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomk322.keepershour.engine.Ending
import com.tomk322.keepershour.engine.Flag
import com.tomk322.keepershour.engine.GameState
import com.tomk322.keepershour.engine.Knowledge
import com.tomk322.keepershour.engine.SceneView
import kotlinx.coroutines.launch

@Composable
fun TitleScreen(
    hasSave: Boolean,
    endingsFound: Int,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LampGlowBackdrop(lit = true)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("THE", style = Instrument, color = FoamDim)
            Text(
                text = "KEEPER'S\nHOUR",
                style = Display,
                color = LampGlow,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Corr Sgeir light, 21 November 1907",
                style = Footnote,
                color = FoamDim,
            )
            Spacer(Modifier.height(48.dp))

            if (hasSave) {
                ChoiceButton("Continue", 0, null, onContinue)
                Spacer(Modifier.height(10.dp))
            }
            ChoiceButton(
                label = if (hasSave) "Start again from the beginning" else "Begin",
                minutes = 0,
                note = if (hasSave) "This clears everything you have learned." else null,
                onClick = onNewGame,
            )

            Spacer(Modifier.height(40.dp))
            Text(
                text = "$endingsFound of ${Ending.entries.size} endings found",
                style = Instrument,
                color = FoamDim,
            )
        }
    }
}

/**
 * Full-screen prose: waking at eleven, a discovery beat, an ending.
 *
 * The world handles rooms; anything that is a moment rather than a place comes through here.
 */
@Composable
fun StoryCard(
    view: SceneView,
    onChoice: (String) -> Unit,
) {
    var skip by remember(view.text) { mutableStateOf(false) }
    var typed by remember(view.text) { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(view.text) { scroll.scrollTo(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        LampGlowBackdrop(lit = view.state.isSet(Flag.LAMP_LIT))

        Column(modifier = Modifier.fillMaxSize()) {
            if (!view.isEnding) {
                Text(
                    text = "${view.state.clock}  ·  ${view.state.minutesLeft} min left",
                    style = Instrument,
                    color = FoamDim,
                    modifier = Modifier.padding(start = 22.dp, top = 14.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .clickable(enabled = !typed) { skip = true }
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Text(
                    text = view.scene.title.uppercase(),
                    style = Instrument,
                    color = if (view.isEnding) LampGlow else LampAmber,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                TypedProse(text = view.text, skipToEnd = skip, onFinished = { typed = true })

                if (typed) {
                    ChoiceList(view) { id ->
                        scope.launch { scroll.scrollTo(0) }
                        onChoice(id)
                    }
                } else {
                    Text(
                        text = "tap to finish",
                        style = Footnote,
                        color = FoamDim.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceList(view: SceneView, onChoice: (String) -> Unit) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(vertical = 18.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (choice in view.choices) {
            ChoiceButton(
                label = choice.label,
                minutes = choice.minutes,
                note = choice.note,
                onClick = { onChoice(choice.id) },
            )
        }
    }
    Spacer(Modifier.height(28.dp))
}

/** What Mairead has worked out, and what she has managed so far. */
@Composable
fun JournalPanel(state: GameState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        KnowledgeSection(state)
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 10.dp),
        )
        EndingsSection(state)
        CarryingSection(state)
    }
}

@Composable
private fun KnowledgeSection(state: GameState) {
    Text("WHAT YOU KNOW", style = Instrument, color = LampAmber)
    Text(
        text = "${state.knowledge.size} of ${Knowledge.entries.size} · carried between loops",
        style = Footnote,
        color = FoamDim,
        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
    )
    if (state.knowledge.isEmpty()) {
        Text("Nothing yet. Go and look at things.", style = Prose, color = FoamDim)
    }
    for (fact in Knowledge.entries.filter { it in state.knowledge }) {
        Text(fact.title, style = ChoiceLabel, color = LampGlow)
        Text(
            text = fact.summary,
            style = Footnote,
            color = FoamDim,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
        )
    }
}

@Composable
private fun EndingsSection(state: GameState) {
    Text("ENDINGS FOUND", style = Instrument, color = LampAmber)
    Text(
        text = "${state.endingsSeen.size} of ${Ending.entries.size}",
        style = Footnote,
        color = FoamDim,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
    )
    for (ending in Ending.entries) {
        val found = ending in state.endingsSeen
        Text(
            text = if (found) ending.title else "· · · · ·",
            style = ChoiceLabel,
            color = if (found) Foam else FoamDim.copy(alpha = 0.45f),
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun CarryingSection(state: GameState) {
    if (state.items.isEmpty()) return
    Text("CARRYING", style = Instrument, color = LampAmber)
    Text(
        text = state.items.joinToString(", ") { it.label },
        style = Footnote,
        color = FoamDim,
        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
    )
}
