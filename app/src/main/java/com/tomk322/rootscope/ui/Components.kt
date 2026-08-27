package com.tomk322.rootscope.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomk322.rootscope.model.RootState
import com.tomk322.rootscope.model.RootVerdict
import com.tomk322.rootscope.model.Row as InfoRow
import com.tomk322.rootscope.model.Section
import com.tomk322.rootscope.model.Tone

private const val BORDER_ALPHA = 0.4f
private const val FILL_ALPHA = 0.12f
private const val PILL_BORDER_ALPHA = 0.35f
private val VERDICT_TEXT_SIZE = 30.sp

@Composable
fun VerdictCard(verdict: RootVerdict, modifier: Modifier = Modifier) {
    val accent = verdict.state.tone().color()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, accent.copy(alpha = BORDER_ALPHA), RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = verdict.state.label,
                style = MonoBoldStyle.copy(fontSize = VERDICT_TEXT_SIZE),
                color = accent,
            )
            Text(
                text = verdict.state.blurb,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            LabelledValue("Solution", verdict.solution, accent)
            verdict.solutionVersion?.let { LabelledValue("Version", it, accent) }
            verdict.idOutput?.let { LabelledValue("id", it, accent) }
            LabelledValue(
                label = "Signals",
                value = "${verdict.detectedSignals.size} of ${verdict.signals.size} triggered",
                valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun RootState.tone(): Tone = when (this) {
    RootState.GRANTED -> Tone.GOOD
    RootState.DENIED, RootState.PRESENT_NOT_GRANTED -> Tone.WARN
    RootState.NONE, RootState.UNKNOWN -> Tone.NEUTRAL
}

@Composable
private fun LabelledValue(label: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(text = value, style = MonoStyle, color = valueColor)
    }
}

/**
 * Header for one section.
 *
 * Rows are emitted as separate lazy items by the caller rather than nested in a Column here: the
 * system-properties section alone runs to a couple of thousand rows on a typical device, and
 * composing those in a single pass stalls the UI thread for seconds.
 */
@Composable
fun SectionHeader(
    section: Section,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                section.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = if (expanded) "-" else "+",
                style = MonoBoldStyle,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** One value, rendered as its own list item so long sections stay lazy. */
@Composable
fun ValueCard(row: InfoRow, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = row.value, style = MonoStyle, color = row.tone.color())
            row.note?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
fun Pill(text: String, tone: Tone, modifier: Modifier = Modifier) {
    val color = tone.color()
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = FILL_ALPHA), CircleShape)
            .border(1.dp, color.copy(alpha = PILL_BORDER_ALPHA), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
