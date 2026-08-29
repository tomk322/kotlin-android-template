@file:Suppress("MagicNumber")

package com.tomk322.keepershour.ui.game

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.tomk322.keepershour.ui.Instrument
import com.tomk322.keepershour.ui.LampAmber
import com.tomk322.keepershour.ui.SeaDeep

private const val STICK_DP = 132
private const val DEADZONE = 0.16f

/**
 * A thumbstick that reports a unit vector.
 *
 * [onMove] is called with the raw direction rather than a position, so the movement loop stays the
 * only thing that knows about speed or collision.
 */
@Composable
fun Thumbstick(onMove: (Offset) -> Unit, modifier: Modifier = Modifier) {
    var knob by remember { mutableStateOf(Offset.Zero) }
    val radiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { (STICK_DP / 2).dp.toPx() }

    Box(
        modifier = modifier
            .size(STICK_DP.dp)
            .drawBehind {
                drawCircle(Color(0x22FFFFFF), size.minDimension / 2f)
                drawCircle(Color(0x33FFFFFF), size.minDimension / 2f, style = Stroke(2f))
                drawCircle(LampAmber.copy(alpha = 0.55f), size.minDimension * 0.19f, center + knob)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { knob = Offset.Zero; onMove(Offset.Zero) },
                    onDragCancel = { knob = Offset.Zero; onMove(Offset.Zero) },
                ) { change, delta ->
                    change.consume()
                    val moved = knob + delta
                    val length = moved.getDistance()
                    knob = if (length > radiusPx) moved * (radiusPx / length) else moved
                    val normalised = knob / radiusPx
                    onMove(if (normalised.getDistance() < DEADZONE) Offset.Zero else normalised)
                }
            },
    )
}

/** The action button. It only appears when there is something in reach worth doing. */
@Composable
fun ActionButton(label: String?, onPress: () -> Unit, modifier: Modifier = Modifier) {
    if (label == null) return
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            onClick = onPress,
            shape = CircleShape,
            color = LampAmber,
            modifier = Modifier.size(84.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("ACT", style = Instrument, color = SeaDeep)
            }
        }
    }
}
