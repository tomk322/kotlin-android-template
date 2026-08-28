package com.tomk322.keepershour.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val FRAME_MS = 16L

/** Reveal the whole passage in roughly this many frames, whatever its length. */
private const val TARGET_FRAMES = 190
private const val BLANK_LINE = "\n\n"

/**
 * Types a passage out a few characters at a time.
 *
 * The reveal rate scales with length rather than being fixed per character, so a two-line beat and
 * a full ending both land in about three seconds and neither makes the player wait on the machine.
 */
@Composable
fun TypedProse(
    text: String,
    skipToEnd: Boolean,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
) {
    var revealed by remember(text) { mutableIntStateOf(0) }
    val step = remember(text) { (text.length / TARGET_FRAMES).coerceAtLeast(1) }

    LaunchedEffect(text) {
        while (revealed < text.length) {
            delay(FRAME_MS)
            revealed = (revealed + step).coerceAtMost(text.length)
        }
        onFinished()
    }

    LaunchedEffect(skipToEnd) {
        if (skipToEnd && revealed < text.length) {
            revealed = text.length
            onFinished()
        }
    }

    val shown = remember(text, revealed) { text.take(revealed) }

    Column(modifier = modifier.fillMaxWidth()) {
        for (paragraph in shown.split(BLANK_LINE)) {
            if (paragraph.isBlank()) continue
            Text(
                text = emphasise(paragraph.trim()),
                style = Prose,
                color = Foam,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
            )
        }
    }
}

/** Renders *starred* runs as italics, which the story uses for letters and log entries. */
fun emphasise(text: String): AnnotatedString = buildAnnotatedString {
    var italic = false
    var index = 0
    while (index < text.length) {
        val marker = text.indexOf('*', index)
        if (marker < 0) {
            appendStyled(text.substring(index), italic)
            break
        }
        appendStyled(text.substring(index, marker), italic)
        italic = !italic
        index = marker + 1
    }
}

private fun Builder.appendStyled(
    text: String,
    italic: Boolean,
) {
    if (text.isEmpty()) return
    if (italic) {
        withStyleItalic { append(text) }
    } else {
        append(text)
    }
}

private inline fun Builder.withStyleItalic(
    block: Builder.() -> Unit,
) {
    val start = pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = LampGlow))
    block()
    pop(start)
}
