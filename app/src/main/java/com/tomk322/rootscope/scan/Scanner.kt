package com.tomk322.rootscope.scan

import android.content.Context
import com.tomk322.rootscope.core.Props
import com.tomk322.rootscope.model.Report
import com.tomk322.rootscope.model.RootState
import com.tomk322.rootscope.model.Row
import com.tomk322.rootscope.model.Section

/** Runs the full sweep and assembles the report the UI renders. */
object Scanner {

    suspend fun run(context: Context, requestRoot: Boolean): Report {
        val props = Props.load()
        val root = RootScanner.scan(context.packageManager, props, requestRoot)

        val sections = buildList {
            addAll(root.sections)
            // Privileged probes are only meaningful once su has actually been granted.
            if (root.verdict.state == RootState.GRANTED) {
                addAll(RootExtras.scan())
            }
            addAll(DeviceScanner.scan(props))
            addAll(HardwareScanner.scan(context))
            add(propsSection(props))
        }

        return Report(
            verdict = root.verdict,
            sections = sections,
            generatedAtMillis = System.currentTimeMillis(),
        )
    }

    /** The raw getprop dump: useful, but hundreds of rows, so it goes last and starts collapsed. */
    private fun propsSection(props: Props) = Section(
        title = "All system properties",
        subtitle = "${props.size} properties",
        rows = props.all().entries.sortedBy { it.key }.map { Row(it.key, it.value) },
    )

    /** Flattens a report into shareable plain text. */
    fun toPlainText(report: Report): String = buildString {
        appendLine("Rootscope report")
        appendLine("Verdict: ${report.verdict.state.label}")
        appendLine("Solution: ${report.verdict.solution}")
        report.verdict.idOutput?.let { appendLine("id: $it") }
        appendLine()
        for (section in report.sections) {
            appendLine("== ${section.title} ==")
            section.subtitle?.let { appendLine("   ($it)") }
            for (row in section.rows) {
                appendLine("${row.label}: ${row.value.replace("\n", "; ")}")
            }
            appendLine()
        }
    }
}
