package com.tomk322.rootscope.scan

import com.tomk322.rootscope.core.Shell
import com.tomk322.rootscope.model.Row
import com.tomk322.rootscope.model.Section
import com.tomk322.rootscope.model.Tone

/**
 * Information that only exists once su has actually been granted.
 *
 * Each probe is independent and failure-tolerant: a device with KernelSU has no `magisk` binary,
 * a device with Magisk has no `ksud`, and neither should blank out the rest of the section.
 */
object RootExtras {

    private const val LONG_TIMEOUT = 8_000L

    suspend fun scan(): List<Section> {
        val identity = Shell.su("id")
        val magiskVersion = Shell.su("magisk -c 2>/dev/null || magisk -v 2>/dev/null")
        val magiskVersionCode = Shell.su("magisk -V 2>/dev/null")
        val ksuVersion = Shell.su("ksud -V 2>/dev/null")
        val apatchVersion = Shell.su("apd -V 2>/dev/null")
        val suVersion = Shell.su("su -v 2>/dev/null")
        val selinuxContext = Shell.su("cat /proc/self/attr/current 2>/dev/null")
        val modules = Shell.su("ls /data/adb/modules 2>/dev/null", LONG_TIMEOUT)
        val moduleDetail = Shell.su(
            "for m in /data/adb/modules/*/; do " +
                "[ -f \"\$m/module.prop\" ] && " +
                "printf '%s  [%s]\\n' " +
                "\"\$(grep -m1 '^name=' \"\$m/module.prop\" | cut -d= -f2)\" " +
                "\"\$(basename \$m)\"; done 2>/dev/null",
            LONG_TIMEOUT,
        )
        val denylist = Shell.su("magisk --denylist ls 2>/dev/null", LONG_TIMEOUT)
        val zygisk = Shell.su("magisk --sqlite \"select * from settings\" 2>/dev/null")
        val mountCount = Shell.su("wc -l < /proc/mounts")
        val packageCount = Shell.su("pm list packages 2>/dev/null | wc -l", LONG_TIMEOUT)
        val kernelCmdline = Shell.su("cat /proc/cmdline 2>/dev/null", LONG_TIMEOUT)
        val bootSlot = Shell.su("getprop ro.boot.slot_suffix")

        return listOf(
            Section(
                title = "Root session",
                subtitle = "Live data from the granted su shell",
                initiallyExpanded = true,
                rows = listOfNotNull(
                    Row("id", identity.text.ifBlank { "no output" }, Tone.GOOD),
                    row("SELinux context", selinuxContext),
                    row("su -v", suVersion),
                    row("Magisk version", magiskVersion, Tone.ACCENT),
                    row("Magisk version code", magiskVersionCode, Tone.ACCENT),
                    row("KernelSU version", ksuVersion, Tone.ACCENT),
                    row("APatch version", apatchVersion, Tone.ACCENT),
                    row("Boot slot", bootSlot),
                ),
            ),
            Section(
                title = "Modules & denylist",
                subtitle = moduleSubtitle(modules),
                rows = listOfNotNull(
                    row("Installed modules", moduleDetail.takeIf { it.ok && it.text.isNotBlank() } ?: modules),
                    row("Denylist entries", denylist),
                    row("Magisk settings", zygisk),
                ).ifEmpty { listOf(Row("Modules", "none found, or this su has no module system")) },
            ),
            Section(
                title = "Privileged counters",
                rows = listOfNotNull(
                    row("Mounted filesystems", mountCount),
                    row("Installed packages", packageCount),
                    row("Kernel cmdline", kernelCmdline),
                ),
            ),
        )
    }

    private fun moduleSubtitle(modules: com.tomk322.rootscope.core.CmdResult): String? =
        modules.text.lineSequence().filter { it.isNotBlank() }.count()
            .takeIf { modules.ok && it > 0 }?.let { "$it installed" }

    /** Drops probes that produced nothing, so absent tooling does not litter the UI. */
    private fun row(
        label: String,
        result: com.tomk322.rootscope.core.CmdResult,
        tone: Tone = Tone.NEUTRAL,
    ): Row? = result.text.takeIf { result.ok && it.isNotBlank() }?.let { Row(label, it, tone) }
}
