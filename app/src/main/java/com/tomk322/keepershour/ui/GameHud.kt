// Layout dimensions and alpha values are design decisions, not extractable constants.
@file:Suppress("MagicNumber")

package com.tomk322.keepershour.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tomk322.keepershour.engine.GameState

/** The warm wash at the top of the screen. It grows when the lamp is burning. */
@Composable
fun LampGlowBackdrop(lit: Boolean, modifier: Modifier = Modifier) {
    val strength by animateFloatAsState(
        targetValue = if (lit) 0.30f else 0.07f,
        animationSpec = tween(durationMillis = 1400),
        label = "lampGlow",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(LampAmber.copy(alpha = strength), Color.Transparent),
                    center = Offset(0.5f, 0f),
                    radius = 1400f,
                ),
            ),
    )
}

/**
 * Clock, loop counter and the bar that runs out.
 *
 * The bar is the only thing on screen that moves on its own, which is the point: it is the ship
 * getting closer.
 */
@Composable
fun Hud(
    state: GameState,
    onJournal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = state.minutesLeft.toFloat() / GameState.LOOP_MINUTES
    val barColour by animateColorAsState(
        targetValue = when {
            fraction > 0.5f -> Verdigris
            fraction > 0.2f -> LampAmber
            else -> Rust
        },
        animationSpec = tween(600),
        label = "timeBar",
    )
    val width by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(400),
        label = "timeWidth",
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.clock,
                style = Clock,
                color = barColour,
            )
            Text(
                text = "  ·  ${state.minutesLeft} min",
                style = Instrument,
                color = FoamDim,
            )
            Box(modifier = Modifier.weight(1f))
            if (state.suspicion > 0) {
                Text(
                    text = "KELL " + "!".repeat(state.suspicion),
                    style = Instrument,
                    color = Rust,
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
            Text(
                text = "LOOP ${state.loop}",
                style = Instrument,
                color = FoamDim,
            )
            TextButton(onClick = onJournal) {
                Text("JOURNAL", style = Instrument, color = LampAmber)
            }
        }

        TimeBar(
            fraction = width,
            colour = barColour,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

/** The hour, drawn as a line that shortens. */
@Composable
private fun TimeBar(fraction: Float, colour: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.outline),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(2.dp)
                .clip(MaterialTheme.shapes.small)
                .background(colour),
        )
    }
}

/** A choice, with its cost and any hint that knowledge unlocked it. */
@Composable
fun ChoiceButton(
    label: String,
    minutes: Int,
    note: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = SeaRaised,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = ChoiceLabel, color = Foam)
                if (note != null) {
                    Text(
                        text = note,
                        style = Footnote,
                        color = FoamDim,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (minutes > 0) {
                Text(
                    text = "${minutes}m",
                    style = Instrument,
                    color = LampAmber,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}
