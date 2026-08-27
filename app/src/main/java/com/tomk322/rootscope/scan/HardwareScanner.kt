package com.tomk322.rootscope.scan

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import com.tomk322.rootscope.core.Format
import com.tomk322.rootscope.core.readFileOrNull
import com.tomk322.rootscope.model.Row
import com.tomk322.rootscope.model.Section
import com.tomk322.rootscope.model.Tone
import java.net.NetworkInterface
import java.util.Locale

/** Live resource state: memory, storage, display, power and network links. */
object HardwareScanner {

    private const val BYTES_PER_MIB = 1024L * 1024L

    fun scan(context: Context): List<Section> = listOf(
        memory(context),
        storage(),
        display(context),
        BatteryScanner.scan(context),
        network(),
    )

    private fun memory(context: Context): Section {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val meminfo = readFileOrNull("/proc/meminfo").orEmpty()

        return Section(
            title = "Memory",
            subtitle = Format.bytes(mem.totalMem),
            rows = listOfNotNull(
                Row("Total RAM", Format.bytes(mem.totalMem), Tone.ACCENT),
                Row("Available", Format.bytes(mem.availMem)),
                Row("Used", Format.bytes(mem.totalMem - mem.availMem)),
                Row("Low-memory threshold", Format.bytes(mem.threshold)),
                Row(
                    "Currently low on memory",
                    if (mem.lowMemory) "yes" else "no",
                    if (mem.lowMemory) Tone.WARN else Tone.GOOD,
                ),
                meminfoKb(meminfo, "MemFree")?.let { Row("MemFree", Format.kilobytes(it)) },
                meminfoKb(meminfo, "Cached")?.let { Row("Cached", Format.kilobytes(it)) },
                meminfoKb(meminfo, "SwapTotal")?.let { Row("Swap / zram total", Format.kilobytes(it)) },
                meminfoKb(meminfo, "SwapFree")?.let { Row("Swap / zram free", Format.kilobytes(it)) },
                Row("App heap limit", Format.bytes(am.memoryClass * BYTES_PER_MIB)),
                Row("Large heap limit", Format.bytes(am.largeMemoryClass * BYTES_PER_MIB)),
            ),
        )
    }

    /** /proc/meminfo lines look like `MemFree:  1234 kB`. */
    private fun meminfoKb(meminfo: String, key: String): Long? = meminfo.lineSequence()
        .firstOrNull { it.startsWith("$key:") }
        ?.filter(Char::isDigit)
        ?.toLongOrNull()

    private fun storage(): Section {
        val data = statFs(Environment.getDataDirectory().absolutePath)
        val external = statFs(Environment.getExternalStorageDirectory().absolutePath)
        val system = statFs(Environment.getRootDirectory().absolutePath)

        return Section(
            title = "Storage",
            subtitle = data?.let { Format.bytes(it.first) }.orEmpty(),
            rows = listOfNotNull(
                data?.let { Row("/data total", Format.bytes(it.first), Tone.ACCENT) },
                data?.let { Row("/data free", Format.bytes(it.second)) },
                data?.let { Row("/data used", Format.bytes(it.first - it.second)) },
                external?.let { Row("External total", Format.bytes(it.first)) },
                external?.let { Row("External free", Format.bytes(it.second)) },
                system?.let { Row("/system total", Format.bytes(it.first)) },
                Row("External storage state", Environment.getExternalStorageState()),
                Row("External is emulated", Environment.isExternalStorageEmulated().toString()),
            ),
        )
    }

    /** Returns total to available bytes, or null when the path cannot be stat'ed. */
    private fun statFs(path: String): Pair<Long, Long>? = runCatching {
        val fs = StatFs(path)
        fs.blockSizeLong * fs.blockCountLong to fs.blockSizeLong * fs.availableBlocksLong
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun display(context: Context): Section {
        val metrics = context.resources.displayMetrics
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val physical = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.maximumWindowMetrics.bounds.let { "${it.width()} x ${it.height()}" }
        } else {
            val real = android.util.DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(real)
            "${real.widthPixels} x ${real.heightPixels}"
        }
        val refresh = runCatching { wm.defaultDisplay.refreshRate }.getOrNull()

        return Section(
            title = "Display",
            subtitle = physical,
            rows = listOfNotNull(
                Row("Physical resolution", physical, Tone.ACCENT),
                Row("Window resolution", "${metrics.widthPixels} x ${metrics.heightPixels}"),
                Row("Density", "${metrics.density}x (${metrics.densityDpi} dpi)"),
                Row("Exact dpi", "x ${metrics.xdpi}, y ${metrics.ydpi}"),
                refresh?.let {
                    Row("Refresh rate", String.format(Locale.US, "%.2f Hz", it), Tone.ACCENT)
                },
                Row("Font scale", context.resources.configuration.fontScale.toString()),
            ),
        )
    }

    private fun network(): Section {
        // NetworkInterface needs no permission and still shows link state and addresses.
        val interfaces = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.inetAddresses.hasMoreElements() }
        }.getOrDefault(emptyList())

        return Section(
            title = "Network interfaces",
            subtitle = "${interfaces.size} up with addresses",
            rows = interfaces.map { iface ->
                Row(
                    label = iface.name +
                        if (runCatching { iface.isLoopback }.getOrDefault(false)) " (loopback)" else "",
                    value = iface.inetAddresses.toList()
                        .joinToString("\n") { it.hostAddress ?: it.toString() }
                        .ifBlank { "no address" },
                    note = runCatching { "MTU ${iface.mtu}" }.getOrNull(),
                )
            }.ifEmpty { listOf(Row("Interfaces", "none reported")) },
        )
    }
}
