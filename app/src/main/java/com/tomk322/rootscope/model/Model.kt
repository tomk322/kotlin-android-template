package com.tomk322.rootscope.model

/** Colour/severity hint for a single reported value. */
enum class Tone { NEUTRAL, GOOD, WARN, BAD, ACCENT }

data class Row(
    val label: String,
    val value: String,
    val tone: Tone = Tone.NEUTRAL,
    /** Longer explanation shown under the value, for things that are easy to misread. */
    val note: String? = null,
)

data class Section(
    val title: String,
    val rows: List<Row>,
    val subtitle: String? = null,
    val initiallyExpanded: Boolean = false,
)

/**
 * A single root indicator.
 *
 * [conclusive] separates proof from circumstance: a working `su` proves root, whereas test-keys
 * or an unlocked bootloader merely correlate with it. Only conclusive signals move the verdict.
 */
data class Signal(
    val name: String,
    val detected: Boolean,
    val detail: String,
    val conclusive: Boolean = false,
)

enum class RootState(val label: String, val blurb: String) {
    GRANTED(
        "ROOTED",
        "su executed and returned uid 0. This device has working root and this app holds it.",
    ),
    DENIED(
        "ROOT DENIED",
        "A superuser daemon is present but refused this app. Root exists; Rootscope was not given it.",
    ),
    PRESENT_NOT_GRANTED(
        "ROOT PRESENT",
        "Root artefacts were found but su did not return uid 0. Grant the prompt, or root is inactive.",
    ),
    NONE(
        "NOT ROOTED",
        "No working su and no root artefacts visible to an unprivileged app.",
    ),
    UNKNOWN("UNKNOWN", "Detection did not complete."),
}

data class RootVerdict(
    val state: RootState,
    val solution: String,
    val solutionVersion: String?,
    val idOutput: String?,
    val signals: List<Signal>,
) {
    val detectedSignals: List<Signal> get() = signals.filter { it.detected }
}

data class Report(
    val verdict: RootVerdict,
    val sections: List<Section>,
    val generatedAtMillis: Long,
)
