package com.tomk322.rootscope.scan

import android.os.Build
import android.os.SystemClock
import com.tomk322.rootscope.core.Format
import com.tomk322.rootscope.core.Format.orUnknown
import com.tomk322.rootscope.core.Props
import com.tomk322.rootscope.core.Shell
import com.tomk322.rootscope.core.readFileOrNull
import com.tomk322.rootscope.model.Row
import com.tomk322.rootscope.model.Section
import com.tomk322.rootscope.model.Tone
import java.util.Locale

/** Identity, ROM, boot state, kernel and CPU. Live resource usage lives in [HardwareScanner]. */
object DeviceScanner {

    suspend fun scan(props: Props): List<Section> = listOf(
        device(props),
        os(props),
        BootScanner.scan(props),
        kernel(),
        cpu(),
    )

    private fun device(props: Props) = Section(
        title = "Device",
        subtitle = "${Build.MANUFACTURER} ${Build.MODEL}",
        initiallyExpanded = true,
        rows = listOf(
            Row("Manufacturer", Build.MANUFACTURER.orUnknown()),
            Row("Brand", Build.BRAND.orUnknown()),
            Row("Model", Build.MODEL.orUnknown()),
            Row("Device (codename)", Build.DEVICE.orUnknown(), Tone.ACCENT),
            Row("Product", Build.PRODUCT.orUnknown()),
            Row("Board", Build.BOARD.orUnknown()),
            Row("Hardware", Build.HARDWARE.orUnknown()),
            Row("Platform", props.get("ro.board.platform", "unknown")),
            Row("SoC manufacturer", props.get("ro.soc.manufacturer", "not reported")),
            Row("SoC model", props.get("ro.soc.model", "not reported")),
            Row("Bootloader", Build.BOOTLOADER.orUnknown()),
            Row("Radio / baseband", Build.getRadioVersion().orUnknown()),
            Row("Primary ABI", Build.SUPPORTED_ABIS.firstOrNull().orUnknown(), Tone.ACCENT),
            Row("Supported ABIs", Build.SUPPORTED_ABIS.joinToString()),
            Row("32-bit ABIs", Build.SUPPORTED_32_BIT_ABIS.joinToString().ifBlank { "none" }),
            Row("64-bit ABIs", Build.SUPPORTED_64_BIT_ABIS.joinToString().ifBlank { "none" }),
        ),
    )

    private fun os(props: Props) = Section(
        title = "Operating system",
        subtitle = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        initiallyExpanded = true,
        rows = buildList {
            add(Row("Android version", Build.VERSION.RELEASE.orUnknown(), Tone.ACCENT))
            add(Row("API level", Build.VERSION.SDK_INT.toString(), Tone.ACCENT))
            add(Row("Codename", Build.VERSION.CODENAME.orUnknown()))
            add(Row("Security patch", Build.VERSION.SECURITY_PATCH.orUnknown()))
            add(Row("Base OS", Build.VERSION.BASE_OS.ifBlank { "not reported" }))
            add(Row("Build ID", Build.ID.orUnknown()))
            add(Row("Build display", Build.DISPLAY.orUnknown()))
            add(Row("Incremental", Build.VERSION.INCREMENTAL.orUnknown()))
            add(Row("Build type", Build.TYPE.orUnknown()))
            add(Row("Build tags", Build.TAGS.orUnknown()))
            add(Row("Build user/host", "${Build.USER}@${Build.HOST}"))
            add(Row("Fingerprint", Build.FINGERPRINT.orUnknown()))
            addAll(customRomRows(props))
            add(Row("Treble enabled", props.get("ro.treble.enabled", "not reported")))
            add(Row("VNDK version", props.get("ro.vndk.version", "not reported")))
            add(Row("Uptime", Format.duration(SystemClock.elapsedRealtime())))
            add(Row("Time awake", Format.duration(SystemClock.uptimeMillis())))
            add(Row("Locale", Locale.getDefault().toString()))
            add(Row("System properties visible", props.size.toString()))
        },
    )

    /** LineageOS and most AOSP forks advertise themselves through their own prop namespace. */
    private fun customRomRows(props: Props): List<Row> = listOfNotNull(
        props.firstOf("ro.lineage.version", "ro.cm.version", "ro.modversion")
            ?.let { Row("LineageOS version", it, Tone.ACCENT) },
        props.firstOf("ro.lineage.build.version", "ro.lineage.display.version")
            ?.let { Row("LineageOS build", it, Tone.ACCENT) },
        props["ro.lineage.releasetype"]?.let { Row("Lineage release type", it) },
        props["ro.lineage.device"]?.let { Row("Lineage device", it) },
    )

    private suspend fun kernel(): Section {
        val banner = readFileOrNull("/proc/version")
        return Section(
            title = "Kernel",
            subtitle = System.getProperty("os.version").orUnknown(),
            rows = listOf(
                Row("Release", System.getProperty("os.version").orUnknown(), Tone.ACCENT),
                Row("Architecture", System.getProperty("os.arch").orUnknown()),
                Row("uname -a", Shell.exec(listOf("uname", "-a")).text.ifBlank { "unavailable" }),
                Row("/proc/version", banner ?: "unreadable"),
                Row(
                    "KernelSU in banner",
                    if (banner?.contains("KernelSU", ignoreCase = true) == true) "yes" else "no",
                ),
            ),
        )
    }

    private fun cpu(): Section {
        val cores = runCatching { Runtime.getRuntime().availableProcessors() }.getOrDefault(0)
        val info = readFileOrNull("/proc/cpuinfo").orEmpty()
        val perCore = (0 until cores).map(::coreRow)
        val allRestricted = perCore.isNotEmpty() && perCore.all { it.value == FREQ_UNAVAILABLE }

        return Section(
            title = "CPU",
            subtitle = "$cores cores",
            rows = buildList {
                add(Row("Cores available", cores.toString(), Tone.ACCENT))
                add(Row("Hardware", cpuInfoField(info, "Hardware") ?: "not reported"))
                add(Row("CPU implementer", cpuInfoField(info, "CPU implementer") ?: "not reported"))
                addAll(perCore)
                if (allRestricted) {
                    add(
                        Row(
                            "Note",
                            "cpufreq sysfs nodes are restricted for apps on recent Android " +
                                "releases. Grant root for per-core frequencies.",
                        ),
                    )
                }
            },
        )
    }

    private const val FREQ_UNAVAILABLE = "frequency nodes not readable"

    private fun coreRow(core: Int): Row {
        val base = "/sys/devices/system/cpu/cpu$core/cpufreq"
        val detail = listOfNotNull(
            readFileOrNull("$base/cpuinfo_min_freq")?.let { Format.khzAsMhz(it) }?.let { "min $it" },
            readFileOrNull("$base/cpuinfo_max_freq")?.let { Format.khzAsMhz(it) }?.let { "max $it" },
            readFileOrNull("$base/scaling_cur_freq")?.let { Format.khzAsMhz(it) }?.let { "now $it" },
            readFileOrNull("$base/scaling_governor")?.let { "gov $it" },
        ).joinToString("  ")
        return Row("cpu$core", detail.ifBlank { FREQ_UNAVAILABLE })
    }

    private fun cpuInfoField(info: String, key: String): String? = info.lineSequence()
        .firstOrNull { it.startsWith(key) }
        ?.substringAfter(":")
        ?.trim()
        ?.ifBlank { null }
}
