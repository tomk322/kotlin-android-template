package com.tomk322.rootscope.scan

import android.content.pm.PackageManager
import android.os.Build
import com.tomk322.rootscope.core.CmdResult
import com.tomk322.rootscope.core.Props
import com.tomk322.rootscope.model.RootState
import com.tomk322.rootscope.model.RootVerdict
import com.tomk322.rootscope.model.Row
import com.tomk322.rootscope.model.Section
import com.tomk322.rootscope.model.Signal
import com.tomk322.rootscope.model.Tone

/**
 * Interprets [RootEvidence] into a verdict and the root-related report sections.
 *
 * Modern su implementations deliberately hide from unprivileged apps: Magisk 24+ mounts under a
 * randomised /debug_ramdisk path, KernelSU keeps its state kernel-side, and both can repackage
 * their manager app under a random package name. A clean filesystem probe is therefore weak
 * evidence of *no* root, and the UI says so rather than implying certainty. Executing su is the
 * only conclusive test, which is why the verdict hinges on it.
 */
object RootScanner {

    data class Outcome(val verdict: RootVerdict, val sections: List<Section>)

    private val DENIAL_MARKERS = listOf("denied", "not allowed", "refused", "rejected")

    suspend fun scan(pm: PackageManager, props: Props, requestRoot: Boolean): Outcome {
        val evidence = RootEvidence.collect(pm, props, requestRoot)
        val signals = buildSignals(evidence)
        val solution = identifySolution(evidence)

        val verdict = RootVerdict(
            state = determineState(evidence),
            solution = solution,
            solutionVersion = null,
            idOutput = evidence.idResult?.text?.ifBlank { null },
            signals = signals,
        )

        return Outcome(
            verdict = verdict,
            sections = listOf(
                signalSection(signals),
                environmentSection(evidence, solution),
                integritySection(evidence),
            ),
        )
    }

    private fun determineState(evidence: RootEvidence): RootState = when {
        evidence.uidZero -> RootState.GRANTED
        evidence.idResult != null && looksDenied(evidence.idResult) -> RootState.DENIED
        evidence.hasArtefacts -> RootState.PRESENT_NOT_GRANTED
        else -> RootState.NONE
    }

    /** su exists but said no - worth distinguishing from su being absent entirely. */
    private fun looksDenied(result: CmdResult): Boolean {
        val text = (result.stdout + result.stderr).lowercase()
        return DENIAL_MARKERS.any { it in text } ||
            (result.exitCode != 0 && !text.contains("not found"))
    }

    private fun identifySolution(evidence: RootEvidence): String = when {
        evidence.kernelSuFiles.isNotEmpty() -> "KernelSU"
        evidence.apatchFiles.isNotEmpty() -> "APatch"
        evidence.magiskFiles.isNotEmpty() -> "Magisk"
        evidence.managers.isNotEmpty() -> evidence.managers.values.first()
        evidence.uidZero -> "Unidentified su implementation"
        else -> "None detected"
    }

    private fun buildSignals(evidence: RootEvidence): List<Signal> = listOf(
        Signal(
            name = "su returns uid 0",
            detected = evidence.uidZero,
            detail = describeIdProbe(evidence),
            conclusive = true,
        ),
        Signal(
            name = "su binary present",
            detected = evidence.suOnDisk.isNotEmpty(),
            detail = evidence.suOnDisk.firstOrNull()
                ?: "no readable su in ${RootPaths.SU.size} known locations",
        ),
        Signal(
            name = "su on PATH",
            detected = evidence.suInPath.ok && evidence.suInPath.text.isNotBlank(),
            detail = evidence.suInPath.text.ifBlank { "not resolvable" },
        ),
        Signal("Magisk artefacts", evidence.magiskFiles.isNotEmpty(), "${evidence.magiskFiles.size} path(s)"),
        Signal("KernelSU artefacts", evidence.kernelSuFiles.isNotEmpty(), "${evidence.kernelSuFiles.size} path(s)"),
        Signal("APatch artefacts", evidence.apatchFiles.isNotEmpty(), "${evidence.apatchFiles.size} path(s)"),
        Signal(
            name = "Superuser manager installed",
            detected = evidence.managers.isNotEmpty(),
            detail = evidence.managers.values.joinToString()
                .ifBlank { "none of ${RootPaths.MANAGER_PACKAGES.size} known packages" },
        ),
        Signal(
            name = "System mounted rw",
            detected = evidence.writableMounts.isNotEmpty(),
            detail = evidence.writableMounts.joinToString().ifBlank { "all read-only" },
        ),
        Signal("test-keys build", evidence.testKeys, Build.TAGS ?: "unknown"),
        Signal(
            name = "Debuggable build type",
            detected = evidence.buildType != "user",
            detail = "ro.build.type=${evidence.buildType}",
        ),
        Signal(
            name = "Dangerous properties",
            detected = evidence.dangerousProps.isNotEmpty(),
            detail = evidence.dangerousProps.keys.joinToString().ifBlank { "none" },
        ),
    )

    private fun describeIdProbe(evidence: RootEvidence): String {
        val result = evidence.idResult ?: return "not attempted yet"
        return when {
            evidence.uidZero -> result.text
            result.timedOut -> "timed out - prompt ignored or su hung"
            else -> "exit ${result.exitCode}: ${result.text.ifBlank { "no output" }}"
        }
    }

    private fun signalSection(signals: List<Signal>): Section = Section(
        title = "Detection signals",
        subtitle = "${signals.count { it.detected }} of ${signals.size} triggered",
        initiallyExpanded = true,
        rows = signals.map { signal ->
            Row(
                // A leading marker flags the one signal that actually proves root.
                label = (if (signal.conclusive) "* " else "") + signal.name,
                value = if (signal.detected) "DETECTED" else "clear",
                tone = when {
                    signal.detected && signal.conclusive -> Tone.GOOD
                    signal.detected -> Tone.WARN
                    else -> Tone.NEUTRAL
                },
                note = signal.detail.ifBlank { null },
            )
        },
    )

    private fun environmentSection(evidence: RootEvidence, solution: String): Section = Section(
        title = "Superuser environment",
        initiallyExpanded = true,
        rows = listOfNotNull(
            Row("Solution", solution, if (solution == "None detected") Tone.NEUTRAL else Tone.ACCENT),
            Row(
                label = "su binaries on disk",
                value = evidence.suOnDisk.joinToString("\n").ifBlank { "none visible" },
                tone = if (evidence.suOnDisk.isEmpty()) Tone.NEUTRAL else Tone.WARN,
                note = "Magisk 24+ and KernelSU hide su from unprivileged apps, so an empty list " +
                    "does not rule out root.".takeIf { evidence.suOnDisk.isEmpty() },
            ),
            Row("su resolvable in PATH", evidence.suInPath.text.ifBlank { "no" }),
            Row("Magisk artefacts", evidence.magiskFiles.joinToString("\n").ifBlank { "none visible" }),
            Row("KernelSU artefacts", evidence.kernelSuFiles.joinToString("\n").ifBlank { "none visible" }),
            Row("APatch artefacts", evidence.apatchFiles.joinToString("\n").ifBlank { "none visible" }),
            Row(
                label = "Manager apps installed",
                value = evidence.managers.values.joinToString("\n").ifBlank { "none visible" },
                note = "Only known package names are probed; a manager repackaged under a random " +
                    "name is invisible here.",
            ),
            evidence.idResult?.let {
                Row(
                    label = "su -c id",
                    value = it.text.ifBlank { "no output (exit ${it.exitCode})" },
                    tone = if (evidence.uidZero) Tone.GOOD else Tone.BAD,
                )
            },
        ),
    )

    private fun integritySection(evidence: RootEvidence): Section = Section(
        title = "Build integrity",
        subtitle = "Correlates with tampering; none of it proves root on its own",
        rows = listOf(
            Row(
                label = "Build tags",
                value = Build.TAGS ?: "unknown",
                tone = if (evidence.testKeys) Tone.WARN else Tone.GOOD,
                note = "Signed with test-keys - not a stock release build."
                    .takeIf { evidence.testKeys },
            ),
            Row(
                label = "Build type",
                value = evidence.buildType,
                tone = if (evidence.buildType == "user") Tone.GOOD else Tone.WARN,
            ),
            Row(
                label = "Dangerous properties",
                value = evidence.dangerousProps.entries
                    .joinToString("\n") { "${it.key}=${it.value}" }
                    .ifBlank { "none set" },
                tone = if (evidence.dangerousProps.isEmpty()) Tone.GOOD else Tone.WARN,
            ),
            Row(
                label = "System partitions mounted rw",
                value = evidence.writableMounts.joinToString("\n").ifBlank { "none" },
                tone = if (evidence.writableMounts.isEmpty()) Tone.GOOD else Tone.BAD,
                note = if (evidence.writableMounts.isEmpty()) {
                    "Read-only, as expected. Systemless root does not remount these."
                } else {
                    "A writable system partition means the image itself has been modified."
                },
            ),
        ),
    )
}
