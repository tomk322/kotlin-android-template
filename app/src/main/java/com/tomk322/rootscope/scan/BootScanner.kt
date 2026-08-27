package com.tomk322.rootscope.scan

import com.tomk322.rootscope.core.Props
import com.tomk322.rootscope.core.readFileOrNull
import com.tomk322.rootscope.model.Row
import com.tomk322.rootscope.model.Section
import com.tomk322.rootscope.model.Tone

/** Verified boot, bootloader lock, SELinux and encryption state. */
object BootScanner {

    fun scan(props: Props): Section = Section(
        title = "Boot & security state",
        initiallyExpanded = true,
        rows = listOf(
            selinuxRow(),
            verifiedBootRow(props.firstOf("ro.boot.verifiedbootstate", "ro.boot.vbmeta.device_state")),
            bootloaderRow(props.firstOf("ro.boot.flash.locked", "ro.boot.locked")),
            Row("dm-verity mode", props.get("ro.boot.veritymode", "not reported")),
            Row("Encryption state", props.firstOf("ro.crypto.state", "ro.crypto.type") ?: "not reported"),
            Row("File-based encryption", props.get("ro.crypto.file_encryption", "not reported")),
            Row("Secure boot", props.get("ro.boot.secureboot", "not reported")),
            Row("Warranty bit / KNOX", props.get("ro.boot.warranty_bit", "not applicable")),
            Row("Debuggable (ro.debuggable)", props.get("ro.debuggable", "0")),
            Row("ro.secure", props.get("ro.secure", "1")),
            Row("ADB over TCP", props.get("service.adb.tcp.port", "not enabled")),
        ),
    )

    private fun selinuxRow(): Row {
        val mode = readFileOrNull("/sys/fs/selinux/enforce")
            ?.let { if (it.trim() == "1") "Enforcing" else "Permissive" }
            ?: "unreadable"
        return Row(
            label = "SELinux",
            value = mode,
            tone = when (mode) {
                "Enforcing" -> Tone.GOOD
                "Permissive" -> Tone.BAD
                else -> Tone.NEUTRAL
            },
            note = "Policy is not being enforced. Strong indicator of a modified boot image."
                .takeIf { mode == "Permissive" },
        )
    }

    private fun verifiedBootRow(state: String?) = Row(
        label = "Verified boot state",
        value = state ?: "not reported",
        tone = when (state?.lowercase()) {
            "green", "locked" -> Tone.GOOD
            "orange", "yellow", "red", "unlocked" -> Tone.WARN
            else -> Tone.NEUTRAL
        },
        note = when (state?.lowercase()) {
            "green" -> "Green: image verified against the OEM key."
            "orange" -> "Orange: bootloader unlocked, verification disabled."
            "yellow" -> "Yellow: boot image signed with a custom key."
            "red" -> "Red: verification failed."
            else -> null
        },
    )

    private fun bootloaderRow(locked: String?) = Row(
        label = "Bootloader locked",
        value = when (locked) {
            "1" -> "yes"
            "0" -> "no"
            null -> "not reported"
            else -> locked
        },
        tone = if (locked == "0") Tone.WARN else Tone.NEUTRAL,
    )
}
