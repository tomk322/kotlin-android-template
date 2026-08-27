package com.tomk322.rootscope.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tomk322.rootscope.model.Report
import com.tomk322.rootscope.model.RootState
import com.tomk322.rootscope.model.Section
import com.tomk322.rootscope.model.Tone

@Composable
fun RootscopeScreen(
    state: ScanState,
    onRescan: (requestRoot: Boolean) -> Unit,
    onShare: (Report) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state) {
                is ScanState.Idle -> Loading("Starting scan")
                is ScanState.Running -> Loading(state.message)
                is ScanState.Done -> ReportBody(state.report, onRescan, onShare)
            }
        }
    }
}

@Composable
private fun Loading(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
            text = message,
            style = MonoStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

@Composable
private fun ReportBody(
    report: Report,
    onRescan: (Boolean) -> Unit,
    onShare: (Report) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val sections = remember(report, query) { filterSections(report.sections, query) }
    val granted = report.verdict.state == RootState.GRANTED
    // Expansion is keyed by section title and survives filtering and rescans.
    val toggled = remember { mutableStateMapOf<String, Boolean>() }
    val filtering = query.isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "ROOTSCOPE",
                style = MonoBoldStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        item { VerdictCard(report.verdict) }
        item { SummaryPills(report) }
        item { ActionRow(granted, onRescan) { onShare(report) } }
        item { FilterField(query) { query = it } }

        for (section in sections) {
            // While filtering, every surviving section is forced open so a search can never hide
            // its own results behind a collapsed header.
            val expanded = filtering || (toggled[section.title] ?: section.initiallyExpanded)

            item(key = "header:${section.title}") {
                SectionHeader(section, expanded, onToggle = { toggled[section.title] = !expanded })
            }
            if (expanded) {
                // Row indices, not labels: the properties dump can repeat a label.
                itemsIndexed(
                    items = section.rows,
                    key = { index, _ -> "row:${section.title}:$index" },
                ) { _, row -> ValueCard(row) }
            }
        }

        if (sections.isEmpty()) {
            item {
                Text(
                    text = "No rows match \"$query\"",
                    style = MonoStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryPills(report: Report) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val granted = report.verdict.state == RootState.GRANTED
        Pill(report.verdict.solution, if (granted) Tone.GOOD else Tone.NEUTRAL)
        Pill(
            "${report.verdict.detectedSignals.size}/${report.verdict.signals.size} signals",
            Tone.ACCENT,
        )
    }
}

@Composable
private fun ActionRow(granted: Boolean, onRescan: (Boolean) -> Unit, onShare: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (!granted) {
            Button(onClick = { onRescan(true) }, modifier = Modifier.weight(1f)) {
                Text("Request root", style = MonoStyle)
            }
        }
        OutlinedButton(onClick = { onRescan(granted) }, modifier = Modifier.weight(1f)) {
            Text("Rescan", style = MonoStyle)
        }
        OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
            Text("Export", style = MonoStyle)
        }
    }
}

@Composable
private fun FilterField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MonoStyle,
        label = { Text("Filter", style = MaterialTheme.typography.labelSmall) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

/**
 * Filters rows by label, value and note. Sections keeping matches are force-expanded, so a search
 * never hides its own results behind a collapsed header.
 */
private fun filterSections(sections: List<Section>, query: String): List<Section> {
    if (query.isBlank()) return sections
    val needle = query.trim().lowercase()
    return sections.mapNotNull { section ->
        val matches = section.rows.filter { row ->
            needle in row.label.lowercase() ||
                needle in row.value.lowercase() ||
                needle in row.note.orEmpty().lowercase()
        }
        when {
            matches.isNotEmpty() -> section.copy(rows = matches, initiallyExpanded = true)
            needle in section.title.lowercase() -> section.copy(initiallyExpanded = true)
            else -> null
        }
    }
}
